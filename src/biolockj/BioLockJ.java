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
import java.io.IOException;
import java.util.*;
import org.apache.commons.io.FileUtils;
import biolockj.exception.BioLockJStatusException;
import biolockj.exception.ConfigPathException;
import biolockj.exception.DockerVolCreationException;
import biolockj.exception.FatalExceptionHandler;
import biolockj.exception.InvalidPipelineException;
import biolockj.module.BioModule;
import biolockj.module.report.Email;
import biolockj.util.*;

/**
 * This is the primary BioLockJ class - its main() method is executed when the jar is run.<br>
 * This class validates the runtime parameters to run a new pipeline or restart a failed pipeline.<br>
 * The Java log file is initialized and the configuration file is processed before starting the pipeline.<br>
 * If the pipeline is successful, the program executes clean up operations (if configured) and creates a status-complete
 * indicator file in the pipeline root directory.<br>
 */
public class BioLockJ {
	
	private static File pipelineDir = null;
	private static String pipelineName = null;
	private static String pipelineInstanceId = null;
	
	private BioLockJ() {}

	/**
	 * Copy file to pipeline root directory.
	 * 
	 * @param file File to copy
	 * @throws Exception if errors occur
	 */
	public static void copyFileToPipelineRoot( final File file ) throws Exception {
		Log.info( BioLockJ.class,
			"Copy file: " + file.getAbsolutePath() + " to pipeline root: " + Config.pipelinePath() );
		final File localFile = new File( Config.pipelinePath() + File.separator + file.getName() );
		if( !localFile.isFile() ) {
			FileUtils.copyFileToDirectory( file, new File( Config.pipelinePath() ) );
			if( !localFile.isFile() )
				throw new Exception( "Unable to copy file to pipeline root directory: " + file.getAbsolutePath() );
		}
	}

	/**
	 * Print error file path, restart instructions, and link to the BioLockJ Wiki
	 * 
	 * @return Help Info
	 */
	public static String getHelpInfo() {
		final File errFile = FatalExceptionHandler.getErrorLog();
		// TODO: reference the jar help menu: showInfo(Constants.HELP)
		return RETURN + "To view the BioLockJ help menu, run \"biolockj --" + Constants.HELP + "\"" + RETURN +
			( errFile != null ? "Check error logs here --> " + errFile.getAbsolutePath() + RETURN: "" ) +
			"For more information, please visit the BioLockJ Wiki:" + Constants.BLJ_WIKI + RETURN;
	}

	/**
	 * Determine project status based on existence of {@value biolockj.Constants#BLJ_COMPLETE} in pipeline root
	 * directory.
	 *
	 * @return true if {@value biolockj.Constants#BLJ_COMPLETE} exists in the pipeline root directory, otherwise false
	 */
	public static boolean isPipelineComplete() {
		return new File( Config.pipelinePath() + File.separator + Constants.BLJ_COMPLETE ).isFile();
	}

	/**
	 * {@link biolockj.BioLockJ} is the BioLockJ.jar Main-Class, and is the first method executed.<br>
	 * Execution summary:<br>
	 * <ol>
	 * <li>Call {@link #initBioLockJ(String[])} to assign pipeline root dir and log file
	 * <li>If change password pipeline, call {@link biolockj.module.report.Email#encryptAndStoreEmailPassword()}
	 * <li>Otherwise execute {@link #runPipeline()}
	 * </ol>
	 * <p>
	 * If pipeline has failed, attempt execute {@link biolockj.module.report.Email} (if configured) to notify user of
	 * failures.
	 *
	 * @param args - String[] runtime parameters passed to the Java program when launching BioLockJ
	 */
	public static void main( final String[] args ) {
		BioLockJUtil.showInfo( args );
		System.out.println( "Starting BioLockJ...");
		try {
			initBioLockJ( args );
			checkDependencies();
			runPipeline();
		} catch( final Exception ex ) {
			FatalExceptionHandler.logFatalError( args, ex );
		} finally {
			if( !BioLockJUtil.isDirectMode() ) pipelineShutDown();
		}
	}
	
	

