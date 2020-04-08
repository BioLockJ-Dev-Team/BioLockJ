/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date March 8, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.seq;

import java.io.*;
import java.util.*;
import biolockj.Config;
import biolockj.Log;
import biolockj.Properties;
import biolockj.api.ApiModule;
import biolockj.exception.ConfigViolationException;
import biolockj.exception.SequnceFormatException;
import biolockj.module.CountsReads;
import biolockj.module.JavaModuleImpl;
import biolockj.module.SeqModule;
import biolockj.module.implicit.RegisterNumReads;
import biolockj.util.*;

/**
 * This BioModule validates fasta/fastq file formats are valid and enforces min/max read lengths.
 * 
 * @blj.web_desc Sequence File Validator
 */
public class SeqFileValidator extends JavaModuleImpl implements SeqModule, ApiModule, CountsReads {
	
	public SeqFileValidator() {
		super();
		addNewProperty( INPUT_SEQ_MAX, Properties.INTEGER_TYPE, "maximum number of bases per read" );
		addNewProperty( INPUT_SEQ_MIN, Properties.INTEGER_TYPE, "minimum number of bases per read" );
		addNewProperty( REQUIRE_EUQL_NUM_PAIRS, Properties.BOOLEAN_TYPE, "Options: Y/N; require number of forward and reverse reads" );
	}

	/**
	 * Set {@value #NUM_VALID_READS} as the number of reads field.
	 */
	@Override
	public void cleanUp() throws Exception {
		super.cleanUp();
		RegisterNumReads.setNumReadFieldName( getMetaColName() );
	}

	@Override
	public List<File> getSeqFiles( final Collection<File> files ) throws SequnceFormatException {
		return SeqUtil.getSeqFiles( files );
	}

	/**
	 * Produce a summary message with counts on total number of reads and number of valid reads containing a barcode
	 * defined in the metadata file.
	 */
	@Override
	public String getSummary() throws Exception {
		final StringBuffer sb = new StringBuffer();
		try {
			long combinedMeanFwReadLen = 0L;
			long combinedMeanRvReadLen = 0L;
			long overallMinReadLen = 0;
			long overallMaxReadLen = 0;

			sb.append( getSeqLenRange() + RETURN );

			if( !this.badFiles.isEmpty() ) sb.append( "Removed " + this.badFiles.size() + " empty files" + RETURN );

			final TreeSet<String> invalidReads = new TreeSet<>();
			final TreeSet<String> shortReads = new TreeSet<>();
			final TreeSet<String> longReads = new TreeSet<>();
			for( final String sampleId: new TreeSet<>( this.sampleStats.keySet() ) ) {
				final Long[] stats = this.sampleStats.get( sampleId );
				combinedMeanFwReadLen += stats[ INDEX_AVG_FW_READ_LEN ];
				combinedMeanRvReadLen += stats[ INDEX_AVG_RV_READ_LEN ];
				Log.debug( getClass(), "combinedMeanFwReadLen=" + combinedMeanFwReadLen );
				Log.debug( getClass(), "combinedMeanRvReadLen=" + combinedMeanRvReadLen );
				if( stats[ INDEX_MIN_READS ] == 0 || overallMinReadLen == 0 )
					overallMinReadLen = Math.max( overallMinReadLen, stats[ INDEX_MIN_READS ] );
				else overallMinReadLen = Math.min( overallMinReadLen, stats[ INDEX_MIN_READS ] );

				overallMaxReadLen = Math.max( overallMaxReadLen, stats[ INDEX_MAX_READS ] );

				if( stats[ INDEX_NUM_READS_INVALID_FORMAT ] > 0 )
					invalidReads.add( sampleId + ":" + stats[ INDEX_NUM_READS_INVALID_FORMAT ] );
				if( stats[ INDEX_NUM_READS_TOO_SHORT ] > 0 )
					shortReads.add( sampleId + ":" + stats[ INDEX_NUM_READS_TOO_SHORT ] );
				if( stats[ INDEX_NUM_TRIMMED_READS ] > 0 )
					longReads.add( sampleId + ":" + stats[ INDEX_NUM_TRIMMED_READS ] );
			}

			sb.append( getSeqModSummary( invalidReads, shortReads, longReads, combinedMeanFwReadLen,
				combinedMeanRvReadLen, overallMinReadLen, overallMaxReadLen ) );
			freeMemory();
		} catch( final Exception ex ) {
			final String msg = "Unable to complete module summary: " + ex.getMessage();
			sb.append( RETURN + msg );
			Log.warn( getClass(), msg );
		}

		return super.getSummary() + sb.toString();
	}

