/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 9, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org
 */
package biolockj;

import java.io.File;
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import biolockj.module.*;
import biolockj.module.report.Email;
import biolockj.module.report.r.R_Module;
import biolockj.util.*;

/**
 * This class initializes and executes each {@link biolockj.module.BioModule} configured for execution.<br>
 * {@link biolockj.module.BioModule}s that generate scripts are monitored until all scripts are complete, before moving
 * on to the next module.<br>
 */
public class Pipeline {
	private Pipeline() {}

	/**
	 * Execute a single pipeline module.
	 * 
	 * @throws Exception if runtime errors occur
	 */
	public static void executeModule() throws Exception {
		ModuleUtil.markStarted( exeModule() );
		refreshRCacheIfNeeded();
		exeModule().executeTask();
		final boolean isJava = exeModule() instanceof JavaModule;
		final boolean hasScripts = ModuleUtil.hasScripts( exeModule() );
		final boolean detachJava = Config.getBoolean( exeModule(), Constants.DETACH_JAVA_MODULES );
		final boolean runDetached = isJava && hasScripts && detachJava;

		if( runDetached ) MasterConfigUtil.saveMasterConfig();
		if( hasScripts && !DockerUtil.inAwsEnv() ) Processor.submit( (ScriptModule) exeModule() );
		if( hasScripts ) waitForModuleScripts();
		exeModule().cleanUp();
		ValidationUtil.validateModule( exeModule() );
		if( !runDetached ) SummaryUtil.reportSuccess( exeModule() );
		ModuleUtil.markComplete( exeModule() );
	}

	/**
	 * Return the BioModule currently being executed
	 * 
	 * @return Current BioModule
	 */
	public static BioModule exeModule() {
		return currentModule;
	}

	/**
	 * Return a list of {@link biolockj.module.BioModule}s constructed by the {@link biolockj.BioModuleFactory}
	 *
	 * @return List of BioModules
	 */
	public static List<BioModule> getModules() {
		return bioModules;
	}

	/**
	 * Return {@value Constants#SCRIPT_SUCCESS} if no pipelineException has been thrown, otherwise return FAILED
	 *
	 * @return pipeline status (SUCCESS or FAILED)
	 */
	public static String getStatus() {
		if( pipelineException != null || getModules() == null || getModules().isEmpty() ) return "FAILED";
		for( final BioModule module: getModules() )
			if( !ModuleUtil.isComplete( module ) && !( module instanceof Email ) ) return "FAILED";
		return Constants.SCRIPT_SUCCESS.toUpperCase();
	}

	/**
	 * This method initializes the Pipeline by building the modules and checking module dependencies.
	 * 
	 * @throws Exception if errors occur
	 */
	public static void initializePipeline() throws Exception {
		Log.info( Pipeline.class, "Initialize " + ( BioLockJUtil.isDirectMode() ? "DIRECT module ":
			DockerUtil.inAwsEnv() ? "AWS ": DockerUtil.inDockerEnv() ? "DOCKER ": "" ) + "pipeline" );
		bioModules = BioModuleFactory.buildPipeline();
		if( !BioLockJUtil.isDirectMode() )
			Config.setConfigProperty( Constants.INTERNAL_ALL_MODULES, BioLockJUtil.getClassNames( bioModules ) );
		initializeModules();
	}

	/**
	 * If moduleName is null, run all modules, otherwise only run the specified module.
	 * 
	 * @param id Module ID
	 * @throws Exception if errors occur
	 */
	public static void runDirectModule( final Integer id ) throws Exception {
		final JavaModule module = (JavaModule) Pipeline.getModules().get( id );
		try {
			Log.info( Pipeline.class,
				"Start Direct BioModule Execution for [ ID #" + id + " ] ---> " + module.getClass().getSimpleName() );
			module.runModule();
			Log.info( Pipeline.class, "DIRECT module ID [" + id + "].runModule() complete!" );
			module.cleanUp();
			ValidationUtil.validateModule( module );
			module.moduleComplete();
			SummaryUtil.reportSuccess( module );
			MasterConfigUtil.saveMasterConfig();
		} catch( final Exception ex ) {
			Log.error( Pipeline.class, "Errors occurred attempting to run DIRECT module [ ID=" + id + " ] --> " +
				module.getClass().getSimpleName(), ex );
			module.moduleFailed();
			SummaryUtil.reportFailure( ex );
		}
	}

