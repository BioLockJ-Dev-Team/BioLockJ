/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 11, 2019
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.util;

import java.io.*;
import java.util.*;
import org.apache.commons.io.FileUtils;
import biolockj.*;
import biolockj.Properties;
import biolockj.api.API_Exception;
import biolockj.exception.*;
import biolockj.module.*;
import biolockj.module.implicit.ImportMetadata;
import biolockj.module.report.Email;

/**
 * This utility builds the Nextflow main.nf used in AWS pipelines.
 */
public class NextflowUtil {

	/**
	 * Sync file or directory with S3 bucket.<br>
	 * Dir example: aws s3 sync $EFS/config s3://blj-2019-04-05/config<br>
	 * File example: aws s3 cp ${BLJ_META}/testMetadata.tsv s3://blj-2019-04-05/metadata/testMetadata.tsv Register each
	 * efsPath to avoid syncing the same path more than once.
	 * 
	 * 
	 * @param efsPath File or directory to sync
	 * @param waitUntilComplete Boolean if enabled will block until process completes before moving on
	 * @throws Exception if errors occur
	 */
	public static void awsSyncS3( final String efsPath, final boolean waitUntilComplete ) throws Exception {
		if( s3SyncRegister.contains( efsPath ) ) {
			Log.info( NextflowUtil.class, "Ignore sync request - sync already requested for: " + efsPath );
			return;
		}

		s3SyncRegister.add( efsPath );

		final boolean isDir = new File( efsPath ).isDirectory();

		String s3Dir = getAwsS3();
		if( efsPath.contains( Config.pipelinePath() ) ) s3Dir += efsPath.replace( Config.pipelinePath(), "" );
		Log.info( BioLockJ.class, "Transfer " + efsPath + " to --> " + s3Dir );

		final String[] s3args = new String[ isDir ? 7: 5 ];
		s3args[ 0 ] = "aws";
		s3args[ 1 ] = "s3";
		s3args[ 2 ] = isDir ? "sync": "cp";
		s3args[ 3 ] = efsPath;
		s3args[ 4 ] = s3Dir;
		if( isDir ) {
			s3args[ 5 ] = "--exclude";
			s3args[ 6 ] = Constants.BLJ_COMPLETE;
		}

		if( waitUntilComplete ) Processor.submitJob( s3args, "S3-Sync-xFer" );
		else Processor.runSubprocess( s3args, "S3-Async-xFer" );
	}

	/**
	 * Get the Nextflow main.nf file path.
	 * 
	 * @return Nextflow main.nf
	 */
	public static File getMainNf() {
		return new File( Config.pipelinePath() + File.separator + MAIN_NF );
	}

	/**
	 * Get the Nextflow report directory
	 * 
	 * @return Nextflow report directory
	 */
	public static File getNfReportDir() {
		final File dir = new File( Config.pipelinePath() + File.separator + NEXTFLOW );
		if( !dir.isDirectory() ) dir.mkdir();
		return dir;
	}

	/**
	 * Get S3 Transfer timeout limit - default = 30 minutes if undefined
	 * 
	 * @return Number of minutes beforer S3 transfer thread will abort
	 */
	public static int getS3_TransferTimeout() {
		try {
			return Config.requirePositiveInteger( null, Constants.AWS_S3_XFER_TIMEOUT );
		} catch( final Exception ex ) {
			Log.error( NextflowUtil.class, "Error occurred waiting for subprocess to compelete!", ex );
		}
		return DEFAULT_S3_TIMEOUT;
	}

	/**
	 * Return true if the Nextflow log has been saved to the pipeline root directory
	 * 
	 * @return TRUE if log exists in pipeline root directory
	 */
	public static boolean nextflowLogExists() {
		return new File( getNfReportDir().getAbsolutePath() + File.separator + NF_LOG_NAME ).isFile();
	}

	/**
	 * Purge EFS data based on pipeline Config.
	 * 
	 * @return TRUE if not errors occur
	 */
	public static boolean purgeEfsData() { //TODO: this needs to be smarted after the dockerVols generalization
		try {
			if( Config.getBoolean( null, AWS_PURGE_EFS_OUTPUT ) ) purge( Config.pipelinePath() );
			else if( Config.getBoolean( null, AWS_PURGE_EFS_INPUTS ) ) {
				purge( DockerUtil.DOCKER_DB_DIR );
			}
			return true;
		} catch( final Exception ex ) {
			Log.error( NextflowUtil.class, "Failed to save datat to S3", ex );
			return false;
		}
	}

