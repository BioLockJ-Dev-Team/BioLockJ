package biolockj.module.implicit.parser.r16s;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import biolockj.Constants;
import biolockj.Log;
import biolockj.Properties;
import biolockj.exception.BioLockJException;
import biolockj.exception.ConfigFormatException;
import biolockj.module.BioModule;
import biolockj.module.JavaModuleImpl;
import biolockj.module.classifier.r16s.RdpClassifier;
import biolockj.util.BashScriptBuilder;
import biolockj.util.BioLockJUtil;
import biolockj.util.ModuleUtil;
import biolockj.util.SeqUtil;
import biolockj.util.TaxaUtil;
import biolockj.module.report.taxa.TaxaLevelTable;

public class RdpHierParser extends JavaModuleImpl {

	public RdpHierParser()  {
		addNewProperty( Constants.RDP_THRESHOLD_SCORE, Properties.NUMERTIC_TYPE, "RdpClassifier will use this property and ignore OTU assignments below this threshold score (0-100)" );
	}

	@Override
	public void checkDependencies() throws Exception {
		if ( TaxaUtil.getTaxaLevels().isEmpty() ) throw new ConfigFormatException( Constants.REPORT_TAXONOMY_LEVELS, "Pipeline must report at least 1 valid taxanomic level." );
	}
	
	/**
	 * For stand-alone testing outside of a BioLockJ pipeline.<br>
	 * 
	 * arg 0 - comma-separated list of taxonomic levels<br>
	 * arg 1 - comma-separated list of input files (one or more) <br>
	 * arg 2 - path/to/dir where the outputs will be saved<br>
	 * 
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		if (args.length != 3) {
			System.out.println( "Bad args. Expected exactly 3 args:");
			System.out.println( "1) comma-separated list of taxonomic levels");
			System.out.println( "2) comma-separated list of input files (one or more) ");
			System.out.println( "3) path/to/dir where the outputs will be saved");
			System.exit( 1 );
		}
		File outDir = new File(args[2]);
		if (outDir.isDirectory()) {
			System.out.println( "Output will be saved to: " + outDir.getAbsolutePath());
		}else {
			System.out.println("Third arg must be a directory that exists.");
			System.exit( 1 );
		}
		
		System.err.println("Building taxa tables with levels: " + args[0]);
		List<String> levels = new ArrayList<>();
		StringTokenizer levs = new StringTokenizer( args[0], "," );
		while(levs.hasMoreTokens()) levels.add( levs.nextToken() );
		
		System.err.println("Building taxa tables from input files: " + args[1]);
		List<File> files = new ArrayList<>();
		StringTokenizer fls = new StringTokenizer( args[1], "," );
		while( fls.hasMoreTokens() ) files.add( new File(fls.nextToken()) );
		
		System.err.println("Building taxa tables ... " );
		Map<String, TaxaLevelTable> tt = buildTaxaTable(levels, files);
		
		System.err.println("Saving taxa tables ... " );
		for (String level : levels) {
			File outFile = new File(outDir, level + Constants.TSV_EXT);
			TaxaUtil.writeDataToFile( outFile, tt.get( level ) );
		}
		
		System.err.println("All done!" );
	}
	
	@Override
	public void runModule() throws Exception{
		Map<String, TaxaLevelTable> tt = buildTaxaTable(TaxaUtil.getTaxaLevels(), getInputFiles());
		for (String level : TaxaUtil.getTaxaLevels()) {
			File outFile = TaxaUtil.getTaxonomyTableFile( getOutputDir(), level, null );
			TaxaUtil.writeDataToFile( outFile, tt.get( level ) );
		}
	}
	
	private static Map<String, TaxaLevelTable> buildTaxaTable(final List<String> levels, final List<File> inFiles) throws BioLockJException {
		//TaxaTable tt = new TaxaTable(levels);
		Map<String, TaxaLevelTable> tt = new HashMap<>();
		for (String lev : levels) tt.put(lev, new TaxaLevelTable( lev ));

		for (File file : inFiles) {
			BufferedReader reader;
			int lineNo = 1;
			try {
				Log.info(RdpHierParser.class, "Parsing file: " + file.getName());
				reader = new BufferedReader( new FileReader( file ) );
				List<String> sampleIds = getSamples(reader.readLine());
				int numCols = 4 + sampleIds.size();

				for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
					lineNo++;
					String[] ss = line.split( Constants.TAB_DELIM );
					if (ss.length != numCols) {
						throw new BioLockJException( "Error while parsing file: " + file.getAbsolutePath() + System.lineSeparator() 
						+ "Line " + lineNo + ". Expected [ " + numCols + " ] elements, found [ " + ss.length + " ]. See line:" + System.lineSeparator() + line);
					}
					String taxid = ss[0];
					String lineage = ss[1];
					String name = stripQuotes( ss[2] );
					String rank = stripQuotes( ss[3] );
					int i=4;
					if ( levels.contains( rank )) {
						TaxaLevelTable tlt = tt.get( rank );
						for (String sampleId : sampleIds) {
							Double value = Double.parseDouble(ss[i]); i++;
							Double endValue = tlt.addValue( sampleId, name, value );
							if ( ! value.equals( endValue ) ) {
								Log.warn(RdpHierParser.class, "Multiple values were combined to get the reported value for sample=" 
												+ sampleId + ", level=" + rank + ", taxon name=" + name);
								Log.warn(RdpHierParser.class, "Most recently added value was [" + value + " from taxid=" + taxid + ", lineage=" + lineage);
							}
						}
					}else {
						Log.debug(RdpHierParser.class, "Skipping counts from line [ " + lineNo + " ], because rank [" + rank + "] is not a reportable level.");
					}
				}
			} catch( FileNotFoundException e ) {
				e.printStackTrace();
				throw new BioLockJException( "Failed to find file: " + file.getAbsolutePath());
			} catch( IOException e ) {
				e.printStackTrace();
				throw new BioLockJException( "Found file, but encountered a problem while parsing line [" + lineNo + "]: " + file.getAbsolutePath());
			} 
			try {
				reader.close();
			} catch( IOException e ) {
				e.printStackTrace();
				throw new BioLockJException( "A problem occurred related to file: " + file.getAbsolutePath());
			}
		}
		for(TaxaLevelTable table : tt.values() ) table.fillEmptyVals();
		
		return tt;
	}
	
	/**
	 * given a header line of a hier_outfile from the RDP classifier, return the list of samples that are given in this file.
	 * Expected header: taxid	lineage	name	rank <sample1> [<sample2> ... ]
	 * @param line header of a file that is the result of the --hier_outfile option to RDP
	 * @return List of sample corresponding to columns, starting with column 5
	 * @throws BioLockJException 
	 */
	private static List<String> getSamples(String line) throws BioLockJException {
		boolean asExpected = true;
		List<String> ids = new ArrayList<>();
		StringTokenizer st = new StringTokenizer( line, Constants.TAB_DELIM );
		if ( st.countTokens() < 5 ) {
			throw new BioLockJException( "A hier_outfile file is expected to have 5 or more columns.  Found [ " 
							+ st.countTokens() + " ] tokens in header line:" + System.lineSeparator() + line );
		}
		if ( st.nextToken().trim() != TAXID ) asExpected = false;
		if ( st.nextToken().trim() != LINEAGE ) asExpected = false;
		if ( st.nextToken().trim() != NAME ) asExpected = false;
		if ( st.nextToken().trim() != RANK ) asExpected = false;
		while(st.hasMoreTokens()) {
			String columnName = st.nextToken();
			String id;
			try {
				id = SeqUtil.getSampleId( new File( columnName ) );
			} catch( Exception e ) {
				e.printStackTrace();
				Log.warn(RdpHierParser.class, "Failed to find a sample name corresponding to [" + columnName + "].");
				Log.warn(RdpHierParser.class, "Adding new sample: " + columnName + "");
				//MetaUtil.addSample( columnName );//TODO
				id = columnName;
			}
			ids.add( id );
		}
		if ( ! asExpected ) {
			Log.warn(RdpHierParser.class, "Unexpected names in header. Expected to find: " 
							+ TAXID + ", " + LINEAGE + ", " + NAME + ", " + RANK + " followed by some number of sample columns."
							+ "  Found: "  + System.lineSeparator() + line );	
		}
		return ids;
	}
	
