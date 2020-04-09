# R_PlotOtus
Add to module run order:                    
`#BioModule biolockj.module.report.r.R_PlotOtus`

## Description 
Generate OTU-metadata box-plots and scatter-plots for each reportable metadata field and each *report.taxonomyLevel* configured

## Properties 
*Properties are the `name=value` pairs in the [configuration](../../../Configuration#properties) file.*                   

### R_PlotOtus properties: 
| Property| Description |
| :--- | :--- |
| *r.pValFormat* | _string_ <br>Sets the format used in R sprintf() function<br>*default:*  %1.2g |

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
| *docker.saveContainerOnExit* | _boolean_ <br>if ture, docker run command will NOT include the --rm flag<br>*default:*  *null* |
| *exe.Rscript* | _executable_ <br>Path for the "Rscript" executable; if not supplied, any script that needs the Rscript command will assume it is on the PATH.<br>*default:*  *null* |
| *pipeline.defaultStatsModule* | _string_ <br>Java class name for default module used generate p-value and other stats<br>*default:*  biolockj.module.report.r.R_CalculateStats |
| *r.colorBase* | _string_ <br>base color used for labels & headings in the PDF report; Must be a valid color in R.<br>*default:*  black |
| *r.colorFile* | _file path_ <br>path to a tab-delimited file giving the color to use for each value of each metadata field plotted.<br>*default:*  *null* |
| *r.colorHighlight* | _string_ <br>color is used to highlight significant OTUs in plot<br>*default:*  red |
| *r.colorPalette* | _string_ <br>palette argument passed to [get_palette {ggpubr}](https://www.rdocumentation.org/packages/ggpubr/versions/0.2/topics/get_palette) to select colors for some output visualiztions<br>*default:*  *null* |
| *r.colorPoint* | _string_ <br>default color of scatterplot and strip-chart plot points<br>*default:*  black |
| *r.debug* | _boolean_ <br>Options: Y/N. If Y, will generate R Script log files<br>*default:*  Y |
| *r.pch* | _integer_ <br>Sets R plot pch parameter for PDF report<br>*default:*  21 |
| *r.rareOtuThreshold* | _numeric_ <br>If >=1, R will filter OTUs found in fewer than this many samples. If <1, R will interperate the value as a percentage and discard OTUs not found in at least that percentage of samples<br>*default:*  1 |
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

## Citation 
Module developed by Mike Sioda                   
BioLockj v1.2.9-dev

