/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 9, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.util;

import java.io.*;
import java.util.*;
import biolockj.*;
import biolockj.Properties;
import biolockj.api.API_Exception;
import biolockj.exception.BioLockJException;
import biolockj.exception.ConfigFormatException;
import biolockj.exception.ConfigPathException;
import biolockj.exception.ConfigViolationException;
import biolockj.exception.DockerVolCreationException;
import biolockj.exception.MetadataException;
import biolockj.module.BioModule;

/**
 * This utility is used to read, modify, or create a metadata file for the sequence data. The 1st row must hold the
 * Sample ID and column names must be unique. Metadata information is cached in this class for quick access throughout
 * the application.
 */
public class MetaUtil {
	// Prevent instantiation
	private MetaUtil() {}

	/**
	 * Adds a column to the metadata file. The updated metadata file is output to the fileDir. Once the new file is
	 * created, the new file is cached in memory and used as the new metadata.
	 *
	 * @param colName Name of new column
	 * @param map Map relates Sample ID to a field value
	 * @param fileDir File representing output directory for new metadata file
	 * @param removeMissingIds if TRUE, sampleIds not include in the map arg will be removed from the metadata
	 * @throws MetadataException if errors occur attemptingto get/put metadata into cache
	 * @throws IOException if errors occur attempting to read metadata file
	 * @throws DockerVolCreationException 
	 * @throws FileNotFoundException if metadata file not found
	 */
	public static void addColumn( final String colName, final Map<String, String> map, final File fileDir,
		final boolean removeMissingIds ) throws MetadataException, IOException, DockerVolCreationException {
		final File newMeta = new File( fileDir.getAbsolutePath() + File.separator + getFileName() );
		Log.info( MetaUtil.class, "Adding new field [" + colName + "] to metadata: " + newMeta.getAbsolutePath() );
		Log.debug( MetaUtil.class, "Current metadata: " + getPath() );
		if( getFieldNames().contains( colName ) ) {
			Log.warn( MetaUtil.class, "Metadata column [" + colName + "] already exists in: " + getPath() );
			return;
		}

		final Set<String> sampleIds = map.keySet();
		final BufferedReader reader = BioLockJUtil.getFileReader( getMetadata() );
		setFile( newMeta );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( getMetadata() ) );
		try {
			writer.write( reader.readLine() + DEFAULT_COL_DELIM + colName + Constants.RETURN );
			for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
				final StringTokenizer st = new StringTokenizer( line, DEFAULT_COL_DELIM );
				final String id = st.nextToken();
				if( sampleIds.contains( id ) )
					writer.write( line + DEFAULT_COL_DELIM + map.get( id ) + Constants.RETURN );
				else if( !removeMissingIds )
					writer.write( line + DEFAULT_COL_DELIM + getNullValue( null ) + Constants.RETURN );
				else Log.warn( MetaUtil.class, getRemoveIdMsg( id ) );

			}
		} finally {
			reader.close();
			writer.close();
			refreshCache();
		}
	}

	/**
	 * Check if metadata file exists
	 *
	 * @return TRUE if metadata exists
	 */
	public static boolean exists() {
		try {
			return getMetadata() != null;
		} catch( final Exception ex ) {
			Log.error( MetaUtil.class, "Error occurred trying to dtermine if metadata file exists on file sytsem", ex );
		}
		return false;
	}

	/**
	 * Determine if the given field has only unique values.
	 * 
	 * @param field Metadata column
	 * @param ignoreNulls if TRUE ignore duplicate {@value #META_NULL_VALUE} values
	 * @return boolean if field values are unique
	 * @throws MetadataException if field not found in metadata file
	 */
	public static boolean fieldValuesAreUnique( final String field, final boolean ignoreNulls )
		throws MetadataException {
		return getFieldValues( field, ignoreNulls ).size() != getUniqueFieldValues( field, ignoreNulls ).size();
	}

	/**
	 * Get metadata field value for given sampleId.
	 *
	 * @param sampleId Sample ID
	 * @param field Field Name (column name in metadata file)
	 * @return Metadata field value
	 * @throws MetadataException if field not found in the metadata for the given sample Id.
	 */
	public static String getField( final String sampleId, final String field ) throws MetadataException {
		if( !getFieldNames().contains( field ) )
			throw new MetadataException( "Invalid field [" + field + "] not found in Metadata = " + getPath() );

		if( getRecord( sampleId ) == null )
			throw new MetadataException( "Invalid Sample ID [" + sampleId + "] not found in Metadata = " + getPath() );

		return getRecord( sampleId ).get( getFieldNames().indexOf( field ) );
	}

	/**
	 * Get a list of all metadata fields (metadata file column names except the 1st).
	 *
	 * @return List of metadata column names, excluding the 1st column
	 * 
	 */
	public static List<String> getFieldNames() {
		try {
			return getRecord( metaId );
		} catch( final MetadataException ex ) {
			Log.error( MetaUtil.class, "Error occurred accessing Config property: " + META_FILE_PATH, ex );
		}
		return new ArrayList<>();
	}

	/**
	 * Get metadata column for given field name.
	 *
	 * @param field Column name
	 * @param ignoreNulls if TRUE ignore duplicate {@value #META_NULL_VALUE} values
	 * @return List of Column values for sample ID
	 * @throws MetadataException if unable to find metadata filed values
	 */
	public static List<String> getFieldValues( final String field, final boolean ignoreNulls )
		throws MetadataException {
		final List<String> vals = new ArrayList<>();
		if( !getFieldNames().contains( field ) )
			throw new MetadataException( "Invalid field [" + field + "] in Metadata = " + getPath() );

		for( final String id: getSampleIds() ) {
			final String val = getField( id, field );
			if( val != null && val.trim().length() > 0 && !val.equals( getNullValue( null ) ) || !ignoreNulls )
				vals.add( val );
		}

		return vals;
	}

	/**
	 * Get the metadata file name, if it exists, otherwise return projectName.tsv
	 *
	 * @return Name of metadata file, or a default name if no metadata file exists #throws Exception if errors occur
	 */
	public static String getFileName() {
		try {
			if( getMetadata() != null ) return getMetadata().getName();
		} catch( final Exception ex ) {
			Log.error( MetaUtil.class, "Error occurred accessing Config property: " + META_FILE_PATH, ex );
		}

		return Config.pipelineName() + "_metadata" + Constants.TSV_EXT;
	}

	/**
	 * Used to generate a guaranteed to be unique column name. If parameter "name" already exists, this will return
	 * name_1, or name_2, etc until an unused column name is found. If a column already exists, but contains only
	 * {@link biolockj.Config}.{@value #META_NULL_VALUE} values, return the name of that empty column to overwrite.
	 * 
	 * @param name Base column name
	 * @return Column name
	 * @throws MetadataException if metadata file not found and cannot be assigned
	 * @throws IOException If unable to modify the system file
	 * @throws FileNotFoundException if metadata file path not found
	 * @throws DockerVolCreationException 
	 */
	public static String getForcedColumnName( final String name )
		throws MetadataException, FileNotFoundException, IOException, DockerVolCreationException {
		String suffix = "";
		while( getFieldNames().contains( name + suffix ) ) {
			if( getFieldValues( name + suffix, true ).isEmpty() ) {
				removeColumn( name + suffix, null );
				break; // reuse the column
			}

			suffix = suffix.isEmpty() ? "_1": new Integer( Integer.valueOf( suffix.substring( 1 ) ) + 1 ).toString();
		}
		return name + suffix;
	}

	/**
	 * Get the metadata file ID column name. This value may change as updates are made by BioModules.
	 *
	 * @return Metadata ID column name
	 */
	public static String getID() {
		return metaId;
	}

	/**
	 * Returns the latest version of the given column.
	 * 
	 * @param name Base column name
	 * @return column name
	 */
	public static String getLatestColumnName( final String name ) {
		int suffix = 1;
		String testName = name;
		String foundName = null;
		while( getFieldNames().contains( testName ) ) {
			foundName = testName;
			testName = name + "_" + suffix++;
		}
		return foundName;
	}

	/**
	 * Metadata file getter. This path changes as new versions are created by the BioModules.
	 * 
	 * @return Metadata file
	 * @throws MetadataException if attempt to assign new metadata file fails
	 */
	public static File getMetadata() throws MetadataException {
		if( metadataFile != null ) return metadataFile;
		try {
			if( Config.getString( null, META_FILE_PATH ) == null ) return null;
			if( DockerUtil.inDockerEnv() ) {
				setFile( Config.getExistingFile( null, META_FILE_PATH ) );
				if( !metadataFile.isFile() )
					throw new ConfigPathException( metadataFile, "Metadata file not found in Docker container" );
			} else setFile( new File( Config.getString( null, META_FILE_PATH ) ) );
			Log.debug( MetaUtil.class, "Returning new metadata file path: " + getPath() );
		} catch( final Exception ex ) {
			throw new MetadataException( "Failed to get handle to metadata file:  " + ex.getMessage() );
		}
		return metadataFile;
	}

	/**
	 * Nulls left as empty cells do not process corrects in R, so a placeholder value must be defined (even if not
	 * used). This value is read from the property ${@link biolockj.Config}?{@value #META_NULL_VALUE}. If undefined, use
	 * {@value #DEFAULT_NULL_VALUE}
	 * 
	 * @param module BioModule calling MetaUtil
	 * @return Null value string
	 */
	public static String getNullValue( final BioModule module ) {
		if( metaNullVal != null ) return metaNullVal;
		if( Config.getString( module, META_NULL_VALUE ) == null ) {
			Log.warn( MetaUtil.class,
				"Undefined prop: " + META_NULL_VALUE + " set to default val: " + DEFAULT_NULL_VALUE );
			metaNullVal = DEFAULT_NULL_VALUE;
			Config.setConfigProperty( META_COLUMN_DELIM, metaNullVal );
		} else metaNullVal = Config.getString( module, META_NULL_VALUE );

		return metaNullVal;
	}

	/**
	 * Return the metadata file path
	 * 
	 * @return String the metadata file path
	 */
	public static String getPath() {
		try {
			if( getMetadata() != null ) return getMetadata().getAbsolutePath();
		} catch( final Exception ex ) {
			Log.error( MetaUtil.class, "Failed to return meatada file path -  metadata file not found! ", ex );
		}
		return "{ Metadata File Not Found }";
	}

	/**
	 * Get metadata row for a given Sample ID.
	 *
	 * @param sampleId Sample ID
	 * @return Metadata row values for sample ID
	 * @throws MetadataException if Sample ID not found or metadata file doesn't exist
	 */
	public static List<String> getRecord( final String sampleId ) throws MetadataException {
		if( metadataMap == null || !metadataMap.keySet().contains( sampleId ) )
			throw new MetadataException( "Invalid Sample ID: " + sampleId );
		return metadataMap.get( sampleId );
	}

	/**
	 * Get the first column from the metadata file.
	 *
	 * @return Sample IDs found in metadata file
	 */
	public static List<String> getSampleIds() {
		final List<String> ids = new ArrayList<>();
		for( final String key: metadataMap.keySet() )
			if( !key.equals( metaId ) ) ids.add( key );
		Collections.sort( ids );
		return ids;
	}

	/**
	 * Return a system generated metadata column name based on the module status.
	 * 
	 * @param module BioModule
	 * @param col Column name
	 * @return Metadata column name
	 * @throws MetadataException if metadata file not found and cannot be assigned
	 * @throws IOException If unable to modify the system file
	 * @throws FileNotFoundException if metadata file path not found
	 * @throws DockerVolCreationException 
	 */
	public static String getSystemMetaCol( final BioModule module, final String col )
		throws MetadataException, FileNotFoundException, IOException, DockerVolCreationException {
		final File outputMeta = module.getMetadata();
		if( ModuleUtil.isComplete( module ) || outputMeta.isFile() ) {
			setFile( outputMeta );
			refreshCache();
			return getLatestColumnName( col );
		}
		return getForcedColumnName( col );
	}

	/**
	 * Count the number of unique values in the given field.
	 * 
	 * @param field Column header
	 * @param ignoreNulls if TRUE ignore duplicate {@value #META_NULL_VALUE} values
	 * @return Number of unique values
	 * @throws MetadataException if field not found
	 */
	public static Set<String> getUniqueFieldValues( final String field, final boolean ignoreNulls )
		throws MetadataException {
		return new HashSet<>( getFieldValues( field, ignoreNulls ) );
	}

	/**
	 * Check if columnName exists in the current metadata file.
	 * 
	 * @param columnName Column name
	 * @return TRUE if columnName exists in hearder row of metadata file
	 */
	public static boolean hasColumn( final String columnName ) {
		return exists() && columnName != null && getFieldNames().contains( columnName );
	}

	/**
	 * Check required properties are defined and unique. Some undefined properties will use a default values as per the
	 * R{utils} read.table() function.
	 * <ul>
	 * <li>{@value #META_COLUMN_DELIM} column separator
	 * <li>{@value #META_COMMENT_CHAR} indicates start of comment in a cell, Requires length == 1
	 * <li>{@value #META_NULL_VALUE} indicates null cell
	 * </ul>
	 * @throws Exception 
	 */
	public static void initialize() throws Exception {

		Log.info( MetaUtil.class,
			"Initialize metadata property [ " + META_NULL_VALUE + " ] = " + getNullValue( null ) );

		if( Config.getString( null, META_COLUMN_DELIM ) == null )
			Config.setConfigProperty( META_COLUMN_DELIM, DEFAULT_COL_DELIM );

		if( Config.getString( null, META_COMMENT_CHAR ) == null )
			Config.setConfigProperty( META_COMMENT_CHAR, DEFAULT_COMMENT_CHAR );

		final String commentChar = Config.getString( null, META_COMMENT_CHAR );
		if( commentChar != null && commentChar.length() > 1 ) throw new MetadataException(
			META_COMMENT_CHAR + " property must be a single character.  Config value = \"" + commentChar + "\"" );

		if( Config.getString( null, META_FILE_PATH ) != null ) {
			Config.requireExistingFile( null, META_FILE_PATH );
			setFile( getMetadata() );
			refreshCache();
		}
		
		getNameToSampleMap();
		Config.getBoolean( null, META_REQUIRED );
		
		Log.info( MetaUtil.class, "Metadata initialized" );
	}
	
	/*
	 * Is this pipeline configured to use one or more columns in the metadata to link files to sample id's.
	 */
	public static boolean hasMetaColToSampleIdMap() throws Exception {
		final List<String> columns = Config.getList( null, META_FILENAME_COLUMN );
		boolean useFileNameColumn = columns != null && !columns.isEmpty();
		
		if( useFileNameColumn ) {
			for( String metaCol: columns ) {
				if( !hasColumn( metaCol ) ) {
					throw new ConfigFormatException( META_FILENAME_COLUMN, "The column [" + metaCol + "] does not exist in the metadata file." );
				}

				if( MetaUtil.getFieldValues( metaCol, true ).isEmpty() ) { 
					throw new ConfigViolationException( META_FILENAME_COLUMN, "Empty columns are not permitted for the filename to sample id mapping."
						+ System.lineSeparator() + "Column [" + metaCol + "] is empty."); 
				}
			}
		}
		
		return useFileNameColumn;
	}
	
	private static HashMap<String, String> getNameToSampleMap() throws Exception{
		if (nameToSample == null) initNameToSampleMap();
		return nameToSample;
	}
	
	/*
	 * Initialize a file-name to sample-name map using the column(s) specified in the config file.
	 * Future methods may add to this map from other sources, but should always start with this map.
	 */
	private static void initNameToSampleMap() throws Exception{
		nameToSample = new HashMap<>();
		if ( hasMetaColToSampleIdMap() ) {
			final List<String> columns = Config.getList( null, META_FILENAME_COLUMN );
			for( String metaCol: columns ) {
				for (String sample : getSampleIds()) {
					String fileName = getField( sample, metaCol );
					if (fileName != null) {
						if (nameToSample.get( fileName ) != null && !nameToSample.get( fileName ).equals( sample )) {
							throw new ConfigViolationException( META_FILENAME_COLUMN,
								"The file name \"" + fileName + "\" cannot be assigned to both samples [" +
									nameToSample.get( fileName ) + "] and [" + sample + "]." );
						}else {
							nameToSample.put(fileName, sample);
							Log.info(MetaUtil.class, "Filename [" + fileName + "] has been associated to sample [" + sample + "].");
						}
					}
				}
			}
		}
	}
	
	private static String getSampleIdFromFileName(String filename) throws Exception {
		Log.debug(MetaUtil.class, "Checking metadata for file name: " + filename);
		String id = getNameToSampleMap().get( filename );
		Log.debug(MetaUtil.class, "Map has keys: " + BioLockJUtil.getCollectionAsString( getNameToSampleMap().keySet() ));
		String msg = id == null ? "File is not given in the metadata." : "Linked " + filename + " to sample [" + id + "].";
		if (getNameToSampleMap().size() > 0 && id==null ) Log.debug( MetaUtil.class, "Files in metadata include: " + BioLockJUtil.printLongFormList( getNameToSampleMap().keySet() ) );
		Log.debug(MetaUtil.class, msg);
		return id;
	}
	public static String getSampleId(File file) throws Exception {
		return getSampleIdFromFileName(file.getName());
	}
	public static Set<String> getSampleFileList() throws Exception{
		return new HashSet<String>(getNameToSampleMap().keySet());
	}

	/**
	 * Refresh the metadata cache.
	 *
	 * @throws MetadataException if unable to refresh cache
	 */
	public static void refreshCache() throws MetadataException {
		if( isUpdated() ) {
			Log.info( MetaUtil.class, "Update metadata cache: " + getPath() );
			metadataMap.clear();
			cacheMetadata( parseMetadataFile() );

			if( !BioLockJUtil.isDirectMode() ) report();

			reportedMetadata = getMetadata();
		} else Log.debug( MetaUtil.class, "Skip metadata refresh cache, path unchanged: " +
			( getMetadata() == null ? "<NO_METADATA_PATH>": getPath() ) );
	}

	/**
	 * Remove the metadata column from the metadata file
	 * 
	 * @param colName Name of column to remove
	 * @param fileDir File representing output directory for new metadata file
	 * @throws MetadataException if metadata file not found and cannot be assigned
	 * @throws IOException If unable to modify the system file
	 * @throws FileNotFoundException if metadata file path not found
	 * @throws DockerVolCreationException 
	 */
	public static void removeColumn( final String colName, final File fileDir )
		throws FileNotFoundException, IOException, MetadataException, DockerVolCreationException {
		File myDir = fileDir;
		if( fileDir == null ) {
			myDir = new File( Config.pipelinePath() + File.separator + ".temp" );
			if( !myDir.isDirectory() ) myDir.mkdirs();
		}

		if( !getFieldNames().contains( colName ) ) {
			Log.warn( MetaUtil.class,
				"Metadata column [" + colName + "] cannot be removed, because it does not exists in: " + getPath() );
			return;
		}

		Log.info( MetaUtil.class, "Removing field [" + colName + "] from metadata: " + getPath() );
		final int index = getFieldNames().indexOf( colName );
		final File newMeta = new File( myDir.getAbsolutePath() + File.separator + getFileName() );
		final BufferedReader reader = BioLockJUtil.getFileReader( getMetadata() );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( newMeta ) );
		try {
			for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
				int i = 1;
				final StringTokenizer st = new StringTokenizer( line, DEFAULT_COL_DELIM );
				writer.write( st.nextToken() );
				while( st.hasMoreTokens() ) {
					final String token = st.nextToken();
					if( i++ != index ) writer.write( DEFAULT_COL_DELIM + token );
				}
				writer.write( Constants.RETURN );
			}
		} finally {
			reader.close();
			writer.close();
			setFile( newMeta );
			refreshCache();
		}
	}

	/**
	 * Set a new metadata file.
	 *
	 * @param file New metadata file
	 * @throws MetadataException if null parameter is passed
	 * @throws DockerVolCreationException 
	 */
	public static void setFile( final File file ) throws MetadataException, DockerVolCreationException {
		if( file == null ) throw new MetadataException( "Cannot pass NULL to MetaUtil.setFile( file )" );
		if( metadataFile != null && file.getAbsolutePath().equals( getPath() ) )
			Log.debug( MetaUtil.class, "===> MetaUtil.setFile() not required, no changes to: " + getPath() );
		BioLockJUtil.ignoreFile( file );
		metadataFile = file;
	}

	private static void cacheMetadata( final List<List<String>> data ) {
		int rowNum = 0;
		final Iterator<List<String>> rows = data.iterator();
		while( rows.hasNext() ) {
			final List<String> row = rows.next();
			final String id = row.get( 0 );
			if( rowNum == 0 ) {
				metaId = id;
				if( isUpdated() ) Log.debug( MetaUtil.class, "Metadata Headers: " + row );
			} else if( rowNum == 1 && isUpdated() ) Log.debug( MetaUtil.class, "Metadata Record (1st Row): " + row );

			if( id != null && !id.equals( getNullValue( null ) ) ) {
				row.remove( 0 );
				if( isUpdated() ) Log.debug( MetaUtil.class, "metadataMap add: " + id + " = " + row );
				metadataMap.put( id, row );
			}
			rowNum++;
		}
	}

	private static String getRemoveIdMsg( final String id ) {
		String msg = "REMOVE SAMPLE ID [" + id + "] from metadata file " + getPath() + " | Reason:  ";
		try {
			final String dirs = Config.requireString( null, Constants.INPUT_DIRS );
			if( SeqUtil.piplineHasSeqInput() ) {
				for( final File seqFile: BioLockJUtil.getPipelineInputFiles() ) {
					final String seqId = SeqUtil.getSampleId( seqFile );
					if( seqId != null && seqId.equals( id ) )
						return msg + "No valid seqs remain in: " + seqFile.getName();
				}
				msg += "Sample not found in pipeline input dirs: " + dirs;
			} else msg += "Sample not referenced in pipeline inputs: " + dirs;
		} catch( final Exception ex ) {
			Log.error( MetaUtil.class,
				"Failed to generate log message with reason why a Sample removed from metadata column", ex );
		}
		return msg;
	}

	private static boolean isUpdated() {
		try {
			final boolean foundNewReport = getMetadata() != null && reportedMetadata != null &&
				!reportedMetadata.getAbsolutePath().equals( getPath() );
			final boolean noReport = getMetadata() != null && reportedMetadata == null;
			return foundNewReport || noReport;
		} catch( final MetadataException ex ) {
			Log.error( MetaUtil.class, "Failed to determine if metadata has been updated - file may not exists", ex );
			return false;
		}
	}

	private static List<List<String>> parseMetadataFile() {
		final List<List<String>> data = new ArrayList<>();
		BufferedReader reader = null;
		try {
			reader = BioLockJUtil.getFileReader( getMetadata() );
			for( String line = reader.readLine(); line != null && !line.isEmpty(); line = reader.readLine() ) {
				if( isUpdated() ) Log.debug( MetaUtil.class, "===> Meta line: " + line );
				final ArrayList<String> record = new ArrayList<>();
				final String[] cells = line.split( DEFAULT_COL_DELIM, -1 );
				for( final String cell: cells )
					if( cell == null || cell.trim().isEmpty() ) record.add( getNullValue( null ) );
					else record.add( removeComments( cell.trim() ) );
				data.add( record );
			}
		} catch( final Exception ex ) {
			Log.error( MetaUtil.class, "Error occurrred parsing metadata file!", ex );
		} finally {
			try {
				if( reader != null ) reader.close();
			} catch( final IOException ex ) {
				Log.error( MetaUtil.class, "Failed to close file reader", ex );
			}
		}
		return data;
	}

	private static String removeComments( final String val ) {
		final String cChar = Config.getString( null, META_COMMENT_CHAR );
		if( cChar != null && cChar.length() > 0 && val.indexOf( cChar ) > -1 )
			return val.substring( 0, val.indexOf( cChar ) );
		return val;
	}

	private static void report() {
		try {
			final String exId = getSampleIds().get( 0 );
			Log.info( MetaUtil.class, META_SPACER );
			Log.info( MetaUtil.class, "===> New Metadata file: " + getPath() );
			Log.info( MetaUtil.class, "===> Sample IDs: " + getSampleIds() );
			Log.info( MetaUtil.class, "===> Metadata fields: " + getFieldNames() );
			Log.info( MetaUtil.class, "===> 1st Record: [" + exId + "]: " + getRecord( exId ) );
			Log.info( MetaUtil.class, META_SPACER );
		} catch( final MetadataException ex ) {
			Log.error( MetaUtil.class, "Failed to log MetaUtil reprot", ex );
		}
	}
	
	public static void registerProps() throws API_Exception {
		Properties.registerProp( META_BARCODE_COLUMN, Properties.STRING_TYPE, META_BARCODE_COLUMN_DESC );
		Properties.registerProp( META_COLUMN_DELIM, Properties.STRING_TYPE, META_COLUMN_DELIM_DESC );
		Properties.registerProp( META_COMMENT_CHAR, Properties.STRING_TYPE, META_COMMENT_CHAR_DESC );
		Properties.registerProp( META_FILE_PATH, Properties.STRING_TYPE, META_FILE_PATH_DESC );
		Properties.registerProp( META_FILENAME_COLUMN, Properties.LIST_TYPE, META_FILENAME_COLUMN_DESC );
		Properties.registerProp( META_NULL_VALUE, Properties.STRING_TYPE, META_NULL_VALUE_DESC );
		Properties.registerProp( META_REQUIRED, Properties.BOOLEAN_TYPE, META_REQUIRED_DESC );
		Properties.registerProp( USE_EVERY_ROW, Properties.BOOLEAN_TYPE, USE_EVERY_ROW_DESC );
	}

	/*
	 * Key string is a file name, value string is a sample id from the metadata.
	 */
	private static HashMap<String, String> nameToSample = null;
	
	/**
	 * {@link biolockj.Config} property {@value #META_BARCODE_COLUMN}
	 * {@value #META_BARCODE_COLUMN_DESC} 
	 */
	public static final String META_BARCODE_COLUMN = "metadata.barcodeColumn";
	private static final String META_BARCODE_COLUMN_DESC = "metadata column with identifying barcodes";

	/**
	 * {@link biolockj.Config} property: {@value #META_COLUMN_DELIM}<br>
	 * {@value #META_COLUMN_DELIM_DESC}
	 */
	public static final String META_COLUMN_DELIM = "metadata.columnDelim";
	private static final String META_COLUMN_DELIM_DESC = "defines how metadata columns are separated; Typically files are tab or comma separated.";

	/**
	 * {@link biolockj.Config} property: {@value #META_COMMENT_CHAR}<br>
	 * {@value #META_COMMENT_CHAR_DESC}
	 */
	public static final String META_COMMENT_CHAR = "metadata.commentChar";
	private static final String META_COMMENT_CHAR_DESC = "metadata file comment indicator; Empty string is a valid option indicating no comments in metadata file.";

	/**
	 * {@link biolockj.Config} String property: {@value #META_FILE_PATH}<br>
	 * {@value #META_FILE_PATH_DESC}
	 */
	public static final String META_FILE_PATH = "metadata.filePath";
	private static final String META_FILE_PATH_DESC = "If absolute file path, use file as metadata.<br>If directory path, must find exactly 1 file within, to use as metadata.";

	/**
	 * {@link biolockj.Config} property {@value #META_FILENAME_COLUMN}<br>
	 * {@value #META_FILENAME_COLUMN_DESC}
	 */
	public static final String META_FILENAME_COLUMN = "metadata.fileNameColumn";
	private static final String META_FILENAME_COLUMN_DESC = "name of the metadata column(s) with input file names";

	/**
	 * {@link biolockj.Config} property: {@value #META_NULL_VALUE}<br>
	 * {@value #META_NULL_VALUE_DESC}
	 */
	public static final String META_NULL_VALUE = "metadata.nullValue";
	private static final String META_NULL_VALUE_DESC = "metadata cells with this value will be treated as empty";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #META_REQUIRED}<br>
	 * {@value #META_REQUIRED_DESC}
	 */
	public static final String META_REQUIRED = "metadata.required";
	private static final String META_REQUIRED_DESC = "If Y, require metadata row for each sample with sequence data in input dirs; If N, samples without metadata are ignored.";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #USE_EVERY_ROW}<br>
	 * {@value #USE_EVERY_ROW_DESC}
	 */
	public static final String USE_EVERY_ROW = "metadata.useEveryRow";
	private static final String USE_EVERY_ROW_DESC = "If Y, require a sequence file for every SampleID (every row) in metadata file; If N, metadata can include extraneous SampleIDs.";

	/**
	 * Default column delimiter = tab character
	 */
	protected static final String DEFAULT_COL_DELIM = Constants.TAB_DELIM;

	/**
	 * Default comment character for any new metadata file created by a BioModule: {@value #DEFAULT_COMMENT_CHAR}
	 */
	protected static final String DEFAULT_COMMENT_CHAR = "";

	/**
	 * Default field value to represent null-value: {@value #DEFAULT_NULL_VALUE}
	 */
	protected static final String DEFAULT_NULL_VALUE = "NA";

	private static String META_SPACER = "************************************************************************";
	private static File metadataFile = null;
	private static final Map<String, List<String>> metadataMap = new HashMap<>();
	private static String metaId = "SAMPLE_ID";
	private static String metaNullVal = null;
	private static File reportedMetadata = null;
}
