package biolockj.launch;

import java.io.File;
import java.io.IOException;
import biolockj.Constants;
import biolockj.exception.BioLockJStatusException;
import biolockj.exception.DockerVolCreationException;
import biolockj.util.DockerUtil;
import biolockj.util.PipelineUtil;

public class Reset {
	
	private File myDir;
	private File pipeDir = null;
	private File resetFromModule = null;
	
	public Reset(String dirPath) throws EndLaunch {
		myDir = new File(dirPath);
		if ( ! myDir.exists() ) throw new EndLaunch( "The pipeline to restart must already exist. Cannot find: [" + myDir.getAbsolutePath() + "]." );
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
		if ( args.length != 1 ) throw new EndLaunch( "The reset process requires the path to the pipeline to reset as an argument.");
		resetPipeline( DockerUtil.containerizePath( args[0] ) );
	}
	
	public static void resetPipeline(String path) throws EndLaunch, BioLockJStatusException, IOException {
		System.out.println( "Starting reset process... " );
		Reset reset = new Reset(path);
		
		reset.resetModules();
		reset.resetPipelineRoot();
		
		System.out.println( "Reset complete." );
		
	}
	
	private void setPipelineDir(final File mydir) throws EndLaunch{	
		if (PipelineUtil.isPipelineDir( mydir )) pipeDir = mydir;
		else if (PipelineUtil.isPipelineDir( mydir.getParentFile() ) ) {
			pipeDir = mydir.getParentFile();
			resetFromModule = mydir;
		}
		else throw new EndLaunch( "Error: The given dir [" + mydir.getAbsolutePath() + "] is not a pipeline or module root directory." );
		System.out.println("Reseting pipeline: " + pipeDir.getAbsolutePath() + (resetFromModule==null ? "":" Starting from module: " + resetFromModule.getName()));;
	}
	
	private void setResetModule() throws EndLaunch {
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
			if ( lastCompleted >= firstIncomplete ) throw new EndLaunch( "Error: Could not determined last completed module." );
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

}
