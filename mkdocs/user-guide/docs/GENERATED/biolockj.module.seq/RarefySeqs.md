# RarefySeqs
Add to module run order:                    
`#BioModule biolockj.module.seq.RarefySeqs`

## Description 
Randomly sub-sample sequences to reduce all samples to the configured maximum.

## Properties 
*Properties are the `name=value` pairs in the [configuration](../../../Configuration#properties) file.*                   

### RarefySeqs properties: 
| Property| Description |
| :--- | :--- |
| *rarefySeqs.max* | _numeric_ <br>Randomly select this number of sequences to keep in each sample<br>*default:*  *null* |
| *rarefySeqs.min* | _numeric_ <br>Discard samples with less than minimum number of sequences<br>*default:*  1 |

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
| *pipeline.defaultSeqMerger* | _string_ <br>Java class name for default module used combined paired read files<br>*default:*  biolockj.module.seq.PearMergeReads |
| *script.defaultHeader* | _string_ <br>Store default script header for MAIN script and locally run WORKER scripts.<br>*default:*  #!/bin/bash |
| *script.numThreads* | _integer_ <br>Used to reserve cluster resources and passed to any external application call that accepts a numThreads parameter.<br>*default:*  8 |
| *script.numWorkers* | _integer_ <br>Set number of samples to process per script (if parallel processing)<br>*default:*  1 |
| *script.permissions* | _string_ <br>Used as chmod permission parameter (ex: 774)<br>*default:*  770 |
| *script.timeout* | _integer_ <br>Sets # of minutes before worker scripts times out.<br>*default:*  *null* |

## Details 
Randomly sub-sample sequences to reduce all samples to the configured maximum `rarefySeqs.max`.  Samples with less than the minimum number of reads `rarefySeqs.min` are discarded.<br>This module will add **biolockj.module.implicit.RegisterNumReads** if there is not already a module to count starting reads per sample.<br>If the input data are paired reads, this module will add a sequence merger, based on property `pipeline.defaultSeqMerger` (currently: biolockj.module.seq.PearMergeReads).

## Adds modules 
**pre-requisite modules**                    
*pipeline-dependent*                   
**post-requisite modules**                    
*none found*                   

## Citation 
Module developed by Mike Sioda                   
BioLockj v1.2.9-dev

