/**
 * @UNCC Fodor Lab
 * @author Anthony Fodor
 * @email anthony.fodor@gmail.com
 * @date Feb 9, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.report.taxa;

import java.util.*;
import biolockj.Config;
import biolockj.Constants;
import biolockj.Log;
import biolockj.api.ApiModule;
import biolockj.exception.ConfigFormatException;
import biolockj.util.BioLockJUtil;

/**
 * This utility is used to log-transform the raw OTU counts on Log10 or Log-e scales.
 * 
 * @blj.web_desc Log Transform Taxa Tables
 */
public class LogTransformTaxaTables extends TransformTaxaTables implements ApiModule {
	
	public LogTransformTaxaTables() {
		super();
		addGeneralProperty( Constants.REPORT_LOG_BASE );
	}
	
	/**
	 * Verify {@link biolockj.Config}.{@value biolockj.Constants#REPORT_LOG_BASE} property is valid (if defined) with a
	 * value = (e or 10).
	 *
	 * @throws ConfigFormatException if REPORT_LOG_BASE is not set to a valid option (e or 10)
	 */
	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		isValidProp(Constants.REPORT_LOG_BASE);
	}
	
	@Override
	public Boolean isValidProp( String property ) throws Exception {
	    Boolean isValid = super.isValidProp( property );
	    switch(property) {
	        case Constants.REPORT_LOG_BASE:
	        	this.logBase = Config.requireString( this, Constants.REPORT_LOG_BASE );
	    		if( !getLogBase().equalsIgnoreCase( LOG_10 ) && !getLogBase().equalsIgnoreCase( LOG_E ) ) {
	    			throw new ConfigFormatException( Constants.REPORT_LOG_BASE,
	    				"Property only accepts value \"" + LOG_10 + "\" or \"" + LOG_E + "\"" );
	    		}
	    		Log.debug( getClass(), "Found logBase: " + this.getLogBase() );
	            isValid = true;
	            break;
	    }
	    return isValid;
	}

	/**
	 * Log transform the data
	 *
	 * @param otuTable OTU raw count table
	 * @throws Exception if unable to construct LogTransformTaxaTables
	 */
	@Override
	protected TaxaLevelTable transform(
		TaxaLevelTable inputData,
		List<String> filteredSampleIDs,
		List<String> filteredTaxaIDs) throws Exception {
		Log.debug( getClass(), "Log transforming table for level: " + inputData.getLevel() );
		TaxaLevelTable newData = new TaxaLevelTable(inputData.getLevel());
		for (String sampleID : filteredSampleIDs) {
			newData.newSampleRow( sampleID );
			for (String taxaID : filteredTaxaIDs) {
				Double oldVal = inputData.get( sampleID ).get( taxaID );
				Double newVal = logTransform(oldVal);
				newData.get( sampleID ).put(taxaID, newVal);
			}
		}
		return(newData);
	}
	
	private Double logTransform(Double val) {
		Double res = null;
		if( getLogBase().equalsIgnoreCase( LOG_E ) ) {
			res = new Double( Math.log( val ) );
		} else if( getLogBase().equalsIgnoreCase( LOG_10 ) ) {
			res = new Double( Math.log10( val ) );
		} 
		return res;
	}
	
	
	@Override
	protected String getProcessSuffix() {
		return "Log" + getLogBase();
	}
	
	/**
	 * Get the Log base (10 or e)
	 * 
	 * @return Log base
	 */
	protected String getLogBase() {
		return this.logBase;
	}
	
	@Override
	public String getSummary() throws Exception {
		return super.getSummary() + "Data were transformed to a Log(base=" + Config.getString( this, Constants.REPORT_LOG_BASE ) + ") scale.";
	}
	

	/**
	 * Log 10 display string a supported value for: {@value biolockj.Constants#REPORT_LOG_BASE}
	 */
	protected static final String LOG_10 = "10";

	/**
	 * Log e display string a supported value for: {@value biolockj.Constants#REPORT_LOG_BASE}
	 */
	protected static final String LOG_E = "e";
	
	private String logBase = "";

	@Override
	public String getDescription() {
		return "Log-transform the raw taxa counts on Log10 or Log-e scales.";
	}

	@Override
	public String getCitationString() {
		return "Module developed by Mike Sioda" + System.lineSeparator()
		+ "BioLockJ " + BioLockJUtil.getVersion();
	}
}
