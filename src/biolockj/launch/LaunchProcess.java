package biolockj.launch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import biolockj.Config;
import biolockj.Constants;
import biolockj.Processor;
import biolockj.exception.ConfigPathException;
import biolockj.exception.DockerVolCreationException;
import biolockj.exception.InvalidPipelineException;
import biolockj.pipelines.PipelineUtil;
import biolockj.util.BioLockJUtil;
import biolockj.util.DockerUtil;

public class LaunchProcess {

	static final String GUI_ARG = "gui";
	protected static final String PASSWORD_ARG = "password";
	protected static final String CONFIG_ARG = "config-override";
	protected static final String RESTART_ARG = "restart";
	protected static final String AWS_ARG = "aws";
	static final String DOCKER_ARG = "docker";
	protected static final String FG_ARG = "foreground";
	protected static final String EXT_MODS_ARG = "external-modules";
	protected static final String BLJ_ARG = "blj";
	protected static final String PROJ_ARG = "blj_proj";
	protected static final String ENV_ARG = "env-var";
	protected static final String WAIT_ARG = "wait-for-start";
	protected static final String PRECHECK_ARG = "precheck-only";
	protected static final String UNUSED_PROPS_ARG = "unused-props";
	protected static final String DEBUG_ARG = "verbose";
	protected static final String DOCKER_MAPPER_ARG = "docker-mapper";
	
	public static final String BIOLOCKJ_TEST_MODE = "BIOLOCKJ_TEST_MODE";
	protected static String BIOLOCKJ_TEST_MODE_VALUE = null;

	public static final String PLS_USE_INSTALL =
		"The install script packaged with BioLockJ sets this value. You may need to start a new terminal window.";

	private String mainArg = null;
	private String configArgVal = null;
	private File configFile = null;
	private String restartArgVal = null;
	private File restartDir = null;
	private boolean replacesPrecheck;
	protected File BLJ_DIR;
	protected File BLJ_PROJ_DIR;
	/*
	 * The directory for the pipeline that has just been launched.
	 */
	private File pipeDir = null;
	/*
	 * Most recently modified pipeline as of the start of the launch process.
	 */
	protected File initDir = null;
	/*
	 * Most recently modified pipeline at a given moment.
	 */
	protected File mostRecent = null;

	private static final List<String> longArgName =
		Arrays.asList( new String[] { GUI_ARG, CONFIG_ARG, RESTART_ARG, AWS_ARG, DOCKER_ARG, FG_ARG, ENV_ARG, WAIT_ARG,
			PRECHECK_ARG, UNUSED_PROPS_ARG, PASSWORD_ARG, PROJ_ARG, EXT_MODS_ARG, BLJ_ARG, DEBUG_ARG, DOCKER_MAPPER_ARG } );
	private static final List<String> takeShortArg = Arrays.asList( new String[] { GUI_ARG, CONFIG_ARG, RESTART_ARG,
		AWS_ARG, DOCKER_ARG, FG_ARG, ENV_ARG, WAIT_ARG, PRECHECK_ARG, UNUSED_PROPS_ARG, "help", "version" } );
	private static final List<String> takesValue =
		Arrays.asList( new String[] { PASSWORD_ARG, CONFIG_ARG, EXT_MODS_ARG, PROJ_ARG, ENV_ARG, DOCKER_MAPPER_ARG } );

	protected HashMap<String, String> parameters = new HashMap<>();

	protected HashMap<String, String> envVars = new HashMap<>();

	protected String command;

	protected List<String> msgs = new ArrayList<>();

	public static void main( String[] args ) throws Exception {
		if( inTestMode() ) showArgs(LaunchProcess.class.getSimpleName(), args);

		LaunchProcess launcher = new LaunchProcess( args );
		launcher.runCommand();
	}

	static boolean inTestMode() {
		BIOLOCKJ_TEST_MODE_VALUE = Processor.getBashVar( BIOLOCKJ_TEST_MODE );
		if (BIOLOCKJ_TEST_MODE_VALUE != null && BIOLOCKJ_TEST_MODE_VALUE.equals( BIOLOCKJ_TEST_MODE )) BIOLOCKJ_TEST_MODE_VALUE = null;
		if (BIOLOCKJ_TEST_MODE_VALUE != null && BIOLOCKJ_TEST_MODE_VALUE.isEmpty()) BIOLOCKJ_TEST_MODE_VALUE = null;
		return BIOLOCKJ_TEST_MODE_VALUE != null;
	}
	
