
### [cluster](Cluster.md)                    
| Property| Description |
| :--- | :--- |
| *cluster.batchCommand* | _string_ <br>Terminal command used to submit jobs on the cluster<br>*default:*  *null* |
| *cluster.host* | _string_ <br>The remote cluster host URL (used for ssh, scp, rsync, etc)<br>*default:*  *null* |
| *cluster.jobHeader* | _string_ <br>Header written at top of worker scripts<br>*default:*  *null* |
| *cluster.modules* | _list_ <br>List of cluster modules to load at start of worker scripts<br>*default:*  *null* |
| *cluster.prologue* | _string_ <br>To run at the start of every script after loading cluster modules (if any)<br>*default:*  *null* |
| *cluster.statusCommand* | _string_ <br>Terminal command used to check the status of jobs on the cluster<br>*default:*  *null* |

### [docker](Docker.md)                    
| Property| Description |
| :--- | :--- |
| *docker.imageName* | _string_ <br>The name of a docker image to override whatever a module says to use.<br>*default:*  *null* |
| *docker.imageTag* | _string_ <br>indicate specific version of Docker images<br>*default:*  *null* |
| *docker.imgOwner* | _string_ <br>name of the Docker Hub user that owns the docker containers<br>*default:*  *null* |
| *docker.saveContainerOnExit* | _boolean_ <br>if ture, docker run command will NOT include the --rm flag<br>*default:*  *null* |

### [exe](../../Configuration/#exe-properties)                    
| Property| Description |
| :--- | :--- |
| *exe.Rscript* | _executable_ <br>Path for the "Rscript" executable; if not supplied, any script that needs the Rscript command will assume it is on the PATH.<br>*default:*  *null* |
| *exe.awk* | _executable_ <br>Path for the "awk" executable; if not supplied, any script that needs the awk command will assume it is on the PATH.<br>*default:*  *null* |
| *exe.docker* | _executable_ <br>Path for the "docker" executable; if not supplied, any script that needs the docker command will assume it is on the PATH.<br>*default:*  *null* |
| *exe.gzip* | _executable_ <br>Path for the "gzip" executable; if not supplied, any script that needs the gzip command will assume it is on the PATH.<br>*default:*  *null* |
| *exe.java* | _executable_ <br>Path for the "java" executable; if not supplied, any script that needs the java command will assume it is on the PATH.<br>*default:*  *null* |
| *exe.python* | _executable_ <br>Path for the "python" executable; if not supplied, any script that needs the python command will assume it is on the PATH.<br>*default:*  *null* |

### humann2                   
| Property| Description |
| :--- | :--- |
| *humann2.disableGeneFamilies* | _boolean_ <br>disable HumanN2 Gene Family report<br>*default:*  *null* |
| *humann2.disablePathAbundance* | _boolean_ <br>disable HumanN2 Pathway Abundance report<br>*default:*  *null* |
| *humann2.disablePathCoverage* | _boolean_ <br>disable HumanN2 Pathway Coverage report<br>*default:*  *null* |

### [input](Input.md)                    
| Property| Description |
| :--- | :--- |
| *input.dirPaths* | _list of file paths_ <br>List of one or more directories containing the pipeline input data.<br>*default:*  *null* |
| *input.ignoreFiles* | _list_ <br>file names to ignore if found in input directories<br>*default:*  *null* |
| *input.requireCompletePairs* | _boolean_ <br>Require all sequence input files have matching paired reads<br>*default:*  Y |
| *input.suffixFw* | _regex_ <br>file suffix used to identify forward reads ininput.dirPaths<br>*default:*  _R1 |
| *input.suffixRv* | _regex_ <br>file suffix used to identify reverse reads ininput.dirPaths<br>*default:*  _R2 |
| *input.trimPrefix* | _string_ <br>prefix to trim from sequence file names or headers to obtain Sample ID<br>*default:*  *null* |
| *input.trimSuffix* | _string_ <br>suffix to trim from sequence file names or headers to obtain Sample ID<br>*default:*  *null* |

