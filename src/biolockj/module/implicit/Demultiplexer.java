/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date September 21, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.implicit;

import java.io.*;
import java.util.*;
import biolockj.*;
import biolockj.Properties;
import biolockj.api.API_Exception;
import biolockj.api.ApiModule;
import biolockj.api.BuildDocs;
import biolockj.exception.ConfigFormatException;
import biolockj.exception.SequnceFormatException;
import biolockj.module.JavaModuleImpl;
import biolockj.module.SeqModule;
import biolockj.util.*;

/**
 * This BioModule splits multiplexed data into a separate file or pair of files (for paired reads) for each sample. The
 * sample ID, or an identification barcode must be included in the header of each read. Barcodes are matched to Sample
 * IDs using the metadata column configured via property "metadata.barcodeColumn".<br>
 * If demultiplexer.strategy=barcode_in_header, register a match if the barcode is found anywhere in the sequence
 * header.<br>
 * If demultiplexer.strategy=barcode_in_seq, register a match if the sequence itself starts with any of the barcodes.
 * The output multiplexed file will have barcodes removed from the sequences since they no longer serve any purpose.<br>
 * Sequences with no matching barcode, or without a matching paired header (if paired), are output to a "NO_MATCH" file
 * in the module temp directory.<br>
 * Paired reads are accepted in 2 multiplexed formats:
 * <ol>
 * <li>Forward and reverse reads can be partitioned into 2 separate files identified by identified by: input.suffixFw
 * and input.suffixRv.
 * <li>Forward and reverse reads can be combined in a single file with read direction indicated in the header by values
 * "1:N:" and "2:N:"
 * </ol>
 * 
 * @blj.web_desc Demultiplexer
 */
public class Demultiplexer extends JavaModuleImpl implements SeqModule, ApiModule {
	
	public Demultiplexer() throws API_Exception {
		super();
		addGeneralProperty( MetaUtil.META_BARCODE_COLUMN, "Values must be unique." );
		addGeneralProperty( MetaUtil.META_FILE_PATH, "Used for matching sample id to barcodes." );
		addGeneralProperty( DemuxUtil.BARCODE_CUTOFF, "(DeuxUtil)"  );
		addGeneralProperty( DemuxUtil.BARCODE_USE_REV_COMP, "(DeuxUtil)" );
		addGeneralProperty( DemuxUtil.DEMUX_STRATEGY, "(DeuxUtil)"  );
		addGeneralProperty( Constants.DEFAULT_MOD_DEMUX );
	}

	/**
	 * Validate module dependencies:
	 * <ol>
	 * <li>If {@link biolockj.Config}.{@value biolockj.util.DemuxUtil#DEMUX_STRATEGY} indicates use of barcodes to
	 * demultiplexer, validate metadata column named
	 * {@link biolockj.Config}.{@value biolockj.util.MetaUtil#META_BARCODE_COLUMN} exists
	 * <li>Call {@link biolockj.util.DemuxUtil#setMultiplexedConfig()} to set multiplexed Config if needed
	 * <li>If {@link biolockj.Config}.{@value biolockj.util.DemuxUtil#BARCODE_CUTOFF} defined, validate between 0.0 -
	 * 1.0
	 * </ol>
	 */
	@Override
	public void checkDependencies() throws Exception {
		DemuxUtil.setMultiplexedConfig();
		getBarcodeCutoff();
		final String demuxStrategy = "Config property [ " + DemuxUtil.DEMUX_STRATEGY + "=" +
			Config.getString( this, DemuxUtil.DEMUX_STRATEGY ) + " ]";

		if( Config.getString( this, DemuxUtil.DEMUX_STRATEGY ) == null ) Log.info( getClass(),
			DemuxUtil.DEMUX_STRATEGY +
				" is undefined for a multiplexed dataset.  Demultiplexer will analyze the file to determine if Sample IDs or barcodes " +
				"should be used for demultiplexing.  Demultiplexer will also determine if reverse compliment barcodes are needed and set: " +
				DemuxUtil.BARCODE_USE_REV_COMP );
		else if( DemuxUtil.demuxWithBarcode() ) {
			if( !MetaUtil.exists() ) throw new Exception( demuxStrategy + " but metadata file is undefined" );

			if( !MetaUtil.getFieldNames().contains( Config.requireString( this, MetaUtil.META_BARCODE_COLUMN ) ) )
				throw new Exception( demuxStrategy + " but the barcode column configured [ " +
					MetaUtil.META_BARCODE_COLUMN + "=" + Config.requireString( this, MetaUtil.META_BARCODE_COLUMN ) +
					" ] is not found in the metadata: " + MetaUtil.getPath() );

			if( MetaUtil.getFieldValues( Config.requireString( this, MetaUtil.META_BARCODE_COLUMN ), true ).isEmpty() )
				throw new Exception( demuxStrategy + " but the barcode column configured [ " +
					MetaUtil.META_BARCODE_COLUMN + "=" + Config.requireString( this, MetaUtil.META_BARCODE_COLUMN ) +
					" ] is empty in the metadata file: " + MetaUtil.getPath() );

			if( DemuxUtil.barcodeInSeq() )
				Log.info( getClass(), "Barcode sequences will be removed from the output file sequences." );
		} else {
			Log.info( getClass(),
				demuxStrategy + " so Demultiplexer will use the Config properties [" + Constants.INPUT_TRIM_PREFIX +
					" & " + Constants.INPUT_TRIM_SUFFIX + "] to extract the Sample ID from the sequence header" );
			Log.info( getClass(), "Config property [ " + Constants.INPUT_TRIM_PREFIX + " ] = " +
				Config.getString( this, Constants.INPUT_TRIM_PREFIX ) );
			Log.info( getClass(), "Config property [ " + Constants.INPUT_TRIM_SUFFIX + " ] = " +
				Config.getString( this, Constants.INPUT_TRIM_SUFFIX ) );

		}
		super.checkDependencies();
	}

