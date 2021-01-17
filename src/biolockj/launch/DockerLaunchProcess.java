package biolockj.launch;

import java.io.File;
import java.io.IOException;
import java.util.List;
import biolockj.api.BioLockJ_API;
import biolockj.exception.BioLockJException;
import biolockj.exception.ConfigPathException;
import biolockj.exception.DockerVolCreationException;
import biolockj.util.DockerUtil;
import biolockj.util.RuntimeParamUtil;

public class DockerLaunchProcess extends LaunchProcess {
	
	public static final int GUI_PORT=8080;
	public static final String EXTERNAL_MODULES_CONTAINER_DIR= "/app/external_modules";

	public DockerLaunchProcess( String[] args ) throws EndLaunch, DockerVolCreationException, ConfigPathException {
		super( args );
	}
	
	public static void main(String[] args) throws InterruptedException {
		try {
		if( inTestMode() ) showArgs(DockerLaunchProcess.class.getSimpleName(), args);
		DockerLaunchProcess launcher = new DockerLaunchProcess(args);
		launcher.runCommand();
		if (launcher.getFlag(GUI_ARG)) startBrowser();//if the gui has its own docker container, this CMD would be defined there
		} catch( EndLaunch el ) {
			Launcher.handleEndLaunchException(el);
		} catch( BioLockJException ex ) {
			Launcher.handleBioLockJException(ex);
		} catch( Exception ex ) {
			Launcher.handleGeneralException(ex);
		}
	}

	@Override
	String createCmd() throws Exception {
		StringBuilder command = new StringBuilder();
		command.append( "docker run --rm" );
		if ( ! getFlag( FG_ARG )) command.append( " -d" );
		if (envVars.size() > 0) for (String var : envVars.keySet() ) command.append(" -e " + var + "=" + envVars.get( var ));
		//command.append( " -e BLJ_OPTIONS=\"" + getOptionVals() + "\"");
		command.append( " " + getVolumes() );
		if (getFlag( GUI_ARG ) ) command.append( "-p " + GUI_PORT + ":3000 --expose " + GUI_PORT + " -w /app/biolockj/web_app" );
		command.append( " " + getDockerImg() );
		if (getFlag( GUI_ARG ) ) command.append( " npm start" );
		else {
			command.append( " java -cp /app/biolockj/dist/BioLockJ.jar" );
			if (getFlag( EXT_MODS_ARG )) command.append( ":" + EXTERNAL_MODULES_CONTAINER_DIR + "/*" );
			command.append( " " + JavaLaunchProcess.BioLockJ_CLASS );
			command.append( getOptionVals() );
		}
		return command.toString();
	}
		
	private String getOptionVals() throws DockerVolCreationException{
		StringBuilder options = new StringBuilder();
		if (getFlag( PASSWORD_ARG )) options.append( " " + RuntimeParamUtil.PASSWORD + " " + getParameter( PASSWORD_ARG ) );
		if (getFlag( AWS_ARG )) options.append( " " + RuntimeParamUtil.AWS_FLAG );
		if (getFlag( PRECHECK_ARG ) || getFlag( UNUSED_PROPS_ARG )) options.append( " " + RuntimeParamUtil.PRECHECK_FLAG );
		if (getFlag( UNUSED_PROPS_ARG )) options.append( " " + RuntimeParamUtil.UNUSED_PROPS_FLAG );
		if (getFlag( DEBUG_ARG )) options.append( " " + RuntimeParamUtil.DEBUG_FLAG );
		if ( DockerUtil.inDockerEnv() ) {
			options.append( " " + RuntimeParamUtil.BLJ_PROJ_DIR + " " + DockerUtil.deContainerizePath(BLJ_PROJ_DIR.getAbsolutePath()) );
			if (getFlag( RESTART_ARG )) {
				options.append( " " + RuntimeParamUtil.RESTART_DIR + " " + DockerUtil.deContainerizePath(getRestartDir()).getAbsolutePath() );
			}
			if ( getConfigFile() != null ) {
				options.append( " " + RuntimeParamUtil.CONFIG_FILE + " " + DockerUtil.deContainerizePath(getConfigFile()).getAbsolutePath() );
			}
		}else {
			options.append( " " + RuntimeParamUtil.BLJ_PROJ_DIR + " " + BLJ_PROJ_DIR.getAbsolutePath() );
			if (getFlag( RESTART_ARG )) {
				options.append( " " + RuntimeParamUtil.RESTART_DIR + " " + getRestartDir().getAbsolutePath() );
			}
			if ( getConfigFile() != null ) {
				options.append( " " + RuntimeParamUtil.CONFIG_FILE + " " + getConfigFile().getAbsolutePath() );
			}
		}
		return options.toString();
	}
	
