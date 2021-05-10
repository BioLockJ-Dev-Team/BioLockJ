/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 18, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.report.r;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import org.apache.commons.io.FileUtils;
import biolockj.*;
import biolockj.Properties;
import biolockj.exception.BioLockJException;
import biolockj.exception.ConfigFormatException;
import biolockj.exception.ConfigNotFoundException;
import biolockj.exception.ConfigPathException;
import biolockj.exception.ConfigViolationException;
import biolockj.exception.DockerVolCreationException;
import biolockj.exception.RequiredRPackageException;
import biolockj.exception.SpecialPropertiesException;
import biolockj.module.BioModule;
import biolockj.module.ScriptModuleImpl;
import biolockj.util.*;

/**
 * This BioModule is the superclass for R script generating modules.
 */
public abstract class R_Module extends ScriptModuleImpl {
	
	public R_Module() {
		super();
		addGeneralProperty( Constants.EXE_RSCRIPT );
		addGeneralProperty( Constants.R_TIMEOUT );
		addGeneralProperty( Constants.R_DEBUG );
		addGeneralProperty( Constants.R_SAVE_R_DATA );
		addGeneralProperty( Constants.R_COLOR_FILE );
		addGeneralProperty( Constants.DEFAULT_STATS_MODULE );
		addNewProperty( getCustomScriptProp(), Properties.FILE_PATH, "Path to a custom R script to use in place of the built-in module script." );
	}

