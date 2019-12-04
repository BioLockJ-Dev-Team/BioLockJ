**biolockj.module.implicit.parser** modules parse classifier output to generate OTU tables.<br>Implicit modules are ignored if included in the Config file unless *project.allowImplicitModules*=Y.<br>

This package contains the following sub-packages:

1. [[module.implicit.parser.r16s]] modules parse [[module.classifier.r16s]] reports.
1. [[module.implicit.parser.wgs]] modules parse [[module.classifier.wgs]] reports.

----

#### ParserModuleImpl
`cannot be included in the pipeline run order`

**Description:**  Abstract implementation of [ParserModule](https://msioda.github.io/BioLockJ/docs/biolockj/module/implicit/parser/ParserModule.html) that the other modules extend to inherit standard functionality.<br>  Abstract modules cannot be added to a pipeline, but the r16s & WGS sub-packages contain modules that inherit standard parser functionality from this class.

**Options:**

   - *report.numHits*

----