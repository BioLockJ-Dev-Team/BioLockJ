/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Dec 18, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import biolockj.*;
import biolockj.exception.BioLockJException;
import biolockj.exception.ConfigFormatException;
import biolockj.exception.ConfigNotFoundException;
import biolockj.exception.DockerVolCreationException;
import biolockj.module.report.taxa.TaxaLevelTable;

/**
 * This utility helps work individual Taxa names, not full OTU path files which need {@link biolockj.util.OtuUtil}.<br>
 * Taxa files containing sample taxa counts for a given taxonomy level as output by
 * {@link biolockj.module.report.taxa.BuildTaxaTables}.
 */
public class TaxaUtil {
	// Prevent instantiation
	private TaxaUtil() {}

	/**
	 * Returns a list of all taxonomy levels, not only the levels configured via
	 * {@link biolockj.Config}.{@value biolockj.Constants#REPORT_TAXONOMY_LEVELS}.
	 * 
	 * @return Ordered List of all possible taxonomy levels
	 */
	public static List<String> allTaxonomyLevels() {
		return TAXA_LEVELS;
	}

	/**
	 * Return the bottom configured taxonomy level from
	 * {@link biolockj.Config}.{@value biolockj.Constants#REPORT_TAXONOMY_LEVELS}
	 * 
	 * @return Taxonomy level
	 */
	public static String bottomTaxaLevel() {
		if( bottomLevel == null ) bottomLevel = getTaxaLevels().get( getTaxaLevels().size() - 1 );
		return bottomLevel;
	}

	/**
	 * Extract taxonomy names at the given level from all given OTUs.
	 * 
	 * @param otus TreeSet of OTUs in {@link biolockj.module.implicit.parser.ParserModule} format
	 * @param level {@link biolockj.Config}.{@value biolockj.Constants#REPORT_TAXONOMY_LEVELS}
	 * @return Ordered TreeSet of unique taxonomy names
	 */
	public static TreeSet<String> findUniqueTaxa( final TreeSet<String> otus, final String level ) {
		final TreeSet<String> uniqueTaxa = new TreeSet<>();
		for( final String otu: otus ) {
			final String taxa = getTaxaName( otu, level );
			if( taxa != null ) uniqueTaxa.add( taxa );
		}
		return uniqueTaxa;
	}

	/**
	 * Return OTU taxonomy level for the leaf edge of the OTU
	 * 
	 * @param otu OTU
	 * @return Leaf taxonomy level
	 */
	public static String getLeafLevel( final String otu ) {
		String lastLevel = null;
		for( final String level: getTaxaLevelSpan() )
			if( otu.contains( level ) ) lastLevel = level;
		return lastLevel;
	}

	/**
	 * Return the absolute taxonomy level for the given level. Constants.DOMAIN, Constants.PHYLUM, Constants.CLASS,
	 * Constants.ORDER, Constants.FAMILY, Constants.GENUS, Constants.SPECIES
	 * <ol>
	 * <li>{@value biolockj.Constants#DOMAIN}
	 * <li>{@value biolockj.Constants#PHYLUM}
	 * <li>{@value biolockj.Constants#CLASS}
	 * <li>{@value biolockj.Constants#ORDER}
	 * <li>{@value biolockj.Constants#FAMILY}
	 * <li>{@value biolockj.Constants#GENUS}
	 * <li>{@value biolockj.Constants#SPECIES}
	 * </ol>
	 * 
	 * @param level Taxonomy level
	 * @return Integer level number
	 */
	public static Integer getLevelNum( final String level ) {
		return allTaxonomyLevels().indexOf( level );
	}

