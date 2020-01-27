/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Jul 31, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.util;

import java.io.File;
import java.io.IOException;
import java.util.*;
import biolockj.*;
import biolockj.exception.BioLockJException;
import biolockj.exception.DockerVolCreationException;
import biolockj.exception.RuntimeParamException;
import biolockj.module.JavaModule;

/**
 * This utility processes the application runtime parameters passed to BioLockJ.
 */
public class RuntimeParamUtil {

	/**
	 * Return TRUE if runtime parameters indicate change password request
	 * 
	 * @return boolean
	 */
	public static boolean doChangePassword() {
		return params.get( PASSWORD ) != null;
	}

	/**
	 * Get a File that is given through a parameter; if in docker, get the in-container form of the path
	 * @param paramName - String key to use with params
	 * @return
	 * @throws DockerVolCreationException 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static File getFileParam(String paramName, boolean dockerize) throws DockerVolCreationException{
		String filePath = params.get( paramName );
		if ( dockerize && DockerUtil.inDockerEnv() ) 
			filePath = DockerUtil.containerizePath( filePath );
		return filePath == null ? null: new File( filePath );
	}
	
	/**
	 * Return TRUE if runtime parameters indicate attempt to restart pipeline
	 * 
	 * @return boolean
	 * @throws DockerVolCreationException 
	 */
	public static boolean doRestart() throws DockerVolCreationException {
		return getRestartDir() != null;
	}

	/**
	 * Runtime property getter for {@value #BLJ_PROJ_DIR}
	 * 
	 * @return $BLJ_PROJ_DIR pipeline parent directory
	 * @throws DockerVolCreationException 
	 */
	public static File get_BLJ_PROJ() throws DockerVolCreationException {
		return get_BLJ_PROJ( true );
	}
	public static File get_BLJ_PROJ(boolean dockerize) throws DockerVolCreationException {
		if (dockerize && DockerUtil.inDockerEnv()) return new File(DockerUtil.DOCKER_PIPELINE_DIR);
		return getFileParam( BLJ_PROJ_DIR, false );
	}

	/**
	 * Runtime property getter for {@value #PASSWORD}
	 * 
	 * @return New clear-text password
	 */
	public static String getAdminEmailPassword() {
		return params.get( PASSWORD );
	}

	/**
	 * Runtime property getter for {@value #CONFIG_FILE}
	 * 
	 * @return {@link biolockj.Config} file
	 * @throws DockerVolCreationException 
	 */
	public static File getConfigFile() throws DockerVolCreationException {
		return getConfigFile( true );
	}
	public static File getConfigFile(boolean dockerize) throws DockerVolCreationException {
		return getFileParam( CONFIG_FILE, dockerize );
	}

	/**
	 * Runtime property getter for {@value #DIRECT_MODE}
	 * 
	 * @return Direct module BioModule class name
	 */
	public static String getDirectModuleDir() {
		return params.get( DIRECT_MODE );
	}

	/**
	 * Runtime property getter for direct module pipeline directory
	 * 
	 * @return Pipeline directory dir
	 */
	public static File getDirectPipelineDir() {
		return params.get( DIRECT_PIPELINE_DIR ) == null ? null: new File( params.get( DIRECT_PIPELINE_DIR ) );
	}

	/**
	 * Return host on which Docker container is running
	 * 
	 * @return Host name
	 */
	public static String getDockerHostName() {
		return params.get( HOSTNAME );
	}

	/**
	 * Runtime property getter for Docker host $USER $HOME dir
	 * 
	 * @return Host {@value #HOST_HOME_DIR} directory
	 * @throws DockerVolCreationException 
	 */
	public static File getHomeDir() throws DockerVolCreationException {
		return getHomeDir( true );
	}
	//TODO: cut down on usage of home dir
	public static File getHomeDir(boolean dockerize) throws DockerVolCreationException {
		if (dockerize && DockerUtil.inDockerEnv()) return new File(DockerUtil.AWS_EC2_HOME);
		return getFileParam( HOME_DIR, dockerize );
	}

	/**
	 * For cluster env, runtime params that need be forward to detached cluster Java modules. For Docker env, return
	 * line with BLJ_OPTIONS in Docker java_module scripts.
	 * 
	 * @param module JavaModule
	 * @return java -jar BioLockJ.jar runtime args
	 * @throws DockerVolCreationException 
	 */
	public static String getJavaModuleArgs( final JavaModule module ) throws DockerVolCreationException {
		Log.info( RuntimeParamUtil.class, "Building BLJ_OPTIONS for java_module script -->" );
		return getJavaComputeNodeArgs( module );
	}

