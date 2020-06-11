/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date June 20, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.classifier.wgs;

import java.io.File;
import java.util.*;
import biolockj.*;
import biolockj.Properties;
import biolockj.api.ApiModule;
import biolockj.exception.*;
import biolockj.module.classifier.ClassifierModuleImpl;
import biolockj.util.*;

/**
 * This BioModule runs biobakery humann2 program to generate the HMP Unified Metabolic Analysis Network<br>
 * HUMAnN is a pipeline for efficiently and accurately profiling the presence/absence and abundance of microbial
 * pathways in a community from metagenomic or metatranscriptomic sequencing data (typically millions of short DNA/RNA
 * reads). This process, referred to as functional profiling, aims to describe the metabolic potential of a microbial
 * community and its members. More generally, functional profiling answers the question "What are the microbes in my
 * community-of-interest doing (or capable of doing)?
 *
 * For more information, please review the BioBakery instruction manual:
 * <a href= "https://bitbucket.org/biobakery/humann2" target="_top">https://bitbucket.org/biobakery/humann2</a><br>
 * 
 * @blj.web_desc HumanN2 Classifier
 */
public class Humann2Classifier extends ClassifierModuleImpl implements ApiModule {

	public Humann2Classifier() {
		super();
		addNewProperty( EXE_HUMANN2, Properties.EXE_PATH, "" );//description for exe's is generated in super method
		addNewProperty( EXE_HUMANN2_JOIN_PARAMS, Properties.LIST_TYPE, EXE_HUMANN2_JOIN_PARAMS_DESC );
		addNewProperty( EXE_HUMANN2_PARAMS, Properties.LIST_TYPE, EXE_HUMANN2_PARAMS_DESC );
		addNewProperty( EXE_HUMANN2_RENORM_PARAMS, Properties.LIST_TYPE, EXE_HUMANN2_RENORM_PARAMS_DESC );
		addNewProperty( HN2_NUCL_DB, Properties.FILE_PATH, HN2_NUCL_DB_DESC );
		addNewProperty( HN2_PROT_DB, Properties.FILE_PATH, HN2_PROT_DB_DESC );
	}

	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception {
		final List<List<String>> data = new ArrayList<>();
		Log.debug( getClass(), "Total # worker scripts = " + ModuleUtil.getNumWorkers( this ) );
		for( final File file: files ) {
			final List<String> lines = new ArrayList<>();
			File hn2InputSeq = file;
			if( doDownloadDB() ) lines.add( FUNCTION_DOWNLOAD_DB );
			else if( waitForDownloadDBs() ) lines.add( FUNCTION_BLOCK_FOR_DBS );

			if( SeqUtil.hasPairedReads() ) {
				lines.add( getPairedReadLine( file ) );
				hn2InputSeq = getMergedReadFile( file );
			}

			lines.add( FUNCTION_RUN_HN2 + " " + hn2InputSeq.getAbsolutePath() );

			// #copying metaphlan2 tables into metaphlan2_output
			// find . -iname *list.tsv -exec cp '{}' ./metaphlan2_output \;
			// #Merging metaphlan2 files
			// humann2_join_tables -i metaphlan2_output -o metaphlan2.tsv --file_name bugs_list

			if( data.size() == files.size() - 1 ) {
				Log.warn( getClass(), "Total samples = " + files.size() + " and data has " + data.size() +
					" samples added so far, so add function to build summary table as last line of last script which will be added to buildScripts() data list" );
				lines.add( FUNCTION_BUILD_SUMMARY_TABLES );
			}
			data.add( lines );
		}

		return data;
	}

	@Override
	public List<List<String>> buildScriptForPairedReads( final List<File> files ) throws Exception {
		final List<File> sortedFiles = new ArrayList<>( getPairedReads().keySet() );
		Collections.sort( sortedFiles );
		return buildScript( sortedFiles );
	}