	/**
	 * Return TreeMap keyed on Sample ID, each sample maps to an inner map(taxa, count).<br>
	 * Input param sampleOtuCounts contains OTUs for which many can share the same level taxonomy assignment.<br>
	 * For example if sample42 contained the following 3 OTU values:
	 * <ul>
	 * <li>phylum__Actinobacteria|class__Actinobacteria|order__Actinomycetales|family__Actinomycetaceae|genus__Actinomyces
	 * 1000
	 * <li>phylum__Bacteroidetes|class__Bacteroidia|order__Bacteroidales|family__Porphyromonadaceae|genus__Barnesiella
	 * 500
	 * <li>phylum__Bacteroidetes|class__Bacteroidia|order__Bacteroidales|family__Porphyromonadaceae|genus__Parabacteroides
	 * 77
	 * </ul>
	 * The phylum level output would contain entries:
	 * <ol>
	 * <li>TreeMap(sample42, TreeMap( Actinobacteria=1000, Bacteroidetes=577 ) )
	 * </ol>
	 * 
	 * @param sampleOtuCounts TreeMap(sampleId, TreeMap(OTU, count)) OTU counts for every sample
	 * @param level {@link biolockj.Config}.{@value biolockj.Constants#REPORT_TAXONOMY_LEVELS}
	 * @return TreeMap(sampleId, TreeMap(taxa, count))
	 */
	public static TreeMap<String, TreeMap<String, Long>>
		getLevelTaxaCounts( final TreeMap<String, TreeMap<String, Long>> sampleOtuCounts, final String level ) {
		final TreeMap<String, TreeMap<String, Long>> taxaCounts = new TreeMap<>();

		for( final String sampleId: sampleOtuCounts.keySet() ) {
			final TreeMap<String, Long> otuCounts = sampleOtuCounts.get( sampleId );
			for( final String otu: otuCounts.keySet() ) {
				final String taxa = getTaxaName( otu, level );
				if( taxa != null ) {
					if( taxaCounts.get( sampleId ) == null ) taxaCounts.put( sampleId, new TreeMap<>() );
					if( taxaCounts.get( sampleId ).get( taxa ) == null ) taxaCounts.get( sampleId ).put( taxa, 0L );
					taxaCounts.get( sampleId ).put( taxa,
						taxaCounts.get( sampleId ).get( taxa ) + otuCounts.get( otu ) );
				}
			}
		}

		return taxaCounts;
	}

	/**
	 * Return a map of the given otu parameter split by level.
	 * 
	 * @param otu OTU path value
	 * @return TreeMap(level, taxa)
	 */
	public static TreeMap<String, String> getTaxaByLevel( final String otu ) {
		final TreeMap<String, String> map = new TreeMap<>();
		for( final String level: getTaxaLevels() ) {
			final String name = getTaxaName( otu, level );
			if( name != null ) map.put( level, getTaxaName( otu, level ) );
		}
		return map;
	}

	/**
	 * Method ensures taxonomy levels are returned in descending order from top to bottom.
	 * 
	 * @return configLevels
	 */
	public static List<String> getTaxaLevels() {
		return configLevels;
	}

	/**
	 * Return taxa levels from top to bottom level, including in-between levels not configured as part of
	 * {@value Constants#REPORT_TAXONOMY_LEVELS}
	 * 
	 * @return List of taxonomy levels
	 */
	public static List<String> getTaxaLevelSpan() {
		if( levelSpan != null ) return levelSpan;
		levelSpan = new ArrayList<>();
		for( final String level: allTaxonomyLevels() ) {
			if( !levelSpan.isEmpty() || level.equals( topTaxaLevel() ) ) levelSpan.add( level );
			if( level.equals( bottomTaxaLevel() ) ) break;
		}
		return levelSpan;
	}

	/**
	 * Extract a taxonomy name at the given level from the given OTU.
	 * 
	 * @param otu OTU name in {@link biolockj.module.implicit.parser.ParserModule} format
	 * @param level {@link biolockj.Config}.{@value biolockj.Constants#REPORT_TAXONOMY_LEVELS}
	 * @return Taxonomy name
	 */
	public static String getTaxaName( final String otu, final String level ) {
		final StringTokenizer st = new StringTokenizer( otu, Constants.OTU_SEPARATOR );
		while( st.hasMoreTokens() ) {
			final String levelOtu = st.nextToken();
			if( levelOtu.startsWith( level + Constants.DELIM_SEP ) )
				return levelOtu.replaceFirst( level + Constants.DELIM_SEP, "" );
		}
		return null;
	}

