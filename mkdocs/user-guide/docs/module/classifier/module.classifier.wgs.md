# Whole Genome Sequence Classifiers

**biolockj.module.classifier.wgs** is a sub-package of [[module.classifier]].<br><br>Package modules categorize whole genome sequence micbrobial samples into Operational Taxonomic Units (OTUs) either by reference or with clustering algorithms.<br>

----

#### Humann2Classifier
`#BioModule biolockj.module.classifier.wgs.Humann2Classifier`

**Description:**  Use the Biobakery [HumanN2](https://bitbucket.org/biobakery/humann2) program to generate the HMP Unified Metabolic Analysis Network.

**Options:**

   - *humann2.disablePathAbundance*
   - *humann2.disablePathCoverage*
   - *humann2.disableGeneFamilies*
   - *humann2.nuclDB*
   - *humann2.protDB*
   
----

#### KrakenClassifier
`#BioModule biolockj.module.classifier.wgs.KrakenClassifier`

**Description:**  Classify WGS samples with [KRAKEN](http://ccb.jhu.edu/software/kraken/).

**Options:**

   - *kraken.db* 

----

#### Kraken2Classifier
`#BioModule biolockj.module.classifier.wgs.Kraken2Classifier`

**Description:**  Classify WGS samples with [KRAKEN 2](https://ccb.jhu.edu/software/kraken2/).

**Options:**

   - *kraken2.db* 

----

#### Metaphlan2Classifier
`#BioModule biolockj.module.classifier.wgs.Metaphlan2Classifier`

**Description:**  Classify WGS samples with [MetaPhlAn](http://bitbucket.org/biobakery/metaphlan2).

**Options:**

   - *exe.python* 
   - *metaphlan2.db*
   - *metaphlan2.mpa_pkl*

----