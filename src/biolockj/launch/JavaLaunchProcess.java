package biolockj.launch;

import java.io.File;
import java.io.IOException;
import biolockj.exception.ConfigPathException;
import biolockj.exception.DockerVolCreationException;
import biolockj.util.RuntimeParamUtil;

public class JavaLaunchProcess extends LaunchProcess {
	
	private File BLJ_JAR_FILE = null;
	static final String BioLockJ_CLASS = "biolockj.BioLockJ";

	public JavaLaunchProcess( String[] args ) throws DockerVolCreationException, EndLaunch, ConfigPathException {
		super(args);
		checkJarFile();
	}

	public static void main(String[] args) throws Exception {
		if( inTestMode() ) showArgs(JavaLaunchProcess.class.getSimpleName(), args);
		JavaLaunchProcess launcher = new JavaLaunchProcess(args);
		launcher.runCommand();
	}
	
	@Override
	String createCmd() throws EndLaunch {
		StringBuilder command = new StringBuilder();
		command.append( "java -cp " + BLJ_JAR_FILE.getAbsolutePath());
		if (getFlag( EXT_MODS_ARG )) {
			command.append(":" + getParameter( EXT_MODS_ARG ) + "/*" );
		}
		command.append( " " + BioLockJ_CLASS );
		command.append( " " + RuntimeParamUtil.BLJ_PROJ_DIR + " " + BLJ_PROJ_DIR.getAbsolutePath() );
		if (getFlag( PASSWORD_ARG ) ) command.append( " " + RuntimeParamUtil.PASSWORD + " " + getParameter( PASSWORD_ARG ));
		if (getFlag( RESTART_ARG ) ) command.append( " " + RuntimeParamUtil.RESTART_DIR + " " + getRestartDir().getAbsolutePath());
		if (getFlag( PRECHECK_ARG ) || getFlag( UNUSED_PROPS_ARG )) command.append( " -precheck" );
		if (getFlag( UNUSED_PROPS_ARG ) ) { command.append( " " + RuntimeParamUtil.UNUSED_PROPS_FLAG ); }
		if ( getConfigFile() != null ) command.append( " " + RuntimeParamUtil.CONFIG_FILE + " " + getConfigFile().getAbsolutePath() );
		if ( ! getFlag( FG_ARG )) {
			command.insert( 0, "nohup " );
			//command.append( " >/dev/null 2>&1 &" );
		}
		return command.toString();
	}

	@Override
	void runCommand() throws Exception {
		String cmd = createCmd();
		if (inTestMode()) {System.out.println( LaunchProcess.BIOLOCKJ_TEST_MODE_VALUE + " " + cmd); throw new EndLaunch( 0 );}
		
		super.runCommand();
		
		System.out.print( "Initializing BioLockJ" );
		try {
			final Process p = Runtime.getRuntime().exec( cmd ); 
			if (getFlag( FG_ARG )) {
				showProcess(p);
			}else {
				confirmJavaStarted(p);
			}
		} catch( final Exception ex ) {
			System.err.println( "Problem occurred running command: " + cmd);
			ex.printStackTrace();
		}
		pipeDir = getMostRecentPipeline();
		printInfo();
	}
	
	private void confirmJavaStarted(Process p) throws InterruptedException, IOException {
		int maxtime = 15; 
		int haveWaited = 0;
		while( (maxtime > haveWaited || getFlag(WAIT_ARG) )
						&& initDir.getAbsolutePath().equals( mostRecent.getAbsolutePath() )
						&& ! restartDirHasStatusFlag()
						&& p.isAlive()) {
			System.out.print(".");
			haveWaited++;
			mostRecent = getMostRecentPipeline();
			if ( haveWaited == 10) {
				System.out.print( "waiting for head java process to start");
			}
			if ( haveWaited == maxtime ) {
				if (getFlag( WAIT_ARG ) ) {
					System.out.println( "The normal timeout has been disabled." );
				}else {
					System.out.println( "Reached max wait time: " + maxtime + " seconds." );
				}
			}
			Thread.sleep( 1000 );
		}
		Thread.sleep( 1000 );
		System.out.print(".");
	}
	
	private void checkJarFile() throws EndLaunch {
		BLJ_JAR_FILE=new File( new File(BLJ_DIR, "dist"), "BioLockJ.jar");
		if ( ! BLJ_JAR_FILE.exists()) {
			msgs.add( "Error: Required env variable BLJ_JAR [\"${BLJ}\"] must be a file on the filesystem. ");
			msgs.add("If you are a developer, you may need to build the program.");
			throw new EndLaunch( 1, msgs);
		}
	}
	
}
