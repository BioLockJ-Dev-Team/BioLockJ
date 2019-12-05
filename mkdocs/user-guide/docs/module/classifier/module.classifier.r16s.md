**biolockj.module.classifier.r16s** is a sub-package of [[module.classifier]].<br><br>Package modules extend [ClassifierModuleImpl](https://msioda.github.io/BioLockJ/docs/biolockj/module/classifier/ClassifierModuleImpl.html) to cluster and classify 16S micbrobial samples for taxonomy assignment.

----

#### QiimeClosedRefClassifier
`#BioModule biolockj.module.classifier.r16s.QiimeClosedRefClassifier`

**Description:**  This module picks OTUs using a closed reference database and constructs an OTU table via the QIIME script [pick_closed_reference_otus.py](http://qiime.org/scripts/pick_closed_reference_otus.html).  Taxonomy is assigned using a pre-defined taxonomy map of reference sequence OTU to taxonomy.  This is the fastest OTU picking method since samples can be processed in parallel batches.  Before the QIIME script is run, batches are prepared in the temp directory, with each batch directory containing a fasta directory with *script.batchSize* fasta files and a QIIME mapping file, created with awk, called batchMapping.tsv for the batch of samples.   Inherits from [QiimeClassifier](../../../module.implicit.qiime#QiimeClassifier).

**Options:**

   - *exe.awk* 

----

#### QiimeDeNovoClassifier
`#BioModule biolockj.module.classifier.r16s.QiimeDeNovoClassifier`

**Description:**  This module runs the QIIME [pick_de_novo_otus.py](http://qiime.org/scripts/pick_de_novo_otus.html) script on all fasta sequence files in a single script since OTUs are assigned by a clustering algorithm.  Additional parameters for this script are set using *exe.classifierParams*.  If *qiime.removeChimeras* = "Y", vsearch is used to find chimeric sequences in the output and the QIIME script [filter_otus_from_otu_table.py](http://qiime.org/scripts/filter_otus_from_otu_table.html) is run to remove them from ./output/otu_table.biom.  Inherits from [QiimeClassifier](../../../module.implicit.qiime#QiimeClassifier).

**Options:**

   - *exe.vsearch* 
   - *exe.vsearchParams* 
   - *qiime.removeChimeras* 

----

#### QiimeOpenRefClassifier
`#BioModule biolockj.module.classifier.r16s.QiimeOpenRefClassifier`

**Description:**  This module runs the QIIME [pick_open_reference_otus.py](http://qiime.org/scripts/pick_open_reference_otus.html) script on all fasta sequence files in a single script since clusters not identified in the reference database are assigned by a clustering algorithm.  Additional parameters for this script are set using *exe.classifierParams*.  If *qiime.removeChimeras* = "Y", vsearch is used to find chimeric sequences in the output and the QIIME script [filter_otus_from_otu_table.py](http://qiime.org/scripts/filter_otus_from_otu_table.html) is run to remove them from ./output/otu_table.biom.  Inherits from [QiimeClassifier](../../../module.implicit.qiime#QiimeClassifier).

**Options:**

   - *exe.vsearch* 
   - *exe.vsearchParams* 
   - *qiime.removeChimeras* 

----

#### RdpClassifier
`#BioModule biolockj.module.classifier.r16s.RdpClassifier`

**Description:**  Classify 16s samples with [RDP](http://rdp.cme.msu.edu/classifier/classifier.jsp).   

**Options:**

   - *exe.java* 
   - *rdp.db*
   - *rdp.jar*
   - *rdp.minThresholdScore*

---

See also: [Typical QIIME Pipeline](../../../Example-Pipeline-Qiime)