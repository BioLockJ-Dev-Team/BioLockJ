# HUMAnN2
Add to module run order:                    
`#BioModule biolockj.module.classifier.wgs.Humann2Classifier`

## Description 
Profile the presence/absence and abundance of microbial pathways in a community from metagenomic or metatranscriptomic sequencing data.

## Properties 
*Properties are the `name=value` pairs in the [configuration](../../../Configuration#properties) file.*                   

### HUMAnN2 properties: 
| Property| Description |
| :--- | :--- |
| *exe.humann2* | _executable_ <br>Path for the "humann2" executable; if not supplied, any script that needs the humann2 command will assume it is on the PATH.<br>*default:*  *null* |
| *humann2.humann2JoinTableParams* | _list_ <br>The parameters to be used with humann2_join_tables<br>*default:*  *null* |
| *humann2.humann2Params* | _list_ <br>The humann2 executable params<br>*default:*  *null* |
| *humann2.humann2RenormTableParams* | _list_ <br>The parameters to use with humann2_renorm_table<br>*default:*  *null* |
| *humann2.nuclDB* | _file path_ <br>Directory containing the nucleotide database<br>*default:*  *null* |
| *humann2.protDB* | _file path_ <br>Directory containing the protein nucleotide database<br>*default:*  *null* |

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
biolockj.module.implicit.parser.wgs.Humann2Parser                   

## Citation 
Franzosa EA*, McIver LJ*, Rahnavard G, Thompson LR, Schirmer M, Weingart G, Schwarzberg Lipson K, Knight R, Caporaso JG, Segata N, Huttenhower C.                    
Species-level functional profiling of metagenomes and metatranscriptomes. Nat Methods 15: 962-968 (2018).                   
http://huttenhower.sph.harvard.edu/humann2                   
BioLockJ module developed by Mike Siota

