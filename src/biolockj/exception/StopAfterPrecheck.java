package biolockj.exception;

/**
 * When running in "precheck" mode, this is the mechanism that stops a pipeline from running.
 * The should ONLY be thrown at the end of the check dependencies phase, and ONLY if the user 
 * has supplied the precheck runtime parameter.
 * 
 * @author Ivory Blakley
 */
public class StopAfterPrecheck extends BioLockJException {

	public StopAfterPrecheck( String msg ) {
		super( msg );
	}
	
	public StopAfterPrecheck( ) {
		super( myMsg );
	}
	
	private static String myMsg = "This pipeline was configured to stop after checking dependencies.";
	
	private static final long serialVersionUID = 1L;

}
