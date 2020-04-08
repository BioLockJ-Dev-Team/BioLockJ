/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Jun 16, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj;

import java.io.*;
import java.util.*;
import biolockj.api.API_Exception;
import biolockj.exception.BioLockJException;
import biolockj.exception.ConfigException;
import biolockj.exception.ConfigPathException;
import biolockj.module.BioModule;
import biolockj.module.report.r.R_Module;
import biolockj.util.BashScriptBuilder;
import biolockj.util.BioLockJUtil;
import biolockj.util.DockerUtil;
import biolockj.util.MetaUtil;
import biolockj.util.NextflowUtil;
import biolockj.util.RMetaUtil;
import biolockj.util.ValidationUtil;

/**
 * Load properties defined in the BioLockJ configuration file, including inherited properties from project.defaultProps
 */
public class Properties extends java.util.Properties {

	/**
	 * Default constructor.
	 */
	public Properties() {
		super();
	}

	/**
	 * Constructor called when {@value biolockj.Constants#PIPELINE_DEFAULT_PROPS} contains a valid file-path
	 *
	 * @param defaultConfig Config built from {@value biolockj.Constants#PIPELINE_DEFAULT_PROPS} property
	 */
	public Properties( final Properties defaultConfig ) {
		super( defaultConfig );
	}

	/**
	 * Load properties, adding escape characters where necessary.
	 *
	 * @param fis FileInputStream
	 * @throws IOException if unable to convert escape characters
	 */
	protected void load( final FileInputStream fis ) throws IOException {
		final Scanner in = new Scanner( fis );
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		while( in.hasNext() ) {
			out.write( in.nextLine().replace( "\\", "\\\\" ).getBytes() );
			out.write( Constants.RETURN.getBytes() );
		}
		in.close();
		final InputStream is = new ByteArrayInputStream( out.toByteArray() );
		super.load( is );
	}

	/**
	 * Instantiate {@link biolockj.Properties} via {@link #buildConfig(File)}
	 *
	 * @param file of {@link biolockj.Properties} file
	 * @return Properties Config properties loaded from files
	 * @throws Exception if unable to extract properties from filePath
	 */
	public static Properties loadProperties( final File file ) throws Exception {
		Log.debug( Properties.class, "Run loadProperties for Config: " + file.getAbsolutePath() );
		final Properties props = buildConfig( file );
		props.setProperty( Constants.INTERNAL_BLJ_MODULE,
			BioLockJUtil.getCollectionAsString( getListedModules( file ) ) );
		if( configRegister.size() > 1 )
			props.setProperty( Constants.INTERNAL_DEFAULT_CONFIG, BioLockJUtil.getCollectionAsString(
				BioLockJUtil.getFilePaths( configRegister.subList( 0, configRegister.size() - 1 ) ) ) );
		return props;
	}

	// SIZE = 2
	// list[ 0 ] = standard.props
	// list[ 1 ] = email.props
	// configRegister.size() = 2
	// configRegister.size() - 2 = 0

	/**
	 * Recursive method handles nested default Config files. Default props are overridden by parent level props.<br>
	 * Standard properties are always imported 1st: {@value biolockj.Constants#STANDARD_CONFIG_PATH}<br>
	 * Docker properties are always imported 2nd if in a Docker container:
	 * {@value biolockj.Constants#DOCKER_CONFIG_PATH}<br>
	 * Then nested default Config files defined by property: {@value biolockj.Constants#PIPELINE_DEFAULT_PROPS}<br>
	 * The project Config file is read last to ensure properties are not overridden in the default Config files.
	 * 
	 * @param propFile BioLockJ Configuration file
	 * @return Properties including default props
	 * @throws Exception if errors occur
	 */
	protected static Properties buildConfig( final File propFile ) throws Exception {
		Log.info( Properties.class,
			"Import All Config Properties for --> Top Level Pipeline Properties File: " + propFile.getAbsolutePath() );
		Properties defaultProps = null;
		final File standConf = Config.getLocalConfigFile( Constants.STANDARD_CONFIG_PATH );
		if( standConf != null && !configRegister.contains( propFile ) ) defaultProps = readProps( standConf, null );

		if( DockerUtil.inDockerEnv() ) {
			final File dockConf = Config.getLocalConfigFile( Constants.DOCKER_CONFIG_PATH );
			if( dockConf != null && !configRegister.contains( dockConf ) )
				defaultProps = readProps( dockConf, defaultProps );
		}

		for( final File pipelineDefaultConfig: getNestedDefaultPropertyFiles( propFile ) )
			if( !configRegister.contains( pipelineDefaultConfig ) )
				defaultProps = readProps( pipelineDefaultConfig, defaultProps );

		final Properties props = readProps( propFile, defaultProps );
		report( props, propFile, true );

		return props;
	}

