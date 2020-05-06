package biolockj.exception;

import biolockj.Pipeline;
import biolockj.module.BioModule;
import biolockj.util.DockerUtil;

/**
 * Some module are run as secondary instances of the BioLockJ program.
 * There are run as script modules and the main program checks for flags in the modules directory.
 * If an exception is thrown, and the secondary instance of BioLockJ invokes the FatalExceptionHandler,
 * then the main program will throw this exception when it resumes.
 * This exception class is a cue to the FatalExceptionHandler, that some of its work may already be done.
 * @author ieclabau
 *
 */
public class DirectModuleException extends BioLockJException {

	public DirectModuleException( String msg ) {
		super( msg + addedMsg() );
	}
	
	public DirectModuleException( ) {
		super( "An error occurred during the execution of a direct module." + addedMsg() );
	}
	
	private static String addedMsg() {
		String msg = "";
		BioModule module = Pipeline.exeModule();
		if (module != null) {
			String logsDir;
			try {
				logsDir = ": " + DockerUtil.deContainerizePath( module.getLogDir().getAbsolutePath() );
			} catch( DockerVolCreationException e ) {
				e.printStackTrace();
				logsDir = ".";
			}
			msg = System.lineSeparator() + "More information may be available in the module's logs" + logsDir;
		}
		return msg;
	}
	
	private static final long serialVersionUID = 6927852512694273571L;

}
