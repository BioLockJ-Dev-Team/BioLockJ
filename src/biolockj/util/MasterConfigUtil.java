/**
 * @UNCC Fodor Lab
 * @author Anthony Fodor
 * @email anthony.fodor@gmail.com
 * @date Aug 9, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.util;

import java.io.*;
import java.util.*;
import org.apache.commons.io.FileUtils;
import biolockj.*;
import biolockj.exception.BioLockJException;

/**
 * Simple utility containing String manipulation and formatting functions.
 */
public class MasterConfigUtil {

	/**
	 * Return the MASTER config file.
	 * 
	 * @return MASTER config
	 */
	public static File getMasterConfig() {
		String configName = Config.pipelineName();
		if( configName.startsWith( Constants.MASTER_PREFIX ) )
			configName = configName.replaceAll( Constants.MASTER_PREFIX, "" );
		return new File(
			Config.pipelinePath() + File.separator + Constants.MASTER_PREFIX + configName + Constants.PROPS_EXT );
	}
	
	/**
	 * Find the MASTER config file in an existing pipeline.
	 * 
	 * @return MASTER config
	 * @throws BioLockJException 
	 */
	public static File getExistingMasterConfig(File pipelineRoot) throws BioLockJException {
		File masterConfig = null;
		File[] possibleMasters = pipelineRoot.listFiles( new FilenameFilter() {
			@Override
			public boolean accept( File dir, String name ) {
				return name.startsWith( Constants.MASTER_PREFIX ) && name.endsWith( Constants.PROPS_EXT );
			}
		} );
		if (possibleMasters.length == 1 ) {
			masterConfig = possibleMasters[0];
			Log.info(MasterConfigUtil.class, "Pipeline dir [" + pipelineRoot.getName() 
				+ "] contains master config file: " + masterConfig.getAbsolutePath());
		}else if (possibleMasters.length > 1) {
			throw new BioLockJException("Could not determine which file to use as master config file.");
		}else if (possibleMasters.length < 1) {
			throw new BioLockJException("Could not find any file to use as master config file.");
		}
		return masterConfig;
	}

	/**
	 * Return the file path of the MASTER Config file generated in the pipeline root directory.
	 * 
	 * @return File-path of the MASTER Config file
	 */
	public static String getPath() {
		return getMasterConfig().getAbsolutePath();
	}

	/**
	 * Remove unused properties from the MASTER config file.
	 * 
	 */
	public static void sanitizeMasterConfig() {

		try {
			Log.info( MasterConfigUtil.class,
				"Distilling the MASTER Config into the FINAL Config file to show only properties accessed during pipeline execution." );

			final Map<String, String> props = new HashMap<>();
			final Map<String, String> usedProps = Config.getUsedProps();
			final Set<String> configMods = Config.getSet( null, Constants.INTERNAL_BLJ_MODULE );
			boolean foundQiime = false;
			for( final String mod: configMods )
				if( mod.toLowerCase().contains( Constants.QIIME ) ) foundQiime = true;
			if( !foundQiime ) usedProps.remove( Constants.QIIME_ALPHA_DIVERSITY_METRICS );

			if( !Log.doDebug() ) {
				Log.info( MasterConfigUtil.class,
					"To view the list of removed Config properties before MASTER config is sanitized in future runs, enable: " +
						Constants.LOG_LEVEL_PROPERTY + "=" + Constants.TRUE );
				Log.info( MasterConfigUtil.class,
					"To add DEBUG statements for only this utility class, add the property (this is a list property so multiple" +
						" class names could be provided - in this example, wee add a single class): " +
						Constants.LIMIT_DEBUG_CLASSES + "=" + MasterConfigUtil.class.getName() );
			}

			usedProps.remove( Constants.INTERNAL_BLJ_MODULE );
			usedProps.remove( Constants.PIPELINE_DEFAULT_PROPS );
			usedProps.remove( Constants.PROJECT_DEFAULT_PROPS);

			for( final String key: usedProps.keySet() ) {
				final String val = usedProps.get( key );
				if( val == null || val.trim().isEmpty() ) Log.debug( MasterConfigUtil.class,
					"Remove unused property from sanatized MASTER Config: " + key + "=" + val );
				else if( !Config.isInternalProperty(key) ) props.put( key, val );
			}

			Log.info( MasterConfigUtil.class, "The original version of project Config contained: " +
				Config.getInitialProperties().size() + " properties" );
			Log.info( MasterConfigUtil.class,
				"The final version of MASTER Config contains: " + props.size() + " properties" );

			saveMasterConfig( props );
		} catch( final Exception ex ) {
			ex.printStackTrace();
		}
	}