	/**
	 * Parse property file for the property {@value biolockj.Constants#PIPELINE_DEFAULT_PROPS}.<br>
	 * 
	 * @param propFile BioLockJ Config file
	 * @return nested default prop file or null
	 * @throws BioLockJException 
	 * @throws IOException if FileReader reader fails to close.
	 * @throws Exception if errors occur
	 */
	protected static ArrayList<File> getDefaultConfig( final File propFile ) throws BioLockJException, IOException  {
		BufferedReader reader = null;
		ArrayList<File> defProps = new ArrayList<>();
		try {
			reader = BioLockJUtil.getFileReader( propFile );
			for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
				final StringTokenizer st = new StringTokenizer( line, "=" );
				if( st.countTokens() > 1 ) {
					String propName = st.nextToken().trim();
					if ( propName.equals( Constants.PIPELINE_DEFAULT_PROPS ) ||
							propName.equals( Constants.PROJECT_DEFAULT_PROPS )) {
						final StringTokenizer inner = new StringTokenizer( st.nextToken().trim(), "," );
						while ( inner.hasMoreTokens() ) {
							defProps.add( Config.getLocalConfigFile( inner.nextToken().trim() ) );
						}
					}
				}
			}
		} catch( IOException e ) {
			if (propFile.exists()) {
				throw new BioLockJException("An error occurred while attempted to read config file: " + propFile.getAbsolutePath());
			}else {
				throw new ConfigPathException(propFile);
			}
		}finally {
			if( reader != null ) reader.close();
		}
		