	@Override
	public void executeTask() throws Exception {
		final List<List<String>> data = buildScript( getInputFiles() );
		BashScriptBuilder.buildScripts( this, data );
	}
	
	
	
	private static String stripQuotes(final String string) {
		String clean = string.trim();
		if ( !clean.isEmpty() && clean.charAt( 0 ) == '"' && clean.charAt( clean.length()-1 ) == '"') {
			clean = clean.substring( 1, clean.length()-1 );
		}
		return clean;
	}
	
	private static String TAXID = "taxid";
	private static String LINEAGE = "lineage";
	private static String NAME = "name";
	private static String RANK = "rank";
	
	/**
	 * File name prefix used to to distinguish additional normalized files from 
	 * the original {@value RdpClassifier#HIER_SUFFIX} files.
	 */
	private final String CN_ADJ = "cnadjusted_";
	
	@Override
	public List<File> getInputFiles() {
		ArrayList<File> list = new ArrayList<>(  );
		BioModule rdp = ModuleUtil.getPreviousModule( this );
		while( ! isValidInputModule( rdp ) ) {
			rdp = ModuleUtil.getPreviousModule( rdp );
		}
		Log.info(this.getClass(), "Input files are coming from RDP module: " + rdp);
		File[] oFiles = rdp.getOutputDir().listFiles(new FilenameFilter() {

			@Override
			public boolean accept( File dir, String name ) {
				return name.endsWith( RdpClassifier.HIER_SUFFIX ) && ! name.startsWith( CN_ADJ );
			}
		} );
		list.addAll( Arrays.asList(oFiles) );
		Log.info(this.getClass(), "Found [" + list.size() + "] input files: " + BioLockJUtil.getCollectionAsString( list ));
		return list;
	}

	@Override
	public boolean isValidInputModule( BioModule module ) {
		return RdpClassifier.class.isInstance( module );
	}
	

}
