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
import biolockj.module.getData.InputDataModule;
import biolockj.util.BioLockJUtil;
import biolockj.util.DockerUtil;

public class SraMetaData extends SequenceReadArchive implements ApiModule, InputDataModule {
	
	public SraMetaData() {
		super();
		addNewProperty( SRP, Properties.LIST_TYPE, SRP_DESC);
		addNewProperty( DB_DIR, Properties.FILE_PATH, DB_DIR_DESC);
		//addNewProperty( SRA_ACC_LIST, Properties.FILE_PATH, SRA_ACC_LIST_DESC );
		addGeneralProperty( EXE_PYSRADB );
	}

	@Override
	public String getDockerImageOwner() {
		return Constants.MAIN_DOCKER_OWNER;
	}
	
	@Override
	public String getDockerImageName() {
		return "pysradb";
	}
	
	@Override
	public String getDockerImageTag() {
		return "v1.3.18";
	}
	
	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		isValidProp(SRP);
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
		lines.add( Config.getExe( this, EXE_PYSRADB )+ " metadata --db $DB --detailed --expand --saveto $2 $1" );
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
		return "Extract metadata via pysradb from local copy of " + DB_NAME + ".";
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
	
	@Override
	public Set<String> getInputDataTypes() {
		Set<String> types = new TreeSet<String>();
		types.add( "sra metadata" );
		return types;
	}
	
	private static final String FUNCTION_NAME = "main";

	private final String EXE_PYSRADB = "exe.pysradb";
	
}