	/**
	 * This method initializes and executes the BioModules set in the BioLockJ configuration file.<br>
	 * 
	 * @throws Exception if any fatal error occurs during execution
	 */
	public static void runPipeline() throws Exception {
		BioLockJUtil.markStatus( Constants.BLJ_STARTED );
		try {
			executeModules();
			SummaryUtil.reportSuccess( null );
		} catch( final Exception ex ) {
			try {
				BioLockJUtil.markStatus( currentModule, Constants.BLJ_FAILED );
				Log.error( Pipeline.class, "Pipeline failed! " + ex.getMessage(), ex );
				pipelineException = ex;
				SummaryUtil.reportFailure( ex );
			} catch( final Exception ex2 ) {
				Log.error( Pipeline.class, "Attempt to update summary has failed: " + ex2.getMessage(), ex2 );
			}

			try {
				BioModule emailMod = null;
				boolean foundIncomplete = false;
				for( final BioModule module: Pipeline.getModules() ) {
					setExeModule( module );
					if( module instanceof Email ) emailMod = module;
					if( !foundIncomplete && !ModuleUtil.isComplete( module ) ) foundIncomplete = true;
					if( foundIncomplete && emailMod != null ) {
						Log.warn( Pipeline.class, "Attempting to send failure notification with Email module: " +
							emailMod.getModuleDir().getName() );
						emailMod.executeTask();
						Log.warn( Pipeline.class, "Attempt appears to be a success!" );
						break;
					}
				}

			} catch( final Exception innerEx ) {
				Log.error( Pipeline.class,
					"Attempt to Email pipeline failure info --> also failed!  " + innerEx.getMessage(), innerEx );
			}

			throw ex;
		}
	}

	/**
	 * This method executes all new and incomplete modules<br>
	 * Before/after a module is executed, set persistent module status by creating status indicator files. Incomplete
	 * modules have an empty file {@value Constants#BLJ_STARTED} in the module directory.<br>
	 * Complete modules have an empty file {@value Constants#BLJ_COMPLETE} in the module directory.<br>
	 * {@link biolockj.module.BioModule}s are run in the order listed in the {@link biolockj.Config} file.<br>
	 * <p>
	 * Execution steps:
	 * <ol>
	 * <li>File {@value Constants#BLJ_STARTED} is added to the module directory
	 * <li>Run module scripts, if any, polling 1/minute for status until all scripts complete or time out.
	 * <li>File {@value Constants#BLJ_STARTED} is replaced by {@value Constants#BLJ_COMPLETE} as status indicator
	 * </ol>
	 *
	 * @throws Exception if script errors occur
	 */
	protected static void executeModules() throws Exception {
		for( final BioModule module: Pipeline.getModules() ) {
			setExeModule( module );
			if( !ModuleUtil.isComplete( module ) ) executeModule();
			else Log.debug( Pipeline.class,
				"Skipping succssfully completed BioLockJ Module: " + module.getClass().getName() );
		}
	}

