package biolockj.exception;

import biolockj.module.Stop;
import biolockj.util.ModuleUtil;

public class StopModuleException extends IntentionalStop {
	
	public StopModuleException(Stop module) {
		super( getMessage(module) );
	}
	
	private static String getMessage(Stop module) {
		return "This pipeline was configured to stop upon completeing module [ " + ModuleUtil.getPreviousModule( module ) + " ].";
	}
		
	private static final long serialVersionUID = 1L;

}
