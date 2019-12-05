### BioLockJ commands are located under $BLJ/script

| Command | Description |
| :-- | :-- |
| **[biolockj](https://github.com/msioda/BioLockJ/blob/master/script/biolockj?raw=true)** [config_path] | Start a pipeline using the Configuration properties in [config_path].<br>Pipeline output directory is created under [$BLJ_PROJ](https://github.com/msioda/BioLockJ/blob/master/script/blj_config?raw=true). |
| **[blj_go](https://github.com/msioda/BioLockJ/blob/master/script/blj_go?raw=true)** | Go to most recent [$BLJ_PROJ](https://github.com/msioda/BioLockJ/blob/master/script/blj_config?raw=true) pipeline & list contents. |
| **[blj_log](https://github.com/msioda/BioLockJ/blob/master/script/blj_log?raw=true)** | Tail last 1K lines from current or most recent [$BLJ_PROJ](https://github.com/msioda/BioLockJ/blob/master/script/blj_config?raw=true) pipeline log file. |
| **[blj_summary](https://github.com/msioda/BioLockJ/blob/master/script/blj_summary?raw=true)** | Print current or most recent [$BLJ_PROJ](https://github.com/msioda/BioLockJ/blob/master/script/blj_config?raw=true) pipeline summary. |
| **[blj_complete](https://github.com/msioda/BioLockJ/blob/master/script/blj_complete?raw=true)** | Manually completes the current module and pipeline status. |
| **[blj_reset](https://github.com/msioda/BioLockJ/blob/master/script/blj_reset?raw=true)** | Reset pipeline status to incomplete.<br>If restarted, execution will start with the current module.  |
| **[blj_downlaod](https://github.com/msioda/BioLockJ/blob/master/script/blj_download?raw=true)** | If on cluster, print command syntax to download current or most recent [$BLJ_PROJ](https://github.com/msioda/BioLockJ/blob/master/script/blj_config?raw=true) pipeline analysis to your local workstation directory: *pipeline.downloadDir*. |
