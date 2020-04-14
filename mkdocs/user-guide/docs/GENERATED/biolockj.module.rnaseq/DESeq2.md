# DESeq2
Add to module run order:                    
`#BioModule biolockj.module.rnaseq.DESeq2`

## Description 
Determine statistically significant differences using DESeq2.

## Properties 
*Properties are the `name=value` pairs in the [configuration](../../../Configuration#properties) file.*                   

### DESeq2 properties: 
| Property| Description |
| :--- | :--- |
| *deseq2.designFactors* | _list_ <br>A comma-separated list of metadata columns to include as factors in the design forumula used with DESeq2.<br>*default:*  *null* |
| *deseq2.designFormula* | _string_ <br>The exact string to use as the design the call to DESeqDataSetFromMatrix().<br>*default:*  *null* |
| *deseq2.scriptPath* | _file path_ <br>An R script to use in place of the default script to call DESeq2.<br>*default:*  *null* |

### General properties applicable to this module: 
| Property| Description |
| :--- | :--- |
| *cluster.batchCommand* | _string_ <br>Terminal command used to submit jobs on the cluster<br>*default:*  *null* |
| *cluster.jobHeader* | _string_ <br>Header written at top of worker scripts<br>*default:*  *null* |
| *cluster.modules* | _list_ <br>List of cluster modules to load at start of worker scripts<br>*default:*  *null* |
| *cluster.prologue* | _string_ <br>To run at the start of every script after loading cluster modules (if any)<br>*default:*  *null* |
| *cluster.statusCommand* | _string_ <br>Terminal command used to check the status of jobs on the cluster<br>*default:*  *null* |
| *docker.imageName* | _string_ <br>The name of a docker image to override whatever a module says to use.<br>*default:*  *null* |
| *docker.imageTag* | _string_ <br>indicate specific version of Docker images<br>*default:*  *null* |
| *docker.imgOwner* | _string_ <br>name of the Docker Hub user that owns the docker containers<br>*default:*  *null* |
| *docker.saveContainerOnExit* | _boolean_ <br>If Y, docker run command will NOT include the --rm flag<br>*default:*  *null* |
| *exe.Rscript* | _executable_ <br>Path for the "Rscript" executable; if not supplied, any script that needs the Rscript command will assume it is on the PATH.<br>*default:*  *null* |
| *script.defaultHeader* | _string_ <br>Store default script header for MAIN script and locally run WORKER scripts.<br>*default:*  #!/bin/bash |
| *script.numThreads* | _integer_ <br>Used to reserve cluster resources and passed to any external application call that accepts a numThreads parameter.<br>*default:*  8 |
| *script.numWorkers* | _integer_ <br>Set number of samples to process per script (if parallel processing)<br>*default:*  1 |
| *script.permissions* | _string_ <br>Used as chmod permission parameter (ex: 774)<br>*default:*  770 |
| *script.timeout* | _integer_ <br>Sets # of minutes before worker scripts times out.<br>*default:*  *null* |

## Details 
The two methods of expresison the design are mutually exclusive.<br>*deseq2.designFormula* is used as an exact string to pass as the design argument to DESeqDataSetFromMatrix(); example: " ~ Location:SoilType" (DO include quotes around the formula). *deseq2.designFactors* is a list (such as "fist,second") of one or more metadata columns to use in a formula. Using this method, the formula will take the form: " ~ first + second " <br>The following two lines are equivilent:<br>`deseq2.designFormula ="~ treatment + batch"`<br>`deseq2.designFactors = treatment,batch `

Advanced users may want to make more advanced modifications to the call to the DESeq2 functions.  The easiest way to do this is to run the module with the default script, and treat that as a working template (ie, see how input/outputs are passed to/from the R script).  Modify the script in that first pipeline, and save the modified script to a stable location.  Then run the pipeline with *deseq2.scriptPath* giving the path to the modified script.

## Adds modules 
**pre-requisite modules**                    
*none found*                   
**post-requisite modules**                    
*none found*                   

## Docker 
If running in docker, this module will run in a docker container from this image:<br>
```
biolockjdevteam/r_deseq2:v1.3.2
```
This can be modified using the following properties:<br>
`DESeq2.imgOwner`<br>
`DESeq2.imageName`<br>
`DESeq2.imageTag`<br>

## Citation 
R Core Team (2019). R: A language and environment for statistical computing. R Foundation for Statistical Computing, Vienna, Austria. URL https://www.R-project.org/.                   
Love, M.I., Huber, W., Anders, S. (2014) Moderated estimation of fold change and dispersion for RNA-seq data with DESeq2. Genome Biology, 15:550. 10.1186/s13059-014-0550-8                   
                   
Module developed by Ivory, Ke and Rosh                   
BioLockJ v1.3.2-dev

