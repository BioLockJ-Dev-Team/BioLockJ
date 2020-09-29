# ForEachSample
Add to module run order:                    
`#BioModule biolockj.module.diy.ForEachSample`

## Description 
Like GenMod, but done for each sample.

## Properties 
*Properties are the `name=value` pairs in the [configuration](../../../Configuration#properties) file.*                   

### ForEachSample properties: 
| Property| Description |
| :--- | :--- |
| *genMod.launcher* | _string_ <br>Define executable language command if it is not included in your $PATH<br>*default:*  *null* |
| *genMod.param* | _string_ <br>parameters to pass to the user's script<br>*default:*  *null* |
| *genMod.resources* | _list of file paths_ <br>path to one or more files to be copied to the module resource folder.<br>*default:*  *null* |
| *genMod.scriptPath* | _file path_ <br>path to user script<br>*default:*  *null* |
| *metadata.filePath* | _string_ <br>If absolute file path, use file as metadata.<br>If directory path, must find exactly 1 file within, to use as metadata. -> The row names of the metadata are used as the looping mechanism for this module.<br>*default:*  *null* |

### General properties applicable to this module: 
| Property| Description |
| :--- | :--- |
| *cluster.batchCommand* | _string_ <br>Terminal command used to submit jobs on the cluster<br>*default:*  *null* |
| *cluster.jobHeader* | _string_ <br>Header written at top of worker scripts<br>*default:*  *null* |
| *cluster.modules* | _list_ <br>List of cluster modules to load at start of worker scripts<br>*default:*  *null* |
| *cluster.prologue* | _string_ <br>To run at the start of every script after loading cluster modules (if any)<br>*default:*  *null* |
| *cluster.statusCommand* | _string_ <br>Terminal command used to check the status of jobs on the cluster<br>*default:*  *null* |
| *docker.saveContainerOnExit* | _boolean_ <br>If Y, docker run command will NOT include the --rm flag<br>*default:*  *null* |
| *docker.verifyImage* | _boolean_ <br>In check dependencies, run a test to verify the docker image.<br>*default:*  *null* |
| *metadata.filePath* | _string_ <br>If absolute file path, use file as metadata.<br>If directory path, must find exactly 1 file within, to use as metadata. -> The row names of the metadata are used as the looping mechanism for this module.<br>*default:*  *null* |
| *script.defaultHeader* | _string_ <br>Store default script header for MAIN script and locally run WORKER scripts.<br>*default:*  #!/bin/bash |
| *script.numThreads* | _integer_ <br>Used to reserve cluster resources and passed to any external application call that accepts a numThreads parameter.<br>*default:*  8 |
| *script.numWorkers* | _integer_ <br>Set number of samples to process per script (if parallel processing)<br>*default:*  1 |
| *script.permissions* | _string_ <br>Used as chmod permission parameter (ex: 774)<br>*default:*  770 |
| *script.timeout* | _integer_ <br>Sets # of minutes before worker scripts times out.<br>*default:*  *null* |

## Details 
A test: This is an extention of the [GenMod](../GenMod) module.<br>  For the purpose of this module, a sample is defined as a row of the metadata file.The user script is run using a command:<br> `[launcher] <script> <sample> [params]`

## Adds modules 
**pre-requisite modules**                    
*none found*                   
**post-requisite modules**                    
*none found*                   

## Docker 
If running in docker, this module will run in a docker container from this image:<br>
```
biolockjdevteam/blj_basic:v1.3.15
```
This can be modified using the following properties:<br>
`ForEachSample.imageOwner`<br>
`ForEachSample.imageName`<br>
`ForEachSample.imageTag`<br>

## Citation 
BioLockJ v1.3.15-dev                   
Module developed by Ivory Blakley

