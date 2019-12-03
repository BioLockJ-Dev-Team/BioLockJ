# OTU report modules

Modules in the **[biolockj.module.report](https://msioda.github.io/BioLockJ/docs/biolockj/module/report/package-summary.html)** sub-pakcage normalize [ParserModule](https://msioda.github.io/BioLockJ/docs/biolockj/module/implicit/parser/ParserModule.html) output, merge the OTU tables with the metadata, or process OTU tables.

----

#### CompileOtuCounts
`#BioModule biolockj.module.report.otu.CompileOtuCounts`

[**Description:**](https://msioda.github.io/BioLockJ/docs/biolockj/module/report/otu/CompileOtuCounts.html "view javadoc")  Compiles the counts from all OTU count files into a single summary OTU count file containing OTU counts for the entire dataset.

[**Options:**](../wiki/Configuration "view option descriptions") *none*

----

#### RarefyOtuCounts
`#BioModule biolockj.module.report.otu.RarefyOtuCounts`

[**Description:**](https://msioda.github.io/BioLockJ/docs/biolockj/module/report/otu/RarefyOtuCounts.html "view javadoc")  Applies a mean iterative post-OTU classification rarefication algorithm so that each output sample will have approximately the same number of OTUs.

[**Options:**](../wiki/Configuration#rarefyOtuCounts "view option descriptions")

   - *rarefyOtuCounts.iterations* 
   - *rarefyOtuCounts.lowAbundantCutoff*
   - *rarefyOtuCounts.quantile*
   - *rarefyOtuCounts.removeSamplesBelowQuantile*

----

#### RemoveLowOtuCounts
`#BioModule biolockj.module.report.otu.RemoveLowOtuCounts`

[**Description:**](https://msioda.github.io/BioLockJ/docs/biolockj/module/report/otu/RemoveLowOtuCounts.html "view javadoc")  Removes OTUs with counts below *report.minCount*.

[**Options:**](../wiki/Configuration#report "view option descriptions")

   - *report.minCount* 
   - *report.numHits*

----

#### RemoveScarceOtuCounts
`#BioModule biolockj.module.report.otu.RemoveScarceOtuCounts`

[**Description:**](https://msioda.github.io/BioLockJ/docs/biolockj/module/report/otu/RemoveScarceOtuCounts.html "view javadoc")  Removes OTUs that are not found in enough samples.

[**Options:**](../wiki/Configuration#report "view option descriptions")

   - *report.scarceCountCutoff* 

----
