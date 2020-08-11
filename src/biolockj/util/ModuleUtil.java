/**
 * @UNCC Fodor Lab
 * @author Anthony Fodor
 * @email anthony.fodor@gmail.com
 * @date Apr 01, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.util;

import java.io.File;
import java.io.IOException;
import java.util.*;
import biolockj.*;
import biolockj.api.BioLockJ_API;
import biolockj.exception.BioLockJStatusException;
import biolockj.exception.ConfigFormatException;
import biolockj.exception.ConfigNotFoundException;
import biolockj.exception.PipelineFormationException;
import biolockj.module.BioModule;
import biolockj.module.JavaModule;
import biolockj.module.classifier.ClassifierModule;
import biolockj.module.report.r.R_Module;

/**
 * This utility holds general methods useful for BioModule interaction and management.
 */
public class ModuleUtil {

	// Prevent instantiation
	private ModuleUtil() {}

	/**
	 * Return the module ID as a 2 digit display number (add leading zero if needed).
	 * 
	 * @param module BioModule
	 * @return ID display value
	 */
	public static String displayID( final BioModule module ) {
		return BioLockJUtil.formatDigits( module.getID(), 2 );
	}
	
	/**
	 * Return the name of the module (or an alias if it has one).
	 * 
	 * @param module BioModule
	 * @return ID display value
	 */
	public static String displayName( final BioModule module ) {
		String alias = module.getAlias();
		if (alias != null) return alias;
		return module.getClass().getSimpleName();
	}
	
	public static String displaySignature( final BioModule module ) {
		return displayID( module ) + "_" + displayName( module );
	}

	/**
	 * Get a classifier module<br>
	 * Use checkAhead parameter to determine if we look forward or backwards starting from the given module.
	 * 
	 * @param module Calling BioModule
	 * @param checkAhead Boolean TRUE to check for NEXT, set FALSE to check BEHIND
	 * @return BioModule
	 */
	public static ClassifierModule getClassifier( final BioModule module, final boolean checkAhead ) {
		for( final BioModule m: getModules( module, checkAhead ) )
			if( m instanceof ClassifierModule ) return (ClassifierModule) m;
		return null;
	}

	/**
	 * Return the min number of samples that can be processed per worker script.
	 * 
	 * @param module BioModule
	 * @return Min # samples/worker
	 * @throws ConfigFormatException 
	 * @throws ConfigNotFoundException 
	 * @throws Exception 
	 */
	public static Integer getMinSamplesPerWorker( final BioModule module ) throws ConfigNotFoundException, ConfigFormatException, Exception {
		return new Double( Math.floor( (double) module.getInputFiles().size() / (double) getNumWorkers( module ) ) )
			.intValue();
	}

	/**
	 * Get a module with given className unless a classifier module is found 1st.<br>
	 * Use checkAhead parameter to determine if we look forward or backwards starting from the given module.
	 * 
	 * @param module Calling BioModule
	 * @param className Target BioModule class name
	 * @param checkAhead Boolean TRUE to check for NEXT, set FALSE to check BEHIND
	 * @return BioModule
	 * @throws Exception if errors occur
	 */
	public static BioModule getModule( final BioModule module, final String className, final boolean checkAhead )
		throws Exception {
		if( module instanceof ClassifierModule ) throw new Exception(
			"ModuleUtil.getModule( module, className, checkAhead) - Param \"module\" cannot be a ClassifierModule: " +
				module.getClass().getName() );

		final ClassifierModule classifier = getClassifier( module, checkAhead );
		for( final BioModule m: getModules( module, checkAhead ) )
			if( m.getClass().getName().equals( className ) ) {
				if( classifier == null ) return m;
				final boolean targetBeforeClassifier = m.getID() < classifier.getID();
				final boolean targetAfterClassifier = m.getID() > classifier.getID();
				if( checkAhead && targetBeforeClassifier || !checkAhead && targetAfterClassifier ) return m;

			}

		return null;
	}
	
	/**
	 * Construct a BioModule based on its className to add it to the pipeline.
	 * 
	 * @param className BioModule class name
	 * @return BioModule module
	 * @throws Exception if errors occur
	 */
	public static BioModule createModuleInstance( final String className ) throws Exception {
		BioModule module;
		try {
			module = (BioModule) Class.forName( className ).newInstance();
		}catch( ClassNotFoundException ex) {
			String path = getFullClass(className);
			if (path == null) suggestFullClass(className);
			module = (BioModule) Class.forName( getFullClass(className) ).newInstance();
		}
		return module;
	}
	
