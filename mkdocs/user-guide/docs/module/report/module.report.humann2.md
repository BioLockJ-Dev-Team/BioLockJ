# Pathway Modules

Modules in the **biolockj.module.report.humann2** sub-package use  [ParserModule](https://msioda.github.io/BioLockJ/docs/biolockj/module/implicit/parser/ParserModule.html) output to produce and process pathway tables, such as those produced by [HumanN2](/module/classifier/module.classifier.wgs#humann2classifier).

---

#### Humann2CountModule
`cannot be included in the pipeline run order`

**Description:** Abstract class extends [JavaModuleImpl](https://msioda.github.io/BioLockJ/docs/biolockj/module/JavaModuleImpl.html) that other humann2 classes extend to inherit shared functionality.  Abstract modules cannot be included in the pipeline run order.

**Options:**

   - *humann2.disablePathAbundance*
   - *humann2.disablePathCoverage*
   - *humann2.disableGeneFamilies*

---

#### AddMetadataToPathwayTables
`#BioModule biolockj.module.report.humann2.AddMetadataToPathwayTables`

**Description:**  Add metadata columns to the OTU abundance tables.

**Options:**

 *none*

---

#### RemoveLowPathwayCounts
`#BioModule biolockj.module.report.humann2.RemoveLowPathwayCounts`

**Description:**  This BioModule Pathway counts below a configured threshold to zero.  These low sample counts are assumed to be miscategorized or genomic contamination.

**Options:**

   - *report.minCount*

----

#### RemoveScarcePathwayCounts
`#BioModule biolockj.module.report.humann2.RemoveScarcePathwayCounts`

**Description:**  This BioModule removes scarce pathways not found in enough samples.  Each pathway must be found in a configurable percentage of samples to be retained.

**Options:**

   - *report.scarceCountCutoff*

----