	/**
	 * Update SeqUtil to indicate data has been demultiplexed.<br>
	 *
	 * @throws Exception if unable to modify property
	 */
	@Override
	public void cleanUp() throws Exception {
		super.cleanUp();
		Config.setConfigProperty( Constants.INTERNAL_MULTIPLEXED, Constants.FALSE );
	}

	@Override
	public List<File> getInputFiles() {
		if( getFileCache().isEmpty() ) try {
			cacheInputFiles( getSeqFiles( findModuleInputFiles() ) );
		} catch( final SequnceFormatException ex ) {
			Log.error( getClass(), "Unable to find module input sequence files: " + ex.getMessage(), ex );
		}
		return getFileCache();
	}

	@Override
	public List<File> getSeqFiles( final Collection<File> files ) throws SequnceFormatException {
		return SeqUtil.getSeqFiles( files );
	}

	/**
	 * Produces initial count and demultiplexed output count summaries for forward/reverse reads.
	 */
	@Override
	public String getSummary() throws Exception {
		final StringBuffer sb = new StringBuffer();
		try {
			if( SeqUtil.hasPairedReads() ) {
				sb.append( "# Forward Reads: " + this.numTotalFwReads + RETURN );
				sb.append( "# Reverse Reads: " + this.numTotalRvReads + RETURN );

				if( this.numTotalFwReads != this.numValidFwReads )
					sb.append( "# Valid Forward Reads (matched to a sample): " + this.numValidFwReads + RETURN );
				if( this.numTotalRvReads != this.numValidRvReads )
					sb.append( "# Valid Reverse Reads (matched to a sample): " + this.numValidRvReads + RETURN );
			} else {
				sb.append( "# Total Reads:  " + this.numTotalFwReads + RETURN );
				if( this.numTotalFwReads != this.numValidFwReads )
					sb.append( "# Valid Reads (matched to a sample): " + this.numValidFwReads + RETURN );
			}
		} catch( final Exception ex ) {
			final String msg = "Unable to complete module summary: " + ex.getMessage();
			sb.append( msg + this.summary + RETURN );
			Log.warn( getClass(), msg );
		}

		return super.getSummary() + sb.toString() + this.summary;
	}

