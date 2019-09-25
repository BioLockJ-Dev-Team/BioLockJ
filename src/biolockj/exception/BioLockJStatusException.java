package biolockj.exception;

public class BioLockJStatusException extends BioLockJException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public BioLockJStatusException( String msg ) {
		super( msg );
		// TODO Auto-generated constructor stub
	}
	
	public BioLockJStatusException( ) {
		super( "BioLockJ either could not determine the status or could not set the status." );
		// TODO Auto-generated constructor stub
	}

}
