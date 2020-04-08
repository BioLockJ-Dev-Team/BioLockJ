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

import java.io.*;
import java.util.*;
import biolockj.*;
import biolockj.Properties;
import biolockj.api.ApiModule;
import biolockj.exception.MetadataException;
import biolockj.module.CountsReads;
import biolockj.module.SeqModuleImpl;
import biolockj.module.implicit.RegisterNumReads;
import biolockj.util.*;

/**
 * This BioModule will merge forward and reverse fastq files using PEAR.<br>
 * For more informations, see the online PEAR manual:
 * <a href="https://sco.h-its.org/exelixis/web/software/pear/doc.html" target=
 * "_top">https://sco.h-its.org/exelixis/web/software/pear/doc.html</a>
 * 
 * @blj.web_desc Merge Reads with PEAR
 */
public class PearMergeReads extends SeqModuleImpl implements ApiModule, CountsReads {
	
	public PearMergeReads() {
		super();
		addNewProperty( EXE_PEAR, Properties.EXE_PATH, "" );
		addNewProperty( EXE_PEAR_PARAMS, Properties.STRING_TYPE, "optionally pass additional parameters to pear." );
	}

	/**
	 * Build the script lines for each sample as a nested list. PAIR program will be called once for each pair of files
	 * to output a single merged read.
	 *
	 * @param files List of module input files
	 * @return List of nested bash script lines.
	 * @throws Exception if error occurs generating bash script lines
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception {
		this.sampleIds.addAll( MetaUtil.getSampleIds() );
		final List<List<String>> data = new ArrayList<>();
		final Map<File, File> map = SeqUtil.getPairedReads( files );
		final Set<File> keys = new TreeSet<>( map.keySet() );

		for( final File file: keys ) {
			final List<String> lines = new ArrayList<>();
			lines.add( FUNCTION_PEAR_MERGE + " " + SeqUtil.getSampleId( file.getName() ) + " " +
				file.getAbsolutePath() + " " + map.get( file ).getAbsolutePath() + " " +
				getTempDir().getAbsolutePath() + " " + getOutputDir().getAbsolutePath() );

			data.add( lines );
		}

		return data;
	}

	/**
	 * Validate module dependencies
	 * <ol>
	 * <li>Verify input files are in fastq format
	 * <li>Verify matching paired reads are found
	 * <li>Validate required {@link biolockj.Config}.{@link #EXE_PEAR} property
	 * <li>If running a restarted pipeline that already merged reads, set paired indicator to false
	 * </ol>
	 */
	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		Config.requireString( this, Constants.INPUT_FORWARD_READ_SUFFIX );
		Config.requireString( this, Constants.INPUT_REVERSE_READ_SUFFIX );
		getRuntimeParams( Config.getList( this, EXE_PEAR_PARAMS ), NUM_THREADS_PARAM );

		if( ModuleUtil.isComplete( this ) ) return;

		if( !SeqUtil.isFastQ() ) throw new Exception( "PAIRED READS CAN ONLY BE ASSEMBLED WITH <FASTQ> FILE INPUT" );

