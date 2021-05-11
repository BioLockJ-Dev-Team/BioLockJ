/**
 * @UNCC Fodor Lab
 * @author Anthony Fodor
 * @email anthony.fodor@gmail.com
 * @date May 29, 2018
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
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.*;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Constants;
import biolockj.Log;
import biolockj.Pipeline;
import biolockj.Processor;
import biolockj.exception.DockerVolCreationException;
import biolockj.module.BioModule;
import biolockj.module.BioModuleImpl;
import biolockj.module.ScriptModule;

/**
 * This module builds an execution summary for the pipeline which is printed to the log file and is be sent to the user
 * if the Email module is configured.
 */
public class SummaryUtil {
	
	/**
	 * Save information about the system, such as OS name and version.
	 */
	public static void touchSystemInfo() {
		File infoFile = getSystemInfoFile();
		try {
			if( !infoFile.exists() ) {
				Log.info( SummaryUtil.class, "Creating " + infoFile.getName() + " file." );
				writeSystemInfo(infoFile);
			}
		} catch( Exception e ) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Write the system host info.
	 * This is used when an instance of 
	 * BioLockJ running outside of a container creates the system info record.  
	 * The instance inside the container will see the container info, not the host.
	 * @param pipelineDir
	 */
	public static boolean sendHostInfo(File pipelineDir) {
		boolean created = false;
		File file = new File( pipelineDir, HOST_INFO_FILE );
		if (file.exists()) created = false;
		else {
			try {
				writeSystemInfo(file);
				created = true;
			} catch( Exception e ) {
				e.printStackTrace();
				created = false;
			}
		}
		return created;
	}
	
	private static File getSystemInfoFile() {
		File parentDir = BioLockJ.getPipelineDir();
		if( BioLockJUtil.isDirectMode() )
			parentDir = new File( ( new File( BioLockJ.getPipelineDir(), RuntimeParamUtil.getDirectModuleDir() ) ),
				BioModuleImpl.TEMP_DIR );
		if( parentDir != null && parentDir.exists() ) {
			File file;
			file = new File( parentDir, SYSTEM_INFO_FILE );
			Log.debug( SummaryUtil.class,
				"path to info file: " + file.getAbsolutePath() );
			return file;
		} else {
			return null;
		}
	}
	private static final String SYSTEM_INFO_FILE = "systemInfo.txt";
	private static final String HOST_INFO_FILE = "systemInfo-host.txt";
	
	private static void writeSystemInfo(File infoFile) throws IOException {
		final BufferedWriter writer = new BufferedWriter( new FileWriter( infoFile ) );
		Properties props = System.getProperties();
		List<String> keys = new ArrayList<>();
		props.keySet().forEach( (obj) -> { keys.add( obj.toString() ); } ); 
		Collections.sort( keys );
		for( String key: keys ) {
			if( key.contains( "java" ) || key.contains( "os." ) || key.contains( "version" ) ) {
				String msg = key + ": " + props.getProperty( key ) + System.lineSeparator();
				Log.debug( SummaryUtil.class, msg );
				writer.write( msg );
			}
		}
		writer.close();
	}

	/**
	 * Called when pipeline fails to add summary details to summary file, if possible.
	 */
	public static void addSummaryFooterForFailedPipeline() {
		String summaryFile = "";
		try {
			summaryFile = getSummaryFile().getAbsolutePath();
			saveSummary( getFooter() );
		} catch( final Exception ex ) {
			Log.error( SummaryUtil.class, "Unable to update summary file: " + summaryFile, ex );
		}
	}

	/**
	 * Print the application name *bigly* with some ASCII art :-)
	 * 
	 * @return My beautiful artwork
	 */
	public static String displayAsciiArt() {
		final StringBuffer sb = new StringBuffer();
		sb.append(
			"  _______      ___      ________      ___           ________      ________      ___ ____            __" +
				RETURN );
		sb.append(
			"|\\   __  \\    |\\  \\    |\\   __  \\    |\\  \\         |\\   __  \\    |\\   ____\\    |\\  \\|\\  \\         |\\  \\" +
				RETURN );
		sb.append(
			"\\ \\  \\|\\ /_   \\ \\  \\   \\ \\  \\|\\  \\   \\ \\  \\        \\ \\  \\|\\  \\   \\ \\  \\___|    \\ \\  \\/  /|_       \\ \\  \\" +
				RETURN );
		sb.append(
			" \\ \\   __  \\   \\ \\  \\   \\ \\  \\\\\\  \\   \\ \\  \\        \\ \\  \\\\\\  \\   \\ \\  \\        \\ \\   ___  \\    __ \\ \\  \\" +
				RETURN );
		sb.append(
			"  \\ \\  \\|\\  \\   \\ \\  \\   \\ \\  \\\\\\  \\   \\ \\  \\____    \\ \\  \\\\\\  \\   \\ \\  \\____    \\ \\  \\\\ \\  \\  |\\  \\\\_\\  \\" +
				RETURN );
		sb.append(
			"   \\ \\_______\\   \\ \\__\\   \\ \\_______\\   \\ \\_______\\   \\ \\_______\\   \\ \\_______\\   \\ \\__\\\\ \\__\\ \\ \\________\\" +
				RETURN );
		sb.append(
			"    \\|_______|    \\|__|    \\|_______|    \\|_______|    \\|_______|    \\|_______|    \\|__| \\|__|  \\|________|" +
				RETURN );
		return sb.toString() + RETURN + ( BioLockJ.isPipelineComplete() ?
			getSpaces( 16 ) + spacedWord( "COMPLETE", 10 ): getSpaces( 27 ) + spacedWord( "FAILED", 10 ) ) + RETURN +
			RETURN;
	}

	/**
	 * Return the min/max/mean/median summary stats for the given metadata numeric column
	 * 
	 * @param map Map(sampleId,count)
	 * @param label Context label
	 * @param addTotal Boolean if should include total count
	 * @return Summary lines
	 * @throws Exception if errors occur
	 */
	public static String getCountSummary( final Map<String, String> map, final String label, final boolean addTotal )
		throws Exception {
		final int pad = getPad( label );
		String msg = BioLockJUtil.addTrailingSpaces( "# Samples:", pad ) +
			BioLockJUtil.formatNumericOutput( new Integer( map.size() ).longValue(), false ) + RETURN;

		if( !map.isEmpty() ) {
			final TreeSet<Long> vals =
				new TreeSet<>( map.values().stream().map( Long::parseLong ).collect( Collectors.toSet() ) );

			msg += BioLockJUtil.addTrailingSpaces( "# " + label + " (min):", pad ) +
				BioLockJUtil.formatNumericOutput( vals.first(), false ) + RETURN;
			msg += BioLockJUtil.addTrailingSpaces( "# " + label + " (median):", pad ) +
				BioLockJUtil.formatNumericOutput( Long.valueOf( SummaryUtil.getMedian( vals, false ) ), false ) +
				RETURN;
			msg += BioLockJUtil.addTrailingSpaces( "# " + label + " (mean):", pad ) +
				BioLockJUtil.formatNumericOutput( Long.valueOf( SummaryUtil.getMean( vals, false ) ), false ) + RETURN;
			msg += BioLockJUtil.addTrailingSpaces( "# " + label + " (max):", pad ) +
				BioLockJUtil.formatNumericOutput( vals.last(), false ) + RETURN;

			Long sum = 0L;
			for( final long val: vals )
				sum += val;

			if( addTotal ) msg += BioLockJUtil.addTrailingSpaces( "# " + label + " (total):", pad ) +
				BioLockJUtil.formatNumericOutput( sum, false ) + RETURN;
			if( !vals.first().equals( vals.last() ) ) {
				final Set<String> minSamples = new HashSet<>();
				final Set<String> maxSamples = new HashSet<>();
				for( final String id: map.keySet() ) {
					if( map.get( id ).equals( vals.first().toString() ) ) minSamples.add( id );
					if( map.get( id ).equals( vals.last().toString() ) ) maxSamples.add( id );
				}

				msg += BioLockJUtil.addTrailingSpaces( "IDs w/ min " + label + ":", pad ) + minSamples + RETURN;
				msg += BioLockJUtil.addTrailingSpaces( "IDs w/ max " + label + ":", pad ) + maxSamples + RETURN;
			}
		}

		return msg;
	}

	/**
	 * Return a num/denom ratio
	 * 
	 * @param num Numerator
	 * @param denom Denominator
	 * @return num/denom Double value
	 */
	public static Double getDoubleRatio( final Double num, final Double denom ) {
		if( num == null || num == 0D ) return 0D;
		if( denom == null || denom == 0D ) return null;
		return num / denom;
	}

	/**
	 * Print the final lines of the summary with overall status, runtime, and the download scp command if applicable.
	 * 
	 * @return Summary info
	 * @throws Exception if any error occurs
	 */
	public static String getFooter() throws Exception {
		final long duration = System.currentTimeMillis() - Constants.APP_START_TIME;
		Log.debug(SummaryUtil.class, "duration : " + duration);
		final StringBuffer sb = new StringBuffer();
		sb.append( EXT_SPACER + RETURN );
		sb.append( getLabel( PIPELINE_STATUS ) + "   " + Pipeline.getStatus() + RETURN );
		sb.append( getLabel( PIPELINE_RUNTIME ) + "  " + getRunTime( duration ) + RETURN );
		sb.append( getLabel( MASTER_CONFIG ) + "     " + MasterConfigUtil.getPath() + RETURN );
		sb.append( getLabel( FINAL_META ) + "    " + ( MetaUtil.exists() ? MetaUtil.getPath(): "N/A" ) + RETURN );
		if( downloadCmd() != null ) sb.append( EXT_SPACER + RETURN + downloadCmd() + RETURN );
		return sb.toString();
	}

	/**
	 * Build a summary of the input files for the given module
	 * 
	 * @param module BioModule to summarize
	 * @return module input summary
	 */
	public static String getInputSummary( final BioModule module ) {
		final StringBuffer sb = new StringBuffer();
		try {
			final int numIn = module.getInputFiles().size();
			if( numIn < 1 ) return null;

			Long inAvg = 0L;
			for( final File f: module.getInputFiles() )
				inAvg += FileUtils.sizeOf( f );

			inAvg = new Double( inAvg / numIn ).longValue();

			sb.append( "# Input files: " + numIn + Constants.RETURN );
			sb.append( "Mean Input File Size: " + FileUtils.byteCountToDisplaySize( inAvg ) + RETURN );
		} catch( final Exception ex ) {
			final String msg =
				"Unable to produce module input summary for: " + module.getClass().getName() + " : " + ex.getMessage();
			sb.append( msg + RETURN );
			Log.warn( SummaryUtil.class, msg );
			ex.printStackTrace();
		}

		return sb.toString();
	}

	/**
	 * Return a num/denom ratio floor value (do not round up)
	 * 
	 * @param num Numerator
	 * @param denom Denominator
	 * @return num/denom Long floor value
	 */
	public static Long getLongRatio( final Double num, final Double denom ) {
		if( num == null || num == 0D ) return 0L;
		if( denom == null || denom == 0D ) return null;
		return new Double( num / denom ).longValue();
	}

	/**
	 * Return the mean value
	 * 
	 * @param vals Collection of values
	 * @param isDouble if the Collection holds Double values
	 * @return mean value
	 */
	@SuppressWarnings("unchecked")
	public static String getMean( final Collection<?> vals, final boolean isDouble ) {
		if( vals.isEmpty() ) return "0.0";
		if( isDouble ) {
			final Collection<Double> doubleVals = (Collection<Double>) vals;
			return Double.valueOf( doubleVals.stream().mapToDouble( i -> i ).sum() / doubleVals.size() ).toString();
		}

		final Collection<Long> longVals = (Collection<Long>) vals;
		return Long
			.valueOf( Double.valueOf( longVals.stream().mapToLong( i -> i ).sum() / longVals.size() ).longValue() )
			.toString();
	}

	/**
	 * Return the median value
	 * 
	 * @param vals Collection of values
	 * @param isDouble if the Collection holds Double values
	 * @return median value
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static String getMedian( final Collection<?> vals, final boolean isDouble ) {
		if( vals.isEmpty() ) return "0.0";
		if( vals.size() == 1 ) return vals.iterator().next().toString();

		List data = null;
		if( isDouble ) data = new ArrayList<>( (Collection<Double>) vals );
		else data = new ArrayList<>( (Collection<Long>) vals );

		Collections.sort( data );

		final int middle = data.size() / 2;
		if( data.size() % 2 == 1 ) return data.get( new Double( data.size() / 2 ).intValue() ).toString();
		else if( isDouble )
			return Double.valueOf( ( (Double) data.get( middle - 1 ) + (Double) data.get( middle ) ) / 2 ).toString();
		else return Long.valueOf( ( (Long) data.get( middle - 1 ) + (Long) data.get( middle ) ) / 2 ).toString();
	}

	/**
	 * Return duration module ran based on modified data of started file, formatted for display (as hours, minutes,
	 * seconds).
	 *
	 * @param module BioModule
	 * @return Formatted module runtime
	 */
	public static String getModuleRunTime( final BioModule module ) {
		final File started =
			new File( module.getModuleDir().getAbsolutePath() + File.separator + Constants.BLJ_STARTED );
		return getRunTime( System.currentTimeMillis() - started.lastModified() );
	}

	/**
	 * Return summary of {@link biolockj.module.BioModule} output directory, with metrics:
	 * <ul>
	 * <li>Number of output files
	 * <li>Mean output file size
	 * <li>Path of new metadata file if any created
	 * </ul>
	 *
	 * @param module BioModule to summarize
	 * @return Summary of module output directory
	 */
	public static String getOutputDirSummary( final BioModule module ) {
		final StringBuffer sb = new StringBuffer();
		try {
			if( module.getOutputDir().listFiles().length == 0 ) return "# Files Output:  0" + RETURN;
			final Collection<File> outFiles =
				FileUtils.listFiles( module.getOutputDir(), HiddenFileFilter.VISIBLE, HiddenFileFilter.VISIBLE );
			int count = outFiles.size();
			BigInteger outAvg = FileUtils.sizeOfAsBigInteger( module.getOutputDir() );
			final File newMeta = module.getMetadata();
			if( newMeta.isFile() && module.getOutputDir().listFiles().length > 1 ) {
				count--;
				outAvg = outAvg.subtract( FileUtils.sizeOfAsBigInteger( newMeta ) );
			}

			if( count == 0 ) outAvg = BigInteger.valueOf( 0 );
			else outAvg = outAvg.divide( BigInteger.valueOf( count ) );

			sb.append( "# Files Output:  " + count + RETURN );
			sb.append( "Mean Output File Size:  " + FileUtils.byteCountToDisplaySize( outAvg ) + RETURN );
			sb.append( newMeta.isFile() ? "New metadata: " + newMeta.getAbsolutePath() + RETURN: "" );

		} catch( final Exception ex ) {
			final String msg =
				"Unable to produce module output summary for: " + module.getClass().getName() + " : " + ex.getMessage();
			sb.append( msg + RETURN );
			Log.error( SummaryUtil.class, msg, ex );
		}

		return sb.toString();
	}

	/**
	 * Summary count label padding as label length + 18
	 * 
	 * @param label Label
	 * @return padding length
	 */
	public static int getPad( final String label ) {
		return label.length() + 15;
	}

	/**
	 * Get runtime message (formatted as hours, minutes, seconds) based on startTime passed.
	 *
	 * @param duration Milliseconds of run time
	 * @return Formatted runtime as XX hours : XX minutes: XX seconds
	 */
	public static String getRunTime( final long duration ) {
		final String format = String.format( "%%0%dd", 2 );
		long elapsedTime = duration / 1000;
		if( elapsedTime < 0 ) elapsedTime = 0;
		final String hours = String.format( format, elapsedTime / 3600 );
		final String minutes = String.format( format, elapsedTime % 3600 / 60 );
		String seconds = String.format( format, elapsedTime % 60 );
		if( hours.equals( "00" ) && minutes.equals( "00" ) && seconds.equals( "00" ) ) seconds = "01";
		return hours + " hours : " + minutes + " minutes : " + seconds + " seconds";
	}

	/**
	 * Return summary of the {@link biolockj.module.ScriptModule} script directory with metrics:
	 * <ul>
	 * <li>Print main script name
	 * <li>Number of worker scripts run
	 * <li>Number of worker scripts successful/failed/incomplete
	 * <li>Average worker script run time
	 * <li>Longest running worker script names/duration
	 * <li>Longest running workers script names/duration
	 * </ul>
	 *
	 * @param module ScriptModule to summarize
	 * @return Summary of module script directory
	 */
	public static String getScriptDirSummary( final ScriptModule module ) {
		final StringBuffer sb = new StringBuffer();
		BufferedReader reader = null;
		try {

			final File mainScript = module.getMainScript();
			if( mainScript == null && !DockerUtil.inAwsEnv() )
				return "Module MAIN script not found in -->" + module.getScriptDir().getAbsolutePath() + RETURN;

			final IOFileFilter ff0 = new WildcardFileFilter( "*" + Constants.SH_EXT );
			final IOFileFilter ffStarted =
				new WildcardFileFilter( "*" + Constants.SH_EXT + "_" + Constants.SCRIPT_STARTED );
			final IOFileFilter ffSuccess =
				new WildcardFileFilter( "*" + Constants.SH_EXT + "_" + Constants.SCRIPT_SUCCESS );
			final IOFileFilter ffFailed =
				new WildcardFileFilter( "*" + Constants.SH_EXT + "_" + Constants.SCRIPT_FAILURES );

			final Collection<File> scripts = FileUtils.listFiles( module.getScriptDir(), ff0, null );
			final Collection<File> scriptsStarted = FileUtils.listFiles( module.getScriptDir(), ffStarted, null );
			final Collection<File> scriptsFailed = FileUtils.listFiles( module.getScriptDir(), ffFailed, null );
			final Collection<File> scriptsSuccess = FileUtils.listFiles( module.getScriptDir(), ffSuccess, null );

			if( mainScript != null ) {
				final File mainSuccess =
					new File( module.getMainScript().getAbsolutePath() + "_" + Constants.SCRIPT_SUCCESS );
				final File mainFail =
					new File( module.getMainScript().getAbsolutePath() + "_" + Constants.SCRIPT_FAILURES );
				final File mainStarted =
					new File( module.getMainScript().getAbsolutePath() + "_" + Constants.SCRIPT_STARTED );

				scripts.remove( module.getMainScript() );
				scriptsFailed.remove( mainFail );
				scriptsSuccess.remove( mainSuccess );
				scriptsStarted.remove( mainStarted );
			}

			final Map<String, Long> longestScripts = new HashMap<>();
			final Map<String, Long> shortestScripts = new HashMap<>();
			long totalRunTime = 0L;
			long maxDuration = 0L;
			final long oneMinute = BioLockJUtil.minutesToMillis( 1 );
			long minDuration = Long.MAX_VALUE;
			final int numCompleted = scriptsSuccess.size() + scriptsFailed.size();
			if( numCompleted < 1 )
				return "No complete scripts found -->" + module.getScriptDir().getAbsolutePath() + RETURN;

			for( final File script: scripts ) {
				final File started = new File( script.getAbsolutePath() + "_" + Constants.SCRIPT_STARTED );
				File finish = new File( script.getAbsolutePath() + "_" + Constants.SCRIPT_SUCCESS );
				if( !finish.isFile() ) finish = new File( script.getAbsolutePath() + "_" + Constants.SCRIPT_FAILURES );
				else {
					final long duration = finish.lastModified() - started.lastModified();
					Log.debug( SummaryUtil.class, script.getName() + " duration: " + duration );
					totalRunTime += duration;
					if( duration > oneMinute && duration > maxDuration ) {
						longestScripts.clear();
						longestScripts.put( script.getName(), duration );
						maxDuration = duration;
					} else if( duration > oneMinute && duration == maxDuration )
						longestScripts.put( script.getName(), duration );

					if( duration > oneMinute && duration < minDuration ) {
						shortestScripts.clear();
						shortestScripts.put( script.getName(), duration );
						minDuration = duration;
					} else if( duration > oneMinute && duration == minDuration )
						shortestScripts.put( script.getName(), duration );
				}
			}

			final Set<String> removeItems = new HashSet<>();
			for( final String name: shortestScripts.keySet() )
				if( longestScripts.keySet().contains( name ) ) removeItems.add( name );

			for( final String name: removeItems ) {
				shortestScripts.remove( name );
				longestScripts.remove( name );
			}

			final int numIncomplete = scriptsStarted.size() - numCompleted;
			if( mainScript != null ) sb.append( "Main Script:  " + mainScript.getAbsolutePath() + RETURN );
			sb.append( "Executed " + scriptsStarted.size() + "/" + scripts.size() + " worker scripts [" );
			sb.append( scriptsSuccess.size() + " successful" );
			sb.append( scriptsFailed.isEmpty() ? "": "; " + scriptsFailed.size() + " failed" );
			sb.append( numIncomplete > 0 ? "; " + numIncomplete + " incomplete": "" );
			sb.append( "]" + RETURN );
			sb.append( "Average worker script runtime: " + getScriptRunTime( totalRunTime / numCompleted ) + RETURN );
			if( !shortestScripts.isEmpty() ) sb.append( "Shortest running scripts [" + getScriptRunTime( minDuration ) +
				"] --> " + shortestScripts.keySet() + RETURN );
			if( !longestScripts.isEmpty() ) sb.append( "Longest running scripts [" + getScriptRunTime( maxDuration ) +
				"] --> " + longestScripts.keySet() + RETURN );

			for( final File failureScript: scriptsFailed ) {
				sb.append( "Script Failed:" + failureScript.getAbsolutePath() + RETURN );
				reader = BioLockJUtil.getFileReader( failureScript );
				for( String line = reader.readLine(); line != null; line = reader.readLine() )
					sb.append( line + RETURN );
			}
		} catch( final Exception ex ) {
			final String msg =
				"Unable to produce module script summary for: " + module.getClass().getName() + " : " + ex.getMessage();
			sb.append( msg + RETURN );
			Log.warn( SummaryUtil.class, msg );
			ex.printStackTrace();
		} finally {
			try {
				if( reader != null ) reader.close();
			} catch( final Exception ex ) {
				Log.error( SummaryUtil.class, "Failed to close file reader", ex );
			}
		}

		return sb.toString();
	}

	/**
	 * Pipeline execution summary.
	 * 
	 * @return Pipeline summary
	 */
	public static String getSummary() {
		final StringBuffer sb = new StringBuffer();
		try {
			final File summary = getSummaryFile();
			if( !summary.isFile() ) sb.append( "NO SUMMARY FOUND" + RETURN );
			else {
				final BufferedReader reader = BioLockJUtil.getFileReader( summary );
				for( String line = reader.readLine(); line != null; line = reader.readLine() )
					sb.append( line + RETURN );
				reader.close();
			}
		} catch( final Exception ex ) {
			final String msg = "Error occurred creating the pipeline summary: " + ex.getMessage();
			sb.append( RETURN + msg );
			Log.error( SummaryUtil.class, msg, ex );
		}
		return sb.toString();
	}

	/**
	 * Getter for the summary file.
	 * 
	 * @return summary file
	 */
	public static File getSummaryFile() {
		return new File( Config.pipelinePath() + File.separator + SUMMARY_FILE );
	}

	/**
	 * Report exception details in the summary
	 *
	 * @param ex Exception thrown causing application runtime failure
	 * @throws Exception if unable to save the updates summary
	 */
	public static void reportFailure( final Exception ex ) throws Exception {
		final StringBuffer sb = new StringBuffer();
		sb.append( getDashes( 60 ) + RETURN + getLabel( "Exception" ) );
		if( ex == null ) sb.append( "Error message not found!" + RETURN );
		else {
			sb.append( ex.getMessage() + RETURN );
			if( ex.getStackTrace() != null & ex.getStackTrace().length > 0 )
				for( final StackTraceElement ste: ex.getStackTrace() )
				sb.append( Constants.TAB_DELIM + ste.toString() + RETURN );
		}
		saveSummary( sb.toString() );
	}

	/**
	 * After each module completes, this method is called to track the execution summary.<br>
	 * If module is null, the pipeline is complete/successful.
	 * 
	 * @param module Completed module
	 * @throws Exception if unable to build the summary
	 */
	public static void reportSuccess( final BioModule module ) throws Exception {
		final File summaryFile = getSummaryFile();
		if (!summaryFile.exists()) saveSummary( getHeading() );
		if( module == null ) {
			saveSummary( getFooter() );
		}else {
			Log.info( SummaryUtil.class,
				"Update BioModule summary [ " + ModuleUtil.displaySignature( module ) + " ] " + summaryFile.getAbsolutePath() );
			
			resetModuleSummary( module );

			final StringBuffer sb = new StringBuffer();
			sb.append( getLabel( MODULE ) + ModuleUtil.displaySignature( module ) + RETURN );
			sb.append( getLabel( MODULE_CLASS ) + module.getClass().getName() + RETURN );
			boolean hasVersion = !module.version().equals( BioModule.NO_VERSION );
			sb.append( getLabel( MODULE_VERSION ) + (hasVersion ? module.version() : "in development") + RETURN );
			sb.append( getLabel( RUN_TIME ) + getModuleRunTime( module ) + RETURN );

			final String summary = module.getSummary();
			
			if( summary != null && !summary.isEmpty() ) {
				sb.append( getDashes( 50 ) + RETURN + summary +
					( summary.endsWith( RETURN ) ? "": RETURN ) );
			}	
			sb.append( EXT_SPACER + RETURN );
			
			saveSummary( sb.toString() );
		}	
	}

	/**
	 * Update the number of attempts in the summary file (called from restart)
	 * 
	 * @throws Exception if unable to update the summary file
	 */
	public static void updateNumAttempts() throws Exception {
		final File summary = getSummaryFile();
		if( summary.exists() ) {
			FileUtils.copyFile( getSummaryFile(), getTempFile() );
			getSummaryFile().delete();
			final BufferedReader reader = BioLockJUtil.getFileReader( getTempFile() );
			final BufferedWriter writer = new BufferedWriter( new FileWriter( getSummaryFile() ) );
			try {
				for( String line = reader.readLine(); line != null; line = reader.readLine() )
					if( line.startsWith( NUM_ATTEMPTS ) ) {
						final String count = line.substring( getLabel( NUM_ATTEMPTS ).length() ).trim();
						final Integer num = Integer.valueOf( count ) + 1;
						writer.write( line.replace( count, num.toString() ) + RETURN );
					} else writer.write( line + RETURN );
				getTempFile().delete();
			} finally {
				if( reader != null ) reader.close();
				writer.close();
			}
		}
	}

	/**
	 * Get the script runtime in hours and minutes
	 * 
	 * @param duration in milliseconds
	 * @return Formatted runtime description
	 */
	protected static String getScriptRunTime( final Long duration ) {
		if( duration == null ) return "N/A";
		final String format = String.format( "%%0%dd", 2 );
		long elapsedTime = duration / 1000;
		if( elapsedTime < 0 ) elapsedTime = 0;
		String hours = String.format( format, elapsedTime / 3600 );
		String minutes = String.format( format, elapsedTime % 3600 / 60 );

		if( hours.equals( "00" ) ) hours = "";
		else if( hours.equals( "01" ) ) hours = "1 hour";
		else hours += " hours";

		if( hours.isEmpty() && minutes.equals( "00" ) ) minutes = "<1 minute";
		else if( minutes.equals( "00" ) ) minutes = "";
		else if( hours.isEmpty() && minutes.equals( "01" ) ) minutes = "1 minute";
		else if( minutes.equals( "01" ) ) minutes = " : 1 minute";
		else if( hours.isEmpty() ) minutes += " minutes";
		else minutes = " : " + minutes + " minutes";
		return hours + minutes;
	}

	/**
	 * Modules can be forced to reset to incomplete status. In this scenario, this method will remove the summary for
	 * completed modules that are rerun.
	 * 
	 * @param module Rerun module
	 * @throws Exception if unable to reset the summary
	 */
	protected static void resetModuleSummary( final BioModule module ) throws Exception {
		FileUtils.copyFile( getSummaryFile(), getTempFile() );
		getSummaryFile().delete();
		final BufferedReader reader = BioLockJUtil.getFileReader( getTempFile() );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( getSummaryFile() ) );
		try {

			boolean foundMod = false;
			for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
				final String label = MODULE + "[";
				if( foundMod ) break;
				else if( line.startsWith( EXCEPTION_LABEL ) ||
					line.startsWith( label ) && line.endsWith( module.getClass().getName() ) ) foundMod = true;
				else writer.write( line + RETURN );
			}
			getTempFile().delete();
		} finally {
			if( reader != null ) reader.close();
			writer.close();
		}
	}

	/**
	 * Save the summary generated to the pipeline root directory.
	 * 
	 * @param summary Pipeline summary
	 * @throws Exception if file processing errors occur
	 */
	protected static void saveSummary( final String summary ) throws Exception {
		final File f = getSummaryFile();
		final FileWriter writer = new FileWriter( f, f.isFile() );
		writer.write( summary );
		writer.close();
		Log.info( SummaryUtil.class, "Summary updated" );
	}

	private static String downloadCmd() throws Exception {
		if( downloadCommand == null ) downloadCommand = DownloadUtil.getDownloadCmd();
		return downloadCommand;
	}

	private static String getDashes( final int len ) {
		return getSpacer( "-", len );
	}

	private static String getHeading() throws DockerVolCreationException {
		final StringBuffer sb = new StringBuffer();
		String meta = Config.getString( null, MetaUtil.META_FILE_PATH );
		if( meta == null ) meta = "N/A";
		sb.append( getLabel( PIPELINE_NAME ) + "  " + Config.pipelineName() + RETURN );
		sb.append( getLabel( PIPELINE_CONFIG ) + Config.getConfigFilePath() + RETURN );
		sb.append( getLabel( PIPELINE_META ) + meta + RETURN );
		sb.append( getLabel( BIOLOCKJ_VERSION_HEADER ) + BioLockJUtil.getVersion() + RETURN );
		sb.append( getLabel( RUNTIME_ENV ) + "       " + getRuntimeEnv() + RETURN );
		sb.append( getLabel( PIPELINE_INPUT ) + "    " + Config.getString( null, Constants.INPUT_DIRS ) + RETURN );
		sb.append( getLabel( NUM_MODULES ) + "      " + Pipeline.getModules().size() + RETURN );
		sb.append( getLabel( NUM_ATTEMPTS ) + "     1" + RETURN );
		sb.append( EXT_SPACER + RETURN );
		return sb.toString();
	}

	/**
	 * Simple method to format labels in email body. Adds a colon and 2 spaces to label.
	 *
	 * @param label String
	 * @return Formatted label
	 */
	private static String getLabel( final String label ) {
		return label + ":  ";
	}

	private static String getRuntimeEnv() throws DockerVolCreationException {
		if( runtimeEnv != null ) return runtimeEnv;
		String parentHost = null;
		String host = null;
		try {
			host = Processor.submitQuery( "hostname", "Query Host" );
			if (Config.isOnCluster() ) parentHost = Config.getString( null, Constants.CLUSTER_HOST );
		} catch( final Exception ex ) {
			Log.error( SummaryUtil.class, "Failed to determine runtime environment host", ex );
		}
		if( parentHost == null ) parentHost = "Unknown-Host";
		if( host == null ) host = "localhost";

		if( DockerUtil.inAwsEnv() || DockerUtil.inDockerEnv() )
			runtimeEnv = ( DockerUtil.inAwsEnv() ? "AWS ": "" ) 
			+ "--> Docker Container [ " + DockerUtil.ROOT_HOME.substring( 1 ) + "@" + host + " ]";
		else if( Config.isOnCluster() ) runtimeEnv = "Cluster";
		else runtimeEnv = "Localhost";
		return runtimeEnv;
	}

	private static String getSpacer( final String val, final int len ) {
		String spacer = "";
		for( int i = 0; i < len; i++ )
			spacer += val;
		return spacer;
	}

	private static String getSpaces( final int len ) {
		return getSpacer( " ", len );
	}

	private static File getTempFile() {
		return new File( Config.pipelinePath() + File.separator + TEMP_SUMMARY_FILE );
	}

	private static String spacedWord( final String word, final int gap ) {
		final StringBuffer sb = new StringBuffer();
		for( final char i: word.toCharArray() )
			sb.append( i + getSpaces( gap ) );
		return sb.toString();
	}

	private static String downloadCommand = null;
	private static final String BIOLOCKJ_VERSION_HEADER = "BioLockJ version";
	private static final String EXCEPTION_LABEL = "Exception:";
	private static final String EXT_SPACER = getDashes( 154 );
	private static final String FINAL_META = "Final Metadata";
	private static final String MASTER_CONFIG = "Master Config";
	private static final String MODULE = "Module";
	private static final String MODULE_CLASS = "Module class";
	private static final String MODULE_VERSION = "Module version";
	private static final String NUM_ATTEMPTS = "# Attempts";
	private static final String NUM_MODULES = "# Modules";
	private static final String PIPELINE_CONFIG = "Pipeline Config";
	private static final String PIPELINE_INPUT = "Pipeline Input";
	private static final String PIPELINE_META = "Pipeline Metadata";
	private static final String PIPELINE_NAME = "Pipeline Name";
	private static final String PIPELINE_OUTPUT = "Pipeline Output";
	private static final String PIPELINE_RUNTIME = "Pipeline Runtime";
	public static final String PIPELINE_STATUS = "Pipeline Status";
	private static final String RETURN = Constants.RETURN;
	private static final String RUN_TIME = "Runtime";
	private static final String RUNTIME_ENV = "Runtime Env";
	private static String runtimeEnv = null;
	private static final String SUMMARY_FILE = "summary" + Constants.TXT_EXT;
	private static final String TEMP_SUMMARY_FILE = ".tempSummary" + Constants.TXT_EXT;
}