	static void showArgs(String process, String[] args) {
		StringBuilder sb = new StringBuilder();
		sb.append( " ---------> Execute CMD [  " + process );
		for (String arg : args) sb.append( " " + arg );
		sb.append( " ]" );
		ProgressUtil.printStatus( sb.toString(), false );
	}

	protected void assignMainArg() {
		if( getFlag( RESTART_ARG ) ) {
			restartArgVal = mainArg;
			if( inTestMode() ) ProgressUtil.printStatus( "Using " + restartArgVal + " as the pipeline to restart.", false );
			if( parameters.get( CONFIG_ARG ) != null ) {
				configArgVal = parameters.get( CONFIG_ARG );
				ProgressUtil.printStatus( "Updating pipeline with config file " + configArgVal + ".", false );
			}
		} else {
			configArgVal = mainArg;
			if( inTestMode() ) ProgressUtil.printStatus( "Using " + configArgVal + " as the config file.", false );
		}
	}

	protected void checkBasicArgCompatibility() throws EndLaunch, DockerVolCreationException {
		if (getFlag( UNUSED_PROPS_ARG ) ) parameters.put(PRECHECK_ARG, String.valueOf( true ));
		if( restartArgVal != null ) {
			if( getFlag( UNUSED_PROPS_ARG ) ) {
				msgs.add( "Error: \"" + UNUSED_PROPS_ARG +
					"\" can only be used with new pipelines; cannot be used in conjunction with \"" + RESTART_ARG +
					"\". " );
				throw new EndLaunch( msgs );
			}
			if( getFlag( PRECHECK_ARG ) ) {
				msgs.add( "Error: \"" + PRECHECK_ARG +
					"\" can only be used with new pipelines; cannot be used in conjunction with " + RESTART_ARG +
					". " );
				throw new EndLaunch( msgs );
			}
			restartDir = new File( restartArgVal );
			if ( ! restartDir.exists() && DockerUtil.containerizePath( restartDir ).exists())
				restartDir = DockerUtil.containerizePath( restartDir );
			if( !restartDir.isDirectory() ) {
				msgs.add( "Error: [" + restartDir.getAbsolutePath() + "] is not a directory on the filesystem." );
				throw new EndLaunch( msgs );
			}
			if( configArgVal != null ) {
				configFile = new File( configArgVal );
				if( !configFile.exists() ) {
					msgs.add( "Error: Config file [ " + configFile.getAbsolutePath() + " ] not found on filesystem." );
					throw new EndLaunch( msgs );
				}
			}else {
				FilenameFilter masterFilter = new FilenameFilter() {
					@Override
					public boolean accept( File dir, String name ) {
						return name.startsWith( Constants.MASTER_PREFIX ) && name.endsWith( Constants.PROPS_EXT );
					}};
				if (restartDir.listFiles(masterFilter).length != 1) {
					throw new EndLaunch( "Cannot find single MASTER*.properties in restart dir [" + restartDir.getAbsolutePath() + "]");
				}else {
					configFile = restartDir.listFiles(masterFilter)[0];
				}
			}
		} else {
			configFile = new File( configArgVal );
			if ( ! configFile.exists() && DockerUtil.containerizePath( configFile ).exists() ) 
				configFile = DockerUtil.containerizePath( configFile );
			if( !configFile.exists() ) {
				msgs.add( "Error: Config file [ " + configFile.getAbsolutePath() + " ] not found on filesystem." );
				throw new EndLaunch( msgs );
			}
		}
		if (getFlag(EXT_MODS_ARG)) {
			File modsDir = new File(parameters.get( EXT_MODS_ARG ));
			if ( ! modsDir.exists() && DockerUtil.containerizePath( modsDir ).exists()) {
				modsDir = DockerUtil.containerizePath( modsDir );
				parameters.put( EXT_MODS_ARG, modsDir.getAbsolutePath() );
			}
			if ( ! modsDir.isDirectory() ) {
				msgs.add( "The value of the [" + EXT_MODS_ARG + "] arg must be an existing directory." );
				msgs.add( "Found: " + parameters.get( EXT_MODS_ARG ) );
				throw new EndLaunch( msgs);
			}
				
		}
	}

