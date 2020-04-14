
The specified script is executed using the modules script directory as the current working directory. A _scriptPath_ is required.  If specified, the _launcher_ program (ie R, Python) will be used.  If specified, any _param_ will be listed as arguments to the script.  If running in docker, _dockerContainerName_ is required.

This is ideal for:

 * Custom analysis for a given pipeline, such as an R or python script
 * Any steps where an appropriate BioLockJ module does not exist

Any step in your analysis process that might otherwise have to be done manually can be stored as a custom script so that the entire process is as reproducible as possible.

It is STRONGLY encouraged that users write scripts using common module conventions:

 * use relative file paths (starting with `.` or `..`)
 * put all generated output in the modules `output` directory (`../output`)
 * put any temporary files in the modules `temp` directory (`../tmep`).  
 * the main pipeline directory would be `../..`, and the output of a previous module such as `PearMergedReads` would be in `../../*_PearMergedReads/output`

To use the GenMod module multiple times in a single pipeline, use the `AS` keyword to direct properties to the correct instance of the module.

For example:
```
#BioModule biolockj.module.diy.GenMod AS Part1
#<other modules>
#BioModule biolockj.module.diy.GenMod AS Part2

Part1.launcher=python
Part1.script=path/to/first/script.py

Part2.script=path/to/bash/script/doLast.sh
```
With this, `script.py` will be run using python.  Then other modules will run. Then `doLast.sh` will be run using the default system (probably bash, unless it has a shebang line specifiying something else).
