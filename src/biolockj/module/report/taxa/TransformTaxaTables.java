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

import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import biolockj.*;
import biolockj.util.*;

/**
 * This utility is used to normalize, log-transform or othewise transfrom data that is stored as taxa counts.
 * This uses the TaxaCountModule methods to read taxa table data, 
 * and extends the functionality to writing taxa table data.
 * 
 */
public abstract class TransformTaxaTables extends TaxaCountModule {
		
	@Override
	public void runModule() throws Exception {
		for( final File file: getInputFiles() ) {
			TaxaLevelTable inputData = TaxaUtil.readTaxaTable(file);
			List<String> filteredSampleIDs = filterSamples(inputData);
			List<String> filteredTaxaIDs = filterTaxa(inputData);
			TaxaLevelTable transformedData = transform( inputData, filteredSampleIDs, filteredTaxaIDs );
			TaxaUtil.writeDataToFile( getOutputFile(file), filteredSampleIDs, filteredTaxaIDs, transformedData );
		}
		Log.info(getClass(), "Output " + getOutputDir().listFiles().length );
	}

	/**
	 * Transform a table of values.
	 * The transform method may or may not make use of the values that were filtered out.
	 *
	 * @return A  
	 * @param inputData the data to be transformed without filtering
	 * @param filteredSampleIDs Sample ids after filtering
	 * @param filteredTaxaIDs Taxa names after filtering
	 * @throws Exception
	 */
	protected abstract TaxaLevelTable  transform(
		TaxaLevelTable inputData,
		List<String> filteredSampleIDs,
		List<String> filteredTaxaIDs) throws Exception ;
	
	/**
	 * Filter out any samples in which all values are 0.
	 * @param inputData
	 * @return
	 */
	protected List<String> filterSamples( HashMap<String, HashMap<String, Double>> inputData ){
		final Set<String> allSampleIDs = inputData.keySet();
		final Set<String> allZeroSamples = new TreeSet<>();
		final List<String> filteredSampleIDs = new ArrayList<>();
		for (String id : allSampleIDs) {
			BigDecimal rowSum = inputData.get( id ).values().stream()
							.map( val -> BigDecimal.valueOf( val ) )
							.reduce(BigDecimal.ZERO, BigDecimal::add);
			if ( rowSum.equals( BigDecimal.ZERO ) ) {
				allZeroSamples.add( id );
			}else {
				filteredSampleIDs.add( id );
			}
		}
		
		Log.info( getClass(), "# samples in input file: " + allSampleIDs.size() );
		Log.info( getClass(), "# samples removed due to all-0 counts: " + allZeroSamples.size() );
		if (allZeroSamples.size() > 0 ) {
			Log.info( getClass(), "Removed samples: " + allZeroSamples.toString() );
		}
		
		Collections.sort( filteredSampleIDs );
		return( filteredSampleIDs );
	}
	
	protected List<String> filterTaxa( HashMap<String, HashMap<String, Double >> inputData ){
		
		Set<String> bigSet = new TreeSet<>();
		for (String sample : inputData.keySet() ) {
			bigSet.addAll( inputData.get( sample ).keySet() );
		}
		
		List<String> allTaxa = new ArrayList<>();
		allTaxa.addAll( bigSet );
		Collections.sort(allTaxa);
		
		return allTaxa;
	};

	protected File getOutputFile(File inputFile) throws Exception{
		String taxaTableSuffix = TaxaUtil.FILE_EXT;
		String level  = TaxaUtil.getTaxonomyTableLevel( inputFile );
		String ending = level + taxaTableSuffix;
		String name = inputFile.getName().replaceAll( ending, getProcessSuffix() + "_" + ending);
		return new File(getOutputDir(), name);
	};
	
	protected abstract String getProcessSuffix();

}
