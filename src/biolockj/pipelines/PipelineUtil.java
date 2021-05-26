package biolockj.pipelines;

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
import biolockj.exception.InvalidPipelineException;
import biolockj.module.BioModule;
import biolockj.util.BioLockJUtil;
import biolockj.util.RuntimeParamUtil;

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
		List<File> pipelines = new ArrayList<>();
		if (projDir.exists()) {
			pipelines.addAll( Arrays.asList( projDir.listFiles() ) );
		}
		Collections.sort( pipelines, new Comparator<File>() {
			@Override
			public int compare( File o1, File o2 ) {
				return Long.compare( o2.lastModified(), o1.lastModified() );
			}
		} );
		for( File pipe: pipelines ) {
			if( PipelineUtil.isPipelineDir( pipe ) ) { return pipe; }
		}
		return projDir;
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
			if( i > -1 && configName.length() > i + 1 ) {
				name = configName.substring( 0, i );
				if (name.startsWith( Constants.MASTER_PREFIX )) name = name.substring( Constants.MASTER_PREFIX.length() );
			}else {
				name = configName;
			}
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
		OtherPipeline pipeline = OtherPipelineFactory.asPipeline(pipeDir);
		return pipeline.getProjectName();
	}
	
	public static String getPipelineId(final File pipeDir) throws InvalidPipelineException {
		OtherPipeline pipeline = OtherPipelineFactory.asPipeline(pipeDir);
		return pipeline.getPipelineId();
	}
	
	/**
	 * Determine if a given directory is a module directory in a pipeline.
	 * @param dir
	 * @return
	 */
	public static boolean isModuleDir(final File dir) {
		boolean bool;
		try {
			OtherPipeline pipeline = OtherPipelineFactory.asPipeline( dir.getParentFile() );
			bool = pipeline.isModuleDir(dir);
		} catch( InvalidPipelineException e ) {
			bool = false;
		}
		return bool;
	}
	
	/**
	 * Determine if a given directory is a valid pipeline.
	 * To be a valid pipeline, the dir must exist, be a directory, 
	 * and contain a master properties file.
	 * 
	 * @param dir
	 * @return
	 */
	public static boolean isPipelineDir( final File dir ) {
		try {
			OtherPipelineFactory.asPipeline(dir);
		} catch( InvalidPipelineException e ) {
			return false;
		}
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
		OtherPipeline pipeline = OtherPipelineFactory.asPipeline(pipeDir);
		return pipeline.getMasterConfig();
	}
	
	/**
	 * Determine if a given pipeline was run in precheck mode, ie using the {@value RuntimeParamUtil#PRECHECK_FLAG} parameter.
	 * @param pipelineDir
	 * @return
	 */
	public static boolean isPrecheckPipeline(final File pipelineDir) {
		boolean bool;
		try {
			OtherPipeline pipeline = OtherPipelineFactory.asPipeline(pipelineDir);
			bool = pipeline.isPrecheckPipeline();
		} catch( InvalidPipelineException e ) {
			bool = false;
		}
		return bool;
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
	 * Get the status flag file of the current pipeline root.
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
			if( ff.exists() ) {
				ff.delete();
				hadFlag = flag;
			}
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
