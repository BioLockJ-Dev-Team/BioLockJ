package biolockj.launch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import biolockj.Constants;

/**
 * This utility controls the information printed to the screen during launch.
 * The launch package controls what the messages are.
 * During the pipeline formation (initialization) and check dependencies phases,
 * the LaunchProcess passes the std out/error lines to {@link #showUserUpdates(String)} 
 * which watches for stings that include KEY strings.
 * 
 * @author Ivory Blakley
 *
 */
public class ProgressUtil {

	/**
	 * Utility class, no instantiation.
	 */
	public ProgressUtil() {}
	
	static Timer t = null;
	static LabeledSpinner activeIndicator = null;
	
	/**
	 * Pass BioLockJ out/err stream lines. This will print only messages that start with one of the status keys: 
	 * {@link biolockj.Constants#STATUS_MARK_KEY} or {@link biolockj.Constants#STATUS_START_KEY} 
	 * @param s a string, expected to be a line of standard out/err from the main instance of BioLockJ.
	 */
	public static void showUserUpdates(String s) {
		if( s.startsWith( Constants.STATUS_START_KEY ) ){
			String status = s.substring( Constants.STATUS_START_KEY.length() );
			startSpinner( status );
		}else if( s.startsWith( Constants.STATUS_MARK_KEY ) ){
			String msg = s.substring( Constants.STATUS_MARK_KEY.length() );
			printStatus( msg, true );
		}
	}
	
	public static void startSpinner(String label){
		clear();
		activeIndicator = new LabeledSpinner(label);
		t = new Timer(); //if you never start a spinner, you never need a timer.
		t.schedule( activeIndicator, 0, 100 );
	}
	
	/**
	 * Print a message to the screen.
	 * 
	 * @param s
	 * @param endExistingStatus if there is an active spinner, should it be terminated. If false, the active spinner
	 * will be resumed (if one existed) after printing this message.
	 */
	public static void printStatus(String s, boolean endExistingStatus) {
		pause();
		System.err.println(s);
		if (endExistingStatus) clear();
		else {
			if ( activeIndicator != null) startSpinner(activeIndicator.getLabel());
		}
	}
	
	/**
	 * Use with caution. This should only be used to clear the current progress animation when something has gone wrong.
	 * Otherwise, let the ProgressUtil clear its own stuff.
	 */
	public static void clear() {
		pause();
		activeIndicator = null;
		if (t != null) t.cancel();
	}
	
	private static void pause() {
		if (activeIndicator != null) {
			activeIndicator.cancel();
			t.purge();
		}
	}
	
	static void showFileContents( File file ) throws IOException {
		BufferedReader reader = new BufferedReader( new FileReader( file ) );
		while( reader.ready() )
			System.err.println( reader.readLine() );
		reader.close();
	}
	
	static void showFileContents( File file, int leading, int trailing ) throws IOException {
		for (int i=0; i < leading; i++) System.err.println();
		showFileContents( file );
		for (int i=0; i < trailing; i++) System.err.println();
	}

}

class LabeledSpinner extends TimerTask  {

	private final String label;
	private final String wipeout;
	static final String anim = "|/-\\";
	int x = 0;
	
	public String getLabel() {
		return label;
	}
	public LabeledSpinner( String label ) {
		this.label = label;
		StringBuffer sb = new StringBuffer();
		for (int i=0; i < label.length(); i++) {
			sb.append( " " );
		}
		wipeout = "\r" + sb.toString() + "  \r";
	}
	
	public void run() {
		System.err.print( "\r" + label  + " " + anim.charAt( x++ % anim.length() ) + "\r");
	}
	
	@Override
	public boolean cancel() {
		boolean b = super.cancel();
		System.err.print( wipeout );
		return b;
	}
	
}
