/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Jan 20, 2019
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.report.taxa;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import biolockj.Log;
import biolockj.module.BioModule;
import biolockj.module.JavaModule;
import biolockj.module.JavaModuleImpl;
import biolockj.util.BioLockJUtil;
import biolockj.util.TaxaUtil;

/**
 * TBD
 */
public abstract class TaxaCountModule extends JavaModuleImpl implements JavaModule
{

	@Override
	public List<File> getInputFiles() throws Exception
	{
		if( getFileCache().isEmpty() )
		{
			final List<File> files = new ArrayList<>();
			for( final File f: findModuleInputFiles() )
			{
				if( TaxaUtil.isTaxaFile( f ) )
				{
					files.add( f );
				}
			}
			cacheInputFiles( files );
		}

		return getFileCache();
	}

	/**
	 * Require taxonomy table module as prerequisite
	 */
	@Override
	public List<String> getPreRequisiteModules() throws Exception
	{
		final List<String> preReqs = new ArrayList<>();
		if( !BioLockJUtil.pipelineInputType( BioLockJUtil.PIPELINE_TAXA_COUNT_TABLE_INPUT_TYPE ) )
		{
			preReqs.add( BuildTaxaTables.class.getName() );
		}
		preReqs.addAll( super.getPreRequisiteModules() );
		return preReqs;
	}

	/**
	 * Check the module output directory for taxonomy table files generated by BioLockJ.
	 * 
	 * @param module BioModule
	 * @return TRUE if module generated taxonomy table files
	 */
	public boolean isTaxaModule( final BioModule module )
	{
		try
		{
			final Collection<File> files = BioLockJUtil.removeIgnoredAndEmptyFiles(
					FileUtils.listFiles( module.getOutputDir(), HiddenFileFilter.VISIBLE, HiddenFileFilter.VISIBLE ) );

			for( final File f: files )
			{
				if( TaxaUtil.isTaxaFile( f ) )
				{
					return true;
				}
			}
		}
		catch( final Exception ex )
		{
			Log.warn( getClass(), "Error occurred while inspecting module output files: " + module );
			ex.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean isValidInputModule( final BioModule module )
	{
		return isTaxaModule( module );
	}

}