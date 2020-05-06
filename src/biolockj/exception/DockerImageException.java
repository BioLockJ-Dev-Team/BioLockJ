package biolockj.exception;

import biolockj.module.BioModule;
import biolockj.util.ModuleUtil;

public class DockerImageException extends BioLockJException {

	public DockerImageException(BioModule module, String image, String msg){
		super( buildMsg(module, image, msg));
	}
	
	private static String buildMsg(BioModule module, String image, String msg) {
		return "BioModule " + ModuleUtil.displaySignature( module ) 
			+ " is set up to use docker image [" + image + "]; this is not a suitable image for running a BioLockJ module." 
			+ System.lineSeparator() + System.lineSeparator() + "Reason: " + msg;
	}
	
	private static final long serialVersionUID = 466423547228588939L;
	
}
