package biolockj.pipelines;

import java.io.File;
import java.io.FilenameFilter;
import biolockj.Constants;
import biolockj.exception.InvalidPipelineException;

/**
 * The defaults here should be suitable for representing pipelines made by the current version of BioLockJ. Child
 * classes with associated versions override these as needed. This represents pipelines that were not made by the
 * current instance of BioLockJ, but that were made by the current (or near-current) version.
 * 
 * @author Ivory Blakley
 *
 */
public class OtherPipelineImpl implements OtherPipeline {

	private final File pipeDir;
	private final File masterProp;
	private final String projectName;
	private final String pipelineId;
	
	public OtherPipelineImpl(File dir) throws InvalidPipelineException{
		pipeDir = dir;
		if (!pipeDir.isDirectory()) throw new InvalidPipelineException(dir);
		masterProp = setMasterConfig();
		projectName = setProjectName();
		pipelineId = setPipelineId();
	}
	
	@Override
	public File getMasterConfig() {
		return masterProp;
	}
	
	@Override
	public File getPipeDir() {
		return pipeDir;
	}
	
	@Override
	public String getPipelineId() {
		return pipelineId;
	}
	
	@Override
	public File getPipelineStatusFlag() {
		return PipelineUtil.getPipelineStatusFlag(getPipeDir());
	}
	
	@Override
	public String getProjectName() {
		return projectName;
	}
	
	@Override
	public boolean isModuleDir(final File dir) {
		if ( ! dir.getParentFile().equals( pipeDir ) ) return false;
		if ( ! dir.isDirectory() ) return false;
		int index = dir.getName().indexOf( '_' );
		if (index < 2) return false;
		if (index == dir.getName().length()) return false;
		Integer moduleNumber = Integer.valueOf( dir.getName().substring( 0, index ) );
		int max = dir.getParentFile().listFiles().length;
		if (moduleNumber.intValue() < 0) return false;
		if (moduleNumber.intValue() >= max ) return false;
		return true;
	}
	
	@Override
	public boolean isPrecheckPipeline() {
		File flagFile = getPipelineStatusFlag();
		if ( flagFile == null ) return false;
		else if ( flagFile.getName().contentEquals( Constants.PRECHECK_COMPLETE ) 
						|| flagFile.getName().contentEquals( Constants.PRECHECK_FAILED) ) {
			return true;
		}else {
			return false;	
		}
	}
	
	/**
	 * Used to initial set the masterProps file object.
	 * @return
	 * @throws InvalidPipelineException
	 */
	protected File setMasterConfig() throws InvalidPipelineException {
		File[] files = getPipeDir().listFiles( new FilenameFilter() {
			@Override
			public boolean accept( File dir, String name ) {
				return ( name.startsWith( Constants.MASTER_PREFIX ) && name.endsWith( Constants.PROPS_EXT ) );
			}
		} );
		// root level must have exactly one MASTER_*.properties file
		if( files == null || files.length != 1 ) {
			throw new InvalidPipelineException( getPipeDir() );
		}
		return files[ 0 ];
	}
	
	protected String setProjectName() {
		return PipelineUtil.getProjectNameFromPropFile( getMasterConfig() );
	}

	protected String setPipelineId() {
		String configName = getMasterConfig().getName();
		String name = configName;
		if( configName.startsWith( Constants.MASTER_PREFIX ) && configName.endsWith( Constants.PROPS_EXT )) {
			name = configName.replaceFirst( Constants.MASTER_PREFIX, "" );
			name = name.substring( 0, name.length()-Constants.PROPS_EXT.length() );
		}
		return name;
	}
	
}