		return defProps ;
	}

	/**
	 * Read the properties defined in the required propFile and defaultProps (if included) to build Config.<br>
	 * Properties in propFile will override the defaultProps.
	 *
	 * @param propFile BioLockJ configuration file
	 * @param defaultProps Default properties
	 * @return {@link biolockj.Properties} instance
	 * @throws FileNotFoundException thrown if propFile is not a valid file path
	 * @throws IOException thrown if propFile or defaultProps cannot be parsed to read in properties
	 */
	protected static Properties readProps( final File propFile, final Properties defaultProps )
		throws FileNotFoundException, IOException {
		if( propFile.isFile() ) {
			configRegister.add( propFile );
			Log.info( Properties.class, "LOAD CONFIG [ #" + ++loadOrder + " ]: ---> " + propFile.getAbsolutePath() );
			final FileInputStream in = new FileInputStream( propFile );
			final Properties tempProps = defaultProps == null ? new Properties(): new Properties( defaultProps );
			tempProps.load( in );
			in.close();
			return tempProps;
		}

		return null;
	}

	public static List<String> getListedModules( final File file ) throws Exception {
		final List<String> modules = new ArrayList<>();
		final BufferedReader reader = BioLockJUtil.getFileReader( file );
		try {
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
				if( line.startsWith( Constants.BLJ_MODULE_TAG ) ) {
					final String moduleName = line.replaceFirst( Constants.BLJ_MODULE_TAG, "" ).trim();
					Log.info( Properties.class, "Configured BioModule: " + moduleName );
					modules.add( moduleName );
				}
		} finally {
			reader.close();
		}

		return modules;
	}

	private static List<File> getNestedDefaultPropertyFiles( final File propFile ) throws Exception {
		final List<File> configFiles = new ArrayList<>();
		final LinkedList<File> defConfigs = new LinkedList<>(getDefaultConfig( propFile ));
		while ( defConfigs.size() > 0 ){
			File defConfig = defConfigs.pop();
			if( ! (configRegister.contains( defConfig ) || configFiles.contains( defConfig )) )
			{
				configFiles.add( defConfig );
				defConfigs.addAll( getDefaultConfig( defConfig ) );
			}
		}
		Collections.reverse( configFiles );
		return configFiles;
	}

	private static void report( final Properties properties, final File config, final boolean projectConfigOnly ) {
		Log.debug( Properties.class, " ---------- Report [ " + config.getAbsolutePath() + " ] ------------> " );
		if( projectConfigOnly ) for( final Object key: properties.keySet() )
			Log.debug( Config.class, "Project Config: " + key + "=" + properties.getProperty( (String) key ) );
		else {
			final Enumeration<?> en = properties.propertyNames();
			while( en.hasMoreElements() ) {
				final String key = en.nextElement().toString();
				Log.debug( Properties.class, key + " = " + properties.getProperty( key ) );
			}
		}

		Log.debug( Properties.class,
			" ----------------------------------------------------------------------------------" );
	}
	
	/**
	 * HashMap with property name as key and the description for this property as the value.
	 */
	private static HashMap<String, String> propDescMap = new HashMap<>();
	
	/**
	 * HashMap with property name as key and the type for this property as the value.
	 */
	private static HashMap<String, String> propTypeMap = new HashMap<>();
	
	private static void fillPropMaps() throws API_Exception {
		Constants.registerProps();
		MetaUtil.registerProps();
		NextflowUtil.registerProps();
		BashScriptBuilder.registerProps();
		DockerUtil.registerProps();
		RMetaUtil.registerProps();
		ValidationUtil.registerProps();
		
		//These aws* properties are only used in the bash layer, there is no java class that owns these.
		addToPropMaps( "aws.ec2InstanceID", STRING_TYPE, "ID of an existing ec2 instance to use as the head node" );//TODO: bash property descriptions
		addToPropMaps( "aws.ec2SpotPer", "", "" );//TODO: bash property descriptions
		addToPropMaps( "aws.ec2TerminateHead", BOOLEAN_TYPE, "" );//TODO: bash property descriptions
		addToPropMaps( "aws.profile", FILE_PATH, "" );//TODO: bash property descriptions
		addToPropMaps( "aws.region", STRING_TYPE, "" );//TODO: bash property descriptions
		addToPropMaps( "aws.saveCloud", BOOLEAN_TYPE, "" );//TODO: bash property descriptions
		addToPropMaps( "aws.stack", STRING_TYPE, "An existing aws cloud stack ID" );//TODO: bash property descriptions
		addToPropMaps( "aws.walltime", "", "" ); // I don't see this used anywhere. //TODO: bash property descriptions
	}
	
	private static void addToPropMaps(final String prop, final String type, final String desc) {
		propTypeMap.put( prop, type);
		propDescMap.put( prop, desc );
	}
	
	/**
	 * Allow utility classes to keep their props private but still register them with Properties for API.
	 * @param prop a property
	 * @param type The expected type of value for that property, must be one of the recognized types in Properties class
	 * @param desc Human readable string for users guide and similar uses.
	 * @throws API_Exception 
	 */
	public static void registerProp(final String prop, final String type, final String desc) throws API_Exception {
		if ( prop != null ) {
			testPropName(prop);
			if (! Arrays.asList( KNOWN_TYPES ).contains( type )) {
				addToPropMaps(prop, "", "[" + type + "] " + desc);
			}
			addToPropMaps(prop, type, desc);
		}else {
			throw new API_Exception( "Cannot register a null property" );
		}
	}
	
	//TODO: at least the 2nd test is not working
	private static void testPropName(String prop) throws API_Exception {
		if (! prop.contains( "." )) throw new API_Exception( "Bad property [" + prop + "], no \'.\', property names should contain exactly one \'.\' ." );
		if ( prop.indexOf( "." ) != prop.lastIndexOf( "." ) ) throw new API_Exception( "Bad property [" + prop + "], too many \'.\'s, property names should contain exactly one \'.\' ." );
		if (! prop.startsWith( prop.substring( 0, 1 ).toLowerCase() )) throw new API_Exception( "Bad property [" + prop + "], property names shoudl start with a lower case letter." );
	}
	
	/**
	 * Allow the API to access the list of properties and descriptions.
	 * @return
	 * @throws API_Exception 
	 */
	public static HashMap<String, String> getPropDescMap() throws API_Exception {
		if (propDescMap.size() == 0) fillPropMaps();
		return propDescMap;
	}
	public static String getDescription( String prop ) throws API_Exception {
		if (prop.startsWith( Constants.EXE_PREFIX ) || prop.startsWith( Constants.HOST_EXE_PREFIX ) ) {
			return describeSpecialProp( prop );
		}
		return getPropDescMap().get( prop );
	}
	
	private static String describeSpecialProp(String prop) {
		if (prop.startsWith( Constants.EXE_PREFIX ) ) {
			return "Path for the \"" + prop.replaceFirst( Constants.EXE_PREFIX, "" ) 
							+ "\" executable; if not supplied, any script that needs the "+ prop.replaceFirst( Constants.EXE_PREFIX, "" ) 
							+ " command will assume it is on the PATH." ;
		}else if (prop.startsWith( Constants.HOST_EXE_PREFIX ) ) {
			return "Host machine path for the \"" + prop.replaceFirst( Constants.HOST_EXE_PREFIX, "" ) 
							+ "\" executable. If running a pipeline in docker, use this property in place of " 
							+ Constants.EXE_PREFIX + prop.replaceFirst( Constants.HOST_EXE_PREFIX, "" ) 
							+ " to point to an executable on the host machine rather than a path within the docker container." ;
		}
		return "";
	}
	
	/**
	 * Allow the API to access the list of properties and descriptions.
	 * @return
	 * @throws API_Exception 
	 */
	public static HashMap<String, String> getPropTypeMap() throws API_Exception {
		if (propTypeMap.size() == 0) fillPropMaps();
		return propTypeMap;
	}
	public static String getPropertyType( String prop ) throws API_Exception {
		if (prop.startsWith( Constants.EXE_PREFIX ) ) return EXE_PATH;
		if (prop.startsWith( Constants.HOST_EXE_PREFIX ) ) return EXE_PATH;
		return getPropTypeMap().get( prop );
	}
	
	/**
	 * Verify that a given exe property has a valid value.
	 * @param property
	 * @return
	 */
	public static boolean isValidExeProp(BioModule module, String property) {
		boolean answer;
		try {
			Config.getExe( module, property );
			answer = true;
		}catch(Exception e) {
			answer = false;
		}
		return answer;
	}
	
	/**
	 * Check if a property has a value in a valid format.
	 * @param property
	 * @return null if the property is no recognized, true if the property has a valid value, false otherwise.
	 * @throws API_Exception
	 */
	public static Boolean isValidProp(String property) throws API_Exception {
		String type = getPropertyType( property );
		if (type == null) return null;
		
		boolean isgood = true;
		
		try {
			if ( type.startsWith( REQUIRED ) ) {
				type = type.replaceAll( REQUIRED, "" );
				if (Config.getString( null, property ) == null) return false;
			}
			
			switch (type) {
				case STRING_TYPE:
					Config.getString( null, property );
					break;
				case BOOLEAN_TYPE:
					Config.getBoolean( null, property );
					break;
				case FILE_PATH:
					Config.getExistingFileObject( Config.getString( null, property ) ) ;
					break;
				case FILE_PATH_LIST:
					List<String> paths = Config.getList( null, property );
					for (String path : paths) {
						Config.getExistingFileObject(path);
					}
					break;
				case LIST_TYPE:
					Config.getList( null, property );
					break;
				case EXE_PATH:
					Config.getExe( null, property );
					break;
				case INTEGER_TYPE:
					Config.getIntegerProp( null, property );
					break;
				case POS_INTEGER_TYPE:
					Config.getPositiveInteger( null, property );
					break;
				case NUMERTIC_TYPE:
					Config.getDoubleVal( null, property );
					break;
			}
		}catch(ConfigException ce) {
			isgood = false;
			System.err.print( ce.getMessage() );
		}catch(Exception ex) {
			isgood = false;
		}
		return isgood;
	}

	private static List<File> configRegister = new ArrayList<>();
	private static int loadOrder = -1;
	private static final long serialVersionUID = 2980376615128441545L;
	
	/**
	 *  Prefix for property types whose values cannot be null.
	 */
	public static final String REQUIRED = "required ";
	
	//Property Types
	public static final String STRING_TYPE = "string";
	public static final String BOOLEAN_TYPE = "boolean";
	public static final String FILE_PATH = "file path";
	public static final String EXE_PATH = "executable";
	public static final String LIST_TYPE = "list";
	public static final String FILE_PATH_LIST = "list of file paths";
	public static final String INTEGER_TYPE = "integer";
	public static final String POS_INTEGER_TYPE = "positive integer";
	public static final String NUMERTIC_TYPE = "numeric";
	public static final String[] KNOWN_TYPES = {STRING_TYPE, BOOLEAN_TYPE, FILE_PATH, EXE_PATH, LIST_TYPE, FILE_PATH_LIST, INTEGER_TYPE, NUMERTIC_TYPE};
}
