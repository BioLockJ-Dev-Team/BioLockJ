package biolockj.pipelines;

import java.io.File;
import biolockj.exception.InvalidPipelineException;
import biolockj.util.RuntimeParamUtil;

public interface OtherPipeline {
	
	/**
	 * Get the File object representing the top level of the pipeline folder.
	 * @return
	 */
	public File getPipeDir();
	
	/**
	 * Get the MASTER_*.properties file for a given pipeline.
	 * 
	 * @return the master config file for the pipeline
	 * @throws InvalidPipelineException 
	 */
	 public File getMasterConfig() throws InvalidPipelineException;
	 
	 public String getProjectName();
	 
	 public String getPipelineId();
	 
	 public boolean isModuleDir(final File dir);
	 
	/**
	 * Determine if a given pipeline was run in precheck mode, ie using the {@value RuntimeParamUtil#PRECHECK_FLAG}
	 * parameter.
	 * 
	 * @param pipelineDir
	 * @return
	 */
	public boolean isPrecheckPipeline();
	
	/**
	 * The current status flag.  The value should not be stored as a field but determined each time the method is called.
	 */
	public File getPipelineStatusFlag();

}
