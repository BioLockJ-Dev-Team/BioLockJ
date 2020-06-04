/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 18, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.report.r;

import java.util.List;
import biolockj.Config;
import biolockj.Constants;
import biolockj.Properties;
import biolockj.api.ApiModule;
import biolockj.util.BioLockJUtil;

/**
 * This BioModule is used to build the R script used to generate OTU-metadata box-plots and scatter-plots for each
 * report field and taxonomy level.
 * 
 * @blj.web_desc R Plot OTUs
 */
public class R_PlotOtus extends R_Module implements ApiModule {
	
	public R_PlotOtus() {
		super();
		addNewProperty( R_PVAL_FORMAT, Properties.STRING_TYPE, "Sets the format used in R sprintf() function" );
		addGeneralProperty( Constants.R_RARE_OTU_THRESHOLD );
		addGeneralProperty( Constants.R_COLOR_BASE );
		addGeneralProperty( Constants.R_COLOR_HIGHLIGHT );
		addGeneralProperty( Constants.R_COLOR_PALETTE );
		addGeneralProperty( Constants.R_COLOR_POINT );
		addGeneralProperty( Constants.R_PCH );
	}

	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		Config.getString( this, R_PVAL_FORMAT );
		Config.getPositiveDoubleVal( this, Constants.R_RARE_OTU_THRESHOLD );
		Config.getString( this, Constants.R_COLOR_BASE );
		Config.getString( this, Constants.R_COLOR_HIGHLIGHT );
		Config.getString( this, Constants.R_COLOR_PALETTE );
		Config.getString( this, Constants.R_COLOR_POINT );
		Config.getString( this, Constants.R_PCH );
		Config.getPositiveInteger( this, Constants.SET_SEED );
	}

	/**
	 * Returns {@link #getStatPreReqs()} unless the pipeline input is already a stats table.
	 */
	@Override
	public List<String> getPreRequisiteModules() throws Exception {
		List<String> preReqs = super.getPreRequisiteModules();
		if( !BioLockJUtil.pipelineInputType( BioLockJUtil.PIPELINE_STATS_TABLE_INPUT_TYPE ) )
			preReqs.add( Config.getString( null, Constants.DEFAULT_STATS_MODULE ) );		
		return preReqs;
	}

	@Override
	public String getDescription() {
		return "Generate OTU-metadata box-plots and scatter-plots for each reportable metadata field and each *report.taxonomyLevel* configured";
	}

	@Override
	public String getCitationString() {
		return "BioLockJ " + BioLockJUtil.getVersion() + System.lineSeparator() + "Module created by Mike Sioda and developed by Ivory Blakley";
	}

	@Override
	protected String getModuleRScriptName() {
		return "R_PlotOtus.R";
	}

	@Override
	protected String getModulePrefix() {
		return "r_PlotOtus";
	}
	
	private static final String R_PVAL_FORMAT = "r.pValFormat";

}
