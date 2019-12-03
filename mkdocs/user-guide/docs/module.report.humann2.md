# Pathway Modules

Modules in the **[biolockj.module.report.humann2](https://msioda.github.io/BioLockJ/docs/biolockj/module/report/humann2/package-summary.html)** sub-package use  [ParserModule](https://msioda.github.io/BioLockJ/docs/biolockj/module/implicit/parser/ParserModule.html) output to produce and process pathway tables, such as those produced by [HumanN2](../wiki/module.classifier.wgs#humann2classifier).

---

#### Humann2CountModule
`cannot be included in the pipeline run order`

[**Description:**](https://msioda.github.io/BioLockJ/docs/biolockj/module/report/humann2/Humann2CountModule.html "view javadoc") Abstract class extends [JavaModuleImpl](https://msioda.github.io/BioLockJ/docs/biolockj/module/JavaModuleImpl.html) that other humann2 classes extend to inherit shared functionality.  Abstract modules cannot be included in the pipeline run order.

[**Options:**](../wiki/Configuration#humann2 "view option descriptions")

   - *humann2.disablePathAbundance*
   - *humann2.disablePathCoverage*
   - *humann2.disableGeneFamilies*

---

#### AddMetadataToPathwayTables
`#BioModule biolockj.module.report.humann2.AddMetadataToPathwayTables`

[**Description:**](https://msioda.github.io/BioLockJ/docs/biolockj/module/report/humann2/AddMetadataToPathwayTables.html "view javadoc")  Add metadata columns to the OTU abundance tables.

[**Options:**](../wiki/Configuration#humann2 "view option descriptions") *none*

---

#### RemoveLowPathwayCounts
`#BioModule biolockj.module.report.humann2.RemoveLowPathwayCounts`

[**Description:**](https://msioda.github.io/BioLockJ/docs/biolockj/module/report/humann2/RemoveLowPathwayCounts.html "view javadoc")  This BioModule Pathway counts below a configured threshold to zero.  These low sample counts are assumed to be miscategorized or genomic contamination.

[**Options:**](../wiki/Configuration#report "view option descriptions")

   - *report.minCount*

----

#### RemoveScarcePathwayCounts
`#BioModule biolockj.module.report.humann2.RemoveScarcePathwayCounts`

[**Description:**](https://msioda.github.io/BioLockJ/docs/biolockj/module/report/humann2/RemoveScarcePathwayCounts.html "view javadoc")  This BioModule removes scarce pathways not found in enough samples.  Each pathway must be found in a configurable percentage of samples to be retained.

[**Options:**](../wiki/Configuration#report "view option descriptions")

   - *report.scarceCountCutoff*

----