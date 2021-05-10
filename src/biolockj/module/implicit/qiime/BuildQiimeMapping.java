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
package biolockj.module.implicit.qiime;

import java.io.*;
import java.util.*;
import biolockj.*;
import biolockj.module.ScriptModule;
import biolockj.module.SeqModuleImpl;
import biolockj.util.*;

/**
 * This BioModule converts the metadata file into a tab delimited QIIME mapping file (if provided).<br>
 * The QIIME mapping file is validated by calling QIIME script validate_mapping_file.py<br>
 * <p>
 * QIIME mapping file requirements:
 * <ol>
 * <li>#SampleID = Required name of 1st column, holds sample unique identifiers, not nullable
 * <li>BarcodeSequence = Required name of 2nd column, is nullable
 * <li>LinkerPrimerSequence = Required name of 3rd column, is nullable
 * <li>InputFileName = Column is required by add_qiime_labels.py, containing demultiplexed file names, not nullable
 * <li>Description = Required name of last column, not nullable
 * </ol>
 * <p>
 * Steps to convert metadata into QIIME mapping:
 * <ol>
 * <li>Reorder existing QIIME mapping columns, if any are out of order
 * <li>Rename the 1st column (containing sample IDs) to the required value: #SampleID
 * <li>Add missing QIIME mapping columns, if any are missing
 * <li>Rename the validated mapping file to the original metadata file name and save to the module output directory.
 * </ol>
 */
public class BuildQiimeMapping extends SeqModuleImpl {
	/**
	 * Create QIIME mapping based on metadata file, output to temp dir. Add required fields if missing.
	 *
	 * @return New Metadata file
	 * @throws Exception if unable to build the mapping file
	 */
	public File addMissingFields() throws Exception {
		final boolean hasQm1 = MetaUtil.getFieldNames().contains( Constants.QIIME_BARCODE_SEQ_COL );
		final boolean hasQm2 = MetaUtil.getFieldNames().contains( Constants.QIIME_LINKER_PRIMER_SEQ_COL );
		final boolean hasQm3 = MetaUtil.getFieldNames().contains( Constants.QIIME_DEMUX_COL );
		final boolean hasQm4 = MetaUtil.getFieldNames().contains( Constants.QIIME_DESC_COL );

		final Map<String, String> metaLines = new HashMap<>();
		String header = null;
		final BufferedReader reader = BioLockJUtil.getFileReader( MetaUtil.getMetadata() );
		for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
			final StringTokenizer st = new StringTokenizer( line, TAB_DELIM );
			metaLines.put( st.nextToken(), line );
			if( header == null ) {
				Log.info( getClass(), "Initial metadata columns = " + line );
				header = line;
			}
		}
		reader.close();

