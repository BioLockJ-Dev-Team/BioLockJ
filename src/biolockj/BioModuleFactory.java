/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 9, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org
 */
package biolockj;

import java.util.*;
import biolockj.api.ApiModule;
import biolockj.exception.PipelineFormationException;
import biolockj.module.BioModule;
import biolockj.module.implicit.ImportMetadata;
import biolockj.util.*;

/**
 * This class initializes pipeline modules, starting with those in the Config file and adding the prerequisite and
 * post-requisite modules.
 */
public class BioModuleFactory {
	private BioModuleFactory() throws Exception {
		initModules();
	}

	/**
	 * This method returns all module post-requisites (including post-requisites of the post-requisites).
	 * 
	 * @param module Current BioModule
	 * @return List of post-requisite module names
	 * @throws Exception if runtime errors occur
	 */
	protected List<String> getPostRequisites( final BioModule module ) throws Exception {
		if( --this.safteyCheck == 0 )
			throw new Exception( "Too many calls [" + SAFE_MAX + "] to getPostRequisites( module )" );
		final List<String> postReqs = new ArrayList<>();
		for( final String postReq: module.getPostRequisiteModules() ) {
			if( !postReqs.contains( postReq ) ) postReqs.add( postReq );

			final List<String> postPostReqs = getPostRequisites( ModuleUtil.createModuleInstance( postReq ) );
			for( final String postPostReq: postPostReqs )
				if( !postReqs.contains( postPostReq ) ) postReqs.add( postPostReq );
		}

		return postReqs;
	}

	/**
	 * This method returns all module prerequisites (including prerequisites of the prerequisites).
	 * 
	 * @param module Current BioModule
	 * @return List of prerequisite module names
	 * @throws Exception if runtime errors occur
	 */
	protected List<String> getPreRequisites( final BioModule module ) throws Exception {
		if( --this.safteyCheck == 0 )
			throw new Exception( "Too many calls [" + SAFE_MAX + "] to getPreRequisites( module )" );
		final List<String> preReqs = new ArrayList<>();
		for( final String preReq: module.getPreRequisiteModules() ) {
			checkPreReq(module, preReq);
			Log.info(BioModuleFactory.class, "Module " + module + " has pre-req: " + preReq);
			BioModule preReqInst = ModuleUtil.createModuleInstance( preReq );
			final List<String> prePreReqs = getPreRequisites( preReqInst );
			for( final String prePreReq: prePreReqs ) {
				checkPreReq(preReqInst, prePreReq);
				if( !preReqs.contains( prePreReq ) ) preReqs.add( prePreReq );
			}
			if( !preReqs.contains( preReq ) ) preReqs.add( preReq );
		}

		return preReqs;
	}
	
	private void checkPreReq(final BioModule module, final String preReq) {
		Log.info(BioModuleFactory.class, "Module " + module + " has pre-req: " + preReq);
		if (module instanceof ApiModule) {
			ApiModule mod = (ApiModule) module;
			if ( !mod.getDescription().contains( preReq )) {
				Log.debug(BioModuleFactory.class, Constants.DEVELOPER_NOTE 
					+ "Module [" + module + "] has preReq [" + preReq + "] that is not mentioned in the user guide Details section.");
			}
		}
	}

	private String addModule( final String className ) {
		if( className.startsWith( MODULE_CLASSIFIER_PACKAGE ) ) this.branchClassifier = true;

		return className;
	}

	/**
	 * The method returns the ordered list of BioModules required as part of the runtime {@link biolockj.Config}
	 * file.<br>
	 * Each line the begins with {@value biolockj.Constants#INTERNAL_BLJ_MODULE} should be followed by the full class
	 * name of a Java class that implements the {@link biolockj.module.BioModule } interface.
	 * 
	 * @return List of BioModules
	 * @throws Exception thrown for invalid BioModules
	 */
	private List<BioModule> buildModules() throws Exception {
		final List<BioModule> bioModules = new ArrayList<>();
		for( final String moduleLine: this.moduleCache ) {
			String[] parts = moduleLine.split(Constants.ASSIGN_ALIAS);
			String className = parts[0].trim();
			final BioModule module = ModuleUtil.createModuleInstance( className );
			if( parts.length > 1 ) {
				if (parts.length > 2) {
					throw new PipelineFormationException( "Too many parts to the module definition line: " + moduleLine );
				}
				module.setAlias( parts[1].trim() );
				Log.debug(BioModuleFactory.class, "This instance of the [" + module.getClass().getSimpleName() 
					+ "] module will be refered to by its alias: \"" + module.getAlias() + "\".");
			}			
			module.init();
			for( final BioModule existingModule : bioModules) {
				if ( ModuleUtil.displayName( existingModule ).equals( ModuleUtil.displayName( module ) ) ) {
					throw new PipelineFormationException( "Cannot have multiple modules in the same pipeline that are called \"" 
				+ ModuleUtil.displayName( module ) + "\"." + System.lineSeparator() + "Use \"module.path AS newName\" in the module run order to give one of them a different alias." );
				}
			}
			bioModules.add( module );
		}

		return bioModules;
	}

