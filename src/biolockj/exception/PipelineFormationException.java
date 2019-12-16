package biolockj.exception;

import java.util.List;
import biolockj.Log;
import biolockj.Pipeline;
import biolockj.module.BioModule;
import biolockj.util.BioLockJUtil;

/**
 * PipelineFormationException is thrown when there is a problem in setting up the modules to run.
 * For example, if there is problem in reading the module run order, or initializing the pipeline.
 * @author Ivory
 *
 */
public class PipelineFormationException extends BioLockJException {

	public PipelineFormationException( String msg ) {
		super( msg );
		List<BioModule> modules = Pipeline.getModules();
		if ( modules != null && modules.size() > 0 ) {
			Log.info(this.getClass(), "The modules defined thus far are: "
				 + BioLockJUtil.getCollectionAsString( modules ));
		}else {
			Log.info(this.getClass(), "No modules have been added to the pipeline yet.");
		}
	}

	private static final long serialVersionUID = -392199812676699152L;

}
