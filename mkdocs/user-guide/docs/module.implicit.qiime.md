**[biolockj.module.implicit.qiime](https://msioda.github.io/BioLockJ/docs/biolockj/module/implicit/qiime/package-summary.html)** modules are [QIIME Script](http://qiime.org/scripts/) wrappers implicitly added (if needed).<br>Implicit modules are ignored if included in the Config file unless *project.allowImplicitModules*=Y.

----

#### BuildQiimeMapping
`(added by BioLockJ) #BioModule biolockj.module.implicit.qiime.BuildQiimeMapping`

[**Description:**](https://msioda.github.io/BioLockJ/docs/biolockj/module/implicit/qiime/BuildQiimeMapping.html "view javadoc")  This module builds a [QIIME mapping file](http://qiime.org/documentation/file_formats.html#mapping-file-overview) from the metadata.  If the metadata file contains the correct columns out of order, awk is used to correct the column order.  The updated mapping file is verified with the QIIME script [validate_mapping_file.py](http://qiime.org/scripts/validate_mapping_file.html)

[**Options:**](../wiki/Configuration#qiime "view option descriptions")

   - *exe.awk*

----

#### QiimeClassifier
`(added by BioLockJ) #BioModule biolockj.module.implicit.qiime.QiimeClassifier`

[**Description:**](https://msioda.github.io/BioLockJ/docs/biolockj/module/implicit/qiime/QiimeClassifier.html "view javadoc")  Generates bash script lines to summarize QIIME results, build taxonomy reports, and add alpha diversity metrics (if configured). For a complete list of available metrics, see: <a href= "http://scikit-bio.org/docs/latest/generated/skbio.diversity.alpha.html" target="_top">http://scikit-bio.org/docs/latest/generated/skbio.diversity.alpha.html</a>

[**Options:**](../wiki/Configuration#qiime "view option descriptions")

   - *qiime.alphaMetrics*
   - *qiime.pynastAlignDB*
   - *qiime.refSeqDB*
   - *qiime.removeChimeras*
   - *qiime.taxaDB*

----

#### MergeQiimeOtuTables
`(added by BioLockJ) #BioModule biolockj.module.implicit.qiime.MergeQiimeOtuTables`

[**Description:**](https://msioda.github.io/BioLockJ/docs/biolockj/module/implicit/qiime/MergeQiimeOtuTables.html "view javadoc")  This module runs the QIIME script [merge_otu_tables.py](http://qiime.org/scripts/merge_otu_tables.html) to combine the multiple otu_table.biom files output by its required prerequisite module [QiimeClosedRefClassifier](../wiki/module.classifier.r16s#QiimeClosedRefClassifier), so is only necessary if #samples > *script.batchSize*.  

[**Options:**](../wiki/Configuration#qiime "view option descriptions") *none*

----