	/**
	 * @throws Exception 
	 * 
	 */
	private static String getFullClass(String simpleName ) throws Exception {
		initClassMap();
		Log.debug(ModuleUtil.class, "Found " + classMap.size() + " classes.");
		String classPath = classMap.get( simpleName );
		if ( classPath == null ) return null;
		else if ( classPath.contains( MODULE_NAME_SEP )) {
			throw new PipelineFormationException( "The name \"" + simpleName + "\" could refer to multiple modules. " + System.lineSeparator() 
			+ "Please use one of the following full class paths:" + System.lineSeparator() + classPath );
		}
		Log.info(ModuleUtil.class, "Interpreting module name \"" + simpleName + "\" as [ " + classPath + " ].");
		return classPath;
	}
	
	private static void initClassMap() throws Exception {
		if (classMap == null) {
			classMap = new HashMap<>();
			List<String> allModules;
			try {
				allModules = BioLockJ_API.listModules();
			}catch(RuntimeException re) {
				Log.warn(ModuleUtil.class, "Extra modules are currently ignored for class-lookup.");
				allModules = BioLockJ_API.listModules("biolockj");
			}
			
			for (String longName : allModules ) {
				String shortName = Class.forName( longName ).getSimpleName();
				if (classMap.containsKey( shortName )) {
					String existing;
					if ( classMap.get( shortName ).startsWith( Constants.BLJ_MODULE_TAG + " " )) existing = classMap.get( shortName );
					else existing = Constants.BLJ_MODULE_TAG + " " + classMap.get( shortName );
					longName = existing + MODULE_NAME_SEP + Constants.BLJ_MODULE_TAG + " " + longName;
				}
				classMap.put( shortName, longName);
			}
		}
	}
	
	private static void suggestFullClass(String className) throws Exception {
		String[] parts = className.split( "[.]" );
		String simpleName = parts[ parts.length - 1 ];
		Log.debug(ModuleUtil.class, "Consider the simple class name: " + simpleName);
		String suggestion = null;
		try{
			suggestion = getFullClass(simpleName);
		}catch(PipelineFormationException pe) {
			throw new PipelineFormationException( "The name \"" + className 
				+ "\" does not represent a valid BioModule. There may other modules called \"" + simpleName + "\"" );
		}
		if ( suggestion != null ) {
			throw new PipelineFormationException( "The name \"" + className 
				+ "\" does not represent a valid BioModule." + System.lineSeparator() + "Consider this module with same name:"
				+ System.lineSeparator() + Constants.BLJ_MODULE_TAG + " " + suggestion);
		}else {
			throw new PipelineFormationException( "The name \"" + className 
				+ "\" does not represent a valid BioModule.");
		}
		
	}
	
	/**
	 * Return pipeline modules after the given module if checkAhead = TRUE<br>
	 * Otherwise return pipeline modules before the given module.<br>
	 * If returning the prior modules, return the pipeline modules in reverse order, so the 1st item in the list is the
	 * module immediately preceding the given module.
	 * 
	 * @param module Reference BioModule
	 * @param checkAhead Set TRUE to return modules after the given reference module
	 * @return List of BioModules before/after the current module, as determined by checkAhead parameter
	 */
	public static List<BioModule> getModules( final BioModule module, final Boolean checkAhead ) {
		List<BioModule> modules = null;
		if( checkAhead ) modules = new ArrayList<>(
			new TreeSet<>( Pipeline.getModules().subList( module.getID() + 1, Pipeline.getModules().size() ) ) );
		else {
			modules = new ArrayList<>( new TreeSet<>( Pipeline.getModules().subList( 0, module.getID() ) ) );
			Collections.reverse( modules );
		}

		return modules;
	}

	/**
	 * BioModules are run in the order configured.<br>
	 * Return the module configured to run after the given module.
	 *
	 * @param module BioModule
	 * @return Next BioModule
	 */
	public static BioModule getNextModule( final BioModule module ) {
		if( module.getID() + 1 == Pipeline.getModules().size() ) return null;
		return Pipeline.getModules().get( module.getID() + 1 );
	}

	/**
	 * For uneven batch sizes, some workers will process 1 extra script. Return the number of workers that do so.
	 * 
	 * @param module BioModule
	 * @return Number of worker scripts that process 1 extra sequence.
	 * @throws Exception 
	 */
	public static Integer getNumMaxWorkers( final BioModule module )
		throws Exception {
		return module.getInputFiles().size() - getNumWorkers( module ) * getMinSamplesPerWorker( module );
	}

