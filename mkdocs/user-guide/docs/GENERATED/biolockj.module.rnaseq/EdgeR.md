# EdgeR
Add to module run order:                    
`#BioModule biolockj.module.rnaseq.EdgeR`

## Description 
Determine statistically significant differences using edgeR.

## Properties 
*Properties are the `name=value` pairs in the [configuration](../../../Configuration#properties) file.*                   

### EdgeR properties: 
| Property| Description |
| :--- | :--- |
| *edgeR.designFactors* | _list_ <br>A comma-separated list of metadata columns to include as factors in the design forumula used with edgeR.<br>*default:*  *null* |
| *edgeR.designFormula* | _string_ <br>The exact string to use as the design the call to model.matrix().<br>*default:*  *null* |
| *edgeR.scriptPath* | _file path_ <br>An R script to use in place of the default script to call edgeR.<br>*default:*  *null* |

### General properties applicable to this module: 
| Property| Description |
| :--- | :--- |
| *cluster.batchCommand* | _string_ <br>Terminal command used to submit jobs on the cluster<br>*default:*  *null* |
| *cluster.jobHeader* | _string_ <br>Header written at top of worker scripts<br>*default:*  *null* |
| *cluster.modules* | _list_ <br>List of cluster modules to load at start of worker scripts<br>*default:*  *null* |
| *cluster.prologue* | _string_ <br>To run at the start of every script after loading cluster modules (if any)<br>*default:*  *null* |
| *cluster.statusCommand* | _string_ <br>Terminal command used to check the status of jobs on the cluster<br>*default:*  *null* |
| *docker.saveContainerOnExit* | _boolean_ <br>If Y, docker run command will NOT include the --rm flag<br>*default:*  *null* |
| *docker.verifyImage* | _boolean_ <br>In check dependencies, run a test to verify the docker image.<br>*default:*  *null* |
| *exe.Rscript* | _executable_ <br>Path for the "Rscript" executable; if not supplied, any script that needs the Rscript command will assume it is on the PATH.<br>*default:*  *null* |
| *script.defaultHeader* | _string_ <br>Store default script header for MAIN script and locally run WORKER scripts.<br>*default:*  #!/bin/bash |
| *script.numThreads* | _integer_ <br>Used to reserve cluster resources and passed to any external application call that accepts a numThreads parameter.<br>*default:*  8 |
| *script.numWorkers* | _integer_ <br>Set number of samples to process per script (if parallel processing)<br>*default:*  1 |
| *script.permissions* | _string_ <br>Used as chmod permission parameter (ex: 774)<br>*default:*  770 |
| *script.timeout* | _integer_ <br>Sets # of minutes before worker scripts times out.<br>*default:*  *null* |

## Details 
The two methods of expresison the design are mutually exclusive.<br>*edgeR.designFormula* is used as an exact string to pass as the design argument to model.matrix(); example:  ~ Location:SoilType. *edgeR.designFactors* is a list (such as "fist,second") of one or more metadata columns to use in a formula. Using this method, the formula will take the form:  ~ first + second  <br>The following two lines are equivilent:<br>`edgeR.designFormula = ~ treatment + batch`<br>`edgeR.designFactors = treatment,batch `

Advanced users may want to make more advanced modifications to the call to the edgeR functions.  The easiest way to do this is to run the module with the default script, and treat that as a working template (ie, see how input/outputs are passed to/from the R script).  Modify the script in that first pipeline, and save the modified script to a stable location.  Then run the pipeline with *edgeR.scriptPath* giving the path to the modified script.

## Adds modules 
**pre-requisite modules**                    
*none found*                   
**post-requisite modules**                    
*none found*                   

## Docker 
If running in docker, this module will run in a docker container from this image:<br>
```
biolockjdevteam/r_edger:v1.3.13
```
This can be modified using the following properties:<br>
`EdgeR.imageOwner`<br>
`EdgeR.imageName`<br>
`EdgeR.imageTag`<br>

## Citation 
R Core Team (2019). R: A language and environment for statistical computing. R Foundation for Statistical Computing, Vienna, Austria. URL https://www.R-project.org/.                   
Mark D. Robinson, Davis J. McCarthy, Gordon K. Smyth, edgeR: a Bioconductor package for differential expression analysis of digital gene expression data, Bioinformatics, Volume 26, Issue 1, 1 January 2010, Pages 139–140, https://doi.org/10.1093/bioinformatics/btp616                   
                   
Module developed by Ivory, Ke and Rosh                   
BioLockJ v1.3.13

