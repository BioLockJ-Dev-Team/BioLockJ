/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 04, 2019
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
import biolockj.exception.ConfigViolationException;
import biolockj.module.report.taxa.NormalizeTaxaTables;
import biolockj.util.BioLockJUtil;
import biolockj.util.RMetaUtil;

/**
 * This BioModule is used to run the R script used to generate OTU-metadata fold-change-barplots for each binary report
 * field. A pdf is created for each taxonomy level.
 * 
 * @blj.web_desc R Plot Effect Size
 */
public class R_PlotEffectSize extends R_Module implements ApiModule {
	
	public R_PlotEffectSize() {
		super();
		addNewProperty( R_PLOT_EFFECT_SIZE_DISABLE_FC, Properties.BOOLEAN_TYPE, "Options: Y/N. If N (default), produce plots for binary attributes showing the fold change. If Y, skip this plot type." );
		addNewProperty( NO_COHENS_D, Properties.BOOLEAN_TYPE, "Options: Y/N. If N (default), produce plots for binary attributes showing effect size calculated as Cohen's d. If Y, skip this plot type." );
		addNewProperty( NO_R2, Properties.BOOLEAN_TYPE, "Options: Y/N. If N (default), produce plots showing effect size calculated as the r-squared value. If Y, skip this plot type." );
		addNewProperty( "r_PlotEffectSize.disablePvalAdj", Properties.BOOLEAN_TYPE, "Options: Y/N. If Y, the non-adjusted p-value is used when determining which taxa to include in the plot and which should get a (*). If N (default), the adjusted p-value is used." );
		addNewProperty( "r_PlotEffectSize.excludePvalAbove", Properties.NUMERTIC_TYPE, "Options: [0,1], Taxa with a p-value above this value are excluded from the plot." );
		addNewProperty( "r_PlotEffectSize.maxNumTaxa", Properties.INTEGER_TYPE, "Each plot is given one page. This is the maximum number of bars to include in each one-page plot." );
		addNewProperty( "r_PlotEffectSize.parametricPval", Properties.BOOLEAN_TYPE, "Options: Y/N. If Y, the parametric p-value is used when determining which taxa to include in the plot and which should get a (*). If N (default), the non-parametric p-value is used." );
		addNewProperty( "r_PlotEffectSize.taxa", Properties.LIST_TYPE, "Override other criteria for selecting which taxa to include in the plot by specifying wich taxa should be included" );
	}

	/**
	 * At least one of the available plot types should NOT be disabled.
	 */
	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		Config.getString( this, Constants.R_COLOR_HIGHLIGHT );

		// Use single "&" to ensure all config values saved to MASTER config
		if( Config.getBoolean( this, R_PLOT_EFFECT_SIZE_DISABLE_FC ) &&
			Config.getBoolean( this, NO_COHENS_D ) && Config.getBoolean( this, NO_R2 ) )
			throw new ConfigViolationException( NO_COHENS_D,
				"When using " + this.getClass().getName() + " at least one of " + NO_COHENS_D + ", " + NO_R2 + ", or " +
					R_PLOT_EFFECT_SIZE_DISABLE_FC + " must not be true." );

		if( !Config.getBoolean( this, R_PLOT_EFFECT_SIZE_DISABLE_FC ) &&
			RMetaUtil.getBinaryFields( this ).isEmpty() )
			throw new ConfigViolationException( R_PLOT_EFFECT_SIZE_DISABLE_FC,
				"Requires binary report fields" );
	}

	/**
	 * Returns {@link Constants#DEFAULT_STATS_MODULE} and if fold change plots are to be generated, add
	 * {@link biolockj.module.report.taxa.NormalizeTaxaTables}
	 */
	@Override
	public List<String> getPreRequisiteModules() throws Exception {
		List<String> preReqs = super.getPreRequisiteModules();
		if( !Config.getBoolean( this, R_PLOT_EFFECT_SIZE_DISABLE_FC ) &&
			!BioLockJUtil.pipelineInputType( BioLockJUtil.PIPELINE_NORMAL_TAXA_COUNT_TABLE_INPUT_TYPE ) )
			preReqs.add( NormalizeTaxaTables.class.getName() );
		preReqs.add( Config.getString( null, Constants.DEFAULT_STATS_MODULE ) );
		return preReqs;
	}

	private static final String NO_COHENS_D = "r_PlotEffectSize.disableCohensD";
	private static final String NO_R2 = "r_PlotEffectSize.disableRSquared";

	/**
	 * {@link biolockj.Config} Boolean property to disable fold change plots: {@value #R_PLOT_EFFECT_SIZE_DISABLE_FC}
	 */
	private static final String R_PLOT_EFFECT_SIZE_DISABLE_FC = "r_PlotEffectSize.disableFoldChange";

	
	@Override
	public String getDescription() {
		return "Generate horizontal barplot representing effect size (Cohen's d, r<sup>2</sup>, and/or fold change) for each reportable metadata field and each *report.taxonomyLevel* configured.";
	}

	@Override
	public String getCitationString() {
		return "BioLockJ " + BioLockJUtil.getVersion() + System.lineSeparator() + "Module developed by Ivory Blakley.";
	}

	@Override
	protected String getModuleRScriptName() {
		return "R_PlotEffectSize.R";
	}

	@Override
	protected String getModulePrefix() {
		return "r_PlotEffectSize";
	}

}
