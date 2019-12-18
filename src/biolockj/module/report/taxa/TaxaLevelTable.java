package biolockj.module.report.taxa;

import java.util.HashMap;

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
	
	private static final long serialVersionUID = 3873959114273802005L;
}