	/**
	 * Create a copy of the sequence files in property {@value biolockj.Constants#INPUT_DIRS}, output to a directory
	 * named {@value biolockj.Constants#INTERNAL_PIPELINE_DIR}/input.
	 *
	 * @throws Exception if unable to copy the files
	 */
	protected static void copyInputData() throws Exception {

		// Copy input does not need to copy files into pipeline dir if not copying entire pipeline dir
		// since copy will take place as S3 xFer.
		if( DockerUtil.inAwsEnv() && Config.getBoolean( null, Constants.PIPELINE_COPY_FILES ) &&
			!Config.getBoolean( null, NextflowUtil.AWS_COPY_PIPELINE_TO_S3 ) ) {
			//NextflowUtil.awsSyncS3( DockerUtil.DOCKER_INPUT_DIR, false );
			for ( File f : BioLockJUtil.getInputDirs() ) {
				NextflowUtil.awsSyncS3( f.getAbsolutePath(), false );
			}
			return;
		}
		final File inputDir = BioLockJUtil.pipelineInternalInputDir();
		final String path =
			Config.pipelinePath() + File.separator + inputDir.getName() + File.separator + Constants.BLJ_COMPLETE;
		final File statusFile = new File( path );
		if( !inputDir.isDirectory() ) inputDir.mkdirs();
		else if( statusFile.isFile() ) return;

		for( final File dir: BioLockJUtil.getInputDirs() ) {
			info( "Copying input files from " + dir + " to " + inputDir.getAbsolutePath() );
			FileUtils.copyDirectory( dir, inputDir );
		}
	}

	
	private static void setPipelineRootDir() throws DockerVolCreationException, InvalidPipelineException, BioLockJStatusException, IOException {
		String pipeType;
		if( RuntimeParamUtil.doRestart() ) {
			pipelineDir = RuntimeParamUtil.getRestartDir();
			pipelineName = PipelineUtil.getProjectName( pipelineDir );
			pipelineInstanceId = PipelineUtil.getPipelineId( pipelineDir );
			pipeType = "RESTART_DIR";
		} else if( BioLockJUtil.isDirectMode() ) {
			pipelineDir = RuntimeParamUtil.getDirectPipelineDir();
			pipelineName = PipelineUtil.getProjectName( pipelineDir );
			pipelineInstanceId = PipelineUtil.getPipelineId( pipelineDir );
			pipeType = "DIRECT";
		} else {
			pipelineName = PipelineUtil.getProjectNameFromPropFile(RuntimeParamUtil.getConfigFile());
			createPipelineDirectory();
			pipeType = "NEW";
		}
		String printPathOnScreen = DockerUtil.deContainerizePath( pipelineDir.getAbsolutePath() );
		System.out.println( Constants.PIPELINE_LOCATION_KEY + printPathOnScreen);
		Log.info( BioLockJ.class, "Assign " + pipeType + " pipeline root directory: " + printPathOnScreen );
	}

	/**
	 * Create the pipeline root directory under $BLJ_PROJ and save the path to
	 * {@link biolockj.Config}.{@value biolockj.Constants#INTERNAL_PIPELINE_DIR}.
	 * <p>
	 * For example, the following {@link biolockj.Config} settings will create:
	 * <b>/projects/MicrobeProj_2018Jan01</b><br>
	 * <ul>
	 * <li>$DOCKER_PROJ = /projects
	 * <li>{@link biolockj.Config} file name = MicrobeProj.properties
	 * <li>Current date = January 1, 2018
	 * </ul>
	 *
	 * Also creates an empty master properties file so the directory is recognized as a valid pipeline; see
	 * {@link PipelineUtil#isPipelineDir(File)}
	 *
	 * @return Pipeline root directory
	 * @throws DockerVolCreationException
	 * @throws IOException
	 * @throws BioLockJStatusException
	 */
	private static void createPipelineDirectory() throws DockerVolCreationException, BioLockJStatusException, IOException {
		final String dateString = BioLockJUtil.getDateString();
		pipelineInstanceId = getProjectName() + "_" + dateString;
		pipelineDir = new File( RuntimeParamUtil.get_BLJ_PROJ().getAbsolutePath(), pipelineInstanceId);
		int i = 2;
		while( pipelineDir.exists() ) {
			//TODO: dateString before i; or no date string
			pipelineInstanceId = getProjectName() + "_" + i++ + "_" + dateString;
			pipelineDir = new File( RuntimeParamUtil.get_BLJ_PROJ().getAbsolutePath(), pipelineInstanceId ); 
		}	
		pipelineDir.mkdirs();
		//empty file because master config is part of a definition of a pipeline.
		BioLockJUtil.createFile( MasterConfigUtil.getMasterConfig().getAbsolutePath() );
	}

