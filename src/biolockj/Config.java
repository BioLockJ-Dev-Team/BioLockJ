/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 16, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import biolockj.exception.*;
import biolockj.module.BioModule;
import biolockj.util.*;

/**
 * Provides type-safe, validated methods for storing/accessing system properties.<br>
 * Initially populated by the properties in the Config file, several additional properties are created and stored in the
 * the Config (to save system determined info such as: pipeline directory and name, has paired reads?, has multiplexed
 * reads?, etc.).
 */
public class Config {
	/**
	 * Parse property value (Y or N) to return boolean, if not found, return false;
	 *
	 * @param module Source BioModule calling this function
	 * @param property Property name
	 * @return boolean value
	 * @throws ConfigFormatException if property value is not null but also not Y or N.
	 */
	public static boolean getBoolean( final BioModule module, final String property ) throws ConfigFormatException {
		String value = getString( module, property );
		if ( value == null ) return false;
		else if ( value.equalsIgnoreCase( Constants.TRUE ) ) return true;
		else if ( value.equalsIgnoreCase( Constants.FALSE ) ) return false;
		throw new ConfigFormatException( property, "Boolean properties must be set to either " + Constants.TRUE +
			" or " + Constants.FALSE + "." );
	}

	/**
	 * Gets the configuration file extension (often ".properties")
	 *
	 * @return Config file extension
	 */
	public static String getConfigFileExt() {
		String ext = null;
		final StringTokenizer st = new StringTokenizer( configFile.getName(), "." );
		if( st.countTokens() > 1 ) while( st.hasMoreTokens() )
			ext = st.nextToken();

		return "." + ext;
	}

	/**
	 * Gets the full Config file path passed to BioLockJ as a runtime parameter.
	 *
	 * @return Config file path
	 */
	public static String getConfigFilePath() {
		return configFile.getAbsolutePath();
	}

	/**
	 * Parse property for numeric (double) value
	 * 
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return Double value or null
	 * @throws ConfigFormatException if property is defined, but set with a non-numeric value
	 */
	public static Double getDoubleVal( final BioModule module, final String property ) throws ConfigFormatException {
		if( getString( module, property ) != null ) try {
			final Double val = Double.parseDouble( getString( module, property ) );
			return val;
		} catch( final Exception ex ) {
			throw new ConfigFormatException( property, "Property only accepts numeric values: " + ex.getMessage() );
		}
		return null;
	}

	/**
	 * Get exe.* property name. If null, return the property name (without the "exe." prefix)
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return String value of executable
	 * @throws SpecialPropertiesException if property name does not start with "exe." or if other exceptions are encountered
	 */
	public static String getExe( final BioModule module, final String property ) throws SpecialPropertiesException {
		if( !property.startsWith( Constants.EXE_PREFIX ) ) throw new SpecialPropertiesException( property,
			"Config.getExe() can only be called for properties that begin with \"" + Constants.EXE_PREFIX + "\"" );
		String inContainerPath = null;
		String rawPath = getString( module, property );
		try {
			if( DockerUtil.inDockerEnv() ) {
				File hostFile = getExistingFile( module, property.replaceFirst( Constants.EXE_PREFIX, Constants.HOST_EXE_PREFIX ) );
				if (hostFile != null) inContainerPath = hostFile.getAbsolutePath();

				if( inContainerPath == null && rawPath != null ) {
					Log.warn( Config.class, "Unlike most properties, the \"" + Constants.EXE_PREFIX +
						"\" properties are not converted to an in-container path." );
					Log.warn( Config.class, "The exact string given will be used in scripts in a docker container." );
					Log.warn( Config.class,
						"To override this behavior, use the \"" + Constants.HOST_EXE_PREFIX + "\" prefix instead." );
				}
			}
		} catch( BioLockJException ex ) {
			throw new SpecialPropertiesException( property, ex );
		}
		// property name after trimming "exe." prefix, for example if exe.Rscript is undefined, return "Rscript"
		if( inContainerPath != null ) return inContainerPath;
		if( rawPath != null ) return rawPath;
		return property.replaceFirst( Constants.EXE_PREFIX, "" );
	}

	/**
	 * Call this function to get the parameters configured for this property.<br>
	 * Make sure the last character for non-null results is an empty character for use in bash scripts calling the
	 * corresponding executable.
	 * 
	 * @param module Calling module
	 * @param property exe parameter name
	 * @return Executable program parameters
	 * @throws Exception if errors occur
	 */
	public static String getExeParams( final BioModule module, final String property ) throws Exception {
		String val = getString( module, property );
		if( val == null ) return "";
		if( val != null && !val.isEmpty() && !val.endsWith( " " ) ) val = val + " ";
		return val;
	}

	/**
	 * Get a valid File directory or return null
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return File directory or null
	 * @throws ConfigPathException if path is defined but is not an existing file
	 * @throws DockerVolCreationException 
	 */
	public static File getExistingDir( final BioModule module, final String property ) throws ConfigPathException, DockerVolCreationException {
		final File f = getExistingFileObject( module, property );
		if( f != null && !f.isDirectory() ) throw new ConfigPathException( property, ConfigPathException.DIRECTORY );
		return f;
	}

