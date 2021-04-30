# ForEachLoop
Add to module run order:                    
`#BioModule biolockj.module.diy.ForEachLoop`

## Description 
Like GenMod, but done for each string in a comma-separated list.

## Properties 
*Properties are the `name=value` pairs in the [configuration](../../../Configuration#properties) file.*                   

### ForEachLoop properties: 
| Property| Description |
| :--- | :--- |
| *genMod.launcher* | _string_ <br>Define executable language command if it is not included in your $PATH<br>*default:*  *null* |
| *genMod.loopBy* | _list_ <br>List used as the looping mechanism for this module.<br>*default:*  *null* |
| *genMod.param* | _string_ <br>parameters to pass to the user's script<br>*default:*  *null* |
| *genMod.resources* | _list of file paths_ <br>path to one or more files to be copied to the module resource folder.<br>*default:*  *null* |
| *genMod.scriptPath* | _file path_ <br>path to user script<br>*default:*  *null* |

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
| *script.defaultHeader* | _string_ <br>Store default script header for MAIN script and locally run WORKER scripts.<br>*default:*  #!/bin/bash |
| *script.numThreads* | _integer_ <br>Used to reserve cluster resources and passed to any external application call that accepts a numThreads parameter.<br>*default:*  8 |
| *script.numWorkers* | _integer_ <br>Set number of samples to process per script (if parallel processing)<br>*default:*  1 |
| *script.permissions* | _string_ <br>Used as chmod permission parameter (ex: 774)<br>*default:*  770 |
| *script.timeout* | _integer_ <br>Sets # of minutes before worker scripts times out.<br>*default:*  *null* |

## Details 
This is an extention of the [GenMod](../GenMod) module.<br>  The given script is run for each element given in the comma-separated list _genMod.loopBy_.The user script is run using a command:<br> `[launcher] <script> <loop-element> [param]`

## Adds modules 
**pre-requisite modules**                    
*none found*                   
**post-requisite modules**                    
*none found*                   

## Docker 
If running in docker, this module will run in a docker container from this image:<br>
```
biolockjdevteam/blj_basic:v1.3.18
```
This can be modified using the following properties:<br>
`ForEachLoop.imageOwner`<br>
`ForEachLoop.imageName`<br>
`ForEachLoop.imageTag`<br>

## Citation 
BioLockJ v1.3.18                   
Module developed by Ivory Blakley