	/**
	 * Execution summary:<br>
	 * <ol>
	 * <li>Call {@link biolockj.util.MemoryUtil#reportMemoryUsage(String)} for baseline memory info
	 * <li>Call {@link biolockj.util.RuntimeParamUtil#registerRuntimeParameters(String[])}
	 * <li>Call {@link biolockj.util.MetaUtil#initialize()} to verify metadata dependencies
	 * <li>Call {@link biolockj.Config#initialize()} to create pipeline root dir and load properties
	 * <li>Initialize {@link Log} with /resources/log4J.properties
	 * <li>Copy initial metadata file into the pipeline root directory
	 * <li>Call {@link biolockj.util.SeqUtil#initialize()} to set Config parameters based on sequence files
	 * </ol>
	 * <p>
	 *
	 * @param args - String[] runtime parameters passed to the Java program when launching BioLockJ
	 * @throws Exception if errors occur
	 */
	protected static void initBioLockJ( final String[] args ) throws Exception {
		Log.debug( BioLockJ.class, "APP_START_TIME (millis): " + Constants.APP_START_TIME );
		System.out.println(Constants.STATUS_START_KEY + "Initializing BioLockJ ...");
		MemoryUtil.reportMemoryUsage( "INTIAL MEMORY STATS" );
		RuntimeParamUtil.registerRuntimeParameters( args );
		DockerUtil.makeMapper();
		RuntimeParamUtil.validateParams();
		setPipelineRootDir();
		DockerUtil.touchDockerInfo();
		SummaryUtil.touchSystemInfo();
		Config.initialize();
		
		if( BioLockJUtil.isDirectMode() ) Log.initialize( getDirectLogName( RuntimeParamUtil.getDirectModuleDir() ) );
		else Log.initialize( Config.pipelineName() );
		
		ValidationUtil.hasStrictValidation(true);
		if( isPipelineComplete() ) throw new Exception( "Pipeline Cancelled!  Pipeline already contains status file: " +
			Constants.BLJ_COMPLETE + " --> Check directory: " + Config.pipelinePath() );

		MetaUtil.initialize();

		if( RuntimeParamUtil.doRestart() ) initRestart();

		if( !BioLockJUtil.isDirectMode() ) {
			if( MetaUtil.getMetadata() != null ) BioLockJ.copyFileToPipelineRoot( MetaUtil.getMetadata() );
			if( DockerUtil.inAwsEnv() ) NextflowUtil.stageRootConfig();
		}

		BioLockJUtil.initPipelineInput();

		if( !BioLockJUtil.isDirectMode() && BioLockJUtil.copyInputFiles() ) copyInputData();

		SeqUtil.initialize();
		
		if( RuntimeParamUtil.doChangePassword() ) {
			Log.info( BioLockJ.class, "Save encrypted password to: " + Config.getConfigFilePath() );
			Email.encryptAndStoreEmailPassword();
			PipelineUtil.markStatus( Constants.BLJ_COMPLETE );
			return;
		}
		
		Pipeline.initializePipeline();
		
		System.out.println(Constants.STATUS_MARK_KEY + "Done initializing BioLockJ.");
		
	}
	
	private static void checkDependencies() throws Exception{
		//ProgressUtil.startSpinner( " Checking dependencies " );
		System.out.println(Constants.STATUS_START_KEY + "Checking dependencies ");
		
		BioLockJUtil.checkVersion();
		if( ! BioLockJUtil.isDirectMode() ) MasterConfigUtil.saveMasterConfig();
		Pipeline.checkModuleDependencies();
		if( ! BioLockJUtil.isDirectMode() ) MasterConfigUtil.saveMasterConfig();
		if( ! BioLockJUtil.isDirectMode() ) Config.showUnusedProps();
		
		System.out.println(Constants.STATUS_MARK_KEY + "Done checking dependencies.");
	}

	/**
	 * Initialize restarted pipeline by:
	 * <ol>
	 * <li>Initialize {@link biolockj.Log} file using the name of the pipeline root directory
	 * <li>Update summary #Attempts count
	 * <li>Delete status file {@value biolockj.Constants#BLJ_FAILED} in pipeline root directory
	 * <li>If pipeline status = {@value biolockj.Constants#BLJ_COMPLETE}
	 * <li>Delete file {@value biolockj.util.DownloadUtil#DOWNLOAD_LIST} in pipeline root directory
	 * </ol>
	 * 
	 * @throws Exception if errors occur
	 */
	protected static void initRestart() throws Exception {
		Log.initialize( Config.pipelineName() );
		Log.warn( BioLockJ.class, RETURN + Constants.LOG_SPACER + RETURN + "RESTART_DIR PROJECT DIR --> " +
			RuntimeParamUtil.getRestartDir().getAbsolutePath() + RETURN + Constants.LOG_SPACER + RETURN );
		Log.info( BioLockJ.class, "Initializing Restarted Pipeline - this may take a couple of minutes..." );

		SummaryUtil.updateNumAttempts();
		if( DownloadUtil.getDownloadListFile().isFile() ) DownloadUtil.getDownloadListFile().delete();
		if( ValidationUtil.getValidationDir().exists() ) ValidationUtil.getValidationDir().delete();
		if( NextflowUtil.getMainNf().isFile() ) NextflowUtil.getMainNf().delete();
		//if( DockerUtil.getInfoFile().exists() ) DockerUtil.getInfoFile().delete();

		PipelineUtil.markStatus( Constants.BLJ_STARTED );
	}

