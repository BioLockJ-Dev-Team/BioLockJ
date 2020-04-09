# Docker                   
                   
Docker is a powerful tool in creating reproducible results.                   
                   
| Property| Description |
| :--- | :--- |
| *docker.imageName* | _string_ <br>The name of a docker image to override whatever a module says to use.<br>*default:*  *null* |
| *docker.imageTag* | _string_ <br>indicate specific version of Docker images<br>*default:*  *null* |
| *docker.imgOwner* | _string_ <br>name of the Docker Hub user that owns the docker containers<br>*default:*  *null* |
| *docker.saveContainerOnExit* | _boolean_ <br>if ture, docker run command will NOT include the --rm flag<br>*default:*  *null* |
                   
                   
All BioLockJ modules are intended to be compatable with a docker environment.  Each module has a default docker image; an environment where the module has been tested and that can spun up again for future use.  This can be altered by the user.                   
