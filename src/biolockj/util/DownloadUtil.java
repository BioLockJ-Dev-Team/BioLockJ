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
import java.math.BigInteger;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.*;
import biolockj.*;
import biolockj.module.BioModule;
import biolockj.module.report.JsonReport;
import biolockj.module.report.humann2.AddMetadataToPathwayTables;
import biolockj.module.report.r.R_Module;
import biolockj.module.report.taxa.*;

/**
 * This utility is used to validate the metadata to help ensure the format is valid R script input.
 */
public final class DownloadUtil {
	// Prevent instantiation
	private DownloadUtil() {}

	/**
	 * If running on cluster, build command for user to download pipeline analysis.<br>
	 * <p>
	 * The pipeline analysis is extracted from the last module output directory (except Email).<br>
	 * Download target = ModuleUtil.getDownloadDir() on the local workstation.<br>
	 * Example download command: scp -rp johnDoe@myUniversity.usa.edu:"dir1, dir2, file1"
	 * /Users/johnDoe/projects/downloads
	 *
	 * @return String (scp command) or null
	 * @throws Exception if {@link biolockj.Config} parameters are missing or invalid
	 */
	public static String getDownloadCmd() throws Exception {

		final List<BioModule> modules = getDownloadModules();

		if( buildRsyncCmd( modules ) ) {
			Log.info( DownloadUtil.class,
				"DownloadUtil building download command --> # downloadable modules: " + modules.size() );
			boolean hasRmods = false;
			final Set<File> downloadPaths = new TreeSet<>();
			for( final BioModule module: modules ) {
				Log.info( DownloadUtil.class, "Updating download list for " + module.getClass().getSimpleName() );
				if( module instanceof R_Module ) {
					downloadPaths.add( ( (R_Module) module ).getScriptDir() );
					downloadPaths.add( module.getOutputDir() );
					if( !ModuleUtil.isComplete( module ) ) downloadPaths.add( module.getTempDir() );
					hasRmods = true;
				} else if( module instanceof NormalizeTaxaTables ) {
					downloadPaths.add( module.getOutputDir() );
					downloadPaths.add( module.getTempDir() );
				} else if( module instanceof AddMetadataToTaxaTables || module instanceof AddMetadataToPathwayTables ||
					module instanceof BuildTaxaTables ) downloadPaths.add( module.getOutputDir() );
			}

			if( hasRmods ) makeRunAllScript( modules );

			final AndFileFilter filter = new AndFileFilter( EmptyFileFilter.NOT_EMPTY, HiddenFileFilter.VISIBLE );
			final Collection<File> dirs =
				FileUtils.listFiles( new File( Config.pipelinePath() ), filter, FalseFileFilter.INSTANCE );

			downloadPaths.addAll( dirs );

			if( BioLockJUtil.pipelineInternalInputDir().isDirectory() &&
				Config.requireSet( null, BioLockJUtil.INTERNAL_PIPELINE_INPUT_TYPES ).size() > 1 ||
				!BioLockJUtil.pipelineInputType( BioLockJUtil.PIPELINE_SEQ_INPUT_TYPE ) )
				downloadPaths.add( BioLockJUtil.pipelineInternalInputDir() );

			if( DockerUtil.inAwsEnv() ) downloadPaths.add( NextflowUtil.getNfReportDir() );

			final String status =
				( ModuleUtil.isComplete( modules.get( modules.size() - 1 ) ) ? "completed": "failed" ) +
					" pipeline -->";
			final String displaySize =
				FileUtils.byteCountToDisplaySize( getDownloadSize( buildDownloadList( downloadPaths ) ) );

			if( DockerUtil.inAwsEnv() ) {
				Log.info( DownloadUtil.class,
					"Size of report files = [ " + displaySize + " ]:" + getDownloadListFile().getAbsolutePath() );
				return null;
			}

			final String src = SRC + "=" + Config.pipelinePath();
			final String cmd = "rsync -prtv --chmod=a+rwx,g+rwx,o-wx --files-from=:$" + SRC + File.separator +
				getDownloadListFile().getName() + " " + getClusterUser() + "@" +
				Config.requireString( null, Constants.CLUSTER_HOST ) + ":$" + SRC + " " + getDownloadDirPath();

			return "Download " + status + " [ " + displaySize + " ]:" + RETURN + src + RETURN + cmd;
		}

		Log.info( DownloadUtil.class, "DownloadUtil found there are no downloadable modules" );
		return null;
	}