	public LaunchProcess( String[] args ) throws EndLaunch, DockerVolCreationException, ConfigPathException {
		for( String longName: longArgName ) {
			if( takesValue.contains( longName ) ) parameters.put( longName, null );
			else parameters.put( longName, String.valueOf( false ) );
		}
		parseOptions( args );
		assignMainArg();
		checkBasicArgCompatibility();
		gatherEnvVars();
		setInitialState();
	}

	private static String getLongArgName( String shortName ) throws EndLaunch {
		for( String longName: longArgName ) {
			if( takeShortArg.contains( longName ) && getShortArgName( longName ).equals(shortName) ) {
				return longName;
			}
		}
		throw new EndLaunch( "Error: unrecognized short-form argument [ -" + shortName + " ]." );
	}

	private static String getShortArgName( String longName ) {
		return longName.substring( 0, 1 );
	}

	private String getMainArg( String[] args ) {
		String lastArg = args[ args.length - 1 ];
		if( lastArg.substring( 0, 1 ).contentEquals( "-" ) ) {
			mainArg = null;
		} else {
			mainArg = lastArg;
		}
		return mainArg;
	}

	private void parseOptions( String[] args ) throws EndLaunch {
		ArrayList<String> options = new ArrayList<>();
		options.addAll( Arrays.asList( args ) );

		if( getMainArg( args ) != null ) {
			options.remove( args.length - 1 );
		}

		if( options.size() > 0 ) {
			// parse_options
			String argName = null;
			for( String arg: options ) {
				// If arg starts with -, we assume it is an argument name
				if( arg.substring( 0, 1 ).contentEquals( "-" ) ) {
					if( argName != null && takesValue.contains( argName ) ) {
						msgs.add( "Error: argument [ " + argName +
							" ] takes a value; the value should not begin with \"-\", found [ " + arg + " ]." );
						throw new EndLaunch( msgs );
					} else {
						argName = null;
					}
					if( arg.substring( 0, 2 ).contentEquals( "--" ) ) {
						// this is a long form arg name
						argName = arg.substring( 2 );
						if( !longArgName.contains( argName ) ) {
							msgs.add( "Error: unrecognized argument name [ " + arg + " ]" );
							throw new EndLaunch( msgs );
						}
						if( !takesValue.contains( argName ) ) {
							parameters.put( argName, String.valueOf( true ) );
							argName = null;
						}
					} else {
						// starts with a single '-' ; this must be a short form arg name
						// process each letter in arg, so multiple short form args can be stacked behind a single -
						for( int i = 1; i < arg.length(); i++ ) {
							if( argName != null && takesValue.contains( argName ) ) {
								msgs.add( "Error: the argument [ " + argName + " ] must take a value." );
								throw new EndLaunch( msgs );
							}
							String letter = arg.substring( i, i + 1 ); 
							String longName = getLongArgName( letter );
							if( takesValue.contains( longName ) ) {
								argName = longName;
							} else {
								parameters.put( longName, String.valueOf( true ) );
								argName = null;
							}
						}
					}
				} else {
					// This must be a value for argName
					if( argName == null ) {
						msgs.add( "Error: unnamed value [ " + arg + " ]" );
						throw new EndLaunch( msgs );
					}
					if( !longArgName.contains( argName ) ) {
						msgs.add( "Error: unrecognized argument name [ " + arg + " ]" );
						throw new EndLaunch( msgs );
					}
					parameters.put( argName, arg );
					argName = null;
				}
			}
			if (argName != null && takesValue.contains( argName )) throw new EndLaunch( "Error: argument [ " + argName + " ] must take a value." );
		}
		if (inTestMode()) displayArgVals();
	}