	/**
	 * Get a valid File or return null. If path is a directory containing exactly 1 file, return that file.
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return File (not directory) or null
	 * @throws ConfigPathException if path is defined but is not an existing file
	 * @throws DockerVolCreationException 
	 */
	public static File getExistingFile( final BioModule module, final String property ) throws ConfigPathException, DockerVolCreationException {
		File f = getExistingFileObject( module, property );
		if( f != null && !f.isFile() ) if( f.isDirectory() && f.list( HiddenFileFilter.VISIBLE ).length == 1 ) {
			Log.warn( Config.class,
				property + " is a directory with only 1 valid file.  Return the lone file within." );
			f = new File( f.list( HiddenFileFilter.VISIBLE )[ 0 ] );
		} else throw new ConfigPathException( property, ConfigPathException.FILE );

		return f;
	}

	/**
	 * Get initial properties ordered by property
	 *
	 * @return map ordered by property
	 */
	public static TreeMap<String, String> getInitialProperties() {
		return convertToMap( unmodifiedInputProps );
	}

	/**
	 * Parse comma delimited property value to return list
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return List of String values (or an empty list)
	 */
	public static List<String> getList( final BioModule module, final String property ) {
		final List<String> list = new ArrayList<>();
		final String val = getString( module, property );
		if( val != null ) {
			final StringTokenizer st = new StringTokenizer( val, "," );
			while( st.hasMoreTokens() )
				list.add( st.nextToken().trim() );
		}

		return list;
	}

	/**
	 * Return file for path after modifying if running in a Docker container and/or interpreting bash env vars.
	 * 
	 * @param path File path
	 * @return Local File
	 * @throws ConfigPathException if the local path
	 * @throws DockerVolCreationException 
	 */
	public static File getLocalConfigFile( final String path ) throws ConfigPathException, DockerVolCreationException {
		if( path == null || path.trim().isEmpty() ) return null;
		String filePath = replaceEnvVar( path.trim() );
		if (DockerUtil.inDockerEnv()) filePath = DockerUtil.containerizePath( filePath );
		final File file = new File( filePath );
		return file;
	}

	/**
	 * Return property name after substituting the module name as its prefix.
	 * Give first priority to a property that uses the module's alias, then one that uses the module name.
	 * If both of those are null, then use the property as given.
	 * 
	 * @param module BioModule
	 * @param property Property name
	 * @return BioModule specific property name
	 */
	public static String getModulePropName( final BioModule module, final String property ) {
		if( module != null ) {
			String aliasProp = getModuleFormProp( module, property );
			if( aliasProp != null && props.getProperty( aliasProp ) != null ) {
				Log.debug( Class.class, "Looking for property [" + property + "], found overriding module-specific form: [" + aliasProp + "]." );
				return aliasProp;
			}
		}
		return property;
	}
	
	public static String getModuleFormProp(final BioModule module, final String property) {
		return ModuleUtil.displayName( module ) + "." + suffix( property );
	}

	/**
	 * Parse property as non-negative integer value
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return Non-negative integer or null
	 * @throws ConfigFormatException if defined but is not a non-negative integer value
	 */
	public static Integer getNonNegativeInteger( final BioModule module, final String property )
		throws ConfigFormatException {
		final Integer val = getIntegerProp( module, property );
		if( val != null && val < 0 )
			throw new ConfigFormatException( property, "Property only accepts non-negative integer values" );
		return val;
	}

	/**
	 * Get the pipeline directory if it is a valid directory on the file system.
	 * 
	 * @return Pipeline directory (if it exists)
	 * @deprecated Use {@link BioLockJ#getPipelineDir()} instead
	 */
	public static File getPipelineDir() {
		return BioLockJ.getPipelineDir();
	}

	/**
	 * Parse property as positive double value
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return Positive Double value or null
	 * @throws ConfigFormatException if property is defined, but not set with a positive number
	 */
	public static Double getPositiveDoubleVal( final BioModule module, final String property )
		throws ConfigFormatException {
		final Double val = getDoubleVal( module, property );
		if( val != null && val <= 0 )
			throw new ConfigFormatException( property, "Property only accepts positive numeric values" );

		return val;
	}

	/**
	 * Parse property as positive integer value
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return Positive Integer value or null
	 * @throws ConfigFormatException if property is defined, but not set with a positive integer
	 */
	public static Integer getPositiveInteger( final BioModule module, final String property )
		throws ConfigFormatException {
		final Integer val = getIntegerProp( module, property );
		if( val != null && val <= 0 )
			throw new ConfigFormatException( property, "Property only accepts positive integer values" );
		return val;
	}

	/**
	 * Get current properties ordered by property
	 *
	 * @return map ordered by property
	 */
	public static TreeMap<String, String> getProperties() {
		return convertToMap( props );
	}

	/**
	 * Parse comma-separated property value to build an unordered Set
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return Set of values or an empty set (if no values)
	 */
	public static Set<String> getSet( final BioModule module, final String property ) {
		final Set<String> set = new HashSet<>();
		set.addAll( getList( module, property ) );
		return set;
	}