	/**
	 * Builds an R script by calling sub-methods to builds the BaseScript and creates the MAIN script shell that sources
	 * the BaseScript, calls runProgram(), reportStatus() and main() which can only be implemented in a subclass.<br>
	 */
	@Override
	public void executeTask() throws Exception {
		getOutputDir();
		getTempDir();
		getLogDir();
		super.executeTask();
	}
	
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception {
		List<List<String>> outer = new ArrayList<>();
		getFunctionLib();
		for (String level : Config.getList( this, Constants.REPORT_TAXONOMY_LEVELS )) {
			List<String> inner = new ArrayList<>();
			inner.add( Config.getExe( this, Constants.EXE_RSCRIPT ) + " " + getModuleRScript().getAbsolutePath() + " " + level );
			outer.add( inner );
		}
		return outer;
	}
	
	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		getModuleRScript();
		Config.getExe( this, Constants.EXE_RSCRIPT );
		boolean its0 = Config.getString( this, Constants.R_TIMEOUT ) != null && Config.getString( this, Constants.R_TIMEOUT ).equals("0");
		if ( ! its0 ) Config.getPositiveInteger( this, Constants.R_TIMEOUT );
		Config.getBoolean( this, Constants.R_DEBUG );
		Config.getBoolean( this, Constants.R_SAVE_R_DATA );
		verifyColorFileFormat();
		checkRPackages();
	}
	
	protected Map<String, String> requiredRPackages(){
		Map<String, String> packages = new HashMap<>();
		//These packages are required by the current BioLockJ_Lib.R functions
		packages.put("properties","https://CRAN.R-project.org");
		packages.put("stringr","https://CRAN.R-project.org");
		packages.put("ggpubr","https://CRAN.R-project.org");
		return packages;
	}

	private void checkRPackages() throws SpecialPropertiesException, IOException, InterruptedException,
		RequiredRPackageException, ConfigNotFoundException {
		Set<String> failedPackages = new HashSet<String>();
		for( String pack: requiredRPackages().keySet() ) {
			if( !checkPackage( pack ) ) failedPackages.add( pack );
		}
		if( failedPackages.size() > 0 ) { throw new RequiredRPackageException( failedPackages, requiredRPackages() ); }
	}
	
	private boolean checkPackage(String packageName) throws SpecialPropertiesException, IOException, InterruptedException, ConfigNotFoundException {
		boolean isGood;
		String cmd = Config.getExe( this, Constants.EXE_RSCRIPT ) + " -e 'library(" + packageName + ");packageVersion(\"" + packageName + "\")'";
		//TODO: revisit this after the check exe feature is finished.
//		if (Config.getString( this, Constants.PIPELINE_ENV ).equals(Constants.PIPELINE_ENV_CLUSTER)) {
//			for (String addLib : BashScriptBuilder.loadModules(this)) {
//				cmd = addLib + "; " + cmd;
//			}
//		}
		if (DockerUtil.inDockerEnv()) {
			//TODO: revisit this after the check exe feature is finished.
			cmd = Config.getExe( this, Constants.EXE_DOCKER ) + " run --rm " + DockerUtil.getDockerImage( this ) + " " + cmd;
		}
		Log.info(R_Module.class, "Checking for R package [" + packageName + "] using command: " + cmd);
		Process p = Runtime.getRuntime().exec( cmd );
		final BufferedReader brOut = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
		final BufferedReader brErr = new BufferedReader( new InputStreamReader( p.getErrorStream() ) );
		String out = null;
		String err = null;
		do{
			if (out != null) Log.info(getClass(), out);
			if (err != null) Log.info(getClass(), err);
			out = brOut.readLine();
			err = brErr.readLine();
		}while( out != null ||  err != null ) ;
		p.waitFor();
		p.destroy();
		isGood = p.exitValue() == 0;
		return isGood;
	}

	/**
	 * The R Script should run quickly, timeout = 10 minutes appears to work well.
	 * @throws ConfigFormatException 
	 */
	@Override
	public Integer getTimeout() throws ConfigFormatException {
		int timeout = 0;
		if (Config.getString( this, Constants.R_TIMEOUT ) == null ||
						Config.getIntegerProp( this, Constants.R_TIMEOUT ) == 0) {
			Log.info(R_Module.class, Constants.R_TIMEOUT + " is set to null or 0; so the timeout time will be calcuated by the module.");
			timeout = calcTimeout(getInputFiles());
			Log.info(getClass(), "Using calculated timeout of " + timeout + " minutes.");
		}else {
			timeout = Config.getPositiveInteger( this, Constants.R_TIMEOUT );
		}
		return timeout;
	}
	
	/**
	 * Use this to estimate the max time needed to process a given list of inputs.
	 * Estimate the time that the head node should wait on the module using all inputs;
	 * Estimate the wait time for a given worker using only its assigned inputs.
	 * @param inputs
	 * @return
	 */
	protected int calcTimeout(final List<File> inputs ) {
		long totalSize = 0;
		for (File input : inputs) {
			totalSize += input.length();
		}
		int megabytes = ((Long) Long.divideUnsigned( totalSize, 1000000 )).intValue();
		if (megabytes == 0) megabytes = 1;
		// a 2-minute baseline, plus 5 minutes per megabyte of input.
		int minutes = 2 + ( 5 * megabytes );
		return minutes;
	}

	private void verifyColorFileFormat() throws ConfigPathException, IOException, ConfigViolationException, DockerVolCreationException {
		final File colorFile = Config.getExistingFile( this, Constants.R_COLOR_FILE );
		if( colorFile != null ) {
			final BufferedReader br = BioLockJUtil.getFileReader( colorFile );
			final String[] header = br.readLine().split( TAB_DELIM );
			br.close();
			if( !header[ 0 ].equals( "key" ) || !header[ 1 ].equals( "color" ) )
				throw new ConfigViolationException( Constants.R_COLOR_FILE,
					"The color reference file [ " + colorFile.getAbsolutePath() +
						" ] should be a tab-delimited file, column labels: \"key\" and \"color\"." + RETURN +
						"Tip: use a color reference file generated by BioLockJ as a template." );
		}
	}

	protected File getFunctionLib() throws IOException, BioLockJException  {
		return getFunctionLib(this);
	}
	
	public static File getFunctionLib(BioModule module) throws IOException, BioLockJException  {
		URL in = R_Module.class.getResource( "R/" + Constants.R_FUNCTION_LIB );
		File lib = new File(module.getResourceDir(), Constants.R_FUNCTION_LIB);
		FileUtils.copyURLToFile(in, lib);
		if( !lib.isFile() ) throw new BioLockJException( "Missing R function library: " + lib.getAbsolutePath() );
		return lib;
	}
	
	protected File getModuleRScript() throws IOException, BioLockJException {
		File script;
		File customScript = Config.getExistingFile( this, getCustomScriptProp() );
		if (customScript != null) {
			script = new File(getResourceDir(), customScript.getName());
			FileUtils.copyFileToDirectory( customScript, getResourceDir() );
		}else {
			script = new File(getResourceDir(), getModuleRScriptName());
			if (!script.exists()) {
				InputStream in = this.getClass().getResourceAsStream( "R/" + getModuleRScriptName() );
				Files.copy( in, script.toPath() );
			}			
		}
		if( !script.isFile() ) throw new BioLockJException( "Missing module R script: " + script.getAbsolutePath() );
		return script;
	}
	
	protected abstract String getModuleRScriptName();
	
	@Override
	public String getDockerImageOwner() {
		return Constants.MAIN_DOCKER_OWNER;
	}
	
	@Override
	public String getDockerImageName() {
		return "r_module";
	}
	
	@Override
	public String getDockerImageTag() {
		return "v1.3.18";
	}
	
	/**
	 * @return The property name that can be used for passing a custom version of the R script used for this module.
	 */
	protected String getCustomScriptProp() {
		return getModulePrefix() + "." + CUSTOM_SCRIPT_PROP_SUFFIX;
	}
	
	/**
	 * Returns that prefix to use for property names.
	 * @return
	 */
	protected abstract String getModulePrefix();
	
	private static final String CUSTOM_SCRIPT_PROP_SUFFIX = "customScript";

	@Override
	public void cleanUp() throws Exception {
		super.cleanUp();
		grapRUsedProps();
	}
	
	protected void grapRUsedProps() throws Exception {
		for (File log : getLogDir().listFiles()) {
			BufferedReader reader = new BufferedReader( new FileReader( log ) );
			String line;
			while( (line = reader.readLine()) != null ) {
				int i1 = line.indexOf( "=" );
				if (line.startsWith( R_USED_PROP_KEY ) && i1 > -1) {
					line = line.replaceFirst( R_USED_PROP_KEY, "" );
					int i = line.indexOf( "=" );
					String name = line.substring( 0, i ).trim();
					String val = line.substring( i + 1 ).trim();
					Config.setConfigProperty( name, val );
				}
			}
			reader.close();
		}
	}
	
	/**
	 * Key string that is also hard-coded in the BioLockJ_Lib.R
	 */
	private static final String R_USED_PROP_KEY = "USED_PROPERTY: ";
	
}
