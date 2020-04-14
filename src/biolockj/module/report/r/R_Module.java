/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 18, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.report.r;

import java.io.*;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import biolockj.*;
import biolockj.exception.ConfigPathException;
import biolockj.exception.ConfigViolationException;
import biolockj.exception.DockerVolCreationException;
import biolockj.exception.SpecialPropertiesException;
import biolockj.module.ScriptModuleImpl;
import biolockj.module.report.humann2.AddMetadataToPathwayTables;
import biolockj.module.report.taxa.AddMetadataToTaxaTables;
import biolockj.util.*;

/**
 * This BioModule is the superclass for R script generating modules.
 */
public abstract class R_Module extends ScriptModuleImpl {
	
	public R_Module() {
		super();
		addGeneralProperty( Constants.EXE_RSCRIPT );
		addGeneralProperty( Constants.R_TIMEOUT );
		addGeneralProperty( Constants.R_DEBUG );
		addGeneralProperty( Constants.R_SAVE_R_DATA );
		addGeneralProperty( Constants.R_COLOR_FILE );
		addGeneralProperty( Constants.DEFAULT_STATS_MODULE );
	}
	
	/**
	 * Run R script in docker.
	 * 
	 * @return Bash script lines for the docker script
	 */
	public List<List<String>> buildDockerBashScript() {
		final List<List<String>> dockerScriptLines = new ArrayList<>();
		final List<String> innerList = new ArrayList<>();
		innerList.add( FUNCTION_RUN_R + " " + getPrimaryScript().getAbsolutePath() );
		dockerScriptLines.add( innerList );
		return dockerScriptLines;
	}

	/**
	 * Not needed for R script modules.
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception {
		return null;
	}

	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		Config.getExe( this, Constants.EXE_RSCRIPT );
		Config.getPositiveInteger( this, Constants.R_TIMEOUT );
		Config.getBoolean( this, Constants.R_DEBUG );
		Config.getBoolean( this, Constants.R_SAVE_R_DATA );
		verifyColorFileFormat();

	}

	/**
	 * Builds an R script by calling sub-methods to builds the BaseScript and creates the MAIN script shell that sources
	 * the BaseScript, calls runProgram(), reportStatus() and main() which can only be implemented in a subclass.<br>
	 */
	@Override
	public void executeTask() throws Exception {
		writePrimaryScript();
		if( DockerUtil.inDockerEnv() ) BashScriptBuilder.buildScripts( this, buildDockerBashScript() );
	}

	/**
	 * If running Docker, run the Docker bash script, otherwise:<br>
	 * Run {@link biolockj.Config}.{@value #EXE_RSCRIPT} command on the generated R Script:
	 * {@link ScriptModuleImpl#getMainScript()}.
	 */
	@Override
	public String[] getJobParams() {
		Log.info( getClass(), "Run MAIN Script: " + getMainScript().getName() );
		if( DockerUtil.inDockerEnv() ) return super.getJobParams();
		final String[] cmd = new String[ 2 ];
		cmd[ 0 ] = getRscriptCmd();
		cmd[ 1 ] = getMainScript().getAbsolutePath();
		return cmd;
	}

	/**
	 * Get the Module script
	 * 
	 * @return Module R script
	 * @throws Exception if errors occur
	 */
	public File getModuleScript() throws Exception {
		final File rFile = new File( getRTemplateDir() + getModuleScriptName() );
		if( !rFile.isFile() ) throw new Exception( "Missing R module script: " + rFile.getAbsolutePath() );
		return rFile;
	}

	/**
	 * Require combined count-metadata tables as input.
	 */
	@Override
	public List<String> getPreRequisiteModules() throws Exception {
		final List<String> preReqs = new ArrayList<>();
		if( !BioLockJUtil.pipelineInputType( BioLockJUtil.PIPELINE_R_INPUT_TYPE ) )
			preReqs.add( getMetaMergedModule() );
		preReqs.addAll( super.getPreRequisiteModules() );
		return preReqs;
	}

	/**
	 * Get the primary R script
	 * 
	 * @return File (R script)
	 */
	public File getPrimaryScript() {
		return new File(
			getScriptDir().getAbsolutePath() + File.separator + MAIN_SCRIPT_PREFIX + getModuleScriptName() );
	}

