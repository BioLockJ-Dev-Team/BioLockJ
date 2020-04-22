# Metaphlan2Classifier
Add to module run order:                    
`#BioModule biolockj.module.classifier.wgs.Metaphlan2Classifier`

## Description 
Classify WGS samples with [MetaPhlAn2](http://bitbucket.org/biobakery/metaphlan2).

## Properties 
*Properties are the `name=value` pairs in the [configuration](../../../Configuration#properties) file.*                   

### Metaphlan2Classifier properties: 
| Property| Description |
| :--- | :--- |
| *exe.metaphlan2* | _executable_ <br>Path for the "metaphlan2" executable; if not supplied, any script that needs the metaphlan2 command will assume it is on the PATH.<br>*default:*  *null* |
| *metaphlan2.db* | _file path_ <br>Directory containing alternate database. Must always be paired with metaphlan2.mpa_pkl<br>*default:*  *null* |
| *metaphlan2.metaphlan2Params* | _list_ <br>additional parameters to use with metaphlan2<br>*default:*  *null* |
| *metaphlan2.mpa_pkl* | _file path_ <br>path to the mpa_pkl file used to reference an alternate DB. Must always be paired with metaphlan2.db<br>*default:*  *null* |

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
biolockj.module.implicit.parser.wgs.Metaphlan2Parser                   

## Docker 
If running in docker, this module will run in a docker container from this image:<br>
```
biolockjdevteam/metaphlan2_classifier:v1.3.2
```
This can be modified using the following properties:<br>
`Metaphlan2Classifier.imgOwner`<br>
`Metaphlan2Classifier.imageName`<br>
`Metaphlan2Classifier.imageTag`<br>

## Citation 
MetaPhlAn2 for enhanced metagenomic taxonomic profiling. Duy Tin Truong, Eric A Franzosa, Timothy L Tickle, Matthias Scholz, George Weingart, Edoardo Pasolli, Adrian Tett, Curtis Huttenhower & Nicola Segata. Nature Methods 12, 902-903 (2015)

