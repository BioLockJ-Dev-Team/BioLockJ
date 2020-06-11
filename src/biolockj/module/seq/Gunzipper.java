/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date June 19, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.seq;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import biolockj.*;
import biolockj.api.ApiModule;
import biolockj.module.SeqModuleImpl;
import biolockj.util.BioLockJUtil;
import biolockj.util.SeqUtil;

/**
 * This BioModule uses gzip to decompress input sequence files.
 * 
 * @blj.web_desc Decompress .gz Files
 */
public class Gunzipper extends SeqModuleImpl implements ApiModule {

	public Gunzipper() {
		addGeneralProperty( Constants.EXE_GZIP );
	}
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception {
		final List<List<String>> data = new ArrayList<>();
		for( final File f: files ) {
			final ArrayList<String> lines = new ArrayList<>();
			if( SeqUtil.isGzipped( f.getName() ) ) lines.add( unzip( f ) );
			else {
				Log.warn( getClass(),
					"May be able to remove this BioModule - input already decompressed: " + f.getAbsolutePath() );
				lines.add( copyToOutputDir( f ) );
			}

			data.add( lines );
		}

		return data;
	}

	/**
	 * This method generates the bash function: {@value #FUNCTION_GUNZIP}.
	 */
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception {
		final List<String> lines = super.getWorkerScriptFunctions();
		lines.add( "function " + FUNCTION_GUNZIP + "() {" );
		lines.add( Config.getExe( this, Constants.EXE_GZIP ) + " -cd $1 > $2" );
		lines.add( "}" + RETURN );
		return lines;
	}

	private String copyToOutputDir( final File file ) {
		return "cp " + file.getAbsolutePath() + " " + getOutputDir().getAbsolutePath();
	}

	private String unzip( final File file ) throws Exception {
		return FUNCTION_GUNZIP + " " + file.getAbsolutePath() + " " + getOutputDir().getAbsolutePath() +
			File.separator + SeqUtil.getSampleId( file ) + SeqUtil.getReadDirectionSuffix( file ) + "." +
			Config.requireString( this, Constants.INTERNAL_SEQ_TYPE );
	}
	
	@Override
	public String getDockerImageName() {
		return "blj_basic";
	}

	/**
	 * Name of the bash function used to decompress gzipped files: {@value #FUNCTION_GUNZIP}
	 */
	protected static final String FUNCTION_GUNZIP = "openZip";

	@Override
	public String getDescription() {
		return "Decompress gzipped files.";
	}

	@Override
	public String getCitationString() {
		return "BioLockJ " + BioLockJUtil.getVersion() + System.lineSeparator() + "Module developed by Mike Sioda";
	}
}