### [metadata](Metadata.md)                    
| Property| Description |
| :--- | :--- |
| *metadata.barcodeColumn* | _string_ <br>metadata column with identifying barcodes<br>*default:*  BarcodeSequence |
| *metadata.columnDelim* | _string_ <br>defines how metadata columns are separated; Typically files are tab or comma separated.<br>*default:*  \t |
| *metadata.commentChar* | _string_ <br>metadata file comment indicator; Empty string is a valid option indicating no comments in metadata file.<br>*default:*  *null* |
| *metadata.fileNameColumn* | _string_ <br>name of the metadata column with input file names<br>*default:*  InputFileName |
| *metadata.filePath* | _string_ <br>If absolute file path, use file as metadata.<br>If directory path, must find exactly 1 file within, to use as metadata.<br>*default:*  *null* |
| *metadata.nullValue* | _string_ <br>metadata cells with this value will be treated as empty<br>*default:*  NA |
| *metadata.required* | _boolean_ <br>If Y, require metadata row for each sample with sequence data in input dirs; If N, samples without metadata are ignored.<br>*default:*  Y |
| *metadata.useEveryRow* | _boolean_ <br>If Y, require a sequence file for every SampleID (every row) in metadata file; If N, metadata can include extraneous SampleIDs.<br>*default:*  *null* |

### pipeline                   
| Property| Description |
| :--- | :--- |
| *pipeline.copyInput* | _boolean_ <br>copy input files into pipeline root directory<br>*default:*  *null* |
| *pipeline.defaultDemultiplexer* | _string_ <br>Java class name for default module used to demultiplex data<br>*default:*  biolockj.module.implicit.Demultiplexer |
| *pipeline.defaultFastaConverter* | _string_ <br>Java class name for default module used to convert files into fasta format<br>*default:*  biolockj.module.seq.AwkFastaConverter |
| *pipeline.defaultProps* | _list of file paths_ <br>file path of default property file(s); Nested default properties are supported (so the default property file can also have a default, and so on).<br>*default:*  *null* |
| *pipeline.defaultSeqMerger* | _string_ <br>Java class name for default module used combined paired read files<br>*default:*  biolockj.module.seq.PearMergeReads |
| *pipeline.defaultStatsModule* | _string_ <br>Java class name for default module used generate p-value and other stats<br>*default:*  biolockj.module.report.r.R_CalculateStats |
| *pipeline.deleteTempFiles* | _boolean_ <br>delete files in temp directories<br>*default:*  *null* |
| *pipeline.detachJavaModules* | _boolean_ <br>If true Java modules do not run with main BioLockJ Java application. Instead they run on compute nodes on the CLUSTER or AWS environments.<br>*default:*  *null* |
| *pipeline.disableAddImplicitModules* | _boolean_ <br>If set to true, implicit modules will not be added to the pipeline.<br>*default:*  *null* |
| *pipeline.disableAddPreReqModules* | _boolean_ <br>If set to true, prerequisite modules will not be added to the pipeline.<br>*default:*  *null* |
| *pipeline.downloadDir* | _file path_ <br>local directory used as the destination in the download command<br>*default:*  $HOME/projects/downloads |
| *pipeline.env* | _string_ <br>Environment in which a pipeline is run. Options: cluster, aws, local<br>*default:*  local |
| *pipeline.inputTypes* | _list_ <br>List of file types. This manually overrides the recommended auto-detection.<br>*default:*  *null* |
| *pipeline.limitDebugClasses* | _list_ <br>limit classes that log debug statements<br>*default:*  *null* |
| *pipeline.logLevel* | _string_ <br>Options: DEBUG, INFO, WARN, ERROR<br>*default:*  INFO |
| *pipeline.permissions* | _string_ <br>Set chmod -R command security bits on pipeline root directory (Ex. 770)<br>*default:*  770 |
| *pipeline.setSeed* | _integer_ <br>set the seed for a random process. Must be positive integer.<br>*default:*  *null* |
| *pipeline.userProfile* | _file path_ <br>Bash profile - may be ~/.bash_profile or ~/.bashrc or others<br>*default:*  ${HOME}/.bash_profile |

