
## What environements are supported

The main BioLockJ program can be used in these environments: 

* a local machine with a unix-like system
* any machine running docker *
* a cluster, running a scheduler like torque
* AWS cloud computing *

(* The launch scripts will still be run from your local machine.)

AWS is currently experimental. To ty it, see [Working in Pure Docker](Pure-Docker.md)

In all cases, the launch process requires a unix-like environment.  This includes linux, macOS, or an ubuntu environment running on Windows.

There is also the option to run purely in docker, without installing even the launch scripts on your local machine.  However this is considered a nitch case scenario and not well supported.

## Choosing an environment

The major resources that come together in a pipeline are:

 * data (project data and reference data)
 * compute resources (mem, ram, cpu)
 * key executables

In theory, you could install all the tools you need on your laptop; put your data on your laptop, and run your whole analysis on your laptop.  This would be a "local" pipeline; a single compute node is handling everything.

However, in practice, a single machine typically doesn't have enough compute resources to run a modern bioinformatics pipeline in a realistic time frame; and the tools may be difficult to install, or even impossible to install on a given system.  

Docker provides key executables by packaging them into containers.  After the initial hurdle of installing docker itself, the 'install' of executables that are available in docker images is trivial, and they produce very consistent results; even when different steps in your pipeline have conflicting system requirements.
The underlying tools for all modules packaged with the main BioLockJ program are available via docker containers.  Docker is the most recommended way to run a pipeline.  However, these executables still have to come together with some compute resources.

A computer cluster offers large amounts of compute resources and plenty of storage.  Some clusters also have adminstrators (or other users) who will install tools for you and mechanisms for you to install tools yourself.  Downsides: cluster systems have their own idiosyncrasies and not everyone has access to one.

AWS provides large amounts of compute resources and interfaces very well with docker and with S3 for convenient and efficient data storage. Downsides: *costs money for each use*; has its own learning curve.
