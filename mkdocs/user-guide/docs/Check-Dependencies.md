
BioLockJ is designed find all problems in one sitting.  Every module includes a _check dependencies_ method, which quickly detects issues that would cause an error during execution.  This is run for all modules in a pipeline _before_ the first module executes.  

When BioLockJ runs, it has three major phases:      

 * pipeline formation - string together the modues specified in the config file along with any additional modules that the program adds on the users behalf; and initiate the utilities needed for the pipeline (such as docker, metadata, determine input type).
 * check dependencies - scan the pipeline for anything that may cause an error during execution
 * run pipeline - execute each module in the sequence.


## Precheck a pipeline

By including the `--precheck-only` argument (or `-p`) when running `biolockj`; you are running in **precheck** mode.  BioLockJ will do the first two phases, and then stop.  This allows you to quickly test changes to your pipeline configuration without actually running a pipeline. It also allows you to see any modules that are automatically added to your pipeline.