	protected void gatherEnvVars() throws EndLaunch, DockerVolCreationException, ConfigPathException {
		try {
			Config.partiallyInitialize( getConfigFile() );
			envVars.putAll( Config.getEnvVarMap() ); //add any env vars that were referenced in the config file
		} catch( Exception e ) {
			msgs.add( "Warning: Local variables referenced in the config file may not be correctly passed to the module environments." );
		}
		checkBljProj();
		checkBljDir();
		if( parameters.get( ENV_ARG ) != null ) {
			StringTokenizer st = new StringTokenizer( parameters.get( ENV_ARG ), "," );
			while( st.hasMoreTokens() ) {
				String token = st.nextToken();
				StringTokenizer pair = new StringTokenizer( token, "=" );
				if( pair.countTokens() != 2 ) {
					msgs.add( "Error: the value of the " + ENV_ARG +
						" argument should be a comma-separated list, where each list element is a key=value pair using exactly one \"=\"." );
					throw new EndLaunch( msgs );
				}
				envVars.put( pair.nextToken().trim(), pair.nextToken().trim() );
			}
		}
	}

	private void checkBljProj() throws DockerVolCreationException, EndLaunch {
		if( parameters.get( PROJ_ARG ) != null ) {
			String path = DockerUtil.containerizePath( removeTrailingSlash( parameters.get( PROJ_ARG ) ) );
			envVars.put( Config.BLJ_PROJ_VAR, path );
			BLJ_PROJ_DIR = new File( path );
		} else {
			BLJ_PROJ_DIR = new File( Processor.getBashVar( Config.BLJ_PROJ_VAR ) );
		}
		if( BLJ_PROJ_DIR == null ) {
			msgs.add( "Error: Required env variable BLJ_PROJ is not defined." );
			msgs.add( PLS_USE_INSTALL );
			throw new EndLaunch( msgs );
		}
		if( ! BLJ_PROJ_DIR.isDirectory() && DockerUtil.containerizePath( BLJ_PROJ_DIR ).exists() ) {
			BLJ_PROJ_DIR = DockerUtil.containerizePath( BLJ_PROJ_DIR );
		}
		if( !BLJ_PROJ_DIR.isDirectory() ) {
			msgs.add(
				"Error: Required env variable [" + Config.BLJ_PROJ_VAR + "] must be a directory on the filesystem." );
			msgs.add( "Found: BLJ_PROJ=" + BLJ_PROJ_DIR.getAbsolutePath() );
			msgs.add( PLS_USE_INSTALL );
			throw new EndLaunch( msgs );
		}
	}

	private void checkBljDir() throws ConfigPathException, EndLaunch {
		try {
			BLJ_DIR = BioLockJUtil.getBljDir();
		} catch( ConfigPathException e ) {
			e.printStackTrace();
			BLJ_DIR = new File( Processor.getBashVar( "${" + Config.BLJ_BASH_VAR + "}" ) );
			if( !BLJ_DIR.isDirectory() ) throw e;
		}
		// not sure how this would happen...
		if( BLJ_DIR == null || !BLJ_DIR.isDirectory() ) {
			msgs.add( "Failed to find the path to the BioLockJ top directory." );
			msgs.add( "This is an uncommon error." );
			throw new EndLaunch( msgs );
		}
	}

	private static String removeTrailingSlash( final String path ) {
		if( path.endsWith( "\\" ) || path.endsWith( "/" ) ) {
			return path.substring( 0, path.length() );
		} else {
			return path;
		}
	}

	void displayArgVals() {
		for( String arg: longArgName ) {
			if( parameters.get( arg ) != null ) {
				if( takesValue.contains( arg ) || parameters.get( arg ).contentEquals( String.valueOf( true ) ) ) {
					ProgressUtil.printStatus( arg + " = " + parameters.get( arg ), false );
				}
			}
		}
	}

	/**
	 * Print the version to standard out.
	 * This is a rare case, usually all printing is done through ProgressUtil
	 */
	public static void printVersion() {
		System.out.println( BioLockJUtil.getVersion() );
	}

