The `biolockj` help menu:                   

```bash                   
biolockj --help                   
```                   

```bash                   
                   
BioLockJ v1.3.2-dev - UNCC Fodor Lab July 2018                   
Usage:                   
  biolockj [options] <config|pipeline>                   
                   
Options:                   
  -v  --version                 Show version                   
  -h  --help                    Show help menu                   
  -p  --precheck-only           Set up pipeline and check dependencies and then STOP;                   
                                do not execute the pipeline. This is helpful when testing edits to config files.                   
  -r  --restart                 Resume an existing pipeline                   
  -c  --config-override <file>  New config file (if restarting a pipeline)                   
      --password <password>     Encrypt password                   
  -d  --docker                  Run in docker                   
  -a  --aws                     Run on aws                   
  -g  --gui                     Start the BioLockJ GUI                   
  -f  --foreground              Run the java process in the foreground without nohup                   
  -w  --wait-for-start          Do not release terminal until pipeline completes check-dependencies step.                   
      --external-modules <dir>  Directory with compiled java code giving additional modules                   
      --blj                     Map $BLJ folder into the docker container;                   
                                this replaces BioLockJ packaged in a docker container with the local copy.                   
  -e  --env-var <var=val>       Environment variables to be passed to the BioLockJ environment.                   
                                Can be a comma-sep list. Values take the form: a=foo,b=bar,c=baz                   
      --blj_proj <dir>          Directory that contains BioLockJ pipelines. If not supplied,                    
                                biolockj will use the value of environment variable "BLJ_PROJ".                   
                   

```                   
