package biolockj.module.diy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import biolockj.Log;
import biolockj.api.API_Exception;
import biolockj.util.BioLockJUtil;

public class ForEachFile extends GenMod {
	
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception {
		final List<List<String>> data = new ArrayList<>();
		transferResources();
		String localScript = transferScript();
		
		for (File file : getInputFiles() ) {
			final ArrayList<String> lines = new ArrayList<>();
			lines.add( getLauncher() + localScript + " " + file.getAbsolutePath() + " " + getScriptParams() );
			Log.info( GenMod.class, "Core command: " + data );
			data.add( lines );
		}
		
		return data;
	}
	
	@Override
	public String getDescription() {
		return "Like GenMod, but done for each file in a previous module's output dir.";
	}
	
	@Override
	public String getDetails() throws API_Exception {
		return "This is an extention of the [GenMod](../GenMod) module.<br>  " +
			"The given script is run for each file in the previous modules output dir.  If there is no previous module, then the input files are used." +
			"The user script is run using a command:<br> `[launcher] <script> <file path> [params]`";
	}
	
	@Override
	public String getCitationString() {
		return "BioLockJ " + BioLockJUtil.getVersion( ) + System.lineSeparator() + "Module developed by Ivory Blakley";
	}
	
}
