/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Mar 18, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.exception;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import biolockj.*;
import biolockj.util.*;

/**
 * FatalExceptionHandler saves logs somewhere the user can access if failures occur before the log file is generated.
 */
public class FatalExceptionHandler {

	/**
	 * Error log file getter
	 * 
	 * @return Error log
	 */
	public static File getErrorLog() {
		return errorLog;
	}

	/**
	 * Print the {@link biolockj.Log} messages and the exception stack trace info to the $USER $HOME directory.
	 * 
	 * @param args Java runtime args
	 * @param ex Fatal application exception
	 */
	public static void logFatalError( final String[] args, final Exception ex ) {
		System.out.println( "System encountered a FATAL ERROR" );
		System.out.println( ERROR_TYPE + ex.getClass().getSimpleName() );
		System.out.println( ERROR_MSG + ex.getMessage() );
		if( Log.getFile() != null && Log.getFile().isFile() ) {
			setErrorLog( Log.getFile() );
			setFailedStatus(ex);
			SummaryUtil.addSummaryFooterForFailedPipeline();
		} else setErrorLog( createErrorLog() );

		logFatalException( args, ex );

		if( getErrorLog() != null ) {
			Log.info( FatalExceptionHandler.class,
				"Local file-system error log path: " + getErrorLog().getAbsolutePath() );
			if( DockerUtil.inDockerEnv() ) Log.info( FatalExceptionHandler.class, "Host file-system error log path: " +
				RuntimeParamUtil.getDockerHostHomeDir() + File.separator + getErrorLog().getName() );
			if( !getErrorLog().isFile() ) dumpLogs( getLogs() );
		} else {
			Log.warn( FatalExceptionHandler.class, "Unable to save logs to file-system: " );
			printLogsOnScreen( getLogs() );
		}
	}

	private static File createErrorLog() {
		Log.warn( FatalExceptionHandler.class, "Pipeline failed before pipeline directory or log were created!" );
		final File dir = getErrorLogDir();
		if( dir == null ) return null;
		final String suffix = getErrorLogSuffix();
		int i = 0;
		File file =
			new File( dir.getAbsolutePath() + File.separator + FATAL_ERROR_FILE_PREFIX + suffix + Constants.LOG_EXT );
		while( file.exists() )
			file = new File( dir.getAbsolutePath() + File.separator + FATAL_ERROR_FILE_PREFIX + suffix + "_" +
				new Integer( ++i ).toString() + Constants.LOG_EXT );
		return file;
	}

	private static void dumpLogs( final List<String> lines ) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter( new FileWriter( getErrorLog() ) );
			for( final String line: lines )
				writer.write( line );
		} catch( final Exception ex ) {
			System.out.println( "Failed to write to: " + getErrorLog().getAbsolutePath() + " : " + ex.getMessage() );
			ex.printStackTrace();
		} finally {
			try {
				if( writer != null ) writer.close();
			} catch( final IOException ex ) {
				System.out.println(
					"Failed to close writer for: " + getErrorLog().getAbsolutePath() + " : " + ex.getMessage() );
			}
		}
	}

	private static File getErrorLogDir() {
		File dir = RuntimeParamUtil.get_BLJ_PROJ();
		if( dir == null || !dir.isDirectory() )
			if( DockerUtil.inDockerEnv() ) dir = new File( DockerUtil.AWS_EC2_HOME );
			else dir = RuntimeParamUtil.getHomeDir();

		if( dir == null || !dir.isDirectory() ) {
			final String path = Config.replaceEnvVar( "${HOME}" );
			if( path != null && !path.isEmpty() ) dir = new File( path );
		}

		if( dir == null || !dir.isDirectory() ) {
			Log.warn( FatalExceptionHandler.class, "Unable to find $BLJ_PROJ or $HOME dirs" );
			return null;
		}
		Log.warn( FatalExceptionHandler.class, "Save Error File to: " + dir.getAbsolutePath() );
		return dir;
	}

	private static String getErrorLogSuffix() {
		if( BioLockJUtil.isDirectMode() ) return RuntimeParamUtil.getDirectModuleDir();
		else if( Config.pipelineName() != null ) return Config.pipelineName();
		else if( RuntimeParamUtil.getConfigFile() != null ) return RuntimeParamUtil.getConfigFile().getName();
		return "Unknown_Config";
	}

	private static List<String> getLogs() {
		final List<String> lines = new ArrayList<>();
		for( final String[] m: Log.getMsgs() ) {
			if( m[ 0 ].equals( Log.DEBUG ) ) lines.add( "[ " + Log.DEBUG + " ] " + m[ 1 ] + Constants.RETURN );
			if( m[ 0 ].equals( Log.INFO ) ) lines.add( "[ " + Log.INFO + " ] " + m[ 1 ] + Constants.RETURN );
			if( m[ 0 ].equals( Log.WARN ) ) lines.add( "[ " + Log.WARN + " ] " + m[ 1 ] + Constants.RETURN );
			if( m[ 0 ].equals( Log.ERROR ) ) lines.add( "[ " + Log.ERROR + " ] " + m[ 1 ] + Constants.RETURN );
		}
		return lines;
	}

	private static void logFatalException( final String[] args, final Exception ex ) {
		Log.error( FatalExceptionHandler.class, Constants.LOG_SPACER );
		Log.error( FatalExceptionHandler.class, Constants.RETURN + "FATAL APPLICATION ERROR " +
			( args == null ? "": " -->" + Constants.RETURN + " Program args: " + RuntimeParamUtil.getRuntimeArgs() ),
			ex );
		Log.error( FatalExceptionHandler.class, Constants.LOG_SPACER );
		ex.printStackTrace();
		Log.error( FatalExceptionHandler.class, Constants.LOG_SPACER );
		Log.error( FatalExceptionHandler.class, BioLockJ.getHelpInfo() );
		Log.error( FatalExceptionHandler.class, Constants.LOG_SPACER );
	}

	private static void printLogsOnScreen( final List<String> lines ) {
		for( final String line: lines )
			System.out.println( line );
	}

	private static void setErrorLog( final File file ) {
		errorLog = file;
	}

	private static void setFailedStatus(Exception fetalEx) {
		try {
			if( Config.getPipelineDir() != null ) {
				String failFlagPath = Config.pipelinePath() + File.separator + Constants.BLJ_FAILED;
				BioLockJUtil.createFile( failFlagPath );
				if( fetalEx != null ) {
					final FileWriter writer = new FileWriter( new File( failFlagPath ) );
					writer.write( ERROR_TYPE + fetalEx.getClass().getSimpleName() + System.lineSeparator() );
					writer.write( ERROR_MSG + fetalEx.getMessage() );
					writer.close();
				}
			}
		} catch( final Exception ex ) {
			Log.error( FatalExceptionHandler.class,
				"Pipeline root directory not found - unable save Pipeline Status File: " + Constants.BLJ_FAILED +
					" : " + ex.getMessage() );
		}
	}

	private static File errorLog = null;
	private static final String FATAL_ERROR_FILE_PREFIX = "BioLockJ_FATAL_ERROR_";
	public static final String ERROR_TYPE = "ERROR TYPE:    ";
	public static final String ERROR_MSG =  "ERROR MESSAGE: ";
}