	/**
	 * Get the actual number of worker scripts generated for a given module, minimum value = 1.
	 * 
	 * @param module BioModule
	 * @return Number of worker scripts generated for a given module.
	 * @throws ConfigFormatException 
	 * @throws ConfigNotFoundException 
	 * @throws Exception 
	 */
	public static Integer getNumWorkers( final BioModule module ) throws ConfigNotFoundException, ConfigFormatException
		{
		if( module instanceof JavaModule ) return 1;
		final int count = Config.requirePositiveInteger( module, Constants.SCRIPT_NUM_WORKERS );
		int numFiles = count;
		try {
			numFiles = module.getInputFiles().size();
		} catch( Exception e ) {
			// if there is an issue with getting input files, 
			// this is not the an effective place to generate the message.
			e.printStackTrace();
		}
		return Math.max( 1, Math.min( count, numFiles ) );
	}

	/**
	 * BioModules are run in the order configured.<br>
	 * Return the module configured to run before the given module.
	 *
	 * @param module BioModule
	 * @return Previous BioModule
	 */
	public static BioModule getPreviousModule( final BioModule module ) {
		if( module.getID() == 0 ) return null;
		return Pipeline.getModules().get( module.getID() - 1 );
	}

	/**
	 * Return TRUE if module has executed.
	 *
	 * @param module BioModule
	 * @return TRUE if module has executed
	 */
	public static boolean hasExecuted( final BioModule module ) {
		return isComplete( module ) || isIncomplete( module );
	}

	/**
	 * Check a BioModule for scripts to execute.
	 * 
	 * @param module BioModule
	 * @return TRUE if the module has scripts to run
	 */
	public static boolean hasScripts( final BioModule module ) {
		final File scriptDir =
			new File( module.getModuleDir().getAbsolutePath() + File.separator + Constants.SCRIPT_DIR );
		return scriptDir.isDirectory() && scriptDir.list().length > 0;
	}

	/**
	 * Return TRUE if module completed successfully.
	 *
	 * @param module BioModule
	 * @return TRUE if module has completed successfully.
	 */
	public static boolean isComplete( final BioModule module ) {
		final File f = new File( module.getModuleDir().getAbsolutePath() + File.separator + Constants.BLJ_COMPLETE );
		return f.isFile();
	}

	/**
	 * Test if module is the first {@link biolockj.module.report.r.R_Module} configured in the pipeline.
	 * 
	 * @param module BioModule to test
	 * @return TRUE if module is 1st {@link biolockj.module.report.r.R_Module} in this branch
	 */
	public static boolean isFirstRModule( final BioModule module ) {
		final List<Integer> rIds = getRModulesIds();
		final List<Integer> filteredR_ids = new ArrayList<>( rIds );
		if( rIds.contains( module.getID() ) ) {
			final List<Integer> cIds = getClassifierIds();
			if( !cIds.isEmpty() ) {
				final BioModule prevClassMod = getClassifier( module, false );
				final BioModule nextClassMod = getClassifier( module, true );
				for( final Integer rId: rIds ) {
					if( prevClassMod != null && rId < prevClassMod.getID() ) filteredR_ids.remove( rId );
					if( nextClassMod != null && rId > nextClassMod.getID() ) filteredR_ids.remove( rId );
				}

				Log.debug( ModuleUtil.class,
					"Removed out-of-scope IDs, leaving valid R IDs = " + filteredR_ids.size() );
			}

			if( !filteredR_ids.isEmpty() ) return filteredR_ids.get( 0 ).equals( module.getID() );
		}

		return false;
	}

	/**
	 * Return TRUE if module started execution but is not complete.
	 *
	 * @param module BioModule
	 * @return TRUE if module started execution but is not complete
	 */
	public static boolean isIncomplete( final BioModule module ) {
		final File s = new File( module.getModuleDir().getAbsolutePath() + File.separator + Constants.BLJ_STARTED );
		final File f = new File( module.getModuleDir().getAbsolutePath() + File.separator + Constants.BLJ_FAILED );
		return s.isFile() || f.isFile();
	}

