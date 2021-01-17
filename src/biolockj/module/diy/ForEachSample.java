package biolockj.module.diy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import biolockj.Log;
import biolockj.api.API_Exception;
import biolockj.exception.ConfigNotFoundException;
import biolockj.util.BioLockJUtil;
import biolockj.util.MetaUtil;

public class ForEachSample extends GenMod {
	
	public ForEachSample() {
		super();
		addGeneralProperty( MetaUtil.META_FILE_PATH, "The row names of the metadata are used as the looping mechanism for this module." );
	}
	
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception {
		final List<List<String>> data = new ArrayList<>();
		transferResources();
		String localScript = transferScript();

		for (String sample : MetaUtil.getSampleIds() ) {
			final ArrayList<String> lines = new ArrayList<>();
			lines.add( getLauncher() + localScript + " " + sample + " " + getScriptParams() );
			Log.info( GenMod.class, "Core command: " + data );
			data.add( lines );
		}
		
		return data;
	}
	
	/**
	 * Supply a list with the correct number of files to match the number of inputs.
	 * These are not files that actually exist; but this list allows for correct worker assignment.
	 * TODO: revisit this when worker assignment is a little smarter.
	 */
	@Override
	public List<File> getInputFiles() {
		List<File> list = new ArrayList<>();
		for (String element : MetaUtil.getSampleIds() ) list.add( new File(element) );
		return list;
	}
	
	@Override
	public String getDescription() {
		return "Like GenMod, but done for each sample listed in the metadata.";
	}
	
	@Override
	public String getDetails() throws API_Exception {
		return "This is an extention of the [GenMod](../GenMod) module.<br>  " +
			"For the purpose of this module, a sample is defined as a row of the metadata file." +
			"The user script is run using a command:<br> `[launcher] <script> <sample> [params]`";
	}
	
	@Override
	public String getCitationString() {
		return "BioLockJ " + BioLockJUtil.getVersion( ) + System.lineSeparator() + "Module developed by Ivory Blakley";
	}

}
