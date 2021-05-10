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
package biolockj.module.getData.sra;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.io.FileUtils;
import biolockj.api.ApiModule;
import biolockj.exception.ConfigNotFoundException;
import biolockj.exception.ConfigPathException;
import biolockj.exception.DockerVolCreationException;
import biolockj.exception.MetadataException;
import biolockj.Config;
import biolockj.module.OutsidePipelineWriter;
import biolockj.module.getData.InputDataModule;
import biolockj.util.BioLockJUtil;
import biolockj.util.DockerUtil;
import biolockj.util.MetaUtil;
import biolockj.util.SeqUtil;
import biolockj.Constants;
import biolockj.Log;
import biolockj.Properties;

public class SraDownload extends SequenceReadArchive implements ApiModule, InputDataModule, OutsidePipelineWriter {

	public SraDownload() {
		super();
		addNewProperty(METADATA_SRA_ID_COL_NAME, Properties.STRING_TYPE,
				"Specifies the metadata file column name containing SRA run ids");
		addNewProperty(DEST_DIR, Properties.FILE_PATH, "Path to directory where downloaded files should be saved. If specified, it must exist.");
		addNewProperty( SRP, Properties.LIST_TYPE, SRP_DESC);
		addNewProperty( SRA_ACC_LIST, Properties.FILE_PATH, SRA_ACC_LIST_DESC );
		addGeneralProperty(EXE_FASTERQ);
		addGeneralProperty(Constants.EXE_GZIP);
		addGeneralProperty( MetaUtil.META_FILE_PATH );
	}

	@Override
	public List<File> getInputFiles() {
		return (new ArrayList<File>());
	}

	@Override
	public List<List<String>> buildScript(List<File> files) throws Exception {

		final String outputDir = getDestDir().getAbsolutePath();
		
		List<String> srrList = getSRRs();

		final List<List<String>> data = new ArrayList<>();
		dataSource = dataSource + Constants.RETURN + "Accessions: ";
		for (final String srr : srrList) { 
			final ArrayList<String> lines = new ArrayList<>();
			String existingFile = existingFileInfo(srr);
			if (existingFile.length() > 0) {
				dataSource = dataSource + Constants.RETURN + srr + " - keeping prexisting file: " + existingFile;
				Log.info(SraDownload.class, "Skipping " + srr + " because [" + existingFile + "] already exists in destination [" + outputDir + "].");
			}else {
				dataSource = dataSource + Constants.RETURN + srr + " (download)";
				final String downloadLine = Config.getExe(this, EXE_FASTERQ) + " -O " + outputDir + " " + srr;
				final String compressLine = Config.getExe(this, Constants.EXE_GZIP) + " " + outputDir + File.separator
						+ srr + "*.fastq";
				lines.add(downloadLine);
				lines.add(compressLine);
				data.add(lines);
			}
		}
		if ( data.isEmpty() ) {
			final ArrayList<String> fallback_lines = new ArrayList<>();
			fallback_lines.add( "ls -lh " + outputDir );
			data.add( fallback_lines );
		}

		return (data);
	}
	
	private File getDestDir() throws ConfigPathException, DockerVolCreationException {
		File dest;
		if ( Config.getExistingDir( this, DEST_DIR ) != null ) {
			dest = Config.getExistingDir( this, DEST_DIR );
		}else {
			dest = getOutputDir();
		}
		return dest;
	}
	
