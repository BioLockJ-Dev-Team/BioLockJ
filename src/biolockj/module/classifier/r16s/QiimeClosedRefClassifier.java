/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Apr 5, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.classifier.r16s;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import biolockj.*;
import biolockj.api.ApiModule;
import biolockj.exception.*;
import biolockj.module.implicit.qiime.MergeQiimeOtuTables;
import biolockj.module.implicit.qiime.QiimeClassifier;
import biolockj.util.*;

/**
 * This BioModule executes the QIIME script pick_closed_reference_otus.py on a FastA sequence files. Unlike open and de
 * novo OTU picking scripts, which require a single multiplexed file for cluster analysis, closed reference OTU picking
 * can be run in batches (output/batch_0, output/batch_1, output/batch_2, etc.). The program awk is used to split the
 * metadata into separate batch-specific QIIME mapping files.
 * 
 * @blj.web_desc QIIME Closed Reference Classifier
 */
public class QiimeClosedRefClassifier extends QiimeClassifier implements ApiModule {

	public QiimeClosedRefClassifier() {
		super();
		addGeneralProperty( Constants.EXE_AWK );
	}

	/**
	 * Create bash script lines to split up the QIIME mapping and fasta files into batches of size
	 * {@link biolockj.Config}.{@value biolockj.Constants#SCRIPT_NUM_WORKERS}
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception {
		final int numFiles = files == null ? 0: files.size();
		Log.info( getClass(),
			"Buidling QIIME Close Ref scripts to assign taxonomy for " + numFiles + " sequence files" );

		final List<List<String>> data = new ArrayList<>();
		List<String> lines = new ArrayList<>();
		if( ModuleUtil.getNumWorkers( this ) == 1 ) {
			lines.addAll( getPickOtuLines( PICK_OTU_SCRIPT, getInputFileDir(), MetaUtil.getPath(), getTempDir() ) );
			lines.add( copyBatchOtuTableToOutputDir( getTempDir(), null ) );
			data.add( lines );
		} else if( files != null ) {
			int startIndex = 1;
			for( final File f: files ) {
				lines.add( "cp " + f.getAbsolutePath() + " " + getBatchFastaDir( data.size() ).getAbsolutePath() );
				if( saveBatch( data.size(), lines.size() ) ) {
					final int numSamples = lines.size();
					Log.debug( getClass(), "Save Worker #" + data.size() + " --> # samples = " + lines.size() );
					data.add( getBatch( lines, data.size(), startIndex ) );
					startIndex += numSamples;
					lines = new ArrayList<>();
				}
			}
			if( !lines.isEmpty() ) data.add( getBatch( lines, data.size(), startIndex ) );
		}
		Log.info( getClass(), "Build script returning data for #workers = " + data.size() );
		return data;
	}

	/**
	 * Call {@link biolockj.module.implicit.qiime.QiimeClassifier} checkOtuPickingDependencies() method to verify OTU
	 * picking script parameters.
	 */
	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		getParams();
	}

	/**
	 * Build the nested list of bash script lines that will be used by {@link biolockj.util.BashScriptBuilder} to build
	 * the worker scripts. Pass{@link #getInputFiles()} to either {@link #buildScript(List)} or
	 * {@link #buildScriptForPairedReads(List)} based on
	 * {@link biolockj.Config}.{@value biolockj.Constants#INTERNAL_PAIRED_READS}. Each worker script is already written
	 * as a batch, so each batch = 1 worker script.
	 */
	@Override
	public void executeTask() throws Exception {
		final List<List<String>> data =
			SeqUtil.hasPairedReads() ? buildScriptForPairedReads( getInputFiles() ): buildScript( getInputFiles() );
		setNumWorkers( data.size() );
		BashScriptBuilder.buildScripts( this, data );
	}

	/**
	 * If paired reads found, return prerequisite module: {@link biolockj.module.seq.PearMergeReads}.
	 */
	@Override
	public List<String> getPostRequisiteModules() throws Exception {
		final List<String> postReqs = new ArrayList<>();
		if( SeqUtil.isMultiplexed() || BioLockJUtil.getPipelineInputFiles().size() > 1 )
			postReqs.add( MergeQiimeOtuTables.class.getName() );
		postReqs.addAll( super.getPostRequisiteModules() );
		return postReqs;
	}

	@Override
	public List<String> getWorkerScriptFunctions() throws Exception {
		final List<String> lines = super.getWorkerScriptFunctions();
		lines.add( "function " + FUNCTION_CREATE_BATCH_MAPPING + "() {" );
		lines.add( Config.getExe( this, Constants.EXE_AWK ) + " 'NR==1' " + MetaUtil.getPath() + " > $1" );
		lines.add( Config.getExe( this, Constants.EXE_AWK ) + " 'NR>'$2'&&NR<='$3 " + MetaUtil.getPath() + " >> $1" );
		lines.add( "}" + RETURN );
		return lines;
	}

	/**
	 * The method returns 1 bash script line that will copy the batch
	 * {@value biolockj.module.implicit.qiime.QiimeClassifier#OTU_TABLE} from the batchDir to the output directory.
	 *
	 * @param batchDir Batch directory
	 * @param batchNum Batch number used to name biom file, or null if processing 1 batch
	 * @return Bash script line to copy temp/otu_table.biom to output/otu_table.biom
	 */
	protected String copyBatchOtuTableToOutputDir( final File batchDir, final Integer batchNum ) {
		final String fileName = batchNum == null ? OTU_TABLE: Constants.OTU_TABLE_PREFIX + "_" + batchNum + ".biom";
		return "cp " + batchDir.getAbsolutePath() + File.separator + OTU_TABLE + " " +
			getOutputDir().getAbsolutePath() + File.separator + fileName;
	}

	/**
	 * Method returns batch script lines for one batch.
	 *
	 * @param lines Batch script lines to copy fasta to batch_#/fasta
	 * @param batchNum Batch number
	 * @param index Start index used to break up mapping file
	 * @return List of batch script lines used to process batch
	 * @throws ConfigException if error occurs batching samples for worker scripts
	 */
	protected List<String> getBatch( final List<String> lines, final int batchNum, final int index )
		throws ConfigException {
		final File batchDir = getBatchDir( batchNum );
		final String mapping = batchDir.getAbsolutePath() + File.separator + BATCH_MAPPING;
		final int endIndex = index + lines.size();
		lines.add( FUNCTION_CREATE_BATCH_MAPPING + " " + mapping + " " + index + " " + endIndex );
		lines.addAll( getPickOtuLines( PICK_OTU_SCRIPT, getBatchFastaDir( batchNum ), mapping, batchDir ) );
		lines.add( copyBatchOtuTableToOutputDir( batchDir, batchNum ) );
		return lines;
	}

	/**
	 * Get the pipeline/module/temp/batch_# dir. Create it if it doesn't exist.
	 *
	 * @param batchNum Batch number
	 * @return Batch temp/batch_# directory
	 */
	protected File getBatchDir( final int batchNum ) {
		final File dir = new File( getTempDir().getAbsolutePath() + File.separator + "batch_" + batchNum );
		if( !dir.isDirectory() ) dir.mkdirs();
		return dir;
	}

	/**
	 * Get the pipeline/module/temp/batch_#/fasta directory. Create it if it doesn't exist.
	 *
	 * @param batchNum Batch number
	 * @return Batch temp/batch_#/fasta directory
	 */
	protected File getBatchFastaDir( final int batchNum ) {
		final File f = new File( getBatchDir( batchNum ) + File.separator + Constants.FASTA );
		if( !f.isFile() ) f.mkdirs();
		return f;
	}

	private boolean saveBatch( final int workerNum, final int sampleCount )
		throws Exception {
		final int minSamplesPerWorker = ModuleUtil.getMinSamplesPerWorker( this ); // 1
		final int maxWorkers = ModuleUtil.getNumMaxWorkers( this ); // 8
		return workerNum < maxWorkers && sampleCount == minSamplesPerWorker + 1 ||
			workerNum >= maxWorkers && sampleCount == minSamplesPerWorker;
	}

	private void setNumWorkers( final Integer count ) throws ConfigNotFoundException, ConfigFormatException {
		if( count != ModuleUtil.getNumWorkers( this ) )
			Config.setConfigProperty( getClass().getSimpleName() + "." + suffix( Constants.SCRIPT_NUM_WORKERS ),
				count.toString() );
	}

	private static String suffix( final String prop ) {
		return prop.indexOf( "." ) > -1 ? prop.substring( prop.indexOf( "." ) + 1 ): prop;
	}

	/**
	 * Closed reference OTU picking script: {@value #PICK_OTU_SCRIPT}
	 */
	public static final String PICK_OTU_SCRIPT = "pick_closed_reference_otus.py";

	/**
	 * Name of each batch mapping file (each in its own batch directory)
	 */
	protected static final String BATCH_MAPPING = "batchMapping" + TSV_EXT;

	/**
	 * Name of the bash function that prepares a batch of seqs for processing: {@value #FUNCTION_CREATE_BATCH_MAPPING}
	 */
	protected static final String FUNCTION_CREATE_BATCH_MAPPING = "createBatchMapping";

	@Override
	public String getDescription() {
		return "Pick OTUs using a closed reference database and construct an OTU table via the QIIME script pick_closed_reference_otus.py";
	}
	
	@Override
	public String getDetails() {
		return "This module picks OTUs using a closed reference database and constructs an OTU table via the QIIME script [pick_closed_reference_otus.py](http://qiime.org/scripts/pick_closed_reference_otus.html).  Taxonomy is assigned using a pre-defined taxonomy map of reference sequence OTU to taxonomy.  This is the fastest OTU picking method since samples can be processed in parallel batches.  Before the QIIME script is run, batches are prepared in the temp directory, with each batch directory containing a fasta directory with *script.batchSize* fasta files and a QIIME mapping file, created with awk, called batchMapping.tsv for the batch of samples.   Inherits from [QiimeClassifier](../../../module.implicit.qiime#QiimeClassifier).";
	}

	@Override
	public String getCitationString() {
		return CITE_QIIME + System.lineSeparator() + "(needs further citation)" + System.lineSeparator() + "http://www.wernerlab.org/software/macqiime/citations";
	}
	
}
