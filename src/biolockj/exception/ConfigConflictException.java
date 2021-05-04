package biolockj.exception;

public class ConfigConflictException extends ConfigException {

	public ConfigConflictException( String msg ) {
		super( msg );
	}

	public ConfigConflictException( String property, String msg ) {
		super( property, msg );
	}
	
	public ConfigConflictException( String[] props, String msg ) {
		super( buildConflictMsg(props, msg) );
	}
	
	protected static String buildConflictMsg(String[] props, String msg) {
		StringBuilder sb = new StringBuilder();
		for (String prop : props) {
			sb.append( showValue(prop) );
		}
		if (msg != null) sb.append( RETURN + msg );
		else sb.append( RETURN + "These property values are incompatible." );
		sb.append( promptChange() );
		return sb.toString();
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
}