### qiime                   
| Property| Description |
| :--- | :--- |
| *qiime.alphaMetrics* | _list_ <br>alpha diversity metrics to calculate through qiime; For complete list of skbio.diversity.alpha options, see <a href= "http://scikit-bio.org/docs/latest/generated/skbio.diversity.alpha.html" target="_top">http://scikit-bio.org/docs/latest/generated/skbio.diversity.alpha.html</a><br>*default:*  shannon |
| *qiime.plotAlphaMetrics* | _boolean_ <br><br>*default:*  Y |

### [r](R.md)                    
| Property| Description |
| :--- | :--- |
| *r.colorBase* | _string_ <br>base color used for labels & headings in the PDF report; Must be a valid color in R.<br>*default:*  black |
| *r.colorFile* | _file path_ <br>path to a tab-delimited file giving the color to use for each value of each metadata field plotted.<br>*default:*  *null* |
| *r.colorHighlight* | _string_ <br>color is used to highlight significant OTUs in plot<br>*default:*  red |
| *r.colorPalette* | _string_ <br>palette argument passed to [get_palette {ggpubr}](https://www.rdocumentation.org/packages/ggpubr/versions/0.2/topics/get_palette) to select colors for some output visualiztions<br>*default:*  *null* |
| *r.colorPoint* | _string_ <br>default color of scatterplot and strip-chart plot points<br>*default:*  black |
| *r.debug* | _boolean_ <br>Options: Y/N. If Y, will generate R Script log files<br>*default:*  Y |
| *r.excludeFields* | _list_ <br>Fields from the metadata that will be excluded from any auto-determined typing, or plotting; R reports must contain at least one valid nominal or numeric metadata field.<br>*default:*  *null* |
| *r.nominalFields* | _list_ <br>Override default property type by explicitly listing it as nominal.<br>*default:*  *null* |
| *r.numericFields* | _list_ <br>Override default property type by explicitly listing it as numeric.<br>*default:*  *null* |
| *r.pch* | _integer_ <br>Sets R plot pch parameter for PDF report<br>*default:*  21 |
| *r.pvalCutoff* | _numeric_ <br>p-value cutoff used to assign label _r.colorHighlight_<br>*default:*  0.05 |
| *r.rareOtuThreshold* | _numeric_ <br>If >=1, R will filter OTUs found in fewer than this many samples. If <1, R will interperate the value as a percentage and discard OTUs not found in at least that percentage of samples<br>*default:*  1 |
| *r.reportFields* | _list_ <br>Metadata fields to include in reports; Fields listed here must exist in the metadata file. R reports must contain at least one valid field.<br>*default:*  *null* |
| *r.saveRData* | _boolean_ <br>If Y, all R script generating BioModules will save R Session data to the module output directory to a file using the extension ".RData"<br>*default:*  *null* |
| *r.timeout* | _integer_ <br>the # minutes before R Script will time out and fail; If undefined, no timeout is used.<br>*default:*  10 |
| *r.useUniqueColors* | _boolean_ <br>force to use a unique color for every value in every field plotted; only recommended for low numbers of metadata columns/values.<br>*default:*  *null* |

### r_PlotMds                   
| Property| Description |
| :--- | :--- |
| *r_PlotMds.reportFields* | _list_ <br>Metadata column names indicating fields to include in the MDS report; Fields listed here must exist in the metadata file.<br>*default:*  *null* |

### report                   
| Property| Description |
| :--- | :--- |
| *report.logBase* | _string_ <br>Options: 10,e,null. If e, use natural log (base e); if 10, use log base 10; if not set, counts will not be converted to a log scale.<br>*default:*  10 |
| *report.minCount* | _integer_ <br>minimum table count allowed, if a count less that this value is found, it is set to 0.<br>*default:*  2 |
| *report.numHits* | _boolean_ <br>Options: Y/N. If Y, and add Num_Hits to metadata<br>*default:*  Y |
| *report.numReads* | _boolean_ <br>Options: Y/N. If Y, and add Num_Reads to metadata<br>*default:*  Y |
| *report.scarceCountCutoff* | _numeric_ <br>Minimum percentage of samples that must contain a count value for it to be kept.<br>*default:*  0.25 |
| *report.scarceSampleCutoff* | _numeric_ <br>Minimum percentage of data columns that must be non-zero to keep the sample.<br>*default:*  0.25 |
| *report.taxonomyLevels* | _list_ <br>Options: domain,phylum,class,order,family,genus,species. Generate reports for listed taxonomy levels<br>*default:*  phylum,class,order,family,genus |
| *report.unclassifiedTaxa* | _boolean_ <br>report unclassified taxa<br>*default:*  Y |

### [script](Script.md)                    
| Property| Description |
| :--- | :--- |
| *script.defaultHeader* | _string_ <br>Store default script header for MAIN script and locally run WORKER scripts.<br>*default:*  #!/bin/bash |
| *script.numThreads* | _integer_ <br>Used to reserve cluster resources and passed to any external application call that accepts a numThreads parameter.<br>*default:*  8 |
| *script.numWorkers* | _integer_ <br>Set number of samples to process per script (if parallel processing)<br>*default:*  1 |
| *script.permissions* | _string_ <br>Used as chmod permission parameter (ex: 774)<br>*default:*  770 |
| *script.timeout* | _integer_ <br>Sets # of minutes before worker scripts times out.<br>*default:*  *null* |

### [validation](Validation.md)                    
| Property| Description |
| :--- | :--- |
| *validation.compareOn* | _list_ <br>Which columns in the expectation file should be used for the comparison. Options: name, size, md5. Default: use all columns in the expectation file.<br>*default:*  *null* |
| *validation.disableValidation* | _boolean_ <br>Turn off validation. No validation file output is produced. Options: Y/N. default: N<br>*default:*  *null* |
| *validation.expectationFile* | _file path_ <br>file path that gives the expected values for file metrics (probably generated by a previous run of the same pipeline)<br>*default:*  *null* |
| *validation.reportOn* | _list_ <br>Which attributes of the file should be included in the validation report file. Options: name, size, md5<br>*default:*  *null* |
| *validation.sizeWithinPercent* | _numeric_ <br>What percentage difference is permitted between an output file and its expectation. Options: any positive number<br>*default:*  *null* |
| *validation.stopPipeline* | _boolean_ <br>If enabled, the validation utlility will stop the pipeline if any module fails validation. Options: Y/N<br>*default:*  *null* |

### aws                   
| Property| Description |
| :--- | :--- |
| *aws.copyDbToS3* | _boolean_ <br>If true, save all input files to S3<br>*default:*  *null* |
| *aws.copyPipelineToS3* | _boolean_ <br>If enabled save pipeline to S3<br>*default:*  *null* |
| *aws.copyReportsToS3* | _boolean_ <br>If enabled save reports to S3<br>*default:*  *null* |
| *aws.ec2AcquisitionStrategy* | _string_ <br>The AWS acquisition strategy (SPOT or DEMAND) sets the service SLA for procuring new EC2 instances<br>*default:*  *null* |
| *aws.ec2InstanceID* | _string_ <br>ID of an existing ec2 instance to use as the head node<br>*default:*  *null* |
| *aws.ec2InstanceType* | _string_ <br>AWS instance type determines initial resource class (t2.micro is common)<br>*default:*  *null* |
| *aws.ec2SpotPer* | __ <br><br>*default:*  *null* |
| *aws.ec2TerminateHead* | _boolean_ <br><br>*default:*  *null* |
| *aws.profile* | _file path_ <br><br>*default:*  *null* |
| *aws.purgeEfsInputs* | _boolean_ <br>If enabled delete all EFS dirs (except pipelines)<br>*default:*  *null* |
| *aws.purgeEfsOutput* | _boolean_ <br>If enabled delete all EFS/pipelines<br>*default:*  *null* |
| *aws.ram* | _string_ <br>AWS memory set in Nextflow main.nf<br>*default:*  *null* |
| *aws.region* | _string_ <br><br>*default:*  *null* |
| *aws.s3* | _string_ <br>AWS S3 pipeline output directory used by Nextflow main.nf<br>*default:*  *null* |
| *aws.s3TransferTimeout* | _integer_ <br>Set the max number of minutes to allow for S3 transfers to complete.<br>*default:*  *null* |
| *aws.saveCloud* | _boolean_ <br><br>*default:*  *null* |
| *aws.stack* | _string_ <br>An existing aws cloud stack ID<br>*default:*  *null* |
| *aws.walltime* | __ <br><br>*default:*  *null* |
