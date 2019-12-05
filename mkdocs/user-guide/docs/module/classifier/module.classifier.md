# Classifier Package

Modules in the **[biolockj.module.classifier](https://msioda.github.io/BioLockJ/docs/biolockj/module/classifier/package-summary.html)** package categorize micbrobial samples into Operational Taxonomic Units (OTUs) either by reference or with clustering algorithms. <br><br>  This package contains 2 sub-packages:<br>

1. module.classifier.r16s contains modules designed to classify 16S data.
1. module.classifier.wgs contains modules designed to classify whole genome sequence data.

Modules in these sub-packages extend the ClassifierModuleImpl class.

----

#### ClassifierModuleImpl

[**Description:**](https://msioda.github.io/BioLockJ/docs/biolockj/module/classifier/ClassifierModuleImpl.html "view javadoc")  Abstract implementation of the [ClassifierModule](https://msioda.github.io/BioLockJ/docs/biolockj/module/classifier/ClassifierModule.html) interface that the other classifier modules extend to inherit standard functionality.<br>  Abstract modules cannot be included in the pipeline run order.

----