		MetaUtil.setFile( getQiimeMapping() );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( MetaUtil.getMetadata() ) );
		writer.write( QIIME_ID );
		if( !hasQm1 ) {
			Log.info( getClass(), "Add required column(2) field = " + Constants.QIIME_BARCODE_SEQ_COL );
			writer.write( TAB_DELIM + Constants.QIIME_BARCODE_SEQ_COL );
		}

		if( !hasQm2 ) {
			Log.info( getClass(), "Add required column(3) field = " + Constants.QIIME_LINKER_PRIMER_SEQ_COL );
			writer.write( TAB_DELIM + Constants.QIIME_LINKER_PRIMER_SEQ_COL );
		}
		final StringTokenizer ht = new StringTokenizer( header, TAB_DELIM );
		ht.nextToken(); // skip the ID column
		while( ht.hasMoreTokens() )
			writer.write( TAB_DELIM + ht.nextToken() );
		if( !hasQm3 ) {
			Log.info( getClass(), "Add required column(n-1) field = " + Constants.QIIME_DEMUX_COL );
			writer.write( TAB_DELIM + Constants.QIIME_DEMUX_COL );
		}

		if( !hasQm4 ) {
			Log.info( getClass(), "Add required column(n) field = " + Constants.QIIME_DESC_COL );
			writer.write( TAB_DELIM + Constants.QIIME_DESC_COL );
		}

		writer.write( RETURN );

		for( final File f: getInputFiles() )
			for( final String key: metaLines.keySet() ) {
				if( !SeqUtil.getSampleId( f ).equals( key ) ) continue;

				writer.write( key + TAB_DELIM );
				if( !hasQm1 ) writer.write( MetaUtil.getNullValue( this ) + TAB_DELIM );

				if( !hasQm2 ) writer.write( MetaUtil.getNullValue( this ) + TAB_DELIM );

				final StringTokenizer st = new StringTokenizer( metaLines.get( key ), TAB_DELIM );
				st.nextToken(); // skip the id
				writer.write( st.nextToken() );
				while( st.hasMoreTokens() )
					writer.write( TAB_DELIM + st.nextToken() );

				if( !hasQm3 ) writer.write( TAB_DELIM + f.getName() );
				// writer.write( TAB_DELIM + key + "." + SeqUtil.FASTA );

				if( !hasQm4 ) writer.write( TAB_DELIM + QIIME_COMMENT );

				writer.write( RETURN );
			}

		writer.close();
		MetaUtil.refreshCache();
		return getQiimeMapping();
	}

	/**
	 * Create the Qiime corrected mapping file by building a new metadata file with ordered columns, and verify the
	 * format via the {@value #SCRIPT_VALIDATE_MAPPING} script.
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception {
		final List<List<String>> data = new ArrayList<>();
		this.initMetaFile = MetaUtil.getMetadata();
		data.add( createQiimeCorrectedMapping() );
		return data;
	}

	/**
	 * Message summarizes contents of {@link biolockj.util.SummaryUtil#getScriptDirSummary(ScriptModule) scriptDir}, and
	 * status of the QIIME mapping files (exists or not).
	 */
	@Override
	public String getSummary() throws Exception {
		final String msg =
			( getOutputDir().listFiles().length == 0 ? "QIIME Mapping not found": "Generated QIIME mapping file" ) +
				RETURN;

		return super.getSummary() + msg;
	}

	/**
	 * This method generates the bash function used to reorder columns as per QIIME requirements:
	 * {@value #FUNCTION_REORDER_FIELDS}
	 */
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception {
		final List<String> lines = super.getWorkerScriptFunctions();
		if( this.metaColumns == null ) return lines;

		final List<Integer> skip = Arrays.asList( this.metaColumns.indexOf( Constants.QIIME_BARCODE_SEQ_COL ),
			this.metaColumns.indexOf( Constants.QIIME_LINKER_PRIMER_SEQ_COL ),
			this.metaColumns.indexOf( Constants.QIIME_DEMUX_COL ),
			this.metaColumns.indexOf( Constants.QIIME_DESC_COL ) );

		String awkBody = "";
		awkBody += Config.getExe( this, Constants.EXE_AWK ) + " -F'\\" + TAB_DELIM + "' -v OFS=\"\\" + TAB_DELIM +
			"\" '{ print $1," + colIndex( this.metaColumns, Constants.QIIME_BARCODE_SEQ_COL ) + "," +
			colIndex( this.metaColumns, Constants.QIIME_LINKER_PRIMER_SEQ_COL ) + ",";

		for( int i = 1; i < this.metaColumns.size(); i++ )
			if( !skip.contains( i ) ) awkBody += " $" + ( i + 1 ) + ",";

		awkBody += colIndex( this.metaColumns, Constants.QIIME_DEMUX_COL ) + ",";
		awkBody += colIndex( this.metaColumns, Constants.QIIME_DESC_COL );
		awkBody += " }' " + this.initMetaFile.getAbsolutePath() + " > " + getOrderedMapping().getAbsolutePath();

		Log.debug( getClass(), FUNCTION_REORDER_FIELDS + " will update column order using awk --> " + awkBody );

		lines.add( "function " + FUNCTION_REORDER_FIELDS + "() {" );
		lines.add( awkBody );
		lines.add( "}" + RETURN );

		return lines;

	}

	/**
	 * Create the bash script line that will save a copy of the validated mapping file into the output directory,
	 * renamed to the file name configured in {@link biolockj.util.MetaUtil#META_FILE_PATH
	 * Config}.{@value biolockj.util.MetaUtil#META_FILE_PATH}
	 *
	 * @return Bash script line to save mapping file to output dir
	 */
	private String copyMappingToOutputDir() {
		return " cp " + getMappingDir() + "*" + VALIDATED_MAPPING + " " + getOutputDir().getAbsolutePath() +
			File.separator + MetaUtil.getFileName();
	}

	/**
	 * Create QIIME Mapping file with columns arranged as required by QIIME. Build bash script line calling
	 * {@value #SCRIPT_VALIDATE_MAPPING} to verify file format. The updated metadata file, containing all fields, is
	 * saved to initMetaFile for use in the bash script function used to reorder the columns.
	 *
	 * @return Bash script lines to create the corrected mapping file
	 * @throws Exception if unable to build the mapping or return the script lines
	 */
	private List<String> createQiimeCorrectedMapping() throws Exception {
		Log.info( getClass(), "Create QIIME Specific Mapping File" );
		final List<String> lines = new ArrayList<>();
		this.initMetaFile = addMissingFields();
		this.metaColumns = getMetaCols();
		if( this.metaColumns != null ) {
			Log.info( getClass(), "Metadata column order:" );
			int d = 0;
			for( final String col: this.metaColumns )
				Log.info( getClass(), "col[" + d++ + "] = " + col );

			lines.add( FUNCTION_REORDER_FIELDS );
			MetaUtil.setFile( getOrderedMapping() );
		} else Log.info( getClass(), "Qiime mapping file columns already in order!" );

		lines.add( validateMapping() );
		lines.add( copyMappingToOutputDir() );
		return lines;
	}

	/**
	 * Get mapping dir, called "mapping" which is the directory the new mapping is output by Qiime
	 * {@value #SCRIPT_VALIDATE_MAPPING}.
	 *
	 * @return Path to {@value #SCRIPT_VALIDATE_MAPPING} output dir
	 */
	private String getMappingDir() {
		final File dir = new File( getTempDir().getAbsolutePath() + File.separator + "mapping" );
		if( !dir.isDirectory() ) dir.mkdirs();

		return dir.getAbsolutePath() + File.separator;
	}

	private File getOrderedMapping() {
		final File orderedDir = new File( getTempDir().getAbsolutePath() + File.separator + "orderedColumns" );
		if( !orderedDir.isDirectory() ) orderedDir.mkdirs();

		return new File( orderedDir.getAbsolutePath() + File.separator + MetaUtil.getFileName() );
	}

	private File getQiimeMapping() {
		return new File( getTempDir().getAbsolutePath() + File.separator + MetaUtil.getFileName() );
	}

	/**
	 * Call validate_mapping_file.py to get corrected QIIME Mapping.
	 */
	private String validateMapping() {
		return SCRIPT_VALIDATE_MAPPING + " -pbm " + MetaUtil.getPath() + " -o " + getMappingDir() + " -j " +
			Constants.QIIME_DEMUX_COL;
	}

	private static String colIndex( final List<String> cols, final String name ) {
		return " $" + ( cols.indexOf( name ) + 1 );
	}

	private static List<String> getMetaCols() {
		final List<String> cols = new ArrayList<>();
		cols.add( MetaUtil.getID() );
		cols.addAll( MetaUtil.getFieldNames() );

		if( cols.indexOf( Constants.QIIME_BARCODE_SEQ_COL ) == 1 &&
			cols.indexOf( Constants.QIIME_LINKER_PRIMER_SEQ_COL ) == 2 &&
			cols.indexOf( Constants.QIIME_DEMUX_COL ) == cols.size() - 2 &&
			cols.indexOf( Constants.QIIME_DESC_COL ) == cols.size() - 1 ) return null;

		return cols;
	}
	
	@Override
	public String getDockerImageOwner() {
		return Constants.MAIN_DOCKER_OWNER;
	}
	
	@Override
	public String getDockerImageName() {
		return "qimme_classifier";
	}
	
	@Override
	public String getDockerImageTag() {
		return "v1.3.18";
	}

	private File initMetaFile = null;
	private List<String> metaColumns = null;

	/**
	 * Name of the bash function that reorders metadata columns: {@value #FUNCTION_REORDER_FIELDS}
	 */
	protected static final String FUNCTION_REORDER_FIELDS = "reorderColumns";

	/**
	 * Comment used to populate {@value biolockj.Constants#QIIME_DESC_COL} column
	 */
	protected static final String QIIME_COMMENT = "BioLockJ Generated Mapping";

	/**
	 * QIIME mapping file required 1st column name, containing the sample ID
	 */
	protected static final String QIIME_ID = "#SampleID";

	/**
	 * QIIME script used to validate the QIIME mapping file format
	 */
	protected static final String SCRIPT_VALIDATE_MAPPING = "validate_mapping_file.py";

	/**
	 * Suffix appended to the validate QIIME mapping file output to temp/mapping dir by bash script
	 */
	protected static final String VALIDATED_MAPPING = "_corrected" + TXT_EXT;
}
