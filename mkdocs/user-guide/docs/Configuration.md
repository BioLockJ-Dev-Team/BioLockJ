A configuration file encapsulates an analysis pipeline.

BioLockJ takes a single configuration file as a runtime parameter.  

```bash
biolockj config.properties
```

Every line in a BioLockJ configuration file is one of:      

 * BioModule (line starts with `#BioModule`)
 * comment (all other lines that start with `#`, has no effect)
 * property (`name=value`)


## BioModule execution order

To include a BioModule in your pipeline, add a `#BioModule` line to the top your configuration file, as shown in the examples found in [templates]( https://github.com/msioda/BioLockJ/tree/master/resources/config/template ).  Each line has the `#BioModule` keyword followed by the path for that module.  For example:

```
#BioModule biolockj.module.seq.PearMergeReads
#BioModule biolockj.module.classifier.wgs.Kraken2Classifier
#BioModule biolockj.module.report.r.R_PlotMds
```

This line is given at the top of the user guide page for each module.

BioModules will be executed in the order they are listed in here.  

A typical pipeline contians one [classifier module](../module/classifier/module.classifier).  Any number of [sequence pre-processing](Build-in-modules#sequence-modules) modules may come before the classifier module. Any number of [report modules](../module/report/module.report) may come after the classifier module.  In addition to the BioModules specified in the configuration file, BioLockJ may add [implicit modules](../module/implicit/module.implicit) that the are required by specified modules.  See [Example Pipeline](Example-Pipeline).

A module can be given an alias by using the `AS` keyword in its execution line:
```
#BioModule biolockj.module.seq.PearMergeReads AS Pear
```
This is is generally used for modules that are used more than once in the same pipeline.  Given this alias, the folder for this module will be called `01_Pear` instead of `01_PearMergeReads`, and any general properties directed to this module would use the prefix `Pear` instead of `PearMergedReads`. An alias must start with a capital letter, and cannot duplicate a name/alias of any other module in the same pipeline.  


## Properties

Properties are defined as name-value pairs. List-values are comma separated. Leading and trailing whitespace is removed so "propName=x,y" is equivalent to "propName = x, y".

See the [list of available properties](GENERATED/General-Properties.md).


### Variables

Bash variables can be referenced in the config. They must be "fully dressed": `${VAR}`          
                    
There are two variables that BioLockJ requires:            
`BLJ` is the file path to the BioLockJ direcotry and                
`BLJ_PROJ` is the directory where pipelines created by BioLockJ are stored and run.            
After installation these are defined in the shell profile.  These can referenced in the config file.

The `~` ("tilde") is replaced with `${HOME}` if (and only if) the ~ is the *first* character.

Variables can also be defined in the config file and referenced in the same way:
```text
DIR=/path/to/big/data/dir                    
sra.destinationDir=${DIR}/seqs  
sra.sraAccList=${DIR}/SraAccList.txt      
input.dirPaths=${DIR}/seqs      
```

If you are referencing environment variables *and* running in docker, you will need to use the -e parameter to biolockj to pass the variables into the docker environment (even if the variable is defined in the config file). For example:          
```
biolockj --docker -e SHEP=$SHEP,DIR=/path/to/big/data/dir config.properties
```    

### Relative file paths

File paths can be given using relative paths.  The path should start with `./`.         
The location `.` is interpreted as being the directory where the configuration file is.        


Example file structure:
```
 /users/joe/analysis01/
                       config.properties 
                       metadata.txt
                       /sra/
                            SraAccList.txt
```
Properties in config.properties can use relative paths:
```text
metadata.filePath=./metadata.txt                    
sra.sraAccList=./sra/SraAccList.txt                   
``` 

Note: `../` is also supported but it does not stack ( ../../../data/ is not supported). 

With this design, the "analysis01" folder could be shared or moved and the configuration file would not need to be updated to reflect the new location of the project files it references.

## Special properties

Some properties invoke special handling.

#### pipeline.defaultProps

`pipeline.defaultProps` is a handled before any other property.  It is used to link another properties file.  The properties from that file are added to the MASTER set. The pipeline.defaultProps property itself is not included in the MASTER properties set.

#### Module-specific forms

Many pipeline properties (usually those used by pipeline utilities) can be directed to a specific module.  For example, `script.numThreads` is a general property that specifies that number of threads alloted to each script launched by any module; and `PearMergeReads.numThreads` overrides that property ONLY for the PearMergeReads module.  

#### exe.* properties

`exe.` properties are used to specify the path to common executables.  Modules are sometimes written to use a common tool, such as `Rscript` or `bowtie`.  These modules will write scripts with the assumption that this command is on the `$PATH` when the script is executed UNLESS `exe.Rscript` is given specifying a path to use.  The `exe.` properties are often specified in a defaultProps file for a given environment rather than in individual project properties files.  

If you are running a pipeline using docker, it is assumed that all file paths in your config file are written in terms of your host machine. The _EXCEPTION_ to this is the `exe.` file paths. Most often, docker containers are used because of the executables baked into them. In the rare case where you want to use an executable from your local machine, while running a pipeline in docker, you can specify this by using the prefix `hostExe.` in place of `exe.`.


### Chaining configuration files

Although all properties can be configured in one file, we recommend chaining default files through the pipeline.defaultProps option. This can often improve the portability, maintainability, and readability of the project-specific configuration files.  

#### Standard Properties

  * BioLockJ will always apply the [standard.properties](https://github.com/msioda/BioLockJ/blob/master/resources/config/default/standard.properties?raw=true) file packaged with BioLockJ under [resources/config/default/](https://github.com/msioda/BioLockJ/tree/master/resources/config/default); you do not need to specify this file in your pipeline.defaultProps chain.
  * IFF running a pipeline in docker, then BioLockJ will apply the [docker.properties](https://github.com/msioda/BioLockJ/blob/master/resources/config/default/docker.properties?raw=true) file packaged with BioLockJ under [resources/config/default/](https://github.com/msioda/BioLockJ/tree/master/resources/config/default).

#### User-specified Defaults
We recommend creating an **environment.properties** file to assign envionment-specific defaults.

  * Set [cluster](#cluster) & [script](#script) properties
  * Set paths to key executables through [exe](#exe) properties
  * Override standard.properties as needed.
  * This information is the same for many (or all) projects run in this environment, and entering the info anew for each project is tedious, time-consuming and error-prone.  

If using a shared system, consider using a user.properties file.   

  * Set user-specific properties such as download.dir and mail.to.
  * For shared projects, use a path that will be updated per-user, such as `~/biolock_user.properties`

Other logical intermediates my also present themselves.  For example, some group of projects may need to override several of the defaults set in environmment.properties, but others still use the those defaults.  Projects in this set can use `pipeline.defaultProps=group2.properties` and the group2.properties files may include `pipeline.defaultProps=environment.properties`

#### Project Properties
Create a new configuration file for each pipeline to assign project-specific properties:

  * Set the [BioModule execution order](#biomodule-execution-order)
  * Set `pipeline.defaultProps = environment.properties`
  * You may use multiple default config files:           
    `pipeline.defaultProps=environment.properties,groupSettings.properties`
  * Override environment.properties and standard.properties as needed
  * Example project configuration files can be found in [templates]( https://github.com/msioda/BioLockJ/tree/master/resources/config/template ). 

If the same property is given in multiple config files, the highest priority goes to the file used to launch the pipeline.  Standard.properties always has the lowest priority.

A copy of each configuration file is stored in the pipeline root directory to serve as primary project documentation.  

