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

		if( Log.getFile() != null && Log.getFile().isFile() ) {
			setErrorLog( Log.getFile() );
		} else {
			setErrorLog( createErrorLog() );
		}
		
		if( ex instanceof DirectModuleException && getExistingFailFlag() != null ) {
			File failFlag = getExistingFailFlag();
				try {
					BufferedReader reader = BioLockJUtil.getFileReader( failFlag );
					for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
						System.out.println(line); // for test suite
					}
					reader.close();
				} catch( Exception e ) {} // well then don't do that.
		} else {
			System.out.println( ERROR_TYPE + ex.getClass().getSimpleName() );
			System.out.println( ERROR_MSG + ex.getMessage() );
			setFailedStatus( ex );
		}

		if( !BioLockJUtil.isDirectMode() && !RuntimeParamUtil.isPrecheckMode()) SummaryUtil.addSummaryFooterForFailedPipeline();
		if( !BioLockJUtil.isDirectMode() ) MasterConfigUtil.saveMasterConfig();
		logFatalException( args, ex );

		if( getErrorLog() != null ) {
			Log.info( FatalExceptionHandler.class,
				"Local file-system error log path: " + getErrorLog().getAbsolutePath() );
			if( DockerUtil.inDockerEnv() ) try {
				Log.info( FatalExceptionHandler.class, "Host file-system error log path: " +
					DockerUtil.deContainerizePath( getErrorLog().getAbsolutePath() ) );
			} catch( DockerVolCreationException docEx ) {
				// well, then, don't do that.
			}
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
		File dir = null;
		try {
			dir = RuntimeParamUtil.get_BLJ_PROJ();
			if( dir == null || !dir.isDirectory() ) {
				if( DockerUtil.inDockerEnv() ) dir = new File( DockerUtil.AWS_EC2_HOME );
			}
		} catch( DockerVolCreationException docEx ) {
			// well then don't do that.
		}

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
		File config = null;
		try {
			config = RuntimeParamUtil.getConfigFile();
		}catch(DockerVolCreationException docEx) {
			// well then don't do that.
		}
		if( BioLockJUtil.isDirectMode() ) return RuntimeParamUtil.getDirectModuleDir();
		else if( Config.pipelineName() != null ) return Config.pipelineName();
		else if( config != null ) return config.getName();
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
		if ( ex.getCause() instanceof BioLockJException ) {
			Log.error( FatalExceptionHandler.class, "Caused by ... " + ex.getClass().getSimpleName());
			ex.getCause().printStackTrace();
		}
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
	
	private static File getExistingFailFlag() {
		boolean fileExists = false;
		File failFlag = null;
		if( BioLockJ.getPipelineDir() != null ) {
			failFlag = new File( BioLockJ.getPipelineDir() + File.separator + Constants.BLJ_FAILED );
			fileExists = failFlag.exists();
		}
		if (fileExists) return failFlag;
		return null;
	}

	private static void setFailedStatus(Exception fetalEx) {
		if (fetalEx instanceof StopAfterPrecheck) {
			try {
				PipelineUtil.markStatus( Constants.PRECHECK_COMPLETE );
			} catch( BioLockJStatusException | IOException e ) {
				Log.error( FatalExceptionHandler.class,
					"Pipeline root directory not found - unable to save Pipeline Status File: " + Constants.PRECHECK_COMPLETE +
					" : " + e.getMessage() );
			}
		}else {
			try {
				File failFlagPath;
				if (RuntimeParamUtil.isPrecheckMode()) {
					failFlagPath = PipelineUtil.markStatus( Constants.PRECHECK_FAILED );
				}else {
					failFlagPath = PipelineUtil.markStatus( Constants.BLJ_FAILED );
				}
				if( BioLockJ.getPipelineDir() != null ) {
					if( fetalEx != null ) {
						final FileWriter writer = new FileWriter( failFlagPath );
						writer.write( ERROR_TYPE + fetalEx.getClass().getSimpleName() + System.lineSeparator() );
						writer.write( ERROR_MSG + fetalEx.getMessage() );
						if ( ! fetalEx.getMessage().endsWith( System.lineSeparator() )) writer.write(System.lineSeparator());
						writer.close();
					}
				}
			} catch( final Exception ex ) {
				Log.error( FatalExceptionHandler.class,
					"Pipeline root directory not found - unable to save Pipeline Status File: " + Constants.BLJ_FAILED +
					" : " + ex.getMessage() );
			}
		}
	}

	private static File errorLog = null;
	private static final String FATAL_ERROR_FILE_PREFIX = "BioLockJ_FATAL_ERROR_";
	public static final String ERROR_TYPE = "ERROR TYPE:    ";
	public static final String ERROR_MSG =  "ERROR MESSAGE: ";
}