	/**
	 * Module execution summary:<br>
	 * <ol>
	 * <li>Execute {@link #breakUpFiles()} to split the multiplex file into smaller size for processing
	 * <li>Execute {@link #getValidHeaders()} to obtain list of valid headers matched to sample ID with the metadata
	 * file and also verifies matching forward and reverse read headers if demuliplexing paired reads.
	 * <li>Execute {@link #demultiplex(Map)} to demultiplex the data into a separate file (or pair of files) for each
	 * sample
	 * </ol>
	 * <p>
	 * If paired reads are combined in a single file the read direction must be identified in the sequence header using
	 * key strings {@value biolockj.util.SeqUtil#ILLUMINA_FW_READ_IND}
	 * {@value biolockj.util.SeqUtil#ILLUMINA_RV_READ_IND}
	 */
	@Override
	public void runModule() throws Exception {
		breakUpFiles();
		demultiplex( getValidHeaders() );
	}

	/**
	 * Some multiplexed files can be very large. This method breaks each input file into smaller files, each
	 * {@value #NUM_LINES_TEMP_FILE} lines in size to avoid memory issues while processing.
	 *
	 * @throws Exception if unexpected errors occur at runtime
	 */
	protected void breakUpFiles() throws Exception {
		final boolean useBarcodes = DemuxUtil.hasValidBarcodes();
		File testFile = null;
		for( final File file: getInputFiles() ) {
			Log.info( getClass(),
				"Break multiplexed file [ " + file.getAbsolutePath() + " ] into files with a max #lines = [ " +
					NUM_LINES_TEMP_FILE + " ] to avoid memory issues while processing." );
			Log.info( getClass(), "Found #lines/read = " + SeqUtil.getNumLinesPerRead() );
			long numReads = 0L;
			long headerFwBarcodes = 0L;
			long headerRvBarcodes = 0L;
			long seqFwBarcodes = 0L;
			long seqRvBarcodes = 0L;
			final BufferedReader reader = BioLockJUtil.getFileReader( file );
			try {
				final List<String> seqLines = new ArrayList<>();
				final List<String> read = new ArrayList<>();
				int i = 0;
				for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
					read.add( line );

					if( useBarcodes ) {
						if( testFile == null && read.size() == 1 ) {
							numReads++;
							final int testBarcodes = hasBarcode( line );
							if( testBarcodes == 1 ) headerFwBarcodes++;
							else if( testBarcodes == 2 ) headerRvBarcodes++;
						}

						if( testFile == null && read.size() == 2 ) {
							final int testBarcodes = hasBarcode( line );
							if( testBarcodes == 1 ) seqFwBarcodes++;
							else if( testBarcodes == 2 ) seqRvBarcodes++;
						}
					}

					if( read.size() == SeqUtil.getNumLinesPerRead() ) {
						seqLines.addAll( read );
						read.clear();
					}

					if( seqLines.size() >= NUM_LINES_TEMP_FILE ) {
						writeSample( seqLines, getSplitFileName( file.getName(), i++ ) );
						seqLines.clear();
					}
				}

				if( !seqLines.isEmpty() ) writeSample( seqLines, getSplitFileName( file.getName(), i++ ) );
			} finally {
				if( reader != null ) reader.close();
			}

			Log.info( getClass(), "Done splitting file: " + file.getAbsolutePath() );

			if( testFile == null && numReads > 0 ) {
				testFile = file;
				buildSummaryAndSetConfig( testFile, numReads, headerFwBarcodes, seqFwBarcodes, headerRvBarcodes,
					seqRvBarcodes );
			}
		}

