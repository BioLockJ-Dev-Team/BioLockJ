#### BioLockJ optimizes your bioinformatics pipeline and metagenomics analysis.  
*  Modular design logically partitions analysis and expedites failure recovery
*  Automated script generation eliminates syntax errors and ensures uniform execution
*  Standardized OTU abundance tables facilitate analysis across datasets
*  Batch scripts take advantage of parallelization on the cluster job queue
*  [[Configuration]] file consolidates project details into a principal reference document (and can reproduce analysis)
* [BioModule](https://msioda.github.io/BioLockJ/docs/biolockj/module/BioModule.html) interface provides a flexible mechanism for adding new functionality

### Setting up Your Pipeline
[[Example Pipeline]]

### BioModule Packages
| Package | Description | 
| :--- | :--- | 
| [[module.classifier]] | The classifier package contains the [ClassifierModule](https://msioda.github.io/BioLockJ/docs/biolockj/module/classifier/ClassifierModule.html) interface, its default implementation [ClassifierModuleImpl](https://msioda.github.io/BioLockJ/docs/biolockj/module/classifier/ClassifierModuleImpl.html) , and [RDP](http://rdp.cme.msu.edu/classifier/classifier.jsp), [QIIME](http://qiime.org), [KRAKEN](http://ccb.jhu.edu/software/kraken/), [KRAKEN2](https://ccb.jhu.edu/software/kraken2/), & [MetPhlAn](http://bitbucket.org/biobakery/metaphlan2) classifier modules.   These modules output tables with OTU counts for each sample at each taxonomy level configured in *report.taxonomyLevels*. | 
| [[module.implicit]] | The implicit package contains modules that are added to user pipelines if BioLockJ deems them necessary based on pipeline input and configuration.  These modules are ignored if found in the Config file until the override property is enabled: *project.allowImplicitModules*=Y. | 
| [[module.report.r]] | The r package generates R scripts to calculate statistics and build data visualizations based on OTU abundance tables. | 
| [[module.report]] | The report package modules are used to normalize OTU abundance tables, merge them with the metadata, and generate reports and notifications. | 
| [[module.seq]] | The seq package contains a collection of modules used to prepare sequence files and/or metadata prior to classification. | 

### BioModule Interface
- BioModules are Java classes that implement the the [biolockj.module.BioModule](https://msioda.github.io/BioLockJ/docs/biolockj/module/BioModule.html) interface.
- All modules inherit (directly or indirectly) from the default implementation [biolockj.module.BioModuleImpl](https://msioda.github.io/BioLockJ/docs/biolockj/module/BioModuleImpl.html)
