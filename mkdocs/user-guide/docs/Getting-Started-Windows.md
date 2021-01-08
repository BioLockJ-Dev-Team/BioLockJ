
## BioLockJ on Windows

_Currently, windows is not officially supported as a system to run BioLockJ_

There are a few avenues for running BioLockJ on a Widows machine:

### 1) Linux subsystem

In essence, install a linux subsystem and use that.  Follow the standard instructions to for [Getting Started](Getting-Started.md) with BioLockJ.  




### 2) Docker with a native launch

In this case, java running on the host machine is required to launch the program; but the manager process and the required environment to run each module is all handled by Docker containers.  

This feature exists, but is still experimental.  It has been shown to work, but is not guaranteed to work.






### 3) Pure-Docker

In the pure-docker case, a handful of powershell commands are used to launch a Docker container.  The Docker container includes all the required software and environment, including java to run the launch process and manager process, and the required environment to run each module.

This feature exists, but is still experimental.  It has been shown to work anecdotally, but is not guaranteed to work.  See [Working in Pure Docker](Pure-Docker.md).


