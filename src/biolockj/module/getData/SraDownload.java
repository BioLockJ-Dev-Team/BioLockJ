/**
 * @UNCC Fodor Lab
 * @author Philip Badzuh
 * @email pbadzuh@uncc.edu
 * @date Feb 14, 2020
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.getData;

import java.io.File;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import biolockj.api.ApiModule;
import biolockj.exception.MetadataException;
import biolockj.Config;
import biolockj.module.ScriptModuleImpl;
import biolockj.util.BioLockJUtil;
import biolockj.util.MetaUtil;
import biolockj.Constants;
import biolockj.Log;
import biolockj.Properties;

public class SraDownload extends ScriptModuleImpl implements ApiModule, InputData {

	public SraDownload() {
		super();
		addNewProperty(METADATA_SRA_ID_COL_NAME, Properties.STRING_TYPE,
				"Specifies the metadata file column name containing SRA run ids", "sra");
		addNewProperty(EXE_FASTERQ, Properties.FILE_PATH, "Optional - specifies a path to fasterq-dump");
		addGeneralProperty(Constants.EXE_GZIP, Properties.FILE_PATH, "Optional - specifies a path to gzip");
	}

	@Override
	public List<File> getInputFiles() {
		return (new ArrayList<File>());
	}

	@Override
	public List<List<String>> buildScript(List<File> files) throws Exception {

		final String outputDir = getOutputDir().getAbsolutePath();
		String sraId = null;

		final List<List<String>> data = new ArrayList<>();
		dataSource = dataSource + Constants.RETURN + "Accessions: ";
		for (final String sample : MetaUtil.getSampleIds()) {
			final ArrayList<String> lines = new ArrayList<>();
			try {
				sraId = MetaUtil.getField(sample, Config.getString(this, METADATA_SRA_ID_COL_NAME));
			} catch (MetadataException e) {
				Log.error(this.getClass(), "Could not get SRA id from metadata column named "
						+ Config.getString(this, METADATA_SRA_ID_COL_NAME) + " for sample " + sample + ".");
				throw e;
			}
			dataSource = dataSource + Constants.RETURN + sraId;
			final String downloadLine = Config.getExe(this, EXE_FASTERQ) + " -O " + outputDir + " " + sraId;
			final String compressLine = Config.getExe(this, Constants.EXE_GZIP) + " " + outputDir + File.separator
					+ sraId + "*.fastq";
			lines.add(downloadLine);
			lines.add(compressLine);
			data.add(lines);
		}

		System.out.println(data);
		return (data);

	}

	@Override
	public Boolean isValidProp(String property) throws Exception {
		Boolean isValid = super.isValidProp(property);
		switch (property) {
		case MetaUtil.META_FILE_PATH:
			try {
				Config.requireExistingFile(this, MetaUtil.META_FILE_PATH);
			} catch (Exception e) {
				isValid = false;
				Log.error(this.getClass(),
						"The " + MetaUtil.META_FILE_PATH + " configuration property is missing.");
				throw e;
			}
			isValid = true;
			break;
		case METADATA_SRA_ID_COL_NAME:
			try {
				Config.requireString(this, METADATA_SRA_ID_COL_NAME);
			} catch (Exception e) {
				isValid = false;
				Log.error(this.getClass(), "The " + METADATA_SRA_ID_COL_NAME
						+ " configuration property is missing or invalid. Must be a string.");
				throw e;
			}
			isValid = true;
			break;

		}
		return isValid;
	}

	@Override
	public void checkDependencies() throws Exception {

		isValidProp(MetaUtil.META_FILE_PATH);
		isValidProp(METADATA_SRA_ID_COL_NAME);

	}

	@Override
	public String getDescription() {
		return ("SraDownload downloads and compresses short read archive (SRA) files to fastq.gz");
	}

	@Override
	public String getDetails() {
		return ("Downloading and compressing files requires fasterq-dump and gzip. Your metadata file should "
				+ "include a column that contains SRA run accessions, and the name of this column must be "
				+ "specified in the configuration file, if named something other than 'sra'");
	}

	@Override
	public String getCitationString() {
		return ("[sra-tools](https://github.com/ncbi/sra-tools)" + System.lineSeparator() 
		+ "Module developed by Philip Badzuh" + System.lineSeparator() 
		+ "BioLockj " + BioLockJUtil.getVersion());
	}
	
	@Override
	public String getDockerImageName() {
		return "";//TODO: actually supply docker image name; this just avoids compile errors.
	}
	
	@Override
	public String getSummary() throws Exception {
		return super.getSummary() + "Data source: " + getDataSource() ;
	}
	
//	@Override
	public String getDataSource() {
		return "https://www.ncbi.nlm.nih.gov/sra" + dataSource;
	}
	@Override
	public Set<String> getInputDataTypes() {
		Set<String> types = new TreeSet<String>();
		types.add( BioLockJUtil.PIPELINE_SEQ_INPUT_TYPE );
		types.add( Constants.FASTQ );
		return types;
	}
	
	private String dataSource = "";

	private static final String METADATA_SRA_ID_COL_NAME = "sraDownload.metadataSraIdColumnName";
	private static final String EXE_FASTERQ = "exe.fasterq-dump";

}
