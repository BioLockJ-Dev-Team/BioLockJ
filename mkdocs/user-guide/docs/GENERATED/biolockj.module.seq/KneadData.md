# KneadData
Add to module run order:                    
`#BioModule biolockj.module.seq.KneadData`

## Description 
Run the Biobakery [KneadData](https://bitbucket.org/biobakery/kneaddata/wiki/Home) program to remove contaminated DNA.

## Properties 
*Properties are the `name=value` pairs in the [configuration](../../../Configuration#properties) file.*                   

### KneadData properties: 
| Property| Description |
| :--- | :--- |
| *exe.kneaddata* | _executable_ <br>Path for the "kneaddata" executable; if not supplied, any script that needs the kneaddata command will assume it is on the PATH.<br>*default:*  *null* |
| *kneaddata.dbs* | _file path_ <br>Path to database for KneadData program<br>*default:*  *null* |
| *kneaddata.kneaddataParams* | _string_ <br>Optional parameters passed to kneaddata<br>*default:*  *null* |

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
biolockjdevteam/kraken_classifier:v1.3.2
```
This can be modified using the following properties:<br>
`KneadData.imgOwner`<br>
`KneadData.imageName`<br>
`KneadData.imageTag`<br>

## Citation 
https://bitbucket.org/biobakery/kneaddata/wiki/Home                   
Module developed by Mike Sioda