	/**
	 * Produce summary file counts for each file extension in the output directory and the number of log files in the
	 * temp directory. Any R Script errors detected during execution will also be printed. also contain details of R
	 * script errors, if any.
	 */
	@Override
	public String getSummary() throws Exception {
		final StringBuffer sb = new StringBuffer();
		try {

			final Map<String, Integer> map = new HashMap<>();
			for( final File file: getOutputDir().listFiles() )
				if( !file.isFile() ) continue;
				else if( file.getName().indexOf( "." ) > -1 ) {
					final String ext = file.getName().substring( file.getName().lastIndexOf( "." ) + 1 );
					if( map.get( ext ) == null ) map.put( ext, 0 );

					map.put( ext, map.get( ext ) + 1 );
				} else {
					if( map.get( "none" ) == null ) map.put( "none", 0 );

					map.put( "none", map.get( "none" ) + 1 );
				}

			final File rScript = getPrimaryScript();
			if( DockerUtil.inAwsEnv() && ( rScript == null || !rScript.isFile() ) )
				sb.append( "Failed to generate R Script!" + RETURN );
			else {
				sb.append( getClass().getSimpleName() + ( getErrors().isEmpty() ? " successful": " failed" ) + ": " +
					rScript.getAbsolutePath() + RETURN );

				for( final String ext: map.keySet() )
					sb.append( "Generated " + map.get( ext ) + " " + ext + " files" + RETURN );

				if( Config.getBoolean( this, Constants.R_DEBUG ) ) {
					final IOFileFilter ff = new WildcardFileFilter( "*" + LOG_EXT );
					final Collection<File> debugLogs = FileUtils.listFiles( getTempDir(), ff, null );
					sb.append( "Generated " + debugLogs.size() + " log files" + RETURN );
				}

				sb.append( getErrors() );
			}

		} catch( final Exception ex ) {
			final String msg = "Unable to produce " + getClass().getName() + " summary : " + ex.getMessage();
			Log.warn( getClass(), msg );
			sb.append( msg + RETURN );
			ex.printStackTrace();
		}

		return sb.toString();
	}

	/**
	 * The R Script should run quickly, timeout = 10 minutes appears to work well.
	 */
	@Override
	public Integer getTimeout() {
		try {
			return Config.getPositiveInteger( this, Constants.R_TIMEOUT );
		} catch( final Exception ex ) {
			Log.error( getClass(), ex.getMessage(), ex );
		}
		return null;
	}

	/**
	 * This method generates the bash function that calls the R script: runScript.
	 */
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception {
		final List<String> lines = super.getWorkerScriptFunctions();
		lines.add( "function " + FUNCTION_RUN_R + "() {" );
		lines.add( Config.getExe( this, Constants.EXE_RSCRIPT ) + " $1" );
		lines.add( "}" + RETURN );
		return lines;
	}

	/**
	 * Get correct meta-merged BioModule type for the give module. This is determined by examining previous configured
	 * modules to see what type of raw count tables are generated.
	 * 
	 * @return MetaMerged BioModule
	 * @throws Exception if errors occur
	 */
	protected String getMetaMergedModule() throws Exception {
		if( PathwayUtil.useHumann2RawCount( this ) ) return AddMetadataToPathwayTables.class.getName();
		return AddMetadataToTaxaTables.class.getName();
	}

	/**
	 * Add {@link biolockj.module.report.r.R_CalculateStats} to standard {@link #getPreRequisiteModules()}
	 * 
	 * @return Statistics Module prerequisite if needed
	 * @throws Exception if errors occur determining eligibility
	 */
	protected List<String> getStatPreReqs() throws Exception {
		final List<String> preReqs = super.getPreRequisiteModules();
		if( !BioLockJUtil.pipelineInputType( BioLockJUtil.PIPELINE_STATS_TABLE_INPUT_TYPE ) )
			preReqs.add( Config.getString( null, Constants.DEFAULT_STATS_MODULE ) );
		return preReqs;
	}

	/**
	 * Initialize the R script by creating the MAIN R script that calls source on the BaseScript and adds the R code for
	 * the runProgarm() method.
	 *
	 * @throws Exception if unable to build the R script stub
	 */
	protected void writePrimaryScript() throws Exception {
		getTempDir();
		getOutputDir();
		FileUtils.copyFile( getMainR(), getPrimaryScript() );
		FileUtils.copyFileToDirectory( getFunctionLib(), getScriptDir() );
		FileUtils.copyFileToDirectory( getModuleScript(), getScriptDir() );
	}