	/**
	 * Initialization occurs by calling {@link biolockj.module.BioModule} methods on configured modules<br>
	 * <ol>
	 * <li>Create module sub-directories under {@value biolockj.Constants#INTERNAL_PIPELINE_DIR} as ordered in
	 * {@link biolockj.Config} file.<br>
	 * <li>Reset the {@link biolockj.util.SummaryUtil} module so previous summary descriptions can be used for completed
	 * modules
	 * <li>Delete incomplete module contents if restarting a failed pipeline
	 * {@value biolockj.module.BioModule#OUTPUT_DIR} directory<br>
	 * <li>Call {@link #refreshRCacheIfNeeded()} to cache R fields after 1st R module runs<br>
	 * <li>Verify dependencies with {@link biolockj.module.BioModule#checkDependencies()}<br>
	 * </ol>
	 *
	 * @throws Exception thrown if propagated by called methods
	 * @return true if no errors are thrown
	 */
	protected static boolean initializeModules() throws Exception {
		for( final BioModule module: getModules() ) {
			setExeModule( module );
			if( ModuleUtil.isIncomplete( module ) && ( !BioLockJUtil.isDirectMode() || module instanceof Email ) ) {
				final String path = module.getModuleDir().getAbsolutePath();
				Log.info( Pipeline.class, "Reset incomplete module: " + path );
				FileUtils.forceDelete( module.getModuleDir() );
				new File( path ).mkdirs();
			}

			info( "Check dependencies for: " + module.getClass().getName() );
			module.checkDependencies();
			ValidationUtil.checkDependencies( module );

			if( ModuleUtil.isComplete( module ) ) {
				module.cleanUp();
				if( !BioLockJUtil.isDirectMode() ) ValidationUtil.validateModule( module );
				refreshRCacheIfNeeded();
			}
		}

		return true;
	}

	/**
	 * The {@link biolockj.module.ScriptModule#getScriptDir()} will contain one main script and one ore more worker
	 * scripts.<br>
	 * An empty file with {@value Constants#SCRIPT_STARTED} appended to the script name is created when execution
	 * begins.<br>
	 * If successful, an empty file with {@value Constants#SCRIPT_SUCCESS} appended to the script name is created.<br>
	 * Upon failure, an empty file with {@value Constants#SCRIPT_FAILURES} appended to the script name is created.<br>
	 * Script status is polled each minute, determining status by counting indicator files.<br>
	 * {@link biolockj.Log} outputs the # of started, failed, and successful scripts (if any change).<br>
	 * {@link biolockj.Log} repeats the previous message every 10 minutes if no status change is detected.<br>
	 *
	 * @param module ScriptModule
	 * @return true if all scripts are complete, regardless of status
	 * @throws Exception thrown to end pipeline execution
	 */
	protected static boolean poll( final ScriptModule module ) throws Exception {
		final Collection<File> scriptFiles = getWorkerScripts( module );
		final int numScripts = scriptFiles.size();
		int numSuccess = 0;
		int numStarted = 0;
		int numFailed = 0;

		for( final File f: scriptFiles ) {
			final File testStarted = new File( f.getAbsolutePath() + "_" + Constants.SCRIPT_STARTED );
			final File testSuccess = new File( f.getAbsolutePath() + "_" + Constants.SCRIPT_SUCCESS );
			final File testFailure = new File( f.getAbsolutePath() + "_" + Constants.SCRIPT_FAILURES );
			numStarted = numStarted + ( testStarted.isFile() ? 1: 0 );
			numSuccess = numSuccess + ( testSuccess.isFile() ? 1: 0 );
			numFailed = numFailed + ( testFailure.isFile() ? 1: 0 );
		}

		final String logMsg = module.getClass().getSimpleName() + " Status (Total=" + numScripts + "): Success=" +
			numSuccess + "; Failed=" + numFailed + "; Running=" + ( numStarted - numSuccess - numFailed ) +
			"; Queued=" + ( numScripts - numStarted );

		if( !statusMsg.equals( logMsg ) ) {
			statusMsg = logMsg;
			pollCount = 0;
			Log.info( Pipeline.class, logMsg );
		} else if( ++pollCount % 10 == 0 ) Log.info( Pipeline.class, logMsg );

		if( numFailed > 0 ) {
			final String failMsg = "SCRIPT FAILED: " + BioLockJUtil.getCollectionAsString( module.getScriptErrors() );
			throw new Exception( failMsg );
		}

		return numScripts > 0 && numSuccess + numFailed == numScripts;
	}

