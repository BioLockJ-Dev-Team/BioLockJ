package biolockj.module.getData.sra;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import biolockj.Config;
import biolockj.Constants;
import biolockj.Log;
import biolockj.Properties;
import biolockj.api.ApiModule;
import biolockj.exception.ConfigFormatException;
import biolockj.module.getData.InputData;
import biolockj.util.BioLockJUtil;
import biolockj.util.DockerUtil;

public class SraMetaData extends SequenceReadArchive implements ApiModule, InputData {
	
	public SraMetaData() {
		super();
		addNewProperty( SRP, Properties.LIST_TYPE, "The project id(s) referencesing a project in the NCBI SRA. example: SRP009633, ERP016051");
	}

	@Override
	public String getDockerImageName() {
		return "pysradb";
	}
	
	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		isValidProp(SRP);
	}
	
	@Override
	public Boolean isValidProp( String property ) throws Exception {
	    Boolean isValid = super.isValidProp( property );
	    switch(property) {
	        case SRP:
	        	List<String> ids = Config.getList( this, SRP );
	        	for (String SRP_ID : ids) {
	        		//check for SRP format, should be three letters followed by (I think) 6 numbers
	    			if ( ! SRP_ID.startsWith( "P", 2 ) || SRP_ID.length() != 9) {
	    				throw new ConfigFormatException( SRP, "SRA project id's are three letters followed by six numbers." );
	    			}
	    		}
	            isValid = true;
	            break;
	    }
	    return isValid;
	}

	@Override
	public List<List<String>> buildScript( List<File> files ) throws Exception {
		List<List<String>> outerList = new ArrayList<>();
		
		List<String> ids = Config.getList( this, SRP );
		for (String SRP_ID : ids) {
			List<String> innerList = new ArrayList<>();
			File outFile = new File(getOutputDir(), SRP_ID + "_metadata.tsv" );
			Log.info(this.getClass(), "Saving metadata for project [" + SRP_ID + "] to file: " + DockerUtil.deContainerizePath(outFile.getAbsolutePath()) );
			innerList.add( FUNCTION_NAME + " " + SRP_ID + " " + outFile.getAbsolutePath() );
			outerList.add( innerList );
		}		
		
		return outerList;
	}
	
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception {
		File dbFolder = Config.getExistingDir( this, DB_DIR );
		File dbFile = new File(dbFolder, DB_NAME);
		Log.info(this.getClass(), "Retrieving data from local database: " + DockerUtil.deContainerizePath(dbFile.getAbsolutePath()) );
		summary = summary + "Local database: " + DockerUtil.deContainerizePath( dbFile.getAbsolutePath() ) + Constants.RETURN;
		
		List<String> lines = super.getWorkerScriptFunctions();
		lines.add( "function " + FUNCTION_NAME + "() {" );
		lines.add( "echo Database: " + dbFile.getAbsolutePath() );
		lines.add( "echo Project ID: $1" );
		lines.add( "echo Saving table to file: $2" );
		lines.add( "DB=" + dbFile.getAbsolutePath() );
		lines.add( "pysradb metadata --db $DB --detailed --expand --saveto $2 $1" );
		lines.add( "}" );
		
		return lines;
	}
	
	@Override
	public List<String> getPreRequisiteModules() throws Exception {
		List<String> list = super.getPreRequisiteModules();
		list.add( SraMetaDB.class.getName() );
		return list;
	}
	
	@Override
	public String getSummary() throws Exception {
		return super.getSummary() + summary;
	}
	
	private String summary = "";

	@Override
	public String getDescription() {
		return "Extract and save metadata available via pysradb from copy of " + DB_NAME + ".";
	}
	
	@Override
	public String getDetails() {
		return "The **" + SraMetaDB.class.getName() + "** module is added a pre-requisite to ensure that the database is available.";
	}

	@Override
	public String getCitationString() {
		return "Module developed by Malcolm Zapatas and Ivory Blakley" + System.lineSeparator()
						+ "BioLockJ " + BioLockJUtil.getVersion();
	}

	private static final String SRP = "sraMetaData.SraProjectId";
	
	private static final String FUNCTION_NAME = "main";

	@Override
	public Set<String> getInputDataTypes() {
		Set<String> types = new TreeSet<String>();
		types.add( "sra metadata" );
		return types;
	}

}
