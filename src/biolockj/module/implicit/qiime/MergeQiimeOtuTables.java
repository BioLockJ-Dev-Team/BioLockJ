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
package biolockj.module.implicit.qiime;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import biolockj.Constants;
import biolockj.Log;
import biolockj.module.BioModule;
import biolockj.module.ScriptModuleImpl;
import biolockj.module.classifier.r16s.QiimeClosedRefClassifier;
import biolockj.util.ModuleUtil;

/**
 * This BioModule will run immediately after QiimeClosedRefClassifier if multiple otu_table.biom files were created.
 */
public class MergeQiimeOtuTables extends ScriptModuleImpl {

	/**
	 * Build a single bash script line to call {@value #SCRIPT_MERGE_OTU_TABLES} to create the single
	 * {@value biolockj.module.implicit.qiime.QiimeClassifier#OTU_TABLE}, as required by
	 * {@link biolockj.module.implicit.parser.r16s.QiimeParser}.
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception {
		String tables = "";
		for( final File f: files )
			if( f.getName().startsWith( Constants.OTU_TABLE_PREFIX ) )
				tables += ( tables.isEmpty() ? "": "," ) + f.getAbsolutePath();
			else Log.warn( getClass(),
				"Ignoring non-" + Constants.OTU_TABLE_PREFIX + " input file: " + f.getAbsolutePath() );

		final List<List<String>> data = new ArrayList<>();
		final List<String> lines = new ArrayList<>();
		final String outputFile = getOutputDir().getAbsolutePath() + File.separator + QiimeClassifier.OTU_TABLE;
		lines.add( SCRIPT_MERGE_OTU_TABLES + " -i " + tables + " -o " + outputFile );
		data.add( lines );

		return data;
	}

	/**
	 * Verify the previous module = {@link biolockj.module.classifier.r16s.QiimeClosedRefClassifier}
	 */
	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		if( !( ModuleUtil.getPreviousModule( this ) instanceof QiimeClosedRefClassifier ) )
			throw new Exception( "Module order exception!  " + RETURN + getClass().getName() +
				" must run immediately after " + QiimeClosedRefClassifier.class.getName() + " (if configured)" );
	}

	/**
	 * Call {@link #buildScript(List)} to create bash script lines needed to merge
	 * {@value biolockj.module.implicit.qiime.QiimeClassifier#OTU_TABLE}s with {@value #SCRIPT_MERGE_OTU_TABLES} unless
	 * only 1 input file found, in which case, just copy it to the output dir.
	 */
	@Override
	public void executeTask() throws Exception {
		if( getInputFiles().size() > 1 ) super.executeTask();
		else if( getInputFiles().size() == 1 ) {
			Log.warn( getClass(),
				"Previous module only output 1 " + QiimeClassifier.OTU_TABLE + "so there is nothing to merge" );
			FileUtils.copyFileToDirectory( getInputFiles().get( 0 ), getOutputDir() );
		} else throw new Exception( "No " + Constants.OTU_TABLE_PREFIX + " files to merge" );
	}

	@Override
	public boolean isValidInputModule( final BioModule module ) {
		return module instanceof QiimeClosedRefClassifier;
	}
	
	@Override
	public String getDockerImageOwner() {
		return Constants.MAIN_DOCKER_OWNER;
	}
	
	@Override
	public String getDockerImageName() {
		return "qimme_classifier";
	}
	
	@Override
	public String getDockerImageTag() {
		return "v1.3.18";
	}

	/**
	 * QIIME script to merge multiple OTU tables in biom format.
	 */
	protected static final String SCRIPT_MERGE_OTU_TABLES = "merge_otu_tables.py";
}
