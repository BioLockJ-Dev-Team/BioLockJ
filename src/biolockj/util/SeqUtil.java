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
package biolockj.util;

import java.io.BufferedReader;
import java.io.File;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import biolockj.*;
import biolockj.exception.*;
import biolockj.module.BioModule;

/**
 * This utility helps interact with FastA and FastQ sequence files.
 */
public class SeqUtil {

	// Prevent instantiation
	private SeqUtil() {}

	/**
	 * Method counts number of reads in the given sequence file by counting the number of lines and dividing by the
	 * number of lines/sample (fasta=2, fastq=4)
	 * 
	 * @param seqFile Sequence file
	 * @return Number of reads in seqFile
	 * @throws Exception if errors occur
	 */
	public static long countNumReads( final File seqFile ) throws Exception {
		long count = 0;
		final BufferedReader r = BioLockJUtil.getFileReader( seqFile );
		for( String line = r.readLine(); line != null; line = r.readLine() )
			count++;
		r.close();

		return count / getNumLinesPerRead();
	}

	/**
	 * Extract the header from the first line of a read (parse to first space). This part of the header line will be
	 * identical on forward and reverse reads, so can be used to find matching pairs of reads.
	 * <p>
	 * To extract the header, trim to the Illumina read direction indicator, if it exists, otherwise to the 1st space.
	 * Method recognizes the headers: {@value #ILLUMINA_FW_READ_IND} and {@value #ILLUMINA_RV_READ_IND}
	 * 
	 * @param line Sequence line
	 * @return Sequence header
	 */
	public static String getHeader( final String line ) {
		final boolean hasFwDelim = line.contains( ILLUMINA_FW_READ_IND );
		final boolean hasRvDelim = line.contains( ILLUMINA_RV_READ_IND );
		final String delim = hasFwDelim ? ILLUMINA_FW_READ_IND: hasRvDelim ? ILLUMINA_RV_READ_IND: null;
		if( delim != null ) return line.trim().substring( 0, line.trim().indexOf( delim ) );
		return line.trim();

	}

	/**
	 * Return the header of each read in the sequence file.
	 * 
	 * @param seq Fasta or Fastq file
	 * @return Set of sequence headers
	 * @throws Exception if unable to parse the seq file
	 */
	public static Set<String> getHeaders( final File seq ) throws Exception {
		final Set<String> headers = new HashSet<>();
		final BufferedReader reader = BioLockJUtil.getFileReader( seq );
		int lineCounter = 1;
		try {
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
				if( lineCounter++ % getNumLinesPerRead() == 1 ) headers.add( getHeader( line ) );
		} finally {
			reader.close();
		}

		return headers;
	}

	/**
	 * Get valid headers found in both forward and reverse read.
	 * 
	 * @param fwRead Forward read sequence file
	 * @param rvRead Reverse read sequence file
	 * @return Sequence headers found in both files
	 * @throws Exception if I/O errors occur
	 */
	public static Set<String> getHeaders( final File fwRead, final File rvRead ) throws Exception {
		final Set<String> headers = getHeaders( fwRead );
		headers.retainAll( getHeaders( rvRead ) );
		return headers;
	}

	/**
	 * Return regex version of IUPAC DNA substitution bases so that only ACGT values are used. For example, if base =
	 * "Y", return "[CT]", if base = "A", return "A".<br>
	 * Function returns base in the same case as the input parameter.
	 * 
	 * @param base DNA Base
	 * @return regex version of IUPAC DNA substitution bases
	 */
	public static String getIupacBase( final String base ) {
		final String upperCaseBase = base.toUpperCase();
		if( DNA_BASE_MAP.keySet().contains( upperCaseBase ) ) {
			String regexBase = DNA_BASE_MAP.get( upperCaseBase );
			if( !base.equals( upperCaseBase ) ) regexBase = regexBase.toLowerCase();
			return regexBase;
		}
		return base;
	}

	/**
	 * Return number of lines per read (for fasta return 2, for fastq return 4)
	 * 
	 * @return 2 or 4
	 * @throws Exception if unable to determine sequence format
	 */
	public static int getNumLinesPerRead() throws Exception {
		return isFastA() ? 2: 4;
	}

	/**
	 * Return number of reads given number of lines in a sample.
	 * 
	 * @param numLines Total number of lines in sample
	 * @return Number of reads
	 * @throws Exception if number of reads cannot be determined
	 */
	public static long getNumReads( final long numLines ) throws Exception {
		return new Double( numLines / getNumLinesPerRead() ).longValue();
	}