	private static void pipelineShutDown() {

		setPipelineSecurity();

		if( DockerUtil.inAwsEnv() ) {
			NextflowUtil.saveNextflowLog();
			NextflowUtil.stopNextflow();
		}

		if( isPipelineComplete() ) {
			MasterConfigUtil.sanitizeMasterConfig();
			MetaUtil.tapSampleAssignmentMap();
			if( DockerUtil.inAwsEnv() ) NextflowUtil.saveNextflowSuccessFlag();
		}

		info( "Log Pipeline Summary..." + RETURN + SummaryUtil.getSummary() + SummaryUtil.displayAsciiArt() );
		if( isPipelineComplete() ) System.exit( 0 );

		System.exit( 1 );
	}

	/**
	 * Delete all {@link biolockj.module.BioModule}/{@value biolockj.module.BioModule#TEMP_DIR} folders.
	 */
	protected static void removeTempFiles() {
		Log.info( BioLockJ.class, "Cleanup BioLockJ module temp data" );
		for( final BioModule module: Pipeline.getModules() )
			if( ModuleUtil.subDirExists( module, BioModule.TEMP_DIR ) ) {
				Log.info( BioLockJ.class, "Delete temp dir for BioModule: " + module.getClass().getName() );
				module.getTempDir().delete();
			}
	}

	/**
	 * Execution summary:<br>
	 * <ol>
	 * <li>Call {@link biolockj.Pipeline#initializePipeline()} to initialize Pipeline modules
	 * <li>For direct module execution call {@link biolockj.Pipeline#runDirectModule(Integer)}
	 * <li>Otherwise execute {@link biolockj.Pipeline#runPipeline()} and save MASTER {@link biolockj.Config}
	 * <li>If {@link biolockj.Config}.{@value biolockj.Constants#RM_TEMP_FILES} = {@value biolockj.Constants#TRUE}, Call
	 * {@link #removeTempFiles()} to delete temp files
	 * <li>Call {@link biolockj.util.BioLockJUtil#createFile(String)} to set the overall pipeline status as successful
	 * </ol>
	 * 
	 * @throws Exception if runtime errors occur
	 */
	protected static void runPipeline() throws Exception {
		
		if( BioLockJUtil.isDirectMode() )
			Pipeline.runDirectModule( getDirectModuleID( RuntimeParamUtil.getDirectModuleDir() ) );
		else {
			
			if( DockerUtil.inAwsEnv() ) NextflowUtil.startNextflow( Pipeline.getModules() );

			Pipeline.runPipeline();

			if( DockerUtil.inAwsEnv() ) {
				NextflowUtil.saveNextflowLog();
				if( NextflowUtil.saveEfsDataToS3() ) NextflowUtil.purgeEfsData();
				else throw new Exception( "Pipeline completed successfully, EFS data failed to transfer to S3!" );
			}
			
			if( Config.getBoolean( null, Constants.RM_TEMP_FILES ) ) removeTempFiles();
			
			PipelineUtil.markStatus( Constants.BLJ_COMPLETE );
		}
	}

	private static void setPipelineSecurity() {
		String desiredPrivs = Config.getString( null, Constants.PIPELINE_PRIVS );
		String pipelineRoot = Config.pipelinePath();
		if( desiredPrivs != null && !desiredPrivs.isEmpty() && pipelineRoot != null && !pipelineRoot.isEmpty() ) {
			try {
				Processor.setFilePermissions( pipelineRoot, desiredPrivs );
			} catch( final Exception ex ) {
				System.out.println( "BioLockJ was unable to set pipeline filesystem privileges." );
				ex.printStackTrace();
			}
		}
	}

	private static String getDirectLogName( final String moduleDir ) throws Exception {
		final File modDir = new File( Config.pipelinePath() + File.separator + moduleDir );
		if( !modDir.isDirectory() )
			throw new Exception( "Direct module directory not found --> " + modDir.getAbsolutePath() );
		final File tempDir = new File( modDir.getAbsoluteFile() + File.separator + BioModule.TEMP_DIR );
		if( !tempDir.isDirectory() ) tempDir.mkdir();
		return modDir.getName() + File.separator + tempDir.getName() + File.separator + moduleDir;
	}

	private static Integer getDirectModuleID( final String moduleDir ) throws Exception {
		return Integer.valueOf( moduleDir.substring( 0, moduleDir.indexOf( "_" ) ) );
	}

	private static void info( final String msg ) {
		if( !BioLockJUtil.isDirectMode() ) Log.info( BioLockJ.class, msg );
	}

	private static final String RETURN = Constants.RETURN;

	/**
	 * Extract the project name from the Config file.
	 * 
	 * @return Project name
	 * @throws DockerVolCreationException 
	 */
	public static String getProjectName() throws DockerVolCreationException {
		return pipelineName;
	}
	
	public static File getPipelineDir() {
		return pipelineDir;
	}
	
	public static String getPipelineId() {
		return pipelineInstanceId;
	}

}
