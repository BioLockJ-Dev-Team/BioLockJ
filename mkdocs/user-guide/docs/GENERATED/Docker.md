# Docker                   
                   
Docker is a powerful tool in creating reproducible results.                   
                   
| Property| Description |
| :--- | :--- |
| *docker.imageName* | _string_ <br>The name of a docker image to override whatever a module says to use. Only use the module-specific-override form of this property.<br>*default:*  *null* |
| *docker.imageOwner* | _string_ <br>name of the Docker Hub user that owns the docker containers Only use the module-specific-override form of this property.<br>*default:*  *null* |
| *docker.imageTag* | _string_ <br>indicate specific version of Docker images Only use the module-specific-override form of this property.<br>*default:*  *null* |
| *docker.mountSock* | _boolean_ <br>should /var/run/docker.sock be mounted for modules.<br>*default:*  N |
| *docker.saveContainerOnExit* | _boolean_ <br>If Y, docker run command will NOT include the --rm flag<br>*default:*  *null* |
| *docker.verifyImage* | _boolean_ <br>In check dependencies, run a test to verify the docker image.<br>*default:*  *null* |
                   
                   
All BioLockJ modules are intended to be compatable with a docker environment.  Each module has a default docker image; an environment where the module has been tested and that can spun up again for future use.  This can be altered by the user.                   
