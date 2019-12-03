# Whole Genome Sequence Classifiers

**[biolockj.module.classifier.wgs](https://msioda.github.io/BioLockJ/docs/biolockj/module/classifier/wgs/package-summary.html)** is a sub-package of [[module.classifier]].<br><br>Package modules categorize whole genome sequence micbrobial samples into Operational Taxonomic Units (OTUs) either by reference or with clustering algorithms.<br>

----

#### Humann2Classifier
`#BioModule biolockj.module.classifier.wgs.Humann2Classifier`

[**Description:**](https://msioda.github.io/BioLockJ/docs/biolockj/module/classifier/wgs/Humann2Classifier.html "view javadoc")  Use the Biobakery [HumanN2](https://bitbucket.org/biobakery/humann2) program to generate the HMP Unified Metabolic Analysis Network.

[**Options:**](../wiki/Configuration#humann2 "view option descriptions")

   - *humann2.disablePathAbundance*
   - *humann2.disablePathCoverage*
   - *humann2.disableGeneFamilies*
   - *humann2.nuclDB*
   - *humann2.protDB*
   
----

#### KrakenClassifier
`#BioModule biolockj.module.classifier.wgs.KrakenClassifier`

[**Description:**](https://msioda.github.io/BioLockJ/docs/biolockj/module/classifier/wgs/KrakenClassifier.html "view javadoc")  Classify WGS samples with [KRAKEN](http://ccb.jhu.edu/software/kraken/).

[**Options:**](../wiki/Configuration#kraken "view option descriptions")

   - *kraken.db* 

----

#### Kraken2Classifier
`#BioModule biolockj.module.classifier.wgs.Kraken2Classifier`

[**Description:**](https://msioda.github.io/BioLockJ/docs/biolockj/module/classifier/wgs/Kraken2Classifier.html "view javadoc")  Classify WGS samples with [KRAKEN 2](https://ccb.jhu.edu/software/kraken2/).

[**Options:**](../wiki/Configuration#kraken2 "view option descriptions")

   - *kraken2.db* 

----

#### Metaphlan2Classifier
`#BioModule biolockj.module.classifier.wgs.Metaphlan2Classifier`

[**Description:**](https://msioda.github.io/BioLockJ/docs/biolockj/module/classifier/wgs/Metaphlan2Classifier.html "view javadoc")  Classify WGS samples with [MetaPhlAn](http://bitbucket.org/biobakery/metaphlan2).

[**Options:**](../wiki/Configuration#metaphlan2 "view option descriptions")

   - *exe.python* 
   - *metaphlan2.db*
   - *metaphlan2.mpa_pkl*

----