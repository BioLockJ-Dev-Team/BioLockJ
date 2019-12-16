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
import biolockj.exception.BioLockJException;
import biolockj.exception.ConfigPathException;
import biolockj.util.BioLockJUtil;
import biolockj.util.DockerUtil;

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

	private static List<String> getListedModules( final File file ) throws Exception {
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

	private static List<File> configRegister = new ArrayList<>();
	private static int loadOrder = -1;
	private static final long serialVersionUID = 2980376615128441545L;
}
