# Demultiplexer
Add to module run order:                    
`#BioModule biolockj.module.implicit.Demultiplexer`

## Description 
Demultiplex samples into separate files for each sample.

## Properties 
*Properties are the `name=value` pairs in the [configuration](../../../Configuration#properties) file.*                   

### Demultiplexer properties: 
| Property| Description |
| :--- | :--- |
| *demultiplexer.barcodeCutoff* | _numeric_ <br>Options: (0.0 - 1.0); if defined, pipeline will fail if the percentage of reads with a barcode is less than this cutoff. -> (DeuxUtil)<br>*default:*  0.05 |
| *demultiplexer.barcodeRevComp* | _boolean_ <br>Options: Y/N. Use reverse compliment of metadata.barcodeColumn if demultimplexer.strategy = barcode_in_header or barcode_in_seq. -> (DeuxUtil)<br>*default:*  *null* |
| *demultiplexer.strategy* | _string_ <br>Options: barcode_in_header, barcode_in_seq, id_in_header, do_not_demux.If using barcodes, they must be provided in the metadata file within column defined by _metadata.barcodeColumn_. -> (DeuxUtil)<br>*default:*  *null* |
| *metadata.barcodeColumn* | _string_ <br>metadata column with identifying barcodes -> Values must be unique.<br>*default:*  BarcodeSequence |
| *metadata.filePath* | _string_ <br>If absolute file path, use file as metadata.<br>If directory path, must find exactly 1 file within, to use as metadata. -> Used for matching sample id to barcodes.<br>*default:*  *null* |

### General properties applicable to this module: 
| Property| Description |
| :--- | :--- |
| *cluster.batchCommand* | _string_ <br>Terminal command used to submit jobs on the cluster<br>*default:*  *null* |
| *cluster.jobHeader* | _string_ <br>Header written at top of worker scripts<br>*default:*  *null* |
| *cluster.modules* | _list_ <br>List of cluster modules to load at start of worker scripts<br>*default:*  *null* |
| *cluster.prologue* | _string_ <br>To run at the start of every script after loading cluster modules (if any)<br>*default:*  *null* |
| *cluster.statusCommand* | _string_ <br>Terminal command used to check the status of jobs on the cluster<br>*default:*  *null* |
| *demultiplexer.barcodeCutoff* | _numeric_ <br>Options: (0.0 - 1.0); if defined, pipeline will fail if the percentage of reads with a barcode is less than this cutoff. -> (DeuxUtil)<br>*default:*  0.05 |
| *demultiplexer.barcodeRevComp* | _boolean_ <br>Options: Y/N. Use reverse compliment of metadata.barcodeColumn if demultimplexer.strategy = barcode_in_header or barcode_in_seq. -> (DeuxUtil)<br>*default:*  *null* |
| *demultiplexer.strategy* | _string_ <br>Options: barcode_in_header, barcode_in_seq, id_in_header, do_not_demux.If using barcodes, they must be provided in the metadata file within column defined by _metadata.barcodeColumn_. -> (DeuxUtil)<br>*default:*  *null* |
| *docker.saveContainerOnExit* | _boolean_ <br>If Y, docker run command will NOT include the --rm flag<br>*default:*  *null* |
| *docker.verifyImage* | _boolean_ <br>In check dependencies, run a test to verify the docker image.<br>*default:*  *null* |
| *metadata.barcodeColumn* | _string_ <br>metadata column with identifying barcodes -> Values must be unique.<br>*default:*  BarcodeSequence |
| *metadata.filePath* | _string_ <br>If absolute file path, use file as metadata.<br>If directory path, must find exactly 1 file within, to use as metadata. -> Used for matching sample id to barcodes.<br>*default:*  *null* |
| *pipeline.defaultDemultiplexer* | _string_ <br>Java class name for default module used to demultiplex data<br>*default:*  biolockj.module.implicit.Demultiplexer |
| *script.defaultHeader* | _string_ <br>Store default script header for MAIN script and locally run WORKER scripts.<br>*default:*  #!/bin/bash |
| *script.numThreads* | _integer_ <br>Used to reserve cluster resources and passed to any external application call that accepts a numThreads parameter.<br>*default:*  8 |
| *script.numWorkers* | _integer_ <br>Set number of samples to process per script (if parallel processing)<br>*default:*  1 |
| *script.permissions* | _string_ <br>Used as chmod permission parameter (ex: 774)<br>*default:*  770 |
| *script.timeout* | _integer_ <br>Sets # of minutes before worker scripts times out.<br>*default:*  *null* |

## Details 
                   
                   
When BioLockJ detects that the input is multiplexed data, BioLockJ automatically adds a Demultiplexer as the 2nd module, using the class path supplied via the `pipeline.defaultDemultiplexer` property. ([ImportMetadata](../biolockj.module.implicit/ImportMetadata) is added as the first module.)                   
                   
This Demultiplexer requires that the sequence headers contain either the Sample ID or an identifying barcode.  Optionally, the barcode can be contained in the sequence itself.  If your data does not conform to one of the following scenarios you will need to pre-process your sequence data to conform to a valid format.                   
                   
#### If samples are not identified by sample ID in the sequence headers:                   
1. Set *demux.strategy*=id_in_header                   
1. Set *input.trimPrefix* to a character string that precedes the sample ID **for all samples**.                   
1. Set *input.trimSuffix* to a character string that comes after the sample ID **for all samples**.                   
                   
**Sample IDs** = mbs1, mbs2, mbs3, mbs4                   
                   
**Scenario 1: Your multiplexed files include Sample IDs in the fastq sequence headers**                    
                   
	@mbs1_134_M01825:384:000000000-BCYPK:1:2106:23543:1336 1:N:0                   
	@mbs2_12_M02825:384:000000000-BCYPK:1:1322:23543:1336 1:N:0                   
	@mbs3_551_M03825:384:000000000-BCYPK:1:1123:23543:1336 1:N:0                   
	@mbs4_1234_M04825:384:000000000-BCYPK:1:9872:23543:1336 1:N:0                   
                   
**Required Config**                   
+ *input.trimPrefix*=@                   
+ *input.trimSuffix*=_                   
                   
All characters before (and including) the 1st "@" in the sequence header are trimmed                   
                   
All characters after (and including) the 1st "_" in the sequence header are trimmed                   
                   
#### If samples are identified by barcode (in the header or sequence):                    
1. Set *demux.strategy*=barcode_in_header or *demux.strategy*=barcode_in_seq                   
1. Set *metadata.filePath* to metadata file path.                   
1. Set *metadata.barcodeColumn* to the barcode column name.                   
1. If the metadata barcodes are listed as reverse compliments, set *demultiplexer.barcodeRevComp*=Y.                   
                   
The metadata file must be prepared by adding a unique sequence barcode in the *metadata.barcodeColumn* column.  This information is often available in a mapping file provided by the sequencing center that produced the raw data.                   
                   
**Metadata file**                    
                   
| ID | BarcodeColumn |                   
| :-- | :-- |                   
| mbs1 | GAGGCATGACTGGATA |                   
| mbs2 | NAGGCATATTTGCACA |                   
| mbs3 | GACCCATGACTGCATA |                   
| mbs4 | TACCCAGCACCGCTTA |                   
                   
**Scenario 2: Your multiplexed files include a barcode in the headers**                   
                   
	@M01825:384:000000000-BCYPK:1:2106:23543:1336 1:N:0:GAGGCATGACTGGATA                   
	@M01825:384:000000000-BCYPK:1:1322:23543:1336 1:N:0:NAGGCATATTTGCACA                   
	@M01825:384:000000000-BCYPK:1:1123:23543:1336 1:N:0:GACCCATGACTGCATA                   
	@M01825:384:000000000-BCYPK:1:9872:23543:1336 1:N:0:TACCCAGCACCGCTTA                    
                   
**Required Config**                   
+ *demux.strategy*=barcode_in_header                   
+ *metadata.barcodeColumn*=BarcodeColumn                   
+ *metadata.filePath*=<path to metadata file>                   
                   
**Scenario 3: Your multiplexed files include a barcode in the sequences**                   
                   
	>M01825:384:000000000-BCYPK:1:2106:23543:1336 1:N:0:                   
        GAGGCATGACTGGATATATACATACTGAGGCATGACTACTTACTATAAGGCTTACTGACTGGTTACTGACTGGGAGGCATGACTACTTACTATAA                   
	>M01825:384:000000000-BCYPK:1:1322:23543:1336 1:N:0:                   
        CAGGCATATTTGCACACTAGAGGCAAGTTACTGACTGGATATACTGAGGCATGGGAGGCATGACTCTATAAGGCTTACTGACTGGTTACTGACTG                   
	>M01825:384:000000000-BCYPK:1:1123:23543:1336 1:N:0: CCATGAGACCTGCATA                   
        CCATGAGACCTGCATACACTGTGGGAGGCATGACTCACTATAAACTACTACTGACTGGATATACTGAGGCATACTGACTGGTTACTTATAAGGCT                   
	>M01825:384:000000000-BCYPK:1:9872:23543:1336 1:N:0:TACCCAGCACCGCTTA                    
        TACCCAGCACCGCTTCCTTGACTTGGGAGGCATGACTCACTATAAACTACTACTGACTGGATATACTGAGGCATACTGACTGGTTACTTATAAGG                   
                        


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
`Demultiplexer.imageOwner`<br>
`Demultiplexer.imageName`<br>
`Demultiplexer.imageTag`<br>

## Citation 
Module developed by Mike Sioda                   
BioLockJ v1.3.13