	/**
	 * Save EFS data to S3 based on pipeline Config.
	 * 
	 * @return TRUE if not errors occur
	 */
	public static boolean saveEfsDataToS3() {
		try {
			if( Config.getBoolean( null, AWS_COPY_PIPELINE_TO_S3 ) ) awsSyncS3( Config.pipelinePath(), true );
			else if( DownloadUtil.getDownloadListFile().isFile() &&
				Config.getBoolean( null, AWS_COPY_REPORTS_TO_S3 ) ) {
				final BufferedReader reader = BioLockJUtil.getFileReader( DownloadUtil.getDownloadListFile() );
				try {
					for( String path = reader.readLine(); path != null; path = reader.readLine() )
						awsSyncS3( Config.pipelinePath() + File.separator + path, true );
				} finally {
					if( reader != null ) reader.close();
				}
			} else Log.warn( NextflowUtil.class,
				"Pipeline ouput will be not saved to configured AWS S3 bucket: " +
					Config.requireString( null, AWS_S3 ) + " due to Config properties [ " + AWS_COPY_PIPELINE_TO_S3 +
					"=" + Constants.FALSE + " ] & [ " + AWS_COPY_REPORTS_TO_S3 + "=" + Constants.FALSE + " ]" );

			return true;
		} catch( final Exception ex ) {
			Log.error( NextflowUtil.class, "Failed to save datat to S3", ex );
			return false;
		}
	}

	/**
	 * Save a copy of the Nextflow log file to the Pipeline root directory
	 */
	public static void saveNextflowLog() {
		if( nextflowLogExists() ) return;
		try {
			final File log = new File( NF_LOG );
			if( !log.isFile() ) {
				Log.warn( NextflowUtil.class, NF_LOG + " not found, cannot copy to pipeline root directory!" );
				return;
			}
			FileUtils.copyFile( log,
				new File( getNfReportDir().getAbsolutePath() + File.separator + NEXTFLOW + Constants.LOG_EXT ) );
		} catch( final Exception ex ) {
			Log.error( NextflowUtil.class, "Failed to copy nextflow.log to pipeline root directory ", ex );
		}
	}

	/**
	 * Save success flag, so after pipeline bash start script can stop/terminate S3 instances if successful.
	 */
	public static void saveNextflowSuccessFlag() {
		try {
			final File f = BioLockJUtil.createFile(
				DockerUtil.AWS_EC2_HOME + File.separator + BioLockJ.getProjectName() + "-success" );
			if( f.isFile() ) Log.info( NextflowUtil.class, "Created pipeline success file: " + f.getAbsolutePath() );
			else Log.warn( NextflowUtil.class, "Failed to generate pipeline success file: " + f.getAbsolutePath() );
		} catch( final Exception ex ) {
			Log.error( NextflowUtil.class, "Error occurred attempting to save pipeline success indicator file", ex );
		}
	}

	/**
	 * Before any AWS or Nextflow functionality can be used, the Docker root user $HOME directory must be updated with
	 * the EC2 user aws + Nextflow config.
	 * 
	 * @throws IOException if source or target config directories are not found
	 */
	public static void stageRootConfig() throws IOException {
		final File ec2Aws = new File( DockerUtil.AWS_EC2_HOME + File.separator + AWS_DIR );
		final File ec2NfConfig =
			new File( DockerUtil.AWS_EC2_HOME + File.separator + NF_DIR + File.separator + "config" );
		final File rootNfDir = new File( DockerUtil.ROOT_HOME + File.separator + NF_DIR );
		final File rootNfConfig = new File( rootNfDir.getAbsolutePath() + File.separator + "config" );
		final File rootAwsConfig =
			new File( DockerUtil.ROOT_HOME + File.separator + AWS_DIR + File.separator + "config" );
		final File rootAwsCred =
			new File( DockerUtil.ROOT_HOME + File.separator + AWS_DIR + File.separator + "credentials" );
		FileUtils.copyFileToDirectory( ec2NfConfig, rootNfDir );
		FileUtils.copyDirectoryToDirectory( ec2Aws, new File( DockerUtil.ROOT_HOME ) );
		if( !rootNfConfig.isFile() )
			throw new IOException( "Root Nextflow config not found --> " + rootNfDir.getAbsolutePath() );
		if( !rootAwsConfig.isFile() )
			throw new IOException( "Root AWS config not found --> " + rootAwsConfig.getAbsolutePath() );
		if( !rootAwsCred.isFile() )
			throw new IOException( "Root Nextflow credentials not found --> " + rootAwsCred.getAbsolutePath() );
	}