	/**
	 * Get validated {@link biolockj.Config}.{@value #DOWNLOAD_DIR} if running on cluster, otherwise return null
	 *
	 * @return String download directory file or null
	 */
	public static String getDownloadDirPath() {
		String dir = Config.getString( null, Constants.DOWNLOAD_DIR );
		if( dir != null ) {
			if( !dir.endsWith( File.separator ) ) dir = dir + File.separator;
			return dir + Config.pipelineName();
		}
		return null;
	}

	/**
	 * Get the download list file.
	 * 
	 * @return File containing list of files to download
	 */
	public static File getDownloadListFile() {
		return new File( Config.pipelinePath() + File.separator + DOWNLOAD_LIST );
	}

	/**
	 * Add files to {@value biolockj.util.DownloadUtil#DOWNLOAD_LIST} in pipeline root directory.
	 * 
	 * @param files - files to add to the download list
	 * @return List of files for download
	 * @throws Exception if errors occur
	 */
	protected static List<File> buildDownloadList( final Collection<File> files ) throws Exception {
		final List<File> downFiles = new ArrayList<>();
		final BufferedWriter writer = new BufferedWriter( new FileWriter( getDownloadListFile() ) );
		try {
			final File pipeRoot = new File( Config.pipelinePath() );
			Log.info( DownloadUtil.class, "Building download list: " + getDownloadListFile().getAbsolutePath() );
			for( final File file: files ) {
				Log.debug( DownloadUtil.class, "Candidate download path: " + file.getAbsolutePath() );
				if( FileUtils.sizeOf( file ) != 0 && !file.getName().startsWith( "." ) ) {
					downFiles.add( file );
					Log.info( DownloadUtil.class, "Add download path: " + file.getAbsolutePath() );
					final String relPath = pipeRoot.toURI().relativize( file.toURI() ).toString();
					writer.write( relPath + RETURN );
				}
			}
		} finally {
			writer.close();
		}
		return downFiles;
	}

	/**
	 * If System.user.home is a file path (like /usr/home/johnDoe), return last directory name (johnDoe). Otherwise
	 * return System.user.home value.
	 *
	 * @return The userId
	 */
	protected static String getClusterUser() {
		String user = System.getProperty( "user.home" );
		if( user == null || user.trim().length() == 0 ) return null;
		if( user.lastIndexOf( File.separator ) > 0 ) user = user.substring( user.lastIndexOf( File.separator ) + 1 );

		return user;
	}

	/**
	 * Get a directory name filter to include module sub-directories.
	 * 
	 * @param includeOutput include the output directory
	 * @param includeScript include the script directory
	 * @param includeTemp include the temp directory
	 * @return a file name filter
	 */
	protected static IOFileFilter getDirFilter( final boolean includeOutput, final boolean includeScript,
		final boolean includeTemp ) {
		final ArrayList<String> dirFilter = new ArrayList<>();
		if( includeOutput ) dirFilter.add( BioModule.OUTPUT_DIR );
		if( includeScript ) dirFilter.add( Constants.SCRIPT_DIR );
		if( includeTemp ) dirFilter.add( BioModule.TEMP_DIR );
		return new NameFileFilter( dirFilter );
	}

