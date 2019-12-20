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
import java.util.stream.Collectors;
import biolockj.*;
import biolockj.exception.BioLockJException;
import biolockj.util.TaxaUtil;

/**
 * This utility is used to normalize and/or log-transform the raw OTU counts using the formulas:
 * <ul>
 * <li>Normalized OTU count formula = (RC/n)*((SUM(x))/N)+1
 * <li>Relative abundance formula = Log(log_base) [ (RC/n)*((SUM(x))/N)+1 ]
 * </ul>
 * The code implementation supports (log_base = e) and (log_base = 10) which is configured via
 * {@link biolockj.Constants#REPORT_LOG_BASE} property.
 * <ul>
 * <li>RC = Sample OTU count read in from each Sample-OTU cell in the raw count file passed to the constructor
 * <li>n = number of sequences in the sample, read in as the row sum (sum of OTU counts for the sample)
 * <li>SUM(x) = total number of counts in the table, read in as the table sum (sum of OTU counts for all samples)
 * <li>N = total number of samples, rowCount - 1 (header row)
 * </ul>
 * Further explanation regarding the normalization scheme, please read The ISME Journal 2013 paper by Dr. Anthony Fodor:
 * "Stochastic changes over time and not founder effects drive cage effects in microbial community assembly in a mouse
 * model" <a href= "https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3806260/" target=
 * "_top">https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3806260/</a>
 * 
 * @blj.web_desc Normalize Taxa Tables
 */
public class NormalizeTaxaTables extends TransformTaxaTables {

	/**
	 * Populate normalized counts with the formula: (RC/n)*((SUM(x))/N)+1
	 *
	 * @param taxaTable OTU raw count table
	 * @throws Exception if unable to construct NormalizeTaxaTables
	 */
	protected TaxaLevelTable  transform(
		TaxaLevelTable inputData,
		List<String> filteredSampleIDs,
		List<String> filteredTaxaIDs) throws Exception {
		
		String level = inputData.getLevel();
		Log.info( getClass(), "Normalizing table for level: " + level );
		
		HashMap<String, Double> rowSums = new HashMap<>();
		for (String sampleID : filteredSampleIDs) {
			Log.debug( getClass(), "Adding values from sample: " + sampleID );
			Double rowSum = inputData.get( sampleID ).values().stream().collect( Collectors.summingDouble( Double::valueOf ) );
			rowSums.put(sampleID, rowSum);
			Log.debug(getClass(), "rowSum = " + rowSum);
		}
		Double tableSum = rowSums.values().stream().collect( Collectors.summingDouble( Double::valueOf ) );
		Log.debug(getClass(), "tableSum = " + tableSum);
		
		Double averageSampleSum = tableSum / filteredSampleIDs.size(); 
		summary += Constants.RETURN + "Total table (" + level + "): " + tableSum;
		summary += Constants.RETURN + "Average sample sequencing depth (" + level + "): " + averageSampleSum;
		
		if (averageSampleSum.isInfinite()) {
			String msg = "The calculated average sample sequencing depth is infinitly large.";
			Log.error(getClass(), msg);
			throw new BioLockJException(msg);
		}
		
		TaxaLevelTable newData = new TaxaLevelTable(level);
		for (String sampleID : filteredSampleIDs) {
			newData.newSampleRow(sampleID);
			for ( String taxaID : filteredTaxaIDs ) {
				Double RC = inputData.get( sampleID ).get( taxaID );
				Double n = rowSums.get( sampleID );
				Double newValue = ( RC / n.doubleValue() ) * averageSampleSum ;
				newData.get( sampleID ).put( taxaID, newValue + 1 );
			}
		}
		
		return(newData);
	}

	@Override
	public String getSummary() throws Exception {
		return super.getSummary() + this.summary;
	}
	
	@Override
	protected String getProcessSuffix() {
		return TaxaUtil.NORMALIZED;
	}
	
	@Override
	public List<String> getPostRequisiteModules() throws Exception {
		List<String> postModules = new ArrayList<>();
		postModules.addAll( super.getPostRequisiteModules() );
		if (Config.getString( this, Constants.REPORT_LOG_BASE ) != null) {
			postModules.add( LogTransformTaxaTables.class.getName() );
		}
		return postModules;
	}
	
	private String summary = "";

}
