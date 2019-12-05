---
### Q: If biolockj indicates that my pipeline started successfully, but the pipeline root directory is not created, how do I debug the root cause of the failure?
---
**A:** Generally, errors are output to the pipeline log file and documented in the notification email, but  invalid configuration settings may cause a fatal error to occur before the pipeline directory is created.  In this scenario, look in your $HOME directory for a file name that starts with "biolockj_FATAL_ERROR_".  

* Verify you are running Java 1.8+

	    java -version

* Look in the error message found in $HOME/biolockj_FATAL_ERROR_* for a reference to one of your Config file parameters, the most common culprit is:  

  - *pipeline.defaultProps*
  - $BLJ_PROJ misconfigured in /script/blj_config

---
### Q: How should I configure *input properties* for a demultiplexed dataset?
---
**A:** Name the sequence files using the Sample IDs listed in your metadata file.  Sequence file names containing a prefix or suffix (in addition the Sample ID) can be used as long as there is a unique character string that can be used to identify the boundary between the Sample ID and its prefix or suffix.  These values can be set via the *input.trimPrefix* & *input.trimSuffix* properties.

1. Set *input.trimPrefix* to a character string that precedes the sample ID **for all samples**
1. Set *input.trimSuffix* to a character string that comes after the sample ID **for all samples**

If a single prefix or suffix identifier cannot be used for all samples, the file names must be updated so that a universal prefix or suffix identifier can be used.

## Example

**Sample IDs** = mbs1, mbs2, mbs3, mbs4

**Example File names**
+ gut_mbs1.fq.gz
+ gut_mbs2.fq.gz
+ oral_mbs3.fq
+ oral_mbs4.fq

**Config Properties**
+ *input.trimPrefix*=_
+ *input.trimSuffix*=.fq

All characters before (and including) the 1st "_" in the file name are trimmed

All characters after (and including) the 1st ".fq" in the file name are trimmed

BioLockJ automatically trims extensions ".fasta" and ".fastq" as if configured in *input.trimSuffix*.

---
### Q: How do I configure my pipeline for multiplexed data?
---
**A:** BioLockJ automatically adds the [Demultiplexer](module/implicit/module.implicit#demultiplexer) as the 2nd module - after [ImportMetadata](module/implicit/module.implicit#importmetadata) - when processing multiplexed data.  The [Demultiplexer](module/implicit/module.implicit#demultiplexer) requires that the sequence headers contain either the Sample ID or an identifying barcode.  Optionally, the barcode can be contained in the sequence itself.  If your data does not conform to one of the following scenarios you will need to pre-process your sequence data to conform to a valid format.

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
1. If the metadata barcodes are listed as reverse compliments, set *demux.barcodeUseReverseCompliment*=Y.

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
     
