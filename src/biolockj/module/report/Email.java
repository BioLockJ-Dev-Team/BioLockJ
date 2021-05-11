/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 9, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.report;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Properties;
import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.mail.*;
import javax.mail.internet.*;
import biolockj.*;
import biolockj.api.ApiModule;
import biolockj.module.BioModuleImpl;
import biolockj.util.*;

/**
 * This BioModule is used to email the user the pipeline execution status and summary. If run on cluster, an scp command
 * is included in the email that can be run to download analysis from the cluster.
 * 
 * @blj.web_desc Email
 */
public class Email extends BioModuleImpl implements ApiModule {
	
	public Email() {
		super();
		addNewProperty( EMAIL_ENCRYPTED_PASSWORD, biolockj.Properties.STRING_TYPE, EMAIL_ENCRYPTED_PASSWORD_DESC );
		addNewProperty( EMAIL_FROM, biolockj.Properties.STRING_TYPE, EMAIL_FROM_DESC );
		addNewProperty( EMAIL_HOST, biolockj.Properties.STRING_TYPE, EMAIL_HOST_DESC );
		addNewProperty( EMAIL_PORT, biolockj.Properties.INTEGER_TYPE, "" );
		addNewProperty( EMAIL_SMTP_AUTH, biolockj.Properties.STRING_TYPE, "" );
		addNewProperty( EMAIL_START_TLS_ENABLE, biolockj.Properties.BOOLEAN_TYPE, "" );
		addNewProperty( EMAIL_TO, biolockj.Properties.STRING_TYPE, "" );
	}

	/**
	 * Verify required email {@link biolockj.Config} properties exist and are properly formatted.
	 * <ol>
	 * <li>Require {@link biolockj.Config}.{@link #EMAIL_HOST}
	 * <li>Require {@link biolockj.Config}.{@link #EMAIL_PORT}
	 * <li>Require {@link biolockj.Config}.{@link #EMAIL_SMTP_AUTH}
	 * <li>Require {@link biolockj.Config}.{@link #EMAIL_START_TLS_ENABLE}
	 * <li>Require {@link biolockj.Config}.{@link #EMAIL_ENCRYPTED_PASSWORD}
	 * <li>Require {@link biolockj.Config}.{@link #EMAIL_FROM} is a valid email address
	 * <li>Require {@link biolockj.Config}.{@link #EMAIL_TO} is a valid email address
	 * <li>
	 * </ol>
	 */
	@Override
	public void checkDependencies() throws Exception {
		Config.requireString( this, EMAIL_HOST );
		Config.requireString( this, EMAIL_PORT );
		Config.requireBoolean( this, EMAIL_SMTP_AUTH );
		Config.requireBoolean( this, EMAIL_START_TLS_ENABLE );
		Config.requireString( this, EMAIL_ENCRYPTED_PASSWORD );
		Config.getString( this, Constants.CLUSTER_HOST );

		new InternetAddress( Config.requireString( this, EMAIL_FROM ) ).validate();
		for( final String email: Config.requireList( this, EMAIL_TO ) )
			new InternetAddress( email ).validate();
	}

	/**
	 * Set status member variable and send email pipeline summary to email addresses listed in:
	 * {@link biolockj.Config}.{@value #EMAIL_TO}
	 */
	@Override
	public void executeTask() throws Exception {
		String emailBody = SummaryUtil.getSummary() + SummaryUtil.getFooter();
		if( emailBody.trim().length() < 1 ) throw new Exception( "Unable to obtain SummaryUtil.getSummary()" );
		try {
			emailBody += RETURN + "Regards," + RETURN + "BioLockJ Admin";
			Log.debug( getClass(), "Attempt to send email ---> " + RETURN + emailBody );
			Transport.send( getMimeMessage( emailBody ) );
			Log.info( getClass(), "EMAIL SENT!" );
			successful = true;
		} catch( final Exception ex ) {
			throw new Exception( "Unable to send email: " + ex.getMessage() );
		}
	}

	/**
	 * Summary simply reports the status.
	 */
	@Override
	public String getSummary() throws Exception {
		if( successful ) return "EMAIL SENT";

		return "EMAIL FAILED";
	}

