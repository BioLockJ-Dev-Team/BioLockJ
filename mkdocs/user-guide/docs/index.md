# 

<img src="img/BioLockJ_Block_green.png" width="300" alt="BioLockJ logo"  class="center">


## What is BioLockJ

**BioLockJ** optimizes your bioinformatics pipeline and metagenomics analysis.  

*  Modular design logically partitions analysis and expedites failure recovery
*  Automated script generation eliminates syntax errors and ensures uniform execution
*  Standardized OTU abundance tables facilitate analysis across datasets
*  Batch scripts take advantage of parallelization on the cluster job queue
*  [configuration](Configuration) file consolidates project details into a principal reference document (and can reproduce analysis)
* [BioModule](https://msioda.github.io/BioLockJ/docs/biolockj/module/BioModule.html) interface provides a flexible mechanism for adding new functionality


## BioLockJ User Guide:
 * [Getting Started](Getting-Started.md)
 * [Commands](Commands.md)
 * Pipeline Componenets
     * [the config file](Configuration.md)
        * Properties
        * [Modules](Built-in-modules.md) 
     * [the metadata](GENERATED/Metadata)
     * input files
     * [Dependencies](Dependencies.md)
 * Features
     * [Check Dependencies](Check-Dependencies.md) before pipeline start
     * [Failure Recovery](Failure-Recovery.md)
     * [Validation](Validation.md)
     * Supported Environments
     * Expand BioLockJ by [Building Modules](Building-Modules.md)
     * BioLockJ [API](BioLockJ-Api)
 * Examples and Templates
     * [Example Pipeline](Example-Pipeline.md)
 * [FAQ & Troubleshooting](FAQ.md)



## Links for Developers

Javadocs                       
[https://BioLockJ-Dev-Team.github.io/BioLockJ/javadocs/](https://BioLockJ-Dev-Team.github.io/BioLockJ/javadocs/)

Developement tests in the sheepdog_testing_suite                  
[https://github.com/BioLockJ-Dev-Team/sheepdog_testing_suite](https://github.com/BioLockJ-Dev-Team/sheepdog_testing_suite)

The user guide for our latest stable version                    
[https://biolockj-dev-team.github.io/BioLockJ/](https://biolockj-dev-team.github.io/BioLockJ/)

The user guide for the current development version, and previous stable versions                 
[https://biolockj.readthedocs.io/en/latest/](https://biolockj.readthedocs.io/en/latest/)

Guidelines for new modules                    
[Building Modules](Building-Modules.md)

## Citing BioLockJ

If you use BioLockJ in your research, you should cite BioLockJ itself AND the tools that make up the pipeline.  The majority of BioLockJ modules are wrappers for independent tools.  See the summary of your pipeline for citation information from the modules in your pipeline. This information is also available in the modules' documentation.

To cite BioLockJ itself, please cite the public project git repository (https://github.com/BioLockJ-Dev-Team/BioLockJ) and author Mike Sioda.