	/**
	 * Create File object of a taxonomy table at the given level, with the given suffix, in the given directory dir.
	 * 
	 * @param dir Target directory
	 * @param level Taxonomy level
	 * @param suffix File suffix
	 * @return Taxonomy Table File
	 * @throws BioLockJException if level is null
	 */
	public static File getTaxonomyTableFile( final File dir, final String level, final String suffix ) throws BioLockJException
		 { // Replace Exception with new TaxaTableException
		if( level == null ) throw new BioLockJException( "Level is required to build a taxonomy table" );
		String mySuffix = suffix;
		if( mySuffix != null && !mySuffix.endsWith( "_" ) ) mySuffix += "_";
		if( mySuffix != null && !mySuffix.startsWith( "_" ) ) mySuffix = "_" + mySuffix;
		if( mySuffix == null ) mySuffix = "_";
		return new File( dir.getAbsolutePath() + File.separator + Config.pipelineName() + "_" + TAXA_TABLE + mySuffix +
			level + Constants.TSV_EXT );

		// Use new getLevelNum( level ) method once I have time to test with R scripts.
		// return new File( dir.getAbsolutePath() + File.separator + Config.pipelineName() + "_" + TAXA_TABLE + mySuffix
		// +
		// getLevelNum(level) + "_" + level + Constants.TSV_EXT );
	}

	/**
	 * Extract the table taxonomy level from an existing taxonomy table file.
	 * 
	 * @param file File Taxonomy Table
	 * @return Taxonomy level
	 */
	public static String getTaxonomyTableLevel( final File file ) {
		for( final String level: getTaxaLevels() )
			if( file.getName().endsWith( level + Constants.TSV_EXT ) ) return level;
		return null;
	}

	/**
	 * Build the name of an unclassified taxa using the given "taxa" parameter.<br>
	 * Returns a name like Unclassified (taxa name) Taxa
	 * 
	 * @param taxa Taxa parent name
	 * @param level Taxa parent level
	 * @return Unclassified Taxa name
	 */
	public static String getUnclassifiedTaxa( final String taxa, final String level ) {
		return Constants.UNCLASSIFIED + " " + taxa + " " + level.substring( 0, 1 ).toUpperCase() + level.substring( 1 );
	}

	/**
	 * Set taxonomy levels ordered by level, from highest to lowest. Require {@value Constants#REPORT_TAXONOMY_LEVELS}
	 * property. Accepts only valid options: {@value Constants#DOMAIN}, {@value Constants#PHYLUM},
	 * {@value Constants#CLASS}, {@value Constants#ORDER}, {@value Constants#FAMILY}, {@value Constants#GENUS},
	 * {@value Constants#SPECIES}
	 *
	 * @return List of ordered taxonomy levels
	 * @throws ConfigNotFoundException if {@value Constants#REPORT_TAXONOMY_LEVELS} is undefined
	 * @throws ConfigFormatException if {@value Constants#REPORT_TAXONOMY_LEVELS} is defined, but does not contain any
	 * valid taxonomy levels
	 * @throws DockerVolCreationException 
	 */
	public static List<String> initTaxaLevels() throws ConfigNotFoundException, ConfigFormatException, DockerVolCreationException {
		configLevels = new ArrayList<>();
		final String errorMsg =
			"Property only accepts valid taxonomy levels ==>  " + BioLockJUtil.getCollectionAsString( TAXA_LEVELS );
		final Set<String> configuredLevels = new HashSet<>();
		final List<String> validOptions = allTaxonomyLevels();

		for( final String element: Config.requireList( null, Constants.REPORT_TAXONOMY_LEVELS ) )
			if( validOptions.contains( element.toLowerCase() ) ) configuredLevels.add( element.toLowerCase() );
			else throw new ConfigFormatException( Constants.REPORT_TAXONOMY_LEVELS,
				"Invalid level defined [" + element + "]  " + errorMsg );

		if( configuredLevels.contains( Constants.DOMAIN ) ) configLevels.add( Constants.DOMAIN );
		if( configuredLevels.contains( Constants.PHYLUM ) ) configLevels.add( Constants.PHYLUM );
		if( configuredLevels.contains( Constants.CLASS ) ) configLevels.add( Constants.CLASS );
		if( configuredLevels.contains( Constants.ORDER ) ) configLevels.add( Constants.ORDER );
		if( configuredLevels.contains( Constants.FAMILY ) ) configLevels.add( Constants.FAMILY );
		if( configuredLevels.contains( Constants.GENUS ) ) configLevels.add( Constants.GENUS );
		if( configuredLevels.contains( Constants.SPECIES ) ) configLevels.add( Constants.SPECIES );

		if( configLevels.isEmpty() ) throw new ConfigFormatException( Constants.REPORT_TAXONOMY_LEVELS,
			"No valid options configured.  " + errorMsg );

		Config.setConfigProperty( Constants.REPORT_TAXONOMY_LEVELS, configLevels );
		return configLevels;
	}

