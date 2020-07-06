/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Jun 2, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.seq;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import biolockj.*;
import biolockj.Properties;
import biolockj.api.ApiModule;
import biolockj.exception.SequnceFormatException;
import biolockj.module.*;
import biolockj.module.classifier.ClassifierModule;
import biolockj.module.report.Email;
import biolockj.util.*;

/**
 * This BioModule will merge sequence files into a single combined sequence file, with either the sample ID or an
 * identifying bar-code (if defined in the metatata) is stored in the sequence header.<br>
 * BioLockJ is designed to run on demultiplexed data so this must be the last module to run in its branch.
 * 
 * @blj.web_desc Multiplexer
 */
public class Multiplexer extends JavaModuleImpl implements SeqModule, ApiModule {

	public Multiplexer() {
		super();
		addNewProperty( DO_GZIP, Properties.BOOLEAN_TYPE, "If enabled, the multiplexed output will be gzipped" );
		addGeneralProperty( MetaUtil.META_BARCODE_COLUMN, "Values should be unique." );
	}

	/**
	 * Validate module dependencies:
	 * <ol>
	 * <li>Validate this is the last module to run (excluding {@link biolockj.module.report.Email})
	 * </ol>
	 * If this module was completed on a previous run, update the property:
	 * {@link biolockj.Config}.{@value biolockj.Constants#INTERNAL_MULTIPLEXED} = {@value biolockj.Constants#TRUE}
	 *
	 */
	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		Log.warn( getClass(), "BioLockJ requires demultiplexed data, so this must be the last module except Email" );
		validateModuleOrder();
	}

	@Override
	public List<File> getSeqFiles( final Collection<File> files ) throws SequnceFormatException {
		return SeqUtil.getSeqFiles( files );
	}

	/**
	 * Produce a summary message with counts on total number of reads and number reads multiplexed.
	 */
	@Override
	public String getSummary() throws Exception {
		final StringBuffer sb = new StringBuffer();
		try {
			if( this.totalNumRvReads > 0 ) {
				sb.append( "# Forward Read Files multiplexed = " + this.fwMap.keySet().size() + RETURN );
				sb.append( "# Reverse Read Files multiplexed = " + this.rvMap.keySet().size() + RETURN );
				sb.append( "# Total Forward Reads = " + this.totalNumFwReads + RETURN );
				sb.append( "# Total Reverse Reads = " + this.totalNumRvReads + RETURN );
			} else if( this.totalNumFwReads > 0 ) {
				sb.append( "# Samples multiplexed = " + this.fwMap.keySet().size() + RETURN );
				sb.append( "# Total Reads = " + this.totalNumFwReads + RETURN );
			} else sb.append( "Module incomplete - no output produced!" + RETURN );

			if( this.rcCount > 0 )
				sb.append( "# Reads saved with existing reverse compliment header barcode: " + this.rcCount + RETURN );

		} catch( final Exception ex ) {
			final String msg = "Unable to complete module summary: " + ex.getMessage();
			sb.append( msg + RETURN );
			Log.warn( getClass(), msg );
		}

		return super.getSummary() + sb.toString();
	}

	// /**
	// * This method generates the bash function: {@value #FUNCTION_GUNZIP}.
	// */
	// @Override
	// public List<String> getWorkerScriptFunctions() throws Exception
	// {
	// final List<String> lines = super.getWorkerScriptFunctions();
	// if( Config.getBoolean( this, DO_GZIP ) )
	// {
	// lines.add( "function " + FUNCTION_GZIP + "() {" );
	// lines.add( Config.getExe( this, Constants.EXE_GZIP ) + " " + getOutputDir().getAbsolutePath() + File.separator
	// + "*" );
	// lines.add( "}" + RETURN );
	// }
	//
	// return lines;
	// }

	/**
	 * Execute {@link #multiplex(File)} on each input file
	 */
	@Override
	public void runModule() throws Exception {
		Log.info( getClass(), "Multiplexing file type = " + Config.requireString( this, Constants.INTERNAL_SEQ_TYPE ) );

		for( final File f: getInputFiles() )
			multiplex( f );

		if( Config.getBoolean( this, DO_GZIP ) ) {
			Log.warn( getClass(), "BioLockJ gzip data in: " + this.muxFiles );
			compressData();
			removeDecompressedFiles();
		}
	}

	/**
	 * Get the header for the sequence.
	 *
	 * @param file Sequence file in fasta or fastq format
	 * @param seqLines Sequence lines for 1 read
	 * @return the header row for the sequence
	 * @throws Exception if errors occur while obtaining header
	 */
	protected String getHeader( final File file, final List<String> seqLines ) throws Exception {
		final String header = seqLines.get( 0 ).trim();
		final String headerChar = header.substring( 0, 1 );
		final String sampleId = SeqUtil.getSampleId( file );
		final long numReads = incrementNumReads( file );

		if( DemuxUtil.barcodeInHeader() ) return header;
		else if( DemuxUtil.hasValidBarcodes() ) {
			final String barcode =
				MetaUtil.getField( sampleId, Config.getString( this, MetaUtil.META_BARCODE_COLUMN ) );
			final String rc = SeqUtil.reverseComplement( barcode );

			if( header.contains( barcode ) ) return header;
			else if( header.contains( rc ) ) {
				this.rcCount++;
				return header;
			} else return header + " " + barcode;
		} else return headerChar + sampleId + "_" + sampleId + "." + numReads + ":" + header.substring( 1 );
	}

	/**
	 * Add file sequences to the multiplexed file. If barcode is defined in the metadata file and set in the Config
	 * file, it will be added to the header (if not already in the header line). If no barcode value is configured, the
	 * sample ID will be used in the sequence headers.
	 *
	 * @param sample Sequence file in Fasta or Fastq format
	 * @throws Exception if I/O errors occur creating multiplexed file
	 */
	protected void multiplex( final File sample ) throws Exception {
		Log.info( getClass(), "Multiplexing file  = " + sample.getAbsolutePath() );
		final File muxFile = new File( getMutliplexeFileName( sample ) );
		final List<String> seqLines = new ArrayList<>();
		BufferedReader reader = null;
		BufferedWriter writer = null;
		try {
			reader = BioLockJUtil.getFileReader( sample );
			writer = new BufferedWriter( new FileWriter( muxFile, true ) );
			for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
				seqLines.add( line );
				if( seqLines.size() == SeqUtil.getNumLinesPerRead() ) // full read
				{
					writer.write( getHeader( sample, seqLines ) + RETURN );
					for( int i = 1; i < seqLines.size(); i++ )
						writer.write( seqLines.get( i ) + RETURN );
					seqLines.clear();
				}
			}
		} finally {
			if( writer != null ) writer.close();
			if( reader != null ) reader.close();
		}
	}

	/**
	 * This method ensures that the multiplexed data is not passed to other modules.<br>
	 * This must be the last module, or last module before the Email.
	 *
	 * @throws Exception if not the last module before {@link biolockj.module.report.Email}
	 */
	protected void validateModuleOrder() throws Exception {
		final int modCount = Pipeline.getModules().size();
		final BioModule nextModule = ModuleUtil.getNextModule( this );
		final BioModule lastModule = Pipeline.getModules().get( modCount - 1 );
		final BioModule penultimateModule = modCount > 1 ? Pipeline.getModules().get( modCount - 2 ): null;

		if( lastModule.equals( this ) ) Log.info( getClass(), "Multiplexing seqs as final step in pipeline." );
		else if( lastModule instanceof Email && penultimateModule != null && penultimateModule.equals( this ) )
			Log.info( getClass(), "Multiplexing seqs  as final step before sending email notification" );
		else if( nextModule instanceof ClassifierModule )
			Log.info( getClass(), "Multiplexing seqs, before branching pipeline with new classifier" );
		else throw new Exception( "Invalid BioModule configuration! " + getClass().getName() +
			" must be the last module executed in the pipeline branch (or last before Email)." + RETURN +
			"All other BioLockJ modules require demultiplexed data." );
	}

	private void compressData() throws Exception {
		for( final String path: this.muxFiles ) {
			FileInputStream fis = null;
			FileOutputStream fos = null;
			GZIPOutputStream gzipOS = null;
			try {
				fis = new FileInputStream( path );
				fos = new FileOutputStream( path + Constants.GZIP_EXT );
				gzipOS = new GZIPOutputStream( fos );
				final byte[] buffer = new byte[ 1024 ];
				int len;
				while( ( len = fis.read( buffer ) ) != -1 )
					gzipOS.write( buffer, 0, len );
			} finally {
				if( gzipOS != null ) gzipOS.close();
				if( fos != null ) fos.close();
				if( fis != null ) fis.close();
			}
		}
	}

	private String getMutliplexeFileName( final File file ) throws Exception {
		final String path = getOutputDir().getAbsolutePath() + File.separator + Config.pipelineName() +
			SeqUtil.getReadDirectionSuffix( file ) + "." + SeqUtil.getSeqType();
		this.muxFiles.add( path );
		return path;
	}

	private long getNumReads( final File file ) {
		Long numReads = null;
		if( SeqUtil.hasPairedReads() && !SeqUtil.isForwardRead( file.getName() ) )
			numReads = this.rvMap.get( file.getName() );
		else numReads = this.fwMap.get( file.getName() );

		if( numReads == null ) numReads = 0L;
		return numReads;
	}

	private long incrementNumReads( final File file ) {
		Long numReads = getNumReads( file );
		numReads++;

		if( SeqUtil.hasPairedReads() && !SeqUtil.isForwardRead( file.getName() ) ) {
			this.rvMap.put( file.getName(), numReads );
			this.totalNumRvReads++;
		} else {
			this.fwMap.put( file.getName(), numReads );
			this.totalNumFwReads++;
		}

		return numReads;
	}

	private void removeDecompressedFiles() {
		for( final String path: this.muxFiles )
			new File( path ).delete();
	}

	private final Map<String, Long> fwMap = new HashMap<>();
	private final Set<String> muxFiles = new HashSet<>();
	private int rcCount = 0;
	private final Map<String, Long> rvMap = new HashMap<>();
	private long totalNumFwReads = 0L;
	private long totalNumRvReads = 0L;
	/**
	 * {@link biolockj.Config} boolean property: {@value #DO_GZIP}:
	 */
	protected static final String DO_GZIP = "multiplexer.gzip";
	
	@Override
	public String getDescription() {
		return "Multiplex samples into a single file, or two files (one with forward reads, one with reverse reads) if multiplexing paired reads.";
	}

	@Override
	public String getCitationString() {
		return "Module developed by Mike Sioda" + System.lineSeparator() + "BioLockJ " + BioLockJUtil.getVersion();
	}

	// private static final String FUNCTION_GZIP = "gZip";
}
