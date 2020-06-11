/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 16, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.implicit.parser.wgs;

import java.io.BufferedReader;
import java.io.File;
import java.util.*;
import biolockj.*;
import biolockj.exception.OtuFileException;
import biolockj.node.*;
import biolockj.node.wgs.Kraken2Node;
import biolockj.util.*;

/**
 * This BioModules parses KrakenClassifier output reports to build standard OTU abundance tables.
 * 
 * @blj.web_desc Kraken2 Parser
 */
public class Kraken2Parser extends KrakenParser {
	
	@Override
	protected void parseSample( final File file ) throws Exception {
		final BufferedReader reader = BioLockJUtil.getFileReader( file );
		try {
			for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
				if( discardOtu( line ) ) continue;
				addOtuNode( new Kraken2Node( SeqUtil.getSampleId( file ), line ) );
			}
		} finally {
			if( reader != null ) reader.close();
		}
	}
	
	
	/**
	 * Parse all {@link biolockj.module.classifier.wgs.Kraken2Classifier} reports in the input directory.<br>
	 * Cache the leaf counts Build an {@link biolockj.node.wgs.Kraken2Node} for each line.<br>
	 * If {@link #isValid(OtuNode)},<br>
	 * <ol>
	 * <li>Create {@link biolockj.node.ParsedSample} for the {@link biolockj.node.wgs.Kraken2Node#getSampleId()} if not
	 * yet created.
	 * <li>Add the {@link biolockj.node.wgs.Kraken2Node#getCount()} (1) to {@link biolockj.node.ParsedSample} OTU count.
	 * </ol>
	 * <p>
	 * Sample Kraken report line (head 7A_reported.tsv) :<br>
	 * FCC6MMAACXX:8:1101:1968:2100#GTATTCTC/1
	 * d__Bacteria|p__Bacteroidetes|c__Bacteroidia|o__Bacteroidales|f__Bacteroidaceae|g__Bacteroides|s__Bacteroides_vulgatus
	 */
	@Override
	public void parseSamples() throws Exception {
		for( final File file: getInputFiles() ) {
			setReportUnclassifiedTaxa( false );
			try {
				parseSample( file );
			} finally {
				setReportUnclassifiedTaxa( true );
			}

			addUnclassifiedTaxa( getParsedSample( SeqUtil.getSampleId( file ) ) );
		}
	}
	

	private void addUnclassifiedTaxa( final ParsedSample sample ) throws Exception {
		final Map<String, Long> leafCounts = sample.getOtuCounts();
		report( leafCounts, "Parsed Input Line", false );
		final Map<String, Long> otuCounts = populateInBetweenTaxa( leafCounts );
		final List<String> levels = new ArrayList<>();
		levels.add( TaxaUtil.bottomTaxaLevel() );

		for( final String level: getParentLevels() ) {
			for( final String otu: leafCounts.keySet() ) {
				if( !TaxaUtil.getLeafLevel( otu ).equals( level ) ) continue;
				final Map<String, Long> levTaxa =
					levelCounts( otuCounts, OtuUtil.buildOtuTaxa( level, TaxaUtil.getTaxaName( otu, level ) ) );
				final Long sum = levTaxa.isEmpty() ? 0L: levTaxa.values().stream().mapToLong( Long::longValue ).sum();
				final Long diff = leafCounts.get( otu ) - sum;
				if( diff < 0 ) throw new Exception(
					"Inconsistent OTU counts in Sample [ " + sample.getSampleId() + " ] - Parent OTU \"" + otu +
						"\" (count=" + leafCounts.get( otu ) + ") < sum child taxa (count=" + sum + ")" );
				if( diff > 0 ) otuCounts.put( buildUnclassifiedOtu( otu, levels ), diff );
			}
			levels.add( level );
		}
		sample.setOtuCounts( populateInBetweenTaxa( otuCounts ) );
	}
	
	private static String buildUnclassifiedOtu( final String otu, final List<String> levels ) {
		String gapOtu = otu;
		final String taxa = otu.substring( otu.lastIndexOf( Constants.OTU_SEPARATOR ) + 1 );
		final String name = taxa.substring( taxa.indexOf( Constants.DELIM_SEP ) + Constants.DELIM_SEP.length() );
		final String level = taxa.substring( 0, taxa.indexOf( Constants.DELIM_SEP ) );
		for( int i = levels.size() - 1; i >= 0; i-- )
			gapOtu += Constants.OTU_SEPARATOR +
				OtuUtil.buildOtuTaxa( levels.get( i ), TaxaUtil.getUnclassifiedTaxa( name, level ) );
		return gapOtu;
	}
	
	private boolean discardOtu( final String line ) {
		for( final String delim: getDiscardLevelDelims() )
			if( line.contains( delim ) ) {
				Log.debug( getClass(), "Discard Line [" + line + "] - due to invalid level: " +
					OtuNodeImpl.delimToLevelMap().get( delim ) );
				return true;
			}
		return false;
	}
	
	private static Map<String, Long> populateInBetweenTaxa( final Map<String, Long> otuCounts ) throws OtuFileException {
		final Map<String, Long> map = new TreeMap<>();
		final Map<String, Long> changes = new TreeMap<>();
		for( final String otu: otuCounts.keySet() ) {
			if( !otu.contains( TaxaUtil.bottomTaxaLevel() ) ) continue;
			final StringBuffer sb = new StringBuffer();
			String taxa = null;
			String gapTaxa = null;
			boolean foundGap = false;
			for( final String level: TaxaUtil.getTaxaLevelSpan() ) {
				if( TaxaUtil.getTaxaName( otu, level ) == null ) {
					if( gapTaxa != null ) taxa = gapTaxa;
					else if( taxa != null ) {
						gapTaxa = TaxaUtil.getUnclassifiedTaxa( taxa, parentLevel( level ) );
						taxa = gapTaxa;
						foundGap = true;
					} else throw new OtuFileException( "Programming error, OTU path missing " +
						TaxaUtil.topTaxaLevel() +
						" in populateInBetweenTaxa( otuCounts ) --> OTUs missing the top level should not be found in any ParsedSample." );
				} else {
					taxa = TaxaUtil.getTaxaName( otu, level );
					gapTaxa = null;
				}
				sb.append( ( sb.length() > 0 ? Constants.OTU_SEPARATOR: "" ) + OtuUtil.buildOtuTaxa( level, taxa ) );
			}
			map.put( sb.toString(), otuCounts.get( otu ) );
			if( foundGap ) changes.put( sb.toString(), otuCounts.get( otu ) );
		}
		report( changes, "BioLockJ filled OTU gap", true );
		return map;
	}

	
	private static List<String> getDiscardLevelDelims() {
		final List<String> levelDelims = new ArrayList<>();
		boolean foundBottomLevel = false;
		final Map<String, String> levelToDelim = new HashMap<>();
		for( final String delim: OtuNodeImpl.delimToLevelMap().keySet() )
			levelToDelim.put( OtuNodeImpl.delimToLevelMap().get( delim ), delim );

		for( final String level: TaxaUtil.allTaxonomyLevels() ) {
			if( foundBottomLevel ) levelDelims.add( levelToDelim.get( level ) );
			if( TaxaUtil.bottomTaxaLevel().equals( level ) ) foundBottomLevel = true;
		}

		return levelDelims;
	}
	
	private static void report( final Map<String, Long> otuCounts, final String msg, final boolean printInfo ) {
		for( final String otu: otuCounts.keySet() )
			if( printInfo ) Log.info( Pipeline.exeModule().getClass(), msg + ": " + otu + " --> " + otuCounts.get( otu ) );
			else Log.debug( Pipeline.exeModule().getClass(), msg + ": " + otu + " --> " + otuCounts.get( otu ) );
	}
	
	private static void setReportUnclassifiedTaxa( final boolean enable ) {
		final boolean logStatus = Log.logsEnabled();
		Log.enableLogs( false );
		final String modProp = Config.getModulePropName( Pipeline.exeModule(), Constants.REPORT_UNCLASSIFIED_TAXA );
		Config.setConfigProperty( modProp, enable ? Constants.TRUE: Constants.FALSE );
		Log.enableLogs( logStatus );
	}


	private static String parentLevel( final String level ) {
		return TaxaUtil.allTaxonomyLevels().get( TaxaUtil.allTaxonomyLevels().indexOf( level ) - 1 );
	}
	
	private static Map<String, Long> levelCounts( final Map<String, Long> otuCounts, final String name ) {
		final Map<String, Long> map = new TreeMap<>();
		for( final String otu: otuCounts.keySet() )
			if( otu.contains( name + Constants.OTU_SEPARATOR ) ) map.put( otu, otuCounts.get( otu ) );
		return map;
	}
	

	private static List<String> getParentLevels() {
		final List<String> levels = new ArrayList<>();
		levels.addAll( TaxaUtil.getTaxaLevelSpan() );
		levels.remove( TaxaUtil.bottomTaxaLevel() );
		Collections.reverse( levels );
		return levels;
	}

}