	/**
	 * Paired reads must have a unique file suffix to identify forward and reverse reads. Parameter files read and a map
	 * with forward read file names as keys and matching reverse reads as the map return value
	 *
	 * @param files List of paired read files
	 * @return Map with key=fwRead and val=rvRead
	 * @throws ConfigViolationException if unpaired reads are found and
	 * {@link biolockj.Config}.{@value Constants#INPUT_REQUIRE_COMPLETE_PAIRS} = {@value biolockj.Constants#TRUE}
	 * @throws SequnceFormatException if input data is null or empty
	 * @throws ConfigFormatException if Boolean Config properties assigned values other that "Y" or "N"
	 * @throws MetadataException if errors occur reading SEQ columns from metadata file
	 */
	public static Map<File, File> getPairedReads( final Collection<File> files )
		throws SequnceFormatException, ConfigFormatException, ConfigViolationException, MetadataException {
		Log.debug( SeqUtil.class, "Looking for paired reads in " + ( files == null ? 0: files.size() ) + " files " );
		if( files == null || files.isEmpty() )
			throw new SequnceFormatException( "No files passed to getPairedReads( files )" );
		final Map<File, File> map = new HashMap<>();
		final Set<String> rvReads = new HashSet<>();
		final Set<File> unpairedFwReads = new HashSet<>();
		for( final File fwRead: files ) {
			final String name = fwRead.getName();
			if( !isForwardRead( name ) ) {
				rvReads.add( name );
				continue;
			}

			File rvRead = null;
			final String sampleID = getSampleId( name );
			Log.debug( SeqUtil.class,
				"Search for paired read to match forward read ( " + name + " ) with sample ID: " + sampleID );
			for( final File searchFile: files )
				if( sampleID.equals( getSampleId( searchFile.getName() ) ) && !isForwardRead( searchFile.getName() ) ) {
					Log.debug( SeqUtil.class, "Matching reverse read: " + searchFile.getName() );
					rvRead = searchFile;
					break;
				}

			if( rvRead != null ) map.put( fwRead, rvRead );
			else unpairedFwReads.add( fwRead );
		}

		for( final File f: map.values() )
			for( final String rv: rvReads )
				if( rv.equals( f.getName() ) ) {
					rvReads.remove( rv );
					break;
				}

		final String msg = ( unpairedFwReads.isEmpty() ? "":
			"Unpaired FW Reads:" + BioLockJUtil.printLongFormList( unpairedFwReads ) ) +
			( rvReads.isEmpty() ? "": "Unpaired RV Reads: " + BioLockJUtil.printLongFormList( rvReads ) );

		if( hasPairedReads() && Config.getBoolean( null, Constants.INPUT_REQUIRE_COMPLETE_PAIRS ) && !msg.isEmpty() )
			throw new ConfigViolationException( Constants.INPUT_REQUIRE_COMPLETE_PAIRS, msg );
		else if( hasPairedReads() && !msg.isEmpty() )
			Log.warn( SeqUtil.class, "Unpaired reads will be ignored because Config property [ " +
				Constants.INPUT_REQUIRE_COMPLETE_PAIRS + "=" + Constants.FALSE + " ]" + Constants.RETURN + msg );

		return map;
	}

	/**
	 * Return read direction indicator for forward or reverse read if found in the file name.
	 * 
	 * @param file Sequence file
	 * @return Forward or reverse read file suffix
	 * @throws Exception if errors occur
	 */
	public static String getReadDirectionSuffix( final File file ) throws Exception {
		return getReadDirectionSuffix( file.getName() );
	}

	/**
	 * Return read direction indicator for forward or reverse read if found in the file name.
	 * 
	 * @param fileName Sequence file name
	 * @return Forward or reverse read file suffix
	 * @throws Exception if errors occur
	 */
	public static String getReadDirectionSuffix( final String fileName ) throws Exception {
		if( hasPairedReads() ) {
			if( SeqUtil.isForwardRead( fileName ) )
				return Config.requireString( null, Constants.INPUT_FORWARD_READ_SUFFIX );
			return Config.requireString( null, Constants.INPUT_REVERSE_READ_SUFFIX );
		}

		return "";
	}

