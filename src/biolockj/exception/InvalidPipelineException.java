package biolockj.exception;

import java.io.File;

public class InvalidPipelineException extends BioLockJException {

	public InvalidPipelineException( String msg ) {
		super( msg );
	}
	
	public InvalidPipelineException( File pipeDir ) {
		super( makeMessage(pipeDir) );
	}
	
	private static String makeMessage(File pipeDir) {
		if (pipeDir==null) return "Error: [ null ] is not a valid pipeline.";
		return "Error: existing pipeline ["+pipeDir.getAbsolutePath()+"] is not a valid pipeline.";
	}
	
	private static final long serialVersionUID = 6278358497757933230L;

}