	/**
	 * Print the help to standard err.
	 * This is a rare case, usually all printing is done through ProgressUtil
	 */
	public static void printHelp() {
		printHelp(System.err);
	}
	/**
	 * Print the help menu.
	 * This is a rare case, usually all printing in the launch package is done through ProgressUtil.
	 * @param s PrintStream; System.out or System.err
	 */
	public static void printHelp(PrintStream s) {
		s.println( "" );
		s.println( "BioLockJ " + BioLockJUtil.getVersion() + " - " + Constants.YEAR_AND_OWNER );
		s.println( "Usage:" );
		s.println( "(bash)");
		s.println( "biolockj [options] <config|pipeline>" );
		s.println( "");
		s.println( "(java)");
		s.println( "java -jar /path/to/BioLockJ.jar [options] <config|pipeline>");
		s.println();
		s.println( "Options:" );

		addSpace( s, "version", "", "Show version" );
		addSpace( s, "help", "", "Show help menu" );
		addSpace( s, PRECHECK_ARG, "", "Set up pipeline and check dependencies and then STOP;" );
		continueDescription( s, "do not execute the pipeline. This is helpful when testing edits to config files." );
		addSpace( s, UNUSED_PROPS_ARG, "",
			"Check dependencies for all modules and report unused properties. Implies -p." );
		continueDescription( s, "This helps remove unnecessary properties and highlights errors in property names." );
		addSpace( s, RESTART_ARG, "", "Resume an existing pipeline" );
		addSpace( s, CONFIG_ARG, "<file>", "New config file (if restarting a pipeline)" );
		addSpace( s, PASSWORD_ARG, "<password>", "Encrypt password" );
		addSpace( s, DOCKER_ARG, "", "Run in docker" );
		addSpace( s, AWS_ARG, "", "Run on aws" );
		addSpace( s, GUI_ARG, "", "Start the BioLockJ GUI" );
		addSpace( s, FG_ARG, "", "Run the java process in the foreground without nohup" );
		addSpace( s, WAIT_ARG, "", "Do not release terminal until pipeline completes check-dependencies step." );
		addSpace( s, EXT_MODS_ARG, "<dir>", "Directory with compiled java code giving additional modules" );
		addSpace( s, BLJ_ARG, "", "Map $BLJ folder into the docker container;" );
		continueDescription( s, "this replaces BioLockJ packaged in a docker container with the local copy." );
		addSpace( s, DEBUG_ARG, "", "Equivalent to adding `" + Constants.LOG_LEVEL_PROPERTY + "=DEBUG` to the config file." );
		addSpace( s, DOCKER_MAPPER_ARG, "<class>", "Rarely used. See user guide. Ignored when not using docker." );
		addSpace( s, ENV_ARG, "<var=val>", "Environment variables to be passed to the BioLockJ environment." );
		continueDescription( s, "Can be a comma-sep list. Values take the form: a=foo,b=bar,c=baz" );
		addSpace( s, PROJ_ARG, "<dir>", "Directory that contains BioLockJ pipelines. If not supplied, " );
		continueDescription( s, "biolockj will use the value of environment variable \"BLJ_PROJ\"." );
		s.println();
		s.println("Run the test pipeline:");
		s.println("biolockj $BLJ/templates/myFirstPipeline/myFirstPipeline.properties");
		s.println();
	}

	private static void addSpace( PrintStream stream, String name, String value, String desc ) {
		if( takeShortArg.contains( name ) ) {
			addSpaceWithShort( stream, name, value, desc );
		} else {
			addSpaceNoShortArg( stream, name, value, desc );
		}
	}

	private static void addSpaceWithShort( PrintStream stream, String name, String value, String desc ) {
		stream.format( "%-1s %-3s %-25s %-10s", "", "-" + getShortArgName( name ), "--" + name + " " + value,
			desc );
		stream.println();
	}

	private static void addSpaceNoShortArg( PrintStream stream, String name, String value, String desc ) {
		stream.format( "%-1s %-3s %-25s %-10s", "", "", "--" + name + " " + value, desc );
		stream.println();
	}

	private static void continueDescription( PrintStream stream, String desc ) {
		stream.format( "%-1s %-3s %-25s %-10s", "", "", "", desc );
		stream.println();
	}

	public String getParameter( String key ) {
		return parameters.get( key );
	}

	public boolean getFlag( String key ) {
		if (! parameters.keySet().contains( key )) ProgressUtil.printStatus("The key [" + key + "] does not exist in parameters.", false);
		if( takesValue.contains( key ) ) return parameters.get( key ) != null;
		else return parameters.get( key ) == String.valueOf( true );
	}