	/**
	 * Method extracts Sample ID from the name param. Possibly input is a file name so remove file extensions. If
	 * demultiplexing (RDP/Kraken support this option), input is a sequence header. If
	 * {@link biolockj.Config}.{@value biolockj.util.MetaUtil#META_FILENAME_COLUMN} is supplied, then possible return
	 * values are limited to the given samples ids, or "" if the file is not in the filename column.
	 *
	 * @param value File name or sequence header
	 * @return Sample ID
	 * @throws SequnceFormatException if sample ID failed to meet ID requirements
	 * @throws MetadataException if errors occur reading SEQ columns from metadata file
	 * @throws ConfigFormatException if Boolean Config properties have values other other "Y" or "N"
	 * 
	 */
	public static String getSampleId( final String value )
		throws SequnceFormatException, MetadataException, ConfigFormatException {
		String id = value;
		if( id.endsWith( Constants.PROCESSED ) ) return id.replace( Constants.PROCESSED, "" );

		final String fwReadSuffix = Config.getString( null, Constants.INPUT_FORWARD_READ_SUFFIX );
		final String rvReadSuffix = Config.getString( null, Constants.INPUT_REVERSE_READ_SUFFIX );
		final String fileNameCol = Config.getString( null, MetaUtil.META_FILENAME_COLUMN );

		if( !isForwardRead( id ) ) {
			final int rvIndex = value.lastIndexOf( rvReadSuffix );
			id = id.substring( 0, rvIndex ) + fwReadSuffix + id.substring( rvIndex + 3 );
		}

		if( MetaUtil.hasColumn( fileNameCol ) && !MetaUtil.getFieldValues( fileNameCol, true ).isEmpty() ) {
			final int ind = MetaUtil.getFieldValues( fileNameCol, false ).indexOf( id );
			if( ind > -1 ) return MetaUtil.getSampleIds().get( ind );
			Log.warn( SeqUtil.class, value + " not processed in pipeline - path not found in metadata column " +
				fileNameCol + " in: " + MetaUtil.getPath() );
			return null;
		}

		// trim directional suffix
		if( !isMultiplexed() && fwReadSuffix != null && id.indexOf( fwReadSuffix ) > 0 )
			id = id.substring( 0, id.lastIndexOf( fwReadSuffix ) );

		// trim files extensions: .gz | .fasta | .fastq
		if( isGzipped( id ) ) id = id.substring( 0, id.length() - 3 );
		if( id.toLowerCase().endsWith( "." + Constants.FASTA ) || id.toLowerCase().endsWith( "." + Constants.FASTQ ) )
			id = id.substring( 0, id.length() - 6 );

		// trim user defined file prefix and/or suffix patterns
		final String trimPrefix = Config.getString( null, Constants.INPUT_TRIM_PREFIX );
		final String trimSuffix = Config.getString( null, Constants.INPUT_TRIM_SUFFIX );
		if( trimPrefix != null && id.indexOf( trimPrefix ) > -1 )
			id = id.substring( trimPrefix.length() + id.indexOf( trimPrefix ) );

		if( trimSuffix != null && id.indexOf( trimSuffix ) > 0 ) id = id.substring( 0, id.indexOf( trimSuffix ) );

		if( id == null || id.isEmpty() )
			throw new SequnceFormatException( "Unable to extract a valid Sample ID from: " + value );
		return id;
	}

	/**
	 * Return only sequence files for sample IDs found in the metadata file.<br>
	 * If {@link biolockj.Config}.{@value biolockj.util.MetaUtil#META_REQUIRED} = {@value biolockj.Constants#TRUE}, an
	 * error is thrown to list the files that cannot be matched to a metadata row.
	 * 
	 * @param files List of input files
	 * @return Module input sequence files
	 * @throws SequnceFormatException if metadata is required for each sequence, but no metadata record is found for 1+
	 * SEQ files
	 */
	public static List<File> getSeqFiles( final Collection<File> files ) throws SequnceFormatException {
		final List<File> seqFiles = new ArrayList<>();
		final List<File> seqsWithoutMetaId = new ArrayList<>();
		try {
			for( final File file: files )
				try {
					if( isSeqFile( file ) && !isMultiplexed() && MetaUtil.exists() &&
						!MetaUtil.getSampleIds().contains( getSampleId( file.getName() ) ) )
						seqsWithoutMetaId.add( file );
					else seqFiles.add( file );
				} catch( final Exception ex ) {
					if( Config.getBoolean( null, MetaUtil.META_REQUIRED ) ) seqsWithoutMetaId.add( file );
					else Log.warn( SeqUtil.class,
						"Ignoring input file not found in metadata <-> Config property [ " + MetaUtil.META_REQUIRED +
							"=" + Constants.FALSE + " ]: " + file.getAbsolutePath() + " --> " + ex.getMessage() );
				}

			if( Config.getBoolean( null, MetaUtil.META_REQUIRED ) && !seqsWithoutMetaId.isEmpty() )
				throw new ConfigViolationException( MetaUtil.META_REQUIRED,
					"No metadata found for the following files: " + Constants.RETURN +
						BioLockJUtil.printLongFormList( seqsWithoutMetaId ) );
		} catch( final Exception ex ) {
			Log.error( SeqUtil.class, "Failed to identify the sequence files from the input file collection", ex );
			throw new SequnceFormatException( ex.getMessage() );
		}
		return seqFiles;
	}

