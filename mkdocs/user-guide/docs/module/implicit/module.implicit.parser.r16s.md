**biolockj.module.implicit.parser.r16s** is a sub package of [[module.implicit.parser]].<br>

Package modules extend [ParserModuleImpl](https://msioda.github.io/BioLockJ/docs/biolockj/module/implicit/parser/ParserModuleImpl.html) to  generate OTU tables from 16S classifier output.

Implicit modules are ignored if included in the Config file unless *project.allowImplicitModules*=Y.<br>

----

#### RdpParser
`(added by BioLockJ) #BioModule biolockj.module.implicit.parser.r16s.RdpParser`

**Description:**  Build OTU tables from [RDP](http://rdp.cme.msu.edu/classifier/classifier.jsp) reports. 

**Options:**

  - *rdp.minThresholdScore*

----

#### QiimeParser
`(added by BioLockJ) #BioModule biolockj.module.implicit.parser.r16s.QiimeParser`

**Description:**  Build OTU tables from [QIIME](http://qiime.org) summarize_taxa.py otu_table text file reports.

**Options:**

 *none*

----