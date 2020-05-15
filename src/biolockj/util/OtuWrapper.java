/** 
 * Author:  anthony.fodor@gmail.com    
 * This code is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version,
* provided that any use properly credits the author.
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details at http://www.gnu.org * * */


package biolockj.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

public class OtuWrapper
{
	/*
	 * All of these lists will be made unmodifiable (and hence thread safe) in
	 * the constructor
	 */
	// lists are sample then otu
	private List<List<Double>> dataPointsNormalized = new ArrayList<List<Double>>();
	private List<List<Double>> dataPointsUnnormalized = new ArrayList<List<Double>>();
	private List<List<Double>> dataPointsNormalizedThenLogged = new ArrayList<List<Double>>();
	private List<String> sampleNames = new ArrayList<String>();
	private List<String> otuNames = new ArrayList<String>();
	private double avgNumber;
	private final String filePath;

	public double getAvgNumber()
	{
		return avgNumber;
	}
	
	public int getSampleIdWithMinCounts() throws Exception
	{
		int returnVal = -1;
		int minCounts =Integer.MAX_VALUE;
		
		for( int x=0; x < this.getSampleNames().size(); x++)
		{
			int thisCounts = this.getCountsForSample(x);
			
			if( thisCounts < minCounts)
			{
				minCounts = thisCounts;
				returnVal = x;
			}
		}
		
		return returnVal;
	}
	
	public void writeRarifiedSpreadhseet(File filepath, boolean withReplacement, int depth) throws Exception
	{
		Random random = new Random(24231);
		int minIndex = getSampleIdWithMinCounts();
		
		
		int rarificaitonDepth = depth;
		if( depth <= 0)
		{
			depth = getCountsForSample(minIndex);
		}
		
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(filepath));
		
		writer.write("sample");
		
		for( String otu : otuNames)
			writer.write("\t" + otu);
		
		writer.write("\n");
		
		for( int x=0; x < getSampleNames().size(); x++)
		{
			writer.write(getSampleNames().get(x));
			
			int[] a = new int[getOtuNames().size()];
			List<Integer> sampleList = getSamplingList(x);

			Collections.shuffle(sampleList, random);
			
			if( ! withReplacement)
			{
				for( int y=0; y < rarificaitonDepth; y++)
					a[sampleList.get(y)]++;	
			}
			else
			{
				for( int y=0; y < rarificaitonDepth; y++)
				{
					a[sampleList.get(random.nextInt(sampleList.size()))]++;
				}
			}
			
			for( int y=0; y < a.length; y++)
				writer.write("\t" + a[y]);
			
			writer.write("\n");
			
		}
		