	/**
	 * Get all sequence header characters for fasta and fastq files.
	 * 
	 * @return List of sequence header 1st character options
	 * @throws Exception if unable to determine sequence type
	 */
	public static final List<String> getSeqHeaderChars() throws Exception {
		if( isFastA() ) return FASTA_HEADER_DELIMS;
		else if( isFastQ() ) return Arrays.asList( FASTQ_HEADER_DELIM );
		else throw new Exception( "Sequence type undefined!" );
	}

	/**
	 * Get sequence type
	 * 
	 * @return sequence typ (fasta/fastq/NA)
	 * @throws ConfigNotFoundException if property is undefined
	 */
	public static String getSeqType() throws ConfigNotFoundException {
		return Config.requireString( null, Constants.INTERNAL_SEQ_TYPE ).toLowerCase();
	}

	/**
	 * Boolean getter for the internal Config property {@value biolockj.Constants#INTERNAL_PAIRED_READS}.
	 * 
	 * @return TRUE if sequences are paired in current state during pipeline execution.
	 */
	public static Boolean hasPairedReads() {
		try {
			return Config.getBoolean( null, Constants.INTERNAL_PAIRED_READS );
		} catch( final ConfigFormatException ex ) {
			Log.error( SeqUtil.class, "Failed to determine if input files are paired reads", ex );
			return false;
		}
	}

	/**
	 * Initialize Config params set by SeqUtil.
	 * 
	 * @throws Exception if runtime errors occur
	 */
	public static void initialize() throws Exception {
		if( piplineHasSeqInput() ) {
			initSeqParams();
			registerDemuxStatus();
			registerPairedReadStatus();

			if( mapSampleIdWithMetaFileNameCol() ) {
				final String colName = Config.requireString( null, MetaUtil.META_FILENAME_COLUMN );
				final int numVals = MetaUtil.getFieldValues( colName, true ).size();
				final int numUniqueVals = MetaUtil.getUniqueFieldValues( colName, true ).size();
				if( numVals != numUniqueVals ) throw new ConfigViolationException( colName,
					"File paths must be unique for this metadata column: " + colName + numUniqueVals +
						" unique values found in " + numVals + " non-null records." );

				DemuxUtil.clearDemuxConfig();
				Config.setConfigProperty( Constants.INPUT_TRIM_PREFIX, "" );
				Config.setConfigProperty( Constants.INPUT_TRIM_SUFFIX, "" );

			} else Config.setConfigProperty( MetaUtil.META_FILENAME_COLUMN, "" );
		} else {
			Config.setConfigProperty( MetaUtil.META_FILENAME_COLUMN, "" );
			Config.setConfigProperty( Constants.INTERNAL_SEQ_TYPE, MetaUtil.getNullValue( null ) );
		}
	}

	/**
	 * Return TRUE if input files are in FastA format.
	 *
	 * @return TRUE if input files are in FastA format
	 * @throws Exception if unable to determine sequence type
	 */
	public static boolean isFastA() throws Exception {
		return getSeqType().equals( Constants.FASTA );
	}

	/**
	 * Return TRUE if input files are in FastQ format.
	 *
	 * @return TRUE if input files are in FastQ format
	 * @throws Exception if unable to determine sequence type
	 */
	public static boolean isFastQ() throws Exception {
		return getSeqType().equals( Constants.FASTQ );
	}

	/**
	 * Return TRUE if reads are unpaired or if name does not contain the reverse read file suffix:
	 * {@value Constants#INPUT_REVERSE_READ_SUFFIX}
	 *
	 * @param name Name of file
	 * @return TRUE for unpaired or forward reads
	 */
	public static boolean isForwardRead( final String name ) {
		final String suffix = Config.getString( null, Constants.INPUT_REVERSE_READ_SUFFIX );
		if( suffix != null && name.contains( suffix ) ) return false;
		return true;
	}

	/**
	 * Determine if file is gzipped based on its file extension.<br>
	 * Any file ending with {@value biolockj.Constants#GZIP_EXT} is treated as a gzipped file.
	 * 
	 * @param fileName File name
	 * @return TRUE if file name ends with .gz
	 */
	public static boolean isGzipped( final String fileName ) {
		return fileName != null && fileName.toLowerCase().endsWith( Constants.GZIP_EXT );
	}