	protected String getMainArgVal() {
		return mainArg;
	}

	protected File getConfigFile() {
		return configFile;
	}

	protected File getRestartDir() {
		return restartDir;
	}

	protected boolean restartDirHasStatusFlag() {
		if( !getFlag( RESTART_ARG ) ) return false;
		File flag = PipelineUtil.getPipelineStatusFlag( getRestartDir() );
		return flag != null;
	}

	protected File getMostRecentPipeline() {
		return PipelineUtil.getMostRecentPipeline(BLJ_PROJ_DIR);
	}

	protected boolean isReplacementForPrecheck() {
		return replacesPrecheck;
	}
	private void setInitialState() {
		initDir = getMostRecentPipeline();
		mostRecent = getMostRecentPipeline();
		try {
			String newProjectName = PipelineUtil.getProjectNameFromPropFile( configFile );
			String previousProjectName = PipelineUtil.getProjectName( mostRecent );
			if (newProjectName.contentEquals( previousProjectName ) && PipelineUtil.isPrecheckPipeline( mostRecent )) {
				replacesPrecheck = true;
			}else replacesPrecheck = false;
		} catch( InvalidPipelineException e ) {
			replacesPrecheck = false;
		}
	}
	
	protected void printInfo() throws InterruptedException, IOException, EndLaunch {
		if (restartDirHasStatusFlag()) {
			printActionOptions();
			printPipelineStatus(600);
		}else if( ! initDir.getAbsolutePath().equals( pipeDir.getAbsolutePath() )  && pipeDir.exists() ) {
			printActionOptions();
			printPipelineStatus(600);
		}else {
			ProgressUtil.printStatus("Pipeline may have failed to launch - check " + BLJ_PROJ_DIR.getAbsolutePath() + " for new pipeline.", true);
			throw new EndLaunch( 1 );
		}
	}

	protected void printActionOptions() {
		ProgressUtil.printStatus( "", true );
		ProgressUtil.printStatus( "After an initial status check, all pipeline updates will be in the pipeline folder.", true );
		printPipeDir();
		if( ! DockerUtil.inDockerEnv() ) {
			System.err.println( "cd-blj       -> Move to pipeline output directory" );
		}
	}
	
	protected void watchProcess(Process p) throws IOException, InterruptedException {
		final BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
		String s = null;
		if ( getFlag( FG_ARG ) ) ProgressUtil.clear();
		while( ( s = br.readLine() ) != null 
						&& ( ! hasPipelineStarted() || getFlag( FG_ARG ) ) )
		{
			scanForKeys(s);
			if ( getFlag( FG_ARG ) ) {
				System.out.println(s);
			}else {
				ProgressUtil.showUserUpdates(s);
			}
			if ( pipeDir == null && restartDirHasStatusFlag() ) setPipedir( restartDir );
			if ( pipeDir == null && foundNewPipeline() ) setPipedir( mostRecent );
		}
	}
	
	private boolean hasPipelineStarted() {
		if (getPipedir() == null) return false;
		else if ( PipelineUtil.getPipelineStatusFlag( getPipedir() ) != null)
			return true;
		return false;
	}

	protected String catchFirstResponse(Process p) throws IOException {
		String id = null;
		final BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
		String s = null;
		//TODO probably a cleaner way to do this.
		while( ( s = br.readLine() ) != null )
		{
			if (id==null && s!=null && !s.isEmpty()) id=s;
		}
		return id;
	}
	
