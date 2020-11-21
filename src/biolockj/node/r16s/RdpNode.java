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
package biolockj.node.r16s;

import java.util.StringTokenizer;
import biolockj.Config;
import biolockj.Constants;
import biolockj.node.OtuNodeImpl;

/**
 * This class represents one line of {@link biolockj.module.classifier.r16s.RdpClassifier} output. RDP outputs the
 * sequence header and taxonomy assignments for all levels in one line. Each line represents a single read. Each level
 * is followed by a confidence score ranging from 0-1, which can be obtained via {@link #getScore()}.
 * <p>
 * Sample output line from RDP classifier: <br>
 * FCABK7W:1:2105:21787:12788#/1 Root rootrank 1.0 Bacteria domain 1.0 Firmicutes phylum 1.0 Clostridia class 1.0
 * Clostridiales order 1.0 Ruminococcaceae family 1.0 Faecalibacterium genus 1.0
 */
public class RdpNode extends OtuNodeImpl {
	/**
	 * Constructor called one line of RDP output.
	 *
	 * @param id Sample ID
	 * @param line RDP Classifier output line
	 * @throws Exception if propagated from {@link #buildRdpNode(String, String)}
	 */
	public RdpNode( final String id, final String line ) throws Exception {
		buildRdpNode( id, line );
	}

	/**
	 * Return lowest RDP confidence percentage (0-100) above the configured threshold value. If no valid levels are
	 * found, return 0.
	 *
	 * @return Integer between 0 and 100
	 */
	public int getScore() {
		return this.score;
	}

	/**
	 * Build the OtuNode by extracting the OTU names for each level from the line. Only OTUs with score above the
	 * threshold are included. Score is set to the lowest score above {@value biolockj.Constants#RDP_THRESHOLD_SCORE}.
	 * If id = null, extract the ID from the 1st token in the line. Remove single and double quotes if found around OTU
	 * name.
	 *
	 * @param id Sample ID
	 * @param line RDP Classifier output line
	 * @throws Exception if required properties are invalid or undefined
	 */
	protected void buildRdpNode( final String id, final String line ) throws Exception {
		if( line == null || id == null || line.isEmpty() || id.isEmpty() ) return;

		final StringTokenizer st = new StringTokenizer( line, Constants.TAB_DELIM );
		setSampleId( id );
		setLine( line );
		setCount( 1 );

		// fisrt token is the squence id
		st.nextToken();
		
		while( st.hasMoreTokens() ) {
			String taxa = getTaxaName( st.nextToken() );
			//Log.debug(this.getClass(), "Found potential taxa name: " + taxa);
			while( st.hasMoreTokens() && ( taxa.equals( "-" ) || taxa.equals( "" ) ) ) {
				//Log.debug(this.getClass(), "Taxa name [" + taxa + "] is stupid, take next token."); 
				taxa = getTaxaName( st.nextToken() );
			}	

			final String level = st.hasMoreTokens() ? st.nextToken().trim(): null;
			final Integer nextScore = st.hasMoreTokens() ? calculateScore( st.nextToken().trim() ): null;

			if( level == null || nextScore == null ||
				nextScore < Config.requirePositiveInteger( null, Constants.RDP_THRESHOLD_SCORE ) ) {
				return;
			}else {
				this.score = nextScore;
				addTaxa( taxa, level );
			}
		}
	}

	/**
	 * Returns default score = 0 if no valid levels are found, otherwise return a score between 1 - 100.
	 *
	 * @return the lowest score above the RDP (between 0 and 100)
	 * @Exception if scoreString has an invalid format
	 */
	private static Integer calculateScore( final String scoreString ) throws Exception {
		String confScore = scoreString;
		if( confScore.equals( "1" ) || confScore.equals( "1.0" ) ) return 100;
		else if( confScore.equals( "0" ) || confScore.equals( "0.0" ) ) return 0;
		else if( !confScore.startsWith( "0." ) ) throw new Exception( rangeError( confScore ) );
		confScore = scoreString.replace( "0.", "" );
		if( confScore.length() == 1 ) confScore += "0";
		final Integer thisScore = Integer.parseInt( confScore );
		if( thisScore < 0 || thisScore > 100 ) throw new Exception( rangeError( confScore ) );
		return thisScore;
	}

	private static String getTaxaName( final String taxa ) {
		return taxa.replaceAll( "'", "" ).replaceAll( "\"", "" ).trim();
	}

	private static String rangeError( final String score ) {
		return "Invalid RDP confidence score | Required range [ 0.0 <= score <= 1.0 ] ---> Actual score = " + score;
	}

	private int score = 0;

	// Override default taxonomy level delimiters set in OtuNodeImpl
	static {
		DOMAIN_DELIM = Constants.DOMAIN;
		CLASS_DELIM = Constants.CLASS;
		FAMILY_DELIM = Constants.FAMILY;
		GENUS_DELIM = Constants.GENUS;
		ORDER_DELIM = Constants.ORDER;
		PHYLUM_DELIM = Constants.PHYLUM;
		SPECIES_DELIM = Constants.SPECIES;
	}
}
