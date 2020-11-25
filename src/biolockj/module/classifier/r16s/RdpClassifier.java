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
package biolockj.module.classifier.r16s;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import biolockj.*;
import biolockj.api.ApiModule;
import biolockj.exception.*;
import biolockj.module.classifier.ClassifierModuleImpl;
import biolockj.module.implicit.parser.r16s.RdpHierParser;
import biolockj.util.*;

/**
 * This BioModule uses RDP to assign taxonomy to 16s sequences.
 * 
 * @blj.web_desc RDP Classifier
 */
public class RdpClassifier extends ClassifierModuleImpl implements ApiModule {
	
	public RdpClassifier() {
		super();
		addNewProperty( RDP_DB, Properties.FILE_PATH, "File path used to define an alternate RDP database" );
		addNewProperty( RDP_JAR, Properties.FILE_PATH, "File path for RDP java executable JAR" );
		addNewProperty( RDP_DOCKER_JAR, Properties.STRING_TYPE, "File path for RDP java executable JAR in docker.", "/app/classifier.jar" );
		addNewProperty( RDP_PARAMS, Properties.LIST_TYPE, "parameters to use when running rdp. (must include \"-f fixrank\")" );
		addNewProperty( JAVA_PARAMS, Properties.LIST_TYPE, "the parameters to java when running rdp." );
		addNewProperty( HIER_COUNTS, Properties.BOOLEAN_TYPE, "Generate TaxaTables using the RDP " + HIER_PARAM + " option; uses the RdpHierParser instead of the standard RdpParser module." );
		addNewProperty( Constants.RDP_THRESHOLD_SCORE, Properties.NUMERTIC_TYPE, "IFF " + HIER_COUNTS + "=Y, RdpClassifier will ignore OTU assignments below this threshold score (0-100)", "80" );
		addGeneralProperty( Constants.DEFAULT_MOD_SEQ_MERGER );
	}

	/**
	 * Build bash script lines to classify unpaired reads with RDP. The inner list contains the bash script lines
	 * required to classify 1 sample (call java to run RDP jar on sample).
	 * <p>
	 * Example line: "java -jar $RDP_PATH t /database/silva128/rRNAClassifier.properties -o
	 * ./output/sample42.fasta_reported.tsv ./input/sample42.fasta"
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception {
		final List<List<String>> data = new ArrayList<>();
		for( final File file: files ) {
			final String outputFile = getOutputDir().getAbsolutePath() + File.separator +
				SeqUtil.getSampleId( file ) + Constants.PROCESSED;
			final ArrayList<String> lines = new ArrayList<>();
			String hier = Config.getBoolean( this, HIER_COUNTS ) ?
				" " + outputFile.replaceFirst( Constants.PROCESSED, HIER_SUFFIX ): "";
			lines.add( FUNCTION_RDP + " " + file.getAbsolutePath() + " " + outputFile + hier );
			data.add( lines );
		}

		return data;
	}

	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		if ( !DockerUtil.inDockerEnv() ) Config.requireExistingFile( this, RDP_JAR );
		getRuntimeParams( getClassifierParams(), null );
		getDbParam();
		Config.getBoolean( this, HIER_COUNTS );
		if ( Config.requirePositiveInteger( this, Constants.RDP_THRESHOLD_SCORE ) > 100) {
			throw new ConfigFormatException( Constants.RDP_THRESHOLD_SCORE, "Value cannot be greater than 100.");
		};
	}

	/**
	 * RDP uses java to run a JAR file, so no special command is required
	 */
	@Override
	public String getClassifierExe() throws ConfigException {
		return null;
	}

	/**
	 * Do not accept -t to define a database, since that instead requires the specific 
	 * property: {@value #RDP_DB} <br>
	 * If {@value #HIER_COUNTS} is true, then skip any parameter for it here, it will added per file.
	 */
	@Override
	public List<String> getClassifierParams() throws ConfigException {
		final List<String> validParams = new ArrayList<>();
		boolean skipHier = Config.getBoolean( this, HIER_COUNTS );
		for( final String param: Config.getList( this, RDP_PARAMS ) ) {
			if( param.startsWith( DB_PARAM ) || param.startsWith( "--train_propfile" ) ) {
				Log.warn( getClass(), "Ignoring " + DB_PARAM + " value: [ " + param + " ] set in Config property " +
					RDP_PARAMS + "since this property must be explictily defined in " + RDP_DB );
			}else if (skipHier && (param.startsWith(  HIER_PARAM ) || param.startsWith(  HIER_PARAM.substring( 1, 3 ) ) ) ){
				Log.info( getClass(),
					"Ignoring " + HIER_PARAM + " value: [ " + param + " ] set in Config property " + RDP_PARAMS +
						". The property " + HIER_COUNTS + " is set to Y, so the " + HIER_PARAM +
						" parameter will set with a different file for each call to RDP." );
			}else {
				validParams.add( param );
			}
		}

		return validParams;
	}