	private List<String> getSRRs() throws IOException, ConfigPathException, ConfigNotFoundException, DockerVolCreationException, MetadataException {
		List<String> list = new ArrayList<>();
		if ( useMetadataColumn() ) {
			for (final String sample : MetaUtil.getSampleIds()) { 
				try {
					list.add( MetaUtil.getField(sample, Config.getString(this, METADATA_SRA_ID_COL_NAME)) );
				} catch (MetadataException e) {
					Log.error(this.getClass(), "Could not get SRA id from metadata column named "
							+ Config.getString(this, METADATA_SRA_ID_COL_NAME) + " for sample " + sample + ".");
					throw e;
				}
			}	
		}else {
			Log.info(SraDownload.class, "Cannot get accessions from column \"" + Config.getString(this, METADATA_SRA_ID_COL_NAME) + "\" in metadata.");
			File accList = Config.requireExistingFile( this, SRA_ACC_LIST );
			FileUtils.copyFileToDirectory( accList, getTempDir() );
			BufferedReader reader = new BufferedReader( new FileReader( accList ) );
			try {
				for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
					if (!line.isEmpty()) list.add( line );
				}
			} finally {
				reader.close();
			}
		}
		return list;
	}
	
	private boolean useMetadataColumn() {
		return Config.getString( this, METADATA_SRA_ID_COL_NAME ) != null ;
	}
	
	@Override
	public List<String> getPreRequisiteModules() throws Exception {
		List<String> preReqs = super.getPreRequisiteModules();
		if ( useMetadataColumn() ) {
			Log.info(SraDownload.class, "Sequences will be downloaded for each accession given in the metadata under the column \"" 
							+ Config.getString( this, METADATA_SRA_ID_COL_NAME ) + "\".");
		}else if (Config.getExistingFile( this, SRA_ACC_LIST ) != null) {
			Log.info(SraDownload.class, "Sequences will be downloaded for each accession given in the file [" 
							+ Config.getString( this, SRA_ACC_LIST ) + "].");
		}else if ( Config.getString( this, SRP ) != null && isValidProp( SRP )) {
			preReqs.add( SrpSrrConverter.class.getName() );
		}
		return preReqs;
	}
	
	private String existingFileInfo( String sraId ) throws ConfigPathException, DockerVolCreationException {
		File dest = getDestDir();
		File[] files = dest.listFiles( new FilenameFilter() {
			@Override
			public boolean accept( File dir, String name ) {
				if ( !dir.equals( dest )) return false;
				if (name.startsWith( sraId )) {
					if ( name.endsWith( Constants.FASTQ ) || name.endsWith( Constants.FASTQ + ".gz") ) {
						return true;
					}
				}
				return false;
			}
		} );
		String returnVal = "";
		for (File file : files) {
			if ( returnVal.length() > 0 ) returnVal += ", ";
			returnVal += DockerUtil.deContainerizePath( file.getName() );
		}
		return returnVal;
	}

	@Override
	public Boolean isValidProp(String property) throws Exception {
		Boolean isValid = super.isValidProp(property);
		switch (property) {
		case METADATA_SRA_ID_COL_NAME:
			if (Config.getString(this, METADATA_SRA_ID_COL_NAME) != null) {
					Config.requireExistingFile( null, MetaUtil.META_FILE_PATH );
					if( MetaUtil.getFieldValues( Config.getString( this, METADATA_SRA_ID_COL_NAME ), true )
									.isEmpty() ) {
						throw new MetadataException( "No accession IDs in metadata column \"" +
							Config.getString( this, METADATA_SRA_ID_COL_NAME ) + "\"." );
					}
			}
			isValid = true;
			break;
		case DEST_DIR:
			Config.getExistingDir( this, DEST_DIR );
			isValid = true;
			break;
		}
		return isValid;
	}

	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		Config.getExistingFile(null, MetaUtil.META_FILE_PATH);
		isValidProp(METADATA_SRA_ID_COL_NAME);
		isValidProp(DEST_DIR);
		if (useMetadataColumn()) isValidProp(METADATA_SRA_ID_COL_NAME);
	}

	@Override
	public String getDescription() {
		return ("SraDownload downloads and compresses short read archive (SRA) files to fastq.gz");
	}

	@Override
	public String getDetails() {
		return ("Downloading and compressing files requires fasterq-dump and gzip." 
				+ "The accessions to download can be specified using any ONE of the following:<br>"
				+ " 1. A metadata file (given by *" + MetaUtil.META_FILE_PATH + "* that has column *" + METADATA_SRA_ID_COL_NAME + "*.<br>"
				+ " 2. *" + SRP + "*, OR <br>"
				+ " 3. *" + SRA_ACC_LIST + "*<br>"
				+ System.lineSeparator() 
				+ "*" + DEST_DIR + "* gives an external directory that can be shared across pipelines. " 
				+ "This is recommended. If it is not specified, the files will be downlaoded to this modules output directory. <br>"
				+ System.lineSeparator() 
				+ "Suggested: " + Constants.INPUT_DIRS + " = ${" + DEST_DIR + "}<br>"
				+ System.lineSeparator() 
				+ "Typically, BioLockJ will automatically determine modules to add to the pipeline to process sequence data. " 
				+ "If the files are not present on the system when the pipeline starts, then it is up to the user to configure any and all sequence processing modules.");
	}

	@Override
	public String getCitationString() {
		return ("[sra-tools](https://github.com/ncbi/sra-tools)" + System.lineSeparator() 
		+ "Module developed by Philip Badzuh" + System.lineSeparator() 
		+ "BioLockJ " + BioLockJUtil.getVersion());
	}
	
	@Override
	public String getDockerImageOwner() {
		return Constants.MAIN_DOCKER_OWNER;
	}
	
	@Override
	public String getDockerImageName() {
		return "sratoolkit";
	}
	
	@Override
	public String getDockerImageTag() {
		return "v1.3.18";
	}
	
	@Override
	public String getSummary() throws Exception {
		return super.getSummary() + "Data source: " + getDataSource() + Constants.RETURN 
						+ "Files saved to: " + DockerUtil.deContainerizePath( getDestDir().getAbsolutePath() );
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
	
	@Override
	public void cleanUp() throws Exception {
		if ( BioLockJUtil.getInputDirs().contains( getDestDir() )) {
			Log.info(SraDownload.class, "Initialize SeqUtil now that sequences have been downloaded.");
			SeqUtil.initialize();
		}
		super.cleanUp();
	}
	
	@Override
	public Set<String> getWriteDirs() throws DockerVolCreationException, ConfigPathException {
		Set<String> dirs = new TreeSet<>();
		dirs.add( getDestDir().getAbsolutePath() );
		return dirs;
	}
	
	private String dataSource = "";

	public static final String METADATA_SRA_ID_COL_NAME = "sra.accessionIdColumn";
	public static final String DEST_DIR = "sra.destinationDir";
	public static final String EXE_FASTERQ = "exe.fasterq-dump";

}
