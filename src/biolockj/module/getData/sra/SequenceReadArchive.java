package biolockj.module.getData.sra;

import biolockj.Properties;
import biolockj.module.ScriptModuleImpl;

public abstract class SequenceReadArchive extends ScriptModuleImpl {
	
	public SequenceReadArchive() {
		super();
		addNewProperty( DB_DIR, Properties.FILE_PATH, "path to the directory where the *"+DB_NAME+"* database is stored.");
	}
	
	protected static final String DB_NAME = "SRAmetadb.sqlite";
	protected static final String DB_DIR = "sequenceReadArchive.metaDataDir";
}
