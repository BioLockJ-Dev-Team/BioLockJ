package biolockj.module.getData;

import java.util.Set;

/**
 * This interface identifies modules that retrieve data.
 * These modules contribute to BioLockJUtil.INTERNAL_PIPELINE_INPUT_TYPES.
 * 
 * @author Ivory Blakley
 *
 */
public interface InputData {

	// TODO: Eventually add this to include in the summary 
//	/**
//	 * Give the url, or similar description of where the data was obtained from.
//	 * @return
//	 */
//	public String getDataSource();
	
	public Set<String> getInputDataTypes();
	
}
