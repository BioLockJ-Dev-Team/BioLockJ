/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date June 20, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.seq;

import java.io.File;
import java.util.*;
import biolockj.*;
import biolockj.api.ApiModule;
import biolockj.exception.SequnceFormatException;
import biolockj.module.SeqModuleImpl;
import biolockj.util.BioLockJUtil;
import biolockj.util.SeqUtil;

/**
 * This BioModule uses awk and gzip to convert input sequence files into a decompressed fasta file format.
 * 
 * @blj.web_desc Awk Fastq to Fasta Converter
 */
public class AwkFastaConverter extends SeqModuleImpl implements ApiModule {
	
	public AwkFastaConverter(){
		addGeneralProperty( Constants.EXE_GZIP );
		addGeneralProperty( Constants.EXE_AWK );
	}

	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception {
		final List<List<String>> data = new ArrayList<>();
		final boolean isMultiLine = Config.getBoolean( this, Constants.INTERNAL_IS_MULTI_LINE_SEQ );
		final String tempDir = getTempDir().getAbsolutePath() + File.separator;
		final String outDir = getOutputDir().getAbsolutePath() + File.separator;
		final String ext = "." + ( isMultiLine ? Constants.FASTA: SeqUtil.getSeqType() );

		for( final File f: files ) {
			final ArrayList<String> lines = new ArrayList<>();
			final String fileId = SeqUtil.getSampleId( f );
			final String dirExt = SeqUtil.getReadDirectionSuffix( f );

			String filePath = f.getAbsolutePath();

			if( SeqUtil.isGzipped( f.getName() ) ) {
				filePath = ( SeqUtil.isFastQ() || isMultiLine ? tempDir: outDir ) + fileId + dirExt + ext;
				lines.add( unzip( f, filePath ) );
			}

			if( Config.getBoolean( this, Constants.INTERNAL_IS_MULTI_LINE_SEQ ) )
				lines.add( convert454( filePath, fileId + dirExt, outDir ) );
			else if( SeqUtil.isFastQ() ) lines.add( convert2fastA( filePath, fileId + dirExt, outDir ) );

			if( !SeqUtil.isGzipped( f.getName() ) && SeqUtil.isFastA() ) {
				Log.warn( getClass(), "Remove this BioModule from Config:  " +
					" Files are already in decompressed FastA format!  It is unnecessary to make duplicate files..." );
				lines.add( copyToOutputDir( filePath, fileId + dirExt + ext ) );
			}

			data.add( lines );
		}

		return data;
	}

	/**
	 * Set {@link biolockj.Config}.{@value biolockj.Constants#INTERNAL_SEQ_TYPE} = {@value biolockj.Constants#FASTA}<br>
	 * Set {@link biolockj.Config}.{@value biolockj.Constants#INTERNAL_SEQ_HEADER_CHAR} =
	 * {@link biolockj.util.SeqUtil#FASTA_HEADER_DEFAULT_DELIM}
	 *
	 * @throws Exception if errors occur
	 */
	@Override
	public void cleanUp() throws Exception {
		super.cleanUp();
		Config.setConfigProperty( Constants.INTERNAL_SEQ_TYPE, Constants.FASTA );
		Config.setConfigProperty( Constants.INTERNAL_SEQ_HEADER_CHAR, SeqUtil.FASTA_HEADER_DEFAULT_DELIM );
	}

	@Override
	public List<File> getSeqFiles( final Collection<File> files ) throws SequnceFormatException {
		return SeqUtil.getSeqFiles( files );
	}

	/**
	 * This method generates the required bash functions used by the module scripts.
	 * <ul>
	 * <li>{@value #FUNCTION_CONVERT_TO_FASTA}
	 * <li>{@value #FUNCTION_GUNZIP}
	 * </ul>
	 */
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception {
		final List<String> lines = super.getWorkerScriptFunctions();
		lines.add( "function " + FUNCTION_CONVERT_TO_FASTA + "() {" );
		lines.add( "cat $1 | " + Config.getExe( this, Constants.EXE_AWK ) +
			" '{ if(NR%4==1) { printf( \">%s \\n\",substr($0,2) ); } else if(NR%4==2) print; }' > $2 " );
		lines.add( "}" + RETURN );
		if( hasGzipped() ) {
			lines.add( "function " + FUNCTION_GUNZIP + "() {" );
			lines.add( Config.getExe( this, Constants.EXE_GZIP ) + " -cd $1 > $2" );
			lines.add( "}" + RETURN );
		}

		if( Config.getBoolean( this, Constants.INTERNAL_IS_MULTI_LINE_SEQ ) ) {
			lines.add( "function " + FUNCTION_CONVERT_454 + "() {" );
			lines.add( "cat $1 | " + Config.getExe( this, Constants.EXE_AWK ) +
				" '{if(substr($0,0,1) == \">\" ) { printf(\"\\n%s\\n\", $0); } else printf $0;}' > $2 " );
			lines.add( "}" + RETURN );
		}
		return lines;
	}

	private String copyToOutputDir( final String source, final String target ) {
		return "cp " + source + " " + getOutputDir().getAbsolutePath() + File.separator + target;
	}

	private boolean hasGzipped() {
		for( final File f: getInputFiles() )
			if( SeqUtil.isGzipped( f.getName() ) ) return true;

		return false;
	}

	/**
	 * Build script line to decompress gzipped sequence file.
	 *
	 * @param file Gzipped file
	 * @param targetPath Output file name
	 * @return Bash Script line to gunzip file
	 */
	protected static String unzip( final File file, final String targetPath ) {
		return FUNCTION_GUNZIP + " " + file.getAbsolutePath() + " " + targetPath;
	}

	private static String convert2fastA( final String filePath, final String fileId, final String outDir ) {
		return FUNCTION_CONVERT_TO_FASTA + " " + filePath + " " + outDir + fileId + "." + Constants.FASTA;
	}

	private static String convert454( final String filePath, final String fileId, final String outDir ) {
		return FUNCTION_CONVERT_454 + " " + filePath + " " + outDir + fileId + "." + Constants.FASTA;
	}
	
	@Override
	public String getDockerImageOwner() {
		return Constants.MAIN_DOCKER_OWNER;
	}
	
	@Override
	public String getDockerImageName() {
		return "blj_basic";
	}
	
	@Override
	public String getDockerImageTag() {
		return "v1.3.18";
	}

	/**
	 * Name of the bash function used to conver 454 format to BioLockJ friendly Illumina format:
	 * {@value #FUNCTION_CONVERT_454}
	 */
	protected static final String FUNCTION_CONVERT_454 = "convert454";

	/**
	 * Name of the bash function that converts the file format to Fasta: {@value #FUNCTION_CONVERT_TO_FASTA}
	 */
	protected static final String FUNCTION_CONVERT_TO_FASTA = "convertToFastA";

	/**
	 * Name of the bash function used to decompress gzipped files: {@value #FUNCTION_GUNZIP}
	 */
	protected static final String FUNCTION_GUNZIP = "decompressGzip";

	@Override
	public String getDescription() {
		return "Convert fastq files into fasta format.";
	}
	
	@Override
	public String getDetails() {
		return "This module was first introduced because it was required for [QIIME](http://qiime.org).";
	}

	@Override
	public String getCitationString() {
		return "BioLockJ " + BioLockJUtil.getVersion() + System.lineSeparator() + "Module developed by Mike Sioda";
	}

}