	/**
	 * Verify that none of the derived command line parameters are included in
	 * {@link biolockj.Config}.{@value #EXE_HUMANN2}{@value biolockj.Constants#PARAMS}. Also verify:
	 * <ul>
	 * <li>{@link biolockj.Config}.{@value #HN2_NUCL_DB} is a valid directory
	 * <li>{@link biolockj.Config}.{@value #HN2_PROT_DB} is a valid directory
	 * </ul>
	 */
	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		getRuntimeParams();
		getParams( EXE_HUMANN2_JOIN_PARAMS );
		getParams( EXE_HUMANN2_RENORM_PARAMS );
		PathwayUtil.verifyConfig( this );
		getDB();
	}

	@Override
	public void cleanUp() throws Exception {
		super.cleanUp();
		this.pairedReads = null;
	}

	/**
	 * Calling super to build scripts, then wait until any database downloads are complete before allowing the Pipeline
	 * to move forward and use these DB.
	 */
	@Override
	public void executeTask() throws Exception {
		super.executeTask();
		try {
			for( final Long id: threadRegister )
				while( Processor.subProcAlive( id ) ) {
					Log.warn( getClass(),
						"Humann2 classifier scripts are ready, waiting on database downloads to complete" );
					Thread.sleep( BioLockJUtil.minutesToMillis( 1 ) );
				}
		} catch( final InterruptedException ex ) {
			Log.error( getClass(), "Error occurred waiting for HumanN2 Download-DB subprocess to compelete!",
				ex );
		}
	}

	/**
	 * Get HumanN2 executable command: {@value #EXE_HUMANN2}
	 */
	@Override
	public String getClassifierExe() throws ConfigException {
		return Config.getExe( this, EXE_HUMANN2 );
	}

	/**
	 * Obtain the humann2 runtime params
	 */
	@Override
	public List<String> getClassifierParams() throws ConfigException {
		final List<String> res = new ArrayList<>();
		for( final String val: Config.getList( this, EXE_HUMANN2_PARAMS ) )
			if( val.startsWith( INPUT_PARAM ) || val.startsWith( LONG_INPUT_PARAM ) || val.startsWith( OUTPUT_PARAM ) ||
				val.startsWith( LONG_OUTPUT_PARAM ) )
				Log.warn( getClass(),
					"Ignore runtime option [ " + val + " ] set in Config property: " + EXE_HUMANN2_PARAMS +
						" because this value is set by BioLockJ at runtime based on pipeline context" );
			else if( val.startsWith( NUCL_DB_PARAM ) )
				Log.warn( getClass(), "Ignore runtime option [ " + val + " ] set in Config property: " +
					EXE_HUMANN2_PARAMS + " because this value is set by BioLockJ Config property: " + HN2_NUCL_DB );
			else if( val.startsWith( PROT_DB_PARAM ) )
				Log.warn( getClass(), "Ignore runtime option [ " + val + " ] set in Config property: " +
					EXE_HUMANN2_PARAMS + " because this value is set by BioLockJ Config property: " + HN2_PROT_DB );
			else res.add( val );
		return res;
	}

	@Override
	public File getDB() throws ConfigNotFoundException, ConfigPathException, DockerVolCreationException {
		if( getDbCache() != null ) return getDbCache();
		setDbCache( BioLockJUtil.getCommonParent( new File( getNuclDbPath() ), new File( getProtDbPath() ) ) );
		return getDbCache();
	}

	@Override
	public String getSummary() throws Exception {
		final StringBuffer sb = new StringBuffer();
		try {
			sb.append( "HumanN2 nucleotide DB: " + getNuclDbPath() + RETURN );
			sb.append( "HumanN2 protein DB: " + getProtDbPath() + RETURN );
		} catch( final Exception ex ) {
			final String msg = "Unable to complete module summary: " + ex.getMessage();
			sb.append( msg + RETURN );
			Log.warn( getClass(), msg );
		}

		return super.getSummary() + sb.toString();
	}

	@Override
	public List<String> getWorkerScriptFunctions() throws Exception {
		final List<String> lines = super.getWorkerScriptFunctions();
		if( doDownloadDB() ) {
			lines.addAll( downloadDbFunction() );
			lines.addAll( blockForDbsFunction() );
		} else if( waitForDownloadDBs() ) lines.addAll( blockForDbsFunction() );
		if( SeqUtil.hasPairedReads() ) lines.addAll( concatPairedReadFunction() );
		lines.addAll( runHn2Function() );
		lines.addAll( joinTableFunction() );
		lines.addAll( renormTableFunction() );
		lines.addAll( buildSummaryFunction() );
		this.workerID++;
		return lines;
	}

	/**
	 * Get formatted runtime parameters and {@value Constants#SCRIPT_NUM_THREADS}
	 *
	 * @return Formatted runtime switches
	 * @throws Exception if errors occur
	 */
	protected String getRuntimeParams() throws Exception {
		return getRuntimeParams( getClassifierParams(), NUM_THREADS_PARAM ) + RM_STRATIFIED_OUTPUT + " " +
			NUCL_DB_PARAM + " " + getNuclDbPath() + " " + PROT_DB_PARAM + " " + getProtDbPath() + " ";
	}

	private List<String> blockForDbsFunction() throws ConfigNotFoundException {
		final List<String> lines = new ArrayList<>();
		lines.add( BLOCK_FOR_DB_COMMENT );
		lines.add( "function " + FUNCTION_BLOCK_FOR_DBS + "() {" );
		lines.add( "count=0" );
		lines.add( "while [ ! -f \"" + getDbFlag( HN2_PROT_DB, Constants.BLJ_COMPLETE ).getAbsolutePath() +
			"\" ] || [ ! -f \"" + getDbFlag( HN2_NUCL_DB, Constants.BLJ_COMPLETE ).getAbsolutePath() + "\" ]; do" );
		lines.add( "sleep 60 && let \"count++\"" );
		lines.add( "[ ${count} -gt " + getTimeOut() + " ] && echo \"Failed to download HumanN2 DBs after " +
			getTimeOut() + " minutes\" && exit 1" );
		lines.add( "done" );
		lines.add( "}" + RETURN );
		return lines;
	}

	private List<String> buildSummaryFunction() throws Exception {
		final List<String> lines = new ArrayList<>();
		lines.add( BUILD_SUMMARY_BASH_COMMENT );
		lines.add( "function " + FUNCTION_BUILD_SUMMARY_TABLES + "() {" );
		lines.add( "count=0 && numStarted=1 && numComplete=0" );
		lines.add( "while [ $numStarted != $numComplete ]; do " );
		lines.add( "numStarted=$(ls \"" + getScriptDir().getAbsolutePath() + File.separator + "\"*" +
			Constants.SCRIPT_STARTED + " | wc -l)" );
		lines.add( "numComplete=$(ls \"" + getScriptDir().getAbsolutePath() + File.separator + "\"*" +
			Constants.SCRIPT_SUCCESS + " | wc -l)" );
		lines.add( "let \"numComplete++\"" );
		lines.add( "[ $numStarted != $numComplete ] && sleep 60" );
		lines.add( "[ ${count} -gt " + getTimeOut() + " ] && echo \"Failed to build HumanN2 summary tables after " +
			getTimeOut() + " minutes\" && sleep 15 && exit 1" );
		lines.add( "done" );
		if( !Config.getBoolean( this, Constants.HN2_DISABLE_PATH_ABUNDANCE ) ) {
			lines.add( getJoinTableLine( HN2_PATH_ABUNDANCE ) );
			lines.add( getRenormTableLine( HN2_PATH_ABUNDANCE, Constants.HN2_PATH_ABUND_SUM ) );
		}
		if( !Config.getBoolean( this, Constants.HN2_DISABLE_PATH_COVERAGE ) ) {
			lines.add( getJoinTableLine( HN2_PATH_COVERAGE ) );
			lines.add( getRenormTableLine( HN2_PATH_COVERAGE, Constants.HN2_PATH_COVG_SUM ) );
		}
		if( !Config.getBoolean( this, Constants.HN2_DISABLE_GENE_FAMILIES ) ) {
			lines.add( getJoinTableLine( HN2_GENE_FAMILIES ) );
			lines.add( getRenormTableLine( HN2_GENE_FAMILIES, Constants.HN2_GENE_FAM_SUM ) );
		}
		lines.add( "}" + RETURN );
		return lines;
	}

	private List<String> concatPairedReadFunction() {
		final List<String> lines = new ArrayList<>();
		lines.add( "function " + FUNCTION_CONCAT_PAIRED_READS + "() {" );
		lines.add( "cat $1 $2 > " + getTempSubDir( TEMP_MERGE_READ_DIR ).getAbsolutePath() + File.separator + "$3" );
		lines.add( "}" + RETURN );
		return lines;
	}

	private boolean doDownloadDB() {
		return !dlHn2DBs.isEmpty() && this.workerID < dlHn2DBs.size() &&
			this.workerID <= Collections.max( dlHn2DBs.values() );
	}

	private String downloadDB( final String prop ) throws ConfigNotFoundException {
		final String[] parts = Config.requireString( this, prop ).split( "\\s" );
		final File db = new File( parts[ 2 ] + File.separator + parts[ 0 ] );
		if( db.isDirectory() ) return db.getAbsolutePath();
		if( DockerUtil.inAwsEnv() ) dlHn2DBs.put( prop, dlHn2DBs.size() );
		else {
			final String[] args = new String[ parts.length + 2 ];
			args[ 0 ] = DOWNLOAD_DB_CMD;
			args[ 1 ] = DL_DB_SWITCH;
			args[ 2 ] = parts[ 0 ]; // <database>
			args[ 3 ] = parts[ 1 ]; // <build>
			args[ 4 ] = parts[ 2 ]; // <install_location>
			threadRegister.add( DockerUtil.downloadDB( args, "Download HumanN2 DB" ) );
		}

		return db.getAbsolutePath();
	}

	private List<String> downloadDbFunction() throws ConfigNotFoundException, ConfigFormatException {
		final List<String> lines = new ArrayList<>();
		final boolean dlNuclDB = dlHn2DBs.get( HN2_NUCL_DB ) != null &&
			( ModuleUtil.getNumWorkers( this ) == 1 || dlHn2DBs.get( HN2_NUCL_DB ) == this.workerID );
		final boolean dlProtDB = dlHn2DBs.get( HN2_PROT_DB ) != null &&
			( ModuleUtil.getNumWorkers( this ) == 1 || dlHn2DBs.get( HN2_PROT_DB ) == this.workerID );
		lines.add( DOWNLOAD_DB_COMMENT );
		lines.add( "function " + FUNCTION_DOWNLOAD_DB + "() {" );
		if( dlNuclDB ) lines.addAll( downloadDbLines( HN2_NUCL_DB ) );
		if( dlProtDB ) lines.addAll( downloadDbLines( HN2_PROT_DB ) );
		if( !dlNuclDB && !dlProtDB || dlHn2DBs.size() == 2 && !dlNuclDB || !dlProtDB )
			lines.add( FUNCTION_BLOCK_FOR_DBS );
		lines.add( "}" + RETURN );
		return lines;
	}

	private List<String> downloadDbLines( final String prop ) throws ConfigNotFoundException {
		final List<String> lines = new ArrayList<>();
		final String[] db = Config.requireString( this, prop ).split( "\\s" );
		lines.add( "touch " + getDbFlag( prop, Constants.BLJ_STARTED ).getAbsolutePath() );
		lines.add( DOWNLOAD_DB_CMD + " " + DL_DB_SWITCH + " " + db[ 0 ] + " " + db[ 1 ] + " " + db[ 2 ] );
		lines.add( "touch " + getDbFlag( prop, Constants.BLJ_COMPLETE ).getAbsolutePath() );
		return lines;
	}

	private File getDbFlag( final String prop, final String status ) throws ConfigNotFoundException {
		final String[] db = Config.requireString( this, prop ).split( "\\s" );
		return new File( getTempDir().getAbsolutePath() + File.separator + "DB_" + db[ 0 ] + "_" + status );
	}

	// private int getBatchNum( final int sampleCount ) throws ConfigNotFoundException, ConfigFormatException {
	// final int numWorkers = Config.requirePositiveInteger( this, ScriptModule.SCRIPT_NUM_WORKERS );
	// return new Double( Math.ceil( new Double( sampleCount ) / new Double( numWorkers ) ) ).intValue();
	// }

	private String getJoinTableLine( final String key ) {
		return FUNCTION_JOIN_HN2_TABLES + " " + getTempSubDir( FUNCTION_RUN_HN2 ) + " " +
			summaryFile( getTempSubDir( JOIN_OUT_DIR ), key ) + " " + key;
	}

	private File getMergedReadFile( final File file ) throws Exception {
		return new File( getTempSubDir( TEMP_MERGE_READ_DIR ).getAbsoluteFile() + File.separator +
			SeqUtil.getSampleId( file ) + BioLockJUtil.fileExt( file ) );
	}

	private String getNuclDbPath() throws ConfigNotFoundException, ConfigPathException, DockerVolCreationException {
		if( nuclDbCache != null ) return nuclDbCache;
		final String prop = Config.requireString( this, HN2_NUCL_DB );
		if( DockerUtil.inDockerEnv() ) {
			if( isDirPath( prop ) ) nuclDbCache = prop;
			else nuclDbCache = downloadDB( HN2_NUCL_DB );
		} else nuclDbCache = Config.requireExistingDir( this, HN2_NUCL_DB ).getAbsolutePath();
		return nuclDbCache;
	}

	private String getPairedReadLine( final File file )
		throws Exception {
		return FUNCTION_CONCAT_PAIRED_READS + " " + file.getAbsolutePath() + " " +
			getPairedReads().get( file ).getAbsolutePath() + " " + SeqUtil.getSampleId( file ) +
			BioLockJUtil.fileExt( file );
	}

	private Map<File, File> getPairedReads()
		throws Exception {
		if( this.pairedReads == null && SeqUtil.hasPairedReads() )
			this.pairedReads = SeqUtil.getPairedReads( getInputFiles() );
		return this.pairedReads;
	}

	private String getParams( final String property ) {
		String params = " ";
		for( final String val: Config.getList( this, property ) )
			if( val.startsWith( INPUT_PARAM ) || val.startsWith( LONG_INPUT_PARAM ) || val.startsWith( OUTPUT_PARAM ) ||
				val.startsWith( LONG_OUTPUT_PARAM ) )
				Log.warn( getClass(), "Ignore runtime option [ " + val + " ] set in Config property: " + property +
					" because this value is set by BioLockJ at runtime based on pipeline context" );
			else params = val + " ";

		return params;
	}

	private String getProtDbPath() throws ConfigNotFoundException, ConfigPathException, DockerVolCreationException {
		if( protDbCache != null ) return protDbCache;
		final String prop = Config.requireString( this, HN2_PROT_DB );
		if( DockerUtil.inDockerEnv() ) {
			if( isDirPath( prop ) ) protDbCache = prop;
			else protDbCache = downloadDB( HN2_PROT_DB );
		} else protDbCache = Config.requireExistingDir( this, HN2_PROT_DB ).getAbsolutePath();
		return protDbCache;
	}

	private String getRenormTableLine( final String input, final String output ) {
		return FUNCTION_RENORM_HN2_TABLES + " " + summaryFile( getTempSubDir( JOIN_OUT_DIR ), input ) + " " +
			getOutputDir().getAbsolutePath() + File.separator + PathwayUtil.getHn2ClassifierOutput( output );
	}

	private File getTempSubDir( final String name ) {
		final File dir = new File( getTempDir().getAbsolutePath() + File.separator + name );
		if( !dir.isDirectory() ) dir.mkdirs();
		return dir;
	}

	private Integer getTimeOut() {
		Integer timeout = null;
		try {
			timeout = Config.getPositiveInteger( this, Constants.SCRIPT_TIMEOUT );
		} catch( final ConfigFormatException ex ) {
			ex.printStackTrace();
		}
		if( timeout == null ) timeout = 420;
		return timeout;
	}

	private List<String> joinTableFunction() throws ConfigException {
		final List<String> lines = new ArrayList<>();
		lines.add( JOIN_BASH_COMMENT );
		lines.add( "function " + FUNCTION_JOIN_HN2_TABLES + "() {" );
		lines.add( getClassifierExe() + JOIN_TABLE_CMD_SUFFIX + getParams( EXE_HUMANN2_JOIN_PARAMS ) + INPUT_PARAM +
			" $1 " + OUTPUT_PARAM + " $2 " + FILE_NAME_PARAM + " $3" );
		lines.add( "}" + RETURN );
		return lines;
	}

	private List<String> renormTableFunction() throws ConfigException {
		final List<String> lines = new ArrayList<>();
		lines.add( RENORM_BASH_COMMENT );
		lines.add( "function " + FUNCTION_RENORM_HN2_TABLES + "() {" );
		lines.add( getClassifierExe() + RENORM_TABLE_CMD_SUFFIX + getParams( EXE_HUMANN2_RENORM_PARAMS ) + INPUT_PARAM +
			" $1 " + OUTPUT_PARAM + " $2" );
		lines.add( "}" + RETURN );
		return lines;
	}

	private List<String> runHn2Function() throws Exception {
		final List<String> lines = new ArrayList<>();
		lines.add( HN2_BASH_COMMENT );
		lines.add( "function " + FUNCTION_RUN_HN2 + "() {" );
		lines.add( getClassifierExe() + " " + getRuntimeParams() + INPUT_PARAM + " $1 " + OUTPUT_PARAM + " " +
			getTempSubDir( FUNCTION_RUN_HN2 ) );
		lines.add( "}" + RETURN );
		return lines;
	}

	private static boolean isDirPath( final String val ) {
		if( !DockerUtil.inDockerEnv() ) return new File( val ).isDirectory();
		return val != null && !val.isEmpty() && !val.trim().contains( " " ) && val.contains( File.separator );
	}

	private static String summaryFile( final File dir, final String key ) {
		return dir + File.separator + Config.pipelineName() + "_" + key + TSV_EXT;
	}

	private static boolean waitForDownloadDBs() {
		return !dlHn2DBs.isEmpty();
	}
	
	@Override
	public String getDockerImageName() {
		return "humann2_classifier";
	}

	@Override
	public String getTitle() {
		return "HUMAnN2";
	}

	@Override
	public String getDescription() {
		return "Profile the presence/absence and abundance of microbial pathways in a community from metagenomic or metatranscriptomic sequencing data.";
	}

	@Override
	public String getCitationString() {
		return "Franzosa EA*, McIver LJ*, Rahnavard G, Thompson LR, Schirmer M, Weingart G, Schwarzberg Lipson K, Knight R, Caporaso JG, Segata N, Huttenhower C. "
						+ System.lineSeparator() + "Species-level functional profiling of metagenomes and metatranscriptomes. Nat Methods 15: 962-968 (2018)."
						+ System.lineSeparator() + "http://huttenhower.sph.harvard.edu/humann2"
						+ System.lineSeparator() + "BioLockJ module developed by Mike Siota";
	}
	
	@Override
	public Boolean isValidProp( String property ) throws Exception {
		Boolean isValid = super.isValidProp( property );
		switch(property) {
			case EXE_HUMANN2:
				isValid = Properties.isValidExeProp( this, EXE_HUMANN2 );
				break;
			case EXE_HUMANN2_JOIN_PARAMS:
				isValid = true;
				try {getParams( EXE_HUMANN2_JOIN_PARAMS );}
				catch(Exception e) { isValid=false; }
				break;
			case EXE_HUMANN2_RENORM_PARAMS:
				isValid = true;
				try {getParams( EXE_HUMANN2_RENORM_PARAMS );}
				catch(Exception e) { isValid=false; }
				break;
		}
		return isValid;
	}

	private Map<File, File> pairedReads = null;
	private int workerID = 0;

	/**
	 * {@link biolockj.Config} {@value biolockj.Properties#EXE_PATH} property for humnan2 executable: {@value #EXE_HUMANN2}
	 */
	protected static final String EXE_HUMANN2 = "exe.humann2";

	/**
	 * {@link biolockj.Config} {@value biolockj.Properties#LIST_TYPE} property: {@value #EXE_HUMANN2_JOIN_PARAMS}
	 * {@value #EXE_HUMANN2_JOIN_PARAMS_DESC}
	 */
	protected static final String EXE_HUMANN2_JOIN_PARAMS = "humann2.humann2JoinTableParams";
	private static final String EXE_HUMANN2_JOIN_PARAMS_DESC = "The parameters to be used with humann2_join_tables";

	/**
	 * {@link biolockj.Config} {@value biolockj.Properties#LIST_TYPE} property: {@value #EXE_HUMANN2_PARAMS}
	 * {@value #EXE_HUMANN2_PARAMS_DESC}
	 */
	protected static final String EXE_HUMANN2_PARAMS = "humann2.humann2Params";
	private static final String EXE_HUMANN2_PARAMS_DESC = "The humann2 executable params";

	/**
	 * {@link biolockj.Config} {@value biolockj.Properties#LIST_TYPE} property: {@value #EXE_HUMANN2_RENORM_PARAMS}
	 * {@value #EXE_HUMANN2_RENORM_PARAMS_DESC}
	 */
	protected static final String EXE_HUMANN2_RENORM_PARAMS = "humann2.humann2RenormTableParams";
	private static final String EXE_HUMANN2_RENORM_PARAMS_DESC = "The parameters to use with humann2_renorm_table";

	/**
	 * {@link biolockj.Config} {@value biolockj.Properties#FILE_PATH} property: {@value #HN2_NUCL_DB}
	 * {@value #HN2_NUCL_DB_DESC}
	 */
	protected static final String HN2_NUCL_DB = "humann2.nuclDB";
	private static final String HN2_NUCL_DB_DESC = "Directory containing the nucleotide database";

	/**
	 * {@link biolockj.Config} {@value biolockj.Properties#FILE_PATH} property: {@value #HN2_PROT_DB}
	 * {@value #HN2_PROT_DB_DESC}
	 */
	protected static final String HN2_PROT_DB = "humann2.protDB";
	private static final String HN2_PROT_DB_DESC = "Directory containing the protein nucleotide database";

	private static final String BLOCK_FOR_DB_COMMENT =
		"# Poll every 60 seconds for DB download complete indicator file";
	private static final String BUILD_SUMMARY_BASH_COMMENT =
		"# Wait until all worker scripts are complete to build summary tables";
	private static final String DL_DB_SWITCH = "--download";
	private static final Map<String, Integer> dlHn2DBs = new HashMap<>();
	private static final String DOWNLOAD_DB_CMD = "humann2_databases";
	private static final String DOWNLOAD_DB_COMMENT = "# Download configured DBs for nucleotide and/or protein DBs";
	private static final String FILE_NAME_PARAM = "--file_name";
	private static final String FUNCTION_BLOCK_FOR_DBS = "waitForDbDownload";
	private static final String FUNCTION_BUILD_SUMMARY_TABLES = "buildSummaryTables";
	private static final String FUNCTION_CONCAT_PAIRED_READS = "mergePairedReads";
	private static final String FUNCTION_DOWNLOAD_DB = "downloadDBs";
	private static final String FUNCTION_JOIN_HN2_TABLES = "joinHn2Tables";
	private static final String FUNCTION_RENORM_HN2_TABLES = "renormHn2Tables";
	private static final String FUNCTION_RUN_HN2 = "runHn2";
	private static final String HN2_BASH_COMMENT = "# Run sample through HMP Unified Metabolic Analysis Network";
	private static final String HN2_GENE_FAMILIES = "genefamilies";
	private static final String HN2_PATH_ABUNDANCE = "pathabundance";
	private static final String HN2_PATH_COVERAGE = "pathcoverage";
	private static final String INPUT_PARAM = "-i";
	private static final String JOIN_BASH_COMMENT = "# Pool data of a given type output for each individual sample";
	private static final String JOIN_OUT_DIR = "joined_tables";
	private static final String JOIN_TABLE_CMD_SUFFIX = "_join_tables";
	private static final String LONG_INPUT_PARAM = "--input";
	private static final String LONG_OUTPUT_PARAM = "--output";
	private static final String NUCL_DB_PARAM = "--nucleotide-database";
	private static String nuclDbCache = null;
	private static final String NUM_THREADS_PARAM = "--threads";
	private static final String OUTPUT_PARAM = "-o";
	private static final String PROT_DB_PARAM = "--protein-database";
	private static String protDbCache = null;
	private static final String RENORM_BASH_COMMENT = "# Renormalize output summary tables" + RETURN +
		"# Renorm unit options: counts/million (default) or relative abundance" + RETURN +
		"# Renorm mode options: community (default) or levelwise";
	private static final String RENORM_TABLE_CMD_SUFFIX = "_renorm_table";
	private static final String RM_STRATIFIED_OUTPUT = "--remove-stratified-output";
	private static final String TEMP_MERGE_READ_DIR = "merged";
	private static final Set<Long> threadRegister = new HashSet<>();
}
