# SraMetaDB
Add to module run order:                    
`#BioModule biolockj.module.getData.sra.SraMetaDB`

## Description 
Makes sure that the SRAmetadb exists, downloads if it does not already exist.

## Properties 
*Properties are the `name=value` pairs in the [configuration](../../../Configuration#properties) file.*                   

### SraMetaDB properties: 
| Property| Description |
| :--- | :--- |
| *exe.gunzip* | _executable_ <br>Path for the "gunzip" executable; if not supplied, any script that needs the gunzip command will assume it is on the PATH.<br>*default:*  *null* |
| *exe.wget* | _executable_ <br>Path for the "wget" executable; if not supplied, any script that needs the wget command will assume it is on the PATH.<br>*default:*  *null* |
| *sequenceReadArchive.metaDataDir* | _file path_ <br>path to the directory where the *SRAmetadb.sqlite* database is stored.<br>*default:*  *null* |
| *sraMetaData.forceUpdate* | _boolean_ <br>Y/N: download a newer verionsion if available.<br>*default:*  N |

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
If *sraMetaData.forceUpdate* is set to Y, then the zipped form of the database is downloaded, and kept and used to compare the local version to the server version; and the server version is downloaded if it is newer.

Server version location: https://starbuck1.s3.amazonaws.com/sradb/SRAmetadb.sqlite.gz

*sequenceReadArchive.metaDataDir* directory must exist.  If the database does not exist at that location, it will be downloaded.

The download process is somewhat error-prone, especially in docker. The download is about 4GB and the unzipped database is up to 30GB.It is generally recommended to download and unzip the database manually:

wget https://starbuck1.s3.amazonaws.com/sradb/SRAmetadb.sqlite.gz;  <br>gunzip SRAmetadb.sqlite

## Adds modules 
**pre-requisite modules**                    
*none found*                   
**post-requisite modules**                    
*none found*                   

## Citation 
Module developed by Malcolm Zapatas and Ivory Blakley                   
BioLockJ v1.2.9-dev

