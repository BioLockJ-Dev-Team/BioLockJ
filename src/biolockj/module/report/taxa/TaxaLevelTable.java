package biolockj.module.report.taxa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class TaxaLevelTable extends HashMap<String, HashMap<String, Double>>{

	private String level;
	public String getLevel() { return level;}
	public TaxaLevelTable(String level){
		this.level = level;
	}
	
	public HashMap<String, Double> newSampleRow(String sampleID){
		HashMap<String, Double> newRow = new HashMap<>();
		this.put(sampleID, newRow);
		return newRow ;
	}
	
	public List<String> listTaxa (){
		Set<String> bigSet = new TreeSet<>();
		for (String sample : this.keySet() ) {
			bigSet.addAll( this.get( sample ).keySet() );
		}
		List<String> allTaxa = new ArrayList<>();
		allTaxa.addAll( bigSet );
		Collections.sort(allTaxa);
		return allTaxa;
	}
	
	public List<String> listSamples (){
		final List<String> allSampleIDs = new ArrayList<>();
		allSampleIDs.addAll( this.keySet() );
		Collections.sort(allSampleIDs);
		return allSampleIDs;
	}
	
	public Double addValue(final String sample, final String taxon, final Double value) {
		HashMap<String, Double> row = this.keySet().contains( sample ) ? this.get( sample ) : newSampleRow(sample);
		Double oldValue = row.containsKey( taxon ) ? row.get( taxon ) : new Double(0);
		row.put( taxon, Double.sum( oldValue, value ) );
		return row.get( taxon );
	}
	
	/**
	 * Replace all null values in the maps with 0.
	 * @return the fraction of all values that were null;
	 */
	public float fillEmptyVals() {
		return fillEmptyVals( new Double(0) );
	}
	/**
	 * Replace all null values in the maps with the given value.
	 * @param value new value to use in place of null
	 * @return the fraction of all values that were null
	 */
	public float fillEmptyVals(Double value) {
		long total = 0;
		long replaced = 0;
		List<String> allTaxa = listTaxa();
		List<String> allSamples = listSamples();
		for (String sample : allSamples ) {
			HashMap<String, Double> row = this.get( sample );
			long hadVals = row.size();
			for (String taxon : allTaxa) {
				if ( ! row.containsKey( taxon ) ) row.put(taxon, value); 
			}
			long numFilled = row.size() - hadVals;
			total = total + row.size();
			replaced = replaced + numFilled;
		}
		return (float) replaced / total;
	}
	
	private static final long serialVersionUID = 3873959114273802005L;
}