	/**
	 * Cache sampleIds to compare to validated sampleIds post-processing. Call {@link #validateFile(File, Integer)} for
	 * each input file.<br>
	 * Call {@link #removeBadFiles()} to remove empty files (cases where all reads fail validation).<br>
	 * Call {@link #verifyPairedSeqs()} if module input files are paired read files.<br>
	 * Call {@link biolockj.util.MetaUtil#addColumn(String, Map, File, boolean)}
	 */
	@Override
	public void runModule() throws Exception {
		this.sampleIds.addAll( MetaUtil.getSampleIds() );
		int count = 0;
		for( final File file: getInputFiles() )
			validateFile( file, count++ );

		removeBadFiles();

		if( SeqUtil.hasPairedReads() ) verifyPairedSeqs();

		MetaUtil.addColumn( getMetaColName(), this.readsPerSample, getOutputDir(), true );
	}

	/**
	 * Remove sequence files in which all reads failed validation checks, leaving only an empty file.
	 */
	protected void removeBadFiles() {
		if( !this.badFiles.isEmpty() ) for( final File file: this.badFiles )
			if( file.delete() ) Log.warn( BioLockJUtil.class, "Deleted empty file: " + file.getAbsolutePath() );
	}

	/**
	 * Validate sequence files:
	 * <ol>
	 * <li>Validate valid 1st sequence header character is expected character
	 * <li>Validate fastq files have same number of bases and quality scores per read
	 * <li>Remove reads below minimum threshold: {@value #INPUT_SEQ_MIN}
	 * <li>Trim reads if above the maximum threshold: {@value #INPUT_SEQ_MAX}
	 * </ol>
	 * Invalid reads are saved to a file in the module temp directory for analysis/review.
	 *
	 * @param file Sequence file
	 * @param fileCount Integer count
	 * @throws Exception if I/O errors occur while processing sequence files
	 */
	protected void validateFile( final File file, final Integer fileCount ) throws Exception {
		Log.info( getClass(), "Validate File [" + fileCount + "]: " + file.getAbsolutePath() );
		final Long[] stats = initStats();
		long combinedReadLen = 0;
		int seqNum = 0;

		final List<String> seqLines = new ArrayList<>();
		final List<String> badLines = new ArrayList<>();
		final File outputFile = new File( getFileName( getOutputDir(), file.getName() ) );
		final BufferedReader reader = BioLockJUtil.getFileReader( file );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( outputFile ) );
		try {
			String line = null;

			try {
				line = SeqUtil.scanFirstLine( reader, file );
			} catch( final Exception ex ) {
				this.badFiles.add( outputFile );
				Log.debug( getClass(), "Error scanning input file: " + ex.getMessage() );
				return;
			}

			do {
				seqLines.add( line.trim() );
				if( seqLines.size() == SeqUtil.getNumLinesPerRead() ) {
					seqNum++;
					final int headerLen = seqLines.get( 0 ).length();
					final int seqLen = seqLines.get( 1 ).length();
					String headerChar = "";
					if( headerLen == 0 ) Log.warn( getClass(), "Sequence #" + seqNum +
						" has an empty header & seq len = " + seqLen + " in ---> " + file.getAbsolutePath() );
					else headerChar = seqLines.get( 0 ).substring( 0, 1 );

					if( !SeqUtil.getSeqHeaderChars().contains( headerChar ) ) {
						stats[ INDEX_NUM_READS_INVALID_FORMAT ]++;
						badLines.addAll( seqLines );
						Log.warn( getClass(),
							"Sequence #" + seqNum + " format invalid.  Must begin with a valid header char (" +
								SeqUtil.getSeqHeaderChars() + ")  --> header line = " + seqLines.get( 0 ) );
					} else if( seqLen < minReadLen() ) {
						stats[ INDEX_NUM_READS_TOO_SHORT ]++;
						badLines.addAll( seqLines );
						Log.warn( getClass(),
							"Sequence #" + seqNum + " format invalid.  Must have a minimum number of bases (" +
								minReadLen() + ")  --> \n" + seqLines.get( 0 ) + "\n" + seqLines.get( 1 ) );
					} else if( SeqUtil.isFastQ() && seqLen != seqLines.get( 3 ).length() ) {
						stats[ INDEX_NUM_READS_INVALID_FORMAT ]++;
						badLines.addAll( seqLines );
						Log.warn( getClass(), "Sequence #" + seqNum + " fastq format invalid.  Must have equal " +
							" number of bases and quality scores: " + seqLines.get( 0 ) );
					} else {
						stats[ INDEX_NUM_VALID_READS ]++;
						setMaxSeq( SeqUtil.getSampleId( file.getName() ), seqLen );
						final Integer seqMax = Config.getPositiveInteger( this, INPUT_SEQ_MAX );
						if( seqMax != null && seqMax > 0 && seqLen > seqMax ) {
							stats[ INDEX_NUM_TRIMMED_READS ]++;
							seqLines.set( 1, seqLines.get( 1 ).substring( 0, seqMax ) );
							if( SeqUtil.isFastQ() ) seqLines.set( 3, seqLines.get( 3 ).substring( 0, seqMax ) );
						}

						final long readLen = seqLines.get( 1 ).length();
						combinedReadLen += readLen;

						if( readLen > 0 && stats[ INDEX_MIN_READS ] == 0 || readLen < stats[ INDEX_MIN_READS ] )
							stats[ INDEX_MIN_READS ] = readLen;
						if( readLen > stats[ INDEX_MAX_READS ] ) stats[ INDEX_MAX_READS ] = readLen;

						for( final String seqLine: seqLines )
							writer.write( seqLine + RETURN );
					}

					seqLines.clear();
				}

				line = reader.readLine();
			} while( line != null );
		} finally {
			writer.close();
			if( reader != null ) reader.close();
		}
		Log.info( BioLockJUtil.class, "Output file: " + outputFile.getAbsolutePath() );

