# RdpHierParser
Add to module run order:                    
`#BioModule biolockj.module.implicit.parser.r16s.RdpHierParser`

## Description 
Create taxa tables from the _hierarchicalCount.tsv files output by RDP.

## Properties 
*Properties are the `name=value` pairs in the [configuration](../../../Configuration#properties) file.*                   

### RdpHierParser properties: 
| Property| Description |
| :--- | :--- |
| *rdp.minThresholdScore* | _numeric_ <br>RdpClassifier will use this property and ignore OTU assignments below this threshold score (0-100)<br>*default:*  80 |

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
This module **requires** that _rdp.hierCounts_=Y for the RdpClassifier module to make the required output type.  As long as _rdp.hierCounts_ is set, this module will automatically be added to the module run order by the RdpClassifier module.<br>If this module is in the module run order, it adds `biolockj.module.classifier.r16s.RdpClassifier` as a pre-quisite module. <br>To use this module without the RDP module, include ModuleOutput[RdpClassifier] in the list of input types:<br>`pipeline.inputTypes=ModuleOutput[RdpClassifier]`<br>When using input from a directory, this module takes **exactly** one input directory.<br><br>This module is an alternative to the default parser, RdpParser.  The two parsers produce nearly identical output. The RdpParser module parses the output for each sequence and determines counts for each taxanomic unit. It fills in missing levels so all sequences are counted for all taxanomic levels; this means reads that are unclassified are reported as an OTU with "unclassified" in the name.By contrast, the RdpHierParser module relies on RDP to determine these totals.When using RdpParser the confidence threshold is applied by the parser, when using RdpHierParser the coinfidence threshold is applied by RDP during classification.

## Adds modules 
**pre-requisite modules**                    
biolockj.module.classifier.r16s.RdpClassifier                   
**post-requisite modules**                    
*none found*                   

## Docker 
If running in docker, this module will run in a docker container from this image:<br>
```
biolockjdevteam/biolockj_controller:v1.3.14
```
This can be modified using the following properties:<br>
`RdpHierParser.imageOwner`<br>
`RdpHierParser.imageName`<br>
`RdpHierParser.imageTag`<br>

## Citation 
Module created by Ivory Blakley

