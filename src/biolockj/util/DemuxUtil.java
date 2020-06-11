/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Mar 19, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.util;

import java.util.*;
import biolockj.*;
import biolockj.Properties;
import biolockj.api.API_Exception;
import biolockj.exception.ConfigNotFoundException;

/**
 * This utility contains standard methods used to handle multiplexed data.
 */
public class DemuxUtil {

	/**
	 * Return TRUE if Config is setup to demultiplex the sequence data based on bar-codes in the sequence headers.
	 * 
	 * @return boolean
	 * @throws ConfigNotFoundException if {@value #DEMUX_STRATEGY} is undefined
	 */
	public static boolean barcodeInHeader() throws ConfigNotFoundException {
		return Config.getString( null, DEMUX_STRATEGY ) != null &&
			Config.requireString( null, DEMUX_STRATEGY ).equals( OPTION_BARCODE_IN_HEADER );
	}

	/**
	 * Return TRUE if Config is setup to demultiplex the sequence data based on bar-codes in the sequence itself.
	 * 
	 * @return boolean
	 * @throws ConfigNotFoundException if {@value #DEMUX_STRATEGY} is undefined
	 */
	public static boolean barcodeInMapping() throws ConfigNotFoundException {
		return Config.getString( null, DEMUX_STRATEGY ) != null &&
			Config.requireString( null, DEMUX_STRATEGY ).equals( OPTION_BARCODE_IN_MAPPING );
	}

	/**
	 * Return TRUE if Config is setup to demultiplex the sequence data based on bar-codes in the sequence itself.
	 * 
	 * @return boolean
	 * @throws ConfigNotFoundException if {@value #DEMUX_STRATEGY} is undefined
	 */
	public static boolean barcodeInSeq() throws ConfigNotFoundException {
		return Config.getString( null, DEMUX_STRATEGY ) != null &&
			Config.requireString( null, DEMUX_STRATEGY ).equals( OPTION_BARCODE_IN_SEQ );
	}

	/**
	 * Clear demultiplexer related fields
	 */
	public static void clearDemuxConfig() {
		Config.setConfigProperty( MetaUtil.META_BARCODE_COLUMN, "" );
		Config.setConfigProperty( DemuxUtil.BARCODE_CUTOFF, "" );
		Config.setConfigProperty( DemuxUtil.BARCODE_USE_REV_COMP, "" );
		Config.setConfigProperty( DemuxUtil.DEMUX_STRATEGY, "" );
		Config.setConfigProperty( DemuxUtil.MAPPING_FILE, "" );
	}

	/**
	 * Return TRUE if barcode column is defined in the Config file and is populated in the metadata file
	 * 
	 * @return TRUE or FALSE
	 * @throws Exception if unable to access metadata file or Config
	 */
	public static boolean demuxWithBarcode() throws Exception {
		if( ( barcodeInHeader() || barcodeInSeq() || barcodeInMapping() ) &&
			MetaUtil.getFieldNames().contains( Config.requireString( null, MetaUtil.META_BARCODE_COLUMN ) ) &&
			!MetaUtil.getFieldValues( Config.requireString( null, MetaUtil.META_BARCODE_COLUMN ), true ).isEmpty() )
			return true;
		return false;
	}

	/**
	 * Return TRUE if Config is setup to demultiplex the sequence data.
	 * 
	 * @return boolean
	 * @throws ConfigNotFoundException if {@value #DEMUX_STRATEGY} is undefined
	 */
	public static Boolean doDemux() throws ConfigNotFoundException {
		if( Config.getString( null, DEMUX_STRATEGY ) == null ) return null;
		else if( !Config.requireString( null, DEMUX_STRATEGY ).equals( OPTION_DO_NOT_DEMUX ) ) return true;
		return false;
	}

	/**
	 * Determine Sample Id by examining the sequence lines.<br>
	 * If {@value #DEMUX_STRATEGY }={@value #OPTION_ID_IN_HEADER}, extract the Sample Id from the sequence header via
	 * {@link biolockj.util.SeqUtil#getSampleIdFromString(String)}<br>
	 * If {@value #DEMUX_STRATEGY }={@value #OPTION_BARCODE_IN_HEADER} and the sequence header contains a bar-code in
	 * the idMap, return the corresponding SampleID from the idMap.<br>
	 * If {@value #DEMUX_STRATEGY }={@value #OPTION_BARCODE_IN_MAPPING} and the sequence header maps to a bar-code in
	 * the idMap, return the corresponding SampleID from the idMap.<br>
	 * If {@value #DEMUX_STRATEGY }={@value #OPTION_BARCODE_IN_SEQ} and the sequence itself begins with a bar-code in
	 * the idMap, return the corresponding SampleID from the idMap.<br>
	 * 
	 * @param seqLines List of lines for one fasta or fatsq read
	 * @return Sample ID or null
	 * @throws Exception if propagated from {@link biolockj.util.SeqUtil} or {@link biolockj.Config}
	 */
	public static String getSampleId( final List<String> seqLines ) throws Exception {
		if( demuxWithBarcode() ) {
			final Map<String, String> map = getIdMap();
			if( map != null ) for( final String barCodeId: map.keySet() )
				if( ( barcodeInHeader() || barcodeInMapping() ) && seqLines.get( 0 ).contains( barCodeId ) ||
					barcodeInSeq() && seqLines.get( 1 ).startsWith( barCodeId ) ) return map.get( barCodeId );
			return null;
		}
		return SeqUtil.getSampleIdFromString( seqLines.get( 0 ) );
	}

