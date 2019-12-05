#### Any Java class that implements the [BioModule](https://msioda.github.io/BioLockJ/docs/biolockj/module/BioModule.html) interface can be added to a BioLockJ pipeline.

The BioLockJ v1.0 implementation is currently focused on metagenomics analysis, but the generalized application framework is not limited to this domain.  Users can implement new BioModules to automate a wide variety of bioinformatics and report analytics.  The [BioModule](https://msioda.github.io/BioLockJ/docs/biolockj/module/BioModule.html) interface was designed so that users can develop new modules on their own.  

## Beginners

See the BioModule hello world tutorial.

## Coding your module

To create a new [BioModule](https://msioda.github.io/BioLockJ/docs/biolockj/module/BioModule.html), simply extend one of the abstract Java superclasses, code it's abstract methods, and add it to your pipeline with #BioModule tag your Config file:<br>
1. [BioModuleImpl](https://msioda.github.io/BioLockJ/docs/biolockj/module/BioModuleImpl.html): Extend if a more specific interface does not apply
1. [ScriptModuleImpl](https://msioda.github.io/BioLockJ/docs/biolockj/module/ScriptModuleImpl.html): Extend if your module generates and executes bash scripts
1. [JavaModuleImpl](https://msioda.github.io/BioLockJ/docs/biolockj/module/JavaModuleImpl.html): Extend if your module only runs Java code 
1. [ClassifierModuleImpl](https://msioda.github.io/BioLockJ/docs/biolockj/module/classifier/ClassifierModuleImpl.html): Extend to support a new classifier program
1. [ParserModuleImpl](https://msioda.github.io/BioLockJ/docs/biolockj/module/implicit/parser/ParserModuleImpl.html): Extend to parse output of a new classifier program
1. [R_Module](https://msioda.github.io/BioLockJ/docs/biolockj/module/report/r/R_Module.html): Extend if your module generates and executes R scripts

#### To support a new classifier, create 3 modules that implement the following interfaces:
1. [ClassifierModule](https://msioda.github.io/BioLockJ/docs/biolockj/module/classifier/ClassifierModule.html): Implement to generate bash scripts needed to call classifier program
1. [ParserModule](https://msioda.github.io/BioLockJ/docs/biolockj/module/implicit/parser/ParserModule.html): Implement to parse classifier output, configure as classifier post-requisite 
1. [OtuNode](https://msioda.github.io/BioLockJ/docs/biolockj/node/OtuNode.html): Classifier specific implementation holds OTU information for 1 sequence

#### [BioModuleImpl](https://msioda.github.io/BioLockJ/docs/biolockj/module/BioModuleImpl.html) is the top-level superclass for all modules.
| Method | Description | 
| :--- | :--- | 
| **checkDependencies()** | **Must override.**  Called before executeTask() to identify Configuration errors and perform runtime validations. |
| **executeTask()** | **Must override.**  Executes core module logic. |
| cleanUp() | Called after executeTask() to run cleanup operations, update Config properties, etc. |
| getInputFiles() | Return previous module output. |
| getModuleDir() | Return module root directory. |
| getOutputDir() | Return module output directory. |
| getPostRequisiteModules() | Returns a list of BioModules to run after the current module. |
| getPreRequisiteModules() | Returns a list of BioModules to run before the current module. |
| getSummary() | Return output directory summary.  Most modules override this method by adding module specific summary details to super.getSummary(). |
| getTempDir() | Return module temp directory.  |
| setModuleDir(path) | Set module directory. |

#### [ScriptModuleImpl](https://msioda.github.io/BioLockJ/docs/biolockj/module/ScriptModuleImpl.html) extends [BioModuleImpl](https://msioda.github.io/BioLockJ/docs/biolockj/module/BioModuleImpl.html):  superclass for script-generating modules.
| Method | Description | 
| :--- | :--- | 
| **buildScript(files)** | **Must override.**  Called by executeTask() for datasets with forward reads only.  The return type is a list of lists.  Each nested list contains the bash script lines required to process 1 sample.  Obtains sequence files from getInputFiles(). |
| buildScriptForPairedReads(files) | Calls back to buildScript(files) by default.  Subclasses override this method to generate unique scripts for datasets containing paired reads.  |
| checkDependencies() | Called before executeTask() to validate *script.batchSize*, *script.exitOnError*, *script.numThreads*, *script.permissions*, *script.timeout* |
| getJobParams() | Return shell command to execute the MAIN script. |
| getScriptDir() | Return module script directory. |
| getSummary() | Adds the script directory summary to super.getSummary().  Most modules override this method by adding module specific summary details to super.getSummary(). |
| getTimeout() | Return *script.timeout*. |
| getWorkerScriptFunctions() | Return bash script lines for any functions needed in the worker scripts. |

#### [JavaModuleImpl](https://msioda.github.io/BioLockJ/docs/biolockj/module/JavaModuleImpl.html) extends [ScriptModuleImpl](https://msioda.github.io/BioLockJ/docs/biolockj/module/ScriptModuleImpl.html): superclass for pure Java modules.
 
To avoid running code on the cluster head node, a temporary instance of BioLockJ is spawned on a cluster node which is launched by the sole worker script from the job queue. 

| Method | Description | 
| :--- | :--- | 
| **runModule()** | **Must override.**  Executes core module logic. |
| buildScript(files) | This method returns a single line calling java on the BioLockJ source code, passing -d parameter to run in direct mode and the full class name of the [JavaModule](https://msioda.github.io/BioLockJ/docs/biolockj/module/JavaModule.html) to indicate the module to run. |
| getSource() | Determines if running code from Jar or source code in order to write valid bash script lines. |
| getTimeout() | Return *java.timeout*. |
| moduleComplete() | Create the script success indicator file. |
| moduleFailed() | Create the script failures indicator file. |

#### [ClassifierModuleImpl](https://msioda.github.io/BioLockJ/docs/biolockj/module/classifier/ClassifierModuleImpl.html) extends [ScriptModuleImpl](https://msioda.github.io/BioLockJ/docs/biolockj/module/ScriptModuleImpl.html): biolockj.module.classifier superclass.
| Method | Description | 
| :--- | :--- | 
| buildScriptForPairedReads(files) | Called by executeTask() for datasets with paired reads.  The return type is a list of lists, where each nested list contains the bash script lines required to process 1 sample.  Obtains sequence files from [SeqUtil](https://msioda.github.io/BioLockJ/docs/biolockj/util/SeqUtil.html).getPairedReads(getInputFiles()). |
| checkDependencies() | Validate Configuration properties *exe.classifier* and *exe.classifierParams*, verify sequence file format, log classifier version info, and verify no biolockj.module.seq modules are configured run after the [ClassifierModule](https://msioda.github.io/BioLockJ/docs/biolockj/module/classifier/ClassifierModule.html). Subclasses should call super.checkDependencies() if overriding this method to retain these verifications. | 
| executeTask() | Call buildScript(files) or buildScriptForPairedReads(files) based input sequence format and calls [BashScriptBuilder](https://msioda.github.io/BioLockJ/docs/biolockj/util/BashScriptBuilder.html) to generate the main script + 1 worker script for every *script.batchSize* samples.  To change the batch scheme, override this method to call the alternate [BashScriptBuilder](https://msioda.github.io/BioLockJ/docs/biolockj/util/BashScriptBuilder.html).buildScripts() method signiture and hard code the batch size.  All biolockj.module.classifier modules override this method. |
| getClassifierExe() | Return Configuration property *exe.classifier* to call the classifier program in the bash scripts.  If the classifier is not included in *cluster.modules*, validate that value is a valid file path.  If *exe.classifier* is undefined, replace the property prefix *exe* with the lowercase prefix of the module class name (less the standard module suffix *classifier*).  For example, use *rdp.classifier* for [RdpClassifier](../module/classifier/module.classifier.r16s#rdpclassifier) and *kraken.classifier* for [KrakenClassifier](../module/classifier/module.classifier.wgs#krakenclassifier).  This allows users to define all classifier programs in a default Configuration file rather than setting *exe.clssifier* in each project Configuration file. |
| getClassifierParams() | Return Configuration property *exe.classifierParams* which may contain a list of parameters (without hyphens) to pass to the classifier program in the bash scripts.  If *exe.classifierParams* is undefined, replace the property prefix *exe* with the lowercase prefix of the module class name as described for *exe.classifier*. |
| getSummary() | Adds input directory summary to super.getSummary().  Most modules override this method to add module specific summary details to super.getSummary(). |
| logVersion() | Run *exe.classifier* --version to log version info.  [RDP](../module/classifier/module.classifier.r16s#rdpclassifier) overrides this method to return null since the version switch is not supported. |

#### [ParserModuleImpl](https://msioda.github.io/BioLockJ/docs/biolockj/module/implicit/parser/ParserModuleImpl.html) extends [JavaModuleImpl](https://msioda.github.io/BioLockJ/docs/biolockj/module/JavaModuleImpl.html): biolockj.module.implicit.parser superclass.
| Method | Description | 
| :--- | :--- |
| **parseSamples()** | **Must override.**  Called by executeTask() to populate the Set returned by getParsedSamples().  Each classifier requires a unique parser module to decode its output.  This method should iterate through the classifier reports to build [OtuNode](https://msioda.github.io/BioLockJ/docs/biolockj/node/OtuNode.html)s for each sample-OTU found in the report.  The [OtuNode](https://msioda.github.io/BioLockJ/docs/biolockj/node/OtuNode.html)s are stored in a [ParsedSample](https://msioda.github.io/BioLockJ/docs/biolockj/node/ParsedSample.html) and cached via addParsedSample([ParsedSample](https://msioda.github.io/BioLockJ/docs/biolockj/node/ParsedSample.html)). |
| addParsedSample( sample ) | Add the [ParsedSample](https://msioda.github.io/BioLockJ/docs/biolockj/node/ParsedSample.html) to the Set returned by getParsedSamples(). |
| buildOtuTables() | Generate OTU abundance tables from [ClassifierModule](https://msioda.github.io/BioLockJ/docs/biolockj/module/classifier/ClassifierModule.html) output. |
| checkDependencies() | Validate Configuration properties (*report.minOtuCount*, *report.minOtuThreshold*, *report.logBase*) and  verify no biolockj.module.classifier modules are configured to run after the [ParserModule](https://msioda.github.io/BioLockJ/docs/biolockj/module/implicit/parser/ParserModule.html). | 
| executeTask() | If *report.numHits*=Y, add "Num_Hits" column to metadata containing the number of reads that map to any OTU for each sample.  Calls buildOtuTables() to generate module output. |
| getParsedSample(id) | Return the [ParsedSample](https://msioda.github.io/BioLockJ/docs/biolockj/node/ParsedSample.html) from the the Set returned by getParsedSamples() for a given id. |
| getParsedSamples() | Return 1 [ParsedSample](https://msioda.github.io/BioLockJ/docs/biolockj/node/ParsedSample.html) for each classified sample in the dataset. |


#### [OtuNodeImpl](https://msioda.github.io/BioLockJ/docs/biolockj/node/OtuNodeImpl.html) is the superclass for the [biolockj.node](https://msioda.github.io/BioLockJ/docs/biolockj/node/package-summary.html) package.
| Method | Description | 
| :--- | :--- | 
| addOtu(level, otu) | A node represents a single OTU, each level in the taxonomic hierarchy is populated with this method. |
| getCount() | Get the OTU count. |
| getLine() | Get the classifier report line used to create the node. |
| getOtuMap() | This map may contain 1 element for each of the *report.taxonomyLevels* and is populated by addOtu(level, otu).  |
| getSampleId() | Get the sample ID to which the OTU belongs. |
| report() | Print node info to log file as DEBUG line - not visible unless *pipeline.logLevel=DEBUG*.  |
| setCount(num) | Set the OTU count. |
| setLine(line) | Set the classifier report line used to create the node. |
| setSampleId(id) | set the sample ID to which the OTU belongs. |

* [OtuNodeImpl](https://msioda.github.io/BioLockJ/docs/biolockj/node/OtuNodeImpl.html) methods do not need to be overridden.  
* New [OtuNode](https://msioda.github.io/BioLockJ/docs/biolockj/node/OtuNode.html) implementations should call existing methods from their constructor.

## Share your module

Other users can use your module by putting the compiled class in the java classpath when they run BioLockj and using #BioModule **classname** in their config file.

You can share your module as code or as a compiled class any way that you like.  The official repository for external BioLockJ modules is [blj_ext_modules](https://github.com/IvoryC/blj_ext_modules).  Each module has a folder at the top level of the repository and should include the java code as well a config file to test the module alone, a test file to run a multi-module pipeline that includes the module, and (where applicable) a dockerfile.
