package biolockj.launch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import biolockj.Constants;
import biolockj.exception.BioLockJException;
import biolockj.exception.BioLockJStatusException;
import biolockj.exception.DockerVolCreationException;
import biolockj.exception.InvalidPipelineException;
import biolockj.pipelines.PipelineUtil;
import biolockj.util.BioLockJUtil;
import biolockj.util.DockerUtil;

public class Reset {
	
	private File myDir;
	private File pipeDir = null;
	private File resetFromModule = null;
	
	public Reset(String dirPath) throws InvalidPipelineException, BioLockJStatusException {
		myDir = new File(dirPath);
		if ( ! myDir.exists() ) throw new InvalidPipelineException( "The pipeline to restart must already exist. Cannot find: [" + myDir.getAbsolutePath() + "]." );
		setPipelineDir(myDir);
		setResetModule();
	}
	
	/**
	 * Reset a pipeline.
	 * @param args - first element must be the path to the pipeline to reset or to the specific module to reset.
	 * @throws EndLaunch
	 * @throws BioLockJStatusException
	 * @throws IOException
	 * @throws DockerVolCreationException 
	 */
	public static void main(String[] args) throws EndLaunch, BioLockJStatusException, IOException, DockerVolCreationException {
		if ( args.length == 0 ) {
			for (String line : getHelpLines() ) System.out.println(line);
			System.exit( 0 );
		}
		if ( args.length > 1 ) {
			System.out.println( "The reset process takes exactly one argument.  Found " + args.length + " args." );
			for (String line : getHelpLines() ) System.out.println(line);
			System.exit( 1 );
		}
		try {
			resetPipeline( DockerUtil.containerizePath( args[0] ) );
		}catch(BioLockJException be) {
			System.out.println( be.getMessage());
			System.exit( 1 );
		}
		
	}
	
	public static boolean resetPipeline(String path) throws BioLockJStatusException, IOException, InvalidPipelineException {
		System.out.println( "Starting reset process... " );
		Reset reset = new Reset(path);
		
		reset.resetModules();
		reset.resetPipelineRoot();
		
		System.out.println( "Reset complete." );
		
		return true;
	}
	
	private void setPipelineDir(final File mydir) throws InvalidPipelineException {	
		if (PipelineUtil.isPipelineDir( mydir )) pipeDir = mydir;
		else if (PipelineUtil.isPipelineDir( mydir.getParentFile() ) ) {
			pipeDir = mydir.getParentFile();
			resetFromModule = mydir;
		}
		else throw new InvalidPipelineException( "Error: The given dir [" + mydir.getAbsolutePath() + "] is not a pipeline or module root directory." );
		System.out.println("Reseting pipeline: " + pipeDir.getAbsolutePath() + (resetFromModule==null ? "":" Starting from module: " + resetFromModule.getName()));;
	}
	
	private void setResetModule() throws BioLockJStatusException {
		if (resetFromModule == null) {
			int lastCompleted = -1;
			int firstIncomplete = 1000;
			for (File modDir : pipeDir.listFiles()) {
				if (PipelineUtil.isModuleDir(modDir)) {
					int currentNum = getModuleNumber(modDir);
					String flag = PipelineUtil.getPipelineStatusFlag( modDir ).getName();
					if (currentNum > lastCompleted && 
									flag.equals( Constants.BLJ_COMPLETE )) {
						lastCompleted = currentNum;
					}
					if ( currentNum < firstIncomplete && ! flag.equals( Constants.BLJ_COMPLETE )) {
						firstIncomplete = currentNum;
						resetFromModule = modDir;
					}
				}
			}
			if ( lastCompleted >= firstIncomplete ) throw new BioLockJStatusException( "Error: Could not determined last completed module." );
		}
		if (resetFromModule == null) {
			System.out.println("No modules to reset; all are complete.");
			System.out.println("If you would like to force a reset from a completed module, use: blj_reset <full/path/to/module/dir>");
		}else {
			System.out.println("Reseting module " + resetFromModule.getName() + " and any subsequent modules...");
		}
	}

	private void resetModules() throws BioLockJStatusException, IOException {
		if( resetFromModule != null ) {
			for( File modDir: pipeDir.listFiles() ) {
				if( PipelineUtil.isModuleDir( modDir ) &&
					getModuleNumber( modDir ) >= getModuleNumber( resetFromModule ) ) {
					PipelineUtil.markStatus( modDir.getAbsolutePath(), Constants.BLJ_FAILED );
				}
			}
		}

	}

	private int getModuleNumber(File modDir) {
		return Integer.valueOf( modDir.getName().substring( 0, modDir.getName().indexOf( '_' ) ) ).intValue();
	}
	
	private void resetPipelineRoot() {
		String hadFlag = PipelineUtil.clearStatus( pipeDir );
		System.out.println("Removed status flag: " + hadFlag );
	}
	
	private static List<String> getHelpLines(){
		List<String> lines = new ArrayList<>();
		lines.add( "Reset a BioLockJ pipeline" );
		lines.add( "BioLockJ version " + BioLockJUtil.getVersion() );
		lines.add( "A pipeline that has completed can be reset so that a restart is possible." );
		lines.add( "Completed modules an be reset so they will be re-run.  See the -r option in `biolockj help`." );
		lines.add( "" );
		lines.add( "Usage:" );
		lines.add( "java -cp ${BLJ}/dist/BioLockJ.jar biolockj.launch.Reset <resetDir>" );
		lines.add( "" );
		lines.add( "In most cases, <resetDir> is the path to the pipeline to reset." );
		lines.add( "If <resetDir> is a specific module, then that module and all subsequent modules are set to status \"" + Constants.BLJ_FAILED + "\"");
		lines.add( "and if the pipeline is restarted then this will be the first module to run." );
		lines.add( "" );
		return lines;
	}

}
