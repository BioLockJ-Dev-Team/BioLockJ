# ImportMetadata
Add to module run order:                    
`#BioModule biolockj.module.implicit.ImportMetadata`

## Description 
Read existing metadata file, or create a default one.

## Properties 
*Properties are the `name=value` pairs in the [configuration](../../../Configuration#properties) file.*                   

### ImportMetadata properties: 
*none*

### General properties applicable to this module: 
| Property| Description |
| :--- | :--- |
| *metadata.columnDelim* | _string_ <br>defines how metadata columns are separated; Typically files are tab or comma separated.<br>*default:*  \t |
| *metadata.filePath* | _string_ <br>If absolute file path, use file as metadata.<br>If directory path, must find exactly 1 file within, to use as metadata.<br>*default:*  *null* |
| *metadata.useEveryRow* | _boolean_ <br>If Y, require a sequence file for every SampleID (every row) in metadata file; If N, metadata can include extraneous SampleIDs.<br>*default:*  *null* |

## Details 
*This module is automatically added to the beginning of every pipeline.*
This module ensures that every pipeline has a metadata file, which is requried for modules that add columns to the metadata.  If the configuration file does not specify a metadata file, this module will create an empty table with a row for each file in the input directory.  This also ensures that any pre-existing metadata file has a suitable format.

## Adds modules 
**pre-requisite modules**                    
*none found*                   
**post-requisite modules**                    
*none found*                   

## Docker 
If running in docker, this module will run in a docker container from this image:<br>
```
biolockjdevteam/biolockj_controller:v1.3.13
```
This can be modified using the following properties:<br>
`ImportMetadata.imageOwner`<br>
`ImportMetadata.imageName`<br>
`ImportMetadata.imageTag`<br>

## Citation 
Module developed by Mike Sioda                   
BioLockJ v1.3.13