	private String getVolumes() throws Exception {
		String EFS = "/mnt/efs";
		StringBuilder volumes = new StringBuilder();
		volumes.append( " -v /var/run/docker.sock:/var/run/docker.sock" );
		if (getFlag(BLJ_ARG)) volumes.append( addVolume(BLJ_DIR.getAbsolutePath(), DockerUtil.CONTAINER_BLJ_DIR) );
		if (getFlag(EXT_MODS_ARG)) volumes.append( addVolume(getParameter( EXT_MODS_ARG ), EXTERNAL_MODULES_CONTAINER_DIR) );
		if (getFlag(AWS_ARG)) {
			//TODO - replace this; docker mounts will done the same way on aws or local machine
			volumes.append( addVolume(EFS, EFS) );
		}else {
			volumes.append( addVolume( BLJ_PROJ_DIR.getAbsolutePath(), DockerUtil.DOCKER_PIPELINE_DIR ));
			if( getFlag(GUI_ARG)) {
				//# TODO - use aws cli in container, don't map in user's
				// TODO - convert from bash if needed
				//vols="${vols} -v ${BLJ}/resources/config/gui:${BLJ_CONFIG}"
				//[ -f "$(which aws)" ] && [ -d "$(dirname $(which aws))" ] && vols="${vols} -v $(dirname $(which aws)):${APP_BIN}"
			}else {
				int i = 1;
				File readableConfig = getConfigFile();
				List<String> configuredDirs = BioLockJ_API.listMounts( readableConfig.getAbsolutePath() );
				for (String dir : configuredDirs) {
					volumes.append( addVolume(dir, EFS + "/vol_" + i) );
					i++;
				}
			}
		}
		return volumes.toString();
	}
	
	private String addVolume(String source, String target) throws DockerVolCreationException {
		return " --mount type=bind,source=" + DockerUtil.deContainerizePath( source ) + ",target=" + target;
	}
	
	private String getDockerImg() {
		return "biolockjdevteam/biolockj_controller:" + DockerUtil.getDefaultImageTag();
	}
		

	@Override
	void runCommand() throws Exception {
		String cmd = createCmd();
		if (inTestMode()) {System.out.println( LaunchProcess.BIOLOCKJ_TEST_MODE_VALUE + " " + cmd); throw new EndLaunch( 0 );}
		
		super.runCommand();
		
		if (getFlag( FG_ARG )) System.out.println( " ---------> Docker CMD [ " + cmd + " ]" );
		
		try {
			final Process p = Runtime.getRuntime().exec( cmd ); 
			if (getFlag( FG_ARG )) {
				watchProcess(p);
			}else {
				String containerId = catchFirstResponse(p);
				System.out.println("Docker container id: " + containerId);
				confirmStart(containerId);
				printInfo();
			}
		} catch(EndLaunch el) {
			throw el;
		} catch( final Exception ex ) {
			System.err.println( "Problem occurred running command: " + cmd);
			ex.printStackTrace();
		}
	}
	
	private void confirmStart(String container) throws InterruptedException, IOException{
		System.out.print( "Initializing BioLockJ" );
		int i=0;
		int maxtime=25;
		if (DockerUtil.inDockerEnv()) maxtime=120; //launch from docker can be sluggish
		
		while( (i < maxtime || getFlag( WAIT_ARG )) && 
						! foundNewPipeline() &&
						isContainerRunning(container) &&
						! restartDirHasStatusFlag() ) {
			System.out.print( "." );
			i++;
			mostRecent = getMostRecentPipeline();
			if (i==10) System.out.print( "waiting for program to start" );
			if (i==maxtime) {
				if (getFlag( WAIT_ARG )) System.out.print("The normal timeout has been disabled.");
				else System.out.print("Reached max wait time: " + maxtime + " seconds.");
			}
			Thread.sleep( 1000 );
		}
		Thread.sleep( 1000 );
		System.out.print( "." );
		Thread.sleep( 1000 );
		System.out.print( "." );
		mostRecent = getMostRecentPipeline();// could lag slightly after program start			
		setPipedir( mostRecent );
	}
	
	private boolean isContainerRunning(String container) throws IOException {
		String inspectCmd = "docker inspect -f '{{.State.Running}}' " + container + " 2>/dev/null";
		final Process p = Runtime.getRuntime().exec( inspectCmd ); 
		return catchFirstResponse(p) == "true";
	}
	
//	# Start the local browswer
	private static void startBrowser() {
		//TODO: conver from bash to java (if needed)
//		sleep 2 # See also: https://stackoverflow.com/questions/3124556/clean-way-to-launch-the-web-browser-from-shell-script#3124750
//		if which xdg-open > /dev/null; then
//			xdg-open http://localhost:${GUI_PORT}
//		elif which gnome-open > /dev/null; then
//			gnome-open http://localhost:${GUI_PORT}
//		elif which python > /dev/null; then
//			python -mwebbrowser http://localhost:${GUI_PORT}
//		else
//			echo "Web browser not found on localhost!"
//		fi
	}

}