	/**
	 * Check for the existance of the barcode column.
	 * 
	 * @return Boolean TRUE if barcode column exists and is populated.
	 */
	public static boolean hasValidBarcodes() {
		try {
			final String barCodeCol = Config.getString( null, MetaUtil.META_BARCODE_COLUMN );
			if( barCodeCol != null && MetaUtil.getFieldNames().contains( barCodeCol ) ) {
				final Set<String> sampleIds = new HashSet<>( MetaUtil.getSampleIds() );
				final Set<String> vals = new HashSet<>( MetaUtil.getFieldValues( barCodeCol, true ) );
				if( sampleIds.size() == vals.size() ) return true;
				Log.warn( DemuxUtil.class,
					"Multiplexer adding Sample ID to sequence headers instead of barcode because dataset contains " +
						sampleIds.size() + " unique Sample IDs but only " + vals.size() + " unique barcodes" );
				for( final String id: MetaUtil.getSampleIds() )
					Log.warn( DemuxUtil.class, "ID [ " + id + " ] ==> " + MetaUtil.getField( id, barCodeCol ) );
			}
		} catch( final Exception ex ) {
			Log.error( DemuxUtil.class, "Error occurred checking metadata file for valid barcodes" + ex.getMessage(),
				ex );
		}

		return false;
	}

	/**
	 * Return TRUE if Config is setup to demultiplex the sequence data based on Sample IDs in the sequence headers.
	 * 
	 * @return boolean
	 */
	public static boolean sampleIdInHeader() {
		try {
			return Config.getString( null, DEMUX_STRATEGY ) != null &&
				Config.getString( null, DEMUX_STRATEGY ).equals( OPTION_ID_IN_HEADER ) || !demuxWithBarcode();
		} catch( final Exception ex ) {
			Log.warn( DemuxUtil.class, "Failed to determine demux strategy: " + ex.getMessage() );
			return false;
		}
	}

	/**
	 * Set the {@link biolockj.Config} properties needed to read the sample IDs from a multiplexed file if no barcode is
	 * provided<br>
	 * Set {@link biolockj.Config}.{@value biolockj.Constants#INPUT_TRIM_PREFIX} = 1st sequence header character.<br>
	 * Set {@link biolockj.Config}.{@value biolockj.Constants#INPUT_TRIM_SUFFIX}
	 * ={@value #SAMPLE_ID_SUFFIX_TRIM_DEFAULT}<br>
	 *
	 * @throws Exception if unable to update the property values
	 */
	public static void setMultiplexedConfig() throws Exception {
		if( DemuxUtil.sampleIdInHeader() ) {
			final String defaultSeqHeadChar = Config.requireString( null, Constants.INTERNAL_SEQ_HEADER_CHAR );

			if( Config.getString( null, Constants.INPUT_TRIM_PREFIX ) == null ) {
				Config.setConfigProperty( Constants.INPUT_TRIM_PREFIX, defaultSeqHeadChar );
				Log.info( DemuxUtil.class, "====> Set: " + Constants.INPUT_TRIM_PREFIX + " = " + defaultSeqHeadChar );
			}

			if( Config.getString( null, Constants.INPUT_TRIM_SUFFIX ) == null ) {
				Config.setConfigProperty( Constants.INPUT_TRIM_SUFFIX, SAMPLE_ID_SUFFIX_TRIM_DEFAULT );
				Log.info( DemuxUtil.class,
					"====> Set: " + Constants.INPUT_TRIM_SUFFIX + " = " + SAMPLE_ID_SUFFIX_TRIM_DEFAULT );
			}
		}
	}

	/**
	 * Return the ID map (key=barcodeID, value=sampleId) Print the map to the log file.<br>
	 * Barcodes, if used, are located in the metadata column defined by property:
	 * {@link biolockj.Config}.{@value biolockj.util.MetaUtil#META_BARCODE_COLUMN}
	 *
	 * @return Id map
	 * @throws Exception if propagated from {@link biolockj.util.MetaUtil} or {@link biolockj.Config}
	 */
	protected static Map<String, String> getIdMap() throws Exception {
		if( !idMap.isEmpty() ) return idMap;

		if( demuxWithBarcode() ) {
			Log.info( DemuxUtil.class,
				"Detected Config." + BARCODE_USE_REV_COMP + " = " + Config.getBoolean( null, BARCODE_USE_REV_COMP ) );

			Log.info( DemuxUtil.class,
				"Building Barcode-SampleID Map using the " +
					( Config.getBoolean( null, BARCODE_USE_REV_COMP ) ? "reverse compliment of ": "" ) +
					"metadata column = " + Config.getString( null, MetaUtil.META_BARCODE_COLUMN ) );
		} else {
			Log.debug( DemuxUtil.class, "Lookup multiplexed Sample IDs in header (no barcodes)" );
			return null;
		}

		for( final String id: MetaUtil.getSampleIds() ) {
			String val = MetaUtil.getField( id, Config.requireString( null, MetaUtil.META_BARCODE_COLUMN ) );

			if( Config.getBoolean( null, BARCODE_USE_REV_COMP ) ) val = SeqUtil.reverseComplement( val );

			idMap.put( val, id );
		}

		for( final String key: idMap.keySet() )
			Log.info( DemuxUtil.class,
				"Barcode-SampleID Map key[ " + key + " ] -->  value[ " + idMap.get( key ) + " ]" );

		return idMap;
	}
	
