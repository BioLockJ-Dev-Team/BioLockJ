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
package biolockj.util;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import biolockj.*;
import biolockj.Properties;
import biolockj.api.API_Exception;
import biolockj.exception.*;
import biolockj.module.*;
import biolockj.module.report.r.R_Module;

/**
 * This utility class generates the bash script files using the lines provided and {@link biolockj.Config} script
 * properties.
 */
public class BashScriptBuilder {

	// Prevents instantiation
	private BashScriptBuilder() {}

	/**
	 * This method builds the bash scripts required for the given module.<br>
	 * Standard local/cluster pipelines include: 1 MAIN script, 1+ worker-scripts.<br>
	 * Docker R_Modules include: 1 MAIN script, 0 worker-scripts - MAIN.R run by MAIN.sh<br>
	 * Docker *non-R_Modules* include: 1 MAIN script, 1+ worker-scripts - MAIN.sh runs workers<br>
	 * AWS Docker R_Modules include: 0 MAIN scripts, 0 worker-scripts - MAIN.R run by Nextflow<br>
	 * AWS Docker *non-R_Modules* include: 0 MAIN scripts, 1+ worker-scripts MAIN.sh runs workers<br>
	 * 
	 * @param module ScriptModule
	 * @param data Bash script lines
	 * @throws PipelineScriptException if any errors occur writing module script
	 */
	public static void buildScripts( final ScriptModule module, final List<List<String>> data )
		throws PipelineScriptException {
		if( data == null || data.size() < 1 )
			throw new PipelineScriptException( module, "All worker scripts are empty" );
		try {
			workerScripts.clear();
			buildWorkerScripts( module, data );
			if( workerScripts.isEmpty() )
				throw new PipelineScriptException( module, false, "No worker script lines created" );
			if( !DockerUtil.inAwsEnv() ) buildMainScript( module );
			workerScripts.clear();
			Processor.setFilePermissions( module.getScriptDir().getAbsolutePath(),
				Config.requireString( module, Constants.SCRIPT_PERMISSIONS ) );
		} catch( final Exception ex ) {
			Log.error( BashScriptBuilder.class, "Localized error details: ", ex );
			throw new PipelineScriptException( module, ex.getMessage() );
		}
	}

	/**
	 * Create bash worker script function: executeLine<br>
	 * Capture status code of the payload script line. Call scriptFailed function to capture all failure info (if any
	 * occur).
	 * 
	 * @return Bash script lines
	 */
	protected static List<String> buildExecuteFunction() {
		final List<String> lines = new ArrayList<>();
		lines.add( "function " + FUNCTION_EXECUTE_LINE + "() {" );
		lines.add( "${1}" );
		lines.add( "statusCode=$?" );
		lines.add( "[ ${statusCode} -ne 0 ] && " + FUNCTION_SCRIPT_FAILED + " \"${1}\" ${2} ${statusCode}" );
		lines.add( "}" + RETURN );
		return lines;
	}

	/**
	 * Build the MIAN script.
	 * 
	 * @param module ScriptModule
	 * @throws IOException if errors occur writing the MAIN script lines
	 * @throws ConfigException if problems occur accessing {@link biolockj.Config} properties
	 * @throws DockerVolCreationException 
	 */
	protected static void buildMainScript( final ScriptModule module ) throws ConfigException, IOException, DockerVolCreationException {

		final List<String> mainScriptLines = initMainScript( module );
		for( final File worker: workerScripts )
			mainScriptLines.add( getMainScriptExecuteWorkerLine( worker.getAbsolutePath() ) );

		mainScriptLines
			.add( RETURN + "touch \"" + getMainScriptPath( module ) + "_" + Constants.SCRIPT_SUCCESS + "\"" );
		final List<String> mainScriptLinesEasyReading = insertPathVars(module, mainScriptLines);
		createScript( module, getMainScriptPath( module ), mainScriptLinesEasyReading );
	}

