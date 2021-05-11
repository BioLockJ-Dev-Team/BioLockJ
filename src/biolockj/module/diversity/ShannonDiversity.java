package biolockj.module.diversity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import biolockj.Log;
import biolockj.api.ApiModule;
import biolockj.module.BioModule;
import biolockj.module.report.taxa.BuildTaxaTables;
import biolockj.module.report.taxa.TaxaLevelTable;
import biolockj.module.report.taxa.TransformTaxaTables;
import biolockj.util.BioLockJUtil;

public class ShannonDiversity extends TransformTaxaTables implements ApiModule
{
	
	/*
	 * Calculate the Shannon diversity for each sample within a table.
	 */
	@Override
	protected TaxaLevelTable transform( TaxaLevelTable inputData, List<String> filteredSampleIDs,
		List<String> filteredTaxaIDs ) throws Exception {
		
		Log.debug(this.getClass(), "Doing shannon diversity for for [" + inputData.getLevel() + "] level.");
		List<String> sampleNames = inputData.listSamples();
		
		TaxaLevelTable newData = new TaxaLevelTable(inputData.getLevel());
		for(String name : sampleNames)
		{
			Double val = getShannonEntropy(inputData, name);
			HashMap<String, Double> cell = new HashMap<>();
			cell.put( SHANNON_COLUMN, val );
			newData.put(name, cell);
		}
		Log.debug(this.getClass(), "Finished ShannonDiversity calculation for [" + inputData.getLevel() + "] level.");
		return newData;
	}
	
	protected List<String> filterTaxa( TaxaLevelTable inputData ){
		List<String> list = new ArrayList<>();
		list.add( SHANNON_COLUMN );
		return list;
	};
	
	private double getShannonEntropy(TaxaLevelTable table, String sample) throws Exception
	{
		double sum = 0;

		Collection<Double> innerList = table.get( sample ).values();

		for (Double d : innerList)
			sum += d;

		List<Double> newList = new ArrayList<Double>();

		for (Double d : innerList)
			newList.add(d / sum);

		sum = 0;
		for (Double d : newList)
			if (d > 0)
			{
				sum += d * Math.log(d);

			}

		return -sum;
	}

	@Override
	public String getDescription()
	{
		return "Calculate shannon diversity as sum p(logp)";
	}

	@Override
	public String getCitationString()
	{	
		return "Module developed by Anthony Fodor.";
	}
	
	@Override
	public String version() {
		return "1.0.0";
	}

	@Override
	public boolean isValidInputModule( final BioModule module ) {
		return module instanceof BuildTaxaTables;
	}

	@Override
	protected String getProcessSuffix() {
		return "shannon";
	}
	
	public static final String SHANNON_COLUMN = "ShannonDiversity";
	
}
