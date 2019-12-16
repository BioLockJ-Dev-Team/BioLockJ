/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 02, 2019
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj;

/**
 * Single Java class to hold shared constant values referenced my multiple classes.
 */
public class Constants {

	/**
	 * Captures the application start time
	 */
	public static final long APP_START_TIME = System.currentTimeMillis();
	
	/**
	 * The key string to define an alias for an individual module: {@value 
	 */
	public static final String ASSIGN_ALIAS = " AS ";

	/**
	 * {@link biolockj.Config} Integer property: {@value #AWS_S3_XFER_TIMEOUT}<br>
	 * Set the max number of minutes to allow for S3 transfers to complete.
	 */
	public static final String AWS_S3_XFER_TIMEOUT = "aws.s3TransferTimeout";

	/**
	 * Bash profile fo;e name: {@value #BASH_PROFILE}
	 */
	public static final String BASH_PROFILE = ".bash_profile";

	/**
	 * Bashrc file name: {@value #BASH_RC}
	 */
	public static final String BASH_RC = ".bashrc";

	/**
	 * Name of the file created in the BioModule or {@value #INTERNAL_PIPELINE_DIR} root directory to indicate execution
	 * was successful: {@value #BLJ_COMPLETE}
	 */
	public static final String BLJ_COMPLETE = "biolockjComplete";

	/**
	 * Name of the file created in the {@value #INTERNAL_PIPELINE_DIR} root directory to indicate fatal application
	 * errors halted execution: {@value #BLJ_FAILED}
	 */
	public static final String BLJ_FAILED = "biolockjFailed";

	/**
	 * Set "#BioModule" tag in {@link biolockj.Config} file to include in pipeline: {@value #BLJ_MODULE_TAG}<br>
	 * Example: #BioModule biolockj.module.ImportMetadata
	 */
	public static final String BLJ_MODULE_TAG = "#BioModule";

	/**
	 * Name of the file created in the BioModule root directory to indicate execution has started: {@value #BLJ_STARTED}
	 */
	public static final String BLJ_STARTED = "biolockjStarted";

	/**
	 * URL to the BioLockJ WIKI
	 */
	public static final String BLJ_WIKI = "https://github.com/msioda/BioLockJ/wiki";

	/**
	 * {@link biolockj.Config} option for {@value #REPORT_TAXONOMY_LEVELS}: {@value #CLASS}
	 */
	public static final String CLASS = "class";

	/**
	 * {@link biolockj.Config} String property: {@value #CLUSTER_HOST}<br>
	 * The remote cluster host URL (used for ssh, scp, rsync, etc)
	 */
	public static final String CLUSTER_HOST = "cluster.host";

	/**
	 * {@link biolockj.Config} String property: Java class name for default module used to demultiplex data:
	 * {@value #DEFAULT_MOD_DEMUX}
	 */
	public static final String DEFAULT_MOD_DEMUX = "pipeline.defaultDemultiplexer";

	/**
	 * {@link biolockj.Config} String property: Java class name for default module used to convert files into fasta:
	 * {@value #DEFAULT_MOD_FASTA_CONV} format
	 */
	public static final String DEFAULT_MOD_FASTA_CONV = "pipeline.defaultFastaConverter";

	/**
	 * {@link biolockj.Config} String property: Java class name for default module used combined paired read files:
	 * {@value #DEFAULT_MOD_SEQ_MERGER}
	 */
	public static final String DEFAULT_MOD_SEQ_MERGER = "pipeline.defaultSeqMerger";

	/**
	 * {@link biolockj.Config} String property: Java class name for default module used generate p-value and other
	 * stats: {@value #DEFAULT_STATS_MODULE}
	 */
	public static final String DEFAULT_STATS_MODULE = "pipeline.defaultStatsModule";

