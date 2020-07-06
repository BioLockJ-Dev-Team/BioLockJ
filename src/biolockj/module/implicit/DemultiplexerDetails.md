

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
     
