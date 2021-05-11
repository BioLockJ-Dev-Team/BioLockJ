package biolockj.module.diy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import biolockj.Config;
import biolockj.Log;
import biolockj.Properties;
import biolockj.api.API_Exception;
import biolockj.exception.ConfigNotFoundException;
import biolockj.util.BioLockJUtil;

public class ForEachLoop extends GenMod {
	
	public ForEachLoop() {
		super();
		addNewProperty( LOOPBY, Properties.LIST_TYPE, "List used as the looping mechanism for this module.");
	}
	
	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		Config.requireList( this, LOOPBY );
	}
	
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception {
		final List<List<String>> data = new ArrayList<>();
		transferResources();
		String localScript = transferScript();
		
		for (String element : getLoopElements() ) {
			final ArrayList<String> lines = new ArrayList<>();
			lines.add( getLauncher() + localScript + " " + element + " " + getScriptParams() );
			Log.info( GenMod.class, "Core command: " + data );
			data.add( lines );
		}
		
		return data;
	}
	
	protected List<String> getLoopElements() throws ConfigNotFoundException{
		return Config.requireList( this, LOOPBY );
	}
	
	@Override
	public String getDescription() {
		return "Like GenMod, but done for each string in a comma-separated list.";
	}
	
	@Override
	public String getDetails() throws API_Exception {
		return "This is an extention of the [GenMod](../GenMod) module.<br>  " +
			"The given script is run for each element given in the comma-separated list _" + LOOPBY + "_." +
			"The user script is run using a command:<br> `[launcher] <script> <loop-element> [param]`";
	}
	
	@Override
	public String getCitationString() {
		return "Module developed by Ivory Blakley";
	}
	
	@Override
	public String version() {
		return "1.0.0";
	}
	
	/**
	 * Supply a list with the correct number of files to match the number of inputs.
	 * These are not files that actually exist; but this list allows for correct worker assignment.
	 * TODO: revisit this when worker assignment is a little smarter.
	 */
	@Override
	public List<File> getInputFiles() {
		List<File> list = new ArrayList<>();
		try {
			for (String element : getLoopElements() ) {
				list.add( new File(element) );
			}
		} catch( ConfigNotFoundException e ) {
			// this exception is handled in checkDependecies and/or in buildScript
			e.printStackTrace();
		}
		return list;
	}
	
	private static String LOOPBY = "genMod.loopBy";

}
