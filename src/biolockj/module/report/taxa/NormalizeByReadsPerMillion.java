package biolockj.module.report.taxa;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import biolockj.Config;
import biolockj.Constants;
import biolockj.Log;
import biolockj.api.API_Exception;
import biolockj.api.ApiModule;
import biolockj.api.BuildDocs;
import biolockj.util.BioLockJUtil;
import biolockj.util.TaxaUtil;

/**
 * Normalize each sample for sequencing depth by reporting each value as the number 
 * of counts per million counts in a given sample.
 * @author Ivory
 *
 */
public class NormalizeByReadsPerMillion extends TransformTaxaTables implements ApiModule {
	
	private static final Double MILLION = new Double( 1000000 );

	@Override
	protected TaxaLevelTable transform( TaxaLevelTable inputData, List<String> filteredSampleIDs,
		List<String> filteredTaxaIDs ) throws Exception {
		
		boolean foundNonIntVals = false;
		String level = inputData.getLevel();
		Log.info( getClass(), "Normalizing table for level: " + level );
		summary += Constants.RETURN + "Normalization factor per sample (" + level + "): ";
		
		TaxaLevelTable newData = new TaxaLevelTable(level);
		for (String sampleID : filteredSampleIDs) {
			newData.newSampleRow( sampleID );
			Double rowSum = inputData.get( sampleID ).values().stream().collect( Collectors.summingDouble( Double::valueOf ) );
			Log.debug(getClass(), "rowSum [" + sampleID + "] = " + rowSum);
			Double NormFactor = rowSum / MILLION;
			summary += Constants.RETURN + sampleID + ": " + NormFactor;
			for ( String taxaID : filteredTaxaIDs ) {
				Double rawValue = inputData.get( sampleID ).get( taxaID );
				if (rawValue.intValue() - rawValue != 0) foundNonIntVals = true;
				Double newValue = rawValue / NormFactor ;
				newData.get( sampleID ).put( taxaID, newValue );
			}
		}
		
		if (foundNonIntVals) {
			Log.warn(getClass(), "The input table contains non-integer values.  This normalization method is designed to be used on raw counts data (ie integers).");
			summary += Constants.RETURN + "The \"Reads Per Million\" normalization metric is typically applied to raw counts; here it was applied to non-integer values.";
		}
		
		return(newData);
	}

	@Override
	protected String getProcessSuffix() {
		return TaxaUtil.NORMALIZED + "_RPM";
	}
	
	@Override
	public String getSummary() throws Exception {
		return super.getSummary() + System.lineSeparator() + getDescription() + summary;
	}
	
	@Override
	public List<String> getPostRequisiteModules() throws Exception {
		List<String> postModules = super.getPostRequisiteModules();
		if (Config.getString( this, Constants.REPORT_LOG_BASE ) != null) {
			postModules.add( LogTransformTaxaTables.class.getName() );
			Log.info(getClass(), "Adding LogTransformTaxaTables module because property [" + Constants.REPORT_LOG_BASE + "] is not null.");
		}
		return postModules;
	}
	
	@Override
	public List<String> getPreRequisiteModules() throws Exception {
		List<String> preModules = new ArrayList<>();
		preModules.addAll( super.getPreRequisiteModules() );
		if (Config.getString( this, Constants.REPORT_LOG_BASE ) != null) {
			preModules.add( AddPseudoCount.class.getName() );
			Log.info(getClass(), "Adding AddPseudoCount module because property [" 
			+ Constants.REPORT_LOG_BASE + "] is not null; and this normalization does not remove 0's.");
		}
		return preModules;
	}
	
	private String summary = "";

	@Override
	public String getDescription() {
		return "new counts = counts / (total counts in sample / 1 million)";
	}
	
	@Override
	public String getDetails() throws API_Exception {
		return "Represent each count value in the table as: counts / (total counts in sample / 1 million)"
						+ Constants.markDownReturn + "If report log base is not null, *LogTransformTaxaTables* is added as a post-requisite to do the log transformation and *AddPseudoCount* is added as a pre-requisite before normalization to avoid taking the log of 0."
						+ Constants.markDownReturn + super.getDetails();
	}

	@Override
	public String getCitationString() {
		return "Module developed by Ivory Blakley.";
	}
	
	@Override
	public String version() {
		return "1.0.0";
	}

}
