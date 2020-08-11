package biolockj.launch;

import java.util.Arrays;
import java.util.List;
import biolockj.exception.BioLockJException;
import biolockj.exception.FatalExceptionHandler;
import biolockj.util.DockerUtil;

public class Launcher {

	public static void main( String[] args ) throws InterruptedException {
		List<String> userArgs = Arrays.asList( args );
		try {

			if( userArgs.contains( "--help" ) || userArgs.contains( "-help" ) || userArgs.contains( "--h" ) ||
				userArgs.contains( "-h" ) ) {
				LaunchProcess.printHelp(System.out);
				throw new EndLaunch( 0 );
			}
			if( userArgs.contains( "--version" ) || userArgs.contains( "-version" ) || userArgs.contains( "--v" ) ||
				userArgs.contains( "-v" ) ) {
				LaunchProcess.printVersion();
				throw new EndLaunch( 0 );
			}

			if( args.length == 0 ) {
				throw new EndLaunch( "biolockj requires at least one argument." );
			}

			if( LaunchProcess.inTestMode() ) LaunchProcess.showArgs("Launcher", args);
			
			LaunchProcess lp = new LaunchProcess( args );
			
			if (lp.getFlag( LaunchProcess.AWS_ARG ) ) {
				System.err.println("This feature is under active development...");
				lp.assignMainArg();
				lp.gatherEnvVars();
				lp.checkBasicArgCompatibility();
				StringBuilder cmd = new StringBuilder();
				cmd.append( lp.BLJ_DIR.getAbsolutePath() + "/script/launch_aws " );
				for (String arg : args) cmd.append( " " + arg );
				if (LaunchProcess.inTestMode()) {
					throw new EndLaunch( 0, LaunchProcess.BIOLOCKJ_TEST_MODE_VALUE + cmd.toString() );
				}else {
					final Process p = Runtime.getRuntime().exec( cmd.toString() ); 
					lp.showProcess( p );
				}
			}else if (lp.getFlag( LaunchProcess.DOCKER_ARG ) ) {
				DockerLaunchProcess.main( args );
			}else if (lp.getFlag( LaunchProcess.GUI_ARG ) ) {
				DockerLaunchProcess.main( args );
			}else JavaLaunchProcess.main( args );

		} catch( EndLaunch el ) {
			handleEndLaunchException(el);
		} catch( BioLockJException ex ) {
			handleBioLockJException(ex);
		} catch( Exception ex ) {
			handleGeneralException(ex);
		}
	}

	static void pauseToRead() throws InterruptedException {
		if( DockerUtil.inDockerEnv() ) Thread.sleep( 3000 );
	}
	
	static void handleEndLaunchException(EndLaunch el) throws InterruptedException {
		pauseToRead();
		System.exit( el.getExitCode() );
	}
	static void handleBioLockJException(BioLockJException ex) throws InterruptedException {
		System.err.println( "An error occurred during the initial launch process." );
		System.err.println( FatalExceptionHandler.ERROR_TYPE + ex.getClass().getSimpleName() );
		System.err.println( FatalExceptionHandler.ERROR_MSG + ex.getMessage() );
		System.err.println();
		pauseToRead();
		System.exit( 1 );
	}
	static void handleGeneralException(Exception ex) throws InterruptedException {
		System.err.println( "An unexpected general error occurred during the initial launch process." );
		ex.printStackTrace();
		pauseToRead();
		System.exit( 1 );
	}

}
