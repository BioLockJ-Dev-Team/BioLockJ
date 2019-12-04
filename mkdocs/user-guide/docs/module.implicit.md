**biolockj.module.implicit** modules are added to BioLockJ pipelines automatically if needed.<br>Implicit modules are ignored if included in the Config file unless *project.allowImplicitModules=Y*<br><br>  This package contains the following sub-packages:

1. [[module.implicit.parser]] contains [ParserModule](https://msioda.github.io/BioLockJ/docs/biolockj/module/implicit/parser/ParserModule.html) interface & [ParserModuleImpl](https://msioda.github.io/BioLockJ/docs/biolockj/module/implicit/parser/ParserModuleImpl.html) superclass.
1. [[module.implicit.parser.r16s]] contains 16S parser modules.
1. [[module.implicit.parser.wgs]] contains WGS parser modules.
1. [[module.implicit.qiime]] contains QIIME Script wrappers.

----

#### Demultiplexer
`(added by BioLockJ) #BioModule biolockj.module.implicit.ImportMetadata`

**Description:**  Demultiplex samples into separate files for each sample. 

**Options:**

   - *demultiplexer.barcodeCutoff*
   - *demultiplexer.barcodeRevComp*
   - *demultiplexer.strategy*
   - *metadata.filePath*

----

#### ImportMetadata
`(added by BioLockJ) #BioModule biolockj.module.implicit.ImportMetadata`

**Description:**  Required 1st module in every pipeline.<br>  If *metadata.filePath* is undefined, a new metadata file will be created with only a single column "SAMPLE_ID".<br>  The imported file is converted to required BioLockJ metadata format: tab-delimited, with unique column headers, and empty cells are now populated with *metadata.nullValue* or "NA" if undefined.  

**Options:**

   - *metadata.columnDelim*
   - *metadata.commentChar*
   - *metadata.filePath*
   - *metadata.nullValue*

----

#### RegisterNumReads
`(added by BioLockJ) #BioModule biolockj.module.implicit.RegisterNumReads`

**Description:**  Add "Num_Reads" column to metadata file to document the total number of reads per sample. 

**Options:**

   - *report.numReads*

----