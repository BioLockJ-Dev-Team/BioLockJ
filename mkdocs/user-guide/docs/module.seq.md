# Seq Package

Modules from the **[biolockj.module.seq](https://msioda.github.io/BioLockJ/docs/biolockj/module/seq/package-summary.html)** package prepare sequence data or metadata prior to classification.<br>If included, seq modules must be ordered to run before modules from any of the other packages.

----

#### AwkFastaConverter
`#BioModule biolockj.module.seq.AwkFastaConverter`

[**Description:**](https://msioda.github.io/BioLockJ/docs/biolockj/module/seq/AwkFastaConverter.html "view javadoc")  Convert fastq files into fasta format (required by [QIIME](http://qiime.org)).   

[**Options:**](../wiki/Configuration#exe "view option descriptions")

   - *exe.awk*
   - *exe.gzip*

----

#### Gunzipper
`#BioModule biolockj.module.seq.Gunzipper`

[**Description:**](https://msioda.github.io/BioLockJ/docs/biolockj/module/seq/Gunzipper.html "view javadoc")  Decompress gzipped files.   

[**Options:**](../wiki/Configuration#exe "view option descriptions")

   - *exe.gzip* 

----

#### KneadData
`#BioModule biolockj.module.seq.KneadData`

[**Description:**](https://msioda.github.io/BioLockJ/docs/biolockj/module/seq/KneadData.html "view javadoc")  Runs the Biobakery [KneadData program](https://bitbucket.org/biobakery/kneaddata/wiki/Home) to remove contaminated DNA.    

[**Options:**](../wiki/Configuration#kneaddata "view option descriptions")

   - *kneaddata.dbs* 
   - *exe.kneaddata*
   - *exe.kneaddataParams*


----

#### Multiplexer
`#BioModule biolockj.module.seq.Multiplexer`

[**Description:**](https://msioda.github.io/BioLockJ/docs/biolockj/module/seq/Multiplexer.html "view javadoc")  Multiplex samples into a single file, or two files (one with forward reads, one with reverse reads) if multiplexing paired reads.<br>  BioLockJ modules require demultiplexed data, so if included, this must be the last module in the pipeline other than [[module.report]] modules.  

[**Options:**](../wiki/Configuration#metadata "view option descriptions")

   - *metadata.barcodeColumn*
   - *metadata.filePath*  

----

#### PearMergeReads
`#BioModule biolockj.module.seq.PearMergeReads`

[**Description:**](https://msioda.github.io/BioLockJ/docs/biolockj/module/seq/PearMergeReads.html "view javadoc")  Merge paired reads (required for [RDP](http://rdp.cme.msu.edu/classifier/classifier.jsp) & [QIIME](http://qiime.org)).  For more informations, see the [online PEAR manual](https://sco.h-its.org/exelixis/web/software/pear/doc.html). 

[**Options:**](../wiki/Configuration#exe "view option descriptions")

   - *exe.pear* 
   - *exe.pearParams* 

---

#### RarefySeqs
`#BioModule biolockj.module.seq.RarefySeqs`

[**Description:**](https://msioda.github.io/BioLockJ/docs/biolockj/module/seq/RarefySeqs.html "view javadoc")  Randomly select samples to reduce all samples to the configured maximum.<br> Samples with less than the minimum number of reads are discarded.     

[**Options:**](../wiki/Configuration#rarefySeqs "view option descriptions")

   - *rarefySeqs.max* 
   - *rarefySeqs.min*  

---

#### SeqFileValidator
`#BioModule biolockj.module.seq.SeqFileValidator`

[**Description:**](https://msioda.github.io/BioLockJ/docs/biolockj/module/seq/SeqFileValidator.html "view javadoc") This BioModule validates fasta/fastq file formats are valid and enforces min/max read lengths.

[**Options:**](../wiki/Configuration#input "view option descriptions")

   - *input.seqMaxLen* 
   - *input.seqMinLen*  
   
---

#### TrimPrimers
`#BioModule biolockj.module.seq.TrimPrimers`

[**Description:**](https://msioda.github.io/BioLockJ/docs/biolockj/module/seq/TrimPrimers.html "view javadoc") Remove primers from reads, option to discard reads unless primers are attached to both forward and reverse reads.

[**Options:**](../wiki/Configuration#trimPrimers "view option descriptions")

  - *trimPrimers.filePath*
  - *trimPrimers.requirePrimer*
