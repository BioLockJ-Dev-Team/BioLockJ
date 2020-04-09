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
import biolockj.exception.ConfigException;
import biolockj.exception.ConfigFormatException;
import biolockj.exception.ConfigPathException;
import biolockj.exception.DockerVolCreationException;
import biolockj.exception.SpecialPropertiesException;
import biolockj.module.WritesOutsidePipeline;
import biolockj.util.BioLockJUtil;
import biolockj.util.DockerUtil;

public class SraMetaDB extends SequenceReadArchive implements ApiModule, WritesOutsidePipeline {
	
	public SraMetaDB() {
		super();
		addNewProperty( DO_UPDATE, Properties.BOOLEAN_TYPE, DO_UPDATE_DESC, "N");
		addGeneralProperty( EXE_GUNZIP );
		addGeneralProperty( EXE_WGET );
	}

	@Override
	public String getDockerImageName() {
		return "blj_basic";
	}
	
	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		isValidProp(DO_UPDATE);
		isValidProp(DB_DIR);
	}
	
	@Override
	public Boolean isValidProp( String property ) throws Exception {
	    Boolean isValid = super.isValidProp( property );
	    switch(property) {
	        case DO_UPDATE:
	        	Config.getBoolean( this, DO_UPDATE );
	            isValid = true;
	            break;
	        case DB_DIR:
	        	File dbFolder = Config.requireExistingDir( this, DB_DIR );
	    		File dbFile = new File(dbFolder, DB_NAME);
	    		if ( dbFile.exists() && ! dbFile.canRead() ) {
	    			throw new ConfigException(DB_DIR, "The database [" + dbFile.getAbsolutePath() + "] exists, but is not readable.");
	    		}
	    		if ( ! dbFile.exists() && ! dbFolder.canWrite() ) {
	    			throw new ConfigException(DB_DIR, "The database [" + dbFile.getAbsolutePath() + "] does not exist, and this folder is not writable.");
	    		}
	        	isValid = true;
	        	break;
	    }
	    return isValid;
	}

	@Override
	public List<List<String>> buildScript( List<File> files ) throws Exception {
		List<List<String>> outerList = new ArrayList<>();
		List<String> innerList = new ArrayList<>();
		
		File dbFolder = Config.getExistingDir( this, DB_DIR );
		File dbFile = new File(dbFolder, DB_NAME);
		summary = summary + "Local database: " + DockerUtil.deContainerizePath( dbFile.getAbsolutePath() ) + Constants.RETURN;
		
		if ( dbFile.exists() && dbFile.canRead() ) {
			Log.info(this.getClass(), "The database [" + DockerUtil.deContainerizePath( dbFile.getAbsolutePath() ) + "] exists, and is readable.");
			innerList.add( "echo ' " + DB_NAME + " already exists. ' " );
			if ( Config.getBoolean( this, DO_UPDATE ) ) {
				innerList.add( "ls -l " + dbFile.getAbsolutePath() );
				innerList.add( "echo 'checking for a newer version...' " );
				innerList.addAll( downloadLines(dbFolder, dbFile) );
			}
		}else {
			Log.info(this.getClass(), "The database [" + DockerUtil.deContainerizePath( dbFile.getAbsolutePath() ) + "] does not exist; setting up download...");
			innerList.add( "echo downloading SRAmetadb.sqlite ");
			innerList.addAll( downloadLines(dbFolder, dbFile) );
		}
		innerList.add( "ls -l " + dbFile.getAbsolutePath() );
		outerList.add( innerList );
		return outerList;
	}
	
	private List<String> downloadLines(final File dbFolder, final File dbFile) throws ConfigFormatException, SpecialPropertiesException{
		summary = summary + "Reference database URL: " + DB_URL + Constants.RETURN;
		List<String> list = new ArrayList<>();
		list.add( "echo This will take up 30GB of space ");
		list.add( "cd " + dbFolder.getPath() );
		list.add( Config.getExe( this, EXE_WGET ) + " -Nc " + DB_URL );
		if ( Config.getBoolean( this, DO_UPDATE ) ) {
			list.add( "# The -k parameter retains the original ziped file, ");
			list.add( "# which allows the system to determine if the server version is newer than the local." );
			list.add( Config.getExe( this, EXE_GUNZIP ) + " -k " + dbFile.getPath() + ".gz" );
		}else {
			list.add( Config.getExe( this, EXE_GUNZIP ) + " " + dbFile.getPath() + ".gz");
		}
		return list;
	}
	
	@Override
	public String getSummary() throws Exception {
		return super.getSummary() + summary;
	}
	
	private String summary = "";
	
	@Override
	public String getDescription() {
		return "Makes sure that the SRAmetadb exists, downloads if it does not already exist.";
	}
	
	@Override
	public String getDetails() {
		return "If *" + DO_UPDATE + "* is set to Y, then the zipped form of the database is downloaded, and kept and used to compare " 
						+ "the local version to the server version; and the server version is downloaded if it is newer." + System.lineSeparator()
						+ "Server version location: " + DB_URL + System.lineSeparator()
						+ "*" + DB_DIR + "* directory must exist.  If the database does not exist at that location, it will be downloaded.";
	}

	@Override
	public String getCitationString() {
		return "Module developed by Malcolm Zapatas and Ivory Blakley" + System.lineSeparator()
						+ "BioLockJ " + BioLockJUtil.getVersion();
	}
	
	@Override
	public Set<String> getWriteDirs() throws DockerVolCreationException, ConfigPathException {
		Set<String> dirs = new TreeSet<>();
		File dbFolder = Config.getExistingDir( this, DB_DIR );
		dirs.add( DockerUtil.deContainerizePath( dbFolder.getAbsolutePath() ) );
		return dirs;
	}

	/**
	 * {@link biolockj.Config} property: {@value #DO_UPDATE}<br>
	 * {@value #DO_UPDATE_DESC}
	 */
	private final String DO_UPDATE = "sraMetaData.forceUpdate";
	private final String DO_UPDATE_DESC = "Y/N: download a newer verionsion if available.";
	
	private final String EXE_WGET = "exe.wget";
	private final String EXE_GUNZIP = "exe.gunzip";
	
	private final String DB_URL = "https://starbuck1.s3.amazonaws.com/sradb/SRAmetadb.sqlite.gz";

}
