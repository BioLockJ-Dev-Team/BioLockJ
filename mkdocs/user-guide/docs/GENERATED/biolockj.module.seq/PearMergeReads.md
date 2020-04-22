# PearMergeReads
Add to module run order:                    
`#BioModule biolockj.module.seq.PearMergeReads`

## Description 
Run pear, the Paired-End reAd mergeR

## Properties 
*Properties are the `name=value` pairs in the [configuration](../../../Configuration#properties) file.*                   

### PearMergeReads properties: 
| Property| Description |
| :--- | :--- |
| *exe.pear* | _executable_ <br>Path for the "pear" executable; if not supplied, any script that needs the pear command will assume it is on the PATH.<br>*default:*  *null* |
| *pearMergeReads.pearParams* | _string_ <br>optionally pass additional parameters to pear.<br>*default:*  *null* |

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
| *script.defaultHeader* | _string_ <br>Store default script header for MAIN script and locally run WORKER scripts.<br>*default:*  #!/bin/bash |
| *script.numThreads* | _integer_ <br>Used to reserve cluster resources and passed to any external application call that accepts a numThreads parameter.<br>*default:*  8 |
| *script.numWorkers* | _integer_ <br>Set number of samples to process per script (if parallel processing)<br>*default:*  1 |
| *script.permissions* | _string_ <br>Used as chmod permission parameter (ex: 774)<br>*default:*  770 |
| *script.timeout* | _integer_ <br>Sets # of minutes before worker scripts times out.<br>*default:*  *null* |

## Details 
*none*

## Adds modules 
**pre-requisite modules**                    
*none found*                   
**post-requisite modules**                    
*none found*                   

## Docker 
If running in docker, this module will run in a docker container from this image:<br>
```
biolockjdevteam/pear:v1.3.2
```
This can be modified using the following properties:<br>
`PearMergeReads.imgOwner`<br>
`PearMergeReads.imageName`<br>
`PearMergeReads.imageTag`<br>

## Citation 
Jiajie Zhang, Kassian Kobert, Tomáš Flouri, Alexandros Stamatakis, PEAR: a fast and accurate Illumina Paired-End reAd mergeR, Bioinformatics, Volume 30, Issue 5, 1 March 2014, Pages 614–620, https://doi.org/10.1093/bioinformatics/btt593                   
https://cme.h-its.org/exelixis/web/software/pear/doc.html                   
Module developed by Mike Sioda

