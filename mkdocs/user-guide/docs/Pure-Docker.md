
## Pure Docker (experimental)

This option is still in the experimental stages.

If you are running from any system that supports docker, you can run all commands through docker containers.

This assumes that you have docker up and running. Double check:
`docker run hello-world`

Notice that many of these commands require a full path.  Here we use **$PWD** so that the commands in code blocks can be copy/pasted.  Be mindful of your current working directory.

## Step 1: 
In powershell or terminal, navigate to a directory where you would like to store all of your BioLockJ materials, and enter the command below.

Docker may prompt you to grant permission to access the `workspace/` folder.
If docker does not allow you to map in the folder, and does not prompt you, you may need to open the file sharing section of your docker preferences and add this folder to the list of files docker is allowed to share.

**Mac / unix**                          
```
mkdir workspace
docker run --rm \
-v /var/run/docker.sock:/var/run/docker.sock \
-v $PWD/workspace:/workspace \
-e HOST_OS_SCRIPT=bash \
biolockjdevteam/biolockj_controller:v1.3.7 setup_workspace
```

**PowerShell**                          
```
mkdir workspace
docker run --rm `
-v /var/run/docker.sock:/var/run/docker.sock `
-v $PWD\workspace:/workspace `
-e HOST_OS_SCRIPT=ps1 `
biolockjdevteam/biolockj_controller:v1.3.7 setup_workspace
```

This will create your docker preamble command.  
The docker preamble passes your biolockj commands into a docker environment.
The command is saved as a docker-wrapper script.

## Step 2:  (recommended) 
If you choose to skip this step, you will need to either copy your docker-preamble from the script each time you call biolockj, or spell out the full path to the docker-wrapper script.

Make your system treat the docker-wrapper script as an executable file.
Notice that these commands use **$PWD** with the assumption that your working directory has not changed since step 1.


**Mac / unix**                          
(depending on your system, you may use `~/.bash_profile` or `~/.bashrc` or `~/.zshrc`, etc)
```
echo 'PATH='"$PWD"'/workspace/source:$PATH' >> ~/.bash_profile
. ~/.bash_profile
```


**PowerShell**                          
Allow powershell to execute scripts created on this machine:
```
Set-ExecutionPolicy RemoteSigned
```
This version only affects the current session:
```
Set-Alias -Name docker-biolockj -Value $PWD\workspace\source\docker-biolockj.ps1
Set-Alias -Name docker-biolockj-api -Value $PWD\workspace\source\docker-biolockj-api.ps1
```
This version makes the command available to future sessions:
```
Add-Content $profile "Set-Alias -Name docker-biolockj -Value $PWD\workspace\source\docker-biolockj.ps1"
Add-Content $profile "Set-Alias -Name docker-biolockj-api -Value $PWD\workspace\source\docker-biolockj-api.ps1"
```

_If that command throws an error “could not find part of the path”, you may need to create the parent folder and try again, for example:_
`mkdir $HOME\Documents\WindowsPowerShell\`



## Step 3: (optional)

Run the test pipeline.

**Mac / unix AND PowerShell**                          
This command does NOT use $PWD.  If you completed step 2, then this is how the command will look regardless of your system, or where you are in the file system.

Notice that the path to the config file is given from the /workspace/, and all file-separators are “/”.
```
docker-biolockj /workspace/source/myFirstPipeline/myFirstPipeline.properties
```

If you did not complete step 2, then you will need to give the full path to the docker-wrapper, or even copy the command from there to the terminal.


## Ever after:

Use biolockj exactly the way that it is described in the rest the documentation WITH THESE EXCEPTIONS:             

 1. You will call the `docker-biolockj` script in place of “biolockj”; likewise, use `docker-biolockj-api` in place of "biolockj-api".
 1. The path to your config file given in your biolockj command must start with /workspace/ referencing the workspace directory you created in step 1. The same is true if you give the path to a stopped pipeline to restart it.
 1. Paths in your config file cannot use environment variables from your local environment.  

File paths in your config file _CAN_ use:     

 - (recommended) The “./” relative path format relative to the config files directory. Relative paths should always use / as the file separator, regardless of host system.  For files in the project folder, file paths given in this format will work consistenetly when the pipeline is run locally, in docker, or on the cloud; and when the project is copied to a different location in the file system or to a different machine.
 - Full file paths using dockerified file paths: 
      - (mac) /Users/user/Documents/file.txt
      - (windows) /host_mnt/c/Users/user/Documents/file.txt
 - Full file paths using your native system (work in progress)
      - (mac) /Users/user/Documents/file.txt
      - (windows) C:\Users\user\Documents\file.txt
 - Variables that are defined in the config file. 
      - VAR=/Users/user/Documents/file.txt
      - file.path=${VAR}/data/file.txt


Tips for the pure-docker user:   

Use `docker-biolockj-api listMounts --config /workspace/<path/to/file>` to see what is going to be mapped in based on a given config file. If any paths are missing, you may need to add them to your file sharing preferences.

Developers working in pure-docker:

After making changes to the source code, build the program by building the docker image; use `$BLJ/resources/docker/docker_build_scripts/buildDockerImages.sh controller` if you can use that script, or copy/paste the build commands from it. This will make a local copy of the docker image with the compiled changes. The image will be tagged with the current development version number (one ahead of the current public release).  This is independent of any build you might have on your local system.