	/**
	 * Get the modules to download. The following are downloaded if found in the pipeline:
	 * <ul>
	 * <li>{@link biolockj.module.report.taxa.AddMetadataToTaxaTables}
	 * <li>{@link biolockj.module.report.humann2.AddMetadataToPathwayTables}
	 * <li>{@link biolockj.module.report.JsonReport}
	 * <li>Any module that inherits from {@link biolockj.module.report.r.R_Module}
	 * </ul>
	 * 
	 * If pipeline contains {@link biolockj.module.report.r.R_PlotEffectSize} and
	 * {@link biolockj.Config}.{@value biolockj.Constants#R_PLOT_EFFECT_SIZE_DISABLE_FC}={@value biolockj.Constants#FALSE}
	 * include additional modules:
	 * <ul>
	 * <li>{@link biolockj.module.implicit.parser.wgs.Humann2Parser}
	 * <li>{@link biolockj.module.report.taxa.BuildTaxaTables}
	 * </ul>
	 * 
	 * @return BioModules to download
	 */
	protected static List<BioModule> getDownloadModules() {
		final List<BioModule> modules = new ArrayList<>();
		try {
			for( final BioModule module: Pipeline.getModules() ) {

				final boolean downloadableType = module instanceof JsonReport || module instanceof R_Module ||
					module instanceof AddMetadataToTaxaTables || module instanceof AddMetadataToPathwayTables ||
					module instanceof BuildTaxaTables || module instanceof NormalizeTaxaTables;

				if( ModuleUtil.hasExecuted( module ) && downloadableType ) modules.add( module );
			}
		} catch( final Exception ex ) {
			Log.warn( DownloadUtil.class, "Unable to find any executed modules to summarize: " + ex.getMessage() );
		}
		return modules;
	}

	/**
	 * Get the total size of all files included for download.
	 * 
	 * @param files List of download files
	 * @return BigInteger total download size
	 */
	protected static BigInteger getDownloadSize( final List<File> files ) {
		BigInteger downloadSize = BigInteger.valueOf( 0L );
		for( final File file: files )
			downloadSize = downloadSize.add( FileUtils.sizeOfAsBigInteger( file ) );

		return downloadSize;
	}

	/**
	 * This script allows a user to run all R scripts together from a single script.
	 * 
	 * @param modules BioModules
	 * @return File Run_All.R script
	 * @throws Exception if errors occur
	 */
	protected static File makeRunAllScript( final List<BioModule> modules ) throws Exception {

		final File pipeRoot = new File( Config.pipelinePath() );
		final File script = getRunAllRScript();
		final BufferedWriter writer = new BufferedWriter( new FileWriter( script ) );

		if( Config.getString( null, Constants.SCRIPT_DEFAULT_HEADER ) != null )
			writer.write( Config.getString( null, Constants.SCRIPT_DEFAULT_HEADER ) + RETURN + RETURN );

		writer.write( "# Use this script to locally run R modules." + RETURN );

//		for( final BioModule mod: modules )
//			if( mod instanceof R_Module ) {
//				final String relPath =
//					pipeRoot.toURI().relativize( ( (R_Module) mod ).getPrimaryScript().toURI() ).toString();
//				writer.write( Constants.RSCRIPT + " " + relPath + RETURN );
//			}

		writer.close();

		return script;
	}

	private static boolean buildRsyncCmd( final List<BioModule> modules ) {
		return modules != null && !modules.isEmpty() &&
			( DockerUtil.inAwsEnv() || Config.isOnCluster() && getDownloadDirPath() != null );
	}

	private static File getRunAllRScript() {
		return new File( Config.pipelinePath() + File.separator + RUN_ALL_SCRIPT );
	}

	/**
	 * Name of the file holding the list of pipeline files to include when running {@link biolockj.util.DownloadUtil}
	 */
	public static final String DOWNLOAD_LIST = "downloadList.txt";

	private static final String RETURN = Constants.RETURN;
	private static final String RUN_ALL_SCRIPT = "Run_All_R" + Constants.SH_EXT;
	private static final String SRC = "src";
}