	/**
	 * Call this method to build the Nextflow main.nf for the current pipeline.
	 * 
	 * @param modules Pipeline modules
	 * @throws Exception if errors occur
	 */
	public static void startNextflow( final List<BioModule> modules ) throws Exception {
		final String plist = buildNextflowProcessList( modules );
		if( plist.isEmpty() ) {
			Log.warn( NextflowUtil.class,
				"Nextflow not neccesary for this pipeline.  All modules are attached java_modules that run on the head node." );
			return;
		}
		final File template = buildInitialTemplate( plist );
		writeNextflowMainNF( getNextflowLines( template ) );
		Log.info( NextflowUtil.class, "Nextflow main.nf generated: " + getMainNf().getAbsolutePath() );
		template.delete();
		startService();
		pollAndSpin();
		Log.info( NextflowUtil.class, "Nextflow service sub-process started!" );
	}

	/**
	 * Stop Nextflow process (required since parent Java process that ran BioLockJ pipeline will not halt until this
	 * subprocess is finished. Also create pipeline success flag file in $HOME dir on the EC2 head node.
	 */
	public static void stopNextflow() {
		if( nfMainThread != null ) {
			nfMainThread.interrupt();
			Processor.deregisterThread( nfMainThread );
			Log.info( NextflowUtil.class, "Nextflow process thread de-registered" );
		}

		try {
			while( Processor.subProcsAlive() ) {
				Log.warn( NextflowUtil.class, "Standard execution complete - waiting for S3-Data-xFers to complete" );
				Thread.sleep( BioLockJUtil.minutesToMillis( 1 ) );
			}
		} catch( final Exception ex ) {
			Log.error( NextflowUtil.class, "Error occurred waiting for subprocess to compelete!", ex );
		}
	}

