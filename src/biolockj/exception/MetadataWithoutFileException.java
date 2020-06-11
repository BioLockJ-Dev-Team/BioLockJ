package biolockj.exception;

import biolockj.Constants;
import biolockj.util.MetaUtil;

public class MetadataWithoutFileException extends MetadataException {
	
	public MetadataWithoutFileException( String sample ) {
		super( createMsg(sample) );
	}
	
	private static String createMsg(String sample) {
		StringBuilder sb = new StringBuilder();
		sb.append( "No data file found for sample [" + sample + "]" + Constants.RETURN );
		sb.append( "This check can be disabled by setting [" + MetaUtil.USE_EVERY_ROW + "=" + Constants.FALSE + "]." );
		sb.append( howToLinkMetaWithFile );
		return sb.toString();
	}
}
