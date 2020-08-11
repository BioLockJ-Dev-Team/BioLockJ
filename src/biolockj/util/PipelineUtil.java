package biolockj.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.io.FileUtils;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Constants;
import biolockj.Properties;
import biolockj.exception.BioLockJStatusException;
import biolockj.exception.DockerVolCreationException;
import biolockj.exception.InvalidPipelineException;
import biolockj.module.BioModule;

public class PipelineUtil {
	
	/**
	 * 
	 * @param pipeDir the pipeline directory to discard
	 * @return boolean true if the pipeline was successfully removed.
	 */
	public static boolean discardPrecheckPipeline(File pipeDir) {
		if ( pipeDir == null || ! isPrecheckPipeline( pipeDir )) return false;
		try {
			FileUtils.forceDelete(pipeDir.getAbsoluteFile());
		} catch( IOException e ) {
			//e.printStackTrace();
		}
		return ! pipeDir.exists();
	}
	
	public static File getMostRecentPipeline(File projDir) {
		List<File> pipelines = new ArrayList<>( Arrays.asList( projDir.listFiles() ) );
		Collections.sort( pipelines, new Comparator<File>() {
			@Override
			public int compare( File o1, File o2 ) {
				return Long.compare( o2.lastModified(), o1.lastModified() );
			}
		} );
		for( File pipe: pipelines ) {
			if( PipelineUtil.isPipelineDir( pipe ) ) { return pipe; }
		}
		return new File(projDir, "the0thPipeline");
	}

	/**
	 * Extract the project name from the Config file.
	 * 
	 * @param configName file name to derive the name from
	 * @return Project name
	 */
	public static String getProjectNameFromPropFile(final File config) {
		String configName = config.getName();
		String name = null;
		if( configName.startsWith( Constants.MASTER_PREFIX ) ) {
			try {
				// notice this does not seek out default props, 
				// and it does not alter any static members.
				final FileInputStream in = new FileInputStream( config );
				final Properties props = new Properties();
				props.load( in );
				in.close();
				name = props.getProperty( Constants.INTERNAL_PIPELINE_NAME, null );
			} catch( Exception e ) {
				e.printStackTrace();
			}
		}
		if (name==null){
			final int i = configName.lastIndexOf( "." );
			if( i > -1 && configName.length() > i + 1 ) name = configName.substring( 0, i );
			if (name.startsWith( Constants.MASTER_PREFIX )) name = name.substring( Constants.MASTER_PREFIX.length() );
		}
		return name;
	}
	/**
	 * Extract the project name from the an existing pipeline.
	 * 
	 * @param configName file name to derive the name from
	 * @return Project name
	 * @throws InvalidPipelineException 
	 */
	public static String getProjectName(final File pipeDir) throws InvalidPipelineException  {
		return getProjectNameFromPropFile( getMasterConfig(pipeDir) );
	}
	
	public static String getPipelineId(final File pipeDir) throws InvalidPipelineException {
		return getPipelineIdFromMasterProps( getMasterConfig( pipeDir ));
	}
	public static String getPipelineIdFromMasterProps(final File masterConfig) {
		String configName = masterConfig.getName();
		String name = configName;
		if( configName.startsWith( Constants.MASTER_PREFIX ) && configName.endsWith( Constants.PROPS_EXT )) {
			name = configName.replaceFirst( Constants.MASTER_PREFIX, "" );
			name = name.substring( 0, name.length()-Constants.PROPS_EXT.length() );
		}
		return name;
	}
	
	/**
	 * Determine if a given directory is a module directory in a pipeline.
	 * @param dir
	 * @return
	 */
	public static boolean isModuleDir(final File dir) {
		if ( ! dir.isDirectory() ) return false;
		if ( ! PipelineUtil.isPipelineDir(dir.getParentFile() ) ) return false;
		int index = dir.getName().indexOf( '_' );
		if (index < 2) return false;
		if (index == dir.getName().length()) return false;
		Integer moduleNumber = Integer.valueOf( dir.getName().substring( 0, index ) );
		int max = dir.getParentFile().listFiles().length;
		if (moduleNumber.intValue() < 0) return false;
		if (moduleNumber.intValue() >= max ) return false;
		return true;
	}
	
	/**
	 * Determine if a given directory is a valid pipeline.
	 * 
	 * @param dir
	 * @return
	 */
	public static boolean isPipelineDir( final File dir ) {
		if( !dir.exists() ) return false;
		if( !dir.isDirectory() ) return false;
		File flagFile = null;
		try {
			getMasterConfig( dir );
			flagFile = getPipelineStatusFlag( dir );
		} catch( InvalidPipelineException e ) {
			return false;
		}
		if (flagFile != null) return true;
		// TODO: simplify this.
		// If it has MASTER_*.properties and a flagFile, its a pipeline.
		// In the odd case of a just-restarted pipeline, there is no flagFile.  
		// But a restarted pipeline must have at least one module already or it wouldn't be worth restarting it.
		// So IFF the pipeline has its first module dir, then it can get away with not having a flag file.
		//
		// pipeline must include at least one module folder, which must start with 00_[A:Z] or 000_[A:Z]
		// do not use isModuleDir method here, that will cause a logic loop
		if( dir.list( new FilenameFilter() {
			@Override
			public boolean accept( File dir, String name ) {
				if( !( new File( dir, name ) ).isDirectory() ) return false;
				return ( name.startsWith( "00_" ) || name.startsWith( "000_" ) );
			}
		} ).length != 1 && flagFile==null) return false;
		return true;
	}
	
