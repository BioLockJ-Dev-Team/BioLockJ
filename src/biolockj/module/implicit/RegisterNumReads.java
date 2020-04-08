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
package biolockj.module.implicit;

import java.io.File;
import java.util.*;
import org.apache.commons.io.FileUtils;
import biolockj.Log;
import biolockj.exception.SequnceFormatException;
import biolockj.module.CountsReads;
import biolockj.module.JavaModuleImpl;
import biolockj.module.SeqModule;
import biolockj.util.*;

/**
 * This BioModule parses sequence file to count the number of reads per sample. The data is stored in a new column
 * Num_Reads. If no metadata file exists, it is created with 2 columns: SampleID and Num_Reads.
 * 
 * @blj.web_desc Register Number of Reads
 */
public class RegisterNumReads extends JavaModuleImpl implements SeqModule, CountsReads {

	@Override
	public List<File> getSeqFiles( final Collection<File> files ) throws SequnceFormatException {
		return SeqUtil.getSeqFiles( files );
	}

	/**
	 * Produce summary message with min, max, mean, and median number of reads.
	 */
	@Override
	public String getSummary() throws Exception {
		String summary = SummaryUtil.getCountSummary( this.readsPerSample, "Reads", true );
		this.sampleIds.removeAll( this.readsPerSample.keySet() );
		if( !this.sampleIds.isEmpty() )
			summary += "Removed empty metadata records: " + BioLockJUtil.getCollectionAsString( this.sampleIds );

		freeMemory();
		return super.getSummary() + summary;
	}

	/**
	 * Register number of reads for each sample by parsing each file and counting number of lines. Add
	 * {@value #NUM_READS} column to metadata and refresh the cache.
	 */
	@Override
	public void runModule() throws Exception {
		this.sampleIds.addAll( MetaUtil.getSampleIds() );
		if( MetaUtil.getFieldNames().contains( NUM_READS ) ) {
			if( MetaUtil.getFieldValues( NUM_READS, false ).size() == MetaUtil.getSampleIds().size() ) {
				Log.warn( getClass(), NUM_READS + " column already  populated in: " + MetaUtil.getPath() );
				FileUtils.copyFileToDirectory( MetaUtil.getMetadata(), getOutputDir() );
				if( getMetadata() == null )
					throw new Exception( "FileUtils.copyFileToDirectory failed to copy metadata to module output dir" );
				return;
			}

			Log.warn( getClass(), NUM_READS + " column partially populated.  Clearing values & recounting reads" );
			MetaUtil.removeColumn( NUM_READS, getTempDir() );
		}

		final List<File> files = getInputFiles();
		Log.info( getClass(), "Counting # reads/sample for " + files.size() + " files" );

		for( final File f: files )
			if( SeqUtil.isForwardRead( f.getName() ) ) {
				final long count = SeqUtil.countNumReads( f );
				Log.debug( getClass(), "Num Reads for :[" + SeqUtil.getSampleId( f.getName() ) + "] = " + count );
				this.readsPerSample.put( SeqUtil.getSampleId( f.getName() ), Long.toString( count ) );
			}

		MetaUtil.addColumn( getNumReadFieldName(), this.readsPerSample, getOutputDir(), true );
	}

	/**
	 * Free up memory.
	 */
	private void freeMemory() {
		this.sampleIds = null;
		this.readsPerSample = null;
	}

	/**
	 * Getter for depricatedReadFields
	 *
	 * @return depricatedReadFields
	 */
	public static Set<String> getDepricatedReadFields() {
		return depricatedReadFields;
	}

	/**
	 * Getter for numReadFieldName
	 *
	 * @return numReadFieldName
	 */
	public static String getNumReadFieldName() {
		return numReadFieldName;
	}

	/**
	 * When a module modifies the number of reads, the new counts must replace the old count fields.
	 *
	 * @param name Name of new number of reads metadata field
	 * @throws Exception if null value passed
	 */
	public static void setNumReadFieldName( final String name ) throws Exception {
		if( name == null )
			throw new Exception( "Null name value passed to RegisterNumReads.setNumReadFieldName(name)" );
		else if( numReadFieldName != null && numReadFieldName.equals( name ) )
			Log.warn( RegisterNumReads.class, "NumReads field already set to: " + numReadFieldName );
		else {
			if( numReadFieldName != null ) depricatedReadFields.add( numReadFieldName );

			depricatedReadFields.remove( name );
			numReadFieldName = name;
		}
	}

	private Map<String, String> readsPerSample = new HashMap<>();

	private Set<String> sampleIds = new HashSet<>();
	/**
	 * Metadata column name for column that holds number of reads per sample: {@value #NUM_READS}
	 */
	protected static final String NUM_READS = "Num_Reads";
	private static final Set<String> depricatedReadFields = new HashSet<>();
	private static String numReadFieldName = NUM_READS;

}
