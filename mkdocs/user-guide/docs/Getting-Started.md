
## Installation and test

### Basic installation

The basic installation assumes a unix-like environment and a **bash** shell.
To see what shell you currently using, run `echo $0`.
**If** you are not in a bash shell, you can change your current session to a bash shell, run `chsh -s /bin/bash`.

#### 1. Download the [latest release](https://github.com/BioLockJ-Dev-Team/BioLockJ/releases/latest) & unpack the tarball.
```bash
tar -zxf BioLockJ-v1.2.7.tar.gz
```
Save the folder wherever you like to keep executables.
**If** you choose to download the source code, you will need to compile it by running `ant` with the `build.xml` file in the `resources` folder. 

#### 2. Run the install script 
The `install` script updates the $USER bash profile to call **[blj_config](https://github.com/msioda/BioLockJ/blob/master/script/blj_config?raw=true)**.  See **[Commands](../Commands)** for a full description of **[blj_config](https://github.com/msioda/BioLockJ/blob/master/script/blj_config?raw=true)**

```bash
cd BioLockJ*
./install
#     Saved backup:  /users/joe/.bash_profile~
#     Saved profile:  /users/joe/.bash_profile
# BioLockJ installation complete!
```

This will add the required variables to your path when you start your next session.<br>

```bash
exit # exit and start a new session
```

Start a new bash session and verify that `biolockj` is on your `$PATH`.  A new terminal window or a fresh log in will start a new session.

```bash
biolockj --version
biolockj --help
```

#### 3. Run the test pipeline
```bash
echo $BLJ
# /path/to/BioLockJ
biolockj ${BLJ}/templates/myFirstPipeline/myFirstPipeline.properties

# Initializing BioLockJ..
# Building pipeline:  /Users/joe/apps/BioLockJ/pipelines/myFirstPipeline_2020Jan17
# blj_go       -> Move to pipeline output directory
# blj_log      -> Tail pipeline log (accepts tail runtime parameters)
# blj_summary  -> View module execution summary
# Fetching pipeline status 
# 
# Pipeline is complete.
```
Notice the use of the `$BLJ` variable. This variable is created by the installation process; it points to the BioLockJ folder.

The myFirstPipeline project is the first in the tutorial series designed to introduce new users to the layout of a BioLockJ pipeline.

You should take a moment to [review your first pipeline](../Getting-Started#review-your-first-pipeline).


### Docker installation

#### 1. Install docker
See the current instructions for installing docker on your system: https://docs.docker.com/get-started/
<br>You'll need to be able to run the docker hello world example:

```bash
docker run hello-world

# Hello from Docker!
# This message shows that your installation appears to be working correctly.

```

#### 2. Turn on file sharing

Depending on your system and docker installation, this may be on by default.

File sharing, also called volume sharing, is what allows programs inside docker containers to interact with files stored on your computer. Dependong on your version of Docker Desktop, this setting may be under `Docker > Prefernces > File Sharing`, or `Preferences > Resources > File Sharing` or something similar. Make sure this feature is enabled.  Any file that must be read by any part of the BioLockJ pipeline must be under one of the share-enabled folders.  The BioLockJ Projects directory (BLJ_PROJ) must also be under one of these share-enabled folders.

#### 3. Download and install BioLockj
Follow the download and install steps in the [Basic Installation](../Getting-Started#Basic-Installation) instructions.

#### 4. Run the test pipeline _in docker_
```bash
biolockj --docker --blj ${BLJ}/templates/myFirstPipeline/myFirstPipeline.properties
# 
# Created "/Users/ieclabau/runDockerClone.sh" 
# This script will launch another instance of this docker image,
# with the same env vars + volumes, but in interactive mode.
# 
# Docker container id: 336259e7d3b8d9ab2fa71202258b562664be1bf9645d503a790ae5e9da15ce97
# Initializing BioLockJ..
# Building pipeline:  /Users/joe/apps/BioLockJ/pipelines/myFirstPipeline_2020Jan17
# blj_go       -> Move to pipeline output directory
# blj_log      -> Tail pipeline log (accepts tail runtime parameters)
# blj_summary  -> View module execution summary
# Fetching pipeline status 
# 
# Pipeline is complete.
```

You should take a moment to [review your first pipeline](../Getting-Started#review-your-first-pipeline).

### Cluster installation

Installing BioLockJ on a cluster follows the same process as the [Basic Installation](../Getting-Started#Basic-Installation).  EACH USER must run the `install` script in order to run the BioLockJ launch scripts.  Use the property `pipeline.env=cluster` in your pipeline configuration to take advantage of parallell computing through the cluster.


## Review your first pipeline

The variable `$BLJ_PROJ` points to your projects folder. See a list of all of the pipelines in your projects folder.  
```bash
ls $BLJ_PROJ
```
By default, `$BLJ_PROJ` is set to the "pipelines" folder in BioLockJ (`$BLJ/pipelines`).  To change this, add a line to your bash_profile (or equivilent file): `export BLJ_PROJ=/path/to/my/projects`.  This line must be _after_ the call to the blj_config script.

Look at your most recent pipeline:
```bash
blj_go
```

This folder represents the analysis pipeline that you launched when you called `biolockj` on the file _${BLJ}/templates/myFirstPipeline/myFirstPipeline.properties_.

Notice that the original configuration ("config") file has been copied to this folder. Review the config file that was used to launch this pipeline:
```bash
cat myFirstPipeline.properties
```

Notice that modules are specified in the config using the keyword `#BioModule`.  Each module in the pipeline creates a folder in the pipeline directory.  Notice that an additional module "00_ImportMetaData" was added automatically.

At the top level of the pipeline we see an empty **flag** file "biolockjComplete" which indicates that the pipeline finished successfully. While the pipeline is still in progress, the flag is "biolockjStarted"; and if the pipeline stopps due to an error, the flag is "biolockjFailed".

The `summary.txt` file is a summary of each module as it ran during pipeline execution.  This is the best place to start when reviewing a pipeline.

The file `"MASTER_myFirstPipeline_<DATE>.properties"` is the complete list of all properties used during this pipeline.  This includes properties that were set in the primary config file ("myFirstPipeline.properties"), and properties that are set as defaults in the BioLockJ program, and properties that are set in user-supplied default config files, which are specified in the primary config file using the `pipeline.defaultProps=` property.  This "MASTER_*.properties" file contains all of the settings required to reproduce this pipeline.  

If the pipeline was run using docker, a file named `dockerInfo.json` will show the container information.

The pipeline log file `"myFirstPipeline_<DATE>.log"` is an excellend resource for troubleshooting.

The `validation` has tables recording the MD5 sum for each output from each module.  If the pipeline is run again, this folder can be used to determine if the results in the new run are an exact match for this run.

Within each module's folder, we see the "biolockjComplete" flag (the same flags are used in modules and at the top level).  All output-procuing modules have a subfolder called `output`.  Most modules also have folders `script` and `temp`.  The output folder is used as input to down-stream modules.  Modules are the building blocks of pipelines.  For more information about modules, see [Built-in BioModules](Built-in-modules.md).



## Making your own pipline
Now that you have a working example, you can make your own pipeline.  
You may want to modify the example above, or look at others under `/templates`.

Things are seldom perfect the first time.  Its safe to assume you will make iterative changes to your pipeline configuration.  BioLockJ offers some tools to facilitate this process.

 * Check your pipeline using [precheck](Check-Dependencies.md) mode 

 * Add modules onto your partial pipeline using [restart](Failure-Recovery.md)

 * Look through the base set of [modules](Built-in-modules.md) and even [create your own](Building-Modules.md)

A recommended practice is to make a subset of your data, and use that to develope your pipeline. 


## Other notes for starting out

**Install any/all software [Dependencies](../Dependencies) required by the modules you wish to include in your pipeline.**

BioLockJ is a pipeline manager, desigend to integrate and manage external tools.  These external tools are not packaged into the BioLockJ program.  BioLockJ must run in an environment where these other tools have been installed **OR** run through docker using the docker images that have the tools installed.  The core program, and all modules packaged with it, have corresponding docker images.

### Notes about environments

The main BioLockJ program can be used in these environments: 

* a local machine with a unix-like system
* any machine running docker *
* a cluster, running a scheduler such as torque
* AWS cloud computing *

(* The launch scripts will still be run from your local machine.)

The launch process requires a unix-like environment.  This includes linux, macOS, or an ubuntu environment running on Windows.

If using **docker**, you will need to run the install script to create the variables used by the launch scripts, even though the main BioLockJ program will run within the biolockj_controller container.

If using **AWS**, you will need to run the install script to create the variables used by the launch scripts, even though the main BioLockJ program will run on AWS.

AWS is currently experimental. To ty it, see [Working in Pure Docker](Pure-Docker.md)

If you are using BioLockJ on a shared system where another user has already installed BioLockJ, **you will need to run the install script** to create the required variables in your own user profile.

There is also the option to run purely in docker, without installing even the launch scripts on your local machine.  However this is considered a nitch case scenario and not well supported.

For more information about the how/why to use each environment, see [Supported Environments](Supported-Environments.md)


### Shutting down a pipeline

BioLockJ will shut down appropriately on its own when a pipeline either completes or fails.  

_Sometimes_, it is necessary to shut down the program pre-maturely.

This is not an ideal exit and the steps depend on your environment.  The main program is terminated by killing the java process.  Any worker-processes that are still in progress will need to be shut down directly (or allowed to time out).

To kill the BioLockJ program on a local system, get the id of the java process and kill it:
```bash
ps
#  PID TTY          TIME CMD
#   1776 pts/0    00:00:00 bash
#   1728 pts/0    00:00:00 ps
#   4437 pts/0    00:00:00 java
kill 4437
```
On a local system, workers are under the main program.

To kill the BioLockJ program running in docker, get the ID of the docker container and use `docker stop`.
```bash
docker ps
# CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS              PORTS               NAMES
# f55a39311eb5        ubuntu              "/bin/bash"         16 minutes ago      Up 16 minutes                           brave_cori
docker stop f55a39311eb5
```
In a docker pipeline, the container IDs for workers will also appear under ps.
If you need to distinguish the BioLockJ containers from other docker containers running on your machine, you see a list of them in the current modules script directory in a file named `MAIN*.sh_Started`.

To kill the BioLockJ program that is run in the foreground (ie, the `-f` arg was used), then killing the current process will kill the program.  This is usually done with `ctrl`+`c` .

To kill the BioLockJ program on a cluster environment, use `kill` just like the local case to stop a process on the head node, and use `qdel` (or the equivilent on your scheduler) to terminate workers running on compute nodes.