	/**
	 * Check the file name to determine if it is a Log normalized taxonomy table file.
	 * 
	 * @param file File to test
	 * @return boolean TRUE if file is a normalized taxonomy count file
	 */
	public static boolean isLogNormalizedTaxaFile( final File file ) {
		for( final String level: getTaxaLevels() )
			if( file.getName().contains( "_" + TAXA_TABLE + "_Log" ) &&
				file.getName().endsWith( "_" + NORMALIZED + "_" + level + Constants.TSV_EXT ) ) return true;
		return false;
	}

	/**
	 * Check the file name to determine if it is a normalized taxonomy table file.
	 * 
	 * @param file File to test
	 * @return boolean TRUE if file is a normalized taxonomy count file
	 */
	public static boolean isNormalizedTaxaFile( final File file ) {
		for( final String level: getTaxaLevels() )
			if( file.getName().endsWith( "_" + TAXA_TABLE + "_" + NORMALIZED + "_" + level + Constants.TSV_EXT ) )
				return true;
		return false;
	}

	/**
	 * Check the file name to determine if it is a taxonomy table file.
	 * 
	 * @param file File to test
	 * @return boolean TRUE if file is a taxonomy count file
	 */
	public static boolean isTaxaFile( final File file ) {
		if( file.getName().contains( "_" + TAXA_TABLE + "_" ) ) for( final String level: getTaxaLevels() )
			if( file.getName().endsWith( level + Constants.TSV_EXT ) ) return true;
		return false;
	}

	/**
	 * Get parent level name of the given level parameter.
	 * 
	 * @param level Taxonomy level
	 * @return Parent taxonomy level
	 */
	public static String parentTaxaLevel( final String level ) {
		final int x = allTaxonomyLevels().indexOf( level );
		if( x == 0 ) {
			Log.debug( TaxaUtil.class, level + " is already the highest taxonomy level so has no parent" );
			return null;
		}
		return allTaxonomyLevels().get( x - 1 );

	}

	/**
	 * Return the top configured taxonomy level from
	 * {@link biolockj.Config}.{@value biolockj.Constants#REPORT_TAXONOMY_LEVELS}
	 * 
	 * @return Taxonomy level
	 */
	public static String topTaxaLevel() {
		if( topLevel == null ) topLevel = getTaxaLevels().get( 0 );
		return topLevel;
	}
	

