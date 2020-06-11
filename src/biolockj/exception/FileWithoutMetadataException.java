package biolockj.exception;

import java.io.File;
import java.util.List;
import biolockj.Constants;
import biolockj.util.BioLockJUtil;
import biolockj.util.MetaUtil;

public class FileWithoutMetadataException extends MetadataException {

	public FileWithoutMetadataException( File file ) {
		super( createMsg(file) );
	}
	
	private static String createMsg(File file) {
		StringBuilder sb = new StringBuilder();
		sb.append( "No metadata found for file [" + file + "]" + Constants.RETURN );
		List<String> ids = MetaUtil.getSampleIds();
		if ( !ids.isEmpty() ) sb.append( "Current sample names in metadata: " + Constants.RETURN +
			BioLockJUtil.printLongFormList( MetaUtil.getSampleIds() ) + Constants.RETURN);
		else {
			sb.append( "There are no samples in the current metadata." + Constants.RETURN);
		}
		sb.append( "This check can be disabled by setting [" + MetaUtil.META_REQUIRED + "=" + Constants.FALSE + "]." );
		sb.append( howToLinkMetaWithFile );
		return sb.toString();
	}

}