		if( stats[ INDEX_NUM_VALID_READS ] == 0 ) this.badFiles.add( file );

		if( !this.badFiles.contains( file ) ) {
			saveRemovedSeqsToFile( badLines, file );
			populateSampleStats( stats, file, combinedReadLen );
		}
	}

	/**
	 * Verify equal number of forward and reverse read files.<br>
	 * if {@value #REQUIRE_EUQL_NUM_PAIRS}={@value biolockj.Constants#TRUE}, verify forward and reverse read files have
	 * an equal number of reads.
	 *
	 * @throws Exception if validations fail or errors occur
	 */
	protected void verifyPairedSeqs() throws Exception {
		if( !this.rvReadsPerSample.isEmpty() ) {
			Log.info( getClass(), "Validing paired reads..." );
			final Set<String> keys = new TreeSet<>( this.readsPerSample.keySet() );
			keys.removeAll( this.rvReadsPerSample.keySet() );
			final Set<String> keys2 = new TreeSet<>( this.rvReadsPerSample.keySet() );
			keys2.removeAll( this.readsPerSample.keySet() );

			if( !keys.isEmpty() && !keys2.isEmpty() )
				throw new Exception( "Unpaired forward reads: " + keys + " & " + "Unpaired reverse reads: " + keys2 );
			else if( !keys.isEmpty() ) throw new Exception( "Unpaired forward reads: " + keys );
			else if( !keys2.isEmpty() ) throw new Exception( "Unpaired reverse reads: " + keys2 );

			if( Config.getBoolean( this, REQUIRE_EUQL_NUM_PAIRS ) ) {
				final Set<String> unequalNumReads = new TreeSet<>();
				for( final String sampleId: this.rvReadsPerSample.keySet() ) {
					final String numFwReads = this.readsPerSample.get( sampleId );
					final String numRvReads = this.rvReadsPerSample.get( sampleId );
					if( !Long.valueOf( numFwReads ).equals( Long.valueOf( numRvReads ) ) ) {
						Log.warn( getClass(), sampleId + " has unequal read count FW=" +
							this.readsPerSample.get( sampleId ) + "; RV=" + this.rvReadsPerSample.get( sampleId ) );
						unequalNumReads.add( sampleId );
					}
				}

				if( !unequalNumReads.isEmpty() ) throw new ConfigViolationException( REQUIRE_EUQL_NUM_PAIRS,
					"Paired reads require an equal number of reads: " + unequalNumReads );
			}
		}
	}

	/**
	 * Free up memory.
	 */
	private void freeMemory() {
		this.readsPerSample = null;
		this.rvReadsPerSample = null;
		this.sampleIds = null;
		this.readsPerSample = null;
		this.sampleStats = null;
		this.badFiles = null;
		this.sampleStats = null;
	}

	private String getMetaColName() throws Exception {
		if( this.otuColName == null ) this.otuColName = MetaUtil.getSystemMetaCol( this, NUM_VALID_READS );
		return this.otuColName;
	}

	private String getSeqLenRange() throws Exception {
		final Integer max = Config.getPositiveInteger( this, INPUT_SEQ_MAX );
		return "Valid SEQ Len Range --> min( " + minReadLen() + " ) - max( " + ( max == null ? "UNLIMITED": max ) +
			" )";
	}

	private String getSeqModSummary( final TreeSet<String> invalidReads, final TreeSet<String> shortReads,
		final TreeSet<String> longReads, final long totalAvgFwLen, final long totalAvgRvLen, final long minReadLen,
		final long maxReadLen ) throws Exception {
		final String label = "Valid Reads";
		final int pad = SummaryUtil.getPad( label );
		if( this.sampleStats.size() == 0 ) throw new Exception( "Summary stats not found!" );

		final StringBuffer sb = new StringBuffer();
		final long avgReadLen = Double.valueOf( totalAvgFwLen / this.sampleStats.size() ).longValue();

		if( totalAvgRvLen > 0 ) {
			final long avgRvReadLen = Double.valueOf( totalAvgRvLen / this.sampleStats.size() ).longValue();
			sb.append( BioLockJUtil.addTrailingSpaces( "Mean valid FW read len:", pad ) + avgReadLen + RETURN );
			sb.append( BioLockJUtil.addTrailingSpaces( "Mean valid RV read len:", pad ) + avgRvReadLen + RETURN );
		} else sb.append( BioLockJUtil.addTrailingSpaces( "Mean valid read len:", pad ) + avgReadLen + RETURN );

		sb.append( BioLockJUtil.addTrailingSpaces( "Min valid read len:", pad ) + minReadLen + RETURN );
		sb.append( BioLockJUtil.addTrailingSpaces( "Max valid read len:", pad ) + maxReadLen + RETURN );

		if( invalidReads.isEmpty() && shortReads.isEmpty() && longReads.isEmpty() )
			sb.append( "SEQ files pass all validations - no sequences were modified or removed." + RETURN );
		else if( !invalidReads.isEmpty() || !shortReads.isEmpty() ) {
			if( !invalidReads.isEmpty() ) sb
				.append( BioLockJUtil.addTrailingSpaces( "Removed invalid reads from:", pad ) + invalidReads + RETURN );

			if( !shortReads.isEmpty() )
				sb.append( BioLockJUtil.addTrailingSpaces( "Removed short reads from:", pad ) + shortReads + RETURN );
			sb.append( BioLockJUtil.addTrailingSpaces( "Discarded reads stored in:", pad ) +
				getTempDir().getAbsolutePath() + RETURN );

			if( !longReads.isEmpty() )
				sb.append( BioLockJUtil.addTrailingSpaces( "Trimmed long reads from:", pad ) + shortReads + RETURN );

			if( !this.maxSeqFound.isEmpty() ) {
				final long max = this.maxSeqFound.keySet().iterator().next();
				final TreeSet<String> ids = new TreeSet<>( this.maxSeqFound.values().iterator().next() );
				sb.append( BioLockJUtil.addTrailingSpaces( "IDs w/ ORIGINAL max read len [" + max + "]:", pad ) + ids +
					RETURN );
			}
		}

		String summary = SummaryUtil.getCountSummary( this.readsPerSample, label, true ) + sb.toString();
		this.sampleIds.removeAll( this.readsPerSample.keySet() );
		if( !this.sampleIds.isEmpty() )
			summary += BioLockJUtil.addTrailingSpaces( "Removed empty metadata records:", pad ) + this.sampleIds;
		return summary;
	}

	private int minReadLen() throws Exception {
		final Integer seqMin = Config.getPositiveInteger( this, INPUT_SEQ_MIN );
		if( seqMin == null ) return 1;
		return seqMin;
	}

	private void populateSampleStats( final Long[] stats, final File file, final long combinedReadLen )
		throws Exception {

		final String id = SeqUtil.getSampleId( file.getName() );
		setNumReads( file, stats );
		final Long numValidReads = stats[ INDEX_NUM_VALID_READS ];
		if( numValidReads == null || numValidReads == 0L ) return;
		final long len = numValidReads > 0 ? Double.valueOf( combinedReadLen / numValidReads ).longValue(): 0;
		if( SeqUtil.isForwardRead( file.getName() ) ) {
			Log.debug( getClass(), "Average FW seq length = " + combinedReadLen + " / " + numValidReads + " = " + len );
			stats[ INDEX_AVG_FW_READ_LEN ] = len;
		} else {
			Log.debug( getClass(), "Average RV seq length = " + combinedReadLen + " / " + numValidReads + " = " + len );
			stats[ INDEX_AVG_RV_READ_LEN ] = len;
		}

		final Long[] otherStats = this.sampleStats.get( id );
		if( otherStats != null && otherStats[ INDEX_NUM_VALID_READS ] != null &&
			otherStats[ INDEX_NUM_VALID_READS ] != 0L ) {
			Log.debug( getClass(), "Merging paired read stats for: " + file.getName() );
			stats[ INDEX_NUM_VALID_READS ] = otherStats[ INDEX_NUM_VALID_READS ] + numValidReads;
			stats[ INDEX_NUM_TRIMMED_READS ] = otherStats[ INDEX_NUM_TRIMMED_READS ] + stats[ INDEX_NUM_TRIMMED_READS ];
			stats[ INDEX_NUM_READS_TOO_SHORT ] =
				otherStats[ INDEX_NUM_READS_TOO_SHORT ] + stats[ INDEX_NUM_READS_TOO_SHORT ];
			stats[ INDEX_NUM_READS_INVALID_FORMAT ] =
				otherStats[ INDEX_NUM_READS_INVALID_FORMAT ] + stats[ INDEX_NUM_READS_INVALID_FORMAT ];
			stats[ INDEX_MIN_READS ] = Math.min( otherStats[ INDEX_MIN_READS ], stats[ INDEX_MIN_READS ] );
			stats[ INDEX_MAX_READS ] = Math.max( otherStats[ INDEX_MAX_READS ], stats[ INDEX_MAX_READS ] );

			if( SeqUtil.isForwardRead( file.getName() ) ) {
				Log.debug( getClass(), "Local average RV seq length = " + stats[ INDEX_AVG_RV_READ_LEN ] );
				stats[ INDEX_AVG_RV_READ_LEN ] = otherStats[ INDEX_AVG_RV_READ_LEN ];
				Log.debug( getClass(), "Updated RV seq length = " + stats[ INDEX_AVG_RV_READ_LEN ] );
			} else {
				Log.debug( getClass(), "Local average FW seq length = " + stats[ INDEX_AVG_FW_READ_LEN ] );
				stats[ INDEX_AVG_FW_READ_LEN ] = otherStats[ INDEX_AVG_FW_READ_LEN ];
				Log.debug( getClass(), "Updated FW seq length = " + stats[ INDEX_AVG_FW_READ_LEN ] );
			}
		}
		this.sampleStats.put( SeqUtil.getSampleId( file.getName() ), stats );
	}

	private void saveRemovedSeqsToFile( final Collection<String> badLines, final File file ) throws Exception {
		if( !badLines.isEmpty() ) {
			final File tempFile = new File( getFileName( getTempDir(), "INVALID_READS_" + file.getName() ) );
			Log.warn( getClass(), "Extracting invalid reads to --> " + tempFile.getAbsolutePath() );
			final BufferedWriter invalidWriter = new BufferedWriter( new FileWriter( tempFile ) );
			try {
				for( final String seqLine: badLines )
					invalidWriter.write( seqLine + RETURN );
			} finally {
				invalidWriter.close();
			}
		}
	}

	private void setMaxSeq( final String sampleId, final long seqLen ) {
		final TreeSet<String> ids = new TreeSet<>();
		ids.add( sampleId );
		if( this.maxSeqFound.isEmpty() ) this.maxSeqFound.put( seqLen, ids );
		else {
			final long currentMaxLen = this.maxSeqFound.keySet().iterator().next();
			final long newMaxLen = Math.max( seqLen, currentMaxLen );
			if( newMaxLen == currentMaxLen ) {
				ids.addAll( this.maxSeqFound.values().iterator().next() );
				this.maxSeqFound.put( newMaxLen, ids );
			} else if( newMaxLen > currentMaxLen ) this.maxSeqFound.put( newMaxLen, ids );
		}
	}

	private void setNumReads( final File file, final Long[] stats ) throws Exception {
		if( SeqUtil.isForwardRead( file.getName() ) ) this.readsPerSample.put( SeqUtil.getSampleId( file.getName() ),
			String.valueOf( stats[ INDEX_NUM_VALID_READS ] ) );
		else this.rvReadsPerSample.put( SeqUtil.getSampleId( file.getName() ),
			String.valueOf( stats[ INDEX_NUM_VALID_READS ] ) );

	}

	private static String getFileName( final File dir, final String name ) {
		final String prefix = dir.getAbsolutePath() + File.separator;
		if( SeqUtil.isGzipped( name ) ) return prefix + name.substring( 0, name.length() - 3 );
		return prefix + name;
	}

	private static Long[] initStats() {
		final Long[] stats = new Long[ 8 ];
		for( int i = 0; i < stats.length; i++ )
			stats[ i ] = 0L;
		return stats;
	}

	private Set<File> badFiles = new HashSet<>();
	private final Map<Long, TreeSet<String>> maxSeqFound = new HashMap<>();
	private String otuColName = null;
	private Map<String, String> readsPerSample = new HashMap<>();
	private Map<String, String> rvReadsPerSample = new HashMap<>();
	private Set<String> sampleIds = new HashSet<>();
	private Map<String, Long[]> sampleStats = new HashMap<>();

	@Override
	public String getDescription() {
		return "This BioModule validates fasta/fastq file formats are valid and enforces min/max read lengths.";
	}

	@Override
	public String getCitationString() {
		return "Module developed by Mike Sioda" + System.lineSeparator() + "BioLockj " + BioLockJUtil.getVersion();
	}
	
	/**
	 * Column name that holds number of valid reads per sample: {@value #NUM_VALID_READS}
	 */
	public static final String NUM_VALID_READS = "Num_Valid_Reads";

	/**
	 * {@link biolockj.Config} Integer property {@value #INPUT_SEQ_MAX} defines the maximum number of bases per read
	 */
	protected static final String INPUT_SEQ_MAX = "seqFileValidator.seqMaxLen";

	/**
	 * {@link biolockj.Config} Integer property {@value #INPUT_SEQ_MIN} defines the minimum number of bases per read
	 */
	protected static final String INPUT_SEQ_MIN = "seqFileValidator.seqMinLen";

	/**
	 * {@link biolockj.Config} Boolean property {@value #REQUIRE_EUQL_NUM_PAIRS} determines if module requires equal
	 * number of forward and reverse reads (simple check).
	 */
	protected static final String REQUIRE_EUQL_NUM_PAIRS = "seqFileValidator.requireEqualNumPairs";

	private static final int INDEX_AVG_FW_READ_LEN = 6;
	private static final int INDEX_AVG_RV_READ_LEN = 7;
	private static final int INDEX_MAX_READS = 2;
	private static final int INDEX_MIN_READS = 1;
	private static final int INDEX_NUM_READS_INVALID_FORMAT = 4;
	private static final int INDEX_NUM_READS_TOO_SHORT = 5;
	private static final int INDEX_NUM_TRIMMED_READS = 3;
	private static final int INDEX_NUM_VALID_READS = 0;

}