	/**
	 * Get property value as String. Empty strings return null.<br>
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property {@link biolockj.Config} file property name
	 * @return String or null
	 */
	public static String getString( final BioModule module, final String property, final String defaultVal ) {
		if( props == null ) return null;
		String prop = getModulePropName( module, property );
		String val = props.getProperty( prop, defaultVal );
		if ( val == null && module != null) {
			val=module.getPropDefault( prop );
			if (val != null) {
				Log.info(Config.class, "Setting property [" + prop + "] to [" 
								+ val + "], the default value supplied by my module: " + ModuleUtil.displaySignature( module ) + ".");
				props.setProperty( prop, val );
			}
		}
		if( val != null ) val = val.trim();
		val = replaceEnvVar( val );
		if( val != null && val.isEmpty() ) val = null;
		moduleUsedProps.put( prop, val );
		return val;
	}
	public static String getString( final BioModule module, final String property ) {
		return getString( module, property, null );
	}

	/**
	 * Parse comma-separated property value to build an ordered Set
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return Set of values or an empty set (if no values)
	 */
	public static Set<String> getTreeSet( final BioModule module, final String property ) {
		final Set<String> set = new TreeSet<>();
		set.addAll( getList( module, property ) );
		return set;
	}

	/**
	 * Cache of the properties used in this pipeline.
	 * 
	 * @return list of properties
	 */
	public static Map<String, String> getUsedProps() {
		getString( null, Constants.PIPELINE_DEFAULT_PROPS );
		allUsedProps.putAll( moduleUsedProps );
		return new HashMap<>( allUsedProps );
	}

	/**
	 * Initialize {@link biolockj.Config} by reading in properties from config runtime parameter. Save a copy of the
	 * primary Config to the pipeline root directory
	 *
	 * @throws Exception if unable to load Props
	 */
	public static void initialize() throws Exception {
		configFile = RuntimeParamUtil.getConfigFile();
		Log.info( Config.class, "Initialize Config: " + configFile.getAbsolutePath() );
		//props = replaceEnvVars( Properties.loadProperties( configFile ) );
		props = Properties.loadProperties( configFile );
		Properties tempProps = replaceEnvVars( props );
		props = tempProps;
		setFilePathProperty( Constants.INTERNAL_PIPELINE_DIR, BioLockJ.getPipelineDir().getAbsolutePath() );
		setConfigProperty( Constants.INTERNAL_PIPELINE_NAME, BioLockJ.getProjectName() );
		setConfigProperty( Constants.INTERNAL_PIPELINE_ID, BioLockJ.getPipelineId() );
		if( !BioLockJUtil.isDirectMode() && !FileUtils.directoryContains( BioLockJ.getPipelineDir(), configFile ) )
			FileUtils.copyFileToDirectory( configFile, BioLockJ.getPipelineDir() );
		Log.info( Config.class, "Total # initial properties: " + props.size() );
		unmodifiedInputProps.putAll( props );
		TaxaUtil.initTaxaLevels();
	}
	
	/**
	 * Allow the system to act with no properties to allow for quick testing of individual props.
	 * @param prop
	 * @param val
	 * @throws Exception
	 */
	public static void initBlankProps() throws Exception {
		props = new Properties();
	}
	
	public static void partiallyInitialize(File tempConfig) throws Exception {
		configFile = tempConfig;
		props = Properties.loadProperties( tempConfig );
		Properties tempProps = replaceEnvVars( props );
		props = tempProps;
	}

	/**
	 * Check if running on cluster
	 * 
	 * @return TRUE if running on the cluster
	 */
	public static boolean isOnCluster() {
		return getString( null, Constants.PIPELINE_ENV ) != null &&
			getString( null, Constants.PIPELINE_ENV ).equals( Constants.PIPELINE_ENV_CLUSTER );
	}

	/**
	 * Get the current pipeline name (root folder name)
	 * 
	 * @return Pipeline name
	 */
	public static String pipelineName() {
		if( BioLockJ.getPipelineDir() == null ) return null;
		return BioLockJ.getPipelineDir().getName();
	}

	/**
	 * Get the current pipeline absolute directory path (root folder path)
	 * 
	 * @return Pipeline directory path
	 */
	public static String pipelinePath() {
		if ( BioLockJ.getPipelineDir() == null ) return null;
		return BioLockJ.getPipelineDir().getAbsolutePath();
	}

	/**
	 * Interpret environment variable if included in the arg string, otherwise return the arg.
	 * Variables are recognized if they are surrounded by ${ and }.  
	 * For example: "dee ${VAR} da" becomes "fee doo da" IFF there is a variable "VAR" 
	 * that is defined as "doo" either in the runtime environment, or in the config file.
	 * The special character "~" ("tilda") is changed to "${HOME}" and then processed IFF 
	 * it is the first character in the string after removing any flanking white space.
	 * 
	 * @param arg Property or runtime argument
	 * @return Updated arg value after replacing env variables
	 */
	public static String replaceEnvVar( final String arg ) {
		if( arg == null ) return null;
		String val = arg.toString().trim();
		if( !hasEnvVar( val ) ) return val;
		if( val.substring( 0, 1 ).equals( "~" ) ) {
			Log.debug( Config.class, "Found property value starting with \"~\" --> \"" + arg + "\"" );
			val = val.replace( "~", "${HOME}" );
			Log.debug( Config.class, "Converted value to use standard syntax --> " + val + "\"" );
		}
		try {
			while( hasEnvVar( val ) ) {
				final String bashVar = val.substring( val.indexOf( "${" ), val.indexOf( "}" ) + 1 );
				Log.debug( Config.class, "Replace \"" + bashVar + "\" in \"" + arg + "\"" );
				final String bashVal = getEnvVarVal( bashVar );
				Log.debug( Config.class, "Bash var \"" + bashVar + "\" = \"" + bashVal + "\"" );
				if( bashVal.equals( bashVar ) ) return arg;
				val = val.replace( bashVar, bashVal );
				Log.debug( Config.class, "Updated \"" + arg + "\" --> " + val + "\"" );
			}
			Log.info( Config.class, "--------> Bash Var Converted \"" + arg + "\" ======> \"" + val + "\"" );
			return val;
		} catch( final Exception ex ) {
			Log.warn( Config.class, "Failed to convert arg \"" + arg + "\"" + ex.getMessage() );
		}
		Log.warn( Config.class, "Return unchanged value \"" + arg + "\"" );
		return arg;
	}

