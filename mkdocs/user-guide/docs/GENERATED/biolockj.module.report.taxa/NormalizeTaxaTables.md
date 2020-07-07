# NormalizeTaxaTables
Add to module run order:                    
`#BioModule biolockj.module.report.taxa.NormalizeTaxaTables`

## Description 
Normalize taxa tables for sequencing depth.

## Properties 
*Properties are the `name=value` pairs in the [configuration](../../../Configuration#properties) file.*                   

### NormalizeTaxaTables properties: 
*none*

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
| *report.logBase* | _string_ <br>Options: 10,e,null. If e, use natural log (base e); if 10, use log base 10; if not set, counts will not be converted to a log scale.<br>*default:*  10 |
| *script.defaultHeader* | _string_ <br>Store default script header for MAIN script and locally run WORKER scripts.<br>*default:*  #!/bin/bash |
| *script.numThreads* | _integer_ <br>Used to reserve cluster resources and passed to any external application call that accepts a numThreads parameter.<br>*default:*  8 |
| *script.numWorkers* | _integer_ <br>Set number of samples to process per script (if parallel processing)<br>*default:*  1 |
| *script.permissions* | _string_ <br>Used as chmod permission parameter (ex: 774)<br>*default:*  770 |
| *script.timeout* | _integer_ <br>Sets # of minutes before worker scripts times out.<br>*default:*  *null* |

## Details 
                   
Normalize taxa tables based on formula:                   
                   
$$ counts_{normalized} = \frac{counts_{raw}}{n} \frac{\sum (x)}{N} +1 $$                   
                   
Where:                             			                   
                   
* \( counts_{raw} \) = raw count; the cell value before normalizing                    
* \( n \) = number of sequences in the sample (total within a sample)                   
* \( \sum (x) \) = total number of counts in the table (total across samples)                   
* \( N \) = total number of samples                   
                   
                   
                   
Typically the data is put on a \( Log_{10} \) scale, so the full forumula is:                   
                   
$$ counts_{final} = Log_{10} \biggl( \frac{counts_{raw}}{n} \frac{\sum (x)}{N} +1 \biggr) $$                   
                   
The \( counts_{final} \) values will be in output dir of the `LogTransformTaxaTables` module.  The \( counts_{normalized} \) values will be in the output of the `NormalizeTaxaTables` module.                   
                   
                   
For further explanation regarding the normalization scheme, please read The ISME Journal 2013 paper by Dr. Anthony Fodor: ["Stochastic changes over time and not founder effects drive cage effects in microbial community assembly in a mouse model"](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3806260/)                   
                   
If _report.logBase_ is not null, then the `LogTransformTaxaTables` will be added as a post-requisite module.                   


## Adds modules 
**pre-requisite modules**                    
*pipeline-dependent*                   
**post-requisite modules**                    
biolockj.module.report.taxa.LogTransformTaxaTables                   

## Docker 
If running in docker, this module will run in a docker container from this image:<br>
```
biolockjdevteam/biolockj_controller:v1.3.6
```
This can be modified using the following properties:<br>
`NormalizeTaxaTables.imageOwner`<br>
`NormalizeTaxaTables.imageName`<br>
`NormalizeTaxaTables.imageTag`<br>

## Citation 
["Stochastic changes over time and not founder effects drive cage effects in microbial community assembly in a mouse model"](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3806260/)                   
Module developed by Mike Sioda                   
BioLockJ v1.3.6