	/**
	 * Extract the project name from the Config file.
	 * 
	 * @return Project name
	 * @throws DockerVolCreationException 
	 */
	public static String getProjectName() throws DockerVolCreationException {
		final String configName = getConfigFile().getName();
		String name = configName;
		final String[] exts = { ".ascii", ".asc", ".plain", ".rft", ".tab", ".text", ".tsv", ".txt",
			Constants.PROPS_EXT, ".prop", ".props", ".config" };

		for( final String ext: exts )
			if( name.toLowerCase().endsWith( ext ) ) name = name.substring( 0, name.length() - ext.length() );

		final int i = name.indexOf( "." );
		if( name.equals( configName ) && i > -1 && name.length() > i + 1 ) name = name.substring( i + 1 );

		if( name.length() > Constants.MASTER_PREFIX.length() && name.startsWith( Constants.MASTER_PREFIX ) )
			name = name.replace( Constants.MASTER_PREFIX, "" );

		return name;
	}

	/**
	 * Return restart pipeline directory
	 * 
	 * @return File directory path
	 * @throws DockerVolCreationException 
	 */
	public static File getRestartDir() throws DockerVolCreationException {
		return getFileParam( RESTART_DIR, true );
	}
	public static File getRestartDir(boolean dockerize) throws DockerVolCreationException {
		return getFileParam( RESTART_DIR, dockerize );
	}

	/**
	 * Return the runtime args as a String.
	 * 
	 * @return Runtime arg String value
	 */
	public static String getRuntimeArgs() {
		return runtimeArgs;
	}

	/**
	 * Return TRUE if runtime parameter {@value #AWS_FLAG} was found
	 * 
	 * @return boolean
	 */
	public static boolean isAwsMode() {
		return params.get( AWS_FLAG ) != null;
	}
	
	public static boolean isPrecheckMode() {
		return params.get( PRECHECK_FLAG ) != null;
	}

	/**
	 * Return TRUE if runtime parameters indicate Logs should be written to system.out
	 * 
	 * @return boolean
	 */
	public static boolean logToSystemOut() {
		return params.get( SYSTEM_OUT_FLAG ) != null;
	}

	/**
	 * Print arg descriptions
	 * 
	 * TODO: PROBLEMATIC APPROACH TO SHOW ARGS BY PRINTING VARIABLES IN THIS METHOD
	 */
	public static void printArgsDescriptions() {
		final String sep = ", ";
		System.err.println( "Usage: java -jar BioLockJ.jar <args>");
		System.err.println( "To run the program, args MUST include one of: ");
		System.err.println( String.join( sep, REQUIRED_ARGS ));
		System.err.println( "Arguments"); 
		System.err.println( "Named arguments for directories:");
		System.err.println( String.join( sep, DIR_ARGS ) );
		System.err.println( "Other named arguments:");
		System.err.println( String.join( sep, NAMED_ARGS ) );
		System.err.println( "Flags:");
		System.err.println( String.join( sep, ARG_FLAGS ) );
		System.err.println( "Info-only arguments:");
		System.err.println( Constants.HELP + sep + Constants.VERSION );
	}

	/**
	 * TODO: runtime params should always store the string as given on the command line, 
	 *       and that should be the host-file path, even when running in docker.
	 * Register and verify the runtime parameters. There are 2 required parameters:<br>
	 * <ol>
	 * <li>The {@link biolockj.Config} file path
	 * <li>The pipeline parent directory ($DOCKER_PROJ)
	 * </ol>
	 *
	 * @param args Program runtime parameters
	 * @throws RuntimeParamException if invalid parameters found
	 * @throws DockerVolCreationException 
	 */
	public static void registerRuntimeParameters( final String[] args ) throws RuntimeParamException, DockerVolCreationException {
		printRuntimeArgs( args );
		parseParams( args );
		verify_BLJ_PROJ();

		if( getDirectModuleDir() != null ) assignMasterConfig( DIRECT_MODE, assignDirectPipelineDir() );
		else if( doRestart() && getConfigFile() == null ) assignMasterConfig( RESTART_DIR, getRestartDir() );
		else try {
			final File localConfig = Config.getLocalConfigFile( params.get( CONFIG_FILE ) );
			params.put( CONFIG_FILE, localConfig.getAbsolutePath() );
		} catch( final Exception ex ) {
			throw new RuntimeParamException( CONFIG_FILE, params.get( CONFIG_FILE ),
				"Failed to get local Confg file path: " + ex.getMessage() );
		}

		validateParams();
	}

