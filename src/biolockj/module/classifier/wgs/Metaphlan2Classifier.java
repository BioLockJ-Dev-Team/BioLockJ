/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 9, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.classifier.wgs;

import java.io.File;
import java.util.*;
import biolockj.Config;
import biolockj.Constants;
import biolockj.Log;
import biolockj.Properties;
import biolockj.api.ApiModule;
import biolockj.exception.*;
import biolockj.module.classifier.ClassifierModuleImpl;
import biolockj.util.*;

/**
 * This BioModule builds the bash scripts used to execute metaphlan2.py to classify WGS sequences with MetaPhlAn2.
 * 
 * @blj.web_desc MetaPhlAn2 Classifier
 */
public class Metaphlan2Classifier extends ClassifierModuleImpl implements ApiModule {
	public Metaphlan2Classifier() {
		super();
		addNewProperty( EXE_METAPHLAN, Properties.EXE_PATH, "" );
		addNewProperty( EXE_METAPHLAN_PARAMS, Properties.LIST_TYPE, "additional parameters to use with metaphlan2" );
		addNewProperty( METAPHLAN2_DB, Properties.FILE_PATH, "Directory containing alternate database. Must always be paired with " + METAPHLAN2_MPA_PKL );
		addNewProperty( METAPHLAN2_MPA_PKL, Properties.FILE_PATH, "path to the mpa_pkl file used to reference an alternate DB. Must always be paired with " + METAPHLAN2_DB );
	}

	/**
	 * Build bash script lines to classify unpaired WGS reads with Metaphlan. The inner list contains 1 bash script line
	 * per sample.
	 * <p>
	 * Example line:<br>
	 * python /app/metaphlan2.py --nproc 8 -t rel_ab_w_read_stats --input_type fasta ./input/sample42.fasta --bowtie2out
	 * ./temp/sample42.bowtie2.bz2 &gt; ./output/sample42_reported.tsv
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception {
		final List<List<String>> data = new ArrayList<>();
		for( final File file: files ) {
			final String fileId = SeqUtil.getSampleId( file );
			final String outputFile = getOutputDir().getAbsolutePath() + File.separator + fileId + Constants.PROCESSED;
			final String bowtie2Out = getTempDir().getAbsolutePath() + File.separator + fileId + BOWTIE_EXT;
			final ArrayList<String> lines = new ArrayList<>();
			lines.add( FUNCTION_RUN_METAPHLAN + " " + file.getAbsolutePath() + " " + bowtie2Out + " " + outputFile );
			data.add( lines );
		}

		return data;
	}

	/**
	 * Build bash script lines to classify paired WGS reads with Metaphlan. The inner list contains 1 bash script line
	 * per sample.
	 * <p>
	 * Example line:<br>
	 * python /app/metaphlan2.py --nproc 8 -t rel_ab_w_read_stats --input_type fasta ./input/sample42_R1.fasta,
	 * ./input/sample42_R2.fasta --bowtie2out ./temp/sample42.bowtie2.bz2 &gt; ./output/sample42_reported.tsv
	 */
	@Override
	public List<List<String>> buildScriptForPairedReads( final List<File> files ) throws Exception {
		final List<List<String>> data = new ArrayList<>();
		final Map<File, File> map = SeqUtil.getPairedReads( files );
		for( final File file: map.keySet() ) {
			final String fileId = SeqUtil.getSampleId( file );
			final String outputFile = getOutputDir().getAbsolutePath() + File.separator + fileId + Constants.PROCESSED;
			final String bowtie2Out = getTempDir().getAbsolutePath() + File.separator + fileId + BOWTIE_EXT;
			final ArrayList<String> lines = new ArrayList<>();
			lines.add( FUNCTION_RUN_METAPHLAN + " " + file.getAbsolutePath() + "," + map.get( file ).getAbsolutePath() +
				" " + bowtie2Out + " " + outputFile );
			data.add( lines );
		}

		return data;
	}

	/**
	 * Verify none of the derived command line parameters are included in
	 * {@link biolockj.Config}.{@value #EXE_METAPHLAN}{@value biolockj.Constants#PARAMS}
	 */
	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		getParams();

		if( getMpaDB() != null && getMpaPkl() == null ) throw new ConfigViolationException( METAPHLAN2_DB,
			"Alternate MetaPhlAn2 DBs require this (currently undefined) Config property: " + ALT_DB_PARAM );