	/**
	 * Check current state of sequence data.
	 * 
	 * @return Boolean TRUE if multiplexed
	 * @throws ConfigFormatException if property assignment is invalid
	 */
	public static Boolean isMultiplexed() throws ConfigFormatException {
		return Config.getBoolean( null, Constants.INTERNAL_MULTIPLEXED );
	}

	/**
	 * Verify 1st character of sequence header and mask 1st sequence for valid DNA/RNA bases "acgtu"
	 * 
	 * @param file File
	 * @return TRUE if file is a sequence file
	 */
	public static boolean isSeqFile( final File file ) {
		BufferedReader reader = null;
		try {
			if( fileSeqMap.keySet().contains( file.getName() ) ) return fileSeqMap.get( file.getName() );
			info( "Check if input file is a SEQ file: " + file.getAbsolutePath() );
			boolean isSeq = false;
			reader = BioLockJUtil.getFileReader( file );

			final String header = SeqUtil.scanFirstLine( reader, file );
			final String idChar = header.substring( 0, 1 );
			String seq = reader.readLine();
			info(
				"header: " + ( header.length() <= MAX_DISPLAY_LEN ? header: header.substring( 0, MAX_DISPLAY_LEN ) ) );
			info( "seq: " + ( seq.length() <= MAX_DISPLAY_LEN ? seq: seq.substring( 0, MAX_DISPLAY_LEN ) ) );
			if( FASTA_HEADER_DELIMS.contains( idChar ) || idChar.equals( FASTQ_HEADER_DELIM ) ) {
				if( !seq.trim().isEmpty() ) {
					seq = seq.trim().toLowerCase().replaceAll( "a", "" ).replaceAll( "c", "" ).replaceAll( "g", "" )
						.replaceAll( "t", "" ).replaceAll( "n", "" );
					isSeq = seq.trim().isEmpty();
				}
			} else info( file.getAbsolutePath() + " is not a sequence file! " + Constants.RETURN + "Line 1: [ " +
				header + " ]" + Constants.RETURN + "Line 2= [ " + seq + " ]" );
			fileSeqMap.put( file.getName(), isSeq );
			return isSeq;
		} catch( final Exception ex ) {
			Log.error( SeqUtil.class, "Error occurred examining file to determine if it is a sequence file or not",
				ex );
		} finally {
			try {
				if( reader != null ) reader.close();
			} catch( final Exception ex ) {
				Log.error( SeqUtil.class, "Failed to close file reader", ex );
			}
		}
		fileSeqMap.put( file.getName(), false );
		return false;
	}

	/**
	 * Check the module to determine if it generated sequence file output.
	 * 
	 * @param module BioModule
	 * @return TRUE if module generated OTU count files
	 */
	public static boolean isSeqModule( final BioModule module ) {
		final Collection<File> files = BioLockJUtil.removeIgnoredAndEmptyFiles(
			FileUtils.listFiles( module.getOutputDir(), HiddenFileFilter.VISIBLE, HiddenFileFilter.VISIBLE ) );
		for( final File f: files )
			if( SeqUtil.isSeqFile( f ) ) return true;
		return false;
	}

	/**
	 * Return TRUE if pipeline input files are sequence files.
	 * 
	 * @return TRUE if pipeline input files are sequence files.
	 * @throws ConfigNotFoundException if {@value biolockj.util.BioLockJUtil#INTERNAL_PIPELINE_INPUT_TYPES} is undefined
	 */
	public static boolean piplineHasSeqInput() throws ConfigNotFoundException {
		return BioLockJUtil.pipelineInputType( BioLockJUtil.PIPELINE_SEQ_INPUT_TYPE );
	}

	/**
	 * Return the DNA reverse compliment for the input dna parameter.
	 * 
	 * @param dna DNA base sequence
	 * @return DNA reverse complement
	 * @throws Exception if sequence contains a non-standard letter (only ACGT accepted)
	 */
	public static String reverseComplement( final String dna ) throws Exception {
		String out = "";
		for( int i = dna.length() - 1; i >= 0; --i ) {
			final char curr = dna.charAt( i );
			if( curr == 'A' ) out += 'T';
			else if( curr == 'T' ) out += 'A';
			else if( curr == 'C' ) out += 'G';
			else if( curr == 'G' ) out += 'C';
			else throw new Exception( "ERROR: Input is not a DNA Sequence: " + dna );
		}
		// Log.debug( SeqUtil.class, "Reverse compliment for:" + dna + " = " + out );
		return out;
	}

