# R_PlotPvalHistograms
Add to module run order:                    
`#BioModule biolockj.module.report.r.R_PlotPvalHistograms`

## Description 
Generate p-value histograms for each reportable metadata field and each *report.taxonomyLevel* configured

## Properties 
*Properties are the `name=value` pairs in the [configuration](../../../Configuration#properties) file.*                   

### R_PlotPvalHistograms properties: 
*none*

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
| *pipeline.defaultStatsModule* | _string_ <br>Java class name for default module used generate p-value and other stats<br>*default:*  biolockj.module.report.r.R_CalculateStats |
| *r.colorFile* | _file path_ <br>path to a tab-delimited file giving the color to use for each value of each metadata field plotted.<br>*default:*  *null* |
| *r.debug* | _boolean_ <br>Options: Y/N. If Y, will generate R Script log files<br>*default:*  Y |
| *r.pvalCutoff* | _numeric_ <br>p-value cutoff used to assign label _r.colorHighlight_<br>*default:*  0.05 |
| *r.saveRData* | _boolean_ <br>If Y, all R script generating BioModules will save R Session data to the module output directory to a file using the extension ".RData"<br>*default:*  *null* |
| *r.timeout* | _integer_ <br>the # minutes before R Script will time out and fail; If undefined, no timeout is used.<br>*default:*  10 |
| *script.defaultHeader* | _string_ <br>Store default script header for MAIN script and locally run WORKER scripts.<br>*default:*  #!/bin/bash |
| *script.numThreads* | _integer_ <br>Used to reserve cluster resources and passed to any external application call that accepts a numThreads parameter.<br>*default:*  8 |
| *script.numWorkers* | _integer_ <br>Set number of samples to process per script (if parallel processing)<br>*default:*  1 |
| *script.permissions* | _string_ <br>Used as chmod permission parameter (ex: 774)<br>*default:*  770 |
| *script.timeout* | _integer_ <br>Sets # of minutes before worker scripts times out.<br>*default:*  *null* |

## Details 
*none*

## Adds modules 
**pre-requisite modules**                    
*pipeline-dependent*                   
**post-requisite modules**                    
*none found*                   

## Docker 
If running in docker, this module will run in a docker container from this image:<br>
```
biolockjdevteam/r_module:v1.3.2
```
This can be modified using the following properties:<br>
`R_PlotPvalHistograms.imgOwner`<br>
`R_PlotPvalHistograms.imageName`<br>
`R_PlotPvalHistograms.imageTag`<br>

## Citation 
Module developed by Mike Sioda                   
BioLockJ v1.3.2-dev

