**[biolockj.module.implicit.parser.r16s](https://msioda.github.io/BioLockJ/docs/biolockj/module/implicit/parser/r16s/package-summary.html)** is a sub package of [[module.implicit.parser]].<br>

Package modules extend [ParserModuleImpl](https://msioda.github.io/BioLockJ/docs/biolockj/module/implicit/parser/ParserModuleImpl.html) to  generate OTU tables from 16S classifier output.

Implicit modules are ignored if included in the Config file unless *project.allowImplicitModules*=Y.<br>

----

#### RdpParser
`(added by BioLockJ) #BioModule biolockj.module.implicit.parser.r16s.RdpParser`

[**Description:**](https://msioda.github.io/BioLockJ/docs/biolockj/module/implicit/parser/r16s/RdpParser.html "view javadoc")  Build OTU tables from [RDP](http://rdp.cme.msu.edu/classifier/classifier.jsp) reports. 

[**Options:**](../wiki/Configuration#rdp "view option descriptions")

  - *rdp.minThresholdScore*

----

#### QiimeParser
`(added by BioLockJ) #BioModule biolockj.module.implicit.parser.r16s.QiimeParser`

[**Description:**](https://msioda.github.io/BioLockJ/docs/biolockj/module/implicit/parser/r16s/QiimeParser.html "view javadoc")  Build OTU tables from [QIIME](http://qiime.org) summarize_taxa.py otu_table text file reports.

[**Options:**](../wiki/Configuration "view option descriptions") *none*

----