	/**
	 * Get the MASTER_*.properties file for a given pipeline.
	 * 
	 * @param pipeDir a pipeline directory
	 * @return the master config file for the pipeline
	 * @throws InvalidPipelineException 
	 */
	public static File getMasterConfig( final File pipeDir ) throws InvalidPipelineException {
		if (pipeDir == null || ! pipeDir.isDirectory() ) throw new InvalidPipelineException( pipeDir );
		File[] files = pipeDir.listFiles( new FilenameFilter() {
			@Override
			public boolean accept( File dir, String name ) {
				return ( name.startsWith( Constants.MASTER_PREFIX ) && name.endsWith( Constants.PROPS_EXT ) );
			}
		} );
		// root level must have exactly one MASTER_*.properties file
		if( files == null || files.length != 1 ) {
			throw new InvalidPipelineException( pipeDir );
		}
		return files[ 0 ];
	}
	
	/**
	 * Determine if a given pipeline was run in precheck mode, ie using the {@value RuntimeParamUtil#PRECHECK_FLAG} parameter.
	 * @param pipelineDir
	 * @return
	 */
	public static boolean isPrecheckPipeline(final File pipelineDir) {
		String flagName = PipelineUtil.getPipelineStatusFlag(pipelineDir).getName();
		if ( flagName.contentEquals( Constants.PRECHECK_COMPLETE ) 
						|| flagName.contentEquals( Constants.PRECHECK_FAILED) ) {
			return true;
		}else {
			return false;	
		}
	}
	
	/**
	 * Get the status flag file for a given directory (could be a module, current pipeline, or restart pipeline).
	 * @param dir
	 * @return
	 */
	public static File getPipelineStatusFlag(File dir) {
		File flagFile = null;
		for( String flag: PipelineUtil.allFlags ) {
			File ff = new File( dir + File.separator + flag );
			if( ff.exists() ) flagFile=ff;
		}
		return flagFile;
	}
	/**
	 * Get the status file file of the current pipeline root.
	 * @return
	 */
	public static File getPipelineStatusFlag() {
		return getPipelineStatusFlag( BioLockJ.getPipelineDir() );
	}
	/**
	 * Remove the current status flag of a given directory
	 * @param dir The pipeline or module directory to remove the flag from
	 */
	public static String clearStatus( File dir ) {
		String hadFlag = null;
		for( String flag: PipelineUtil.allFlags ) {
			File ff = new File( dir, flag );
			if( ff.exists() ) ff.delete();
			hadFlag = flag;
		}
		return hadFlag;
	}
	/**
	 * Remove the current status flag of a given directory
	 * @param dirPath a String giving the absolute path to a pipeline or module
	 */
	public static String clearStatus( String dirPath ) {
		return clearStatus(new File(dirPath));
	}
	
	/**
	 * Set the status of a module or pipeline using a status flag file.
	 * @param dirPath the module or pipeline to mark
	 * @param statusFlag the flag to mark it with
	 * @return
	 * @throws BioLockJStatusException
	 * @throws IOException
	 */
	public static File markStatus( final String dirPath, final String statusFlag )
		throws BioLockJStatusException, IOException {
		clearStatus( dirPath );
		return ( BioLockJUtil.createFile( dirPath + File.separator + statusFlag ) );
	}
	/**
	 * Set the status of a module or pipeline using a status flag file.
	 * @param statusFlag
	 * @return
	 * @throws BioLockJStatusException
	 * @throws IOException
	 */
	public static File markStatus( final String statusFlag ) throws BioLockJStatusException, IOException {
		return ( markStatus( Config.pipelinePath(), statusFlag ) );
	}
	/**
	 * Set the status of a module using a status flag file.
	 * @param module
	 * @param statusFlag
	 * @return
	 * @throws BioLockJStatusException
	 * @throws IOException
	 */
	public static File markStatus( final BioModule module, final String statusFlag )
		throws BioLockJStatusException, IOException {
		return ( markStatus( module.getModuleDir().getAbsolutePath(), statusFlag ) );
	}
	
	
	
	public static final String[] allFlags = { Constants.BLJ_STARTED, Constants.BLJ_FAILED, Constants.BLJ_COMPLETE,
	Constants.PRECHECK_COMPLETE, Constants.PRECHECK_FAILED, Constants.PRECHECK_STARTED };

	
}
