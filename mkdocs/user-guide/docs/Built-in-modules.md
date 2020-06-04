# BioModules
Some modules are packaged with BioLockJ (see below).

To use modules created by a third-party, add the compiled files (jar file) to your biolockj extentions folder.  When you call `biolockj`, use the `--external-modules` arg to pass in the location of the extra modules:                  
`biolockj --external-modules </path/to/extentions/folder> <config.properties>`

To create your own modules, see [Building-Modules](../Building-Modules).

In all cases, add modules to your [BioModule order section](../Configuration#biomodule-execution-order) to include them in your pipeline.

## Built-in BioModules:

### [classifiers](module/classifier/module.classifier.md)     

  * [r16s classifiers](module/classifier/module.classifier.r16s.md)
  * [wgs classifiers](module/classifier/module.classifier.wgs.md)

### [implicit modules](module/implicit/module.implicit.md)   

  * [implicit parsers](module/implicit/module.implicit.parser.md)
  * [module.implicit.parser.r16s.md](module/implicit/module.implicit.parser.r16s.md)
  * [module.implicit.parser.wgs.md](module/implicit/module.implicit.parser.wgs.md)
  * [implicit qiime modules](module/implicit/module.implicit.qiime.md)

### [report modules](module/report/module.report.md)      

  * [humann2](module/report/module.report.humann2.md)
  * [report by otu](module/report/module.report.otu.md)
  * [report by taxon](module/report/module.report.taxa.md)
  * [R reports](module/report/module.report.r.md)

### taxa table modules

  * [BuildTaxaTables](GENERATED/biolockj.module.report.taxa/BuildTaxaTables.md)
  * [AddPseudoCount](GENERATED/biolockj.module.report.taxa/AddPseudoCount.md)
  * [NormalizeTaxaTables](GENERATED/biolockj.module.report.taxa/NormalizeTaxaTables.md)
  * [NormalizeByReadsPerMillion](GENERATED/biolockj.module.report.taxa/NormalizeByReadsPerMillion.md)
  * [LogTransformTaxaTables](GENERATED/biolockj.module.report.taxa/LogTransformTaxaTables.md)
  * [AddMetadataToTaxaTables](GENERATED/biolockj.module.report.taxa/AddMetadataToTaxaTables.md)

### sequence modules

BioLockJ comes packaged with several modules for sequence pre-processing.

  * [AwkFastaConverter](GENERATED/biolockj.module.seq/AwkFastaConverter.md)
  * [Gunzipper](GENERATED/biolockj.module.seq/Gunzipper.md)
  * [KneadData](GENERATED/biolockj.module.seq/KneadData.md)
  * [Multiplexer](GENERATED/biolockj.module.seq/Multiplexer.md)
  * [PearMergeReads](GENERATED/biolockj.module.seq/PearMergeReads.md)
  * [RarefySeqs](GENERATED/biolockj.module.seq/RarefySeqs.md)
  * [SeqFileValidator](GENERATED/biolockj.module.seq/SeqFileValidator.md)
  * [TrimPrimers](GENERATED/biolockj.module.seq/TrimPrimers.md)

### DIY modules  

  * [GenMod](GENERATED/biolockj.module.diy/GenMod.md)
  * [Rmarkdown](GENERATED/biolockj.module.diy/Rmarkdown.md)


## List All
See generated docs for [all modules](GENERATED/all-modules.md).