	/**
	 * Used to authenticate the service email account defined in EMAIL_FROM. WARNING - This is a trivial level of
	 * encryption! Service email account should never send confidential information!
	 *
	 * @param val Encrypted password
	 * @return Clear-text password
	 */
	String decrypt( final String val ) {
		String decryptedPassword = null;
		try {
			final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance( "PBEWithMD5AndDES" );
			final SecretKey key = keyFactory.generateSecret( new PBEKeySpec( PASSWORD ) );
			final Cipher pbeCipher = Cipher.getInstance( "PBEWithMD5AndDES" );
			pbeCipher.init( Cipher.DECRYPT_MODE, key, new PBEParameterSpec( SALT, 20 ) );
			decryptedPassword = new String( pbeCipher.doFinal( base64Decode( val ) ), "UTF-8" );
		} catch( final Exception ex ) {
			Log.error( getClass(), ex.getMessage(), ex );
		}

		return decryptedPassword;

	}

	/**
	 * Build an authenticated javax.mail.Session using {@link biolockj.Config} email properties
	 *
	 * @return javax.mail.Session required to send a MimeMessage
	 * @throws Exception if {@link biolockj.Config} finds missing or invalid email properties
	 */
	protected Session getSession() throws Exception {
		String startTls = "false";
		String smtpAuth = "false";
		if( Config.getBoolean( this, EMAIL_SMTP_AUTH ) ) smtpAuth = "true";
		if( Config.getBoolean( this, EMAIL_START_TLS_ENABLE ) ) startTls = "true";

		final Properties props = new Properties();
		props.put( EMAIL_SMTP_AUTH, smtpAuth );
		props.put( EMAIL_START_TLS_ENABLE, startTls );
		props.put( EMAIL_HOST, Config.requireString( this, EMAIL_HOST ) );
		props.put( EMAIL_PORT, Config.requireString( this, EMAIL_PORT ) );

		final Session session = Session.getInstance( props, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				try {
					return new PasswordAuthentication( Config.requireString( null, EMAIL_FROM ),
						decrypt( Config.requireString( null, EMAIL_ENCRYPTED_PASSWORD ) ) );
				} catch( final Exception ex ) {
					Log.error( getClass(),
						"Unable to build PasswordAuthentication due to missing/invalid properties: " + EMAIL_FROM +
							" or " + EMAIL_ENCRYPTED_PASSWORD + " : " + ex.getMessage(),
						ex );
				}

				return null;
			}
		} );

		return session;
	}

	/**
	 * Create the MimeMessage using the emailBody parameter.
	 *
	 * @param emailBody Pipeline summary
	 * @return MimeMessage
	 * @throws Exception if email properties are missing or invalid
	 */
	private Message getMimeMessage( final String emailBody ) throws Exception {
		final Message message = new MimeMessage( getSession() );
		message.setFrom( new InternetAddress( Config.requireString( this, EMAIL_FROM ) ) );
		message.addRecipients( Message.RecipientType.TO, InternetAddress.parse( getRecipients() ) );
		message.setSubject( "BioLockJ " + Config.pipelineName() + " " + Pipeline.getStatus() );
		message.setContent( getContent( emailBody ), "text/plain; charset=utf-8" );
		return message;
	}

	/**
	 * Returns formatted MimeMessage recipient list read in from {@link biolockj.Config}.{@value #EMAIL_TO}
	 *
	 * @return {@link biolockj.Config}.{@value #EMAIL_TO} as a comma separated list.
	 * @throws Exception if {@link biolockj.Config}.{@value #EMAIL_TO} is missing or invalid
	 */
	private String getRecipients() throws Exception {
		final StringBuffer addys = new StringBuffer();
		for( final String to: Config.requireList( this, EMAIL_TO ) ) {
			if( addys.length() != 0 ) addys.append( "," );
			addys.append( to );
		}

		return addys.toString();
	}

	/**
	 * Read clear-text password by call to {@link biolockj.util.RuntimeParamUtil#getAdminEmailPassword()}<br>
	 * Encrypt password by call to {@link #encrypt(String)}
	 *
	 * @throws Exception if unable to encrypt or store password
	 */
	public static void encryptAndStoreEmailPassword() throws Exception {
		Log.info( Email.class, "About to encrypt and store new admin email password in MASTER Config: " +
			MasterConfigUtil.getMasterConfig().getAbsolutePath() );
		Config.setConfigProperty( EMAIL_ENCRYPTED_PASSWORD, encrypt( RuntimeParamUtil.getAdminEmailPassword() ) );
		if( MasterConfigUtil.saveMasterConfig() ) Log.info( Email.class,
			"New admin email password [ " + EMAIL_ENCRYPTED_PASSWORD + "=" +
				Config.requireString( null, EMAIL_ENCRYPTED_PASSWORD ) + " ] saved to MASTER Config: " +
				MasterConfigUtil.getMasterConfig().getAbsolutePath() );
		else Log.error( Email.class,
			"Failed to store enctryped password in the MASTER Config - check Log file for error details" );
	}

	/**
	 * Encrypt clear-text password
	 *
	 * @param password Clear-text password
	 * @return Encrypted password
	 * @throws GeneralSecurityException Encryption error
	 * @throws UnsupportedEncodingException Encryption error
	 */
	protected static String encrypt( final String password )
		throws GeneralSecurityException, UnsupportedEncodingException {
		final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance( "PBEWithMD5AndDES" );
		final SecretKey key = keyFactory.generateSecret( new PBEKeySpec( PASSWORD ) );
		final Cipher pbeCipher = Cipher.getInstance( "PBEWithMD5AndDES" );
		pbeCipher.init( Cipher.ENCRYPT_MODE, key, new PBEParameterSpec( SALT, 20 ) );
		return base64Encode( pbeCipher.doFinal( password.getBytes( "UTF-8" ) ) );
	}

	private static byte[] base64Decode( final String encodedPassword ) {
		return Base64.getDecoder().decode( encodedPassword );
	}

	private static String base64Encode( final byte[] bytes ) {
		return Base64.getEncoder().encodeToString( bytes );
	}

	/**
	 * Wraps emailBody in javax.mail Multipart/BodyPart
	 *
	 * @param emailBody Pipeline summary
	 * @return Multipart wrapper object
	 * @throws Exception if propagated by MimeBodyPart
	 */
	private static Multipart getContent( final String emailBody ) throws Exception {
		final Multipart multipart = new MimeMultipart();
		final BodyPart mimeMsg = new MimeBodyPart();
		mimeMsg.setText( emailBody );
		multipart.addBodyPart( mimeMsg );
		return multipart;
	}
	
	@Override
	public String getDockerImageOwner() {
		return Constants.MAIN_DOCKER_OWNER;
	}
	
	@Override
	public String getDockerImageName() {
		return Constants.MAIN_DOCKER_IMAGE;
	}

	@Override
	public String getDescription() {
		return "Send an email containing the pipeline summary when the pipeline either completes or fails.";
	}

	@Override
	public String getCitationString() {
		return "Module developed by Mike Sioda." + System.lineSeparator() + "BioLockJ " + BioLockJUtil.getVersion();
	}
	
	@Override
	public String version() {
		return "1.0.0";
	}
		
	/**
	 * {@link biolockj.Config} String property: {@value #EMAIL_ENCRYPTED_PASSWORD}<br>
	 * {@value #EMAIL_ENCRYPTED_PASSWORD_DESC}
	 */
	protected static final String EMAIL_ENCRYPTED_PASSWORD = "mail.encryptedPassword";
	private static final String EMAIL_ENCRYPTED_PASSWORD_DESC = "The Base 64 encrypted password is stored in the Config file using this property.";

	/**
	 * {@link biolockj.Config} String property: {@value #EMAIL_FROM}<br>
	 * {@value #EMAIL_FROM_DESC}
	 */
	protected static final String EMAIL_FROM = "mail.from";
	private static final String EMAIL_FROM_DESC = "Admin email address used to send user pipeline notifications";

	/**
	 * {@link biolockj.Config} String property: {@value #EMAIL_HOST}<br>
	 * {@link javax.mail.Session} SMTP host
	 */
	protected static final String EMAIL_HOST = "mail.smtp.host";
	private static final String EMAIL_HOST_DESC = "javax.mail.Session SMTP host";

	/**
	 * {@link biolockj.Config} Integer property: {@value #EMAIL_PORT}<br>
	 * {@link javax.mail.Session} SMTP port
	 */
	protected static final String EMAIL_PORT = "mail.smtp.port";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #EMAIL_SMTP_AUTH}<br>
	 * {@link javax.mail.Session} SMTP authorization flag, set to {@value biolockj.Constants#TRUE} if required by
	 * {@value #EMAIL_HOST}
	 */
	protected static final String EMAIL_SMTP_AUTH = "mail.smtp.auth";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #EMAIL_START_TLS_ENABLE}<br>
	 * {@link javax.mail.Session} SMTP TLS enable flag, set to {@value biolockj.Constants#TRUE} if required by
	 * {@value #EMAIL_HOST}
	 */
	protected static final String EMAIL_START_TLS_ENABLE = "mail.smtp.starttls.enable";

	/**
	 * {@link biolockj.Config} List property: {@value #EMAIL_TO} This property defines the email recipients
	 */
	protected static final String EMAIL_TO = "mail.to";

	private static final char[] PASSWORD = "enfldsgbnlsngdlksdsgm".toCharArray();

	private static final byte[] SALT =
		{ (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12, (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12, };
	private static boolean successful = false;

}
