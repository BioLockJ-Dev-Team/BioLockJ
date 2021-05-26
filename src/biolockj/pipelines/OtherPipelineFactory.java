package biolockj.pipelines;

import java.io.File;
import biolockj.exception.InvalidPipelineException;

public class OtherPipelineFactory {

	// no instantiation
	private OtherPipelineFactory() {}
	
	//TODO use package permission once PipelineUtil is moved to this package
	public static OtherPipeline asPipeline(final File dir) throws InvalidPipelineException {
		//is this a potential pipeline dir --exists and is a directory ?
		// what version of BioLockJ made this pipeline ?
		// Use version to determine which class of OtherPipeline to use for it
		return new OtherPipelineImpl(dir);
	}

}
