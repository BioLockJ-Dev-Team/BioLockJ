package biolockj.module.report;

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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;
import org.apache.commons.io.FileUtils;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.BioModule;
import biolockj.module.JavaModule;
import biolockj.module.JavaModuleImpl;
import biolockj.module.implicit.parser.ParserModuleImpl;
import biolockj.util.*;

/**
 * This BioModule
 */
public class RemoveLowCountOtus extends JavaModuleImpl implements JavaModule
{

	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		Config.requirePositiveInteger( MIN_OTU_COUNT );
	}

	/**
	 * Update {@link biolockj.module.implicit.parser.ParserModuleImpl} OTU_COUNT field name.
	 */
	@Override
	public void cleanUp() throws Exception
	{
		super.cleanUp();
		ParserModuleImpl.setNumHitsFieldName( getMetaColName() );
	}

	/**
	 * Produce summary message with min, max, mean, and median number of reads.
	 */
	@Override
	public String getSummary() throws Exception
	{
		String summary = "Remove OTU below count --> " + getMetaColName() + RETURN;
		summary += "# Unique OTU removed:  " + uniqueOtuRemoved.size() + RETURN;
		summary += "# Total OTU removed:   " + totalOtuRemoved + RETURN;
		summary += SummaryUtil.getCountSummary( hitsPerSample, "OTUs" );
		sampleIds.removeAll( hitsPerSample.keySet() );
		if( !sampleIds.isEmpty() )
		{
			summary += "Removed empty samples: " + BioLockJUtil.getCollectionAsString( sampleIds );
		}
		hitsPerSample = null;
		return super.getSummary() + summary;
	}

	@Override
	public boolean isValidInputModule( final BioModule previousModule ) throws Exception
	{
		return OtuUtil.outputHasOtuCountFiles( previousModule );
	}

	@Override
	public void runModule() throws Exception
	{
		sampleIds.addAll( MetaUtil.getSampleIds() );
		final TreeMap<String, TreeMap<String, Integer>> sampleOtuCounts = OtuUtil.getSampleOtuCounts( getInputFiles() );

		final TreeMap<String, TreeSet<String>> lowCountOtus = removeLowCountOtus( sampleOtuCounts );
		logLowCountOtus( lowCountOtus );
		if( Config.getBoolean( Config.REPORT_NUM_HITS ) )
		{
			MetaUtil.addColumn( getMetaColName(), hitsPerSample, getOutputDir(), true );
		}
	}

	/**
	 * Save a list of scarce OTUs to the module temp directory.
	 * 
	 * @param lowCountOtus TreeMap(sampleId, TreeSet(OTU)) of OTUs found in too few samples
	 * @throws Exception if errors occur
	 */
	protected void logLowCountOtus( final TreeMap<String, TreeSet<String>> lowCountOtus ) throws Exception
	{
		if( lowCountOtus == null || lowCountOtus.isEmpty() )
		{
			Log.info( getClass(), "No low-count OTUs detected!" );
			return;
		}
		final BufferedWriter writer = new BufferedWriter( new FileWriter( getLowCountOtuLogFile() ) );
		try
		{
			for( final String id: lowCountOtus.keySet() )
			{
				final TreeSet<String> otus = lowCountOtus.get( id );

				for( final String otu: otus )
				{
					writer.write( id + ": " + otu + RETURN );
				}
			}
		}
		finally
		{
			if( writer != null )
			{
				writer.close();
			}
		}

		Log.info( getClass(),
				"Found " + lowCountOtus.size() + " samples with low count OTUs removed - OTU list saved to --> "
						+ getLowCountOtuLogFile().getAbsolutePath() );
	}

	/**
	 * Remove OTUs below the {@link biolockj.Config}.{@value #MIN_OTU_COUNT}
	 * 
	 * @param sampleOtuCounts TreeMap(SampleId, TreeMap(OTU, count)) OTU counts for every sample
	 * @return TreeMap(SampleId, TreeMap(OTU, count)) Updated sampleOtuCounts after removal of low counts.
	 * @throws Exception if errors occur
	 */
	protected TreeMap<String, TreeSet<String>> removeLowCountOtus(
			final TreeMap<String, TreeMap<String, Integer>> sampleOtuCounts ) throws Exception
	{
		final TreeMap<String, TreeSet<String>> lowCountOtus = new TreeMap<>();
		Log.debug( getClass(), "Build low count files for total # files: " + sampleOtuCounts.size() );
		for( final String sampleId: sampleOtuCounts.keySet() )
		{
			final Set<String> badOtus = new TreeSet<>();
			Log.debug( getClass(), "Check for low OTU counts in: " + sampleId );
			int numOtus = 0;
			final TreeMap<String, Integer> otuCounts = sampleOtuCounts.get( sampleId );

			final Set<String> validOtus = new TreeSet<>( otuCounts.keySet() );
			int numOtuRemoved = 0;
			for( final String otu: otuCounts.keySet() )
			{
				final int count = otuCounts.get( otu );
				if( count < Config.requirePositiveInteger( MIN_OTU_COUNT ) )
				{
					uniqueOtuRemoved.add( otu );
					totalOtuRemoved += count;
					badOtus.add( otu );
					Log.debug( getClass(), sampleId + ": Remove Low OTU count: " + otu + "=" + count );
					validOtus.remove( otu );
					if( lowCountOtus.get( sampleId ) == null )
					{
						lowCountOtus.put( sampleId, new TreeSet<>() );
					}
					lowCountOtus.get( sampleId ).add( otu );
					numOtuRemoved += count;
				}
				else
				{
					numOtus += count;
					// Log.debug( getClass(),
					// sampleId + ": update OTU count to " + numOtus + " after adding: " + otu + "=" + count );
				}
			}

			if( numOtus > 0 )
			{
				Log.debug( getClass(), sampleId + ": Reduce total OTU count by: " + numOtuRemoved );
				hitsPerSample.put( sampleId, String.valueOf( numOtus ) );

				if( numOtuRemoved == 0 )
				{
					FileUtils.copyFileToDirectory( getFileMap().get( sampleId ), getOutputDir() );
				}
				else
				{

					Log.warn( getClass(),
							sampleId + ": Removed " + badOtus.size() + " low OTU counts (below " + MIN_OTU_COUNT + "="
									+ Config.requirePositiveInteger( MIN_OTU_COUNT ) + ") --> "
									+ BioLockJUtil.getCollectionAsString( badOtus ) );

					final File otuFile = OtuUtil.getOtuCountFile( getOutputDir(), sampleId, getMetaColName() );
					final BufferedWriter writer = new BufferedWriter( new FileWriter( otuFile ) );
					try
					{
						for( final String otu: validOtus )
						{
							writer.write( otu + TAB_DELIM + otuCounts.get( otu ) + RETURN );
						}
					}
					finally
					{
						if( writer != null )
						{
							writer.close();
						}

						getFileMap().put( sampleId, otuFile );
					}
				}

			}
		}

		return lowCountOtus;
	}

	private Map<String, File> getFileMap() throws Exception
	{
		if( fileMap == null )
		{
			fileMap = new HashMap<>();
			for( final File f: getInputFiles() )
			{
				fileMap.put( OtuUtil.getSampleId( f ), f );
			}
		}
		return fileMap;
	}

	private File getLowCountOtuLogFile() throws Exception
	{
		return new File( getTempDir().getAbsolutePath() + File.separator + "lowCountOtus" + TXT_EXT );
	}

	private String getMetaColName() throws Exception
	{
		return "OTU_COUNT_min" + Config.requirePositiveInteger( MIN_OTU_COUNT );
	}

	private Map<String, File> fileMap = null;
	private Map<String, String> hitsPerSample = new HashMap<>();
	private final Set<String> sampleIds = new HashSet<>();
	private int totalOtuRemoved = 0;
	private final Set<String> uniqueOtuRemoved = new HashSet<>();

	/**
	 * {@link biolockj.Config} Positive Integer property {@value #MIN_OTU_COUNT} defines the minimum number of OTUs
	 * allowed, if a count less that this value is found, it is set to 0.
	 */
	protected static final String MIN_OTU_COUNT = "removeLowCounts.minOtuCount";
}
