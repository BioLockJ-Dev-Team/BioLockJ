
## BioLockJ on Windows

_Currently, windows is not officially supported as a system to run BioLockJ_

There are a few avenues for running BioLockJ on a Widows machine.  The WSL2 avenue is the most likely to be supported into the future.

### Windows Subsystem for Linux (WSL2)

**This option has been shown to work, but it is not rigorously tested in our current release testing process, and so is _not officially_ supported.**

#### 1. Install WSL2

Set up WSL2 on your machine and link it to your linux subsystem, see the [Microsoft documentation](https://docs.microsoft.com/en-us/windows/wsl/install-win10).

#### 2. Install linux distribution

For our tests, we chose the most recent Ubuntu distribution, see [tested environments](../Supported-Environments/#Tested-environments); presumably others also work.

#### 3. Install java

Install java in the linux subsystem.  
```
apt-get update
sudo apt install default-jre
```

#### 4. Install docker (with WSL2)

Set up docker to work with your linux subsystem on Windows.  <br>See [docker documentation](https://docs.docker.com/docker-for-windows/wsl/) <br>You'll need to be able to run the docker hello world example:

```bash
docker run hello-world

# Hello from Docker!
# This message shows that your installation appears to be working correctly.

```

#### 4. Install BioLockJ

Follow the standard instructions to for [Getting Started](Getting-Started.md) with BioLockJ, operating within the linux subsystem.  

_In theory, operating within this system will be identical to working with BioLockJ in a unix-like operating system.  However we recommend (and run tests) using docker, as this removes the added troubleshooting of adapting to subtle differences across environments, which could be compounded by the system stacking, not to mention the often tedious task of installing all dependencies for all pipelines._


---

### Alternative: Docker with a native launch

In this case, java running on the host machine is required to launch the program; but the manager process and the required environment to run each module is all handled by Docker containers.  

**This feature exists, but is still experimental.  It is not guaranteed to work.**

All code chunks in this section assume you are running PowerShell **as administrator**.

#### 1. Download the [latest release](https://github.com/BioLockJ-Dev-Team/BioLockJ/releases/latest) & unpack the tarball.  

Third party tools such as [7Zip](https://www.7-zip.org/) allow you to unzip tar files on Windows.

Save the uncompressed folder wherever you like to keep executables.
<br>**If** you choose to download the source code, you will need to compile it by running `ant` with the `build.xml` file in the `resources` folder. 

#### 2. Set PowerShell variables

In PowerShell, navigate (cd) into the BioLockJ folder, and run
```
Set-Variable -Name BLJ -Value $PWD
Add-Content $profile "Set-Variable -Name BLJ -Value $BLJ"
```

cd into a folder of your choice, such as C:Users\Documents\biolockj_pipelines, and run
```
Set-Variable -Name BLJ_PROJ -Value $PWD
Add-Content $profile "Set-Variable -Name BLJ_PROJ -Value $BLJ_PROJ"
```

Test the variables.
```
$BLJ
$BLJ_PROJ
```

**Note:** The Set-Variable lines apply to the current session; the Add-content lines apply to future sessions.  

If the Add-Conent lines throw an error to effect "could not find path", then you may need to create the parent folder and try again, for example:
```
$profile
## see file path of the profile:  $HOME\Documents\WindowsPowerShell\Microsoft.PowerShell_profile.ps1
mkdir $HOME\Documents\WindowsPowerShell\
```

#### 3. Set an alias for the biolockj executable.

```
Set-Alias -Name biolockj -Value $BLJ\script\run-biolockj.ps1
Add-Content $profile "Set-Alias -Name biolockj -Value $BLJ\script\run-biolockj.ps1"
```

Allow PowerShell to execute scripts on this machine:
```
Set-ExecutionPolicy RemoteSigned
```

Test that calling this alias makes a call to the BioLockJ program.
```
biolockj --version
biolockj --help
```
This should show the biolockj help menu.

Set an alias for the biolockj supporting tool: biolockj-api. 

```
Set-Alias -Name biolockj-api -Value $BLJ\script\run-biolockj-api.ps1
Add-Content $profile "Set-Alias -Name biolockj-api -Value $BLJ\script\run-biolockj-api.ps1"
```

Test that calling this alias makes a call to the BioLockJ program.
```
biolockj-api
```
This should show the biolockj-api help menu.

#### 4. Install docker

See the current instructions for installing docker on your system: 
<br>[https://docs.docker.com/get-started/](https://docs.docker.com/get-started/)
<br>You'll need to be able to run the docker hello world example:

```bash
docker run hello-world

# Hello from Docker!
# This message shows that your installation appears to be working correctly.

```

#### 5. Run test pipeline

When you run the program, you will see a pop-up window asking for permission to share specific folders. Say yes.<br>
If that does not appear, see [why doesn't my pipeline run in docker](../FAQ/#question-why-doesnt-my-pipeline-run-in-docker).

```bash
biolockj -d $BLJ\templates\myFirstPipeline\myFirstPipeline.properties
# 
# Docker container id: 336259e7d3b8d9ab2fa71202258b562664be1bf9645d503a790ae5e9da15ce97
# Initializing BioLockJ..
# Building pipeline:  /Users/joe/apps/BioLockJ/pipelines/myFirstPipeline_2020Jan17
# Fetching pipeline status 
# 
# Pipeline is complete.
```

---


### Alternative: Pure-Docker

In the pure-docker case, a handful of power-shell commands are used to launch a Docker container.  The Docker container includes all the required software and environment, including java to run the launch process and manager process, and the required environment to run each module.

**This feature exists, but is still experimental.  It has been shown to work anecdotally, but is not guaranteed to work.**

See [Working in Pure Docker](Pure-Docker.md).