		if( getMpaPkl() != null && getMpaDB() == null ) throw new ConfigViolationException( ALT_DB_PARAM,
			"MetaPhlAn2 mpa pickle files requires the (currently undefined) Config property: " + METAPHLAN2_DB );

	}

	/**
	 * Metaphlan runs python scripts, so no special command is required
	 */
	@Override
	public String getClassifierExe() throws ConfigException {
		return Config.getExe( this, EXE_METAPHLAN );
	}

	/**
	 * Obtain the metaphlan2 runtime params
	 * @throws DockerVolCreationException 
	 */
	@Override
	public List<String> getClassifierParams() throws ConfigException, DockerVolCreationException {
		final List<String> params = Config.getList( this, EXE_METAPHLAN_PARAMS );
		if( getMpaDB() != null ) params.add( ALT_DB_PARAM + " " + getMpaDB().getAbsolutePath() );
		return params;
	}

	@Override
	public File getDB() throws ConfigPathException, ConfigNotFoundException, DockerVolCreationException {
		if ( DockerUtil.inDockerEnv() && Config.getString( this, METAPHLAN2_DB ) == null) {
			return new File( DEFAULT_DB_IN_DOCKER );
		}
		return Config.requireExistingDir( this, METAPHLAN2_DB );
	}

	@Override
	public List<String> getWorkerScriptFunctions() throws Exception {
		final List<String> lines = super.getWorkerScriptFunctions();
		lines.add( "function " + FUNCTION_RUN_METAPHLAN + "() {" );
		lines.add( getClassifierExe() + getWorkerFunctionParams() + "$1 --bowtie2out $2 > $3" );
		lines.add( "}" + RETURN );
		return lines;
	}

	/**
	 * Metaphlan queries require standard parameters: --input_type, --nproc, -t<br>
	 * Verify no invalid runtime params are passed and add rankSwitch if needed.<br>
	 * Set the rankSwitch based on the {@link biolockj.Config}.{@value biolockj.Constants#REPORT_TAXONOMY_LEVELS} if
	 * only one taxonomy level is to be reported, otherwise report all levels.
	 *
	 * @return runtime parameters
	 * @throws Exception if errors occur
	 */
	protected String getParams() throws Exception {
		if( this.defaultSwitches == null ) {
			final String params = BioLockJUtil.join( getClassifierParams() );

			if( params.indexOf( "--input_type " ) > -1 )
				throw new Exception( "Invalid classifier option (--input_type) found in property (" +
					EXE_METAPHLAN_PARAMS + "). BioLockJ derives this value by examinging one of the input files." );
			if( params.indexOf( NUM_THREADS_PARAM ) > -1 ) throw new Exception( "Ignoring nvalid classifier option (" +
				NUM_THREADS_PARAM + ") found in property (" + EXE_METAPHLAN_PARAMS +
				"). BioLockJ derives this value from property: " + Constants.SCRIPT_NUM_THREADS );
			if( params.indexOf( "--bowtie2out " ) > -1 )
				throw new Exception( "Invalid classifier option (--bowtie2out) found in property (" +
					EXE_METAPHLAN_PARAMS + "). BioLockJ outputs bowtie2out files to Metaphlan2Classifier/temp." );
			if( params.indexOf( "-t rel_ab_w_read_stats " ) > -1 )
				throw new Exception( "Invalid classifier option (-t rel_ab_w_read_stats). BioLockJ hard codes this " +
					"option for MetaPhlAn so must not be included in the property file." );
			if( params.indexOf( "--tax_lev " ) > -1 ) throw new Exception(
				"Invalid classifier option (--tax_lev) found in property (" + EXE_METAPHLAN_PARAMS +
					"). BioLockJ sets this value based on: " + Constants.REPORT_TAXONOMY_LEVELS );
			if( params.indexOf( "-s " ) > -1 )
				throw new Exception( "Invalid classifier option (-s) found in property (" + EXE_METAPHLAN_PARAMS +
					"). SAM output not supported.  BioLockJ outputs " + TSV_EXT + " files." );
			if( params.indexOf( "-o " ) > -1 )
				throw new Exception( "Invalid classifier option (-o) found in property (" + EXE_METAPHLAN_PARAMS +
					"). BioLockJ outputs results to: " + getOutputDir().getAbsolutePath() + File.separator );

			this.defaultSwitches =
				getRuntimeParams( getClassifierParams(), NUM_THREADS_PARAM ) + "-t rel_ab_w_read_stats ";

			if( TaxaUtil.getTaxaLevels().size() == 1 )
				this.defaultSwitches += "--tax_lev " + this.taxaLevelMap.get( TaxaUtil.getTaxaLevels().get( 0 ) ) + " ";
		}

		return this.defaultSwitches;
	}

	private File getMpaDB() throws ConfigPathException, ConfigNotFoundException, DockerVolCreationException {
		return getDB();
	}

	private File getMpaPkl() throws Exception {
		final String path = Config.getString( this, METAPHLAN2_MPA_PKL );
		if( path == null ) return null;
		if( DockerUtil.inDockerEnv() ) return getDB();
		return Config.requireExistingFile( this, METAPHLAN2_MPA_PKL );
	}

	private String getWorkerFunctionParams() throws Exception {
		return " " + getParams() + INPUT_TYPE_PARAM + SeqUtil.getSeqType() + " ";
	}

	private String defaultSwitches = null;

	private final Map<String, String> taxaLevelMap = new HashMap<>();

	{
		this.taxaLevelMap.put( Constants.SPECIES, METAPHLAN_SPECIES );
		this.taxaLevelMap.put( Constants.GENUS, METAPHLAN_GENUS );
		this.taxaLevelMap.put( Constants.FAMILY, METAPHLAN_FAMILY );
		this.taxaLevelMap.put( Constants.ORDER, METAPHLAN_ORDER );
		this.taxaLevelMap.put( Constants.CLASS, METAPHLAN_CLASS );
		this.taxaLevelMap.put( Constants.PHYLUM, METAPHLAN_PHYLUM );
		this.taxaLevelMap.put( Constants.DOMAIN, METAPHLAN_DOMAIN );
	}
	
	@Override
	public String getDockerImageOwner() {
		return Constants.MAIN_DOCKER_OWNER;
	}
	
	@Override
	public String getDockerImageName() {
		if (Config.getString( this, METAPHLAN2_DB ) != null )
			return "metaphlan2_classifier_dbfree";
		else
			return "metaphlan2_classifier";
	}
	
	@Override
	public String getDockerImageTag() {
		return "v1.3.18";
	}
	
	@Override
	public String getDescription() {
		return "Classify WGS samples with [MetaPhlAn2](http://bitbucket.org/biobakery/metaphlan2).";
	}

	@Override
	public String getCitationString() {
		return "MetaPhlAn2 for enhanced metagenomic taxonomic profiling. Duy Tin Truong, Eric A Franzosa, Timothy L Tickle, Matthias Scholz, George Weingart, Edoardo Pasolli, Adrian Tett, Curtis Huttenhower & Nicola Segata. Nature Methods 12, 902-903 (2015)";
	}
	
	private static final String DEFAULT_DB_IN_DOCKER = "/mnt/efs/db";
	
	/**
	 * {@link biolockj.Config} exe property used to obtain the metaphlan2 executable
	 */
	protected static final String EXE_METAPHLAN = "exe.metaphlan2";

	/**
	 * {@link biolockj.Config} List property used to obtain the metaphlan2 executable params
	 */
	protected static final String EXE_METAPHLAN_PARAMS = "metaphlan2.metaphlan2Params";

	/**
	 * {@link biolockj.Config} Directory property containing alternate database: {@value #METAPHLAN2_DB}<br>
	 * Must always be paired with {@value #METAPHLAN2_MPA_PKL}
	 */
	protected static final String METAPHLAN2_DB = "metaphlan2.db";

	/**
	 * {@link biolockj.Config} File property containing path to the mpa_pkl file used to reference an alternate DB
	 * {@value #METAPHLAN2_MPA_PKL}<br>
	 * Must always be paired with {@value #METAPHLAN2_DB}
	 */
	protected static final String METAPHLAN2_MPA_PKL = "metaphlan2.mpa_pkl";
	private static final String ALT_DB_PARAM = "--mpa_pkl";
	private static final String BOWTIE_EXT = ".bowtie2.bz2";
	private static final String FUNCTION_RUN_METAPHLAN = "runMetaphlan";
	private static final String INPUT_TYPE_PARAM = "--input_type ";
	private static final String METAPHLAN_CLASS = "c";
	private static final String METAPHLAN_DOMAIN = "k";
	private static final String METAPHLAN_FAMILY = "f";
	private static final String METAPHLAN_GENUS = "g";
	private static final String METAPHLAN_ORDER = "o";
	private static final String METAPHLAN_PHYLUM = "p";
	private static final String METAPHLAN_SPECIES = "s";
	private static final String NUM_THREADS_PARAM = "--nproc";

}
