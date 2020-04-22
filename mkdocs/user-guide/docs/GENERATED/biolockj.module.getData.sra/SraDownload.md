# SraDownload
Add to module run order:                    
`#BioModule biolockj.module.getData.sra.SraDownload`

## Description 
SraDownload downloads and compresses short read archive (SRA) files to fastq.gz

## Properties 
*Properties are the `name=value` pairs in the [configuration](../../../Configuration#properties) file.*                   

### SraDownload properties: 
| Property| Description |
| :--- | :--- |
| *exe.fasterq-dump* | _executable_ <br>Path for the "fasterq-dump" executable; if not supplied, any script that needs the fasterq-dump command will assume it is on the PATH.<br>*default:*  *null* |
| *sra.accessionIdColumn* | _string_ <br>Specifies the metadata file column name containing SRA run ids<br>*default:*  *null* |
| *sra.destinationDir* | _file path_ <br>Path to directory where downloaded files should be saved. If specified, it must exist.<br>*default:*  *null* |
| *sra.sraAccList* | _file path_ <br>A file that has one SRA accession per line and nothing else.<br>*default:*  *null* |
| *sra.sraProjectId* | _list_ <br>The project id(s) referencesing a project in the NCBI SRA. example: SRP009633, ERP016051<br>*default:*  *null* |

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
| *exe.gzip* | _executable_ <br>Path for the "gzip" executable; if not supplied, any script that needs the gzip command will assume it is on the PATH.<br>*default:*  *null* |
| *metadata.filePath* | _string_ <br>If absolute file path, use file as metadata.<br>If directory path, must find exactly 1 file within, to use as metadata.<br>*default:*  *null* |
| *script.defaultHeader* | _string_ <br>Store default script header for MAIN script and locally run WORKER scripts.<br>*default:*  #!/bin/bash |
| *script.numThreads* | _integer_ <br>Used to reserve cluster resources and passed to any external application call that accepts a numThreads parameter.<br>*default:*  8 |
| *script.numWorkers* | _integer_ <br>Set number of samples to process per script (if parallel processing)<br>*default:*  1 |
| *script.permissions* | _string_ <br>Used as chmod permission parameter (ex: 774)<br>*default:*  770 |
| *script.timeout* | _integer_ <br>Sets # of minutes before worker scripts times out.<br>*default:*  *null* |

## Details 
Downloading and compressing files requires fasterq-dump and gzip.The accessions to download can be specified using any ONE of the following:<br> 1. A metadata file (given by *metadata.filePath* that has column *sra.accessionIdColumn*.<br> 2. *sra.sraProjectId*, OR <br> 3. *sra.sraAccList*<br>
*sra.destinationDir* gives an external directory that can be shared across pipelines. This is recommended. If it is not specified, the files will be downlaoded to this modules output directory. <br>
Suggested: input.dirPaths = ${sra.destinationDir}<br>
Typically, BioLockJ will automatically determine modules to add to the pipeline to process sequence data. If the files are not present on the system when the pipeline starts, then it is up to the user to configure any and all sequence processing modules.

## Adds modules 
**pre-requisite modules**                    
*none found*                   
**post-requisite modules**                    
*none found*                   

## Docker 
If running in docker, this module will run in a docker container from this image:<br>
```
biolockjdevteam/sratoolkit:v1.3.2
```
This can be modified using the following properties:<br>
`SraDownload.imgOwner`<br>
`SraDownload.imageName`<br>
`SraDownload.imageTag`<br>

## Citation 
[sra-tools](https://github.com/ncbi/sra-tools)                   
Module developed by Philip Badzuh                   
BioLockJ v1.3.2-dev