	/**
	 * Refresh R cache if about to run the 1st R module.
	 * 
	 * @throws Exception if errors occur
	 */
	protected static void refreshRCacheIfNeeded() throws Exception {
		if( ModuleUtil.isFirstRModule( exeModule() ) ) {
			Log.info( Pipeline.class,
				"Refresh R-cache before running 1st R module: " + exeModule().getClass().getName() );
			RMetaUtil.classifyReportableMetadata( exeModule() );
		}
	}

	private static IOFileFilter getWorkerScriptFilter( final ScriptModule module ) {
		String filterString = "*" + Constants.SH_EXT;
		if( DockerUtil.inDockerEnv() && module instanceof R_Module )
			filterString = BioModule.MAIN_SCRIPT_PREFIX + "*" + Constants.SH_EXT;
		else if( module instanceof R_Module ) filterString = BioModule.MAIN_SCRIPT_PREFIX + "*" + Constants.R_EXT;

		return new WildcardFileFilter( filterString );

	}

	private static Collection<File> getWorkerScripts( final ScriptModule module ) throws Exception {
		final Collection<File> scriptFiles =
			FileUtils.listFiles( module.getScriptDir(), getWorkerScriptFilter( module ), null );

		final File mainScript = module.getMainScript();
		if( !( module instanceof R_Module ) && mainScript != null ) scriptFiles.remove( mainScript );

		if( !DockerUtil.inAwsEnv() ) Log.debug( Pipeline.class,
			"mainScript = " + ( mainScript == null ? "<null>": mainScript.getAbsolutePath() ) );
		for( final File f: scriptFiles )
			Log.debug( Pipeline.class, "Worker Script = " + f.getAbsolutePath() );

		return scriptFiles;
	}

	private static void info( final String msg ) {
		if( !BioLockJUtil.isDirectMode() ) Log.info( Pipeline.class, msg );
	}

	private static void logScriptTimeOutMsg( final ScriptModule module ) throws Exception {
		final String prompt = "------> ";
		Log.info( Pipeline.class, prompt + "Java program wakes every 60 seconds to check execution progress" );
		Log.info( Pipeline.class, prompt + "Status determined by existance of indicator files in " +
			module.getScriptDir().getAbsolutePath() );
		Log.info( Pipeline.class, prompt + "Indicator files end with: \"_" + Constants.SCRIPT_STARTED + "\", \"_" +
			Constants.SCRIPT_SUCCESS + "\", or \"_" + Constants.SCRIPT_FAILURES + "\"" );
		Log.info( Pipeline.class,
			prompt + "If any change to #Success/#Failed/#Running/#Queued changed, new values logged" );
		if( module.getTimeout() == null || module.getTimeout() > 10 ) Log.info( Pipeline.class, prompt +
			"Status message repeats every 10 minutes while scripts are executing (if status remains unchanged)." );

		if( module.getTimeout() != null ) Log.info( Pipeline.class,
			prompt + "Running scripts will time out after the configured SCRIPT TIMEOUT = " + module.getTimeout() );
		else Log.info( Pipeline.class, prompt + "Running scripts will NEVER TIME OUT." );
	}

	private static void setExeModule( final BioModule module ) {
		currentModule = module;
	}

	/**
	 * This method calls executes script module scripts and monitors them until complete or timing out after
	 * {@value #POLL_TIME} seconds.
	 *
	 * @throws Exception if errors occur
	 */
	private static void waitForModuleScripts() throws Exception {
		final ScriptModule module = (ScriptModule) exeModule();
		logScriptTimeOutMsg( module );
		int numMinutes = 0;
		boolean finished = false;
		while( !finished ) {
			finished = poll( module );
			if( !finished ) {
				if( module.getTimeout() != null && module.getTimeout() > 0 && numMinutes++ >= module.getTimeout() )
					throw new Exception( module.getClass().getName() + " timed out after " + numMinutes + " minutes." );
				Thread.sleep( BioLockJUtil.minutesToMillis( 1 ) );
			}
		}
	}

	private static List<BioModule> bioModules = null;
	private static BioModule currentModule = null;
	private static Exception pipelineException = null;
	private static int pollCount = 0;
	private static String statusMsg = "";
}