	/**
	 * Required to return a valid boolean {@value Constants#TRUE} or {@value Constants#FALSE}
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return boolean {@value Constants#TRUE} or {@value Constants#FALSE}
	 * @throws ConfigNotFoundException if propertyName is undefined
	 * @throws ConfigFormatException if property is defined, but not set to a boolean value
	 */
	public static boolean requireBoolean( final BioModule module, final String property )
		throws ConfigNotFoundException, ConfigFormatException {
		requireString( module, property );
		return (getBoolean( module, property ));
	}

	/**
	 * Requires valid double value
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return Double value
	 * @throws ConfigNotFoundException if property is undefined
	 * @throws ConfigFormatException if property is defined, but set with a non-numeric value
	 */
	public static Double requireDoubleVal( final BioModule module, final String property )
		throws ConfigNotFoundException, ConfigFormatException {
		final Double val = getDoubleVal( module, property );
		if( val == null ) throw new ConfigNotFoundException( property );

		return val;
	}

	/**
	 * Requires valid existing directory.
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return File directory
	 * @throws ConfigPathException if path is defined but is not an existing file
	 * @throws ConfigNotFoundException if property is undefined
	 * @throws DockerVolCreationException 
	 */
	public static File requireExistingDir( final BioModule module, final String property )
		throws ConfigPathException, ConfigNotFoundException, DockerVolCreationException {
		final File f = getExistingDir( module, property );
		if( f == null ) throw new ConfigNotFoundException( property );

		return f;
	}

	/**
	 * Requires valid list of file directories
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return List of File directories
	 * @throws ConfigPathException if directory paths are undefined or do not exist
	 * @throws ConfigNotFoundException if a required property is undefined
	 * @throws DockerVolCreationException 
	 */
	public static List<File> requireExistingDirs( final BioModule module, final String property )
		throws ConfigPathException, ConfigNotFoundException, DockerVolCreationException {
		final List<File> returnDirs = new ArrayList<>();
		for( final String d: requireSet( module, property ) ) {
			final File dir = getExistingFileObject( module, property );
			if( dir != null && !dir.isDirectory() )
				throw new ConfigPathException( property, ConfigPathException.DIRECTORY );

			returnDirs.add( dir );
		}

		if( !returnDirs.isEmpty() ) Config.setConfigProperty( property, returnDirs );
		return returnDirs;
	}

	/**
	 * Require valid existing file
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return File with filename defined by property
	 * @throws ConfigPathException if path is defined but is not an existing file
	 * @throws ConfigNotFoundException if property is undefined
	 * @throws DockerVolCreationException 
	 */
	public static File requireExistingFile( final BioModule module, final String property )
		throws ConfigPathException, ConfigNotFoundException, DockerVolCreationException {
		final File f = getExistingFile( module, property );
		if( f == null ) throw new ConfigNotFoundException( property );
		return f;
	}

	/**
	 * Requires valid integer value
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return Integer value
	 * @throws ConfigNotFoundException if property is undefined
	 * @throws ConfigFormatException if property is not a valid integer
	 */
	public static Integer requireInteger( final BioModule module, final String property )
		throws ConfigNotFoundException, ConfigFormatException {
		final Integer val = getIntegerProp( module, property );
		if( val == null ) throw new ConfigNotFoundException( property );

		return val;
	}

	/**
	 * Require valid list property
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return List
	 * @throws ConfigNotFoundException if property is undefined
	 */
	public static List<String> requireList( final BioModule module, final String property )
		throws ConfigNotFoundException {
		final List<String> val = getList( module, property );
		if( val == null || val.isEmpty() ) throw new ConfigNotFoundException( property );
		return val;
	}

	/**
	 * Require valid positive double value
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return Positive Double
	 * @throws ConfigNotFoundException if property is undefined
	 * @throws ConfigFormatException if property is defined, but not set to a positive numeric value
	 */
	public static Double requirePositiveDouble( final BioModule module, final String property )
		throws ConfigNotFoundException, ConfigFormatException {
		final Double val = requireDoubleVal( module, property );
		if( val <= 0 ) throw new ConfigFormatException( property, "Property only accepts positive numeric values" );

		return val;
	}

