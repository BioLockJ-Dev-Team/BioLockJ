/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 9, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import biolockj.*;
import biolockj.util.*;

/**
 * Superclass for Java BioModules that will be called in separate instances of the application.
 */
public abstract class JavaModuleImpl extends ScriptModuleImpl implements JavaModule {

	/**
	 * Java script only require 2 lines, one to run the blj_config to update our $PATH and gain access to environment
	 * variables, and then the direct call to the BioLockJ.jar.
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception {
		final List<List<String>> data = new ArrayList<>();
		final ArrayList<String> lines = new ArrayList<>();
		lines.add( runBioLockJ_CMD() + " " + RuntimeParamUtil.getJavaModuleArgs( this ) );

		data.add( lines );
		return data;
	}

	/**
	 * JavaModules run pure Java code.<br>
	 * If in Direct mode, execute {@link #runModule()} to run the module functionality; another BioLockJ instance 
	 * is the manager that has launched this instance to run this module.<br>
	 * 
	 * Otherwise, this instance is the manager,<br>
	 * If {@link biolockj.Config}.{@value biolockj.Constants#DETACH_JAVA_MODULES}={@value biolockj.Constants#TRUE} execute
	 * {@link biolockj.module.ScriptModule#executeTask()} to build the bash scripts to launch a secondary instance in direct mode to run this module.<br>
	 * Otherwise (if {@value biolockj.Constants#DETACH_JAVA_MODULES}={@value biolockj.Constants#FALSE}), 
	 * execute {@link #runModule()} to run the Java code to execute module functionality within the manager instance.
	 */
	@Override
	public void executeTask() throws Exception {
		if( BioLockJUtil.isDirectMode() ) runModule();
		else if( Config.getBoolean( this, Constants.DETACH_JAVA_MODULES ) ) super.executeTask();
		else runModule();
	}

	/**
	 * If in Docker mode, set {@value #BLJ_OPTIONS} which will be reference in every worker script when running BioLockJ
	 * in direct mode.
	 */
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception {
		final List<String> lines = new ArrayList<>();
		if( DockerUtil.inDockerEnv() )
			lines.add( BLJ_OPTIONS + "=\"" + RuntimeParamUtil.getJavaModuleArgs( this ) + "\"" + RETURN );

		return lines;
	}

	/**
	 * If module is a {@link biolockj.module.SeqModule} input must contain sequence data.
	 */
	@Override
	public boolean isValidInputModule( final BioModule module ) {
		if( this instanceof SeqModule ) return SeqUtil.isSeqModule( module );
		return super.isValidInputModule( module );
	}

	@Override
	public void moduleComplete() throws Exception {
		markStatus( Constants.SCRIPT_SUCCESS );
		Log.info( getClass(), "Direct module complete!  Terminate direct application instance." );
	}

	@Override
	public void moduleFailed() throws Exception {
		markStatus( Constants.SCRIPT_FAILURES );
		Log.info( getClass(), "Direct module failed!  Terminate direct application instance." );
	}

	@Override
	public abstract void runModule() throws Exception;

	/**
	 * This method sets the module status by saving the indicator file to the module root dir.
	 * 
	 * @param status Success or Failures
	 * @throws Exception if unable to set the status
	 */
	protected void markStatus( final String status ) throws Exception {
		File statusIndicator = null;
		File script = null;
		final Collection<File> files = FileUtils.listFiles( getScriptDir(), HiddenFileFilter.VISIBLE, null );
		final String key1 = ".0_" + getClass().getSimpleName() + SH_EXT;
		for( final File file: files ) {
			if( statusIndicator == null && file.getName().endsWith( key1 + "_" + status ) ) statusIndicator = file;
			if( script == null && file.getName().endsWith( key1 ) ) script = file;
		}

		if( script == null ) {
			final String msg = "Cannot find DIRECT script in:  " + getScriptDir().getAbsolutePath();
			Log.warn( getClass(), msg );
			throw new Exception( msg );
		}

		if( statusIndicator == null ) {
			final String path = script.getAbsolutePath() + "_" + status;
			Log.info( getClass(), "Saving file: " + path );
			final FileWriter writer = new FileWriter( new File( path ) );
			writer.close();
		} else {
			final String msg = "Program status already set: " + statusIndicator.getAbsolutePath();
			Log.warn( getClass(), msg );
			throw new Exception( msg );
		}
	}

	/**
	 * Get the java command to launch a module directly.  
	 * The java command will be a repeat of the command originally used to launch the program, 
	 * only the arguments to BioLockJ will be different; they are set in a different method.
	 * If any external modules were used in this pipeline, they should be added to the class path in the launch script.
	 * At this stage, the same class path used to launch the program is used for the module.
	 * 
	 * @return java source parameter (either Jar or main class with class-path)
	 * @throws Exception if unable to determine source
	 */
	protected final String runBioLockJ_CMD() {
		String javaString = "java -cp " + System.getProperty("java.class.path") + " " + BioLockJ.class.getName();
		Log.debug( getClass(), "BioLockJ Java source code for java command: " + javaString );
		return javaString;
	}
	
	public String getDockerImageName() {
		return Constants.MAIN_DOCKER_IMAGE;
	}

	/**
	 * Docker environment variable holding the Docker program switches: {@value #BLJ_OPTIONS}
	 */
	protected static final String BLJ_OPTIONS = "BLJ_OPTIONS";
}
