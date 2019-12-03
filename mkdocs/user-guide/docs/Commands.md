### BioLockJ commands are located under $BLJ/script

| Command | Description |
| :-- | :-- |
| **[biolockj](https://github.com/msioda/BioLockJ/blob/master/script/biolockj?raw=true)** [config_path] | Start a pipeline using the [[Configuration]] properties in [config_path].<br>Pipeline output directory is created under [$BLJ_PROJ](https://github.com/msioda/BioLockJ/blob/master/script/blj_config?raw=true). |
| **[blj_build](https://github.com/msioda/BioLockJ/blob/master/script/blj_build?raw=true)** | Run [build.xml](https://github.com/msioda/BioLockJ/blob/master/resources/build.xml?raw=true) ant script to build BioLockJ.jar from Java source code. |
| **[blj_complete](https://github.com/msioda/BioLockJ/blob/master/script/blj_complete?raw=true)** | Manually completes the current module and pipeline status. |
| **[blj_config](https://github.com/msioda/BioLockJ/blob/master/script/blj_config?raw=true)** | The [install](https://github.com/msioda/BioLockJ/blob/master/install?raw=true) script updates ~/.bash_profile to call [blj_config](https://github.com/msioda/BioLockJ/blob/master/script/blj_config?raw=true).<br>Adds BioLockJ [[Commands]] into your $PATH & sets program variables:<br> $BLJ:  BioLockJ application directory.<br> $BLJ_SCRIPT: BioLockJ script directory containing executable script files.<br> $BLJ_PROJ: Root pipeline directory used in multiple [[Commands]]. |
| **[blj_downlaod](https://github.com/msioda/BioLockJ/blob/master/script/blj_download?raw=true)** | If on cluster, print command syntax to download current or most recent [$BLJ_PROJ](https://github.com/msioda/BioLockJ/blob/master/script/blj_config?raw=true) pipeline analysis to your local workstation directory: *pipeline.downloadDir*. |
| **[blj_functions](https://github.com/msioda/BioLockJ/blob/master/script/blj_functions?raw=true)** | This script contains common functions used in BioLockJ. |
| **[blj_go](https://github.com/msioda/BioLockJ/blob/master/script/blj_go?raw=true)** | Go to most recent [$BLJ_PROJ](https://github.com/msioda/BioLockJ/blob/master/script/blj_config?raw=true) pipeline & list contents. |
| **[blj_log](https://github.com/msioda/BioLockJ/blob/master/script/blj_log?raw=true)** | Tail last 1K lines from current or most recent [$BLJ_PROJ](https://github.com/msioda/BioLockJ/blob/master/script/blj_config?raw=true) pipeline log file. |
| **[blj_reset](https://github.com/msioda/BioLockJ/blob/master/script/blj_reset?raw=true)** | Reset pipeline status to incomplete.<br>If restarted, execution will start with the current module.  |
| **[blj_summary](https://github.com/msioda/BioLockJ/blob/master/script/blj_summary?raw=true)** | Print current or most recent [$BLJ_PROJ](https://github.com/msioda/BioLockJ/blob/master/script/blj_config?raw=true) pipeline summary. |
