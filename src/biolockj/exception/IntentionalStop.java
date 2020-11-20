package biolockj.exception;

/**
 * Parent exception for BioLockJ Exception that arise when the user explicitly intended to make the pipeline stop.
 * @author Ivory Blakley
 *
 */
public abstract class IntentionalStop extends BioLockJException {

	private static final long serialVersionUID = 1L;

	public IntentionalStop( String msg ) {
		super( msg );
	}

}
