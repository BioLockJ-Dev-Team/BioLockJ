/**
 * @UNCC Fodor Lab
 * @author Ivory Blakley
 * @email ieclabau@uncc.edu
 * @date May 27, 2019
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.exception;

import biolockj.module.BioModule;
import biolockj.util.ModuleUtil;

/**
 * ValidationException is thrown at module runtime if the Validation module finds a discrepancy between the expectations
 * given for one or more files and the files found in the previous module.
 * 
 * @author Ivory Blakley
 */
public class ValidationException extends BioLockJException {

	/**
	 * Generate BioModule specific error message.
	 * 
	 * @param module BioModule
	 */
	public ValidationException( final BioModule module ) {
		super( buildMessage( module ) );
	}

	/**
	 * Default error message (not BioModule specific).
	 * 
	 * @param msg Error message
	 */
	public ValidationException( final String msg ) {
		super( msg );
	}

	private static String buildMessage( final BioModule module ) {
		return "This pipeline has validaiton turned on to verify the output of module " +
			ModuleUtil.displaySignature( module ) +
			". The output is different from the expectations, so the pipeline was halted.";
	}

	private static final long serialVersionUID = 9095697645997739998L;

}
