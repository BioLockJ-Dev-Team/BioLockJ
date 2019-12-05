# Built-in BioModules
Some modules are packaged with BioLockJ (see below).

To use modules created by a third-party, add the compiled files (jar file) to your biolockj extentions folder.  When you call `biolockj`, use the `--external-modules` arg to pass in the location of the extra modules:                  
`biolockj --external-modules </path/to/extentions/folder> <config.properties>`

To create your own modules, see [Building-Modules](Building-Modules).

In all cases, add modules to your [BioModule order section](Configuration#biomodule-execution-order) to include them in your pipeline.

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

### [sequence modules](module/seq/module.seq.md)

### DIY modules  

  * [GenMod](module/diy/module.diy.md)