	/**
	 * This method returns the 1st non-empty line and moves the BufferedReader pointer to this line. By passing the
	 * reader, subsequent calls to reader.readLine() will pick up from here.
	 * 
	 * @param reader BufferedReader reader for sequence file
	 * @param file Sequence file read by the BufferedReader (used for log and error messages)
	 * @return String First non-empty line
	 * @throws Exception if errors occur
	 */
	public static String scanFirstLine( final BufferedReader reader, final File file ) throws Exception {
		Integer i = 0;
		String line = reader.readLine();
		while( line != null && line.isEmpty() ) {
			i++;
			line = reader.readLine();
		}

		if( line == null ) throw new Exception( "Input dir contains empty file: " + file.getAbsolutePath() );
		if( i != 0 )
			Log.warn( SeqUtil.class, "Skipped [ " + i + " ] empty lines at the top of ---> " + file.getAbsolutePath() );

		return line.trim();
	}

	/**
	 * Set {@value biolockj.Constants#INPUT_IGNORE_FILES}, {@value Constants#INTERNAL_SEQ_HEADER_CHAR}, and
	 * {@value Constants#INTERNAL_SEQ_TYPE}
	 * <ul>
	 * <li>Ignore the metadata file {@link biolockj.Config}.{@value biolockj.util.MetaUtil#META_FILE_PATH}
	 * <li>If {@link biolockj.Config} pipeline contains {@link biolockj.module.seq} or
	 * {@link biolockj.module.classifier} {@link biolockj.module.BioModule}s, ignore non-(fasta/fastq) files found in
	 * {@link Constants#INPUT_DIRS}
	 * </ul>
	 *
	 * @throws Exception if {@link Constants#INPUT_DIRS} undefined or file reader I/O Exception occurs
	 */
	protected static void initSeqParams() throws Exception {
		String foundFasta = null;
		String foundFastq = null;
		String headerChar = null;

		for( final File f: BioLockJUtil.getPipelineInputFiles() ) {
			final BufferedReader reader = BioLockJUtil.getFileReader( f );
			try {
				String line = scanFirstLine( reader, f );
				int numSeqLines = 0;
				while( line != null ) {
					line = line.trim();
					if( headerChar == null ) {
						headerChar = line.substring( 0, 1 ); // set only once
						Log.debug( SeqUtil.class, " --> 1st headerChar = " + headerChar );
					} else {
						Log.debug( SeqUtil.class, f.getName() + " --> Line[" + ( numSeqLines + 1 ) + "] = " + line );
						final String testChar = line.trim().substring( 0, 1 );
						if( !testChar.equals( headerChar ) && !testChar.equals( "+" ) ) {
							numSeqLines++;
							if( numSeqLines > 1 )
								Log.warn( SeqUtil.class, f.getName() + " --> Line [ " + ( numSeqLines + 1 ) +
									" ] after TEST CHARACTER: " + headerChar + " = #seq lines = " + numSeqLines );
						} else {
							if( numSeqLines == 0 ) numSeqLines++;
							break;
						}
					}
					line = reader.readLine();
				}

				if( headerChar == null ) throw new Exception(
					"Failed to to identify 1st character of sequence header in: " + f.getAbsolutePath() );

				info( f.getAbsolutePath() + " --> #lines/read: " + ( numSeqLines + 1 ) );
				if( numSeqLines > 1 && Config.getString( null, Constants.INTERNAL_IS_MULTI_LINE_SEQ ) == null &&
					( FASTA_HEADER_DELIMS.contains( headerChar ) || headerChar.equals( FASTQ_HEADER_DELIM ) ) ) {
					info( "Multi-line input file detected: # lines/seq: " + numSeqLines );
					Config.setConfigProperty( Constants.INTERNAL_IS_MULTI_LINE_SEQ, Constants.TRUE );
					if( numMultiSeqLines != null && numMultiSeqLines == 0 ) numMultiSeqLines = numSeqLines;
					else if( numMultiSeqLines != null && numMultiSeqLines != numSeqLines ) {
						Log.warn( SeqUtil.class, "Multi-line input file has inconsistant number of lines per read" );
						numMultiSeqLines = null;
					}
				}
			} catch( final Exception ex ) {
				throw new Exception(
					"Found invalid sequence file: " + f.getAbsolutePath() + " --> " + ex.getMessage() );
			} finally {
				if( reader != null ) reader.close();
			}

			if( FASTA_HEADER_DELIMS.contains( headerChar ) ) {
				foundFasta = f.getAbsolutePath();
				Log.debug( SeqUtil.class, "detected FASTA: " + foundFasta );
			} else if( headerChar.equals( FASTQ_HEADER_DELIM ) ) {
				foundFastq = f.getAbsolutePath();
				Log.debug( SeqUtil.class, "detected FASTQ: " + foundFastq );
			} else throw new Exception( "Invalid sequence file format (1st character = " + headerChar + ") in: " +
				f.getAbsolutePath() + Constants.RETURN + "FASTA files must begin with either character: " +
				BioLockJUtil.getCollectionAsString( FASTA_HEADER_DELIMS ) + " and FASTQ files must begin with \"" +
				FASTQ_HEADER_DELIM + "\"" );
		}

		if( foundFasta != null && foundFastq != null ) throw new ConfigFormatException( Constants.INTERNAL_SEQ_TYPE,
			"Input files from: " + Constants.INPUT_DIRS + " must all be of a single type (FASTA or FASTQ)." +
				Constants.RETURN + "FASTA file found: " + foundFasta + Constants.RETURN + "FASTQ file found: " +
				foundFastq );

		if( foundFasta == null && foundFastq == null ) {
			if ( Config.getList( null, BioLockJUtil.PIPELINE_SEQ_INPUT_TYPE ).contains( Constants.FASTA ) ) foundFasta = "true";
			else if ( Config.getList( null, BioLockJUtil.PIPELINE_SEQ_INPUT_TYPE ).contains( Constants.FASTQ ) ) foundFastq = "true";
			else {
				throw new ConfigFormatException( Constants.INTERNAL_SEQ_TYPE, "No FASTA or FASTQ files found in: " + Constants.INPUT_DIRS );
			}
		}

		Config.setConfigProperty( Constants.INTERNAL_SEQ_HEADER_CHAR, headerChar );

		if( foundFasta != null ) setSeqType( Constants.FASTA );
		if( foundFastq != null ) setSeqType( Constants.FASTQ );

		final String configSeqType = Config.getString( null, Constants.INTERNAL_SEQ_TYPE );
		if( configSeqType != null && !configSeqType.toLowerCase().equals( getSeqType() ) ) {
			Log.warn( SeqUtil.class, "Override Config.: " + Constants.INTERNAL_SEQ_TYPE + "=" + configSeqType );
			Log.warn( SeqUtil.class, "New Config.: " + Constants.INTERNAL_SEQ_TYPE + "=" + getSeqType() );
		}
	}