	/**
	 * Require valid positive integer value
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return Positive Integer
	 * @throws ConfigNotFoundException if property is undefined
	 * @throws ConfigFormatException if property is defined, but not set to a positive integer value
	 */
	public static Integer requirePositiveInteger( final BioModule module, final String property )
		throws ConfigNotFoundException, ConfigFormatException {
		final Integer val = requireInteger( module, property );
		if( val <= 0 ) throw new ConfigFormatException( property, "Property only accepts positive integers" );
		return val;
	}

	/**
	 * Require valid Set value
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return Set of values
	 * @throws ConfigNotFoundException if property is undefined
	 */
	public static Set<String> requireSet( final BioModule module, final String property )
		throws ConfigNotFoundException {
		final Set<String> val = getTreeSet( module, property );
		if( val == null || val.isEmpty() ) throw new ConfigNotFoundException( property );
		return val;
	}

	/**
	 * Require valid String value
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return String value
	 * @throws ConfigNotFoundException if property is undefined
	 */
	public static String requireString( final BioModule module, final String property ) throws ConfigNotFoundException {
		if( getString( module, property ) == null ) throw new ConfigNotFoundException( property );

		return getString( module, property ).trim();
	}

	/**
	 * Sets a property value in the props cache as a list
	 *
	 * @param name Property name
	 * @param data Collection of data to store using the key = property
	 * @param module if module is not null, and the props already include a module-specific form of this property, save the new value to the module specific form.
	 * @throws DockerVolCreationException 
	 */
	public static void setConfigProperty( final String name, final Collection<?> data, final BioModule module ) throws DockerVolCreationException {
		String val = null;
		if( data != null && !data.isEmpty() && data.iterator().next() instanceof File ) {
			final Collection<String> fileData = new ArrayList<>();
			for( final Object obj: data ) {
				String path = ( (File) obj ).getAbsolutePath();
				path = DockerUtil.deContainerizePath( path );
				fileData.add( path );
			}
			val = BioLockJUtil.getCollectionAsString( fileData );
		} else val = BioLockJUtil.getCollectionAsString( data );

		setConfigProperty(name, val, module);
	}
	/**
	 * Sets a property value in the props cache as a list
	 *
	 * @param name Property name
	 * @param data Collection of data to store using the key = property
	 * @throws DockerVolCreationException 
	 */
	public static void setConfigProperty( final String name, final Collection<?> data ) throws DockerVolCreationException {
		setConfigProperty(name, data, null);
	}

	public static void setFilePathProperty(final String name, final String val, final BioModule module) throws DockerVolCreationException {
		String path = DockerUtil.deContainerizePath( val );
		setConfigProperty(name, path, module);
	}
	public static void setFilePathProperty(final String name, final String val) throws DockerVolCreationException {
		setFilePathProperty(name, val, null);
	}
	
	/**
	 * Sets a property value in the props cache
	 *
	 * @param name Property name
	 * @param val Value to assign to property
	 * @param module if module is not null, and the props already include a module-specific form of this property, save the new value to the module specific form.
	 */
	public static void setConfigProperty( final String genericName, final String val, final BioModule module ) {
		String name = getModulePropName(module, genericName);
		if ( !genericName.equals( name )) Log.debug(Config.class, "Saving value to property [" + name + "] in place of [" + genericName + "].");
		
		String origProp = (String) props.get( name );
		if (origProp != null && origProp.isEmpty() ) origProp = null;		
		
		if ( origProp == null && val == null) {}
		else if ( origProp != null && val != null && origProp.equals( val ) ) 
			Log.debug( Config.class, "Set config property [ " + name + " ] = " + val + "; already set to this value");
		else if ( val == null) 
			Log.info(Config.class, "Overwriting [" + name + "=" + origProp + "] to set config property [" + name + "=]");
		else if ( origProp != null ) 
			Log.info(Config.class, "Overwriting [" + name + "=" + origProp + "] to set config property [" + name + "=" + val + "]");
		else  Log.info( Config.class, "Set config property [ " + name + " ] = " + val );
		
		moduleUsedProps.put( name, val );
		props.setProperty( name, val );
	}
	/**
	 * Sets a property value in the props cache
	 *
	 * @param name Property name
	 * @param val Value to assign to property
	 */
	public static void setConfigProperty( final String name, final String val ) {
		setConfigProperty(name, val, null);
	}

	/**
	 * Build File using filePath.
	 *
	 * @param filePath File path
	 * @return File or null
	 * @throws ConfigPathException if path is defined but is not found on the file system
	 * @throws DockerVolCreationException 
	 */
	public static File getFileObjectFromPath( final String filePath, boolean convertRelativePath, boolean containerizePath ) throws DockerVolCreationException {
		if( filePath != null ) {
			String path  = filePath;
			if (convertRelativePath) path = convertRelativePath( path );
			if (containerizePath) path = DockerUtil.containerizePath( path );
			Log.debug(Config.class, "The file path \"" + filePath + "\" has been internally interpreted as: " + path );
			final File f = new File( path );
			return f;
		}
		return null;
	}
	/**
	 * Build File using filePath.
	 *
	 * @param filePath File path
	 * @return File or null
	 * @throws ConfigPathException if path is defined but is not found on the file system
	 * @throws DockerVolCreationException 
	 */
	public static File getExistingFileObjectFromPath( final String filePath ) throws ConfigPathException, DockerVolCreationException {
		return getExistingFileObjectFromPath( filePath, true, true );
	}
	/**
	 * Build File using filePath.
	 * In some internal cases, it may be best to avoid converting relative paths, or to the docker conversion.
	 * If the path is null, return null. If the path does not lead to a valid file, throw exception.
	 * 
	 * @param filePath
	 * @param convertRelativePath - should the "./" be converted to be relative to the config file
	 * @param containerizePath - should this be converted from host-path form to in-docker-container form.
	 * @return
	 * @throws ConfigPathException
	 * @throws DockerVolCreationException
	 */
	public static File getExistingFileObjectFromPath( final String filePath, boolean convertRelativePath, boolean containerizePath ) throws ConfigPathException, DockerVolCreationException {
		if( filePath != null ) {
			File f = getFileObjectFromPath(filePath, convertRelativePath, containerizePath);
			if( f.exists() ) return f;
			else throw new ConfigPathException( f );
		}
		return null;
	}
	
