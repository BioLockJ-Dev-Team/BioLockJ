package biolockj.launch;

import biolockj.Constants;

public class ProgressMsgDemo {

	public ProgressMsgDemo() {}

	public static void main( String[] args ) throws InterruptedException {
		
		long time;
		if (args.length > 0) time = Long.valueOf( args[0] );
		else time = 1000;
		
		ProgressUtil.printStatus( "This is a stand-alone message, unrealted to long processes.", false);
		
		ProgressUtil.startSpinner( "The long process is running " );
		Thread.sleep( time );
		ProgressUtil.printStatus( "The long process is DONE!", true);
		
		ProgressUtil.startSpinner( "Running multi-task process..." );
		Thread.sleep( time );
		ProgressUtil.printStatus( "Interupting message: part 1 of 3 is done.", false);
		Thread.sleep( time );
		ProgressUtil.printStatus( "Interupting message: part 2 of 3 is done.", false);
		ProgressUtil.printStatus( "This is a stand-alone message, unrealted to long processes.", false);
		Thread.sleep( time );
		ProgressUtil.printStatus( "Interupting message: part 3 of 3 is done.", false);
		ProgressUtil.printStatus( "Multi-task process...DONE!", true);
		
		//An example line from the log,
		//Could come from a code line: Log.info(Constants.STATUS_START_KEY + "Doing a thing in check dependencies...");
		String log1 = Constants.STATUS_START_KEY + "Doing a thing in check dependencies...";
		//And a following line,
		//Could come from a code line: Log.info(Constants.STATUS_MARK_KEY + "Done doing that thing.");
		String log2 = Constants.STATUS_MARK_KEY + "Done doing that thing.";
		ProgressUtil.showUserUpdates( log1 );
		Thread.sleep( time );
		ProgressUtil.printStatus( "Unrelated message.", false);
		Thread.sleep( time );
		ProgressUtil.showUserUpdates( log2 );
		
	}

}
