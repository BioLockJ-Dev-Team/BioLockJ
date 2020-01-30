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
import java.util.stream.LongStream;
import org.apache.commons.lang.math.NumberUtils;
import biolockj.*;
import biolockj.exception.ConfigFormatException;
import biolockj.exception.SequnceFormatException;
import biolockj.module.JavaModuleImpl;
import biolockj.module.SeqModule;
import biolockj.module.implicit.RegisterNumReads;
import biolockj.util.*;

/**
 * This BioModule imposes a minimum and/or maximum number of reads per sample. Samples below the minimum are discarded.
 * Samples above the maximum are limited by selecting random reads up to the maximum value.
 * 
 * @blj.web_desc Rarefy Seqs
 */
public class RarefySeqs extends JavaModuleImpl implements SeqModule {

	/**
	 * Validate module dependencies
	 * <ol>
	 * <li>Validate {@link biolockj.Config}.{@link #INPUT_RAREFYING_MIN} is a non-negative integer
	 * <li>Validate {@link biolockj.Config}.{@link #INPUT_RAREFYING_MAX} is a positive integer that is greater than or
	 * equal to {@link biolockj.Config}.{@link #INPUT_RAREFYING_MIN} (if defined)
	 * </ol>
	 */
	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		final Integer rarefyingMax = Config.getPositiveInteger( this, INPUT_RAREFYING_MAX );
		final Integer rarefyingMin = Config.getNonNegativeInteger( this, INPUT_RAREFYING_MIN );
		if( rarefyingMin == null && rarefyingMax == null ||
			rarefyingMax != null && rarefyingMin != null && rarefyingMin > rarefyingMax )
			throw new Exception(
				"Invalid parameters!  RarefySeqs requires " + INPUT_RAREFYING_MIN + " <= " + INPUT_RAREFYING_MAX );
	}

	/**
	 * Set {@value #NUM_RAREFIED_READS} as the number of reads field.
	 */
	@Override
	public void cleanUp() throws Exception {
		super.cleanUp();
		RegisterNumReads.setNumReadFieldName( getMetaColName() );
		MetaUtil.addColumn( getMetaColName(), this.readsPerSample, getOutputDir(), true );
	}

	/**
	 * This method always requires a prerequisite module with a "number of reads" count such as:
	 * {@link biolockj.module.implicit.RegisterNumReads}. If paired reads found, also return a 2nd module:
	 * {@link biolockj.module.seq.PearMergeReads}.
	 */
	@Override
	public List<String> getPreRequisiteModules() throws Exception {
		final List<String> preReqs = super.getPreRequisiteModules();
		if( SeqUtil.hasPairedReads() ) preReqs.add( ModuleUtil.getDefaultMergePairedReadsConverter() );
		else if( SeqUtil.piplineHasSeqInput() && needsCountModule() ) preReqs.add( RegisterNumReads.class.getName() );

		return preReqs;
	}

	@Override
	public List<File> getSeqFiles( final Collection<File> files ) throws SequnceFormatException {
		return SeqUtil.getSeqFiles( files );
	}

	/**
	 * Produce summary message with min, max, mean, and median number of reads.
	 */
	@Override
	public String getSummary() throws Exception {
		final String label = "Reads";
		final int pad = SummaryUtil.getPad( label );
		String summary = SummaryUtil.getCountSummary( this.readsPerSample, "Reads", true );
		this.sampleIds.removeAll( this.readsPerSample.keySet() );
		if( !this.sampleIds.isEmpty() )
			summary += BioLockJUtil.addTrailingSpaces( "Removed empty metadata records:", pad ) +
				BioLockJUtil.getCollectionAsString( this.sampleIds );
		this.readsPerSample = null;
		return super.getSummary() + summary;
	}

	/**
	 * For each file with number reads outside of {@link biolockj.Config}.{@link #INPUT_RAREFYING_MIN} and
	 * {@link biolockj.Config}.{@link #INPUT_RAREFYING_MAX} values, generate a new sequence file from a shuffled list of
	 * its sequences.
	 */
	@Override
	public void runModule() throws Exception {
		Log.info( getClass(), "Base #Reads based on: " + RegisterNumReads.getNumReadFieldName() );
		this.sampleIds.addAll( MetaUtil.getSampleIds() );
		final List<File> files = getInputFiles();
		for( int i = 0; i < files.size(); i++ ) {
			final File f = files.get( i );
			rarefy( f );
			if( i % 25 == 0 || i + 1 == files.size() )
				Log.info( getClass(), "Done rarefying " + i + "/" + files.size() + " files." );
		}

	}

	/**
	 * Build the rarefied file for the input file, keeping only the given indexes
	 *
	 * @param input Sequence file
	 * @param indexes List of indexes to keep
	 * @throws Exception if unable to build rarefied file
	 */
	protected void buildRarefiedFile( final File input, final List<Long> indexes ) throws Exception {
		Log.info( getClass(), "Rarefy [#index=" + indexes.size() + "]: " + input.getAbsolutePath() );
		Log.debug( getClass(), "indexes: " + BioLockJUtil.getCollectionAsString( indexes ) );
		final String fileExt = "." + SeqUtil.getSeqType();
		final String name =
			getOutputDir().getAbsolutePath() + File.separator + SeqUtil.getSampleId( input.getName() ) + fileExt;
		final File output = new File( name );
		final BufferedReader reader = BioLockJUtil.getFileReader( input );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( output ) );
		Log.info( getClass(),
			"Building file [#lines/read=" + SeqUtil.getNumLinesPerRead() + "]: " + output.getAbsolutePath() );

		try {
			long index = 0;
			int i = 0;
			final Set<Long> usedIndexes = new HashSet<>();
			for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
				if( indexes.contains( index ) ) {
					if( !usedIndexes.contains( index ) ) Log.debug( getClass(), "Add to usedIndexes: " + index );
					usedIndexes.add( index );
					writer.write( line + RETURN );
				}

				if( ++i % SeqUtil.getNumLinesPerRead() == 0 ) index++;
			}

			this.readsPerSample.put( SeqUtil.getSampleId( input.getName() ), Integer.toString( indexes.size() ) );

			if( !usedIndexes.containsAll( indexes ) ) {
				indexes.removeAll( usedIndexes );
				throw new Exception(
					"Error occurred rarefying indexes for: " + input.getAbsolutePath() + " ---> " + indexes );
			}
		} finally {
			reader.close();
			writer.close();
		}
	}

	/**
	 * Builds the rarefied file if too many seqs found, or adds files with too few samples to the list of bad samples.
	 *
	 * @param seqFile Sequence file to rarefy
	 * @throws Exception if processing errors occur
	 */
	protected void rarefy( final File seqFile ) throws Exception {
		final Integer maxConfig = Config.getNonNegativeInteger( this, INPUT_RAREFYING_MAX );
		final Integer minConfig = Config.getNonNegativeInteger( this, INPUT_RAREFYING_MIN );
		Long max = 0L;
		Long min = 0L;
		final String sampleId = SeqUtil.getSampleId( seqFile.getName() );
		final long numReads = getCount( sampleId, RegisterNumReads.getNumReadFieldName() );

		if( maxConfig != null ) max = numReads < maxConfig.longValue() ? numReads: maxConfig.longValue();

		if( minConfig == null ) min = 1L;
		else min = minConfig.longValue();

		Log.debug( getClass(), "min = " + min );
		Log.debug( getClass(), "max = " + max );
		Log.debug( getClass(), "numReads = " + numReads );
		if( numReads >= min ) {
			final long[] range = LongStream.rangeClosed( 0, numReads - 1 ).toArray();
			Log.debug( getClass(), "range.length = " + range.length );
			final List<Long> indexes = new ArrayList<>();
			Collections.addAll( indexes, Arrays.stream( range ).boxed().toArray( Long[]::new ) );

			Log.debug( getClass(), "Sample #indexes size #1 -->  [" + indexes.size() + "]" );

			Collections.shuffle( indexes, getRandomSeed() );
			indexes.subList( max.intValue(), indexes.size() ).clear();

			Log.debug( getClass(), "Sample #indexes size  #2 -->  [" + indexes.size() + "]" );

			buildRarefiedFile( seqFile, indexes );
		} else Log.info( getClass(),
			"Remove sample [" + sampleId + "] - contains (" + numReads +
				") reads, which is less than minimum # reads (" +
				Config.getNonNegativeInteger( this, INPUT_RAREFYING_MIN ) + ")" );
	}

	private String getMetaColName() throws Exception {
		if( this.otuColName == null ) this.otuColName = MetaUtil.getSystemMetaCol( this, NUM_RAREFIED_READS );

		return this.otuColName;
	}

	private Random getRandomSeed() throws ConfigFormatException {
		final Integer seed = Config.getPositiveInteger( this, Constants.SET_SEED );
		if( seed != null ) return new Random( seed );
		return new Random();
	}

	private boolean needsCountModule() throws Exception {
		for( final String module: Config.requireList( this, Constants.INTERNAL_BLJ_MODULE ) )
			if( module.contentEquals( RegisterNumReads.class.getName() ) || module.equals( SeqFileValidator.class.getName() ) || module.equals( TrimPrimers.class.getName() ) )
				return false;
			else if( module.equals( getClass().getName() ) ) return true;

		return true;
	}

	private static Long getCount( final String sampleId, final String attName ) throws Exception {
		if( MetaUtil.getFieldNames().contains( attName ) ) {
			final String count = MetaUtil.getField( sampleId, attName );
			if( count != null && NumberUtils.isNumber( count ) ) return Long.valueOf( count );
		}

		return null;
	}

	private String otuColName = null;
	private Map<String, String> readsPerSample = new HashMap<>();
	private final Set<String> sampleIds = new HashSet<>();

	/**
	 * Metadata column name for column that holds number of rarefied reads per sample: {@value #NUM_RAREFIED_READS}
	 */
	public static final String NUM_RAREFIED_READS = "Num_Rarefied_Reads";

	/**
	 * {@link biolockj.Config} property {@value #INPUT_RAREFYING_MAX} defines the maximum number of reads per file
	 */
	protected static final String INPUT_RAREFYING_MAX = "rarefySeqs.max";

	/**
	 * {@link biolockj.Config} property {@value #INPUT_RAREFYING_MIN} defines the minimum number of reads per file
	 */
	protected static final String INPUT_RAREFYING_MIN = "rarefySeqs.min";

}