	/**
	 * Save a single version of the Config file with all inherited properties for the default config (if any exist).
	 * 
	 * @return boolean status indicator TRUE if successful
	 */
	public static boolean saveMasterConfig() {
		return saveMasterConfig( Config.getProperties() );
	}

	/**
	 * Save a single version of the Config file with all inherited properties for the default config (if any exist).
	 * Include internal.* properties if any provided.
	 * 
	 * @param props Properties map
	 * @return boolean status indicator TRUE if successful
	 */
	public static boolean saveMasterConfig( final Map<String, String> props ) {

		if( getMasterConfig().isFile() ) try {
			FileUtils.moveFile( getMasterConfig(), getTempConfig() );
		} catch( final Exception ex ) {
			Log.error( MasterConfigUtil.class, "Failed to archive: " + getMasterConfig().getAbsolutePath(), ex );
			return false;
		}
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter( new FileWriter( getMasterConfig() ) );
			writeModuleRunOrder( writer );
			writeDefPropsChain( writer );
			final Set<String> keys = new TreeSet<>( getProps( props ).keySet() );
			for( final String key: keys )
				writer.write( key + "=" + getProps( props ).get( key ) + RETURN );
		} catch( final IOException ex ) {
			Log.error( MasterConfigUtil.class, "Failed to archive: " + getMasterConfig().getAbsolutePath(), ex );
			return false;
		} finally {
			if( writer != null ) try {
				writer.close();
			} catch( final IOException ex2 ) {
				Log.error( MasterConfigUtil.class, "Failed to close MASTER config writer", ex2 );
			}
			if( getTempConfig().isFile() ) getTempConfig().delete();
		}

		return getMasterConfig().isFile();
	}

	private static List<String> getInitConfig() throws IOException {
		final File masterConfig = getMasterConfig();
		if( !masterConfig.isFile() || !Config.getConfigFilePath().equals( masterConfig.getAbsolutePath() ) )
			return null;
		final List<String> initConfig = new ArrayList<>();
		final BufferedReader reader = BioLockJUtil.getFileReader( masterConfig );
		try {
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
				if( line.startsWith( PROJ_CONFIG_FLAG ) || line.startsWith( DEFAULT_CONFIG_FLAG ) )
					initConfig.add( line );
		} finally {
			if( reader != null ) reader.close();
		}
		return initConfig;
	}

	private static Map<String, String> getProps( final Map<String, String> props ) {
		if( props == null ) return Config.getProperties();
		return props;
	}

	private static File getTempConfig() {
		return new File( Config.pipelinePath() + File.separator + TEMP_PREFIX + Config.pipelineName() );
	}
	
	private static void writeModuleRunOrder( final BufferedWriter writer ) throws IOException {
		for( final String module: Config.getList( null, Constants.INTERNAL_BLJ_MODULE ) )
			writer.write( Constants.BLJ_MODULE_TAG + " " + module + RETURN );
		writer.write( RETURN );
	}
	
	private static void writeDefPropsChain( final BufferedWriter writer ) throws IOException {
		writer.write( "# This properties file was generated from the following files:" + RETURN );
		final List<String> initConfig = getInitConfig();
		if( initConfig == null ) {
			writer.write( PROJ_CONFIG_FLAG + Config.getConfigFilePath() + RETURN );
			final List<String> defaults = Config.getList( null, Constants.INTERNAL_DEFAULT_CONFIG );
			for( int i = defaults.size(); i > 0; i-- )
				writer.write( DEFAULT_CONFIG_FLAG + defaults.get( i - 1 ) + RETURN );
		} else for( final String line: initConfig )
			writer.write( line + RETURN );
		writer.write( RETURN );
	}

	private static final String DEFAULT_CONFIG_FLAG = "# ----> Default Config: ";
	private static final String PROJ_CONFIG_FLAG = "# ----> Project Config: ";
	private static final String RETURN = Constants.RETURN;
	private static final String TEMP_PREFIX = ".TEMP_";
	private static final String FINAL_PREFIX = "FINAL_";
}