	/**
	 * Build File using module and property name.
	 *
	 * @param module A BioModule, can be null
	 * @param property A string property name.
	 * @return File or null
	 * @throws ConfigPathException if path is defined but is not found on the file system
	 * @throws DockerVolCreationException 
	 */
	public static File getExistingFileObject( final BioModule module, final String property ) throws ConfigPathException, DockerVolCreationException {
		final File f = getExistingFileObjectFromPath( getString( module, property ) );
		if( props != null && f != null ) setConfigProperty( property, f.getAbsolutePath(), module );
		return f;
	}
	
	public static List<File> getExistingFileList( final BioModule module, final String property ) throws ConfigPathException, DockerVolCreationException {
		if ( Config.getString( module, property ) == null ) return null;
		List<File> files = new ArrayList<>();
		List<String> paths = getList( module, property );
		for (String path : paths ) {
			File file = getExistingFileObjectFromPath( path );
			files.add(file);
		}
		if( props != null && !files.isEmpty() ) setConfigProperty( property, files, module );
		return files;
	}
	
	public static String convertWindowsPathForDocker(final String path) {
		String newpath;
		if (DockerUtil.inDockerEnv()
						&& Character.isUpperCase(path.charAt(0)) 
						&& path.startsWith( ":", 1 ) 
						&& path.contains( "\\" ) ) {
			newpath = "/host_mnt/" + Character.toLowerCase( path.charAt( 0 ) ) + path.substring( 2 ).replace( "\\", "/" );
		}else {
			newpath = path;
		}
		return newpath;
	}
	
	/**
	 * Given a relative path (ie, one that starts with "."), get the absolute path---even when runnig in docker.
	 * In the config file, users can use ./ to reference files in the same directory as the config file.
	 * And by extension, "../" can reference the parent of that directory.
	 * We tell users to use host paths in their config file, so when we replace the . or .., 
	 * it is important that replace it with the HOST path to the config file directory.  
	 * @param filePath
	 * @return
	 * @throws DockerVolCreationException
	 */
	public static String convertRelativePath(final String filePath, final String CONFIG_DOT) throws DockerVolCreationException {
		String path = filePath;
		if ( path.startsWith(".") ) {
			final String CONFIG_DOT_DOT=(new File(CONFIG_DOT)).getParent();
			if ( path.startsWith( ".." + File.separator ) ) path = path.replaceFirst( "..", CONFIG_DOT_DOT);
			if ( path.equals( ".." ) ) path = CONFIG_DOT_DOT;
			if ( path.startsWith( "." + File.separator ) ) path = path.replaceFirst( ".", CONFIG_DOT);
			if ( path.equals( "." ) ) path = CONFIG_DOT;
		}
		return path;
	}
	private static String convertRelativePath(final String filePath) throws DockerVolCreationException {
		final String CONFIG_DOT=DockerUtil.deContainerizePath( configFile.getParent() );
		return convertRelativePath(filePath, CONFIG_DOT);
	}

	/**
	 * Interpret env variables defined in the Config file and runtime env - for example<br>
	 * These props are used in: $BLJ/resources/config/defult/docker.properties:<br>
	 * <ul>
	 * <li>BLJ_ROOT=/mnt/efs
	 * <li>EFS_DB=${BLJ_ROOT}/db
	 * <li>humann2.protDB=${EFS_DB}/uniref
	 * </ul>
	 * Therefore, getString( "humann2.protDB" ) returns "/mnt/efs/db/uniref"<br>
	 * If not found, check runtiem env (i.e., $HOME/bash_profile)
	 * 
	 * @param properties All Config Properties
	 * @return Properties after replacing env variables
	 */
	protected static Properties replaceEnvVars( final Properties properties ) {
		final Properties convertedProps = properties;
		final Enumeration<?> en = properties.propertyNames();
		Log.debug( Config.class, " ---------------------- replace Config Env Vars ----------------------" );
		while( en.hasMoreElements() ) {
			final String key = en.nextElement().toString();
			String val = properties.getProperty( key );
			val = replaceEnvVar( val );
			Log.debug( Config.class, key + " = " + val );
			convertedProps.put( key, val );
		}
		Log.debug( Config.class, " --------------------------------------------------------------------" );
		return convertedProps;
	}

