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
package biolockj;

import java.io.*;
import java.util.*;
import biolockj.exception.ConfigPathException;
import biolockj.module.ScriptModule;
import biolockj.util.BioLockJUtil;
import biolockj.util.NextflowUtil;

/**
 * {@link biolockj.module.ScriptModule}s that generate scripts will submit a main script to the OS for execution as a
 * {@link biolockj.Processor}.
 */
public class Processor {

	/**
	 * Class used to submit processes on their own Thread.
	 */
	public class Subprocess implements Runnable {

		/**
		 * Execute the command args in a separate thread and log output with label.
		 * 
		 * @param args Command args
		 * @param label Log label
		 */
		public Subprocess( final String[] args, final String label ) {
			this.args = args;
			this.label = label;
		}

		@Override
		public void run() {
			try {
				new Processor().runJob( this.args, this.label );
			} catch( final Exception ex ) {
				Log.error( getClass(),
					"Problem occurring within Subprocess-" + this.label + " --> " + ex.getMessage() );
				ex.printStackTrace();
			}
		}

		private String[] args = null;
		private String label = null;
	}

	/**
	 * Empty constructor to facilitate subprocess creation
	 */
	Processor() {}

	/**
	 * Execute the command args and log output with label.
	 * 
	 * @param args Command args
	 * @param label Log label
	 * @return last line of process output
	 * @throws IOException if errors occur reading the InputStream
	 * @throws InterruptedException if the thread process is interrupted
	 */
	protected String runJob( final String[] args, final String label, File workDir ) throws IOException, InterruptedException {
		Log.info( getClass(), "[ " + label + " ]: STARTING CMD --> " + getArgsAsString( args ) );
		final Process p = Runtime.getRuntime().exec( args, null, workDir );
		final BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
		String returnVal = null;
		String s = null;
		while( ( s = br.readLine() ) != null )
			if( !s.trim().isEmpty() ) {
				Log.info( getClass(), "[ " + label + " ]: " + s );
				if( returnVal == null ) returnVal = s;
			}
		p.waitFor();
		p.destroy();
		Log.info( getClass(), "[ " + label + " ]: COMPLETE" );
		return returnVal;
	}
	protected String runJob( final String[] args, final String label ) throws IOException, InterruptedException {
		return runJob( args, label, null );
	}

	/**
	 * De-register a thread, so it is not considered when shutting down the application.
	 * 
	 * @param thread Subprocess thread
	 */
	public static void deregisterThread( final Thread thread ) {
		threadRegister.remove( thread );
	}

	/**
	 * Return the value of the bash variable from the runtime shell.
	 * 
	 * @param bashVar Bash variable name
	 * @return Bash env variable value or null if not found (or undefined)
	 */
	public static String getBashVar( final String bashVar ) {
		if( bashVar == null ) return null;
		String bashVarValue = null;
		Log.info( Processor.class, "[ Get Bash Var (" + bashVar + ") ]: STARTING" );
		try {
			final String var = bashVar.startsWith( "$" ) || bashVar.equals( "~" ) ? bashVar: "$" + bashVar;
			Log.info( Processor.class,
				"[ Get Bash Var (" + bashVar + ") ]: CMD --> " + getArgsAsString( bashVarArgs( var ) ) );
			final Process p = Runtime.getRuntime().exec( bashVarArgs( var ) );
			final BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
			String s = null;
			while( ( s = br.readLine() ) != null )
				if( s.startsWith( BLJ_GET_ENV_VAR_KEY ) ) {
					bashVarValue = s.replace( BLJ_GET_ENV_VAR_KEY, "" ).trim();
					break;
				}
			p.waitFor();
			p.destroy();
		} catch( final Exception ex ) {
			Log.error( Processor.class, "Problem occurred looking up bash env. variable: " + bashVar, ex );
		}
		if( bashVarValue == null ) Log.warn( Processor.class, "[ Get Bash Var (" + bashVar + ") ]: FAILED" );
		if( bashVarValue != null && bashVarValue.trim().isEmpty() ) bashVarValue = null;
		Log.info( Processor.class, "[ Get Bash Var (" + bashVar + ") ]: COMPLETE" );
		return bashVarValue;
	}

	/**
	 * Instantiates a new {@link biolockj.Processor}.<br>
	 * String[] array used to control spacing between command/params.<br>
	 * As if executing on terminal args[0] args[1]... args[n-1] as one command.
	 *
	 * @param args Terminal command created from args (adds 1 space between each array element)
	 * @param label to associate with the process
	 * @return Thread ID
	 */
	public static Thread runSubprocess( final String[] args, final String label ) {
		final Thread t = new Thread( new Processor().new Subprocess( args, label ) );
		threadRegister.put( t, System.currentTimeMillis() );
		Log.warn( Processor.class,
			"Register Thread: " + t.getId() + " - " + t.getName() + " @" + threadRegister.get( t ) );
		t.start();
		return t;
	}