	/**
	 * Create bash worker script function: scriptFailed<br>
	 * Failure details written to the failure indicator file:<br>
	 * <ol>
	 * <li>$1 Script payload line to execute
	 * <li>$2 Script line number
	 * <li>$3 Script line failure status code
	 * </ol>
	 * 
	 * @param path Script path
	 * @return Bash script lines
	 */
	protected static List<String> buildScriptFailureFunction( final String path ) {
		final List<String> lines = new ArrayList<>();
		lines.add( "function " + FUNCTION_SCRIPT_FAILED + "() {" );
		lines.add( "echo \"Line #${2} failure status code [ ${3} ]:  ${1}\" >> \"" + path + "_" +
			Constants.SCRIPT_FAILURES + "\"" );
		lines.add( "exit ${3}" );
		lines.add( "}" + RETURN );
		return lines;
	}

	/**
	 * Create the script. Leading zeros added if needed so all worker scripts have same number of digits. Print the
	 * worker script as DEBUG to the log file.
	 * 
	 * @param module ScriptModule 
	 * @param scriptPath Worker script path
	 * @param lines Ordered bash script lines to write to the scriptPath
	 * @return File bash script
	 * @throws IOException if unable to write the bash script to file system
	 */
	protected static File createScript( final ScriptModule module, final String scriptPath, final List<String> lines ) throws IOException {
		Log.info( BashScriptBuilder.class, "Write new script: " + scriptPath );
		final File workerScript = new File( scriptPath );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( workerScript ) );
		try {
			writeScript( module, writer, lines );
		} finally {
			writer.close();
		}
		return workerScript;
	}

	/**
	 * Call executeLine function in the worker script
	 * 
	 * @param workerScriptPath Worker script path
	 * @return bash script line
	 */
	protected static String getMainScriptExecuteWorkerLine( final String workerScriptPath ) {
		final StringBuffer line = new StringBuffer();
		if( DockerUtil.inDockerEnv() ) line.append( DockerUtil.SPAWN_DOCKER_CONTAINER + " " );
		else if( Config.isOnCluster() ) line.append( FUNCTION_RUN_JOB + " " );
		line.append( workerScriptPath );
		return FUNCTION_EXECUTE_LINE + " \"" + line.toString() + "\" ${LINENO}";
	}

	/**
	 * Pass each line and the current line number to: executeLine
	 *
	 * @param lines Basic script lines generated by a BioModule.
	 * @return List of bash script lines
	 */
	protected static List<String> getWorkerScriptLines( final List<String> lines ) {
		final List<String> out = new ArrayList<>();
		for( final String line: lines )
			out.add( FUNCTION_EXECUTE_LINE + " \"" + line + "\" ${LINENO}" );
		return out;
	}

	/**
	 * Build the file path for the numbered worker script. Leading zeros added if needed so all worker scripts have same
	 * number of digits.
	 * 
	 * @param module ScriptModule is the module that owns the scripts
	 * @return Absolute path of next worker script
	 * @throws ConfigFormatException if {@value biolockj.Constants#SCRIPT_NUM_WORKERS} property is not a positive
	 * integer
	 * @throws ConfigNotFoundException if {@value biolockj.Constants#SCRIPT_NUM_WORKERS} property is undefined
	 */
	protected static String getWorkerScriptPath( final ScriptModule module )
		throws ConfigNotFoundException, ConfigFormatException {
		if( DockerUtil.inAwsEnv() && module instanceof R_Module ) return getMainScriptPath( module );
		return module.getScriptDir().getAbsolutePath() + File.separator + ModuleUtil.displayID( module ) + "." +
			getWorkerId( workerNum(), ModuleUtil.getNumWorkers( module ).toString().length() ) + "_" +
			module.getClass().getSimpleName() + Constants.SH_EXT;
	}

	/**
	 * Create the ScriptModule main script that calls all worker scripts.
	 *
	 * @param module ScriptModule script directory is where main script is written
	 * @return List of lines for the main script that has the prefix
	 * {@value biolockj.module.ScriptModule#MAIN_SCRIPT_PREFIX}
	 * @throws ConfigException if errors occur accessing {@link biolockj.Config} properties
	 * @throws DockerVolCreationException 
	 */
	protected static List<String> initMainScript( final ScriptModule module ) throws ConfigException, DockerVolCreationException {
		final List<String> lines = new ArrayList<>();
		final String mainScriptPath = getMainScriptPath( module );
		final String header = Config.getString( module, Constants.SCRIPT_DEFAULT_HEADER );
		final String startedFlag = mainScriptPath + "_" + Constants.SCRIPT_STARTED;
		if( header != null ) lines.add( header + RETURN );
		lines.add( "# BioLockJ " + BioLockJUtil.getVersion() + ": " + mainScriptPath + RETURN );
		lines.addAll( pathVariableVals(module) );
		lines.add( "touch \"" + startedFlag + "\"" + RETURN );
		lines.add( "cd " + module.getScriptDir().getAbsolutePath() + RETURN );
		if( DockerUtil.inDockerEnv() ) {
			lines.addAll( DockerUtil.buildSpawnDockerContainerFunction( module, startedFlag ) );
		}else if( Config.isOnCluster() ) {
			lines.addAll( buildRunClusterJobFunction( module ) );
		}
		lines.addAll( buildScriptFailureFunction( mainScriptPath ) );
		lines.addAll( buildExecuteFunction() );
		return lines;
	}
	
	private static List<String> buildRunClusterJobFunction( ScriptModule module ) throws ConfigNotFoundException{
		List<String> lines = new ArrayList<>();
		String startedFlag = getMainScriptPath( module ) + "_" + Constants.SCRIPT_STARTED;
		lines.add( "# Submit job script" );
		lines.add( "function " + FUNCTION_RUN_JOB + "() {" );
		lines.add( "scriptName=$(basename $1)");
		lines.add( "id=$(" + Config.requireString( module, CLUSTER_BATCH_COMMAND ) + " $1)" );
		lines.add( "echo \"$scriptName:" + CLUSTER_KEY + ":$id\" >> " + startedFlag );
		lines.add( "}" + RETURN );
		return(lines);
	}
	
	/**
	 * Get the script lines to assign values to the common directories.
	 * Using these variables in place of full file paths makes the script easier to read.
	 * The scripts file paths are replaced with variables in {@link insertPathVars}.
	 * @see insertPathVars
	 * @param module
	 * @return
	 */
	protected static List<String> pathVariableVals (ScriptModule module) {
		final List<String> lines = new ArrayList<>();
		lines.add( PIPE_DIR + "=\"" + Config.pipelinePath() + "\"" );
		lines.add( MOD_DIR + "=\"" + module.getModuleDir().getAbsolutePath() + "\"" );
		lines.add( SCRIPT_DIR + "=\"" + module.getScriptDir().getAbsolutePath() + "\"" );
		lines.add( TEMP_DIR + "=\"" + module.getTempDir().getAbsolutePath() + "\"" );
		lines.add( OUTPUT_DIR + "=\"" + module.getOutputDir().getAbsolutePath() + "\"" );
		lines.add( "" );
		return lines;
	}
	
	/**
	 * Paths to common module directories are added via {@link pathVariableVals}.
	 * Using these variables in place of full file paths makes the script easier to read.
	 * @see pathVariableVals
	 * @param module
	 * @param scriptLines
	 * @return
	 */
	protected static List<String> insertPathVars (ScriptModule module, List<String> scriptLines){
		final List<String> lines = new ArrayList<>();
		for( final String line: scriptLines ) {
			String data = line;
			if( !data.trim().startsWith( TEMP_DIR ) && data.contains( module.getTempDir().getAbsolutePath() ) ) 
				data = data.replaceAll( module.getTempDir().getAbsolutePath(), Matcher.quoteReplacement( TEMP_DIR_VAR ) );
			if( !data.trim().startsWith( SCRIPT_DIR ) && data.contains( module.getScriptDir().getAbsolutePath() ) ) 
				data = data.replaceAll( module.getScriptDir().getAbsolutePath(), Matcher.quoteReplacement( SCRIPT_DIR_VAR ) );
			if( !data.trim().startsWith( OUTPUT_DIR ) && data.contains( module.getOutputDir().getAbsolutePath() ) ) 
				data = data.replaceAll( module.getOutputDir().getAbsolutePath(), Matcher.quoteReplacement( OUTPUT_DIR_VAR ) );
			if( !data.trim().startsWith( MOD_DIR ) && data.contains( module.getModuleDir().getAbsolutePath() ) ) 
				data = data.replaceAll( module.getModuleDir().getAbsolutePath(), Matcher.quoteReplacement( MOD_DIR_VAR ) );
			if( !data.trim().startsWith( PIPE_DIR ) && data.contains( Config.pipelinePath() ) ) 
				data = data.replaceAll( Config.pipelinePath(), Matcher.quoteReplacement( PIPE_DIR_VAR ) );
			lines.add( data );
		}
		return lines;
	}

	/**
	 * Create the numbered worker scripts. Leading zeros added if needed so all worker scripts names are the same
	 * length. If run on cluster and cluster.jobHeader is defined, add cluster.jobHeader as header for worker scripts.
	 * Otherwise, use the same script.defaultHeader as used in the MAIN script.
	 *
	 *
	 * @param module ScriptModule script directory is where main script is written
	 * @param scriptPath Worker script absolute file path
	 * @return Lines for the worker script
	 * @throws Exception if required properties are undefined
	 */
	protected static List<String> initWorkerScript( final ScriptModule module, final String scriptPath )
		throws Exception {
		final List<String> lines = new ArrayList<>();
		final String header = Config.getString( module, SCRIPT_JOB_HEADER );
		final String defaultHeader = Config.getString( module, Constants.SCRIPT_DEFAULT_HEADER );
		if( Config.isOnCluster() && header != null ) lines.add( header );
		else if( defaultHeader != null ) lines.add( defaultHeader );
		lines.add( "" );
		lines.add( "# BioLockJ." + BioLockJUtil.getVersion() + ": " + scriptPath);
		lines.add( "" );
		lines.addAll( pathVariableVals(module) );
		lines.add( "touch \"" + scriptPath + "_" + Constants.SCRIPT_STARTED  + "\"");
		lines.add( "" );
		lines.add( "cd " + module.getScriptDir().getAbsolutePath());
		lines.add( "" );
		lines.addAll( loadModules( module ) );

		final List<String> workerFunctions = module.getWorkerScriptFunctions();
		if( workerFunctions != null && !workerFunctions.isEmpty() ) lines.addAll( workerFunctions );
		lines.addAll( buildScriptFailureFunction( scriptPath ) );
		lines.addAll( buildExecuteFunction() );
		return lines;
	}

	/**
	 * This method formats the bash script to indent if statement code blocks
	 * 
	 * @param module ScriptModule 
	 * @param writer BufferedWriter writes to the file
	 * @param scriptLines List or shell script lines
	 * @throws IOException if errors occur writing the new script
	 */
	protected static void writeScript( final ScriptModule module, final BufferedWriter writer, final List<String> scriptLines )
		throws IOException {
		int indentCount = 0;
		try {
			for( String line: scriptLines ) {
				String data = line.trim();
				if( data.equals( "fi" ) || data.equals( "}" ) || data.equals( "elif" ) ||
					data.equals( "else" ) || data.equals( "done" ) ) indentCount--;

				int i = 0;
				while( i++ < indentCount )
					line = Constants.INDENT + line;
				writer.write( line + RETURN );
				Log.debug( BashScriptBuilder.class, line );

				if( data.endsWith( "{" ) || line.equals( "elif" ) || data.equals( "else" ) ||
					data.startsWith( "if" ) && data.endsWith( "then" ) ||
					data.startsWith( "while" ) && data.endsWith( "do" ) ) indentCount++;
			}
		} finally {
			if( writer != null ) writer.close();
		}
	}

	private static void buildWorkerScripts( final ScriptModule module, final List<List<String>> data )
		throws Exception {
		int sampleCount = 0;
		String workerScriptPath = getWorkerScriptPath( module );
		List<String> workerLines = initWorkerScript( module, workerScriptPath );
		final Iterator<List<String>> it = data.iterator();
		while( it.hasNext() ) {
			final List<String> lines = it.next();
			if( lines.isEmpty() )
				throw new PipelineScriptException( module, true, " Worker script #" + workerNum() + " is empty." );
			workerLines.addAll( getWorkerScriptLines( lines ) );
			if( saveWorker( module, ++sampleCount, data.size() ) || !it.hasNext() ) {
				if( !( module instanceof JavaModule ) )
					workerLines.add( "touch \"" + workerScriptPath + "_" + Constants.SCRIPT_SUCCESS + "\"" );
				final List<String> workerLinesEasyReading = insertPathVars(module, workerLines);
				workerScripts.add( createScript( module, workerScriptPath, workerLinesEasyReading ) );
				sampleCount = 0;
				if( it.hasNext() ) {
					workerScriptPath = getWorkerScriptPath( module );
					workerLines = initWorkerScript( module, workerScriptPath );
				}
			}
		}

		Log.info( BashScriptBuilder.class, Constants.LOG_SPACER );
		Log.info( BashScriptBuilder.class,
			workerNum() + " WORKER scripts created for: " + module.getClass().getName() );
		Log.info( BashScriptBuilder.class, Constants.LOG_SPACER );
	}

	private static String getMainScriptPath( final ScriptModule module ) {
		return new File( module.getScriptDir().getAbsolutePath() + File.separator + BioModule.MAIN_SCRIPT_PREFIX +
			module.getModuleDir().getName() + Constants.SH_EXT ).getAbsolutePath();
	}

	private static Integer getMinSamplesPerWorker( final BioModule module, final int count )
		throws ConfigNotFoundException, ConfigFormatException {
		return new Double( Math.floor( (double) count / (double) ModuleUtil.getNumWorkers( module ) ) ).intValue();
	}

	private static String getWorkerId( final int scriptNum, final int digits ) {
		return String.format( "%0" + digits + "d", scriptNum );
	}

	/**
	 * Return lines to script that load cluster modules based on {@link biolockj.Config}.{@value #CLUSTER_MODULES}
	 *
	 * @param module ScriptModule
	 * @return Bash Script lines to load cluster modules
	 */
	private static List<String> loadModules( final ScriptModule module ) {
		final List<String> lines = new ArrayList<>();
		for( final String clusterMod: Config.getList( module, CLUSTER_MODULES ) )
			lines.add( "module load " + clusterMod );
		if( !lines.isEmpty() ) lines.add( "" );
		final String prologue = Config.getString( module, CLUSTER_PROLOGUE );
		if( prologue != null ) lines.add( prologue + RETURN );
		return lines;
	}

	private static boolean saveWorker( final BioModule module, final int sampleCount, final int count )
		throws ConfigNotFoundException, ConfigFormatException {
		final int maxWorkers = count - ModuleUtil.getNumWorkers( module );
		final int minSamplesPerWorker = getMinSamplesPerWorker( module, count );
		return workerNum() < maxWorkers && sampleCount == minSamplesPerWorker + 1 ||
			workerNum() >= maxWorkers && sampleCount == minSamplesPerWorker;
	}

	private static int workerNum() {
		return workerScripts.size();
	}

	/**
	 * Register properties with the Properties class for API access.
	 * @throws API_Exception 
	 */
	public static void registerProps() throws API_Exception {
		Properties.registerProp(CLUSTER_BATCH_COMMAND, Properties.STRING_TYPE, CLUSTER_BATCH_COMMAND_DESC);
		Properties.registerProp(CLUSTER_STATUS_COMMAND, Properties.STRING_TYPE, CLUSTER_STATUS_COMMAND_DESC);
		Properties.registerProp(CLUSTER_MODULES, Properties.LIST_TYPE, CLUSTER_MODULES_DESC);
		Properties.registerProp(CLUSTER_PROLOGUE, Properties.STRING_TYPE, CLUSTER_PROLOGUE_DESC);
		Properties.registerProp(SCRIPT_JOB_HEADER, Properties.STRING_TYPE, SCRIPT_JOB_HEADER_DESC);
	}
	/**
	 * Let modules see property names.
	 */
	public static List<String> listProps(){
		ArrayList<String> props = new ArrayList<>();
		props.add( CLUSTER_BATCH_COMMAND );
		props.add( CLUSTER_STATUS_COMMAND );
		props.add( CLUSTER_MODULES );
		props.add( CLUSTER_PROLOGUE );
		props.add( SCRIPT_JOB_HEADER );
		return props;
	}
	
	/**
	 * {@link biolockj.Config} String property: {@value #CLUSTER_BATCH_COMMAND}<br>
	 * {@value #CLUSTER_BATCH_COMMAND_DESC}
	 */
	protected static final String CLUSTER_BATCH_COMMAND = "cluster.batchCommand";
	private static final String CLUSTER_BATCH_COMMAND_DESC = "Terminal command used to submit jobs on the cluster";
	
	/**
	 * {@link biolockj.Config} String property: {@value #CLUSTER_STATUS_COMMAND}<br>
	 * {@value #CLUSTER_STATUS_COMMAND_DESC}
	 */
	protected static final String CLUSTER_STATUS_COMMAND = "cluster.statusCommand";
	private static final String CLUSTER_STATUS_COMMAND_DESC = "Terminal command used to check the status of jobs on the cluster";

	/**
	 * {@link biolockj.Config} List property: {@value #CLUSTER_MODULES}<br>
	 * {@value #CLUSTER_MODULES_DESC}
	 */
	protected static final String CLUSTER_MODULES = "cluster.modules";
	private static final String CLUSTER_MODULES_DESC = "List of cluster modules to load at start of worker scripts";

	/**
	 * One parameter of the {@link biolockj.Config} String property {@value #SCRIPT_JOB_HEADER} to set number of cores.
	 */
	protected static final List<String> CLUSTER_NUM_PROCESSORS = Arrays.asList( "procs", "ppn" );

	/**
	 * {@link biolockj.Config} String property: {@value #CLUSTER_PROLOGUE}<br>
	 * {@value #CLUSTER_PROLOGUE_DESC}
	 */
	protected static final String CLUSTER_PROLOGUE = "cluster.prologue";
	private static final String CLUSTER_PROLOGUE_DESC = "To run at the start of every script after loading cluster modules (if any)";

	/**
	 * {@link biolockj.Config} String property: {@value #SCRIPT_JOB_HEADER}<br>
	 * {@value #SCRIPT_JOB_HEADER_DESC}
	 */
	protected static final String SCRIPT_JOB_HEADER = "cluster.jobHeader";
	private static final String SCRIPT_JOB_HEADER_DESC = "Header written at top of worker scripts";

	private static final String FUNCTION_EXECUTE_LINE = "executeLine";
	private static final String FUNCTION_RUN_JOB = "runJob";
	private static final String CLUSTER_KEY = "cluster";
	private static final String FUNCTION_SCRIPT_FAILED = "scriptFailed";
	private static final String MOD_DIR = "modDir";
	private static final String MOD_DIR_VAR = "${" + MOD_DIR + "}";
	private static final String OUTPUT_DIR = "outputDir";
	private static final String OUTPUT_DIR_VAR = "${" + OUTPUT_DIR + "}";
	private static final String PIPE_DIR = "pipeDir";
	private static final String PIPE_DIR_VAR = "${" + PIPE_DIR + "}";
	private static final String SCRIPT_DIR = "scriptDir";
	private static final String SCRIPT_DIR_VAR = "${" + SCRIPT_DIR + "}";
	private static final String TEMP_DIR = "tempDir";
	private static final String TEMP_DIR_VAR = "${" + TEMP_DIR + "}";
	
	private static final String RETURN = Constants.RETURN;
	private static final List<File> workerScripts = new ArrayList<>();
}
