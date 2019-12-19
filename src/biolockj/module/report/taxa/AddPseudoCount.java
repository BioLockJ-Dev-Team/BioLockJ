package biolockj.module.report.taxa;

import java.util.List;
import biolockj.Log;

public class AddPseudoCount extends TransformTaxaTables {

	@Override
	protected TaxaLevelTable transform( TaxaLevelTable inputData, List<String> filteredSampleIDs,
		List<String> filteredTaxaIDs ) throws Exception {
		
		boolean foundNonIntVals = false;
		boolean found0s = false;
		String level = inputData.getLevel();
		Log.info( getClass(), "Adding 1 to each value in table for level: " + level );
		
		TaxaLevelTable newData = new TaxaLevelTable(level);
		for (String sampleID : filteredSampleIDs) {
			newData.newSampleRow( sampleID );
			for ( String taxaID : filteredTaxaIDs ) {
				Double rawValue = inputData.get( sampleID ).get( taxaID );
				if (rawValue.intValue() - rawValue != 0) foundNonIntVals = true;
				if (rawValue.intValue() == 0) found0s = true;
				Double newValue = rawValue + 1 ;
				newData.get( sampleID ).put( taxaID, newValue );
			}
		}
		
		if (foundNonIntVals) {
			Log.warn(getClass(), "The input table contains non-integer values.  Adding a pseudo count is only appropriate on raw counts data (ie integers).");
		}
		if (!found0s) {
			Log.warn(getClass(), "No 0's were found in the input table.  Adding a pseudo count is typically done to avoid 0's.");
		}
		
		return(newData);
	}
	
	@Override
	public String getSummary() throws Exception {
		return super.getSummary() + RETURN + "Added a pseudocount (+1) to each value in each taxa table.";
	}

	@Override
	protected String getProcessSuffix() {
		return "p1";
	}

}