	/**
	 * Set file permissions by executing chmod {@value biolockj.Constants#SCRIPT_PERMISSIONS} on generated bash scripts.
	 *
	 * @param path Target directory path
	 * @param permissions Set the chmod security bits (ex 764)
	 * @throws Exception if chmod command command fails
	 */
	public static void setFilePermissions( final String path, final String permissions ) throws Exception {
		if( BioLockJUtil.hasNullOrEmptyVal( Arrays.asList( path, permissions ) ) ) return;
		final StringTokenizer st = new StringTokenizer( "chmod -R " + permissions + " " + path );
		final String[] args = new String[ st.countTokens() ];
		for( int i = 0; i < args.length; i++ )
			args[ i ] = st.nextToken();
		submitJob( args, "Set File Privs" );
	}

	/**
	 * This method is called by script generating {@link biolockj.module.ScriptModule}s to update the script
	 * file-permissions to ensure they are executable by the program. Once file permissions are set, the main script
	 * (passed in the args param) is executed. Calls {@link #setFilePermissions(String, String)} and
	 * {@link #submit(ScriptModule)}
	 *
	 * @param module ScriptModule that is submitting its main script as a Processor
	 * 
	 * @throws IOException if errors occur reading the InputStream
	 * @throws InterruptedException if the thread process is interrupted
	 */
	public static void submit( final ScriptModule module ) throws IOException, InterruptedException {
		new Processor().runJob( module.getJobParams(), module.getClass().getSimpleName(), module.getScriptDir() );
	}

	/**
	 * Instantiates a new {@link biolockj.Processor}.<br>
	 * String[] array used to control spacing between command/params.<br>
	 * As if executing on terminal args[0] args[1]... args[n-1] as one command.
	 *
	 * @param args Terminal command created from args (adds 1 space between each array element)
	 * @param label - Process label
	 * @throws IOException if errors occur reading the InputStream
	 * @throws InterruptedException if the thread process is interrupted
	 */
	public static void submitJob( final String[] args, final String label ) throws IOException, InterruptedException {
		new Processor().runJob( args, label );
	}

	/**
	 * Run script that expects a single result
	 * 
	 * @param cmd Command
	 * @param label Process Label
	 * @return script output
	 * @throws IOException if errors occur reading the InputStream
	 * @throws InterruptedException if the thread process is interrupted
	 */
	public static String submitQuery( final String cmd, final String label ) throws IOException, InterruptedException {
		return new Processor().runJob( new String[] { cmd }, label );
	}

	/**
	 * Check if a specific process is alive
	 * 
	 * @param id - Registered thread ID
	 * @return Boolean TRUE only if the ID is alive
	 */
	public static boolean subProcAlive( final Long id ) {
		if( threadRegister.isEmpty() ) return false;
		for( final Thread t: threadRegister.keySet() )
			if( t.isAlive() && t.getId() == id ) return true;
		return false;
	}

	/**
	 * Check if any Subprocess threads are still running.
	 * 
	 * @return boolean TRUE if all complete
	 */
	public static boolean subProcsAlive() {
		if( threadRegister.isEmpty() ) return false;
		final long max = BioLockJUtil.minutesToMillis( NextflowUtil.getS3_TransferTimeout() );
		Log.info( Processor.class, "Running Subprocess Threads will be terminated if incomplete after [ " +
			NextflowUtil.getS3_TransferTimeout() + " ] minutes." );
		for( final Thread t: threadRegister.keySet() )
			if( t.isAlive() ) {
				final String id = t.getId() + " - " + t.getName();
				final long runTime = System.currentTimeMillis() - threadRegister.get( t );
				final int mins = BioLockJUtil.millisToMinutes( runTime );
				Log.warn( Processor.class,
					"Subprocess Thread [ " + id + " ] is ALIVE - runtime = " + mins + " minutes" );
				if( runTime > max ) {
					t.interrupt();
					threadRegister.remove( t );
				} else {
					Log.warn( Processor.class,
						"Subprocess Thread [ " + id + " ] is ALIVE - runtime = " + mins + " minutes" );
					return true;
				}
			}
		return false;
	}

	private static String[] bashVarArgs( final String bashVar ) throws ConfigPathException {
		final File profile = BioLockJUtil.getUserProfile();
		if( profile != null )
			return new String[] { bashVarScript().getAbsolutePath(), bashVar, profile.getAbsolutePath() };
		return new String[] { bashVarScript().getAbsolutePath(), bashVar };
	}

	private static File bashVarScript() throws ConfigPathException {
		final File script = new File( BioLockJUtil.getBljDir().getAbsolutePath() + File.separator +
			Constants.SCRIPT_DIR + File.separator + BLJ_GET_ENV_VAR_SCRIPT );
		if( script.isFile() ) return script;
		throw new ConfigPathException( script );
	}

	private static String getArgsAsString( final String[] args ) {
		final StringBuffer sb = new StringBuffer();
		for( final String arg: args )
			sb.append( arg + " " );
		return sb.toString();
	}

	private static final String BLJ_GET_ENV_VAR_KEY = "BLJ_GET_ENV_VAR";
	private static final String BLJ_GET_ENV_VAR_SCRIPT = "get_env_var";
	private static final Map<Thread, Long> threadRegister = new HashMap<>();
}