	/**
	 * Inspect the pipeline input files to determine if input includes paired reads.
	 * 
	 * @throws Exception if unable to determine if reads are paired
	 */
	protected static void registerPairedReadStatus() throws Exception {
		boolean foundPairedReads = false;
		if( isMultiplexed() ) {
			if( BioLockJUtil.getPipelineInputFiles().size() > 1 ) foundPairedReads = true;
			else {
				boolean foundFw = false;
				boolean foundRv = false;
				final File testFile = BioLockJUtil.getPipelineInputFiles().iterator().next();
				final BufferedReader reader = BioLockJUtil.getFileReader( testFile );
				try {
					info( "Reading multiplexed file to check for paired reads: " + testFile.getAbsolutePath() );
					final List<String> seqLines = new ArrayList<>();
					for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
						seqLines.add( line );
						if( seqLines.size() == SeqUtil.getNumLinesPerRead() ) {
							if( seqLines.get( 0 ).contains( ILLUMINA_FW_READ_IND ) ) foundFw = true;
							else if( seqLines.get( 0 ).contains( ILLUMINA_RV_READ_IND ) ) foundRv = true;
							else Log.warn( SeqUtil.class,
								"Invalid header, no direction indicator: " + seqLines.get( 0 ) );

							seqLines.clear();
						}
					}
				} finally {
					if( reader != null ) reader.close();
				}

				foundPairedReads = foundFw && foundRv;
				if( foundRv && !foundFw )
					throw new Exception( "Invalid dataset.  Found only reverse reads in mutliplexed file!" +
						"Reverse read indicator found in headers: " + ILLUMINA_RV_READ_IND +
						"All datasets must contain forward read indicators in the headers: " + ILLUMINA_FW_READ_IND );
			}
		} else foundPairedReads = !getPairedReads( BioLockJUtil.getPipelineInputFiles() ).isEmpty();

