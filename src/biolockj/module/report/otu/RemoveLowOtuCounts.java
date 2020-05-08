package biolockj.module.report.otu;

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
import java.io.*;
import java.util.*;
import org.apache.commons.io.FileUtils;
import biolockj.*;
import biolockj.api.ApiModule;
import biolockj.module.implicit.parser.ParserModuleImpl;
import biolockj.util.*;

/**
 * This BioModule set low OTU counts below a configured threshold to zero.<br>
 * These low sample counts are assumed to be miscategorized or contaminents.
 * 
 * @blj.web_desc Remove Low OTU Counts
 */
public class RemoveLowOtuCounts extends OtuCountModule implements ApiModule{

	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		isValidProp(Constants.REPORT_MIN_COUNT);
	}
	
	@Override
	public Boolean isValidProp( String property ) throws Exception {
		Boolean isValid = super.isValidProp( property );
		switch(property) {
			case Constants.REPORT_MIN_COUNT:
				getMinCount();
				isValid = true;
				break;
		}
		return isValid;
	}

	/**
	 * Update {@link biolockj.module.implicit.parser.ParserModuleImpl} OTU_COUNT field name.
	 */
	@Override
	public void cleanUp() throws Exception {
		super.cleanUp();
		ParserModuleImpl.setNumHitsFieldName( getMetaColName() + "_" + Constants.OTU_COUNT );
	}

	/**
	 * Produce summary message with min, max, mean, and median number of reads.
	 */
	@Override
	public String getSummary() throws Exception {
		final String label = "OTUs";
		final int pad = SummaryUtil.getPad( label );
		String summary = "Remove OTU below count --> " + getMetaColName() + RETURN;
		summary +=
			BioLockJUtil.addTrailingSpaces( "# Unique OTUs removed:", pad ) + this.uniqueOtuRemoved.size() + RETURN;
		summary += BioLockJUtil.addTrailingSpaces( "# Total OTUs removed:", pad ) + this.totalOtuRemoved + RETURN;
		summary += SummaryUtil.getCountSummary( this.hitsPerSample, label, false );
		this.sampleIds.removeAll( this.hitsPerSample.keySet() );
		if( !this.sampleIds.isEmpty() ) summary += "Removed empty metadata records: " + this.sampleIds;
		this.hitsPerSample = null;
		return super.getSummary() + summary;
	}

	@Override
	public void runModule() throws Exception {
		this.sampleIds.addAll( MetaUtil.getSampleIds() );
		final TreeMap<String, TreeMap<String, Long>> sampleOtuCounts = OtuUtil.getSampleOtuCounts( getInputFiles() );

		final TreeMap<String, TreeSet<String>> lowCountOtus = removeLowOtuCounts( sampleOtuCounts );
		logLowCountOtus( lowCountOtus );
		if( Config.getBoolean( this, Constants.REPORT_NUM_HITS ) ) MetaUtil
			.addColumn( getMetaColName() + "_" + Constants.OTU_COUNT, this.hitsPerSample, getOutputDir(), true );
	}

	/**
	 * Save a list of low count OTUs to the module temp directory.
	 *
	 * @param lowCountOtus TreeMap(sampleId, TreeSet(OTU)) of OTUs found in too few samples
	 * @throws Exception if errors occur
	 */
	protected void logLowCountOtus( final TreeMap<String, TreeSet<String>> lowCountOtus ) throws Exception {
		if( lowCountOtus == null || lowCountOtus.isEmpty() ) {
			Log.info( getClass(), "No low-count OTUs detected!" );
			return;
		}
		final BufferedWriter writer = new BufferedWriter( new FileWriter( getLowCountOtuLogFile() ) );
		try {
			for( final String id: lowCountOtus.keySet() ) {
				final TreeSet<String> otus = lowCountOtus.get( id );

				for( final String otu: otus )
					writer.write( id + ": " + otu + RETURN );
			}
		} finally {
			writer.close();
		}

		Log.info( getClass(),
			"Found " + lowCountOtus.size() + " samples with low count OTUs removed - OTU list saved to --> " +
				getLowCountOtuLogFile().getAbsolutePath() );
	}

	/**
	 * Remove OTUs below the {@link biolockj.Config}.{@value biolockj.Constants#REPORT_MIN_COUNT }
	 *
	 * @param sampleOtuCounts TreeMap(SampleId, TreeMap(OTU, count)) OTU counts for every sample
	 * @return TreeMap(SampleId, TreeMap(OTU, count)) Updated sampleOtuCounts after removal of low counts.
	 * @throws Exception if errors occur
	 */
	protected TreeMap<String, TreeSet<String>>
		removeLowOtuCounts( final TreeMap<String, TreeMap<String, Long>> sampleOtuCounts ) throws Exception {
		final TreeMap<String, TreeSet<String>> lowCountOtus = new TreeMap<>();
		Log.debug( getClass(), "Build low count files for total # files: " + sampleOtuCounts.size() );
		for( final String sampleId: sampleOtuCounts.keySet() ) {
			final Set<String> badOtus = new TreeSet<>();
			Log.debug( getClass(), "Check for low OTU counts in: " + sampleId );
			long numOtus = 0;
			final TreeMap<String, Long> otuCounts = sampleOtuCounts.get( sampleId );

			final Set<String> validOtus = new TreeSet<>( otuCounts.keySet() );
			long numOtuRemoved = 0;
			for( final String otu: otuCounts.keySet() ) {
				final long count = otuCounts.get( otu );
				if( count < getMinCount() ) {
					this.uniqueOtuRemoved.add( otu );
					this.totalOtuRemoved += count;
					badOtus.add( otu );
					Log.debug( getClass(), sampleId + ": Remove Low OTU count: " + otu + "=" + count );
					validOtus.remove( otu );
					if( lowCountOtus.get( sampleId ) == null ) lowCountOtus.put( sampleId, new TreeSet<>() );
					lowCountOtus.get( sampleId ).add( otu );
					numOtuRemoved += count;
				} else numOtus += count;
				// Log.debug( getClass(),
				// sampleId + ": update OTU count to " + numOtus + " after adding: " + otu + "=" + count );
			}

			if( numOtus > 0 ) {
				Log.debug( getClass(), sampleId + ": Reduce total OTU count by: " + numOtuRemoved );
				this.hitsPerSample.put( sampleId, String.valueOf( numOtus ) );

				if( numOtuRemoved == 0 ) FileUtils.copyFileToDirectory( getFileMap().get( sampleId ), getOutputDir() );
				else {
					Log.warn( getClass(), sampleId + ": Removed " + badOtus.size() + " low OTU counts (below " +
						getMinCount() + ") --> " + badOtus );

					final File otuFile = OtuUtil.getOtuCountFile( getOutputDir(), sampleId, getMetaColName() );
					final BufferedWriter writer = new BufferedWriter( new FileWriter( otuFile ) );
					try {
						for( final String otu: validOtus )
							writer.write( otu + TAB_DELIM + otuCounts.get( otu ) + RETURN );
					} finally {
						writer.close();
						getFileMap().put( sampleId, otuFile );
					}
				}

			}
		}

		return lowCountOtus;
	}

	private Map<String, File> getFileMap() throws Exception {
		if( this.fileMap == null ) {
			this.fileMap = new HashMap<>();
			for( final File f: getInputFiles() )
				this.fileMap.put( OtuUtil.getSampleId( f ), f );
		}
		return this.fileMap;
	}

	private File getLowCountOtuLogFile() {
		return new File( getLogDir().getAbsolutePath() + File.separator + "lowCountOtus" + TXT_EXT );
	}

	private String getMetaColName() throws Exception {
		return "min" + getMinCount();
	}

	private Integer getMinCount() throws Exception {
		return Config.requirePositiveInteger( this, Constants.REPORT_MIN_COUNT );
	}
	
	@Override
	public String getDescription() {
		return "Removes OTUs with counts below report.minCount.";
	}

	@Override
	public String getCitationString() {
		return "Module developed by Mike Sioda" + System.lineSeparator() 
			+ "BioLockJ " + BioLockJUtil.getVersion();
	}

	private Map<String, File> fileMap = null;
	private Map<String, String> hitsPerSample = new HashMap<>();
	private final Set<String> sampleIds = new HashSet<>();
	private long totalOtuRemoved = 0;
	private final Set<String> uniqueOtuRemoved = new HashSet<>();

}
