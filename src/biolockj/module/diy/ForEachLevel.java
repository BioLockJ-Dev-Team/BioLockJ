package biolockj.module.diy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import biolockj.Config;
import biolockj.Constants;
import biolockj.Log;
import biolockj.api.API_Exception;
import biolockj.util.BioLockJUtil;

public class ForEachLevel extends GenMod {
	
	public ForEachLevel() {
		super();
		addGeneralProperty( Constants.REPORT_TAXONOMY_LEVELS, "Used as the looping mechanism for this module." );
	}
	
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception {
		final List<List<String>> data = new ArrayList<>();
		transferResources();
		String localScript = transferScript();

		for (String level : Config.getList( this, Constants.REPORT_TAXONOMY_LEVELS ) ) {
			final ArrayList<String> lines = new ArrayList<>();
			lines.add( getLauncher() + localScript + " " + level + " " + getScriptParams() );
			Log.info( GenMod.class, "Core command: " + data );
			data.add( lines );
		}
		
		return data;
	}
	
	@Override
	public String getDescription() {
		return "Like GenMod, but done for each taxonomic level.";
	}
	
	@Override
	public String getDetails() throws API_Exception {
		return "This is an extention of the [GenMod](../GenMod) module.<br>  " +
			"This module runs the specified script for each of the configured taxonomic levels, see " + Constants.REPORT_TAXONOMY_LEVELS +  " under (General Properties)[GENERATED/General-Properties/#report]." +
			"The user script is run using a command:<br> `[launcher] <script> <level> [params]`";
	}
	
	@Override
	public String getCitationString() {
		return "BioLockJ " + BioLockJUtil.getVersion( ) + System.lineSeparator() + "Module developed by Ivory Blakley";
	}


}
