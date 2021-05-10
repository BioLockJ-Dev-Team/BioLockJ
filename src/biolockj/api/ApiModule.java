package biolockj.api;

import java.util.List;
import biolockj.module.BioModule;

public interface ApiModule extends BioModule {

	/**
	 * Tests to see if the value val is valid for property prop; primarily tests format. This is designed for individual
	 * properties. To make sure property values are compatible, use
	 * {@link biolockj.module.BioModule#checkDependencies()}. Using switch/case or a stack of if/else is recommended.
	 * Within each case, call any/all method that is used by this module to access the value from the config file,
	 * leveraging the checks in the Config.get* methods.
	 * 
	 * This method should never actually return false. If the value is not valid, it should throw an exception that
	 * includes a helpful message about whats not valid. As part of a throwable, that message is passed along to
	 * wherever the call started. Any time that "false" is actually the desired form, this method should be wrapped in
	 * a try/catch.
	 * 
	 * @param property
	 * @param value
	 * @return true if value is recognized and good, false/Exception if it is recognized and invalid, null if prop is
	 * not recognized.
	 * @throws Exception
	 */
	public Boolean isValidProp(String property) throws Exception;
	
	/**
	 * List properties that this module uses, including those called by any super class.
	 * @return
	 */
	public List<String> listProps();

	/**
	 * Get a human readable name for this module.
	 * @return
	 */
	public String getTitle();
	
	/**
	 * Get a list of Strings describing the menu structure that should be used in the GUI.
	 * By default (BioModuleImpl) this is the just the package structure of the module class.
	 * The option to override the this method is a way to de-couple developers organization from the presentation to the user.
	 */
	public List<String> getMenuPlacement();

	/**
	 * Briefly describe what this module does. 
	 * A more detailed description may be returned by {@link getDetails}.
	 * @return
	 */
	public String getDescription();

	/**
	 * Describe a given property / how it is used (including how it is used by a super class)
	 * @return
	 */
	public String getDescription(String prop) throws API_Exception;
	
	/**
	 * A extension of {@link getDescription}. Beyond the brief description, give details such as
	 * the interaction between properties.
	 * @return
	 */
	public String getDetails() throws API_Exception;
	
	/**
	 * Get the type for a given property.
	 * @return
	 */
	public String getPropType(String prop) throws API_Exception;
	
	/**
	 *  At a minimum, this should return the name and/or url for the wrapped tool.
	 *  For BioLockJ home-grown modules, it can cite BioLockJ and give the current version.
	 *  Ideally this will include the version for the wrapped tool. Tool version may require using
	 *  a stored variable that can be filled in during execution; and the string that is returned pre-execution may not have the version.
	 * @return
	 */
	public String getCitationString();
	
}