	/**
	 * Read a table of counts, formatted with samples as rows (ids in first column) and taxa as columns (ids in header).
	 * @param taxaTable
	 * @return Map linking sample name to map that links taxa IDs to value.
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws BioLockJException
	 */
	public static TaxaLevelTable readTaxaTable(final File taxaTable) throws FileNotFoundException, IOException, BioLockJException{
		TaxaLevelTable data = new TaxaLevelTable(TaxaUtil.getTaxonomyTableLevel( taxaTable ));
		final List<String> otuNames = new ArrayList<>();
		boolean foundBigValues = false;
		
		final BufferedReader reader = BioLockJUtil.getFileReader( taxaTable );
		try {
			otuNames.addAll( getOtuNames( reader.readLine() ) );
			String nextLine = reader.readLine();

			while( nextLine != null ) {
				final StringTokenizer st = new StringTokenizer( nextLine, DELIM );
				final String sampleID = st.nextToken();
				final HashMap<String, Double> rowValues = data.newSampleRow( sampleID );
				int i = 0;
				while( st.hasMoreTokens() ) {
					final String nextToken = st.nextToken();
					if( nextToken.length() > 0 ) {
						Double cellValue = new Double( nextToken );
						if ( (cellValue + 1) <= cellValue ) foundBigValues = true;
						rowValues.put( otuNames.get( i ), cellValue);
					}
					i++;
				}
				if ( rowValues.size() != otuNames.size() ) {
					throw new BioLockJException("Header included [" + otuNames.size() + "] taxa, but the row for sample [" + 
				sampleID + "] has [" + rowValues.size() + "] values.");
				}

				nextLine = reader.readLine();
			}
		} finally {
			if( reader != null ) reader.close();
		}
		if (foundBigValues) Log.warn(TaxaUtil.class, "Values in this table may be larger than can be precisely stored as a Double.");
		return data;
	}
	
	/**
	 * Parse Taxa names from the given header line.
	 * 
	 * @param header Head line of table
	 * @return List of Taxa
	 */
	private static List<String> getOtuNames( final String header ) {
		final List<String> otuNames = new ArrayList<>();
		final StringTokenizer st = new StringTokenizer( header, DELIM );
		st.nextToken(); // skip ID & then strip quotes
		while( st.hasMoreTokens() )
			otuNames.add( BioLockJUtil.removeOuterQuotes( st.nextToken() ) );
		return otuNames;
	}
	

	/**
	 * Write transformed data to file
	 * 
	 * @param outFile File to write to
	 * @param sampleNames Sample names to include
	 * @param taxaNames Taxa names to include
	 * @param taxaCounts transformed Taxa counts
	 * @throws Exception if errors occur
	 */
	public static void writeDataToFile( final File outFile, final List<String> sampleNames,
		final List<String> taxaNames, final TaxaLevelTable taxaCounts ) throws Exception {
		final BufferedWriter writer = new BufferedWriter( new FileWriter( outFile ) );

		writer.write( MetaUtil.getID() );

		for( final String s: taxaNames ) writer.write( DELIM + s );

		for( String sampleID : sampleNames ) {
			writer.write( Constants.RETURN );
			writer.write( sampleID );
			for( String taxaID : taxaNames ) {
				writer.write( DELIM + taxaCounts.get( sampleID ).get( taxaID ) );
			}	
		}

		writer.close();
	}
	
	public static void writeDataToFile( final File outFile, final TaxaLevelTable taxaCounts ) throws Exception {
		writeDataToFile(outFile, taxaCounts.listSamples(), taxaCounts.listTaxa(), taxaCounts);
	}


	/**
	 * File suffix appended to normalized taxa count tables: {@value #NORMALIZED}
	 */
	public static final String NORMALIZED = "norm";

	/**
	 * Included in the file name of each file output. One file per sample is output by the ParserModule.
	 */
	protected static final String TAXA_TABLE = "taxaCount";

	private static String bottomLevel = null;
	private static List<String> configLevels = null;
	private static List<String> levelSpan = null;
	private static final List<String> TAXA_LEVELS = Arrays.asList( Constants.DOMAIN, Constants.PHYLUM, Constants.CLASS,
		Constants.ORDER, Constants.FAMILY, Constants.GENUS, Constants.SPECIES );
	private static String topLevel = null;
	
	private static final String DELIM = Constants.TAB_DELIM;
	public static final String FILE_EXT = Constants.TSV_EXT;
	
}