		writer.flush();  writer.close();
	}
	
	public List<Integer> getSamplingList(int sampleID) throws Exception
	{
		List<Integer> list = new ArrayList<Integer>();
		

		for(int x=0; x < getOtuNames().size(); x++)
		{
			int intVal = (int) getDataPointsUnnormalized().get(sampleID).get(x).doubleValue();
			
			if( intVal != getDataPointsUnnormalized().get(sampleID).get(x))
				throw new Exception("No");
			
			for( int y=0; y < intVal; y++ )
			{
				list.add(x);
			}
		}
		
		if( list.size() != getCountsForSample( sampleID ))
			throw new Exception("No");
		
		return list;
	}

	
	public int getSampleIdWithMostCounts() throws Exception
	{
		int returnVal = -1;
		int maxCounts =0;
		
		for( int x=0; x < this.getSampleNames().size(); x++)
		{
			int thisCounts = this.getCountsForSample(x);
			
			if( thisCounts > maxCounts)
			{
				maxCounts = thisCounts;
				returnVal = x;
			}
		}
		
		return returnVal;
	}
	
	public String getFilePath()
	{
		return filePath;
	}
	
	public static class MaxColumnHolder
	{
		public int taxaIndex;
		public double proportion;
	}
	
	public double getChaoRichness(String sampleName) throws Exception
	{
		return getChaoRichness(getIndexForSampleName(sampleName));
	}
	
	private static class TaxaCounts implements Comparable<TaxaCounts>
	{
		String taxaName;
		double counts =0;
		
		@Override
		public int compareTo(TaxaCounts o)
		{
			return Double.compare(o.counts, this.counts);
		}
	}
	
	public HashMap<String, Double> getTaxaListSortedByNumberOfCounts()
	{
		HashMap<String, TaxaCounts> tempMap = new HashMap<String,TaxaCounts>();
		
		for( int x=0; x < this.getSampleNames().size(); x++)
			for( int y=0; y < this.getOtuNames().size(); y++)
			{
				TaxaCounts tc = tempMap.get(this.getOtuNames().get(y));
				
				if(tc==null)
				{
					tc = new TaxaCounts();
					tempMap.put(this.getOtuNames().get(y), tc);
					tc.taxaName = this.getOtuNames().get(y);
				}
				
				tc.counts += dataPointsUnnormalized.get(x).get(y);
			}
		
		List<TaxaCounts> list = new ArrayList<TaxaCounts>( tempMap.values() );
		Collections.sort(list);
		
		HashMap<String, Double> map = new LinkedHashMap<String,Double>();
		
		for( TaxaCounts tc : list )
			map.put( tc.taxaName, tc.counts );
		
		return map;
	}
	
	/*
	 * Makes a new column called other and collapses all rare taxa 
	 * into that new category
	 */
	public void writeReducedOtuSpreadsheetsWithTaxaAsColumns(
			File newFilePath, int numTaxaToInclude) throws Exception
	{
		HashSet<String> toInclude = new LinkedHashSet<String>();
		HashMap<String, Double> taxaSorted = 
				getTaxaListSortedByNumberOfCounts();
		
		numTaxaToInclude = Math.min(numTaxaToInclude, taxaSorted.size());
		
		for( String s : taxaSorted.keySet() )
		{
			if( numTaxaToInclude > 0 )
			{
				toInclude.add(s);
				
				numTaxaToInclude--;
			}
		}
		
		//System.out.println("Including " + toInclude);
		
		if(toInclude.contains("other"))
			throw new Exception("Other already defined");
		
		HashMap<String, Double> otherCounts =new HashMap<String,Double>();
		
		for( int x=0; x < this.getSampleNames().size(); x++)
		{
			double counts =0;
			
			for(int y=0; y < this.getOtuNames().size(); y++)
				if( ! toInclude.contains(this.getOtuNames().get(y)))
					counts += this.dataPointsUnnormalized.get(x).get(y);
			
			otherCounts.put(this.getSampleNames().get(x), counts);
		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(newFilePath));
		
		writer.write("sample");
		
		for(String s : toInclude)
			writer.write("\t" + s);
		
		writer.write("\tother\n");
		
		for(int x=0;x < this.sampleNames.size(); x++)
		{
			writer.write(this.sampleNames.get(x));
			
			for(String s : toInclude)
			{
				writer.write( "\t" + this.dataPointsUnnormalized.get(x).get(getIndexForOtuName(s)) );
			}
				
			writer.write("\t" + otherCounts.get(this.sampleNames.get(x)) + "\n" );
		}
		
		writer.flush();  writer.close();
		
	}
	
	public double getChaoRichness( int sampleIndex) throws Exception
	{
		double richness = getRichness(sampleIndex);
		
		int singetons=0;
		int doubletons =0;
		
		List<Double> list = dataPointsUnnormalized.get(sampleIndex);
		
		for( Double d : list)
		{
			if( d== 1)
				singetons++;
			else if (d == 2)
				doubletons++;
		}
		
		return richness + singetons*(singetons-1) / 2*(doubletons +1);
	}
	
	public double getAverageRelativeAbundnace(int col)
	{
		List<Double> list = new ArrayList<Double>();
		
		for( int x=0; x < getSampleNames().size(); x++)
			list.add(getDataPointsUnnormalized().get(x).get(col));
		
		return new Avevar(list).getAve();
	}
	
	/*
	 * Takes the median as the middle position.
	 * Doesn't average if there are an even number of taxa
	 */
	public double getTaxaMedian(int col)
	{
		List<Double> taxaList = new ArrayList<Double>();
		
		for( int x=0; x < getSampleNames().size(); x++)
			taxaList.add(dataPointsUnnormalized.get(x).get(col));
		
		Collections.sort(taxaList);
		
		return taxaList.get(taxaList.size() / 2);
	}
	
	public double getCountForTaxaExcludingTheseSamples( int taxaIndex, HashSet<String> samples)
	{
		double sum =0;
		
		for( int x=0; x < getSampleNames().size(); x++)
			 if( ! samples.contains(getSampleNames().get(x)))
				 sum+= dataPointsUnnormalized.get(x).get(taxaIndex);
		
		return sum;
	}
	
	public double getTaxaAverageExcludingZeros(int col)
	{
		double sum =0;
		int n=0;
		
		List<List<Double>> list = getDataPointsUnnormalized();
		
		for( int x=0; x< getSampleNames().size(); x++ )
		{
			double val = list.get(x).get(col);
			
			if( val != 0)
			{
				sum+=val;
				n++;
			}
		}
		
		return sum / n;
	}
	
	public double getCountsForSampleExcludingTheseTaxa(int sampleIndex, HashSet<String> taxa)
	{
		double sum =0;
		
		for( int x=0; x < this.getOtuNames().size(); x++)
			if( ! taxa.contains( this.getOtuNames().get(x) ))
				sum += this.dataPointsUnnormalized.get(sampleIndex).get(x);
		
		return sum;
	}
	
	public double getMinimumExcludingZeros(int col)
	{
		double d = Double.MAX_VALUE;
		
		List<List<Double>> list = getDataPointsUnnormalized();
		
		for( int x=0; x< getSampleNames().size(); x++ )
		{
			double val = list.get(x).get(col);
			
			if( val != 0)
			{
				d = Math.min(d,  val);
			}
		}
		
		return d;
	}
	
	private static void throwIfDuplicateSamples(OtuWrapper wrapper1, OtuWrapper wrapper2)
		throws Exception
	{
		HashSet<String> set = new HashSet<String>();
		
		for(String s : wrapper1.getSampleNames())
		{
			if(set.contains(s))
				throw new Exception("Duplicate "  + s);
			
			set.add(s);
		}
		
		for(String s : wrapper2.getSampleNames())
		{
			if(set.contains(s))
				throw new Exception("Duplicate "  + s);
			
			set.add(s);
		}
	}
	
	public static void merge(File inFile1, File inFile2, File outFile) throws Exception
	{
		OtuWrapper wrapper1 = new OtuWrapper(inFile1);
		OtuWrapper wrapper2 = new OtuWrapper(inFile2);
		throwIfDuplicateSamples(wrapper1, wrapper2);
		
		HashSet<String> otuSet= new LinkedHashSet<String>();
		
		for(String s : wrapper1.getOtuNames())
			otuSet.add(s);
		
		for(String s : wrapper2.getOtuNames())
			otuSet.add(s);
		
		List<String> otuList = new ArrayList<String>();
		otuList.addAll(otuSet);
		Collections.sort(otuList);
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
		
		writer.write("samples");
		
		for(String s: otuList)
			writer.write("\t" + s);
		
		writer.write("\n");
		addWrapper(wrapper1, writer, otuList);
		addWrapper(wrapper2, writer, otuList);
		writer.flush(); writer.close();
	}
	
	private static void addWrapper(OtuWrapper wrapper, BufferedWriter writer, List<String> otuList)
			throws Exception
		{
			for(String s : wrapper.getSampleNames())
			{
				int sampleIndex = wrapper.getIndexForSampleName(s);
				writer.write(s);
				
				for( String s2 : otuList )
				{
					int index = wrapper.getIndexForOtuName(s2);
					if( index == -1 )
					{
						writer.write("\t0");
					}
					else
					{
						writer.write("\t" + wrapper.getDataPointsUnnormalized().get(sampleIndex).get(index));
					}
						
				}
				
				writer.write("\n");
			}
			
			writer.flush();

		}

	public int getIndexForOtuName(String s) throws Exception
	{
		for (int x = 0; x < otuNames.size(); x++)
			if (otuNames.get(x).equals(s))
				return x;
		
		return -1;
	}

	public int getIndexForSampleName(String s) throws Exception
	{
		for (int x = 0; x < sampleNames.size(); x++)
			if (sampleNames.get(x).equals(s))
				return x;
		
		return -1;
	}
	
	public double getNumberOfSequencesForOTU(String otu) throws Exception
	{
		double sum =0;
		int index = getIndexForOtuName(otu);
		
		for( int x=0; x < getSampleNames().size(); x++)
			sum += dataPointsUnnormalized.get(x).get(index);
		
		return sum;
	}
	
	public double getNumberOfSequencesForSampleWithMaxNumberOfSequences()
		throws Exception
	{
		double d = -1;
		
		for(String s : getSampleNames())
			d = Math.max(getNumberSequences(s), d);
		
		return d;
	}

	public double getNumberSequences(String sampleName) throws Exception
	{
		double num = 0;

		int sampleIndex = getIndexForSampleName(sampleName);

		for (int x = 0; x < otuNames.size(); x++)
			num += dataPointsUnnormalized.get(sampleIndex).get(x);

		return num;
	}

	public int getRichness(String sampleName) throws Exception
	{
		return getRichness(getIndexForSampleName(sampleName));
	}

	public int getRichness(int sampleIndex) throws Exception
	{
		int richness = 0;

		for (int x = 0; x < otuNames.size(); x++)
			if (dataPointsUnnormalized.get(sampleIndex).get(x) > 0.1)
				richness++;

		return richness;
	}

	/*
	 * Returns the number of species you observed on average for that index 
	 * over the numIterations
	 */
	public float[] getRarefactionCurve(int sampleIndex, int numIterations) throws Exception
	{
		List<Integer> otuList = new ArrayList<Integer>();
		
		List<Double> initialData = dataPointsUnnormalized.get(sampleIndex);
		
		int someVal =0;
		for( Double d : initialData)
		{
			someVal++;
			
			for( int x=0; x < d; x++)
				otuList.add(someVal);
		}
		
		float[] returnArray = new float[otuList.size()];
		
		for( int x=0; x < numIterations; x++)
		{
			Collections.shuffle(otuList);
			
			HashSet<Integer> observedOtus = new HashSet<Integer>();
			
			for( int y=0; y < otuList.size();y++)
			{
				observedOtus.add(otuList.get(y));
				returnArray[y] += observedOtus.size();
			}
		}
		
		for( int x=0; x< returnArray.length; x++)
			returnArray[x] = returnArray[x] / numIterations;
		
		return returnArray;
			
	}
	
	public float[] getRarefactionCurve(int sampleIndex, int numIterations, int limitNumSequences) 
		throws Exception
	{
		List<Integer> otuList = new ArrayList<Integer>();
		
		List<Double> initialData = dataPointsUnnormalized.get(sampleIndex);
		
		int someVal =0;
		for( Double d : initialData)
		{
			someVal++;
			
			for( int x=0; x < d; x++)
				otuList.add(someVal);
		}
		
		float[] returnArray = new float[limitNumSequences];
		
		for( int x=0; x < numIterations; x++)
		{
			Collections.shuffle(otuList);
			
			HashSet<Integer> observedOtus = new HashSet<Integer>();
			
			for( int y=0; y < limitNumSequences;y++)
			{
				observedOtus.add(otuList.get(y));
				returnArray[y] += observedOtus.size();
			}
		}
		
		for( int x=0; x< returnArray.length; x++)
			returnArray[x] = returnArray[x] / numIterations;
		
		return returnArray;
			
	}
	
	public MaxColumnHolder getMostAbundantTaxa(int sampleIndex)
	{
		List<Double> innerList = dataPointsNormalized.get(sampleIndex);

		MaxColumnHolder mch = new MaxColumnHolder();

		for (int x = 0; x < innerList.size(); x++)
		{
			double val = innerList.get(x);

			if (mch.proportion < val)
			{
				mch.proportion = val;
				mch.taxaIndex = x;
			}
		}

		return mch;
	}

	public List<String> getOtuNames()
	{
		return otuNames;
	}

	public List<String> getSampleNames()
	{
		return sampleNames;
	}
	
	public static double crank(List<Double> w)
	{
		double s;
		
		int j=1,ji,jt;
		double t,rank;

		int n=w.size();
		s=0.0f;
		while (j < n) 
		{
			if ( ! w.get(j).equals(w.get(j-1)))
			{
				w.set(j-1,j + 0.0);
				++j;
			} 
			else 
			{
				for (jt=j+1;jt<=n && w.get(jt-1).equals(w.get(j-1));jt++);
				rank=0.5f*(j+jt-1);
				for (ji=j;ji<=(jt-1);ji++)
					w.set(ji-1,rank);
				t=jt-j;
				s += (t*t*t-t);
				j=jt;
			}
		}
		if (j == n) w.set(n-1,n + 0.0);
		
		return s;
	}
	
	private static class RankHolder implements Comparable<RankHolder>
	{
		int originalIndex;
		double rank;
		double originalData;
		boolean tieMark = false;
		
		@Override
		public int compareTo(RankHolder o)
		{
			return Double.compare(this.originalData, o.originalData);
		}
	}
	
	public List<List<Double>> getRankNormalizedDataPoints()
	{
		List<List<Double>> rankList = new ArrayList<List<Double>>();
		
		for( int x=0; x < getSampleNames().size(); x++)
		{
			List<RankHolder> innerRanks = new ArrayList<RankHolder>();
			
			for( int y=0; y < getOtuNames().size(); y++ )
			{
				RankHolder rh = new RankHolder();
				rh.originalData = getDataPointsNormalized().get(x).get(y);
				rh.originalIndex = y;
				innerRanks.add(rh);
			}
			
			Collections.sort(innerRanks, new Comparator<RankHolder>()
					{@Override
					public int compare(RankHolder o1, RankHolder o2)
					{
						return Double.compare(o1.originalData, o2.originalData);
					}});
			
			List<Double> crankedList = new ArrayList<Double>();
			
			for( RankHolder rh : innerRanks)
				crankedList.add(rh.originalData);
			crank(crankedList);
			
			for( int y=0; y < innerRanks.size(); y++)
				innerRanks.get(y).rank = crankedList.get(y);
			
			double[] ranks = new double[innerRanks.size()];
			
			for( int y=0; y < innerRanks.size(); y++)
			{
				RankHolder rh = innerRanks.get(y);
				ranks[rh.originalIndex] = rh.rank;
			}
			
			List<Double> newList = new ArrayList<Double>();
			
			for( Double d : ranks)
				newList.add(d);
			
			rankList.add(newList);
		}
		
		return rankList;
	}

	public List<List<Double>> getDataPointsNormalized()
	{
		return dataPointsNormalized;
	}

	public List<List<Double>> getDataPointsNormalizedThenLogged()
	{
		return dataPointsNormalizedThenLogged;
	}

	public boolean hasOneOverThreshold(int otuIndex, double threshold)
	{
		for (int x = 0; x < getDataPointsUnnormalized().size(); x++)
		{
			double d = getDataPointsUnnormalized().get(x).get(otuIndex);

			if (d >= threshold)
				return true;
		}

		return false;
	}

	// samples as columns; no taxa names
	public void writeLoggedNormalizedDataForR( File file) throws Exception
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		
		for( int x=0; x < getSampleNames().size(); x++)
			writer.write( getSampleNames().get(x) + (x < getSampleNames().size()-1 ? "\t" : "\n") );
		
		for( int y=0; y < getOtuNames().size(); y++)
		{
			for( int x=0; x < getSampleNames().size(); x++)
				writer.write( getDataPointsNormalizedThenLogged().get(x).get(y)
						+ (x < getSampleNames().size()-1 ? "\t" : "\n") );	
		}
		
		writer.flush();  writer.close();
	}
	

	// samples as columns; no taxa names
	public void writePresentAbsenceDataForR( File file) throws Exception
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		
		for( int x=0; x < getSampleNames().size(); x++)
			writer.write( getSampleNames().get(x) + (x < getSampleNames().size()-1 ? "\t" : "\n") );
		
		for( int y=0; y < getOtuNames().size(); y++)
		{
			for( int x=0; x < getSampleNames().size(); x++)
				writer.write( 
						(getDataPointsUnnormalized().get(x).get(y) > 0.1 ? "1" : "0") + 
						 (x < getSampleNames().size()-1 ? "\t" : "\n") );	
		}
		
		writer.flush();  writer.close();
	}

	public static void transpose(String inFile, String outFile, boolean makeSamplesUnique) throws Exception
	{
		BufferedReader reader = new BufferedReader(new FileReader(inFile));
		
		String[] firstLineSplits = reader.readLine().split("\t");
		
		List<String> sampleNames = new ArrayList<String>();

		List<List<Double>> data = new ArrayList<List<Double>>();
		
		for( int x=1; x < firstLineSplits.length; x++)
		{
			sampleNames.add(firstLineSplits[x]);
			data.add(new ArrayList<Double>());
		}
		
		if( makeSamplesUnique)
			for( int x=0; x < sampleNames.size(); x++)
				sampleNames.set(x, sampleNames.get(x) + "_" + x);
			
		List<String> otuNames = new ArrayList<String>();
		for(String s= reader.readLine(); s != null; s= reader.readLine())
		{
			String[] otuSplits = s.split("\t");
			
			if( otuSplits.length != firstLineSplits.length)
				throw new Exception("NO " + otuSplits.length + " " + firstLineSplits.length + " " +   s);
			
			otuNames.add(otuSplits[0]);
			
			for( int x=1; x < otuSplits.length; x++)
				data.get(x-1).add(Double.parseDouble(otuSplits[x]));
		}
		
		reader.close();
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
		
		writer.write("otu");
		
		for(String s : otuNames)
			writer.write("\t" + s);
		
		writer.write("\n");
		
		for(int x=0; x < sampleNames.size(); x++)
		{
			writer.write(sampleNames.get(x) );
			
			List<Double> innerList = data.get(x);
			
			if( innerList.size() != otuNames.size())
				throw new Exception("No");
			
			for( Double d : innerList )
				writer.write("\t" + d);
			
			writer.write("\n");
		}
		
		writer.flush();  writer.close();
		
		
	}
	
	public double getGeometricMeanForSample(int sampleIndex)
	{
		double sum =0;
		
		for( int x=0; x< getOtuNames().size(); x++)
			sum+= Math.log10(getDataPointsUnnormalized().get(sampleIndex).get(x) + 1.0);
		
		return Math.pow(10, sum /getOtuNames().size());
	}
	
	/*
	public BigDecimal getGeometricMeanWithArbitraryPrecisions(int sampleIndex)
	{
		BigDecimal sum = new BigDecimal(1);
		
		for( int x=0; x< getOtuNames().size(); x++)
			sum = sum.multiply( new BigDecimal(getDataPointsUnnormalized().get(sampleIndex).get(x) + 1.0));
		
		System.out.println(sum.toString());
		
		return sum.
		
	}
	*/
	
	
	public static void assertTwoWrappersEqual( OtuWrapper wrapper1, OtuWrapper wrapper2 ) throws Exception
	{
		if( wrapper1.getSampleNames().size() != wrapper2.getSampleNames().size()  )
			throw new Exception("Unexpected sample sizes");
		
		if( wrapper1.getOtuNames().size() != wrapper2.getOtuNames().size())
			throw new Exception("Unexpected taxa sizes");
		
		List<String> sampleList1 = new ArrayList<String>();
		List<String> sampleList2 = new ArrayList<String>();
		
		for( String s : wrapper1.getSampleNames() )
			sampleList1.add(s);
		
		for( String s: wrapper2.getSampleNames())
			sampleList2.add(s);
		
		Collections.sort(sampleList1);
		Collections.sort(sampleList2);
		
		for( int x=0; x < wrapper1.getSampleNames().size(); x++)
			if( ! sampleList1.get(x).equals(sampleList2.get(x)) )
				throw new Exception("No match in list");
		
		List<String> taxaList1 = new ArrayList<String>();
		List<String> taxaList2 = new ArrayList<String>();
		
		for( String s: wrapper1.getOtuNames())
			taxaList1.add(s);
		
		for( String s : wrapper2.getOtuNames())
			taxaList2.add(s);
		
		Collections.sort(taxaList1);
		Collections.sort(taxaList2);
		
		for( int x=0; x < wrapper1.getOtuNames().size(); x++)
			if( ! taxaList1.get(x).equals(taxaList2.get(x)) )
				throw new Exception("No match in list");
		
		for( int x=0; x < sampleList1.size(); x++)
			for( int y=0; y < taxaList1.size(); y++)
			{
				String sampleName = sampleList1.get(x);
				String otuName = taxaList1.get(x);
				double dataPoint1= wrapper1.getDataPointsUnnormalized().get(wrapper1.getIndexForSampleName(sampleName)).
										get(wrapper1.getIndexForOtuName(otuName));
				
				double dataPoint2= wrapper2.getDataPointsUnnormalized().get(wrapper2.getIndexForSampleName(sampleName)).
						get(wrapper2.getIndexForOtuName(otuName));
				
				if( dataPoint1 != dataPoint2)
					throw new Exception("No " + dataPoint1 + " " + dataPoint2);
				
			}
	}
	
	public void writeLoggedDataWithTaxaAsColumns(File file) throws Exception
	{
		writeLoggedDataWithTaxaAsColumns(file, this.sampleNames, this.otuNames);
	}

	public double getTaxaSum(String sample, List<String> otus) throws Exception
	{
		double sum = 0;

		int sampleIndex = getIndexForSampleName(sample);

		for (String s : otus)
		{
			int otuIndex = getIndexForOtuName(s);
			sum += dataPointsNormalized.get(sampleIndex).get(otuIndex);
		}

		return sum;
	}

	public void writeLoggedDataWithTaxaAsColumns(File file,
			List<String> newFileNames, List<String> newOtuNames)
			throws Exception
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));

		writer.write("sample");

		for (String s : newOtuNames)
			writer.write("\t" + s);

		writer.write("\n");

		for (int x = 0; x < getSampleNames().size(); x++)
		{
			writer.write(newFileNames.get(x));

			for (int y = 0; y < getOtuNames().size(); y++)
			{
				writer.write("\t"
						+ dataPointsNormalizedThenLogged.get(x).get(y));
			}

			writer.write("\n");
		}

		writer.flush();
		writer.close();
	}
	
	public void writeUnnormalizedFirstTaxaWithTaxaAsColumns(File file, int numTaxa)
		throws Exception
	{

		BufferedWriter writer = new BufferedWriter(new FileWriter(file));

		writer.write("sample");

		for (int x=0; x< numTaxa; x++)
			writer.write("\t" + getOtuNames().get(x));

		writer.write("\tother\n");

		for (int x = 0; x < getSampleNames().size(); x++)
		{
			writer.write(getSampleNames().get(x));

			for (int y = 0; y < numTaxa; y++)
			{
				writer.write("\t" + dataPointsNormalized.get(x).get(y));
				
			}


			double sum =0;
			
			for( int y=numTaxa; y < getOtuNames().size(); y++)
				sum += dataPointsNormalized.get(x).get(y);
			
			writer.write("\t" + sum + "\n");
		}

		writer.flush();
		writer.close();
	}
	
	public void writeRawCountsWithRandomNoise(String file, long seed) throws Exception
	{
		Random random = new Random(seed);
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));

		writer.write("sample");

		for (String s : getOtuNames())
			writer.write("\t" + s);

		writer.write("\n");

		for (int x = 0; x < getSampleNames().size(); x++)
		{
			writer.write(getOtuNames().get(x));

			for (int y = 0; y < getOtuNames().size(); y++)
			{
				writer.write("\t" + (dataPointsUnnormalized.get(x).get(y) + random.nextDouble()/10000 ) );
			}

			writer.write("\n");
		}

		writer.flush();
		writer.close();

	}
	
	public void writeNormalizedUnloggedDataWithSamplesAsColumns(File file) throws Exception
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		
		writer.write("taxa");
		
		for( String s: getSampleNames())
			writer.write("\t" + s);
		
		writer.write("\n");
		
		for( int x=0; x < getOtuNames().size(); x++)
		{
			writer.write( getOtuNames().get(x) );
			
			for( int y=0; y < getSampleNames().size(); y++)
			{
				writer.write("\t" + dataPointsNormalized.get(y).get(x));
			}
			
			writer.write("\n");
		}
		
		writer.flush();  writer.close();
		
	}

	public void writeNormalizedUnloggedDataWithTaxaAsColumns(File file,
			List<String> newFileNames, List<String> newOtuNames)
			throws Exception
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));

		writer.write("sample");

		for (String s : newOtuNames)
			writer.write("\t" + s);

		writer.write("\n");

		for (int x = 0; x < getSampleNames().size(); x++)
		{
			writer.write(newFileNames.get(x));

			for (int y = 0; y < getOtuNames().size(); y++)
			{
				writer.write("\t" + dataPointsNormalized.get(x).get(y));
			}

			writer.write("\n");
		}

		writer.flush();
		writer.close();
	}

	
	public void writeRawDataWithTaxaAsColumns(String filePath) throws Exception
	{
		writeRawDataWithTaxaAsColumns(new File(filePath));
	}
	
	public void writeRawDataWithTaxaAsColumns(File file) throws Exception
	{
		writeRawDataWithTaxaAsColumns(file, this.getSampleNames(), this.getOtuNames());
	}
	
	public void writeRawDataWithTaxaAsColumns(File file,
			List<String> newFileNames, List<String> newOtuNames)
			throws Exception
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));

		writer.write("sample");

		for (String s : newOtuNames)
			writer.write("\t" + s);

		writer.write("\n");

		for (int x = 0; x < getSampleNames().size(); x++)
		{
			writer.write(newFileNames.get(x));

			for (int y = 0; y < getOtuNames().size(); y++)
			{
				writer.write("\t" + dataPointsUnnormalized.get(x).get(y));
			}

			writer.write("\n");
		}

		writer.flush();
		writer.close();
	}

	public void writeunLoggedDataWithTaxaAsColumns(File file,
			List<String> newFileNames, List<String> newOtuNames)
			throws Exception
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));

		writer.write("sample");

		for (String s : newOtuNames)
			writer.write("\t" + "S_" + s);

		writer.write("\n");

		for (int x = 0; x < getSampleNames().size(); x++)
		{
			writer.write(newFileNames.get(x));

			for (int y = 0; y < getOtuNames().size(); y++)
			{
				writer.write("\t" + dataPointsNormalized.get(x).get(y));
			}

			writer.write("\n");
		}

		writer.flush();
		writer.close();
	}

	public void writeloggedDataWithSamplesAsColumns(File file) throws Exception
	{
		writeloggedDataWithSamplesAsColumns(file, getSampleNames(),
				getOtuNames());
	}

	public void writeloggedDataWithSamplesAsColumns(File file,
			List<String> newFileNames, List<String> newOtuNames)
			throws Exception
	{
		if (newFileNames == null)
			newFileNames = getSampleNames();

		if (newOtuNames == null)
			newOtuNames = getOtuNames();

		BufferedWriter writer = new BufferedWriter(new FileWriter(file));

		writer.write("taxa");

		for (String s : newFileNames)
			writer.write("\t" + s);

		writer.write("\n");

		for (int x = 0; x < getOtuNames().size(); x++)
		{
			writer.write(newOtuNames.get(x));

			for (int y = 0; y < getSampleNames().size(); y++)
				writer.write("\t"
						+ dataPointsNormalizedThenLogged.get(y).get(x));

			writer.write("\n");
		}

		writer.flush();
		writer.close();
	}
	
	public void writeunLoggedDataWithSamplesAsColumns(File file)
		throws Exception
	{
		writeunLoggedDataWithSamplesAsColumns(file,
				this.sampleNames, this.otuNames);
	}
	
	public void writeunLoggedDataWithSamplesAsColumns(File file,
			List<String> newFileNames, List<String> newOtuNames)
			throws Exception
	{
		if (newFileNames == null)
			newFileNames = getSampleNames();

		if (newOtuNames == null)
			newOtuNames = getOtuNames();

		BufferedWriter writer = new BufferedWriter(new FileWriter(file));

		writer.write("taxa");

		for (String s : newFileNames)
			writer.write("\t" + s);

		writer.write("\n");

		for (int x = 0; x < getOtuNames().size(); x++)
		{
			writer.write(newOtuNames.get(x));

			for (int y = 0; y < getSampleNames().size(); y++)
				writer.write("\t" + dataPointsNormalized.get(y).get(x));

			writer.write("\n");
		}

		writer.flush();
		writer.close();
	}
	
	public void writeUnNormalizedDataWithSamplesAsColumns(File file,
			List<String> newFileNames, List<String> newOtuNames)
			throws Exception
	{
		if (newFileNames == null)
			newFileNames = getSampleNames();

		if (newOtuNames == null)
			newOtuNames = getOtuNames();

		BufferedWriter writer = new BufferedWriter(new FileWriter(file));

		writer.write("taxa");

		for (String s : newFileNames)
			writer.write("\t" + s);

		writer.write("\n");

		for (int x = 0; x < getOtuNames().size(); x++)
		{
			writer.write(newOtuNames.get(x));

			for (int y = 0; y < getSampleNames().size(); y++)
				writer.write("\t" + dataPointsUnnormalized.get(y).get(x));

			writer.write("\n");
		}

		writer.flush();
		writer.close();
	}

	public List<Double> getUnnormalizedDataForTaxa(String taxa)
			throws Exception
	{
		List<Double> list = new ArrayList<Double>();

		int index = -1;

		for (int x = 0; x < otuNames.size(); x++)
			if (otuNames.get(x).equals(taxa))
				index = x;

		if (index == -1)
			throw new Exception("Can't find " + taxa);

		for (int x = 0; x < dataPointsUnnormalized.size(); x++)
			list.add(dataPointsUnnormalized.get(x).get(index));

		return list;
	}

	public List<List<Double>> getDataPointsUnnormalized()
	{
		return dataPointsUnnormalized;
	}

	public double getShannonEntropy(String sampleName) throws Exception
	{
		return getShannonEntropy(getIndexForSampleName(sampleName));
	}
	
	public double getShannonEntropy(int sampleIndex) throws Exception
	{
		double sum = 0;

		List<Double> innerList = getDataPointsUnnormalized().get(sampleIndex);

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
	
	public double getShannonEntropyFromLogNormalized(int sampleIndex) throws Exception
	{
		double sum = 0;

		List<Double> innerList = getDataPointsNormalizedThenLogged().get(sampleIndex);

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
	
	public double getSimpsonsIndex(String sample) throws Exception
	{
		return getSimpsonsIndex(getIndexForSampleName(sample));
	}

	public double getSimpsonsIndex(int sampleIndex)
	{
		double sum = 0;

		List<Double> innerList = getDataPointsUnnormalized().get(sampleIndex);

		for (Double d : innerList)
			if (d > 0.1)
				sum += d;

		double returnVal = 0;

		for (Double d : innerList)
			if (d > 0.1)
			{
				double n = d / sum;

				returnVal += n * n;
			}

		return returnVal;

	}

	public double getEvenness(String sampleName)  throws Exception
	{
		return getEvenness(getIndexForSampleName(sampleName));
	}
	
	public double getEvenness(int sampleIndex) throws Exception
	{
		double sum = 0;

		List<Double> innerList = getDataPointsUnnormalized().get(sampleIndex);

		for (Double d : innerList)
			if (d > 0.1)
				sum++;

		return getShannonEntropy(sampleIndex) / Math.log(sum);
	}

	public double[][] getUnnorlalizedAsArray()
	{
		double[][] d = new double[dataPointsUnnormalized.size()][dataPointsUnnormalized
				.get(0).size()];

		for (int x = 0; x < dataPointsUnnormalized.size(); x++)
			for (int y = 0; y < dataPointsUnnormalized.get(0).size(); y++)
				d[x][y] = dataPointsUnnormalized.get(x).get(y);

		return d;
	}

	public double[][] getPresenceAbsenceArray()
	{
		double[][] d = new double[dataPointsUnnormalized.size()][dataPointsUnnormalized.get(0).size()];
		                                          
		for (int x = 0; x < dataPointsUnnormalized.size(); x++)
			for (int y = 0; y < dataPointsUnnormalized.get(0).size(); y++)
				d[x][y] = (dataPointsUnnormalized.get(x).get(y) > 0.1 ? 1 : 0);

		return d;

	}
	
	public double[][] getNormalizedThenLoggedAsArray()
	{
		double[][] d = new double[dataPointsNormalizedThenLogged.size()][dataPointsNormalizedThenLogged
				.get(0).size()];
		// new
		// double[dataPointsNormalizedThenLogged.get(0).size()][dataPointsNormalizedThenLogged.size()];

		for (int x = 0; x < dataPointsNormalizedThenLogged.size(); x++)
			for (int y = 0; y < dataPointsNormalizedThenLogged.get(0).size(); y++)
				d[x][y] = dataPointsNormalizedThenLogged.get(x).get(y);

		return d;
	}
	
	public double[][] getLoggedAsArray()
	{
		double[][] d = new double[dataPointsUnnormalized.size()][dataPointsUnnormalized
				.get(0).size()];
		// new
		// double[dataPointsNormalizedThenLogged.get(0).size()][dataPointsNormalizedThenLogged.size()];

		for (int x = 0; x < dataPointsUnnormalized.size(); x++)
			for (int y = 0; y < dataPointsUnnormalized.get(0).size(); y++)
				d[x][y] =  Math.log10( dataPointsUnnormalized.get(x).get(y));

		return d;
	}
	
	public double[][] getAsArray()
	{
		double[][] d = new double[dataPointsUnnormalized.size()][dataPointsUnnormalized
				.get(0).size()];
		// new
		// double[dataPointsNormalizedThenLogged.get(0).size()][dataPointsNormalizedThenLogged.size()];

		for (int x = 0; x < dataPointsUnnormalized.size(); x++)
			for (int y = 0; y < dataPointsUnnormalized.get(0).size(); y++)
				d[x][y] =  dataPointsUnnormalized.get(x).get(y);

		return d;
	}
	
	public double[][] getAsGeoNormalizedLoggedArray()
	{
		double[][] d = new double[dataPointsUnnormalized.size()][dataPointsUnnormalized
				.get(0).size()];

		for (int x = 0; x < dataPointsUnnormalized.size(); x++)
		{
			double logGeoMean = Math.log10( this.getGeometricMeanForSample(x));
			
			for (int y = 0; y < dataPointsUnnormalized.get(0).size(); y++)
				d[x][y] = Math.log10( dataPointsUnnormalized.get(x).get(y) +1 ) - logGeoMean ;

		}
			
		return d;
	}

	public double[][] getNormalizedAsArray()
	{
		double[][] d = new double[dataPointsNormalized.size()][dataPointsNormalized
				.get(0).size()];

		for (int x = 0; x < dataPointsNormalized.size(); x++)
			for (int y = 0; y < dataPointsNormalized.get(0).size(); y++)
				d[x][y] = dataPointsNormalized.get(x).get(y);

		return d;
	}
	
	public double[][] getCubeRootNormalizedAsArray()
	{
		double[][] d = new double[dataPointsNormalized.size()][dataPointsNormalized
				.get(0).size()];

		for (int x = 0; x < dataPointsNormalized.size(); x++)
			for (int y = 0; y < dataPointsNormalized.get(0).size(); y++)
				d[x][y] = Math.pow( dataPointsNormalized.get(x).get(y), (1/3.0));

		return d;
	}
	
	public void writeUnnormalizedDataToFile(File file) throws Exception
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));

		writer.write("sample");

		for (String s : otuNames)
			writer.write("\t" + s);

		writer.write("\n");

		for (int x = 0; x < sampleNames.size(); x++)
		{
			writer.write(sampleNames.get(x));

			for (int y = 0; y < otuNames.size(); y++)
				writer.write("\t" + dataPointsUnnormalized.get(x).get(y));

			writer.write("\n");
		}

		writer.flush();
		writer.close();
	}

	public void writeNormalizedDataToFile(File file) throws Exception
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));

		writer.write("sample");

		for (String s : otuNames)
			writer.write("\t" + s);

		writer.write("\n");

		for (int x = 0; x < sampleNames.size(); x++)
		{
			writer.write(sampleNames.get(x));

			for (int y = 0; y < otuNames.size(); y++)
				writer.write("\t" + dataPointsNormalized.get(x).get(y));

			writer.write("\n");
		}

		writer.flush();
		writer.close();
	}

	public void writeNormalizedLoggedDataToFile(String filePath) throws Exception
	{
		writeNormalizedLoggedDataToFile(new File(filePath));
	}
	
	public void writeNormalizedLoggedDataToFile(File file) throws Exception
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));

		writer.write("sample");

		for (String s : otuNames)
			writer.write("\t" + s);

		writer.write("\n");

		for (int x = 0; x < sampleNames.size(); x++)
		{
			writer.write(sampleNames.get(x));

			for (int y = 0; y < otuNames.size(); y++)
				writer.write("\t" + dataPointsNormalizedThenLogged.get(x).get(y));

			writer.write("\n");
		}

		writer.flush();
		writer.close();
	}
	
	public void writeNormalizedLoggedDataToFile(File file, List<String> newSampleNames) throws Exception
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));

		writer.write("sample");

		for (String s : otuNames)
			writer.write("\t" + s);

		writer.write("\n");

		for (int x = 0; x < sampleNames.size(); x++)
		{
			writer.write(newSampleNames.get(x));

			for (int y = 0; y < otuNames.size(); y++)
				writer.write("\t" + dataPointsNormalizedThenLogged.get(x).get(y));

			writer.write("\n");
		}

		writer.flush();
		writer.close();
	}
	
	
	public HashMap<String, Double> getNormalizedDataAsMap() throws Exception
	{
		HashMap<String, Double> map = new HashMap<String, Double>();

		for (int x = 0; x < sampleNames.size(); x++)
		{
			String sampleName = sampleNames.get(x);
			sampleName = new StringTokenizer(sampleName, "_").nextToken();

			for (int y = 0; y < otuNames.size(); y++)
			{
				String key = otuNames.get(y) + "@" + sampleName;

				if (map.containsKey(key))
					throw new Exception("Duplicate key");

				map.put(key, dataPointsNormalized.get(x).get(y));
			}
		}

		return map;
	}

	public HashMap<String, Double> getLoggedNormalizedDataAsMap()
			throws Exception
	{
		HashMap<String, Double> map = new HashMap<String, Double>();

		for (int x = 0; x < sampleNames.size(); x++)
		{
			String sampleName = sampleNames.get(x);
			sampleName = new StringTokenizer(sampleName, "_").nextToken();

			for (int y = 0; y < otuNames.size(); y++)
			{
				String key = otuNames.get(y) + "@" + sampleName;

				if (map.containsKey(key))
					throw new Exception("Duplicate key");

				map.put(key, dataPointsNormalizedThenLogged.get(x).get(y));
			}
		}

		return map;
	}

	public int getTotalCounts() throws Exception
	{
		int sum = 0;

		for (int x = 0; x < getOtuNames().size(); x++)
			sum += getCountsForTaxa(x);

		return sum;
	}

	public int getCountsForTaxa(String s) throws Exception
	{
		return getCountsForTaxa(getIndexForOtuName(s));
	}
	
	public int getCountsForTaxa(int index) throws Exception
	{
		double counts = 0;

		for (int x = 0; x < getDataPointsUnnormalized().size(); x++)
			counts += getDataPointsUnnormalized().get(x).get(index);

		return (int) (counts + 0.1);
	}

	public int getCountsForSample(int index) throws Exception
	{
		double counts = 0;

		for (int x = 0; x < getDataPointsUnnormalized().get(index).size(); x++)
			counts += getDataPointsUnnormalized().get(index).get(x);

		return (int) (counts + 0.1);

	}
	
	public int getCountsForSample(String sample) throws Exception
	{
		return getCountsForSample(getIndexForSampleName(sample));
	}

	public Double getNormalizedLoggedDataPoint(String sample, String taxa)
			throws Exception
	{
		int x = -1;

		for (int i = 0; x == -1 && i < getSampleNames().size(); i++)
			if (sample.equals(getSampleNames().get(i)))
				x = i;

		int y = -1;
		for (int i = 0; y == -1 && i < getOtuNames().size(); i++)
			if (taxa.equals(getOtuNames().get(i)))
				y = i;

		if (x == -1 || y == -1)
			return null;

		return dataPointsNormalizedThenLogged.get(x).get(y);
	}

	public Double getUnnormalizedDataPoint(String sample, String taxa)
			throws Exception
	{
		int x = -1;

		for (int i = 0; x == -1 && i < getSampleNames().size(); i++)
			if (sample.equals(getSampleNames().get(i)))
				x = i;

		int y = -1;
		for (int i = 0; y == -1 && i < getOtuNames().size(); i++)
			if (taxa.equals(getOtuNames().get(i)))
				y = i;

		if (x == -1 || y == -1)
			return null;

		return dataPointsUnnormalized.get(x).get(y);
	}

	public OtuWrapper(String filePath) throws Exception
	{
		this(new File(filePath));
	}

	public OtuWrapper(File f) throws Exception
	{
		this(f, null, null);
	}

	private static boolean excludeTaxa(String taxaName,
			HashSet<String> excludedTaxa)
	{
		if (excludedTaxa == null)
			return true;

		for (String s : excludedTaxa)
			if (s.equalsIgnoreCase(taxaName))
				return true;

		return false;
	}

	public OtuWrapper(String filepath, HashSet<String> excludedSamples,
			HashSet<String> excludedTaxa) throws Exception
	{
		this(filepath, excludedSamples, excludedTaxa, 0.01);
	}

	public OtuWrapper(String filepath, HashSet<String> excludedSamples,
			HashSet<String> excludedTaxa, double threshold) throws Exception
	{
		this(new File(filepath), excludedSamples, excludedTaxa, threshold);
	}

	public OtuWrapper(File f, HashSet<String> excludedSamples,
			HashSet<String> excludedTaxa) throws Exception
	{
		this(f, excludedSamples, excludedTaxa, -1000);
	}
	
	public double getBrayCurtis(int i, int j, boolean log)
	{
		double si = 0;
		double sj =0;
		double cij =0;
		
		List<List<Double>> list = getDataPointsNormalized();
		
		if(log)
			list = getDataPointsNormalizedThenLogged();
		
		for( int x=0; x < getOtuNames().size(); x++)
		{
			si+= list.get(i).get(x);
			sj+= list.get(j).get(x);
			cij += Math.abs( list.get(i).get(x) - list.get(j).get(x));
		}

		return  cij / (si + sj);
	}
	
	public void writeBrayCurtisForMothur(String filepath, boolean log) throws Exception
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filepath)));
		
		writer.write(getSampleNames().size() + "\n");
		
		
		for(int x=0; x < getSampleNames().size(); x++)
		{
			writer.write(getSampleNames().get(x));
			
			for( int y=0; y < getSampleNames().size(); y++)
				writer.write("\t" + getBrayCurtis(x, y,log));
			
			writer.write("\n");
		}
		
		writer.flush();  writer.close();
	}

	public void writeRankedSpreadsheet(String newFilePath) throws Exception
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(newFilePath));
		
		writer.write("taxa");
		
		for( String s : this.getOtuNames())
			writer.write("\t" + s);
		
		writer.write("\n");
		
		for( int x=0; x < this.getSampleNames().size(); x++)
		{
			writer.write(this.getSampleNames().get(x));
			
			Integer[] ranked= this.getSimpleRankForSample(x);
			
			if( ranked.length != this.getDataPointsNormalized().get(x).size())
				throw new Exception("Logic error");
			
			for( int y=0; y < ranked.length; y++)
				writer.write("\t" + ranked[y]);
			
			writer.write("\n");
		}
		
		writer.flush();  writer.close();
	}
	
	public Integer[] getSimpleRankForSample(int sampleIndex) throws Exception
	{
		List<RankHolder> rankedList = new ArrayList<RankHolder>();
		
		for( int x=0; x < this.getDataPointsUnnormalized().get(sampleIndex).size(); x++)
		{
			RankHolder rh = new RankHolder();
			rh.originalIndex = x;
			rh.originalData = this.getDataPointsUnnormalized().get(sampleIndex).get(x);
			rankedList.add(rh);
		}
		
		Collections.shuffle(rankedList);
		Collections.sort(rankedList);
		Collections.reverse(rankedList);
		
		for(int x=0; x < rankedList.size();x++)
		{
			RankHolder rh = rankedList.get(x);
			rh.rank = (x+1);
		}
		
		Integer[] returnVals = new Integer[ this.getDataPointsUnnormalized().get(sampleIndex).size()];
		
		for( RankHolder rh : rankedList)
			returnVals[rh.originalIndex] = (int) (rh.rank + 0.001);
		
		return returnVals;
	}
	
	Integer[] getRankForSample(int sampleIndex) throws Exception
	{
		List<RankHolder> rankedList = new ArrayList<RankHolder>();
		
		for( int x=0; x < this.getDataPointsUnnormalized().get(sampleIndex).size(); x++)
		{
			RankHolder rh = new RankHolder();
			rh.originalIndex = x;
			rh.originalData = this.getDataPointsUnnormalized().get(sampleIndex).get(x);
			rankedList.add(rh);
		}
		
		Collections.sort(rankedList);
		
		double lastCount = 0.0;
		
		for(int x=0; x < rankedList.size();x++)
		{
			RankHolder rh = rankedList.get(x);
			rh.tieMark = true;
			
			if( rh.originalData > lastCount )
			{
				int backIndex = x-1;
				
				while( backIndex >= 0 && rankedList.get(backIndex).tieMark == true)
				{
					RankHolder priorH = rankedList.get(backIndex);
					priorH.tieMark = false;
					priorH.rank = x;
					backIndex--;
				}
			}
			
			lastCount = rh.originalData;
		}
		
		int backIndex = rankedList.size()-1;
		
		while( backIndex > 0 && rankedList.get(backIndex).tieMark == true)
		{
			RankHolder priorH = rankedList.get(backIndex);
			priorH.tieMark = false;
			priorH.rank = rankedList.size();
			backIndex--;
		}
		
		HashMap<Integer, Integer> rankMap = new HashMap<Integer, Integer>();
		
		int val=0;
		for( int x=rankedList.size()-1; x>=0; x--)
		{
			int intVal = (int) (rankedList.get(x).rank + 0.001);
			if( !rankMap.containsKey(intVal))
			{
				val++;
				rankMap.put(intVal, val);
			}
		}
		
		for( RankHolder rh : rankedList)
			rh.rank = rankMap.get((int) (rh.rank + 0.001));
		
		Integer[] returnVals = new Integer[ this.getDataPointsUnnormalized().get(sampleIndex).size()];
		
		for( RankHolder rh : rankedList)
			returnVals[rh.originalIndex] = (int) (rh.rank + 0.001);
		
		return returnVals;
	}

	public OtuWrapper(File f, HashSet<String> excludedSamples,
			HashSet<String> excludedTaxa, double threshold) throws Exception
	{
		this.filePath = f.getAbsolutePath();
		BufferedReader reader = new BufferedReader(new FileReader(f));

		String nextLine = reader.readLine();

		TabReader tr = new TabReader(nextLine);

		tr.nextToken();

		HashSet<Integer> skipColumns = new HashSet<Integer>();

		int x = 0;
		while (tr.hasMore())
		{
			String taxaName = tr.nextToken();

			if (taxaName.startsWith("\"") && taxaName.endsWith("\""))
				taxaName = taxaName.substring(1, taxaName.length() - 1);

			if (excludedTaxa == null || !excludeTaxa(taxaName, excludedTaxa))
			{
				otuNames.add(taxaName);
			} else
			{
				skipColumns.add(x);
				System.out.println("Wrapper excluding taxa " + taxaName);
			}

			x++;

		}

		nextLine = reader.readLine();

		int totalCounts = 0;
		while (nextLine != null)
		{
			tr = new TabReader(nextLine);

			String sampleName = tr.nextToken();

			boolean includeSample = true;

			if (excludedSamples != null)
				for (String s : excludedSamples)
					if (sampleName.equals(s))
						includeSample = false;

			if (includeSample)
			{
				sampleNames.add(sampleName);
				List<Double> innerList = new ArrayList<Double>();
				dataPointsUnnormalized.add(innerList);
				dataPointsNormalized.add(new ArrayList<Double>());
				dataPointsNormalizedThenLogged.add(new ArrayList<Double>());

				x = 0;
				while (tr.hasMore())
				{
					String nextToken = tr.nextToken();

					double d = 0;

					if (nextToken.length() > 0)
						d = Double.parseDouble(nextToken);

					if (!skipColumns.contains(x))
					{
						innerList.add(d);
						totalCounts += d;
					}

					x++;
				}
			} else
			{
				System.out.println("Wrapper excluding " + sampleName);
			}

			if (x != skipColumns.size() + otuNames.size())
				throw new Exception("Logic error " + x + " " +  skipColumns.size() + " " + otuNames.size());

			nextLine = reader.readLine();
		}

		// System.out.println( sampleNames.size() + " " + otuNames.size());
		assertNum(totalCounts, dataPointsUnnormalized);
		removeThreshold(otuNames, dataPointsUnnormalized, threshold);

		if (threshold < 0.1)
		{
			assertNoZeros(dataPointsUnnormalized);
			assertNum(totalCounts, dataPointsUnnormalized);
		}

		avgNumber = ((double) totalCounts) / dataPointsNormalized.size();
		//avgNumber =1;	
		
		for (x = 0; x < dataPointsUnnormalized.size(); x++)
		{
			List<Double> unnormalizedInnerList = dataPointsUnnormalized.get(x);
			double sum = 0;

			for (Double d : unnormalizedInnerList)
				sum += d;

			List<Double> normalizedInnerList = dataPointsNormalized.get(x);
			List<Double> loggedInnerList = dataPointsNormalizedThenLogged
					.get(x);

			for (int y = 0; y < unnormalizedInnerList.size(); y++)
			{
				double val = avgNumber * unnormalizedInnerList.get(y) / sum;
				normalizedInnerList.add(val);
				loggedInnerList.add(Math.log10(val + 1));
			}
		}

		this.dataPointsNormalized = Collections
				.unmodifiableList(this.dataPointsNormalized);
		this.dataPointsNormalizedThenLogged = Collections
				.unmodifiableList(this.dataPointsNormalizedThenLogged);
		this.dataPointsUnnormalized = Collections
				.unmodifiableList(this.dataPointsUnnormalized);
		this.otuNames = Collections.unmodifiableList(otuNames);
		this.sampleNames = Collections.unmodifiableList(sampleNames);
	}
	
	public float getFractionZeroForTaxa(int taxaIndex ) 
	{
		float f =0;
		
		for( int x=0; x < getSampleNames().size(); x++)
			if( dataPointsUnnormalized.get(x).get(taxaIndex) < 0.001  )
				f = f +1;
		
		//System.out.println( getOtuNames().get(taxaIndex) + " " + f + " " + getSampleNames().size() );
		return f / getSampleNames().size();
	}

	private static void assertNoZeros(List<List<Double>> dataPointsUnnormalized)
			throws Exception
	{
		for (int x = 0; x < dataPointsUnnormalized.size(); x++)
		{
			for (int y = 0; y < dataPointsUnnormalized.get(x).size(); y++)
			{
				double sum = 0;

				for (Double d : dataPointsUnnormalized.get(x))
					sum += d;

				if (sum == 0)
					throw new Exception("Logic error");

			}
		}
	}

	private static void assertNum(int totalCounts,
			List<List<Double>> dataPointsUnnormalized) throws Exception
	{
		int sum = 0;

		for (int x = 0; x < dataPointsUnnormalized.size(); x++)
			for (int y = 0; y < dataPointsUnnormalized.get(x).size(); y++)
				sum += dataPointsUnnormalized.get(x).get(y);

		if (totalCounts != sum)
			throw new Exception("Logic error " + totalCounts + " " + sum);

		if (dataPointsUnnormalized.size() > 0)
		{
			int length = dataPointsUnnormalized.get(0).size();

			for (int x = 0; x < dataPointsUnnormalized.size(); x++)
				if (length != dataPointsUnnormalized.get(x).size())
					throw new Exception("Jagged array");
		}
	}

	private static void removeThreshold(List<String> otuNames,
			List<List<Double>> dataPointsUnNormalized, double threshold)
	{
		List<Boolean> removeList = new ArrayList<Boolean>();

		for (int x = 0; x < otuNames.size(); x++)
		{
			int sum = 0;

			for (int y = 0; y < dataPointsUnNormalized.size(); y++)
			{
				sum += dataPointsUnNormalized.get(y).get(x);
			}

			if (sum <= threshold)
				removeList.add(true);
			else
				removeList.add(false);
		}

		for (int y = 0; y < dataPointsUnNormalized.size(); y++)
		{
			int x = 0;

			for (Iterator<Double> i = dataPointsUnNormalized.get(y).iterator(); i
					.hasNext();)
			{
				i.next();
				if (removeList.get(x))
					i.remove();

				x++;
			}
		}

		int x = 0;

		for (Iterator<String> i = otuNames.iterator(); i.hasNext();)
		{
			i.next();
			if (removeList.get(x))
				i.remove();

			x++;
		}
	}
}