	private static TreeMap<String, String> convertToMap( final Properties bljProps ) {
		final TreeMap<String, String> map = new TreeMap<>();
		final Iterator<String> it = bljProps.stringPropertyNames().iterator();
		while( it.hasNext() ) {
			final String key = it.next();
			map.put( key, bljProps.getProperty( key ) );
		}
		return map;
	}

	private static String getEnvVarVal( final String dressedBashVar ) throws ConfigNotFoundException {
		final String bashVar = stripBashMarkUp( dressedBashVar );
		
		String bashVal = null;
		
		bashVal = replaceEnvVarFromBuiltIns(bashVar);
		addEnvVarToMap(bashVar, bashVal);
		
		if( bashVal == null ) bashVal = replaceEnvVarFromProps(bashVar);
		if( bashVal != null ) return bashVal;
		
		if( envVarMap.get( bashVar ) != null ) {
			return envVarMap.get( bashVar );
		}
		
		if( bashVal == null ) bashVal = replaceEnvVarFromEnvironment(bashVar);
		addEnvVarToMap(bashVar, bashVal);
				
		return bashVal;
	}
	
	private static void addEnvVarToMap(String key, String value) throws ConfigNotFoundException {
		if( value != null && !value.trim().isEmpty() ) {
			envVarMap.put( key, value );
			moduleUsedProps.put( key, value );
		}
	}
	

	private static String replaceEnvVarFromBuiltIns(final String bashVar) throws ConfigNotFoundException {
		String bashVal = null;
		try {
			if( bashVar.equals( BLJ_BASH_VAR ) ) {
				File dir = BioLockJUtil.getBljDir();
				if( dir != null && dir.isDirectory() ) {
					bashVal = dir.getAbsolutePath();
				}
			} else if( bashVar.equals( BLJ_PROJ_VAR ) ) {
				final File dir = RuntimeParamUtil.get_BLJ_PROJ();
				if( dir != null && dir.isDirectory() ) {
					bashVal = dir.getAbsolutePath();
				}
			}
		} catch( ConfigPathException | DockerVolCreationException cpe ) {
			Log.warn( Config.class, "Error occurred attempting to decode built-in environment variable: " + bashVar +
				" --> " + cpe.getMessage() );
			cpe.printStackTrace();
		}
		return bashVal;
	}
	
	private static String replaceEnvVarFromProps(final String bashVar) {
		String bashVal = null;
		if( props == null ) {
			Log.info( Config.class, "no props to reference." );
		}
		if( props != null ) {
			Log.info( Config.class, "Got props, value for [" + bashVar + "] is: " + props.getProperty( bashVar ) );
		}
		if( props != null && props.getProperty( bashVar ) != null ) {
			bashVal = props.getProperty( bashVar );
			moduleUsedProps.put( bashVar, bashVal );

		}
		return bashVal;
	}
	
	private static String replaceEnvVarFromEnvironment(final String bashVar) throws ConfigNotFoundException {
		String bashVal = null;
		boolean useEnvVars = true;
		try {
			if (props != null) useEnvVars = getBoolean( null, Constants.PIPELINE_USE_EVARS );
		} catch( ConfigFormatException e1 ) {
			//e1.printStackTrace();
			// up until the properties are read, variables WILL access environment variables, even if this is set to false.
		}		
		if ( useEnvVars ) {
			try {
				bashVal = Processor.getBashVar( bashVar );
			} catch( final Exception ex ) {
				Log.warn( Config.class,
					"An unexpected error occurred attempting to decode environment var: " + bashVar + " --> " + ex.getMessage() );
			}
		}else{
			Log.info( Config.class, "The variable [" + bashVar +
				"] is not defined in the config file, and the use of local environment variables has been disabbled.  Set [ " +
				Constants.PIPELINE_USE_EVARS + "=" + Constants.TRUE +
				" ] to enable access to local environment variables." );
		}
		return bashVal;
	}

	/**
	 * Parse property value as integer
	 *
	 * @param property Property name
	 * @return integer value or null
	 * @throws ConfigFormatException if property is defined, but does not return an integer
	 */
	public static Integer getIntegerProp( final BioModule module, final String property )
		throws ConfigFormatException {
		if( getString( module, property ) != null ) try {
			final Integer val = Integer.parseInt( getString( module, property ) );
			return val;
		} catch( final Exception ex ) {
			throw new ConfigFormatException( property, "Property only accepts integer values: " + ex.getMessage() );
		}

		return null;
	}

	private static boolean hasEnvVar( final String val ) {
		return val.startsWith( "~/" ) ||
			val.contains( "${" ) && val.contains( "}" ) && val.indexOf( "${" ) < val.indexOf( "}" );
	}

	private static String stripBashMarkUp( final String bashVar ) {
		if( bashVar != null && bashVar.startsWith( "${" ) && bashVar.endsWith( "}" ) ) {
			return bashVar.substring( 2, bashVar.length() - 1 ); 
		}
		return bashVar;
	}

	private static String suffix( final String prop ) {
		return prop.indexOf( "." ) > -1 ? prop.substring( prop.indexOf( "." ) + 1 ): prop;
	}
	
	public static boolean isInternalProperty( final String property ) {
		return property.startsWith( Constants.INTERNAL_PREFIX );
	}
	