	/**
	 * Method determines if the given module is a metadata-module (which does not use/modify sequence data.
	 * 
	 * @param module BioModule in question
	 * @return TRUE if module produced exactly 1 file (metadata file)
	 */
	public static boolean isMetadataModule( final BioModule module ) {
		boolean foundMeta = false;
		boolean foundOther = false;
		final List<File> files = Arrays.asList( module.getOutputDir().listFiles() );
		for( final File f: files )
			if( f.getName().equals( MetaUtil.getFileName() ) ) foundMeta = true;
			else if( !Config.getSet( module, Constants.INPUT_IGNORE_FILES ).contains( f.getName() ) ) foundOther = true;

		return foundMeta && !foundOther;
	}

	/**
	 * Method creates a file named {@value biolockj.Constants#BLJ_COMPLETE} in module root directory to document module
	 * has completed successfully. Also clean up by removing file {@value biolockj.Constants#BLJ_STARTED}.
	 *
	 * @param module BioModule
	 * @throws IOException 
	 * @throws BioLockJStatusException 
	 * @throws Exception if unable to create {@value biolockj.Constants#BLJ_COMPLETE} file
	 */
	public static void markComplete( final BioModule module ) throws BioLockJStatusException, IOException{
		PipelineUtil.markStatus( module, Constants.BLJ_COMPLETE );
		Log.info( ModuleUtil.class, Constants.LOG_SPACER );
		Log.info( ModuleUtil.class,
			"FINISHED [ " + ModuleUtil.displaySignature( module ) + " ] " );
		Log.info( ModuleUtil.class, Constants.LOG_SPACER );
	}

	/**
	 * Method creates a file named {@value biolockj.Constants#BLJ_STARTED} in module root directory to document module
	 * has completed successfully. Also sets the start time and caches module name to list of executed modules so we can
	 * check later if it ran during this pipeline execution (as opposed to a previous failed run).
	 *
	 * @param module module
	 * @throws IOException 
	 * @throws BioLockJStatusException 
	 * @throws Exception if unable to create {@value biolockj.Constants#BLJ_STARTED} file
	 */
	public static void markStarted( final BioModule module ) throws BioLockJStatusException, IOException{
		PipelineUtil.markStatus( module, Constants.BLJ_STARTED );
		Log.info( ModuleUtil.class, Constants.LOG_SPACER );
		Log.info( ModuleUtil.class,
			"STARTING [ " + ModuleUtil.displaySignature( module ) + " ] " );
		Log.info( ModuleUtil.class, Constants.LOG_SPACER );
	}

	/**
	 * Check if a module was in the pipeline at least once.
	 * 
	 * @param className module simple name
	 * @return boolean
	 */
	public static boolean moduleExists( final String className ) {
		for( final BioModule m: Pipeline.getModules() )
			if( m.getClass().getName().equals( className ) ) return true;
		return false;
	}

	/**
	 * Get BioModule subdirectory File object with given name. If directory doesn't exist, create it.
	 *
	 * @param module BioModule
	 * @param subDirName BioModule sub-directory name
	 * @return BioModule sub-directory File object
	 */
	public static File requireSubDir( final BioModule module, final String subDirName ) {
		final File dir = new File( module.getModuleDir().getAbsolutePath() + File.separator + subDirName );
		if( !dir.isDirectory() ) {
			dir.mkdirs();
			Log.info( ModuleUtil.class, "Create directory: " + dir.getAbsolutePath() );
		}
		return dir;
	}

	/**
	 * Return TRUE if BioModule sub-directory exists
	 *
	 * @param module BioModule
	 * @param subDirName BioModule sub-directory name
	 * @return TRUE if BioModule sub-directory exists
	 */
	public static boolean subDirExists( final BioModule module, final String subDirName ) {
		final File dir = new File( module.getModuleDir().getAbsolutePath() + File.separator + subDirName );
		return dir.isDirectory();
	}

	private static List<Integer> getClassifierIds() {
		final List<Integer> ids = new ArrayList<>();
		for( final BioModule m: Pipeline.getModules() )
			if( m instanceof ClassifierModule ) ids.add( m.getID() );
		return ids;
	}

	private static List<Integer> getRModulesIds() {
		final List<Integer> ids = new ArrayList<>();
		for( final BioModule m: Pipeline.getModules() )
			if( m instanceof R_Module ) ids.add( m.getID() );
		return ids;
	}
	
	/**
	 * Key is the simple name and value is the long name for module classes.
	 */
	private static HashMap<String, String> classMap = null;
	
	private static final String MODULE_NAME_SEP = System.lineSeparator();

}
