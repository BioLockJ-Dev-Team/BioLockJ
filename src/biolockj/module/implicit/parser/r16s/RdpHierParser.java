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
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Constants;
import biolockj.Log;
import biolockj.Properties;
import biolockj.api.ApiModule;
import biolockj.exception.BioLockJException;
import biolockj.exception.ConfigFormatException;
import biolockj.exception.ConfigPathException;
import biolockj.exception.DockerVolCreationException;
import biolockj.module.BioModule;
import biolockj.module.JavaModuleImpl;
import biolockj.module.classifier.r16s.RdpClassifier;
import biolockj.util.BashScriptBuilder;
import biolockj.util.BioLockJUtil;
import biolockj.util.ModuleUtil;
import biolockj.util.SeqUtil;
import biolockj.util.TaxaUtil;
import biolockj.module.report.taxa.TaxaLevelTable;

public class RdpHierParser extends JavaModuleImpl implements ApiModule {

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
		while( fls.hasMoreTokens() ) {
			File file = new File(fls.nextToken());
			files.add( file );
			System.err.println("File [" + file.getName() + "] exists: " + file.exists() );
		}
		
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
	
	@Override
	public List<File> getInputFiles() {
		ArrayList<File> list = new ArrayList<>(  );
		BioModule rdp = ModuleUtil.getPreviousModule( this );
		while( rdp != null & ! isValidInputModule( rdp ) ) {
			rdp = ModuleUtil.getPreviousModule( rdp );
		}
		File inputDir;
		if (rdp == null) {
			inputDir = useInputDir();
		}else {
			Log.info(this.getClass(), "Input files are coming from RDP module: " + rdp);
			inputDir = rdp.getOutputDir();
		}
		
		File[] oFiles = inputDir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept( File dir, String name ) {
				return name.endsWith( RdpClassifier.HIER_SUFFIX ) && ! name.startsWith( CN_ADJ );
			}
		} );
		list.addAll( Arrays.asList(oFiles) );
		Log.info(this.getClass(), "Found [" + list.size() + "] input files: " + BioLockJUtil.getCollectionAsString( list ));
		return list;
	}
	
	private File useInputDir() {
		File dir;
		try {
			dir = Config.getExistingDir( this, Constants.INPUT_DIRS );
		} catch( ConfigPathException | DockerVolCreationException e ) {
			e.printStackTrace();
			Log.error(this.getClass(), "Failed to find input dir.");
			dir = new File(BioLockJ.getPipelineDir(), "input");
		}
		return dir;
	}

	@Override
	public boolean isValidInputModule( BioModule module ) {
		return RdpClassifier.class.isInstance( module );
	}

	@Override
	public List<String> getPreRequisiteModules() throws Exception {
		List<String> list = super.getPreRequisiteModules();
		List<String> inTypes = Config.getList( this, BioLockJUtil.INTERNAL_PIPELINE_INPUT_TYPES );
		Log.debug(this.getClass(), "Found input types: " + BioLockJUtil.getCollectionAsString( inTypes ) );
		if ( inTypes.stream().anyMatch( s -> s.equals( okInputTypes[0] ) ) ) {
			Log.debug(this.getClass(), "Input types include required input type: " + okInputTypes[0]);
		}else {
			Log.debug(this.getClass(), "Input types list does not include required input type: " + okInputTypes[0]);
			Log.debug(this.getClass(), "Module " + RdpClassifier.class.getName() + " is required." );
			list.add( RdpClassifier.class.getName() );
		}
		return list;
	}

	@Override
	public String getDescription() {
		return "Create taxa tables from the " + RdpClassifier.HIER_SUFFIX + " files output by RDP.";
	}
	
	@Override
	public String getDetails() {
		StringBuffer sb = new StringBuffer();
		sb.append( "This module **requires** that _" + RdpClassifier.HIER_COUNTS + "_=" + Constants.TRUE + " for the " );
		sb.append( RdpClassifier.class.getSimpleName() + " module to make the required output type.  As long as _" + RdpClassifier.HIER_COUNTS );
		sb.append( "_ is set, this module will automatically be added to the module run order by the " + RdpClassifier.class.getSimpleName() + " module." );
		sb.append( "<br>If this module is in the module run order, it adds `" + RdpClassifier.class.getName() );
		sb.append( "` as a pre-quisite module. <br>To use this module without the RDP module, include " );
		sb.append( okInputTypes[ 0 ] + " in the list of input types:<br>`" + Constants.INPUT_TYPES + "=" );
		sb.append( okInputTypes[ 0 ] + "`<br>When using input from a directory, this module takes **exactly** one input directory." );
		sb.append( "<br><br>This module is an alternative to the default parser, " + RdpParser.class.getSimpleName() + ".  " );
		sb.append( "The two parsers produce nearly identical output. The " + RdpParser.class.getSimpleName() );
		sb.append( " module parses the output for each sequence and determines counts for each taxanomic unit. It fills in missing levels so all sequences are counted for all taxanomic levels; this means reads that are unclassified are reported as an OTU with \"unclassified\" in the name."  );
		sb.append( "By contrast, the " + this.getClass().getSimpleName() + " module relies on RDP to determine these totals." );
		sb.append( "When using " + RdpParser.class.getSimpleName() + " the confidence threshold is applied by the parser,");
		sb.append( " when using " + this.getClass().getSimpleName() + " the coinfidence threshold is applied by RDP during classification." );
		return sb.toString();
	}

	@Override
	public String getCitationString() {
		return "Module created by Ivory Blakley";
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
	
	private static final String[] okInputTypes = {"ModuleOutput[RdpClassifier]"};

}