		Log.info( getClass(), "Number of temp files created: " + getSplitDir().listFiles().length );
	}

	/**
	 * Demultiplex the file into separate small temp files, with {@value #NUM_LINES_TEMP_FILE} lines each for
	 * processing. If paired reads, generate separate forward and reverse files for each sample output file named by
	 * sample ID.
	 *
	 * @param validHeaders Set of valid headers
	 * @throws Exception if error occurs reading the multiplexed file
	 */
	protected void demultiplex( final Map<String, Set<String>> validHeaders ) throws Exception {
		printCounts( validHeaders );
		// int count = 0;
		// int loopCount = 0;
		boolean doPrint = true;

		for( final File file: getSplitDir().listFiles() ) {
			Log.info( getClass(), "Demultiplexing file " + file.getAbsolutePath() );
			final List<String> seqLines = new ArrayList<>();
			final Map<String, List<String>> output = new HashMap<>();
			final BufferedReader reader = BioLockJUtil.getFileReader( file );
			try {
				for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
					seqLines.add( line );
					if( seqLines.size() == SeqUtil.getNumLinesPerRead() ) {
						final String header = SeqUtil.getHeader( seqLines.get( 0 ) );
						final String sampleId = getSampleId( header, validHeaders );

						String otu = null;

						if( sampleId == null ) otu = getNoMatchFileName( file.getName(), seqLines.get( 0 ) );
						else {
							otu = getOutputFileName( sampleId, file.getName(), seqLines.get( 0 ) );
							incrementCounts( file.getName(), seqLines.get( 0 ) );

							if( doPrint ) {
								doPrint = false;
								Log.info( getClass(), "EXAMPLE Demultiplexed Sample ID: " + sampleId );
								Log.info( getClass(), "EXAMPLE Demultiplexed sequence file: " + otu );
							}
						}

						if( !output.keySet().contains( otu ) ) output.put( otu, new ArrayList<>() );

						output.get( otu ).addAll( seqLines );
						seqLines.clear();
					}
				}

				for( final String outName: output.keySet() ) {
					Log.debug( getClass(), outName + " adding # lines = " + output.get( outName ).size() );
					writeSample( output.get( outName ), outName );
				}
			} finally {
				if( reader != null ) reader.close();
			}
		}
	}

	/**
	 * Get valid forward read headers that belong to reads with a valid barcode or sample identifier.
	 *
	 * @return Map sampleID to list of valid headers
	 * @throws Exception if error occur
	 */
	protected Map<String, Set<String>> getValidFwHeaders() throws Exception {
		final Map<String, Set<String>> validHeaders = new HashMap<>();
		final boolean isPaird = SeqUtil.hasPairedReads();
		final boolean isCombined = isPaird && getInputFiles().size() == 1;

		Log.info( getClass(), "Get FW Headers from temp files " +
			( isCombined ? "created from multiplexed file with both forward and reverse reads": "" ) );

		// check forward reads only
		for( final File file: getSplitDir().listFiles() ) {
			// skip files ending with _R2 suffix since contain only reverse reads
			if( !isCombined && !SeqUtil.isForwardRead( file.getName() ) ) continue;

			Log.info( getClass(), "Processing split file for FW headers: " + file.getAbsolutePath() );

			final List<String> seqLines = new ArrayList<>();
			final BufferedReader reader = BioLockJUtil.getFileReader( file );
			try {
				for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
					seqLines.add( line );
					if( seqLines.size() == SeqUtil.getNumLinesPerRead() ) {
						// if not combined must be a file of only forward reads due to continue above
						if( !isCombined || seqLines.get( 0 ).contains( SeqUtil.ILLUMINA_FW_READ_IND ) ) {
							this.numTotalFwReads++;
							final String sampleId = DemuxUtil.getSampleId( seqLines );
							if( sampleId != null ) {
								if( validHeaders.get( sampleId ) == null )
									validHeaders.put( sampleId, new HashSet<>() );
								final String header = SeqUtil.getHeader( seqLines.get( 0 ) );
								validHeaders.get( sampleId ).add( header );
							}
						}

						seqLines.clear();
					}
				}
			} finally {
				if( reader != null ) reader.close();
			}

		}

		String msg = " # valid reads = ";
		if( SeqUtil.hasPairedReads() ) msg = " # valid fw read headers = ";

		for( final String sampleId: validHeaders.keySet() )
			Log.debug( getClass(), sampleId + msg + validHeaders.get( sampleId ).size() );

		return validHeaders;
	}

	/**
	 * This method obtains all valid headers for the forward reads, and returns only headers that also have a matching
	 * reverse read
	 *
	 * @return Map of valid headers for each sample ID
	 * @throws Exception if unable to obtain headers
	 */
	protected Map<String, Set<String>> getValidHeaders() throws Exception {
		final Map<String, Set<String>> validFwHeaders = getValidFwHeaders();
		if( !SeqUtil.hasPairedReads() ) {
			Log.info( getClass(), "Demultiplexing unpaired reads...# " + validFwHeaders.size() );
			return validFwHeaders;
		}

		final Map<String, Set<String>> validHeaders = new HashMap<>();
		final boolean isCombined = getInputFiles().size() == 1;

		// check reverse reads only
		for( final File file: getSplitDir().listFiles() ) {
			// skip files ending with _R1 suffix since contain only forward reads
			if( !isCombined && SeqUtil.isForwardRead( file.getName() ) ) continue;

			Log.info( getClass(), "Processing split file for RV headers: " + file.getAbsolutePath() );

			final List<String> seqLines = new ArrayList<>();
			final BufferedReader reader = BioLockJUtil.getFileReader( file );
			try {
				for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
					seqLines.add( line );
					if( seqLines.size() == SeqUtil.getNumLinesPerRead() ) {
						// if not combined must be a file of only reverse reads due to continue above
						if( !isForwardRead( file.getName(), seqLines.get( 0 ) ) ) {
							final String header = SeqUtil.getHeader( seqLines.get( 0 ) );
							this.numTotalRvReads++;

							for( final String sampleId: validFwHeaders.keySet() )
								if( validFwHeaders.get( sampleId ).contains( header ) ) {
									if( validHeaders.get( sampleId ) == null )
										validHeaders.put( sampleId, new HashSet<>() );

									validHeaders.get( sampleId ).add( header );
									break;
								}
						}

						seqLines.clear();
					}
				}
			} finally {
				reader.close();
			}
		}

		for( final String sampleId: validHeaders.keySet() )
			Log.info( getClass(), sampleId + " # valid headers = " + validHeaders.get( sampleId ).size() );

		return validHeaders;
	}

	private void buildSummaryAndSetConfig( final File file, final long numReads, final long headerFwBarcodes,
		final long seqFwBarcodes, final long headerRvBarcodes, final long seqRvBarcodes ) throws Exception {
		this.summary += "Compre #BC (BarCode) vs. #rcBC (Reverse Compliment BC) in Headers vs. Sequences" + RETURN;
		this.summary += "Test Sequence File: " + file.getAbsolutePath() + RETURN;
		this.summary += "# Total Reads:              " + numReads + RETURN;
		this.summary += "#BC Headers:          " + headerFwBarcodes + RETURN;
		this.summary += "#rcBC Headers: " + headerRvBarcodes + RETURN;
		this.summary += "#BC Sequences: " + seqFwBarcodes + RETURN;
		this.summary += "#rcBC Sequences: " + seqRvBarcodes + RETURN;

		final boolean useRevComp =
			useRevCompBarcodes( headerFwBarcodes + seqFwBarcodes, headerRvBarcodes + seqRvBarcodes );
		final boolean useSeqBarcodes =
			useSeqBarcodes( headerFwBarcodes + headerRvBarcodes, seqFwBarcodes + seqRvBarcodes );
		final long numBarcodes = useRevComp ? useSeqBarcodes ? seqRvBarcodes: headerRvBarcodes:
			useSeqBarcodes ? seqFwBarcodes: headerFwBarcodes;
		final long numHeaderBarcodes = headerFwBarcodes + headerRvBarcodes;
		final long numSeqBarcodes = seqFwBarcodes + seqRvBarcodes;
		final long numFwBarcodes = headerFwBarcodes + seqFwBarcodes;
		final long numRvBarcodes = headerRvBarcodes + seqRvBarcodes;

		final String rc = Config.getString( this, DemuxUtil.BARCODE_USE_REV_COMP );
		final String st = Config.getString( this, DemuxUtil.DEMUX_STRATEGY );
		final String strategy = "Config." + DemuxUtil.DEMUX_STRATEGY + "=" + ( st == null ? "UNDEFINED": st );
		final String revCompAssign =
			"Config." + DemuxUtil.BARCODE_USE_REV_COMP + "=" + ( rc == null ? "UNDEFINED": rc );

		if( numBarcodes == 0 ) {
			if( strategyConfigSet() && !DemuxUtil.sampleIdInHeader() ) throw new Exception(
				strategy + " however no baracodes or Sample IDs found in test file: " + file.getAbsolutePath() );
			else if( !strategyConfigSet() ) {
				this.summary += "Set: " + DemuxUtil.DEMUX_STRATEGY + "=" + DemuxUtil.OPTION_ID_IN_HEADER +
					" based on #BC & #rcBC counts" + RETURN;
				Config.setConfigProperty( DemuxUtil.DEMUX_STRATEGY, DemuxUtil.OPTION_ID_IN_HEADER );
			}
		} else if( strategyConfigSet() ) {
			if( DemuxUtil.barcodeInHeader() && useSeqBarcodes ) this.summary += "WARNING: [" + strategy +
				"]  --> but [" + numSeqBarcodes + "] sequence BC > [" + numHeaderBarcodes + "] header BC in";
			else if( DemuxUtil.barcodeInSeq() && !useSeqBarcodes ) this.summary += "WARNING: [" + strategy +
				"] --> but [" + numHeaderBarcodes + "] header BC > [" + numSeqBarcodes + "] sequence BC";
			else if( numHeaderBarcodes == numSeqBarcodes ) this.summary += "WARNING: [" + numHeaderBarcodes +
				"] header BC = [" + numSeqBarcodes +
				"] sequence BC --> TrimPrimers Module can be used to remove sequence BC if located at very start of sequence.";
		} else if( useSeqBarcodes ) {
			this.summary += "Set: " + DemuxUtil.DEMUX_STRATEGY + "=" + DemuxUtil.OPTION_BARCODE_IN_SEQ +
				" based on BC counts" + RETURN;
			Config.setConfigProperty( DemuxUtil.DEMUX_STRATEGY, DemuxUtil.OPTION_BARCODE_IN_SEQ );
		} else {
			this.summary += "Set: " + DemuxUtil.DEMUX_STRATEGY + "=" + DemuxUtil.OPTION_BARCODE_IN_HEADER +
				" based on BC counts" + RETURN;
			Config.setConfigProperty( DemuxUtil.DEMUX_STRATEGY, DemuxUtil.OPTION_BARCODE_IN_HEADER );

		}

		if( useRevCompConfigSet() ) {
			if( Config.getBoolean( this, DemuxUtil.BARCODE_USE_REV_COMP ) && !useRevComp )
				this.summary += "WARNING: [" + revCompAssign + "] --> but [" + numFwBarcodes + "] BC > [" +
					numRvBarcodes + "] rcBC" + RETURN;
		} else {
			final Double cutoff = getBarcodeCutoff();
			if( cutoff != null ) {
				final Long cutoffNumReads = new Long( Math.round( cutoff * numReads ) );
				if( numBarcodes < cutoffNumReads ) {
					final String displayCutoff = new Long( Math.round( cutoff * 100 ) ).toString() + "%";
					throw new Exception(
						"Total # barcodes: " + numBarcodes + " < cutoff [ " + displayCutoff + " of reads = " +
							cutoffNumReads + " ]" + RETURN + " Review Summary -- > " + RETURN + this.summary );
				}
			}
			final String val = useRevComp ? Constants.TRUE: Constants.FALSE;
			this.summary +=
				"Set: " + DemuxUtil.BARCODE_USE_REV_COMP + "=" + val + " based on #BC & #rcBC counts" + RETURN;
			Config.setConfigProperty( DemuxUtil.BARCODE_USE_REV_COMP, val );
		}
	}

	private Double getBarcodeCutoff() throws Exception {
		final Double val = Config.getPositiveDoubleVal( this, DemuxUtil.BARCODE_CUTOFF );
		if( val != null && val > 1 )
			throw new ConfigFormatException( DemuxUtil.BARCODE_CUTOFF, "Must be between 0.0 - 1.0" );
		return val;
	}

	private String getFileSuffix( final String name, final String header ) throws Exception {
		String suffix = "";
		if( SeqUtil.hasPairedReads() )
			suffix = isForwardRead( name, header ) ? Config.requireString( this, Constants.INPUT_FORWARD_READ_SUFFIX ):
				Config.requireString( this, Constants.INPUT_REVERSE_READ_SUFFIX );

		return suffix + "." + ( SeqUtil.isFastA() ? Constants.FASTA: Constants.FASTQ );
	}

	private String getNoMatchFileName( final String fileName, final String header ) throws Exception {
		return getTempDir().getAbsolutePath() + File.separator + "NO_MATCH" + getFileSuffix( fileName, header );

	}

	private String getOutputFileName( final String sampleId, final String fileName, final String header )
		throws Exception {
		return getOutputDir().getAbsolutePath() + File.separator + sampleId + getFileSuffix( fileName, header );
	}

	private File getSplitDir() {
		return ModuleUtil.requireSubDir( this, getTempDir().getName() + File.separator + "split" );
	}

	private String getSplitFileName( final String fileName, final int i ) throws Exception {
		String suffix = "";
		if( getInputFiles().size() > 1 ) suffix = SeqUtil.getReadDirectionSuffix( fileName );

		suffix += "." + ( SeqUtil.isFastA() ? Constants.FASTA: Constants.FASTQ );

		return getSplitDir().getAbsolutePath() + File.separator + "split_" + i + suffix;
	}

	private int hasBarcode( final String line ) throws Exception {
		for( final String code: MetaUtil.getFieldValues( Config.requireString( this, MetaUtil.META_BARCODE_COLUMN ),
			true ) )
			if( line.contains( code ) ) return 1;
			else if( line.contains( SeqUtil.reverseComplement( code ) ) ) return 2;
		return 0;
	}

	private void incrementCounts( final String name, final String header ) throws Exception {
		if( isForwardRead( name, header ) ) this.numValidFwReads++;
		else this.numValidRvReads++;
	}

	private boolean isForwardRead( final String name, final String header ) throws Exception {
		if( !SeqUtil.hasPairedReads() ) return true;

		final boolean isCombined = getInputFiles().size() == 1;
		if( isCombined ) if( header.contains( SeqUtil.ILLUMINA_FW_READ_IND ) ) return true;
		else if( header.contains( SeqUtil.ILLUMINA_RV_READ_IND ) ) return false;
		else throw new Exception(
			"Sequence header in " + name + " does not indicate forward[" + SeqUtil.ILLUMINA_FW_READ_IND +
				"] or reverse[" + SeqUtil.ILLUMINA_RV_READ_IND + "] read for header = " + header );

		return SeqUtil.isForwardRead( name );
	}

	private void printCounts( final Map<String, Set<String>> validHeaders ) {
		long size = 0L;
		for( final String sampleId: validHeaders.keySet() )
			size += validHeaders.get( sampleId ).size();

		Log.info( getClass(), "Total fw reads = " + this.numTotalFwReads );
		Log.info( getClass(), "Total rv reads = " + this.numTotalRvReads );
		Log.info( getClass(), "Number valid reads = " + size );
	}

	private boolean strategyConfigSet() {
		return Config.getString( this, DemuxUtil.DEMUX_STRATEGY ) != null;
	}

	private boolean useRevCompBarcodes( final long numBarcodes, final long numReverseComplimentBarcodes )
		throws Exception {
		if( useRevCompConfigSet() ) return Config.getBoolean( this, DemuxUtil.BARCODE_USE_REV_COMP );
		return numReverseComplimentBarcodes > numBarcodes;
	}

	private boolean useRevCompConfigSet() {
		return Config.getString( this, DemuxUtil.BARCODE_USE_REV_COMP ) != null;
	}

	private boolean useSeqBarcodes( final long headerBarcodes, final long seqBarcodes ) throws Exception {
		if( strategyConfigSet() ) return DemuxUtil.barcodeInSeq();
		return seqBarcodes > headerBarcodes;
	}

	private static String getSampleId( final String header, final Map<String, Set<String>> validHeaders ) {
		for( final String sampleId: validHeaders.keySet() )
			if( validHeaders.get( sampleId ).contains( header ) ) return sampleId;
		return null;
	}

	private static void writeSample( final List<String> lines, final String fileName ) throws Exception {
		final File outFile = new File( fileName );
		final boolean exists = outFile.isFile();
		final BufferedWriter writer = new BufferedWriter( new FileWriter( outFile, exists ) );
		for( final String line: lines )
			writer.write( line + RETURN );
		writer.close();
	}

	private long numTotalFwReads = 0L;
	private long numTotalRvReads = 0L;

	private long numValidFwReads = 0L;
	private long numValidRvReads = 0L;

	private String summary = "";

	/**
	 * Module splits multiplexed file into smaller files with this number of lines: {@value #NUM_LINES_TEMP_FILE}
	 */
	protected static final int NUM_LINES_TEMP_FILE = 2000000;

	@Override
	public String getDescription() {
		return "Demultiplex samples into separate files for each sample.";
	}
	
	@Override
	public String getDetails() throws API_Exception {
		return BuildDocs.copyFromModuleResource( this, "DemultiplexerDetails.md" );
	}

	@Override
	public String getCitationString() {
		return "Module developed by Mike Sioda" + System.lineSeparator() + "BioLockJ " + BioLockJUtil.getVersion();
	}
}
