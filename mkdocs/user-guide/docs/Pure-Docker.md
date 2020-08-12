
## Pure Docker (experimental)

This option is still in the experimental stages.

If you are running from any system that supports docker, you can run all commands through docker containers.

This assumes that you have docker up and running. 
Double check that docker is working on your system:
```
docker run hello-world
```

For Windows systems, you will need to run PowerShell **as administrator**.

For all systems, notice that many of these commands require a full path.  Here we use **$PWD** so that the commands in code blocks can be copy/pasted.  Be mindful of your current working directory.

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
biolockjdevteam/biolockj_controller:latest setup_workspace
```

**PowerShell**                          
```
mkdir workspace
docker run --rm `
-v /var/run/docker.sock:/var/run/docker.sock `
-v $PWD\workspace:/workspace `
-e HOST_OS_SCRIPT=ps1 `
biolockjdevteam/biolockj_controller:latest setup_workspace
```

This will create your docker preamble command.  
The docker preamble passes your biolockj commands into a docker environment.
The command is saved as a docker-wrapper script.

## Step 2:

Make your system treat the docker-wrapper script as an executable file.

You could choose to name the alias something other than "biolockj" if you also call the biolockj command locally and want to avoid ambiguity. 

Notice that these commands use **$PWD** with the assumption that your working directory has not changed since step 1.

**Mac / unix**     

Depending on your system, you may use `~/.bash_profile` or `~/.bashrc` or `~/.zshrc`, etc.
```
echo 'PATH='"$PWD"'/workspace/script:$PATH' >> ~/.bash_profile
```

(optional) Aliases are not universally supported; but where they are supported they are convenient and you will be able to copy/paste example commands that designed for local system calls.
```
echo 'alias biolockj='"$PWD"'/workspace/script/docker-biolockj' >> ~/.bash_profile
echo 'alias biolockj-api='"$PWD"'/workspace/script/docker-biolockj-api' >> ~/.bash_profile
```                     

Start a new session, or simply source your profile:
```
. ~/.bash_profile
```


**PowerShell**                          
Allow powershell to execute scripts created on this machine:
```
Set-ExecutionPolicy RemoteSigned
```
This only affects the current session:
```
Set-Alias -Name biolockj -Value $PWD\workspace\script\docker-biolockj.ps1
Set-Alias -Name biolockj-api -Value $PWD\workspace\script\docker-biolockj-api.ps1
```
This makes the command available to future sessions:
```
Add-Content $profile "Set-Alias -Name biolockj -Value $PWD\workspace\script\docker-biolockj.ps1"
Add-Content $profile "Set-Alias -Name biolockj-api -Value $PWD\workspace\script\docker-biolockj-api.ps1"
```
_If that command throws an error “could not find part of the path”, you may need to create the parent folder and try again, for example:_
`mkdir $HOME\Documents\WindowsPowerShell\`


## Step 3: (optional)

Run the test pipeline.

**Mac / unix AND PowerShell**                          
This command does NOT use $PWD.  If you completed step 2, then this is how the command will look regardless of your system, or where you are in the file system.

Notice that the path to the property file is given from the /workspace/, and all file-separators are “/”.
```
biolockj /workspace/templates/myFirstPipeline/myFirstPipeline.properties
```

If you did not complete step 2, then you will need to give the full path to the docker-wrapper, or even copy the command from there to the terminal.


## Ever after:

Use biolockj exactly the way that it is described in the rest the documentation WITH THESE EXCEPTIONS:             

 - The path to your properties file (or restart dir) given in your biolockj command must be under `/workspace/` (the workspace directory you created in step 1). The path can be given by starting with "/workspace/" or by using the 'dockerified' absolute file path.
 - If you used a different name for your alias in step 2 (or chose not use the alias at all), you will call the `docker-biolockj` script (or `docker-biolockj.ps1` script) in place of “biolockj”.

File paths in your property file _CAN_ use:     

 - (recommended) The “./” relative path format relative to the property file's directory. Relative paths should always use / as the file separator, regardless of host system.  For files in the project folder, file paths given in this format will work consistenetly when the pipeline is run locally, in docker, or on the cloud; and when the project is copied to a different location in the file system or to a different machine.
 - Full file paths using dockerified file paths: 
      - (mac) /Users/user/Documents/file.txt
      - (windows) /host_mnt/c/Users/user/Documents/file.txt
 - Full file paths using your native system (work in progress)
      - (mac) /Users/user/Documents/file.txt
      - (windows) C:\Users\user\Documents\file.txt
 - Variables that are defined in the property file. 
      - VAR=/Users/user/Documents/file.txt
      - file.path=${VAR}/data/file.txt

### Tips for the pure-docker user:   

Your property file may include file paths that are not under the project, or even under the workspace directory; but you will need to make sure that docker is configured to share those folders.  It is generally simpler to keep everything under the workspace folder.

Use `biolockj-api listMounts --config /workspace/<path/to/file>` to see what is going to be mapped in based on a given property file. If any paths are missing, you may need to add them to your file sharing preferences.

### Developers working in pure-docker:

After making changes to the source code, build the program by building the docker image. Use `$BLJ/resources/docker/docker_build_scripts/buildDockerImages.sh controller` if you can use that script, or copy/paste the build commands from it. This will make a local copy of the docker image with the compiled changes. The image will be tagged with the current development version number (one ahead of the current public release).  This is independent of any build you might have on your local system.