	/**
	 * Build the main.nf lines from the template file by replacing several parameters.
	 * 
	 * @param template Generated .main.nf template
	 * @return List of lines to save in final main.nf
	 * @throws Exception if errors occur
	 */
	protected static List<String> getNextflowLines( final File template ) throws Exception {
		Log.info( NextflowUtil.class, "Build main.nf from the Nextflow template: " + template.getAbsolutePath() );
		final List<String> lines = new ArrayList<>();
		final BufferedReader reader = BioLockJUtil.getFileReader( template );
		try {
			ScriptModule module = null;
			for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
				String onDemandLabel = null;
				Log.debug( NextflowUtil.class, "READ LINE: " + line );
				if( line.trim().startsWith( PROCESS ) ) {
					module = getModule( line.replace( PROCESS, "" ).replaceAll( "\\{", "" ).trim() );
					line = line.replaceAll( "\\.", "_" );
				} else if( module != null ) if( line.contains( NF_CPUS ) )
					line = line.replace( NF_CPUS, Config.getString( module, NF_CPUS.substring( 1 ) ) );
				else if( line.contains( NF_MEMORY ) ) line = line.replace( NF_MEMORY,
					"'" + Config.requirePositiveInteger( module, NF_MEMORY.substring( 1 ) ) + " GB'" );
				else if( line.contains( NF_DOCKER_IMAGE ) ) {
					line = line.replace( NF_DOCKER_IMAGE, getDockerImageLabel( module ) );
					if( Config.requireString( module, EC2_ACQUISITION_STRATEGY ).toUpperCase().equals( ON_DEMAND ) )
						onDemandLabel = "    label '" + ON_DEMAND + "'";
				} else if( line.contains( MODULE_SCRIPT ) )
					line = line.replace( MODULE_SCRIPT, module.getScriptDir().getAbsolutePath() );
				else if( line.trim().equals( "}" ) ) module = null;

				Log.debug( NextflowUtil.class, "ADD LINE: " + line );
				lines.add( line );
				if( onDemandLabel != null ) lines.add( onDemandLabel );
			}
		} finally {
			if( reader != null ) reader.close();
		}
		Log.info( NextflowUtil.class, "Done building main.nf with # lines = " + lines.size() );
		return lines;
	}

	private static File buildInitialTemplate( final String modules ) throws Exception {
		Log.info( NextflowUtil.class, "Build Nextflow initial template: " + templateConfig().getAbsolutePath() );
		Log.info( NextflowUtil.class, "Nextflow modules: " + modules );

		final String[] args = new String[ 3 ];
		args[ 0 ] = templateScript().getAbsolutePath();
		args[ 1 ] = templateConfig().getAbsolutePath();
		args[ 2 ] = modules;
		Processor.submitJob( args, "Build Nf Template" );
		if( !templateConfig().isFile() )
			throw new Exception( "Nextflow Template file is not found at path: " + templateConfig().getAbsolutePath() );
		Log.info( NextflowUtil.class, "Nextflow Template file created: " + templateConfig().getAbsolutePath() );
		return templateConfig();
	}

	private static String buildNextflowProcessList( final List<BioModule> modules ) throws ConfigFormatException {
		String plist = "";
		for( final BioModule m: modules ) {
			if( m instanceof ImportMetadata || m instanceof Email ) continue;
			if( m instanceof JavaModule && !Config.getBoolean( m, Constants.DETACH_JAVA_MODULES ) ) Log.warn(
				NextflowUtil.class,
				"Confg property [ " + Constants.DETACH_JAVA_MODULES + "=" + Constants.FALSE + " ] so JavaModule \"" +
					m.getClass().getName() +
					"\" must run on a head node deployed with SUFFICIENT RESOURCES --> Current Config value --> [ " +
					EC2_INSTANCE_TYPE + "=" + Config.getString( m, EC2_INSTANCE_TYPE ) + " ]" );
			else plist += ( plist.isEmpty() ? "": " " ) + m.getClass().getName();
		}
		return plist;
	}

	private static String getAwsS3() throws Exception {
		final String s3 = Config.requireString( null, AWS_S3 ) + File.separator + Config.pipelineName();
		if( s3.startsWith( S3_DIR ) ) return s3;
		return S3_DIR + s3;
	}

	private static String getDockerImageLabel( final BioModule module ) throws ConfigNotFoundException {
		return "'" + IMAGE + "_" + DockerUtil.getDockerImage(module).replaceAll("/", "_").replaceAll(":", "_");
	}

	private static ScriptModule getModule( final String className ) {
		Log.debug( NextflowUtil.class, "Calling getModule( " + className + " )" );
		for( final BioModule module: Pipeline.getModules() )
			if( module.getClass().getName().equals( className ) )
				if( usedModules.contains( module.getID() ) ) Log.debug( NextflowUtil.class,
					"Skip module [ ID = " + module.getID() +
						" ] in since it was already used, look for another module of type: " +
						module.getClass().getName() );
				else {
				Log.debug( NextflowUtil.class, "getModule( " + className + " ) RETURN module [ ID = " + module.getID() +
					" ] --> " + module.getClass().getName() );
				usedModules.add( module.getID() );
				return (ScriptModule) module;
				}
		return null;
	}

	private static boolean poll() throws Exception {
		final File nfLog = new File( NF_LOG );
		if( nfLog.isFile() ) {
			final BufferedReader reader = BioLockJUtil.getFileReader( nfLog );
			try {
				for( String line = reader.readLine(); line != null; line = reader.readLine() )
					if( line.contains( NF_INIT_FLAG ) ) return true;
			} finally {
				if( reader != null ) reader.close();
			}
		} else Log.info( NextflowUtil.class, "Nextflow log file \"" + NF_LOG + "\" has not been created yet..." );
		return false;
	}

	private static void pollAndSpin() throws Exception {
		Log.info( NextflowUtil.class, "Nextflow initializing...Poll " + NF_LOG +
			" every 30 seconds until the status message \"" + NF_INIT_FLAG + "\" is logged" );
		int numSecs = 0;
		boolean finished = false;
		while( !finished ) {
			finished = poll();
			if( !finished ) {
				if( numSecs > NF_TIMEOUT ) throw new Exception( "Nextflow timed out after " + numSecs + " seconds." );
				Thread.sleep( 30 * 1000 );
				numSecs += 30;
			} else Log.info( NextflowUtil.class, "Nextflow initialization complete!" );
		}
	}

	private static boolean purge( final String path ) throws Exception {
		Log.info( BioLockJ.class, "Delete everything under/including --> " + path );
		final String[] args = new String[ 3 ];
		args[ 0 ] = "rm";
		args[ 1 ] = "-rf";
		args[ 2 ] = path + File.separator + "*";
		Processor.submitJob( args, "Clear-AWS-Data" );
		return true;
	}

	private static void setNfMainThread( final Thread thread ) {
		nfMainThread = thread;
	}

	private static void startService() throws ConfigNotFoundException {
		final String reportBase = getNfReportDir().getAbsolutePath() + File.separator + Config.pipelineName() + "_";
		final String[] args = new String[ 11 ];
		args[ 0 ] = NEXTFLOW;
		args[ 1 ] = "run";
		args[ 2 ] = "-work-dir";
		args[ 3 ] = S3_DIR + Config.requireString( null, AWS_S3 ) + File.separator + NEXTFLOW;
		args[ 4 ] = "-with-trace";
		args[ 5 ] = reportBase + "nextflow_trace.tsv";
		args[ 6 ] = "-with-timeline";
		args[ 7 ] = reportBase + "nextflow_timeline.html";
		args[ 8 ] = "-with-dag";
		args[ 9 ] = reportBase + "nextflow_diagram.html";
		args[ 10 ] = getMainNf().getAbsolutePath();
		setNfMainThread( Processor.runSubprocess( args, "Nextflow" ) );
	}

	private static File templateConfig() {
		return new File( Config.pipelinePath() + File.separator + ".template_" + MAIN_NF );
	}

	private static File templateScript() throws ConfigPathException {
		return new File( BioLockJUtil.getBljDir().getAbsolutePath() + File.separator + Constants.SCRIPT_DIR +
			File.separator + MAKE_NEXTFLOW_SCRIPT );
	}

	private static void writeNextflowMainNF( final List<String> lines ) throws IOException {
		Log.debug( NextflowUtil.class, "Create " + getMainNf().getAbsolutePath() + " with # lines = " + lines.size() );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( getMainNf() ) );
		try {
			boolean indent = false;
			for( String line: lines ) {
				if( line.trim().equals( "}" ) ) indent = !indent;
				if( indent ) line = "    " + line;
				writer.write( line + Constants.RETURN );
				if( line.trim().endsWith( "{" ) ) indent = !indent;
			}
		} finally {
			writer.close();
		}
	}

	/**
	 * Register properties with the Properties class for API access.
	 * @throws API_Exception 
	 */
	public static void registerProps() throws API_Exception {
		Properties.registerProp( AWS_COPY_DB_TO_S3, Properties.BOOLEAN_TYPE, AWS_COPY_DB_TO_S3_DESC);
		Properties.registerProp( AWS_COPY_PIPELINE_TO_S3, Properties.BOOLEAN_TYPE, AWS_COPY_PIPELINE_TO_S3_DESC);
		Properties.registerProp( AWS_COPY_REPORTS_TO_S3, Properties.BOOLEAN_TYPE, AWS_COPY_REPORTS_TO_S3_DESC);
		Properties.registerProp( AWS_PURGE_EFS_INPUTS, Properties.BOOLEAN_TYPE, AWS_PURGE_EFS_INPUTS_DESC);
		Properties.registerProp( AWS_PURGE_EFS_OUTPUT, Properties.BOOLEAN_TYPE, AWS_PURGE_EFS_OUTPUT_DESC);
		Properties.registerProp( AWS_RAM, Properties.STRING_TYPE, AWS_RAM_DESC );  // TODO: follow-up on type
		Properties.registerProp( AWS_S3, Properties.STRING_TYPE, AWS_S3_DESC );
		Properties.registerProp( EC2_ACQUISITION_STRATEGY, Properties.STRING_TYPE, EC2_ACQUISITION_STRATEGY_DESC );
		Properties.registerProp( EC2_INSTANCE_TYPE, Properties.STRING_TYPE, EC2_INSTANCE_TYPE_DESC );
	}
	
	/**
	 * Name of the AWS S3 sub-directory used to save pipeline reports
	 */
	public static final String AWS_CONFIG_DIR = "config";

	/**
	 * {@link biolockj.Config} Boolean property:{@value #AWS_COPY_DB_TO_S3}<br>
	 * {@value AWS_COPY_DB_TO_S3_DESC}
	 */
	public static final String AWS_COPY_DB_TO_S3 = "aws.copyDbToS3";
	private static final String AWS_COPY_DB_TO_S3_DESC = "If true, save all input files to S3";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #AWS_COPY_PIPELINE_TO_S3}<br>
	 * {@value #AWS_COPY_PIPELINE_TO_S3_DESC}
	 */
	public static final String AWS_COPY_PIPELINE_TO_S3 = "aws.copyPipelineToS3";
	private static final String AWS_COPY_PIPELINE_TO_S3_DESC = "If enabled save pipeline to S3";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #AWS_COPY_PIPELINE_TO_S3}<br>
	 * {@value #AWS_COPY_REPORTS_TO_S3_DESC}
	 */
	public static final String AWS_COPY_REPORTS_TO_S3 = "aws.copyReportsToS3";
	private static final String AWS_COPY_REPORTS_TO_S3_DESC = "If enabled save reports to S3";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #AWS_PURGE_EFS_INPUTS}<br>
	 * {@value #AWS_PURGE_EFS_INPUTS_DESC}
	 */
	public static final String AWS_PURGE_EFS_INPUTS = "aws.purgeEfsInputs";
	private static final String AWS_PURGE_EFS_INPUTS_DESC = "If enabled delete all EFS dirs (except pipelines)";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #AWS_PURGE_EFS_OUTPUT}<br>
	 * {@value #AWS_PURGE_EFS_OUTPUT_DESC}
	 */
	public static final String AWS_PURGE_EFS_OUTPUT = "aws.purgeEfsOutput";
	private static final String AWS_PURGE_EFS_OUTPUT_DESC = "If enabled delete all EFS/pipelines";

	/**
	 * {@link biolockj.Config} String property: {@value #AWS_RAM}<br>
	 * {@value #AWS_RAM_DESC}
	 */
	public static final String AWS_RAM = "aws.ram";
	private static final String AWS_RAM_DESC = "AWS memory set in Nextflow main.nf";

	/**
	 * Name of the AWS S3 subdirectory used to save pipeline reports
	 */
	public static final String AWS_REPORT_DIR = "reports";

	/**
	 * {@link biolockj.Config} String property: {@value #AWS_S3}<br>
	 * {@value #AWS_S3_DESC}
	 */
	public static final String AWS_S3 = "aws.s3";
	private static final String AWS_S3_DESC = "AWS S3 pipeline output directory used by Nextflow main.nf";

	/**
	 * {@link biolockj.Config} String property: {@value #EC2_ACQUISITION_STRATEGY}<br>
	 * {@value #EC2_ACQUISITION_STRATEGY_DESC}
	 */
	protected static final String EC2_ACQUISITION_STRATEGY = "aws.ec2AcquisitionStrategy";
	private static final String EC2_ACQUISITION_STRATEGY_DESC = "The AWS acquisition strategy (SPOT or DEMAND) sets the service SLA for procuring new EC2 instances";

	/**
	 * {@link biolockj.Config} String property: {@value #EC2_INSTANCE_TYPE}<br>
	 * {@value #EC2_INSTANCE_TYPE_DESC}
	 */
	protected static final String EC2_INSTANCE_TYPE = "aws.ec2InstanceType";
	private static final String EC2_INSTANCE_TYPE_DESC = "AWS instance type determines initial resource class (t2.micro is common)";

	/**
	 * The Docker container will generate a nextflow.log file in the root directory, this is the file name
	 */
	protected static final String NF_LOG = "/.nextflow.log";

	private static final String AWS_DIR = ".aws";
	private static final Integer DEFAULT_S3_TIMEOUT = 30;
	private static final String IMAGE = "image";
	private static final String MAIN_NF = "main.nf";
	private static final String MAKE_NEXTFLOW_SCRIPT = "make_nextflow";
	private static final String MODULE_SCRIPT = "BLJ_MODULE_SUB_DIR";
	private static final String NEXTFLOW = "nextflow";
	private static final String NF_CPUS = "$" + Constants.SCRIPT_NUM_THREADS;
	private static final String NF_DIR = ".nextflow";
	private static final String NF_DOCKER_IMAGE = "$nextflow.dockerImage";
	private static final String NF_INIT_FLAG = "Session await";
	private static final String NF_LOG_NAME = ".nextflow.log";
	private static final String NF_MEMORY = "$" + AWS_RAM;
	private static final int NF_TIMEOUT = 180;
	private static Thread nfMainThread = null;
	private static final String ON_DEMAND = "DEMAND";
	private static final String PROCESS = "process";
	private static final String S3_DIR = "s3://";
	private static final Set<String> s3SyncRegister = new HashSet<>();
	private static final Set<Integer> usedModules = new HashSet<>();
}