	/**
	 * Dump all of the properties stored for the current module into the allUsedProps set,
	 * and clear out the module-used-props to start with a clean slate.
	 */
	public static void resetUsedProps() {
		allUsedProps.putAll( moduleUsedProps );
		moduleUsedProps.clear();
	}
	
	public static void saveModuleProps( BioModule module ) throws IOException {
		File modConfig = new File(module.getLogDir(), ModuleUtil.displayName( module ) + USED_PROPS_SUFFIX);
		BufferedWriter writer = new BufferedWriter( new FileWriter( modConfig ) );
		try {
			writer.write( "# Properties used during the execution of module: " + ModuleUtil.displaySignature( module ) + Constants.RETURN);
			for( final String key: moduleUsedProps.keySet() )
				if (moduleUsedProps.get( key ) != null) {
					writer.write( key + "=" + moduleUsedProps.get( key ) + Constants.RETURN );
				}
		}finally {
			writer.close();
		}
	}
	
	public static void showUnusedProps() throws FileNotFoundException, IOException {
		allUsedProps.putAll( moduleUsedProps );
		Properties props = new Properties();
		Log.info(Config.class, "Path to configFile: " + configFile.getAbsolutePath());
		props.load( new FileInputStream( configFile) );
		Map<String, String> primaryProps = convertToMap( props );
		primaryProps.keySet().removeAll( allUsedProps.keySet() );
		Set<String> keys = new HashSet<>( primaryProps.keySet() );
		for ( String prop : keys ) {
			if ( primaryProps.get( prop ) == null 
							|| primaryProps.get( prop ).isEmpty() 
							|| prop.equals( Constants.PIPELINE_DEFAULT_PROPS )) {
				primaryProps.remove( prop );
			}
		}
		if( !primaryProps.isEmpty() ) {
			BufferedWriter writer =
				new BufferedWriter( new FileWriter( new File( BioLockJ.getPipelineDir(), Constants.UNVERIFIED_PROPS_FILE ) ) );
			try {
				String msg = "Properties from the PRIMARY config file that were NOT USED during check-dependencies:";
				Log.warn( Config.class, msg );
				writer.write( "### " + msg + Constants.RETURN + "#" + Constants.RETURN );
				for( final String prop: primaryProps.keySet() ) {
					if( Properties.isDeprecatedProp( prop ) ) {
						Log.warn( Config.class, "      " + Properties.deprecatedPropMessage( prop ) );
						writer.write( "# " + Properties.deprecatedPropMessage( prop ) + Constants.RETURN );
					}
					Log.warn( Config.class, "      " + prop + "=" + primaryProps.get( prop ) );
					writer.write( prop + "=" + primaryProps.get( prop ) + Constants.RETURN );
				}
			} finally {
				writer.close();
			}
		}
	}
	
	/**
	 * Return the String, String map of environment variables and their values.
	 * This map will only include variables that are referenced in the config file using the ${VAR} format.
	 * @return
	 * @throws ConfigNotFoundException 
	 */
	public static Map<String, String> getEnvVarMap() throws ConfigNotFoundException {
		syncEnvVarsWithProps();
		return envVarMap;
	}
	
	private static void syncEnvVarsWithProps() throws ConfigNotFoundException {
		if( props != null ) {
			List<String> vars = getList( null, Constants.PIPELINE_ENV_VARS );
			for( String var: vars ) {
				String val = getEnvVarVal( var );
				if( val == null ) throw new ConfigNotFoundException( var, "This pipeline expects that a variable [" +
					var +
					"] will be set as an environment variable; but this variable has no value in the current context." +
					System.lineSeparator() + "If this variable is required, define it in the configuration file:  " +
					var + "=<some value>" + System.lineSeparator() +
					"If this variable is not required, update the values listed for [" + Constants.PIPELINE_ENV_VARS +
					"]." );
				envVarMap.put( var, val );
			}
			List<String> allVars = new ArrayList<String>();
			allVars.addAll( envVarMap.keySet() );
			if( props != null ) for( String var: allVars )
				props.put( var, envVarMap.get( var ) );
			try {
				setConfigProperty( Constants.PIPELINE_ENV_VARS, allVars );
			} catch( DockerVolCreationException e ) {
				// These are strings, not files will never need to convert file path; don't create noise in throws
				// declarations.
				e.printStackTrace();
			}
		}
	}
	
	public static void checkDependencies( BioModule module ) throws ConfigNotFoundException, ConfigFormatException   {
		getBoolean( module, Constants.PIPELINE_USE_EVARS );
		Config.getPositiveInteger( module, Constants.SCRIPT_DELAY_FOR_FILE_UPDATES );
		getEnvVarMap();
	}

	public static final String BLJ_BASH_VAR = "BLJ";
	
	public static final String BLJ_PROJ_VAR = "BLJ_PROJ";
	
	private static final Map<String, String> envVarMap = new HashMap<>();
	private static File configFile = null;
	static Properties props = null;
	private static Properties unmodifiedInputProps = new Properties();
	private static final Map<String, String> allUsedProps = new HashMap<>();
	private static final Map<String, String> moduleUsedProps = new HashMap<>();
	private static final String USED_PROPS_SUFFIX = "_used.properties";
	private static final String UNUSED_PROPS_FILE = "unused.properties";
	
}