	/**
	 * Register the complete list of Java class.getSimpleName() values for the configured modules.
	 * 
	 * @throws Exception if errors occur
	 */
	private void initModules() throws Exception {
		final List<String> configModules = getConfigModules();
		List<String> branchModules = new ArrayList<>();

		for( final String moduleLine: configModules ) {
			this.safteyCheck = SAFE_MAX;
			String[] parts = moduleLine.split(Constants.ASSIGN_ALIAS);
			String className = parts[0].trim();
			final BioModule module = ModuleUtil.createModuleInstance( className );
			if( !Config.getBoolean( null, Constants.DISABLE_PRE_REQ_MODULES ) )
				for( final String mod: getPreRequisites( module ) )
				if( !branchModules.contains( mod ) ) branchModules.add( addModule( mod ) );

			if( !branchModules.contains( moduleLine ) ) branchModules.add( addModule( moduleLine ) );

			this.safteyCheck = SAFE_MAX;
			if( !module.getPostRequisiteModules().isEmpty() ) for( final String mod: getPostRequisites( module ) )
				if( !branchModules.contains( mod ) ) branchModules.add( addModule( mod ) );

			if( this.foundClassifier && this.branchClassifier ) {
				info( "Found another classifier: reset branch" );
				this.branchClassifier = false;
				this.foundClassifier = false;
				this.moduleCache.addAll( branchModules );
				branchModules = new ArrayList<>();
			} else if( this.branchClassifier ) {
				this.foundClassifier = true;
				this.branchClassifier = false;
			}

		}

		if( !branchModules.isEmpty() ) this.moduleCache.addAll( branchModules );

	}

	/**
	 * Build all modules for the pipeline.
	 * 
	 * @return List of BioModules
	 * @throws Exception if errors occur
	 */
	public static List<BioModule> buildPipeline() throws Exception {
		if( factory == null ) initFactory();
		return factory.buildModules();
	}

	/**
	 * Get the configured modules + implicit modules if configured.
	 */
	private static List<String> getConfigModules() throws Exception {
		final List<String> configModules = Config.requireList( null, Constants.INTERNAL_BLJ_MODULE );
		final List<String> modules = new ArrayList<>();
		if( !Config.getBoolean( null, Constants.DISABLE_ADD_IMPLICIT_MODULES ) ) {
			if( modules.contains( ImportMetadata.class.getName() )) {
				info( ImportMetadata.class.getName() + " has already been placed in the module run order.");
				if (modules.indexOf( ImportMetadata.class.getName() ) != 0) {
					Log.warn(BioModuleFactory.class, ImportMetadata.class.getName() + " is typically the FIRST module in the pipeline.");
				}
			}else if( Config.getExistingFile( null, MetaUtil.META_FILE_PATH ) != null ) {
				info( "Pipeline has metadata file. Set 1st module: " + ImportMetadata.class.getName() );
				modules.add( ImportMetadata.class.getName() );
			}else if( BioLockJUtil.pipelineInputType( BioLockJUtil.PIPELINE_SEQ_INPUT_TYPE ) ) {
				info( "Pipeline has no metadata file. Metadata can be inferred from input files. Set 1st module: " + ImportMetadata.class.getName() );
				modules.add( ImportMetadata.class.getName() );
				if ( BioLockJUtil.getInputModules().size() > 0) {
					Log.warn(BioModuleFactory.class, ImportMetadata.class.getName() + "was added to the beginning of the pipeline.");
					Log.warn(BioModuleFactory.class, "The following module(s) may bring input data to the pipeline: " + BioLockJUtil.getCollectionAsString( BioLockJUtil.getInputModules() ) );
					Log.warn(BioModuleFactory.class, "That data will not be detected by " + ImportMetadata.class.getName() );
					Log.warn(BioModuleFactory.class, "To remedy this, add " + ImportMetadata.class.getName() + " in the biomodule run order after the data input module(s)." );
				}
			}

			if( SeqUtil.isMultiplexed() ) {
				info( "Set required module (for multiplexed data): " + Config.getString( null, Constants.DEFAULT_MOD_DEMUX ) );
				configModules.remove( Config.getString(null, Constants.DEFAULT_MOD_DEMUX) );
				modules.add( Config.getString(null, Constants.DEFAULT_MOD_DEMUX) );
			}

			if( Config.getBoolean( null, Constants.INTERNAL_IS_MULTI_LINE_SEQ ) ) {
				info(
					"Set required module (for multi seq-line fasta files ): " + Config.getString( null, Constants.DEFAULT_MOD_FASTA_CONV) );
				configModules.remove( Config.getString( null, Constants.DEFAULT_MOD_FASTA_CONV) );
				modules.add( Config.getString( null, Constants.DEFAULT_MOD_FASTA_CONV) );
			}
		}

		for( final String module: configModules ) {
			modules.add( module );
		}
		return modules;
	}

	private static void info( final String msg ) {
		if( !BioLockJUtil.isDirectMode() ) Log.info( BioModuleFactory.class, msg );
	}

	private static void initFactory() throws Exception {
		factory = new BioModuleFactory();
	}

	private boolean branchClassifier = false;
	private boolean foundClassifier = false;
	private List<String> moduleCache = new ArrayList<>();
	private int safteyCheck = 0;
	private static BioModuleFactory factory = null;
	private static final String MODULE_CLASSIFIER_PACKAGE = "biolockj.module.classifier";
	private static final int SAFE_MAX = 10;
}
