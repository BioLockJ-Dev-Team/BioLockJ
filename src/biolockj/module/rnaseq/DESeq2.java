package biolockj.module.rnaseq;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import biolockj.Config;
import biolockj.Constants;
import biolockj.Log;
import biolockj.Properties;
import biolockj.api.ApiModule;
import biolockj.exception.ConfigFormatException;
import biolockj.exception.ConfigNotFoundException;
import biolockj.exception.ConfigPathException;
import biolockj.exception.ConfigViolationException;
import biolockj.exception.DockerVolCreationException;
import biolockj.module.BioModule;
import biolockj.module.ScriptModuleImpl;
import biolockj.module.report.r.R_Module;
import biolockj.module.report.taxa.BuildTaxaTables;
import biolockj.util.BioLockJUtil;
import biolockj.util.MasterConfigUtil;
import biolockj.util.MetaUtil;
import biolockj.util.TaxaUtil;

public class DESeq2 extends ScriptModuleImpl implements ApiModule {

	public DESeq2() {
		super();
		addGeneralProperty( Constants.EXE_RSCRIPT );
		addNewProperty( FACTORS, Properties.LIST_TYPE,
			"A comma-separated list of metadata columns to include as factors in the design forumula used with DESeq2." );
		addNewProperty( DESIGN, Properties.STRING_TYPE, "The exact string to use as the design the call to DESeqDataSetFromMatrix()." );
		addNewProperty( SCRIPT_PATH, Properties.FILE_PATH, "An R script to use in place of the default script to call DESeq2." );
	}

	@Override
	protected List<File> findModuleInputFiles() {
		List<File> allFiles = super.findModuleInputFiles();
		List<File> inputFiles = new ArrayList<>();
		for ( File file : allFiles ) {
			if( TaxaUtil.isTaxaFile( file ) ) {
				inputFiles.add( file );
			}
		}
		return inputFiles;
	}
	
	@Override
	public void executeTask() throws Exception {
		super.executeTask();
		FileUtils.copyFileToDirectory( R_Module.getFunctionLib(), getModuleDir() );
		setDesignString();
	}
	
	@Override
	public List<List<String>> buildScript( List<File> files ) throws Exception {
		List<List<String>> outer = new ArrayList<>();
		String scriptName = copyDEseqScript();
		for( final File file: files ) {
			List<String> inner = new ArrayList<>();
			String line = Config.getExe( this, Constants.EXE_RSCRIPT ) + " ../" + scriptName + " "
							+ file.getAbsolutePath() + " " 
							+ MetaUtil.getMetadata().getAbsolutePath() + " "
							+ TaxaUtil.getTaxonomyTableLevel(file) + "_";
			inner.add( line );
			outer.add( inner );
		}

		return outer;
	}
	
	private void setDesignString() throws ConfigViolationException {
		if ( Config.getString( this, DESIGN ) != null) {
			//return Config.getString( this, DESIGN );
		}else if ( Config.getList( this, FACTORS ) != null ) {
			String design = "";
			for (String factor : Config.getList( this, FACTORS )) {
				String symbol = design.length()==0 ? " ~ " : " + " ;
				design += ( symbol + factor);
			}
			Config.setConfigProperty( DESIGN, "\"" + design + "\"" );
			MasterConfigUtil.saveMasterConfig();
		}else {
			throw new ConfigViolationException( "Must specifiy one of [" + DESIGN + "] or [" + FACTORS + "]." );
		}
	}