	/**
	 * In an otu string for multiple levels, each separated by {@value #OTU_SEPARATOR}, each otu has a level prefix
	 * ending with {@value #DELIM_SEP}
	 */
	public static final String DELIM_SEP = "__";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #DETACH_JAVA_MODULES}<br>
	 * If {@value biolockj.Constants#TRUE} Java modules do not run with main BioLockJ Java application. Instead they run
	 * on compute nodes on the CLUSTER or AWS environments.
	 */
	public static final String DETACH_JAVA_MODULES = "pipeline.detachJavaModules";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #DISABLE_ADD_IMPLICIT_MODULES}<br>
	 * If set to {@value #TRUE}, implicit modules will not be added to the pipeline.
	 */
	public static final String DISABLE_ADD_IMPLICIT_MODULES = "pipeline.disableAddImplicitModules";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #DISABLE_PRE_REQ_MODULES}<br>
	 * If set to {@value #TRUE}, prerequisite modules will not be added to the pipeline.
	 */
	public static final String DISABLE_PRE_REQ_MODULES = "pipeline.disableAddPreReqModules";

	/**
	 * Default Docker {@link biolockj.Config} file imported after {@value #STANDARD_CONFIG_PATH} (if files exist)
	 */
	public static final String DOCKER_CONFIG_PATH = "${BLJ}/resources/config/default/docker.properties";

	/**
	 * {@link biolockj.Config} property for GenMod to set its Docker container name: {@value #DOCKER_CONTAINER_NAME}
	 */
	public static final String DOCKER_CONTAINER_NAME = "genMod.dockerContainerName";

	/**
	 * {@link biolockj.Config} option for {@value #REPORT_TAXONOMY_LEVELS}: {@value #DOMAIN}
	 */
	public static final String DOMAIN = "domain";

	/**
	 * Prefix used in several {@link biolockj.Config} String properties. {@value #EXE_PREFIX}<br>
	 * These properties are used to give the path to executables. Unlike most file path properties, they are not
	 * converted from host-path style when using a docker container; thus, an "{@value #EXE_PREFIX}" property can be
	 * used to specify an executable within a docker container.
	 * In contrast, the "hostExe." prefix is used the same way, but it is translated to allow an in-container script to use the host file path specified.
	 */
	public static final String EXE_PREFIX = "exe.";
	
	/**
	 * {@link biolockj.Config} String property: {@value #EXE_AWK}<br>
	 * Set command line executable awk.
	 */
	public static final String EXE_AWK = "exe.awk";

	/**
	 * {@link biolockj.Config} String property {@value #EXE_DOCKER}<br>
	 * Set command line executable docker
	 */
	public static final String EXE_DOCKER = "exe.docker";

	/**
	 * {@link biolockj.Config} String property {@value #EXE_GZIP}<br>
	 * Set command line executable gzip
	 */
	public static final String EXE_GZIP = "exe.gzip";

	/**
	 * {@link biolockj.Config} property for java executable: {@value #EXE_JAVA}
	 */
	public static final String EXE_JAVA = "exe.java";

	/**
	 * {@link biolockj.Config} property to python executable
	 */
	public static final String EXE_PYTHON = "exe.python";

	/**
	 * Boolean {@link biolockj.Config} property value option: {@value #FALSE}
	 */
	public static final String FALSE = "N";

	/**
	 * {@link biolockj.Config} option for {@value #REPORT_TAXONOMY_LEVELS}: {@value #FAMILY}
	 */
	public static final String FAMILY = "family";

	/**
	 * File extension for fasta files = {@value #FASTA}
	 */
	public static final String FASTA = "fasta";

	/**
	 * File extension for fastq files: {@value #FASTQ}
	 */
	public static final String FASTQ = "fastq";
	/**
	 * {@link biolockj.Config} option for {@value #REPORT_TAXONOMY_LEVELS}: {@value #GENUS}
	 */
	public static final String GENUS = "genus";

	/**
	 * Gzip compressed file extension: {@value #GZIP_EXT}
	 */
	public static final String GZIP_EXT = ".gz";

	/**
	 * Argument to print help menu: {@value #HELP}
	 */
	public static final String HELP = "-help";
	
	/**
	 * Prefix used in several {@link biolockj.Config} String properties. {@value #HOST_EXE_PREFIX}<br>
	 * @see #EXE_PREFIX
	 */
	public static final String HOST_EXE_PREFIX = "hostExe.";

	/**
	 * {@link biolockj.Config} Boolean property to disable HumanN2 Gene Family report:
	 * {@value #HN2_DISABLE_GENE_FAMILIES}
	 */
	public static final String HN2_DISABLE_GENE_FAMILIES = "humann2.disableGeneFamilies";

	/**
	 * {@link biolockj.Config} Boolean property to disable HumanN2 Pathway Abundance report:
	 * {@value #HN2_DISABLE_PATH_ABUNDANCE}
	 */
	public static final String HN2_DISABLE_PATH_ABUNDANCE = "humann2.disablePathAbundance";

	/**
	 * {@link biolockj.Config} Boolean property to disable HumanN2 Pathway Coverage report:
	 * {@value #HN2_DISABLE_PATH_COVERAGE}
	 */
	public static final String HN2_DISABLE_PATH_COVERAGE = "humann2.disablePathCoverage";

	/**
	 * HumanN2 file suffix identifier for Gene Family Summary report: {@value #HN2_GENE_FAM_SUM}
	 */
	public static final String HN2_GENE_FAM_SUM = "geneFam";

	/**
	 * HumanN2 file suffix identifier for Pathway Abundance Summary report: {@value #HN2_PATH_ABUND_SUM}
	 */
	public static final String HN2_PATH_ABUND_SUM = "pAbund";

	/**
	 * HumanN2 file suffix identifier for Pathway Coverage Summary report: {@value #HN2_PATH_COVG_SUM}
	 */
	public static final String HN2_PATH_COVG_SUM = "pCovg";

	/**
	 * HumanN2 meta column to store the total pathway count/sample: {@value #HN2_TOTAL_PATH_COUNT}
	 */
	public static final String HN2_TOTAL_PATH_COUNT = "Total_Pathways";

	/**
	 * HumanN2 meta column to store the unique pathway count/sample: {@value #HN2_TOTAL_PATH_COUNT}
	 */
	public static final String HN2_UNIQUE_PATH_COUNT = "Unique_Pathways";

	/**
	 * Standard indent = 4 spaces.
	 */
	public static final String INDENT = "    ";

	/**
	 * {@link biolockj.Config} List property: {@value #INPUT_DIRS}<br>
	 * Set sequence file directories
	 */
	public static final String INPUT_DIRS = "input.dirPaths";
	/**
	 * {@link biolockj.Config} String property: {@value #INPUT_FORWARD_READ_SUFFIX}<br>
	 * Set file suffix used to identify forward reads in {@value #INPUT_DIRS}
	 */
	public static final String INPUT_FORWARD_READ_SUFFIX = "input.suffixFw";

	/**
	 * {@link biolockj.Config} List property: {@value #INPUT_IGNORE_FILES}<br>
	 * Set file names to ignore if found in {@value #INPUT_DIRS}
	 */
	public static final String INPUT_IGNORE_FILES = "input.ignoreFiles";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #INPUT_REQUIRE_COMPLETE_PAIRS}<br>
	 * Require 100% sequence input files are matching paired reads
	 */
	public static final String INPUT_REQUIRE_COMPLETE_PAIRS = "input.requireCompletePairs";

	/**
	 * {@link biolockj.Config} String property: {@value #INPUT_REVERSE_READ_SUFFIX}<br>
	 * Set file suffix used to identify forward reads in {@value #INPUT_DIRS}
	 */
	public static final String INPUT_REVERSE_READ_SUFFIX = "input.suffixRv";

	/**
	 * {@link biolockj.Config} String property: {@value #INPUT_TRIM_PREFIX}<br>
	 * Set value of prefix to trim from sequence file names or headers to obtain Sample ID.
	 */
	public static final String INPUT_TRIM_PREFIX = "input.trimPrefix";

	/**
	 * {@link biolockj.Config} property {@value #INPUT_TRIM_SEQ_FILE} defines the file path to the file that defines the
	 * primers as regular expressions.
	 */
	public static final String INPUT_TRIM_SEQ_FILE = "trimPrimers.filePath";

	/**
	 * {@link biolockj.Config} String property: {@value #INPUT_TRIM_SUFFIX}<br>
	 * Set value of suffix to trim from sequence file names or headers to obtain Sample ID.
	 */
	public static final String INPUT_TRIM_SUFFIX = "input.trimSuffix";
	
	/**
	 * Any {@link biolockj.Config} property that starts with the {@value INTERNAL_PREFIX}
	 * prefix will not be included in the final MASTER properties file.
	 */
	public static final String INTERNAL_PREFIX = "internal.";

	/**
	 * Internal {@link biolockj.Config} List property: {@value #INTERNAL_ALL_MODULES}<br>
	 * List of all configured, implicit, and pre/post-requisite modules for the pipeline.<br>
	 * Example: biolockj.module.ImportMetadata, etc.
	 */
	public static final String INTERNAL_ALL_MODULES = "internal.allModules";

	/**
	 * Internal {@link biolockj.Config} List property: {@value #INTERNAL_BLJ_MODULE}<br>
	 * List of all project config modules.<br>
	 */
	public static final String INTERNAL_BLJ_MODULE = "internal.configModules";

	/**
	 * Internal {@link biolockj.Config} List property: {@value #INTERNAL_DEFAULT_CONFIG}<br>
	 * List of all nested default config files.<br>
	 */
	public static final String INTERNAL_DEFAULT_CONFIG = "internal.defaultConfig";

	/**
	 * {@link biolockj.Config} Internal Boolean property: {@value #INTERNAL_IS_MULTI_LINE_SEQ}<br>
	 * Store TRUE if {@link biolockj.util.SeqUtil} determines input sequences are multi-line format.
	 */
	public static final String INTERNAL_IS_MULTI_LINE_SEQ = "internal.isMultiLineSeq";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #INTERNAL_MULTIPLEXED}<br>
	 * Set to true if multiplexed reads are found, set by the application runtime code.
	 */
	public static final String INTERNAL_MULTIPLEXED = "internal.multiplexed";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #INTERNAL_PAIRED_READS}<br>
	 * Set to true if paired reads are found, set by the application runtime code.
	 */
	public static final String INTERNAL_PAIRED_READS = "internal.pairedReads";

	/**
	 * {@link biolockj.Config} String property: {@value #INTERNAL_PIPELINE_DIR}<br>
	 * Stores the path of the pipeline root directory path set by the application runtime code.
	 */
	public static final String INTERNAL_PIPELINE_DIR = "internal.pipelineDir";

	/**
	 * {@link biolockj.Config} property: {@value #INTERNAL_SEQ_HEADER_CHAR}<br>
	 * The property holds the 1st character used in the sequence header for the given dataset
	 */
	public static final String INTERNAL_SEQ_HEADER_CHAR = "internal.seqHeaderChar";

	/**
	 * {@link biolockj.Config} Internal property: {@value #INTERNAL_SEQ_TYPE}<br>
	 * The sequence type requires either {@value #FASTA} or {@value #FASTQ}<br>
	 * System will auto-detect if not configured
	 */
	public static final String INTERNAL_SEQ_TYPE = "internal.seqType";

	/**
	 * Java runtime arg used to pass application jar path: {@value #JAR_ARG}
	 */
	public static final String JAR_ARG = "-jar";

	/**
	 * {@link biolockj.Config} property used to limit classes that log debug statements when
	 * {@value #LOG_LEVEL_PROPERTY}={@value biolockj.Constants#TRUE}
	 */
	public static final String LIMIT_DEBUG_CLASSES = "pipeline.limitDebugClasses";

	/**
	 * BioLockJ log file extension: {@value #LOG_EXT}
	 */
	public static final String LOG_EXT = ".log";

	/**
	 * {@link biolockj.Config} property used to set log sensitivity in
	 * <a href= "https://github.com/msioda/BioLockJ/blob/master/resources/log4j.properties?raw=true" target=
	 * "_top">log4j.properties</a><br>
	 * <i>log4j.rootLogger=${pipeline.logLevel}, file, stdout</i>
	 * <ol>
	 * <li>DEBUG - Log all messages
	 * <li>INFO - Log info, warning and error messages
	 * <li>WARN - Log warning and error messages
	 * <li>ERROR - Log error messages only
	 * </ol>
	 */
	public static final String LOG_LEVEL_PROPERTY = "pipeline.logLevel";

	/**
	 * Spacer used to improve log file readability
	 */
	public static final String LOG_SPACER = "========================================================================";

	/**
	 * Prefix added to the master Config file: {@value #MASTER_PREFIX}
	 */
	public static final String MASTER_PREFIX = "MASTER_";

	/**
	 * BioLockJ SEQ module package: {@value #MODULE_SEQ_PACKAGE}
	 */
	public static final String MODULE_SEQ_PACKAGE = "biolockj.module.seq";

	/**
	 * BioLockJ WGS Classifier module package: {@value #MODULE_WGS_CLASSIFIER_PACKAGE}
	 */
	public static final String MODULE_WGS_CLASSIFIER_PACKAGE = "biolockj.module.classifier.wgs";

	/**
	 * {@link biolockj.Config} option for {@value #REPORT_TAXONOMY_LEVELS}: {@value #ORDER}
	 */
	public static final String ORDER = "order";

	/**
	 * Included in the file name of each file output. One file per sample is output by the ParserModule.
	 */
	public static final String OTU_COUNT = "otuCount";

	/**
	 * A pipe is used to separate each taxa {@value #OTU_SEPARATOR}
	 */
	public static final String OTU_SEPARATOR = "|";

	/**
	 * QIIME OTU table prefix: {@value #OTU_TABLE_PREFIX}
	 */
	public static final String OTU_TABLE_PREFIX = "otu_table";

	/**
	 * {@link biolockj.Config} property suffix for exe.* properties, used to set optional parameters: {@value #PARAMS}
	 */
	public static final String PARAMS = "Params";

	/**
	 * BioLockJ PDF file extension: {@value #PDF_EXT}
	 */
	public static final String PDF_EXT = ".pdf";

	/**
	 * {@link biolockj.Config} option for {@value #REPORT_TAXONOMY_LEVELS}: {@value #PHYLUM}
	 */
	public static final String PHYLUM = "phylum";

	/**
	 * {@link biolockj.Config} property set to copy input files into pipeline root directory:
	 * {@value #PIPELINE_COPY_FILES}
	 */
	public static final String PIPELINE_COPY_FILES = "pipeline.copyInput";

	/**
	 * {@link biolockj.Config} String property: {@value #PIPELINE_DEFAULT_PROPS}<br>
	 * Set file path of default property file. Nested default properties are supported (so the default property file can
	 * also have a default, and so on).
	 */
	public static final String PIPELINE_DEFAULT_PROPS = "pipeline.defaultProps";

	/**
	 * {@link biolockj.Config} property to allow a free-hand description to a pipeline: {@value #PIPELINE_DESC} TODO:
	 * needs to be implemented.
	 */
	public static final String PIPELINE_DESC = "pipeline.desc";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #PIPELINE_ENV}<br>
	 * Options: {@value #PIPELINE_ENV_CLUSTER}, {@value #PIPELINE_ENV_AWS}, {@value #PIPELINE_ENV_LOCAL}
	 */
	public static final String PIPELINE_ENV = "pipeline.env";

	/**
	 * {@link biolockj.Config} option for property: {@value #PIPELINE_ENV}<br>
	 * Used to indicate running as an Amazon web service: {@value #PIPELINE_ENV_AWS}
	 */
	public static final String PIPELINE_ENV_AWS = "aws";

	/**
	 * {@link biolockj.Config} option for property: {@value #PIPELINE_ENV}<br>
	 * Used to indicate running on the cluster: {@value #PIPELINE_ENV_CLUSTER}
	 */
	public static final String PIPELINE_ENV_CLUSTER = "cluster";

	/**
	 * {@link biolockj.Config} option for property: {@value #PIPELINE_ENV}<br>
	 * Used to indicate running on a local machine (laptop, etc): {@value #PIPELINE_ENV_LOCAL}
	 */
	public static final String PIPELINE_ENV_LOCAL = "local";
	
	/**
	 * Indicator for related programs (bash code, sheepdog_testing_suite) of the pipelines path: {@value #PIPELINE_LOCATION_KEY}
	 * @sheepdog_testing_suite
	 */
	public static final String PIPELINE_LOCATION_KEY = "Pipeline root directory: ";

	/**
	 * {@link biolockj.Config} property to assign a name to a pipeline: {@value #PIPELINE_NAME} TODO: needs to be
	 * implemented.
	 */
	public static final String PIPELINE_NAME = "pipeline.name";

	/**
	 * File suffix appended to processed samples in the module output directory: {@value #PROCESSED}
	 */
	public static final String PROCESSED = "_reported" + Constants.TSV_EXT;

	/**
	 * {@link biolockj.Config} String property: {@value #PROJECT_DEFAULT_PROPS}<br>
	 * Set file path of default property file. Nested default properties are supported (so the default property file can
	 * also have a default, and so on).
	 */
	public static final String PROJECT_DEFAULT_PROPS = "project.defaultProps";
	
	/**
	 * {@link biolockj.Config} property to assign a free-hand to a project: {@value #PROJECT_DESC} TODO: needs to be
	 * implemented.
	 */
	public static final String PROJECT_DESC = "project.desc";

	/**
	 * {@link biolockj.Config} property to assign a name to a project: {@value #PROJECT_NAME} TODO: needs to be
	 * implemented.
	 */
	public static final String PROJECT_NAME = "project.name";

	/**
	 * BioLockJ properties file extension: {@value #PROPS_EXT}
	 */
	public static final String PROPS_EXT = ".properties";

	/**
	 * QIIME application: {@value #QIIME}
	 */
	public static final String QIIME = "qiime";

	/**
	 * {@link biolockj.Config} list property to calculate alpha diversity metrics.<br>
	 * For complete list of skbio.diversity.alpha options, see
	 * <a href= "http://scikit-bio.org/docs/latest/generated/skbio.diversity.alpha.html" target=
	 * "_top">http://scikit-bio.org/docs/latest/generated/skbio.diversity.alpha.html</a><br>
	 * {@value #QIIME_ALPHA_DIVERSITY_METRICS}
	 */
	public static final String QIIME_ALPHA_DIVERSITY_METRICS = "qiime.alphaMetrics";

	/**
	 * Qiime may find ambiguous taxa identified in various formats in different databases. The following accounts for
	 * Green Genes 13.8 and Silva 132: "Ambiguous_taxa", "Other", "Unassigned"
	 */
	public static final String[] QIIME_AMBIGUOUS_TAXA = { "Ambiguous_taxa", "Other", "Unassigned" };

	/**
	 * QIIME mapping file required 2nd column name
	 */
	public static final String QIIME_BARCODE_SEQ_COL = "BarcodeSequence";

	/**
	 * QIIME mapping column created by {@link biolockj.module.implicit.qiime.BuildQiimeMapping} that stores the name of
	 * the original fasta file associated with the sample: {@value #QIIME_DEMUX_COL}
	 */
	public static final String QIIME_DEMUX_COL = "BioLockJFileName";

	/**
	 * QIIME mapping file required name of last column
	 */
	public static final String QIIME_DESC_COL = "Description";

	/**
	 * QIIME mapping file required 3rd column name
	 */
	public static final String QIIME_LINKER_PRIMER_SEQ_COL = "LinkerPrimerSequence";

	/**
	 * File extension of BioLockJ generated R Scripts: {@value #R_EXT}
	 */
	public static final String R_EXT = ".R";

	/**
	 * {@link biolockj.Config} Boolean property to signal R scripts to build HumanN2 reports
	 */
	public static final String R_INTERNAL_RUN_HN2 = "R_internal.runHumann2";

	/**
	 * {@link biolockj.Config} Boolean property to disable fold change plots: {@value #R_PLOT_EFFECT_SIZE_DISABLE_FC}
	 */
	public static final String R_PLOT_EFFECT_SIZE_DISABLE_FC = "r_PlotEffectSize.disableFoldChange";

	/**
	 * {@link biolockj.Config} String property: {@value #RDP_THRESHOLD_SCORE}<br>
	 * RdpParser will ignore OTU assignments below the threshold score (0-100)
	 */
	public static final String RDP_THRESHOLD_SCORE = "rdp.minThresholdScore";

	/**
	 * {@link biolockj.Config} String property: {@value #REPORT_LOG_BASE}<br>
	 * Required to be set to "e" or "10" to build log normalized reports.
	 */
	public static final String REPORT_LOG_BASE = "report.logBase";

	/**
	 * {@link biolockj.Config} Positive Integer property {@value #REPORT_MIN_COUNT} defines the minimum table count
	 * allowed, if a count less that this value is found, it is set to 0.
	 */
	public static final String REPORT_MIN_COUNT = "report.minCount";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #REPORT_NUM_HITS}<br>
	 * If set to {@value #TRUE}, NUM_OTUS will be added to metadata file by
	 * {@link biolockj.module.implicit.parser.ParserModuleImpl} and included in R reports
	 */
	public static final String REPORT_NUM_HITS = "report.numHits";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #REPORT_NUM_READS}<br>
	 * If set to {@value #TRUE} and NUM_READS exists in metadata file, NUM_READS will be included in the R reports
	 */
	public static final String REPORT_NUM_READS = "report.numReads";

	/**
	 * {@link biolockj.Config} Positive Double property {@value #REPORT_SAMPLE_CUTOFF} defines minimum percentage of
	 * data columns must be non-zero to keep the sample.
	 */
	public static final String REPORT_SAMPLE_CUTOFF = "report.scarceSampleCutoff";

	/**
	 * {@link biolockj.Config} Positive Double property {@value #REPORT_SCARCE_CUTOFF} defines minimum percentage of
	 * samples that must contain a count value for it to be kept.
	 */
	public static final String REPORT_SCARCE_CUTOFF = "report.scarceCountCutoff";

	/**
	 * {@link biolockj.Config} List property: {@value #REPORT_TAXONOMY_LEVELS}<br>
	 * This property drives a lot of BioLockJ functionality and determines which taxonomy-levels are reported. Note,
	 * some classifiers do not identify {@value #SPECIES} level OTUs.<br>
	 * Options = {@value #DOMAIN}, {@value #PHYLUM}, {@value #CLASS}, {@value #ORDER}, {@value #FAMILY},
	 * {@value #GENUS}, {@value #SPECIES}
	 */
	public static final String REPORT_TAXONOMY_LEVELS = "report.taxonomyLevels";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #REPORT_UNCLASSIFIED_TAXA}<br>
	 * Set the max number of minutes to allow for S3 transfers to complete.
	 */
	public static final String REPORT_UNCLASSIFIED_TAXA = "report.unclassifiedTaxa";

	/**
	 * Return character: *backslash-n*
	 */
	public static final String RETURN = "\n";

	/**
	 * {@link biolockj.Config} property set to delete {@link biolockj.module.BioModule#getTempDir()} files:
	 * {@value #RM_TEMP_FILES}
	 */
	public static final String RM_TEMP_FILES = "pipeline.deleteTempFiles";

	/**
	 * Rscript exe commmand.
	 */
	public static final String RSCRIPT = "Rscript";

	/**
	 * {@link biolockj.Config} List property: {@value #SCRIPT_DEFAULT_HEADER}<br>
	 * Store default script header for MAIN script and locally run WORKER scripts.
	 */
	public static final String SCRIPT_DEFAULT_HEADER = "script.defaultHeader";

	/**
	 * Name of the script sub-directory: {@value #SCRIPT_DIR}
	 */
	public static final String SCRIPT_DIR = "script";

	/**
	 * File suffix appended to failed scripts: {@value #SCRIPT_FAILURES}
	 */
	public static final String SCRIPT_FAILURES = "Failures";

	/**
	 * {@link biolockj.Config} Integer property: {@value #SCRIPT_NUM_THREADS}<br>
	 * Used to reserve cluster resources and passed to any external application call that accepts a numThreads
	 * parameter.
	 */
	public static final String SCRIPT_NUM_THREADS = "script.numThreads";

	/**
	 * {@link biolockj.Config} Integer property: {@value #SCRIPT_NUM_WORKERS}<br>
	 * Set number of samples to process per script (if parallel processing)
	 */
	public static final String SCRIPT_NUM_WORKERS = "script.numWorkers";

	/**
	 * {@link biolockj.Config} String property: {@value #SCRIPT_PERMISSIONS}<br>
	 * Used as chmod permission parameter (ex: 774)
	 */
	public static final String SCRIPT_PERMISSIONS = "script.permissions";

	/**
	 * File suffix appended to started script: {@value #SCRIPT_STARTED}
	 */
	public static final String SCRIPT_STARTED = "Started";

	/**
	 * File suffix appended to successful scripts: {@value #SCRIPT_SUCCESS}
	 */
	public static final String SCRIPT_SUCCESS = "Success";

	/**
	 * {@link biolockj.Config} Integer property: {@value #SCRIPT_TIMEOUT}<br>
	 * Sets # of minutes before worker scripts times out.
	 */
	public static final String SCRIPT_TIMEOUT = "script.timeout";

	/**
	 * {@link biolockj.Config} property {@value #SET_SEED} set the seed for a random process. Must be positive integer.
	 */
	public static final String SET_SEED = "pipeline.setSeed";

	/**
	 * BioLockJ shell script file extension: {@value #SH_EXT}
	 */
	public static final String SH_EXT = ".sh";

	/**
	 * {@link biolockj.Config} option for {@value #REPORT_TAXONOMY_LEVELS}: {@value #SPECIES}
	 */
	public static final String SPECIES = "species";

	/**
	 * Default {@link biolockj.Config} imported for all pipelines (if file exists)
	 */
	public static final String STANDARD_CONFIG_PATH = "${BLJ}/resources/config/default/standard.properties";

	/**
	 * BioLockJ tab character: {@value #TAB_DELIM}
	 */
	public static final String TAB_DELIM = "\t";

	/**
	 * Boolean {@link biolockj.Config} property value option: {@value #TRUE}
	 */
	public static final String TRUE = "Y";

	/**
	 * BioLockJ tab delimited text file extension: {@value #TSV_EXT}
	 */
	public static final String TSV_EXT = ".tsv";

	/**
	 * BioLockJ standard text file extension: {@value #TXT_EXT}
	 */
	public static final String TXT_EXT = ".txt";

	/**
	 * Unclassified taxa prefix: {@value #UNCLASSIFIED}
	 */
	public static final String UNCLASSIFIED = "Unclassified";

	/**
	 * {@link biolockj.Config} File property: {@value #USER_PROFILE}<br>
	 * Bash profile - may be ~/.bash_profile or ~/.bashrc or others
	 */
	public static final String USER_PROFILE = "pipeline.userProfile";

	/**
	 * BioLockJ main() runtime arg used to print version info: {@value #VERSION}
	 */
	public static final String VERSION = "-version";
	
	/**
	 * String used by sheepdog_testing_suite to assert that validation is enabled: {@value #VALIDATION_ENABLED}.
	 */
	public static final String VALIDATION_ENABLED = "This pipeline has validation enabled.";

	/**
	 * {@link biolockj.Config} property to define permission settings when running chmod on pipeline root dir:
	 * {@value #PIPELINE_PRIVS}
	 */
	protected static final String PIPELINE_PRIVS = "pipeline.permissions";
}
