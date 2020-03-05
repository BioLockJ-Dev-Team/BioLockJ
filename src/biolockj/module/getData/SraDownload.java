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

import biolockj.api.ApiModule;
import biolockj.exception.MetadataException;
import biolockj.Config;
import biolockj.module.ScriptModuleImpl;
import biolockj.util.BioLockJUtil;
import biolockj.util.MetaUtil;
import biolockj.Constants;
import biolockj.Log;
import biolockj.Properties;

public class SraDownload extends ScriptModuleImpl implements ApiModule {

	public SraDownload() {
		super();
		addNewProperty(METADATA_SRA_ID_COL_NAME, Properties.STRING_TYPE,
				"Specifies the metadata file column name containing SRA run ids", "sra");
		addGeneralProperty(Constants.INPUT_DIRS,
				"Specifies a path to dummy seq data e.g. $SHEP/data_tiny/input/seq/fq/single_sample/separate_fw_rv/rhizosphere_16S_data/R1/rhizo_R1_subdir");
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
		for (final String sample : MetaUtil.getSampleIds()) {
			final ArrayList<String> lines = new ArrayList<>();
			try {
				sraId = MetaUtil.getField(sample, Config.getString(this, METADATA_SRA_ID_COL_NAME));
			} catch (MetadataException e) {
				Log.error(this.getClass(), "Could not get SRA id from metadata column named "
						+ Config.getString(this, METADATA_SRA_ID_COL_NAME) + " for sample " + sample + ".");
				throw e;
			}
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
		return ("Module developed by Philip Badzuh" + System.lineSeparator() + "BioLockj " + BioLockJUtil.getVersion());
	}

	private static final String METADATA_SRA_ID_COL_NAME = "sraDownload.metadataSraIdColumnName";
	private static final String EXE_FASTERQ = "exe.fasterq-dump";
}
