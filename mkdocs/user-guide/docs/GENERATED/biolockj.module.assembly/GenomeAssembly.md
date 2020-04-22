# GenomeAssembly
Add to module run order:                    
`#BioModule biolockj.module.assembly.GenomeAssembly`

## Description 
Assemble WGS sequences with MetaSPAdes, bin contigs with Metabat2 and check quality with checkM.

## Properties 
*Properties are the `name=value` pairs in the [configuration](../../../Configuration#properties) file.*                   

### GenomeAssembly properties: 
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
biolockjdevteam/genome_assembly:v1.2.0
```
This can be modified using the following properties:<br>
`GenomeAssembly.imgOwner`<br>
`GenomeAssembly.imageName`<br>
`GenomeAssembly.imageTag`<br>

## Citation 
checkM database                   
https://data.ace.uq.edu.au/public/CheckM_databases/checkm_data_2015_01_16.tar.gz                   
HMMER                   
http://eddylab.org/software/hmmer/hmmer.tar.gz                   
MASH                   
https://github.com/marbl/Mash/releases/download/v2.2/mash-Linux64-v2.2.tar                   
Metabat2                   
https://bitbucket.org/berkeleylab/metabat/downloads/metabat-static-binary-linux-x64_v2.12.1.tar.gz                   
MetaSPAdes                   
https://github.com/ablab/spades/releases/download/v3.13.0/SPAdes-3.13.0-Linux.tar.gz                   
PPLACER                   
https://github.com/matsen/pplacer/releases/download/v1.1.alpha19/pplacer-linux-v1.1.alpha19.zip                   
PRODIGAL                   
https://github.com/hyattpd/Prodigal/releases/download/v2.6.3/prodigal.linux                   
                   
Module developed by Shan Sun                   
BioLockJ v1.3.2-dev