	/**
	 * DESeq should only take raw values. So don't include '|| module instanceof TransformTaxaTables' even though that input is the right format.
	 */
	@Override
	public boolean isValidInputModule( BioModule module ) {
		return module instanceof BuildTaxaTables;
	}

	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		isValidProp( Constants.EXE_RSCRIPT );
		isValidProp( FACTORS );
		isValidProp( DESIGN );
		if ( Config.getString( this, FACTORS ) != null && Config.getString( this, DESIGN ) != null ) {
			throw new ConfigViolationException( "The properties [" + FACTORS + "] and [" + DESIGN + "] are mutually exclusive." );
		}
		if ( Config.getString( this, FACTORS ) == null && Config.getString( this, DESIGN ) == null ) {
			throw new ConfigNotFoundException( FACTORS, "Must supply one of [" + FACTORS + "] or [" + DESIGN + "]." );
		}
	}
	
	@Override
	public String getSummary() throws Exception {
		return super.getSummary() + System.lineSeparator() 
			+ "design parameter for DESeq2: " + Config.getString( this, DESIGN );
	}

	@Override
	public Boolean isValidProp( String property ) throws Exception {
		Boolean isValid = super.isValidProp( property );
		switch( property ) {
			case Constants.EXE_RSCRIPT:
				Config.getExe( this, Constants.EXE_RSCRIPT );
				isValid = true;
				break;
			case FACTORS:
				List<String> vals = Config.getList( this, FACTORS );
				for( String val: vals ) {
					MetaUtil.getFieldValues( val, true );
				}
				isValid = true;
				break;
			case DESIGN:
				String d = Config.getString( this, DESIGN );
				if ( d != null && !d.contains( "~" )) throw new ConfigFormatException( DESIGN, "The design should contain a '~', whole value should be in quotes." );
				else isValid = true;
				break;
		}
		return isValid;
	}

	@Override
	public String getDockerImageName() {
		return "r_deseq2";
	}

	public String copyDEseqScript() throws IOException, ConfigPathException, DockerVolCreationException {
		String name;
		File script = Config.getExistingFile( this, SCRIPT_PATH );
		if ( script != null) {
			name = script.getName();
			Log.info(this.getClass(), "Using user-supplied R script [" + name + "] for DESeq2 module.");
			FileUtils.copyFileToDirectory( script, getModuleDir() );
		}else {
			name = SCRIPT_NAME;
			Log.info(this.getClass(), "Using standard R script [" + name + "] for DESeq2 module.");
			File scriptDest = new File( getModuleDir(), name );
			Files.copy( this.getClass().getResourceAsStream( name ), scriptDest.toPath() );
		}
		return name;
	}

	@Override
	public String getDescription() {
		return "Determine statistically significant differences using DESeq2.";
	}
	
	@Override
	public String getDetails() {
		StringBuilder sb = new StringBuilder();
		sb.append( "The two methods of expresison the design are mutually exclusive.<br>" );
		sb.append( "*" + DESIGN +
			"* is used as an exact string to pass as the design argument to DESeqDataSetFromMatrix(); example: \" ~ Location:SoilType\" (DO include quotes around the formula). " );
		sb.append( "*" + FACTORS +
			"* is a list (such as \"fist,second\") of one or more metadata columns to use in a formula. " );
		sb.append( "Using this method, the formula will take the form: \" ~ first + second \" <br>" );
		sb.append( "The following two lines are equivilent:<br>" );
		sb.append( "`" + DESIGN + " =\"~ treatment + batch\"`<br>" );
		sb.append( "`" + FACTORS + " = treatment,batch `" );
		sb.append( System.lineSeparator() + System.lineSeparator() );
		sb.append( "Advanced users may want to make more advanced modifications to the call to the DESeq2 functions.  " );
		sb.append( "The easiest way to do this is to run the module with the default script, and treat that as a working template (ie, see how input/outputs are passed to/from the R script).  " );
		sb.append( "Modify the script in that first pipeline, and save the modified script to a stable location.  Then run the pipeline with *"+ SCRIPT_PATH + "* giving the path to the modified script." );
		return sb.toString();
	}

	@Override
	public String getCitationString() {
		return DEFAULT_CITATION + System.lineSeparator() + System.lineSeparator()
						+ "Module developed by Ivory, Ke and Rosh" + System.lineSeparator() 
						+ "BioLockJ " + BioLockJUtil.getVersion();
	}

	private static final String DESIGN = "deseq2.designFormula";
	private static final String FACTORS = "deseq2.designFactors";
	private static final String SCRIPT_PATH = "deseq2.scriptPath";
	private static final String SCRIPT_NAME = "DESeq2_module.R";
	private static String DEFAULT_CITATION = "R Core Team (2019). R: A language and environment for statistical computing. R Foundation for Statistical Computing, Vienna, Austria. URL https://www.R-project.org/." 
					+ System.lineSeparator()
					+ "Love, M.I., Huber, W., Anders, S. (2014) Moderated estimation of fold change and dispersion for RNA-seq data with DESeq2. Genome Biology, 15:550. 10.1186/s13059-014-0550-8";

}