	/**
	 * Print the runtime args to System.out or using the Logger based on useSysOut parameter.
	 * 
	 * @param args Runtime Args
	 */
	protected static void printRuntimeArgs( final String[] args ) {
		int numArgs = 0;
		Log.info( RuntimeParamUtil.class, Constants.RETURN + Constants.LOG_SPACER );
		if( args != null && args.length > 0 ) {
			Log.info( RuntimeParamUtil.class, "Application runtime args:" );
			for( final String arg: args ) {
				runtimeArgs += ( runtimeArgs.isEmpty() ? "": " " ) + arg;
				Log.info( RuntimeParamUtil.class, " ---> arg[" + numArgs++ + "] = " + arg );
			}
		} else Log.info( RuntimeParamUtil.class, "No Java runtime args found!" );
		Log.info( RuntimeParamUtil.class, Constants.LOG_SPACER );
	}

	private static File assignDirectPipelineDir() throws RuntimeParamException, DockerVolCreationException {
		Log.info( RuntimeParamUtil.class,
			"Separating pipeline dir name and module name from: \"" + DIRECT_MODE + "\" " + getDirectModuleDir() );
		final StringTokenizer st = new StringTokenizer( getDirectModuleDir(), ":" );
		if( st.countTokens() != 2 ) throw new RuntimeParamException( DIRECT_MODE, getDirectModuleDir(),
			"Invalid parameter format = $PIPELINE_DIR_NAME:$MODULE_DIR_NAME (with a single colon \":\" - but " +
				st.countTokens() + " instances of \":\" were found" );

		final File pipelineDir = new File( get_BLJ_PROJ().getAbsolutePath() + File.separator + st.nextToken() );
		if( !pipelineDir.isDirectory() ) throw new RuntimeParamException( DIRECT_MODE, getDirectModuleDir(),
			"Direct module pipeline directory not found: " + pipelineDir.getAbsolutePath() );

		params.put( DIRECT_PIPELINE_DIR, pipelineDir.getAbsolutePath() );
		params.put( DIRECT_MODE, st.nextToken() );
		return getDirectPipelineDir();
	}

	private static void assignLastParam( final String param ) throws RuntimeParamException {
		if( !params.keySet().contains( CONFIG_FILE ) && !params.keySet().contains( RESTART_DIR ) &&
			!params.keySet().contains( param ) && !params.values().contains( param ) ) params.put( CONFIG_FILE, param );
		else if( NAMED_ARGS.contains( param ) )
			throw new RuntimeParamException( param, "", "Missing argument for named parameter" );
	}

	private static void assignMasterConfig( final String param, final File pipelineDir ) throws RuntimeParamException {
		if( pipelineDir == null ) throw new RuntimeParamException( param, "", "Pipeline root directory not found!" );
		if( !pipelineDir.isDirectory() )
			throw new RuntimeParamException( param, pipelineDir.getAbsolutePath(), "System directory not found" );
		try {
			File masterConfig = MasterConfigUtil.getExistingMasterConfig( pipelineDir );
			params.put( CONFIG_FILE, masterConfig.getAbsolutePath() );
		}catch( BioLockJException bljEx ) {
			throw new RuntimeParamException( param, "", bljEx.getMessage() );
		}
	}

	private static String getDir( final String path ) {
		if( path != null && path.endsWith( File.separator ) ) return path.substring( 0, path.length() - 1 );
		return Config.replaceEnvVar( path );
	}

	private static String getJavaComputeNodeArgs( final JavaModule module ) throws DockerVolCreationException {
		Log.info( RuntimeParamUtil.class, "Building java args for compute nodes  -->" );
		return BLJ_PROJ_DIR + " " + get_BLJ_PROJ(false).getAbsolutePath() + " " 
			 + HOME_DIR + " " + getHomeDir(false).getAbsolutePath() + " " 
			 + DIRECT_MODE + " " + Config.pipelineName() + ":" + module.getModuleDir().getName();
	}