	/**
	 * Show the status the just-launched pipeline.
	 * @param maxtime - maximum time in seconds to wait for a status.
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws EndLaunch 
	 */
	protected void printPipelineStatus(int maxtime) throws InterruptedException, IOException, EndLaunch {
		ProgressUtil.printStatus("", true);
		ProgressUtil.startSpinner("Fetching pipeline status ");
		int haveWaited = 0;
		while( haveWaited < maxtime ||
						getFlag( WAIT_ARG ) ||
						getFlag( PRECHECK_ARG)) {
			
			File unverified = new File(pipeDir, Constants.UNVERIFIED_PROPS_FILE);
			if (unverified.exists() && ! getFlag( RESTART_ARG )) {
				ProgressUtil.printStatus( "Warning: see \"unverified.properties\"", true );
				ProgressUtil.showFileContents( unverified, 1, 1 );
			}
			String flag = "none";
			File flagFile = PipelineUtil.getPipelineStatusFlag(pipeDir);
			if (flagFile != null) flag = flagFile.getName();
			if (flag.equals(Constants.BLJ_FAILED)) {
				ProgressUtil.printStatus( "BioLockJ has stopped.", true );
				ProgressUtil.showFileContents( flagFile, 1, 1 );
				throw new EndLaunch( 1 );
			}
			else if (flag.equals(Constants.PRECHECK_FAILED)) {
				ProgressUtil.printStatus( "There is a problem with this pipeline configuration.", true );
				ProgressUtil.showFileContents( flagFile, 1, 1 );
				throw new EndLaunch( 1 );
			}
			else if (flag.equals(Constants.BLJ_COMPLETE)) {
				ProgressUtil.printStatus( "Pipeline is complete.", true );
				throw new EndLaunch( 0 );
			}
			else if (flag.equals(Constants.PRECHECK_COMPLETE)) {
				ProgressUtil.printStatus( "Precheck is complete. No problems were found in this pipeline configuration.", true );
				throw new EndLaunch( 0 );
			}
			else if (flag.equals(Constants.BLJ_STARTED)) {
				ProgressUtil.printStatus( "Pipeline is running.", true );
				throw new EndLaunch( 0 );
			}
			else if (haveWaited == maxtime) {
				if (getFlag( WAIT_ARG ) || getFlag( PRECHECK_ARG ) || getFlag( UNUSED_PROPS_ARG )) {
					ProgressUtil.printStatus("(no timeout)", false);
				}
				else {
					ProgressUtil.printStatus("Reached max wait time: " + maxtime + " seconds. ", true);
					throw new EndLaunch( 1 );
				}
			}
			Thread.sleep( 1000 );
		}
		ProgressUtil.printStatus("", true);
		ProgressUtil.printStatus("Could not verify that the pipeline is running.", true);
		ProgressUtil.printStatus("It may still be checking dependencies.", true);
	}
	
	String createCmd() throws Exception {
		return "";
	}

	void runCommand() throws Exception {
		ProgressUtil.startSpinner( "Initializing" );
		if( restartDirHasStatusFlag() ) Reset.resetPipeline( restartDir.getAbsolutePath() );
		if( isReplacementForPrecheck() ) {
			ProgressUtil.printStatus("Discarding pre-existing precheck pipeline: " + mostRecent.getAbsolutePath(), false);
			PipelineUtil.discardPrecheckPipeline( mostRecent );
			initDir = getMostRecentPipeline();
			mostRecent = getMostRecentPipeline();
		}
	}
	
	void setPipedir(File dir) {
		pipeDir = dir;
	}
	
	File getPipedir() {
		return pipeDir;
	}
	
	/**
	 * Print the path to the pipeline root to standard out.
	 * This is a rare case, usually all printing is done through ProgressUtil.
	 * This should be done exactly once during the launch process.
	 */
	void printPipeDir() {
		if (getPipedir() != null) System.out.println(getPipedir().getAbsolutePath());
	}
	
	boolean foundNewPipeline() {
		return ! initDir.getAbsolutePath().equals( mostRecent.getAbsolutePath() );
	}
	
	/**
	 * Scan string for specific keys which warrant specific actions.
	 * @param s
	 */
	void scanForKeys(String s) {
		if ( getPipedir() == null ) grabPipelineLocation(s);
	}
	
	void grabPipelineLocation( String s ) {
		if( s.startsWith( Constants.PIPELINE_LOCATION_KEY ) ) {
			String path = s.replace( Constants.PIPELINE_LOCATION_KEY, "" ).trim();
			setPipedir( new File( path ) );
			if( restartDirHasStatusFlag() ) {
				ProgressUtil.printStatus( "Restarting pipeline: " + pipeDir.getAbsolutePath(), false );
			} else {
				ProgressUtil.printStatus( "Building pipeline: " + pipeDir.getAbsolutePath(), false );
			}
		}
	}
	
}
