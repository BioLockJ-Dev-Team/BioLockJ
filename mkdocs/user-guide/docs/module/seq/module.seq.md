# Seq Package

Modules from the **[biolockj.module.seq](https://msioda.github.io/BioLockJ/docs/biolockj/module/seq/package-summary.html)** package prepare sequence data or metadata prior to classification.<br>If included, seq modules must be ordered to run before modules from any of the other packages.

----

#### AwkFastaConverter
`#BioModule biolockj.module.seq.AwkFastaConverter`

**Description:**  Convert fastq files into fasta format (required by [QIIME](http://qiime.org)).   

**Options:**

   - *exe.awk*
   - *exe.gzip*

----

#### Gunzipper
`#BioModule biolockj.module.seq.Gunzipper`

**Description:**  Decompress gzipped files.   

**Options:**

   - *exe.gzip* 

----

#### KneadData
`#BioModule biolockj.module.seq.KneadData`

**Description:**  Runs the Biobakery [KneadData program](https://bitbucket.org/biobakery/kneaddata/wiki/Home) to remove contaminated DNA.    

**Options:**

   - *kneaddata.dbs* 
   - *exe.kneaddata*
   - *exe.kneaddataParams*


----

#### Multiplexer
`#BioModule biolockj.module.seq.Multiplexer`

**Description:**  Multiplex samples into a single file, or two files (one with forward reads, one with reverse reads) if multiplexing paired reads.<br>  BioLockJ modules require demultiplexed data, so if included, this must be the last module in the pipeline other than module.report modules.  

**Options:**

   - *metadata.barcodeColumn*
   - *metadata.filePath*  

----

#### PearMergeReads
`#BioModule biolockj.module.seq.PearMergeReads`

**Description:**  Merge paired reads (required for [RDP](http://rdp.cme.msu.edu/classifier/classifier.jsp) & [QIIME](http://qiime.org)).  For more informations, see the [online PEAR manual](https://sco.h-its.org/exelixis/web/software/pear/doc.html). 

**Options:**

   - *exe.pear* 
   - *exe.pearParams* 

---

#### RarefySeqs
`#BioModule biolockj.module.seq.RarefySeqs`

**Description:**  Randomly select samples to reduce all samples to the configured maximum.<br> Samples with less than the minimum number of reads are discarded.     

**Options:**

   - *rarefySeqs.max* 
   - *rarefySeqs.min*  

---

#### SeqFileValidator
`#BioModule biolockj.module.seq.SeqFileValidator`

**Description:** This BioModule validates fasta/fastq file formats are valid and enforces min/max read lengths.

**Options:**

   - *input.seqMaxLen* 
   - *input.seqMinLen*  
   
---

#### TrimPrimers
`#BioModule biolockj.module.seq.TrimPrimers`

**Description:**  Remove primers from reads, option to discard reads unless primers are attached to both forward and reverse reads.

**Options:**

  - *trimPrimers.filePath*
  - *trimPrimers.requirePrimer*