	@Override
	public File getDB() throws ConfigPathException, ConfigNotFoundException, DockerVolCreationException {
		if( Config.getString( this, RDP_DB ) != null ) {
			if( DockerUtil.inDockerEnv() ) return new File( Config.getString( this, RDP_DB ) );
			return Config.requireExistingFile( this, RDP_DB );
		}
		return null;
	}

	/**
	 * If paired reads found, add prerequisite: {@link biolockj.module.seq.PearMergeReads}.
	 */
	@Override
	public List<String> getPreRequisiteModules() throws Exception {
		final List<String> preReqs = new ArrayList<>();
		if( SeqUtil.hasPairedReads() ) preReqs.add( Config.getString( null, Constants.DEFAULT_MOD_SEQ_MERGER ) );
		preReqs.addAll( super.getPreRequisiteModules() );
		return preReqs;
	}

	/**
	 * This method generates the required bash functions: {@value #FUNCTION_RDP}
	 */
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception {
		final List<String> lines = super.getWorkerScriptFunctions();
		String hierParam = "";
		if (Config.getBoolean( this, HIER_COUNTS )) {
			hierParam = " " + HIER_PARAM + " $3 " + CONF_PARAM + " " + getConfThreshold() + " ";
		}
		lines.add( "function " + FUNCTION_RDP + "() {" );
		lines.add( Config.getExe( this, Constants.EXE_JAVA ) + " " + getJavaParams() + Constants.JAR_ARG + " " +
			getJar() + " " + getRuntimeParams( getClassifierParams(), null ) 
			+ getDbParam() + hierParam + OUTPUT_PARAM + " $2 $1" );
		lines.add( "}" + RETURN );
		return lines;
	}

	private String getDbParam() throws ConfigPathException, ConfigNotFoundException, DockerVolCreationException {
		if( getDB() == null ) return "";
		return DB_PARAM + " " + Config.requireExistingFile( this, RDP_DB ).getAbsolutePath() + " ";
	}

	private String getJar() throws Exception {
		String path;
		if ( DockerUtil.inDockerEnv() ) {
			path = Config.requireString( this, RDP_DOCKER_JAR );
		}else {
			path = Config.requireExistingFile( this, RDP_JAR ).getAbsolutePath();
		}
		return path;
	}

	private String getJavaParams() throws Exception {
		return Config.getExeParams( this, JAVA_PARAMS );
	}
	
	@Override
	public String getDockerImageName() {
		return "rdp_classifier";
	}
	
	String getConfThreshold() throws ConfigNotFoundException, ConfigFormatException {
		int val = Config.requirePositiveInteger( this, Constants.RDP_THRESHOLD_SCORE );
		if ( val > 100) {
			throw new ConfigFormatException( Constants.RDP_THRESHOLD_SCORE, "Value cannot be greater than 100.");
		};
		return (new DecimalFormat("0.00")).format( (float) val / 100 );
	}

	/**
	 * Name of the RdpClassifier bash script function used to assign taxonomy: {@value #FUNCTION_RDP}
	 */
	protected static final String FUNCTION_RDP = "runRdp";

	/**
	 * {@link biolockj.Config} File property used to define an alternate RDP database file: {@value #RDP_DB}
	 */
	protected static final String RDP_DB = "rdp.db";
	
	/**
	 * {@link biolockj.Config} File property for RDP java executable jar WHEN RUNNING IN DOCKER: {@value #RDP_DOCKER_JAR}
	 * This is the path in the default docker container.
	 */
	protected static final String RDP_DOCKER_JAR = "rdp.containerJar";

	/**
	 * {@link biolockj.Config} File property for RDP java executable JAR: {@value #RDP_JAR}
	 */
	protected static final String RDP_JAR = "rdp.jar";

	/**
	 * {@link biolockj.Config} List property for RDP java executable JAR runtime params: {@value #RDP_PARAMS}
	 */
	protected static final String RDP_PARAMS = "rdp.params";
	
	public static final String HIER_COUNTS = "rdp.hierCounts";
	
	public static final String HIER_SUFFIX = "_hierarchicalCount.tsv";
	
	/**
	 * {@link biolockj.Config} List property: {@value #JAVA_PARAMS}
	 * The parameters for the call to java when running rdp.
	 */
	protected static final String JAVA_PARAMS = "rdp.javaParams";

	private static final String DB_PARAM = "-t";
	private static final String OUTPUT_PARAM = "-o";
	private static final String HIER_PARAM = "--hier_outfile";
	private static final String CONF_PARAM = "--conf";
	
	@Override
	public String getDescription() {
		return "Classify 16s samples with [RDP](http://rdp.cme.msu.edu/classifier/classifier.jsp).";
	}

	@Override
	public String getCitationString() {
		return "Module developed by Mike Sioda" + System.lineSeparator() + "BioLockJ " + BioLockJUtil.getVersion();
	}

	@Override
	public List<String> getPostRequisiteModules() throws Exception {
		List<String> list = new ArrayList<>();
		if ( Config.getBoolean( this, HIER_COUNTS ) ) {
			list.add( RdpHierParser.class.getName() );
		}
		else list.addAll( super.getPostRequisiteModules() );
		return list;
	}

}
