# QiimeDeNovoClassifier
Add to module run order:                    
`#BioModule biolockj.module.classifier.r16s.QiimeDeNovoClassifier`

## Description 
Run the QIIME pick_de_novo_otus.py script on all fasta sequence files

## Properties 
*Properties are the `name=value` pairs in the [configuration](../../../Configuration#properties) file.*                   

### QiimeDeNovoClassifier properties: 
| Property| Description |
| :--- | :--- |
| *exe.vsearch* | _executable_ <br>Path for the "vsearch" executable; if not supplied, any script that needs the vsearch command will assume it is on the PATH.<br>*default:*  *null* |
| *qiime.params* | _list_ <br>Parameters for qiime<br>*default:*  *null* |
| *qiime.pynastAlignDB* | _file path_ <br>path to define ~/.qiime_config pynast_template_alignment_fp<br>*default:*  *null* |
| *qiime.refSeqDB* | _file path_ <br>path to define ~/.qiime_config pick_otus_reference_seqs_fp and assign_taxonomy_reference_seqs_fp<br>*default:*  *null* |
| *qiime.removeChimeras* | _boolean_ <br>if vsearch is needed for chimera removal<br>*default:*  Y |
| *qiime.taxaDB* | _file path_ <br>path to define ~/.qiime_config assign_taxonomy_id_to_taxonomy_fp<br>*default:*  *null* |
| *qiime.vsearchParams* | _list_ <br>Parameters for vsearch<br>*default:*  *null* |

### General properties applicable to this module: 
| Property| Description |
| :--- | :--- |
| *cluster.batchCommand* | _string_ <br>Terminal command used to submit jobs on the cluster<br>*default:*  *null* |
| *cluster.jobHeader* | _string_ <br>Header written at top of worker scripts<br>*default:*  *null* |
| *cluster.modules* | _list_ <br>List of cluster modules to load at start of worker scripts<br>*default:*  *null* |
| *cluster.prologue* | _string_ <br>To run at the start of every script after loading cluster modules (if any)<br>*default:*  *null* |
| *cluster.statusCommand* | _string_ <br>Terminal command used to check the status of jobs on the cluster<br>*default:*  *null* |
| *docker.imageName* | _string_ <br>The name of a docker image to override whatever a module says to use.<br>*default:*  *null* |
| *docker.imageTag* | _string_ <br>indicate specific version of Docker images<br>*default:*  *null* |
| *docker.imgOwner* | _string_ <br>name of the Docker Hub user that owns the docker containers<br>*default:*  *null* |
| *docker.saveContainerOnExit* | _boolean_ <br>if ture, docker run command will NOT include the --rm flag<br>*default:*  *null* |
| *pipeline.defaultFastaConverter* | _string_ <br>Java class name for default module used to convert files into fasta format<br>*default:*  biolockj.module.seq.AwkFastaConverter |
| *pipeline.defaultSeqMerger* | _string_ <br>Java class name for default module used combined paired read files<br>*default:*  biolockj.module.seq.PearMergeReads |
| *script.defaultHeader* | _string_ <br>Store default script header for MAIN script and locally run WORKER scripts.<br>*default:*  #!/bin/bash |
| *script.numThreads* | _integer_ <br>Used to reserve cluster resources and passed to any external application call that accepts a numThreads parameter.<br>*default:*  8 |
| *script.numWorkers* | _integer_ <br>Set number of samples to process per script (if parallel processing)<br>*default:*  1 |
| *script.permissions* | _string_ <br>Used as chmod permission parameter (ex: 774)<br>*default:*  770 |
| *script.timeout* | _integer_ <br>Sets # of minutes before worker scripts times out.<br>*default:*  *null* |

## Details 
This module runs the QIIME [pick_de_novo_otus.py](http://qiime.org/scripts/pick_de_novo_otus.html) script on all fasta sequence files in a single script since OTUs are assigned by a clustering algorithm.  Additional parameters for this script are set using *exe.classifierParams*.  If *qiime.removeChimeras* = "Y", vsearch is used to find chimeric sequences in the output and the QIIME script [filter_otus_from_otu_table.py](http://qiime.org/scripts/filter_otus_from_otu_table.html) is run to remove them from ./output/otu_table.biom.  Inherits from [QiimeClassifier](../../../module.implicit.qiime#QiimeClassifier).

## Adds modules 
**pre-requisite modules**                    
*pipeline-dependent*                   
**post-requisite modules**                    
biolockj.module.implicit.qiime.QiimeClassifier                   
biolockj.module.implicit.parser.r16s.QiimeParser                   

## Citation 
QIIME allows analysis of high-throughput community sequencing data                   
J Gregory Caporaso, Justin Kuczynski, Jesse Stombaugh, Kyle Bittinger, Frederic D Bushman, Elizabeth K Costello, Noah Fierer, Antonio Gonzalez Pena, Julia K Goodrich, Jeffrey I Gordon, Gavin A Huttley, Scott T Kelley, Dan Knights, Jeremy E Koenig, Ruth E Ley, Catherine A Lozupone, Daniel McDonald, Brian D Muegge, Meg Pirrung, Jens Reeder, Joel R Sevinsky, Peter J Turnbaugh, William A Walters, Jeremy Widmann, Tanya Yatsunenko, Jesse Zaneveld and Rob Knight; Nature Methods, 2010; doi:10.1038/nmeth.f.303                   
(needs further citation)                   
http://www.wernerlab.org/software/macqiime/citations