	public static void registerProps() throws API_Exception {
		Properties.registerProp( BARCODE_CUTOFF, Properties.NUMERTIC_TYPE, "Options: (0.0 - 1.0); if defined, pipeline will fail if the percentage of reads with a barcode is less than this cutoff." );
		Properties.registerProp( BARCODE_USE_REV_COMP, Properties.BOOLEAN_TYPE, "Options: Y/N. Use reverse compliment of metadata.barcodeColumn if demultimplexer.strategy = barcode_in_header or barcode_in_seq." );
		Properties.registerProp( DEMUX_STRATEGY, Properties.STRING_TYPE, "Options: barcode_in_header, barcode_in_seq, id_in_header, do_not_demux." + 
			"If using barcodes, they must be provided in the metadata file within column defined by _metadata.barcodeColumn_." );
	}

	/**
	 * {@link biolockj.Config} property {@value #BARCODE_CUTOFF} will look for barcodes in
	 * {@value biolockj.util.MetaUtil#META_BARCODE_COLUMN} to exist in this percentage of multiplex file.
	 */
	public static final String BARCODE_CUTOFF = "demultiplexer.barcodeCutoff";

	/**
	 * {@link biolockj.Config} boolean property {@value #BARCODE_USE_REV_COMP} will use the reverse compliment of
	 * {@value biolockj.util.MetaUtil#META_BARCODE_COLUMN} to match sample IDs to sequences.
	 */
	public static final String BARCODE_USE_REV_COMP = "demultiplexer.barcodeRevComp";

	/**
	 * {@link biolockj.Config} property {@value #DEMUX_STRATEGY} tells BioLockJ how to match sequences with Sample
	 * IDs.<br>
	 * Options: barcode_in_header, barcode_in_mapping, barcode_in_seq, id_in_header, do_not_demux.<br>
	 * If using barcodes, metadata column {@value biolockj.util.MetaUtil#META_BARCODE_COLUMN} is required.
	 */
	public static final String DEMUX_STRATEGY = "demultiplexer.strategy";

	/**
	 * {@link biolockj.Config} String property {@value #MAPPING_FILE} lists the path to the mapping file to match
	 * sequence headers to barcodes. The barcodes found in metadata file
	 * {@value biolockj.util.MetaUtil#META_BARCODE_COLUMN} can be used to match sample IDs to sequences.
	 */
	public static final String MAPPING_FILE = "demultiplexer.mapping";

	/**
	 * {@link biolockj.Config} property {@value #DEMUX_STRATEGY} option: {@value #OPTION_BARCODE_IN_HEADER}
	 */
	public static final String OPTION_BARCODE_IN_HEADER = "barcode_in_header";

	/**
	 * {@link biolockj.Config} property {@value #DEMUX_STRATEGY} option: {@value #OPTION_BARCODE_IN_MAPPING}
	 */
	public static final String OPTION_BARCODE_IN_MAPPING = "barcode_in_mapping";

	/**
	 * {@link biolockj.Config} property {@value #DEMUX_STRATEGY} option: {@value #OPTION_BARCODE_IN_SEQ}
	 */
	public static final String OPTION_BARCODE_IN_SEQ = "barcode_in_seq";

	/**
	 * {@link biolockj.Config} property {@value #DEMUX_STRATEGY} option: {@value #OPTION_DO_NOT_DEMUX}
	 */
	public static final String OPTION_DO_NOT_DEMUX = "do_not_demux";

	/**
	 * {@link biolockj.Config} property {@value #DEMUX_STRATEGY} option: {@value #OPTION_ID_IN_HEADER}
	 */
	public static final String OPTION_ID_IN_HEADER = "id_in_header";

	/**
	 * Multiplexed files created by BioLockJ may add sample ID to the sequence header if no barcode is provided.<br>
	 * If sample ID is added, it is immediately followed by the character: {@value #SAMPLE_ID_SUFFIX_TRIM_DEFAULT}<br>
	 * This value can be used then to set {@link biolockj.Config}.{@value biolockj.Constants#INPUT_TRIM_SUFFIX}
	 */
	protected static final String SAMPLE_ID_SUFFIX_TRIM_DEFAULT = "_";

	private static final Map<String, String> idMap = new HashMap<>();

}
