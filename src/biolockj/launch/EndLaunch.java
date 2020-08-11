package biolockj.launch;

import java.util.Arrays;
import java.util.List;

/*
 * All exits from the launch process pass through here.
 */
public class EndLaunch extends Exception {

	int exitCode = 0;
	
	public EndLaunch(int code) {
		exitCode=code;
	}
	
	public EndLaunch(int code, String msg) {
		String[] s = {msg};
		List<String> msgs = Arrays.asList( s );
		new EndLaunch( code, msgs );
	}
	public EndLaunch(String msg) {
		new EndLaunch(1,  msg);
	}
	
	public EndLaunch(int code, List<String> msgs) {
		for (String msg : msgs ) {
			System.err.println(msg);
		}
		if (code != 0) {
			LaunchProcess.printHelp(System.err);
			//System.err.println("See the help menu:  biolockj --help");
		}
		exitCode=code;
	}
	public EndLaunch(List<String> msgs) {
		new EndLaunch(1,  msgs);
	}
	
	public int getExitCode() {
		return exitCode;
	}

	private static final long serialVersionUID = -4642395134353303882L;
	
}
