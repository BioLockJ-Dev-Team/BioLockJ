package biolockj.module;

import java.util.Set;
import biolockj.exception.ConfigPathException;
import biolockj.exception.DockerVolCreationException;

/**
 * Implementing modules get to have write permissions outside of the pipeline.
 * @author ieclabau
 *
 */
public interface WritesOutsidePipeline extends BioModule {
	
	public Set<String> getWriteDirs () throws DockerVolCreationException, ConfigPathException;
	
}
