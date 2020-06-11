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
import biolockj.*;
import biolockj.api.ApiModule;
import biolockj.module.implicit.parser.ParserModuleImpl;
import biolockj.node.*;
import biolockj.node.wgs.KrakenNode;
import biolockj.util.*;

/**
 * This BioModules parses KrakenClassifier output reports to build standard OTU abundance tables.
 * 
 * @blj.web_desc Kraken Parser
 */
public class KrakenParser extends ParserModuleImpl implements ApiModule {
	
	public KrakenParser() {
		super();
		addGeneralProperty( Constants.REPORT_UNCLASSIFIED_TAXA );
	}

	@Override
	public void addOtuNode( final OtuNode node ) throws Exception {
		if( isValid( node ) ) {
			final ParsedSample sample = getParsedSample( node.getSampleId() );
			if( sample == null ) addParsedSample( new ParsedSample( node ) );
			else sample.addNode( node );
		}
	}

	/**
	 * Parse all {@link biolockj.module.classifier.wgs.KrakenClassifier} reports in the input directory.<br>
	 * Cache the leaf counts Build an {@link biolockj.node.wgs.KrakenNode} for each line.<br>
	 * If {@link #isValid(OtuNode)},<br>
	 * <ol>
	 * <li>Create {@link biolockj.node.ParsedSample} for the {@link biolockj.node.wgs.KrakenNode#getSampleId()} if not
	 * yet created.
	 * <li>Add the {@link biolockj.node.wgs.KrakenNode#getCount()} (1) to {@link biolockj.node.ParsedSample} OTU count.
	 * </ol>
	 * <p>
	 * Sample Kraken report line (head 7A_reported.tsv) :<br>
	 * FCC6MMAACXX:8:1101:1968:2100#GTATTCTC/1
	 * d__Bacteria|p__Bacteroidetes|c__Bacteroidia|o__Bacteroidales|f__Bacteroidaceae|g__Bacteroides|s__Bacteroides_vulgatus
	 */
	@Override
	public void parseSamples() throws Exception {
		for( final File file: getInputFiles() ) parseSample( file );
	}

	/**
	 * Construct a new KrakenNode
	 * 
	 * @param file KrakenClassifier output file
	 * @throws Exception if any errors occur
	 */
	protected void parseSample( final File file ) throws Exception {
		final BufferedReader reader = BioLockJUtil.getFileReader( file );
		try {
			for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
				final OtuNode node = new KrakenNode( SeqUtil.getSampleId( file ), line );
				if( node.getTaxaMap() == null || node.getTaxaMap().get( TaxaUtil.topTaxaLevel() ) == null ) {
					Log.debug( getClass(), "Skip OTU missing top taxa level: " + line );
					continue;
				}
				if( Config.getBoolean( this, Constants.REPORT_UNCLASSIFIED_TAXA ) ) {
					String taxa = null;
					String parentLevel = null;
					for( final String level: TaxaUtil.getTaxaLevelSpan() ) {
						if( node.getTaxaMap().get( level ) == null )
							node.getTaxaMap().put( level, TaxaUtil.getUnclassifiedTaxa( taxa, parentLevel ) );
						else	 {
							taxa = node.getTaxaMap().get( level );
							parentLevel = level;
						}
					}
				}

				addOtuNode( node );
			}
		} finally {
			if( reader != null ) reader.close();
		}
	}

	@Override
	public String getDescription() {
		return "Build OTU tables from [KRAKEN](http://ccb.jhu.edu/software/kraken/) mpa-format reports.";
	}

	@Override
	public String getCitationString() {
		return "Module developed by Mike Sioda" + System.lineSeparator() + "BioLockJ " + BioLockJUtil.getVersion();
	}
}
