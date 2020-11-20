# Stop
Add to module run order:                    
`#BioModule biolockj.module.Stop`

## Description 
Stop a pipeline.

## Properties 
*Properties are the `name=value` pairs in the [configuration](../../../Configuration#properties) file.*                   

### Stop properties: 
*none*

### General properties applicable to this module: 
*none*

## Details 
This module immediatley stops a pipeline. <br>This is useful when troubleshooting a pipeline, or while a pipeline is a work-in-progress.  Any downstream modules will be checked in the checkDependencies phase, but will not be reached during the module execution phase.<br>This module and the current pipeline will be flagged as `biolockjFailed`.<br>To progress a pipeline past this module, remove this module from the BioModule run order, and restart the pipeline.

## Adds modules 
**pre-requisite modules**                    
*none found*                   
**post-requisite modules**                    
*none found*                   

## Docker 
If running in docker, this module will run in a docker container from this image:<br>
```
biolockjdevteam/biolockj_controller:v1.3.14
```
This can be modified using the following properties:<br>
`STOP.imageOwner`<br>
`STOP.imageName`<br>
`STOP.imageTag`<br>

## Citation 
Module created by Ivory Blakley

