**biolockj.module.implicit.parser.wgs** is a sub package of [[module.implicit.parser]].<br><br>Package modules extend [ParserModuleImpl](https://msioda.github.io/BioLockJ/docs/biolockj/module/implicit/parser/ParserModuleImpl.html) to  generate OTU tables from WGS classifier output.<br>Implicit modules are ignored if included in the Config file unless *project.allowImplicitModules*=Y.<br>

----

#### Humann2Parser
`(added by BioLockJ) #BioModule biolockj.module.implicit.parser.wgs.Humann2Parser`

**Description:**  Build OTU tables from [HumanN2 classifier module](../wiki/module.classifier.wgs#Humann2Classifier) output. 

**Options:**

 *none*

----

#### KrakenParser
`(added by BioLockJ) #BioModule biolockj.module.implicit.parser.wgs.KrakenParser`

**Description:**  Build OTU tables from [KRAKEN](http://ccb.jhu.edu/software/kraken/) mpa-format reports.

**Options:**

 *none*

----

#### Kraken2Parser
`(added by BioLockJ) #BioModule biolockj.module.implicit.parser.wgs.Kraken2Parser`

**Description:**  Build OTU tables from [KRAKEN 2](https://ccb.jhu.edu/software/kraken2/) mpa-format reports. 

**Options:**

 *none*

----

#### Metaphlan2Parser
`(added by BioLockJ) #BioModule biolockj.module.implicit.parser.wgs.MetaphlanParser`

**Description:**  Build OTU tables from [Metaphlan2 classifier module](../wiki/module.classifier.wgs#Metaphlan2Classifier) reports.   

**Options:**

 *none*

----