	private static void parseParams( final String[] args ) throws RuntimeParamException {
		String prevParam = "";
		for( final String arg: args ) {
			if( ARG_FLAGS.contains( arg ) ) params.put( arg, Constants.TRUE );
			else if( DIR_ARGS.contains( prevParam ) ) params.put( prevParam, getDir( arg ) );
			else if( NAMED_ARGS.contains( prevParam ) ) params.put( prevParam, arg );
			else extraParams.add( arg );
			prevParam = arg;
		}

		assignLastParam( prevParam );
		extraParams.removeAll( params.keySet() );
		extraParams.removeAll( params.values() );
		if( !extraParams.isEmpty() )
			throw new RuntimeParamException( "Unexpected runtime parameters found:  { " + extraParams + " }" );
	}

	private static void validateParams() throws RuntimeParamException, DockerVolCreationException {
		if( getHomeDir() == null )
			throw new RuntimeParamException( HOME_DIR, "", "$HOME directory required, but not found" );
		if( getConfigFile() == null )
			throw new RuntimeParamException( CONFIG_FILE, "", "Config file required, but not found" );
		if( !getHomeDir().isDirectory() ) throw new RuntimeParamException( HOME_DIR, getHomeDir().getAbsolutePath(),
			"System directory-path not found" );
		if( !getConfigFile().isFile() ) throw new RuntimeParamException( CONFIG_FILE, getConfigFile().getAbsolutePath(),
			"System file-path not found" );
	}

	private static void verify_BLJ_PROJ() throws RuntimeParamException, DockerVolCreationException {
		if( get_BLJ_PROJ() == null )
			throw new RuntimeParamException( BLJ_PROJ_DIR, "", "$BLJ_PROJ directory required, but not found" );
		if( !get_BLJ_PROJ().isDirectory() ) throw new RuntimeParamException( BLJ_PROJ_DIR,
			get_BLJ_PROJ().getAbsolutePath(), "System directory-path not found" );
	}

	/**
	 * {@link biolockj.Config} AWS end parameter switch: {@value #AWS_FLAG}
	 */
	public static final String AWS_FLAG = "-aws";

	/**
	 * Automatically added $BLJ_PROJ by biolockj script: {@value #BLJ_PROJ_DIR}
	 */
	protected static final String BLJ_PROJ_DIR = "-projectDir";

	/**
	 * {@link biolockj.Config} file path runtime parameter switch: {@value #CONFIG_FILE}
	 */
	protected static final String CONFIG_FILE = "-config";

	/**
	 * Direct mode runtime parameter switch: {@value #DIRECT_MODE}
	 */
	protected static final String DIRECT_MODE = "-direct";
	
	/**
	 * Used internally when in direct mode, 
	 * stores the name of the pipeline after it is extracted from the {@value DIRECT_MODE} runtime parameter.
	 */
	private static final String DIRECT_PIPELINE_DIR = "--pipeline-dir";

	/**
	 * Automatically added $HOME by biolockj/dockblj script: {@value #HOME_DIR}
	 */
	protected static final String HOME_DIR = "-homeDir";

	/**
	 * Automatically added $(hostname) by biolockj/dockblj script: {@value #HOSTNAME}
	 */
	protected static final String HOSTNAME = "-hostname";

	/**
	 * Change password runtime parameter switch: {@value #PASSWORD}
	 */
	protected static final String PASSWORD = "-password";

	/**
	 * Restart pipeline runtime parameter switch: {@value #RESTART_DIR}
	 */
	protected static final String RESTART_DIR = "-restartDir";

	/**
	 * Log to System.out instead of Log for debug early runtime errors with switch: {@value #SYSTEM_OUT_FLAG}
	 */
	protected static final String SYSTEM_OUT_FLAG = "-systemOut";
	
	/**
	 * Flag argument; if present, BioLockJ will stop after checkDependencies step. flag: {@value #SYSTEM_OUT_FLAG}
	 */
	protected static final String PRECHECK_FLAG = "-precheck";

	private static final List<String> ARG_FLAGS = Arrays.asList( AWS_FLAG, SYSTEM_OUT_FLAG, PRECHECK_FLAG );
	private static final List<String> DIR_ARGS = Arrays.asList( BLJ_PROJ_DIR, HOME_DIR, RESTART_DIR );
	private static final List<String> extraParams = new ArrayList<>();
	private static final List<String> NAMED_ARGS = Arrays.asList( CONFIG_FILE, DIRECT_MODE, HOSTNAME, PASSWORD );
	private static final List<String> REQUIRED_ARGS = Arrays.asList(CONFIG_FILE, RESTART_DIR, DIRECT_MODE);
	private static final Map<String, String> params = new HashMap<>();
	private static String runtimeArgs = "";
}
