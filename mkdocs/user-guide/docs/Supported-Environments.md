# Notes about environments

The main BioLockJ program can be used in these environments: 

* a local machine with a unix-like system (ie, linux or Mac, some features require a bash shell)
* a local machine with a unix-like system running docker * 
* (coming soon) Windows 10 running docker *
* a cluster, running a supported scheduler such as torque
* (coming soon) any machine running docker (see [Working in Pure Docker](Pure-Docker.md))

(* The launch scripts will still be run from your local machine, this requires java, but not a bash shell)

The launch process requires a unix-like environment.  This includes linux, macOS, or an ubuntu environment running on Windows. Windows support is still in development.

If using **docker**, you will need to run the install script to create the variables used by the launch scripts, even though the main BioLockJ program will run within the biolockj_controller container.

If using **AWS**, you will need to run the install script to create the variables used by the launch scripts, even though the main BioLockJ program will run on AWS. This is still experimental.

If you are using BioLockJ on a shared system where another user has already installed BioLockJ, **you will need to run the install script** to create the required variables in your own user profile.

There is also the option to run purely in docker, without installing even the launch scripts on your local machine.  However this is considered a niche case scenario and not well supported.

The helper commands (such as `cd-blj`) assume a bash shell, though others may also work.
To see what shell you currently using, run `echo $0`.
**If** you are not in a bash shell, you can change your current session to a bash shell, run `chsh -s /bin/bash`.

There is also the option to run purely in docker, without installing even the launch scripts on your local machine.  However this is considered a niche case scenario and not well supported.

## Choosing an environment

The major resources that come together in a pipeline are:

 * data (project data and reference data)
 * compute resources (memory, ram, cpu)
 * key executables

In theory, you could install all the tools you need on your laptop; put your data on your laptop, and run your whole analysis on your laptop.  This would be a "local" pipeline; a single compute node is handling everything.

However, in practice, a single machine typically doesn't have enough compute resources to run a modern bioinformatics pipeline in a realistic time frame; and the tools may be difficult to install, or even impossible to install on a given system.  

Docker provides key executables by packaging them into containers.  After the initial hurdle of installing docker itself, the 'install' of executables that are available in docker images is trivial, and they produce very consistent results; even when different steps in your pipeline have conflicting system requirements.
The underlying tools for all modules packaged with the main BioLockJ program are available via docker containers.  Docker is the most recommended way to run a pipeline.  However, these executables still have to come together with some compute resources.

A computer cluster offers large amounts of compute resources and plenty of storage.  Some clusters also have administrators (or other users) who will install tools for you and mechanisms for you to install tools yourself.  Downsides: cluster systems have their own idiosyncrasies and not everyone has access to one.

AWS provides large amounts of compute resources and interfaces very well with docker and with S3 for convenient and efficient data storage. Downsides: *costs money for each use*; has its own learning curve.

## Tested environments

We try make our software as system-agnostic as possible; but it is impossible to verify every possible stack of hardware / operating system / software.  We test multiple environments for each release, and we try to represent the resources of our user base.

### Release Testing

The most recent release testing was done on:

 * locally on **Mac** OS Version 10.15.7 (2.2 GHz 6-Core Intel Core i7, 16 GB 2400 MHz DDR4)
 * through **docker on Mac** OS Version 10.15.7 (same as above), using docker desktop Version 2.5.0.0
 * locally and as a **cluster** using Red Hat Enterprise Linux Server 7.5 (Maipo)
 * through **docker on Windows** Version 1909 using docker desktop version 3.0.0 and Ubuntu 20.04.1 using WSL2


### Anecdotal tests

BioLockJ has run successfully on:

 * locally on Mac OS Version 10.15.5 (1.7 GHz Quad-Core Intel Core i7, 16 GB 2133 MHz LPDDR3)
 * through docker on Mac OS Version 11.1 (2.3GHz Quad-Core Intel Core i5, 8 GB 2133MHz LPDDR3) with docker Version 3.0.3 
 * through docker on Windows 10 Education version 1909; Intel Core-i7 6700K CPU @ 4.00 GHZ (hand-built box); ubuntu 20.04 and docker 20.10.0