		info( "Set " + Constants.INTERNAL_PAIRED_READS + "=" + ( foundPairedReads ? Constants.TRUE: Constants.FALSE ) );
		Config.setConfigProperty( Constants.INTERNAL_PAIRED_READS, foundPairedReads ? Constants.TRUE: Constants.FALSE );
	}

	private static void info( final String msg ) {
		if( !BioLockJUtil.isDirectMode() ) Log.info( SeqUtil.class, msg );
	}

	private static boolean mapSampleIdWithMetaFileNameCol() throws Exception {
		final String metaCol = Config.getString( null, MetaUtil.META_FILENAME_COLUMN );
		return metaCol != null && MetaUtil.hasColumn( metaCol ) && !MetaUtil.getFieldValues( metaCol, true ).isEmpty();
	}

	/**
	 * Unpaired reads must be multiplexed into a single file. Multiplexed paired reads must be contained in either 1
	 * file, or 2 (1 file with forward reads and 1 file with reverse reads). Based on the number of files and file
	 * names, we determine if the data is multiplexed. This function can be overridden by the Config property
	 * {@value biolockj.util.DemuxUtil#DEMUX_STRATEGY} with option {@value biolockj.util.DemuxUtil#OPTION_DO_NOT_DEMUX}
	 * to return false regardless of input file data.
	 *
	 * @throws Exception if validation fails
	 */
	private static void registerDemuxStatus() throws Exception {
		final int count = BioLockJUtil.getPipelineInputFiles().size();
		info( "Register Demux Status for: " + count + " input files." );
		boolean isMultiplexed = false;
		if( DemuxUtil.doDemux() != null && !DemuxUtil.doDemux() ) Log.debug( SeqUtil.class, "Do not demultiplex!" );
		else if( count == 1 ) {
			info( "Must demultiplex data!  Found exactly 1 input file" );
			isMultiplexed = true;
		} else if( count == 2 ) {

			boolean foundFw = false;
			boolean foundRv = false;
			final String fwRead = Config.getString( null, Constants.INPUT_FORWARD_READ_SUFFIX );
			final String rvRead = Config.getString( null, Constants.INPUT_REVERSE_READ_SUFFIX );
			final Set<String> suffixes = new HashSet<>();
			if( fwRead != null ) suffixes.add( fwRead );
			if( rvRead != null ) suffixes.add( rvRead );
			info( "Checking 2 input files for paired read suffix values: " + suffixes );
			for( final File file: BioLockJUtil.getPipelineInputFiles() )
				if( fwRead != null && file.getName().contains( fwRead ) ) {
					info( "Found forward read: " + file.getName() );
					foundFw = true;
				} else if( rvRead != null && file.getName().contains( rvRead ) ) {
					info( "Found reverse read: " + file.getName() );
					foundRv = true;
				}

			if( foundFw && foundRv ) {
				info( "Must demultiplex data!  Found exactly 1 set of paired input files" );
				isMultiplexed = true;
			}
		}

		Config.setConfigProperty( Constants.INTERNAL_MULTIPLEXED, isMultiplexed ? Constants.TRUE: Constants.FALSE );

		if( isMultiplexed && numMultiSeqLines == null ) throw new Exception(
			"Multi-line sequence files must be demultiplexed before analyzed by BioLockJ because the number of lines per read is inconsistant." );
	}

	private static void setSeqType( final String type ) {
		Config.setConfigProperty( Constants.INTERNAL_SEQ_TYPE, type );
	}

	/**
	 * Default 1st character for a FASTA file: {@value #FASTA_HEADER_DEFAULT_DELIM}
	 */
	public static final String FASTA_HEADER_DEFAULT_DELIM = ">";

	/**
	 * Illumina forward read indicator found in sequence header
	 */
	public static final String ILLUMINA_FW_READ_IND = " 1:N:";

	/**
	 * Illumina reverse read indicator found in sequence header
	 */
	public static final String ILLUMINA_RV_READ_IND = " 2:N:";

	private static final Map<String, String> DNA_BASE_MAP = new HashMap<>();

	private static final List<String> FASTA_HEADER_DELIMS = Arrays.asList( ">", ";" );
	private static final String FASTQ_HEADER_DELIM = "@";
	private static final Map<String, Boolean> fileSeqMap = new HashMap<>();
	private static final Integer MAX_DISPLAY_LEN = 200;
	private static Integer numMultiSeqLines = 0;
	static {
		// IUPAC DNA BASE Substitutions
		// http://www.dnabaser.com/articles/IUPAC%20ambiguity%20codes.html
		DNA_BASE_MAP.put( "Y", "[CT]" );
		DNA_BASE_MAP.put( "R", "[AG]" );
		DNA_BASE_MAP.put( "W", "[AT]" );
		DNA_BASE_MAP.put( "S", "[GC]" );
		DNA_BASE_MAP.put( "K", "[TG]" );
		DNA_BASE_MAP.put( "M", "[CA]" );
		DNA_BASE_MAP.put( "D", "[AGT]" );
		DNA_BASE_MAP.put( "V", "[ACG]" );
		DNA_BASE_MAP.put( "H", "[ACT]" );
		DNA_BASE_MAP.put( "B", "[CGT]" );
		DNA_BASE_MAP.put( "N", "[ACGT]" );
		DNA_BASE_MAP.put( "X", "[ACGT]" );
	}

}