	private String getErrors() throws Exception {
		final IOFileFilter ff = new WildcardFileFilter( "*" + Constants.SCRIPT_FAILURES );
		final Collection<File> scriptsFailed = FileUtils.listFiles( getScriptDir(), ff, null );
		if( scriptsFailed.isEmpty() ) return "";
		final String rSpacer = "-------------------------------------";
		final StringBuffer errors = new StringBuffer();

		errors.append( INDENT + rSpacer + RETURN );
		errors.append( INDENT + "R Script Errors:" + RETURN );
		final BufferedReader reader = BioLockJUtil.getFileReader( scriptsFailed.iterator().next() );
		try {
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
				errors.append( INDENT + line + RETURN );
		} finally {
			if( reader != null ) reader.close();
		}
		errors.append( INDENT + rSpacer + RETURN );
		return errors.toString();
	}

	private String getModuleScriptName() {
		return getClass().getSimpleName() + Constants.R_EXT;
	}

	private String getRscriptCmd() {
		try {
			return Config.getExe( this, Constants.EXE_RSCRIPT );
		} catch( final SpecialPropertiesException ex ) {
			Log.error( getClass(), Constants.EXE_RSCRIPT + " property misconfigured", ex );
		}
		return Constants.RSCRIPT;
	}

	private void verifyColorFileFormat() throws ConfigPathException, IOException, ConfigViolationException, DockerVolCreationException {
		final File colorFile = Config.getExistingFile( this, Constants.R_COLOR_FILE );
		if( colorFile != null ) {
			final BufferedReader br = BioLockJUtil.getFileReader( colorFile );
			final String[] header = br.readLine().split( TAB_DELIM );
			br.close();
			if( !header[ 0 ].equals( "key" ) || !header[ 1 ].equals( "color" ) )
				throw new ConfigViolationException( Constants.R_COLOR_FILE,
					"The color reference file [ " + colorFile.getAbsolutePath() +
						" ] should be a tab-delimited file, column labels: \"key\" and \"color\"." + RETURN +
						"Tip: use a color reference file generated by BioLockJ as a template." );
		}
	}

	/**
	 * Get the main R script
	 * 
	 * @return Main R script
	 * @throws Exception if errors occur
	 */
	public static File getMainR() throws Exception {
		final File rFile = new File( getRTemplateDir() + Constants.R_MAIN_SCRIPT );
		if( !rFile.isFile() ) throw new Exception( "Missing R function library: " + rFile.getAbsolutePath() );
		return rFile;
	}

	/**
	 * Get the BioLockJ resource R directory.
	 * 
	 * @return System file path
	 * @throws Exception if errors occur
	 */
	public static String getRTemplateDir() throws Exception {
		return BioLockJUtil.getBljDir().getAbsolutePath() + File.separator + "resources" + File.separator + "R" +
			File.separator;
	}

	/**
	 * This method generates an R script with the given rCode saved to the given path.
	 *
	 * @param path Path to new R Script
	 * @param rCode R script code
	 * @throws Exception if I/O errors occur
	 */
	public static void writeNewScript( final String path, final String rCode ) throws Exception {
		final BufferedWriter writer = new BufferedWriter( new FileWriter( path ) );
		try {
			writeScript( writer, rCode );
		} finally {
			writer.close();
		}
	}

	/**
	 * This method formats the rCode to indent code blocks surround by curly-braces "{ }"
	 * 
	 * @param writer BufferedWriter writes to the file
	 * @param rCode R code
	 * @throws Exception if I/O errors occur
	 */
	protected static void writeScript( final BufferedWriter writer, final String rCode ) throws Exception {
		int indentCount = 0;
		final StringTokenizer st = new StringTokenizer( rCode, RETURN );
		while( st.hasMoreTokens() ) {
			final String line = st.nextToken();

			if( line.equals( "}" ) ) indentCount--;

			int i = 0;
			while( i++ < indentCount )
				writer.write( INDENT );

			writer.write( line + RETURN );

			if( line.endsWith( "{" ) ) indentCount++;
		}
	}

	public static File getFunctionLib() throws Exception {
		final File rFile = new File( getRTemplateDir() + Constants.R_FUNCTION_LIB );
		if( !rFile.isFile() ) throw new Exception( "Missing R function library: " + rFile.getAbsolutePath() );

		return rFile;
	}
	
	@Override
	public String getDockerImageName() {
		return "r_module";
	}

	private static final String FUNCTION_RUN_R = "runScript";
	private static final String INDENT = "   ";
}
