/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Mar 03, 2019
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.exception;

import biolockj.module.BioModule;
import biolockj.module.ScriptModule;
import biolockj.util.BioLockJUtil;
import biolockj.util.MetaUtil;

/**
 * PipelineScriptException is thrown by {@link biolockj.util.BashScriptBuilder} if errors occur writing MASTER & WORKER
 * scripts.
 */
public class PipelineScriptException extends BioLockJException {

	/**
	 * Create standard error to throw writing MASTER & WORKER bash scripts.
	 *
	 * @param module ScriptModule
	 * @param isWorker If the error is thrown while writing a worker script
	 * @param msg Exception message details
	 */
	public PipelineScriptException( final ScriptModule module, final boolean isWorker, final String msg ) {
		super( "Error writing " + ( isWorker ? "WORKER": "MAIN" ) + " script for: " + module.getClass().getName() +
			" --> " + msg + showCommonCauses(module) );
	}

	/**
	 * Create standard error to throw if problems occur generating pipeline bash scripts.
	 *
	 * @param module ScriptModule
	 * @param msg Exception message details
	 */
	public PipelineScriptException( final ScriptModule module, final String msg ) {
		super( "Error writing script for: " + module.getClass().getName() + " --> " + msg  + showCommonCauses(module));
	}

	/**
	 * Create a standard exception message.
	 *
	 * @param msg Exception message details
	 */
	public PipelineScriptException( final String msg ) {
		super( msg  + System.lineSeparator() + COMMON_CAUSES);
	}
	
	private static final String showCommonCauses( final BioModule module ) {
		StringBuilder sb = new StringBuilder();
		try {
			sb.append( System.lineSeparator() + COMMON_CAUSES );
			sb.append( System.lineSeparator() + System.lineSeparator() );
			sb.append( "Module input files: " + System.lineSeparator() );
			sb.append( BioLockJUtil.getCollectionAsString( module.getInputFiles() ) );
			sb.append( System.lineSeparator() );
			sb.append( "Current metadata samples:" + System.lineSeparator() );
			sb.append( BioLockJUtil.getCollectionAsString( MetaUtil.getSampleIds() ) );
		} catch( Exception ex ) {}
		return sb.toString();
	}
	
	public static final String COMMON_CAUSES = "Most often, this means there is not valid input for this module; this results from an upstream error, or a mis-match between the data and the metadata.";

	private static final long serialVersionUID = 3153279611111591414L;

}