		if( !SeqUtil.hasPairedReads() ) throw new Exception( getClass().getName() +
			" requires paired input data as a combined multiplexed file or as separate files named with " +
			" matching sample IDs ending in the forward & reverse file suffix values: " +
			Config.requireString( this, Constants.INPUT_FORWARD_READ_SUFFIX ) + " & " +
			Config.requireString( this, Constants.INPUT_REVERSE_READ_SUFFIX ) );
	}

	/**
	 * Set {@link biolockj.Config}.{@value biolockj.Constants#INTERNAL_PAIRED_READS} = {@value biolockj.Constants#FALSE}
	 * and register number of reads.
	 */
	@Override
	public void cleanUp() throws Exception {
		super.cleanUp();
		Config.setConfigProperty( Constants.INTERNAL_PAIRED_READS, Constants.FALSE );
		if( !MetaUtil.getFieldNames().contains( getMetaColName() ) && this.readsPerSample != null ) {
			Log.info( getClass(),
				"Counting # merged reads/sample for " + getOutputDir().listFiles().length + " files" );
			for( final File f: getOutputDir().listFiles() ) {
				final long count = SeqUtil.countNumReads( f );
				Log.info( getClass(), "Num merged Reads for File:[" + f.getName() + "] ==> ID:[" +
					SeqUtil.getSampleId( f.getName() ) + "] = " + count );
				this.readsPerSample.put( SeqUtil.getSampleId( f.getName() ), Long.toString( count ) );
			}

			MetaUtil.addColumn( getMetaColName(), this.readsPerSample, getOutputDir(), true );
		} else Log.warn( getClass(),
			"Counts for # merged reads/sample already found in metadata, not re-counting " + MetaUtil.getPath() );
		RegisterNumReads.setNumReadFieldName( getMetaColName() );
	}

	/**
	 * Produce summary message with min, max, mean, and median number of reads.
	 */
	@Override
	public String getSummary() throws Exception {
		final String label = "Paired Reads";
		final int pad = SummaryUtil.getPad( label );
		String summary = SummaryUtil.getCountSummary( this.readsPerSample, "Paired Reads", true );
		this.sampleIds.removeAll( this.readsPerSample.keySet() );
		if( !this.sampleIds.isEmpty() )
			summary += BioLockJUtil.addTrailingSpaces( "Removed empty metadata records:", pad ) +
				BioLockJUtil.getCollectionAsString( this.sampleIds );
		this.readsPerSample = null;
		return super.getSummary() + summary;
	}

	/**
	 * This method generates the required bash functions used by the worker scripts: {@value #FUNCTION_PEAR_MERGE}
	 */
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception {
		final List<String> lines = super.getWorkerScriptFunctions();
		lines.add( "function " + FUNCTION_PEAR_MERGE + "() {" );
		lines.add( Config.getExe( this, EXE_PEAR ) + " " +
			getRuntimeParams( Config.getList( this, EXE_PEAR_PARAMS ), NUM_THREADS_PARAM ) + FW_READ_PARAM + "$2 " +
			RV_READ_PARAM + "$3 " + OUTPUT_PARAM + "$4" + File.separator + "$1" );
		lines.add( "mv $4" + File.separator + "$1.assembled." + Constants.FASTQ + " $5" + File.separator + "$1." +
			Constants.FASTQ );
		lines.add( "}" + RETURN );
		return lines;
	}

	private String getMetaColName() throws FileNotFoundException, MetadataException, IOException {
		if( this.otuColName == null ) this.otuColName = MetaUtil.getSystemMetaCol( this, NUM_MERGED_READS );
		return this.otuColName;
	}
	
	@Override
	public String getDockerImageName() {
		return "pear";
	}

	private String otuColName = null;
	private Map<String, String> readsPerSample = new HashMap<>();
	private final Set<String> sampleIds = new HashSet<>();
	/**
	 * Metadata column name for column that holds number of reads per sample after merging: {@value #NUM_MERGED_READS}
	 */
	public static final String NUM_MERGED_READS = "Num_Merged_Reads";
	/**
	 * {@link biolockj.Config} property {@value #EXE_PEAR} defines the command line PEAR executable
	 */
	protected static final String EXE_PEAR = "exe.pear";
	/**
	 * {@link biolockj.Config} property {@value #EXE_PEAR_PARAMS} is used to set the PEAR executable runtime parameters
	 */
	protected static final String EXE_PEAR_PARAMS = "pearMergeReads.pearParams";
	/**
	 * Name of the bash function that merges files with PEAR: {@value #FUNCTION_PEAR_MERGE}
	 */
	protected static final String FUNCTION_PEAR_MERGE = "mergeReads";

	private static final String FW_READ_PARAM = "-f ";

	private static final String NUM_THREADS_PARAM = "-j";

	private static final String OUTPUT_PARAM = "-o ";

	private static final String RV_READ_PARAM = "-r ";

	@Override
	public String getDescription() {
		return "Run pear, the Paired-End reAd mergeR";
	}

	@Override
	public String getCitationString() {
		return "Jiajie Zhang, Kassian Kobert, Tomáš Flouri, Alexandros Stamatakis, PEAR: a fast and accurate Illumina Paired-End reAd mergeR, Bioinformatics, Volume 30, Issue 5, 1 March 2014, Pages 614–620, https://doi.org/10.1093/bioinformatics/btt593" 
						+ System.lineSeparator() + "https://cme.h-its.org/exelixis/web/software/pear/doc.html" 
						+ System.lineSeparator() + "Module developed by Mike Sioda";
	}
	
}
