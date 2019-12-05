Configuration files contain all system properties, program inputs, cutoff values, external dependencies, and format specifications used during pipeline execution.  
BioLockJ takes a single configuration file as a runtime parameter.  Although all properties can be configured in one file, we recommend chaining default files through the pipeline.defaultProps option. This can often improve the portability, maintainability, and readability of the project-specific configuration files.  

#### Standard Properties

  * BioLockJ will always apply the [standard.properties](https://github.com/msioda/BioLockJ/blob/master/resources/config/default/standard.properties?raw=true) file packaged with BioLockJ under [resources/config/default/](https://github.com/msioda/BioLockJ/tree/master/resources/config/default); you do not need to specify this file in your pipeline.defaultProps chain.
  * IFF running a pipeline in docker, then BioLockJ will apply the [docker.properties](https://github.com/msioda/BioLockJ/blob/master/resources/config/default/docker.properties?raw=true) file packaged with BioLockJ under [resources/config/default/](https://github.com/msioda/BioLockJ/tree/master/resources/config/default).

#### User-specified Defaults
We recommend creating an **environment.properties** file to assign envionment-specific defaults.

  * Set [cluster](#cluster) & [script](#script) properties
  * Set paths to key executables through [exe](#exe) properties
  * Override standard.properties as needed.
  * This information is the same for many (or all) projects run in this environment, and entering the info anew for each project is tedious, time-consuming and error-prone.  

If using a shared system, consider using a user.properties file.   

  * Set user-specific properties such as download.dir and mail.to.
  * For shared projects, use a path that will be updated per-user, such as `~/biolock_user.properties`

Other logical intermediates my also present themselves.  For example, some group of projects may need to override several of the defaults set in environmment.properties, but others still use the those defaults.  Projects in this set can use `pipeline.defaultProps=group2.properties` and the group2.properties files may include `pipeline.defaultProps=environment.properties`

#### Project Properties
Create a new configuration file for each pipeline to assign project-specific properties:

  * Set the [BioModule execution order](#biomodule-execution-order)
  * Set `pipeline.defaultProps = environment.properties`
  * You may use multiple default config files:           
    `pipeline.defaultProps=environment.properties,groupSettings.properties`
  * Override environment.properties and standard.properties as needed
  * Example project configuration files can be found in [templates]( https://github.com/msioda/BioLockJ/tree/master/resources/config/template ). 

If the same property is given in multiple config files, the highest priority goes to the file used to launch the pipeline.  Standard.properties always has the lowest priority.

A copy of each configuration file is stored in the pipeline root directory to serve as primary project documentation.  

## BioModule execution order

To include a BioModule in your pipeline, add a `#BioModule` line to the top your configuration file, as shown in the examples found in [templates]( https://github.com/msioda/BioLockJ/tree/master/resources/config/template ).  Each line has the `#BioModule` keyword followed by the path to the jar file for that module.  For example:

```
#BioModule biolockj.module.seq.PearMergeReads
#BioModule biolockj.module.classifier.wgs.Kraken2Classifier
#BioModule biolockj.module.report.r.R_PlotMds
```

BioModules will be executed in the order they are listed in here.  A typical pipeline contians one [classifier module](/module/classifier/module.classifier).  Any number of [sequence pre-processing](/module/seq/module.seq) modules may come before the classifier module. Any number of [report modules](/module/report/module.report) may come after the classifier module.  In addition to the BioModules specified in the configuration file, BioLockJ may add [implicit modules](/module/implicit/module.implicit) that the are required by specified modules.  See [Example Pipeline](Example-Pipeline).

## Summary of Properties

Properties are defined as name-value pairs. List-values are comma separated. Leading and trailing whitespace is removed so "propName=x,y" is equivalent to "propName = x, y".

#### aws
| Property| Description |
| :--- | :--- |
| *aws.profile* | String |
| *aws.ram* | AWS memory applied through Nextflow.  example value: "8 GB" |
| *aws.stack* | String |
| *aws.s3* | String |

#### cluster
| Property| Description |
| :--- | :--- |
| *cluster.batchCommand* | The command to submit jobs on the cluster |
| *cluster.host* | Cluster host address |
| *cluster.jobHeader* | Job script header to define # of nodes, # of cores, RAM, walltime, etc. |
| *cluster.modules* | List of modules to load before execution.  Adds “module load” command to bash scripts |
| *cluster.prologue* | Command(s) to run at the start of every script after loading cluster modules (if any) |
| *cluster.runJavaAsScript* | Options: Y/N.  If Y, each [JavaModule](https://msioda.github.io/BioLockJ/docs/biolockj/module/JavaModule.html) will instantiate a clone of the application in direct mode on a job node via a single worker script to avoid overworking the head node where BioLockJ is deployed |
| *cluster.validateParams* | Options: Y/N.  If Y, validate *cluster.jobHeader* "ppn:" or "procs:" value matches *script.numThreads* |

#### [demultiplexer](/module/implicit/module.implicit#demultiplexer)
| Property| Description |
| :--- | :--- |
| *demultiplexer.barcodeCutoff* | desc |
| *demultimplexer.barcodeRevComp* | Options: Y/N. Use reverse compliment of *metadata.barcodeColumn* if *demultimplexer.strategy* = barcode_in_header or barcode_in_seq. |
| *demultimplexer.strategy* | Options: barcode_in_header, barcode_in_seq, id_in_header, do_not_demux.<br> Set the [Demultiplexer](https://msioda.github.io/BioLockJ/docs/biolockj/module/implicit/Demultiplexer.html) strategy.  If using barcodes, they must be provided in the *metadata.filePath* with in column name defined by *metadata.barcodeColumn*. |

#### docker
| Property| Description |
| :--- | :--- |
| *docker.imgVersion* | By default, docker will always use 'latest', but advanced users may specify a different tag. |
| *docker.user* | Docker Hub user name with the BioLockJ containers. By default the "biolockj" user is used to pull the standard modules, but advanced users can deploy their own versions of these modules and add new modules in their own Docker Hub account. |
| *docker.saveContainerOnExit* | Y/N. If Y,  property removed the default --rm flag on docker run command |

#### exe
| Property| Description |
| :--- | :--- |
| *exe.awk* | Define executable awk command, if default "awk" is not included in your $PATH |
| *exe.docker* | Define executable docker command, if default "docker" is not included in your $PATH |
| *exe.gzip* | Define executable gzip command, if default "gzip" is not included in your $PATH |
| *exe.humann2* | Define executable humann2 command, if default "humann2" is not included in your $PATH |
| *exe.humann2Params* | Optional humann2 parameters |
| *exe.humann2JoinTableParams* | Optional parameters |
| *exe.humann2RenormTableParams* | Optional parameters |
| *exe.java* | Define executable java command, if default "java" is not included in your $PATH |
| *exe.javaParams* | Optional parameters |
| *exe.kneaddata* | Define executable kneaddata command, if default "kneaddata" is not included in your $PATH |
| *exe.kneaddataParams* | Optional kneaddata parameters |
| *exe.kraken* | Define executable kraken command, if default "kraken" is not included in your $PATH |
| *exe.krakenParams* | Optional kraken parameters |
| *exe.kraken2* | Define executable kraken2 command, if default "kraken2" is not included in your $PATH |
| *exe.kraken2Params* | Optional kraken2 parameters |
| *exe.metaphlan2* | Define executable metaphlan2 command, if default "metaphlan2" is not included in your $PATH |
| *exe.metaphlan2Params* | Optional metaphlan2 parameters |
| *exe.pear* | Define executable pear command, if default "pear" is not included in your $PATH  |
| *exe.pearParams* | Optional pear parameters |
| *exe.python* | Define executable python command, if default "python" is not included in your $PATH |
| *exe.Rscript* | Define executable Rscript command, if default "Rscript" is not included in your $PATH |
| *exe.vsearch* | Define executable vsearch command, if default "vsearch" is not included in your $PATH |
| *exe.vsearchParams* | Optional vsearch parameters |

#### [GenMod](/module/diy/module.DIY#genmod)
| Property| Description |
| :--- | :--- |
| *genMod.launcher* | Define executable language command if it is not included in your $PATH  |
| *genMod.param* | Any parameters that is needed for user's script |
| *genMod.scriptPath* | Path where user script is stored |

#### [humann2](/module/classifier/module.classifier.wgs#humann2classifier)
| Property| Description |
| :--- | :--- |
| *humann2.disableGeneFamilies* | Options: Y/N.  If Y, disable HumanN2 Gene Family report |
| *humann2.disablePathAbundance* | Options: Y/N.  If Y, disable HumanN2 Pathway Abundance report |
| *humann2.disablePathCoverage* | Options: Y/N.  If Y, disable HumanN2 Pathway Coverage report |
| *humann2.keepUnintegrated* | Options: Y/N.  If Y, keep UNINTEGRATED column in count tables (otherwise this column is dropped) |
| *humann2.keepUnmapped* | Options: Y/N.  If Y, keep UNMAPPED column in count tables (otherwise this column is dropped) |
| *humann2.nuclDB* | Directory property may contain multiple nucleotide database files |
| *humann2.protDB* | Directory property may contain protein nucleotide database files |

#### input
| Property| Description |
| :--- | :--- |
| *input.dirPaths* | List of directories containing pipeline input files |
| *input.ignoreFiles* | List of files to ignore if found in * input.dirPaths* |
| *input.requireCompletePairs* | Options: Y/N.  Stop pipeline if any unpaired FW or RV read sequence file is found |
| *input.suffixFw* | File name suffix to indicate a forward read |
| *input.suffixRv* | File name suffix to indicate a reverse read |
| *input.trimPrefix* | For files named by Sample ID, provide the prefix preceding the ID to trim when extracting Sample ID.  For multiplexed sequences, provide any characters in the sequence header preceding the ID. For fastq, this value could be “@” if the sample ID was added to the header immediately after the "@" symbol. |
| *input.trimSuffix* | For files named by Sample ID, provide the suffix after the ID, often this is just the file extension.  Do not include read direction indicators listed in *input.suffixFw/input.suffixRv*. For multiplexed sequences, provide 1st character in the sequence header found after every embedded Sample ID.  If undefined, “_” is used as the default end-of-sample-ID delimiter. |

#### [kneaddata](/module/seq/module.seq#kneaddata)
| Property| Description |
| :--- | :--- |
| *kneaddata.dbs* | Path to database for [KneadData program](https://bitbucket.org/biobakery/kneaddata/wiki/Home) |

#### [kraken](/module/classifier/module.classifier.wgs#krakenclassifier)
| Property| Description |
| :--- | :--- |
| *kraken.db* | Path to kraken database |

#### [kraken2](/module/classifier/module.classifier.wgs#kraken2classifier)
| Property| Description |
| :--- | :--- |
| *kraken2.db* | Path to kraken2 database |

#### [mail](/module/report/module.report#email)
| Property| Description |
| :--- | :--- |
| *mail.encryptedPassword* | Encrypted password from email.from account.  If BioLockJ is passed a 2nd parameter (in addition to the config file), the 2nd parameter should be the clear-text password.  The password will be encrypted and stored in the prop file for future use.  **WARNING: Base64 encryption is only a trivial roadblock for malicious users.  This functionality is intended merely to keep clear-text passwords out of the configuration files and should only be used with a disposable *email.from* account.** |
| *mail.from* | Notification emails sent from this account, provided *email.encryptedPassword* is valid |
| *mail.smtp.auth* | Options: Y/N. Set the SMTP authorization property |
| *mail.smtp.host* | Email SMTP Host |
| *mail.smtp.port* | Email SMTP Host |
| *mail.smtp.starttls.enable* | Options: Y/N. Set the SMTP start TLS property |
| *mail.to* | Comma-separated email recipients list |

#### metadata
| Property| Description |
| :--- | :--- |
| *metadata.barcodeColumn* | Metadata column name containing the barcode used for demultiplexing |
| *metadata.columnDelim* | Define column delimiter for *metadata.filePath* file, default = *tab* |
| *metadata.commentChar* | Define how comments are indicated in *metadata.filePath* file, default = "" |
| *metadata.fileNameColumn* | Column in metadata file giving file names used to identify each sample. Standard default: "InputFileName". Values should be simple names, not file paths, and unique to each sample. Using this column in the metadata overrides the use of input.trimPreifx and input.trimSuffix. For paired reads, give the forward read file and use input.suffixFw and input.suffixRv to link to the reverse file.|
| *metadata.filePath* | Metadata file path, must have unique column headers |
| *metadata.nullValue* | Define how null values are represented in metadata |
| *metadata.required* | Options: Y/N.  Require every sequence file has a corresponding row in metadata file |
| *metadata.useEveryRow* | Options: Y/N.  Requires every metadata row to have a corresponding sequence file |

#### [metaphlan2](/module/classifier/module.classifier.wgs#metaphlan2classifier)
| Property | Description |
| :--- | :--- |
| *metaphlan2.db* | Directory property containing alternate database. Must always be paired with *metaphlan2.mpa_pkl* |
| *metaphlan2.mpa_pkl* | File property containing path to the mpa_pkl file used to reference an alternate DB.  Must always be paired with *metaphlan2.db* |

#### [multiplexer](/module/seq/module.seq#multiplexer)
| Property| Description |
| :--- | :--- |
| *multiplexer.gzip* | Options: Y/N.  If Y, gzip the multiplexed output |

#### pipeline
| Property| Description |
| :--- | :--- |
| *pipeline.copyInput* | Options: Y/N.  If Y, copy *input.dirPaths* into a new directory under the project root directory |
| *pipeline.defaultDemultiplexer* | Assign module to demultiplex datasets.  Default = [Demultiplexer](/module.implicit#Demultiplexer) |
| *pipeline.defaultFastaConverter* | Assign module to convert fastq sequence files into fasta format when required.  Default = [AwkFastaConverter](/module.seq#AwkFastaConverter) |
| *pipeline.defaultSeqMerger* | Assign module to merge paired reads when required.  Default = [PearMergeReads](/module.seq#PearMergeReads) |
| *pipeline.defaultStatsModule* | Java class name for default module used generate p-value and other stats |
| *pipeline.defaultProps* | Path to a default BioLockJ configuration file containing default property values that are overridden if defined in the primary configuration file  |
| *pipeline.deleteTempFiles* | Options: Y/N.  If Y, delete module temp dirs after execution |
| *pipeline.disableAddImplicitModules* | Options: Y/N.  If Y, implicit modules will not be added to the pipeline |
| *pipeline.disableAddPreReqModules* | Options: Y/N.  If Y, prerequisite modules will not be added to the pipeline. |
| *pipeline.downloadDir* | The pipeline summary includes an scp command for the user to download the pipeline analysis if executed on a cluster server.  This property defines the target directory on the users workstation to which the analysis will be downloaded. |
| *pipeline.env* | Options: aws, cluster, local.  Describes runtime environment |
| *pipeline.limitDebugClasses* | used to limit classes that log debug statements |
| *pipeline.logLevel* | Options: DEBUG, INFO, WARN, ERROR.  Determines Java log level sensitivity |
| *pipeline.permissions* | Set chmod -R command security bits on pipeline root directory (Ex. 770) |
| *pipeline.userProfile* | Bash users typically use ~/.bash_profile (the standard default). |

#### [qiime](/module/classifier/module.classifier.r16s)
| Property| Description |
| :--- | :--- |
| *qiime.alphaMetrics* | Options listed online: [scikit-bio.org](http://scikit-bio.org/docs/latest/generated/skbio.diversity.alpha.html) |
| *qiime.params* | Optional parameters passed to qiime scripts |
| *qiime.pynastAlignDB* | File property to define ~/.qiime_config pynast_template_alignment_fp. If supplied, *qiime.refSeqDB* and *qiime.taxaDB* must also be supplied and all three must share some parent directory. |
| *qiime.refSeqDB* | File property to define ~/.qiime_config pick_otus_reference_seqs_fp and assign_taxonomy_reference_seqs_fp. If supplied, *qiime.pynastAlignDB* and *qiime.taxaDB* must also be supplied and all three must share some parent directory. |
| *qiime.removeChimeras* | Options: Y/N.  If Y, remove chimeras after open or de novo OTU picking using  *exe.vsearch* |
| *qiime.taxaDB* | File property to define ~/.qiime_config assign_taxonomy_id_to_taxonomy_fp. If supplied, *qiime.pynastAlignDB* and *qiime.refSeqDB* must also be supplied and all three must share some parent directory. |

#### [r](/module/report/module.report.r)
| Property| Description |
| :--- | :--- |
| *r.colorBase* | This is the base color used for labels & headings in the PDF report |
| *r.colorHighlight* | This color is used to highlight significant OTU plot titles |
| *r.colorPalette* | palette argument passed to [get_palette {ggpubr}](https://www.rdocumentation.org/packages/ggpubr/versions/0.2/topics/get_palette) to select colors for some output visualiztions |
| *r.colorPoint* | Sets the color of scatterplot and strip-chart plot points |
| *r.debug* | Options: Y/N.  If Y, will generate R Script log files  |
| *r.excludeFields* | List metadata columns to exclude from R script reports |
| *r.nominalFields* | Explicitly override default field type assignment to model as a nominal field in R |
| *r.numericFields* | Explicitly override default field type assignment to model as a numeric field in R |
| *r.pch* | Sets R plot pch parameter for PDF report |
| *r.pvalCutoff* | Sets p-value cutoff used to assign label *r.colorHighlight* |
| *r.pValFormat* | Sets the format used in R sprintf() function |
| *r.rareOtuThreshold* | If >1, R will filter OTUs below value provided.  If <1, R will interperate the value as a percentage and discard OTUs not found in at least that percentage of samples |
| *r.reportFields* | Override field used to explicitly list metadata columns to report in the R scripts.  If left undefined, all columns are reported |
| *r.saveRData* | Options: Y/N.  If Y, all R script generating BioModules will save R Session data to the module output directory to a file using the extension ".RData" |
| *r.timeout* | Sets # minutes before R Script will time out and fail |

#### [r_CalculateStats](/module/report/module.report.r#r_calculatestats)
| Property| Description |
| :--- | :--- |
| *r_CalculateStats.pAdjustScope* | Options: GLOBAL, LOCAL, TAXA, ATTRIBUTE.  Used to set the p.adjust "n" parameter for how many simultaneous p-value calculations |
| *r_CalculateStats.pAdjustMethod* | Sets the p.adjust "method" parameter |

#### [r_PlotEffectSize](/module/report/module.report.r#r_ploteffectsize)
| Property| Description |
| :--- | :--- |
| *r_PlotEffectSize.parametricPval* | Options: Y/N. If Y, the parametric p-value is used when determining which taxa to include in the plot and which should get a (*).  If N (default), the non-parametric p-value is used. |
| *r_PlotEffectSize.disablePvalAdj* | Options: Y/N. If Y, the non-adjusted p-value is used when determining which taxa to include in the plot and which should get a (*).  If N (default), the adjusted p-value is used. |
| *r_PlotEffectSize.excludePvalAbove* | Options: [0,1], Taxa with a p-value above this value are excluded from the plot. |
| *r_PlotEffectSize.taxa* | Override other criteria for selecting which taxa to include in the plot by specifying wich taxa should be included |
| *r_PlotEffectSize.maxNumTaxa* | Each plot is given one page. This is the maximum number of bars to include in each one-page plot. |
| *r_PlotEffectSize.disableCohensD* | Options: Y/N. If N (default), produce plots for binary attributes showing effect size calculated as Cohen's d. If Y, skip this plot type. |
| *r_PlotEffectSize.disableRSquared* | Options: Y/N. If N (default), produce plots showing effect size calculated as the r-squared value. If Y, skip this plot type. |
| *r_PlotEffectSize.disableFoldChange* | Options: Y/N. If N (default), produce plots for binary attributes showing the fold change. If Y, skip this plot type. |

#### [r_PlotMds](/module/report/module.report.r#r_plotmds)
| Property| Description |
| :--- | :--- |
| *r_PlotMds.numAxis* | Sets # MDS axis to plot |
| *r_PlotMds.distance* | distance metric for calculating MDS (default: bray) |
| *r_PlotMds.reportFields* | Override field used to explicitly list metadata columns to build MDS plots.  If left undefined, all columns are reported |

#### [rarefyOtuCounts](/module/report/module.report.otu#rarefyotucounts)
| Property| Description |
| :--- | :--- |
| *rarefyOtuCounts.iterations* | Positive integer. The number of iterations to randomly select the *rarefyOtuCounts.quantile* of OTUs |
| *rarefyOtuCounts.lowAbundantCutoff* | Minimum percentage of samples that must contain an OTU. |
| *rarefyOtuCounts.quantile* | Quantile for rarefication. The number of OTUs/sample are ordered, all samples with more OTUs than the quantile sample are subselected without replacement until they have the same number of OTUs as the quantile sample |
| *rarefyOtuCounts.rmLowSamples* | Options: Y/N. If Y, all samples below the *rarefyOtuCounts.quantile* quantile sample are removed |

#### [rarefySeqs](/module/seq/module.seq#rarefyseqs)
| Property| Description |
| :--- | :--- |
| *rarefySeqs.max* | Randomly select maximum number of sequences per sample |
| *rarefySeqs.min* | Discard samples without minimum number of sequences  |

#### [rdp](/module/classifier/module.classifier.r16s#rdpclassifier)
| Property| Description |
| :--- | :--- |
| *rdp.db* | File property used to define an alternate RDP database file |
| *rdp.jar* | File property for RDP java executable JAR |
| *rdp.minThresholdScore* | Required RDP minimum threshold score for valid OTUs |

#### report
| Property| Description |
| :--- | :--- |
| *report.logBase* | Options: 10/e.  If e, use natural log (base e), otherwise use log base 10 |
| *report.minCount* | Integer, minimum table count allowed. If a count less that this value is found, it is set to 0. |
| *report.numHits* | Options: Y/N.  If Y, and add Num_Hits to metadata |
| *report.numReads* | Options: Y/N.  If Y, and add Num_Reads to metadata |
| *report.scarceCountCutoff* | Minimum percentage of samples that must contain a count value for it to be kept. |
| *report.scarceSampleCutoff* | Minimum percentage of data columns that must be non-zero to keep the sample. |
| *report.taxonomyLevels* | Options: domain, phylum, class, order, family, genus, species.  Generate reports for listed taxonomy levels |

#### script
| Property| Description |
| :--- | :--- |
| *script.batchSize* | Number of sequence files to process per worker script |
| *script.defaultHeader* | Used to set shebang line to define scripts as bash executables, such as "#!/bin/bash" |
| *script.numThreads* | Integer value passed to any module that takes a number of threads parameter |
| *script.permissions* | Set chmod command security bits on generated scripts (Ex. 770) |
| *script.timeout* | Integer, time (minutes) before worker scripts times out. |

#### [seqFileValidator](/module/seq/module.seq#seqfilevalidator)
| Property| Description |
| :--- | :--- |
| *seqFileValidator.requireEqualNumPairs* | Options: Y/N. default Y. | 
| *seqFileValidator.seqMaxLen* | maximum number of bases per read | 
| *seqFileValidator.seqMinLen* | minimum number of bases per read | 

#### [trimPrimers](/module/seq/module.seq#trimprimers)
| Property| Description |
| :--- | :--- |
| *trimPrimers.filePath* | Path to file containing one primer sequence per line. | 
| *trimPrimers.requirePrimer* | Options: Y/N. If Y, [TrimPrimers](/module.seq#TrimPrimers) will discard reads that do not include a primer sequence. |

#### [validation](/Validation)
| Property| Description |
| :--- | :--- |
| *validation.compareOn* | Which columns in the expectation file should be used for the comparison. Options: name, size, md5. Default: use all columns in the expectation file.  |
| *validation.disableValidation* | Turn off validation. No validation file output is produced. Options: Y/N. default: N |
| *validation.expectationFile* | File path to the table of expectations.  If a directory is given, BioLockJ will look for a file named after the module being evaluated. |
| *validation.reportOn* | Which attributes of the file should be included in the validation report file. Options: name, size, md5  |
| *validation.sizeWithinPercent* | What percentage difference is permitted between an output file and its expectation. Options: any positive number |
| *validation.stopPipeline* | If enabled, the validation utlility will stop the pipeline if any module fails validation. Options: Y/N | 

