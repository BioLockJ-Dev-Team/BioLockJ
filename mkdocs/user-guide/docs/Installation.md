#### 1. Download the [latest release](https://github.com/msioda/BioLockJ/releases/latest) & unpack the tarball.

        > tar -zxf BioLockJ-1.2.0-beta

Put the folder to wherever you like to keep executables.

If you choose to download the source code, you will need to compile it by running `ant` from the resources folder. 

#### 2. Run the install script 
* The **[install](https://github.com/msioda/BioLockJ/blob/master/install?raw=true)** script updates the $USER bash profile to call **[blj_config](https://github.com/msioda/BioLockJ/blob/master/script/blj_config?raw=true)**.  See **[[Commands]]** for a full description of **[blj_config](https://github.com/msioda/BioLockJ/blob/master/script/blj_config?raw=true)**

        > ./install
        >     Saved backup:  /users/msioda/.bash_profile~
        >     Saved profile:  /users/msioda/.bash_profile
        > BioLockJ installation complete!

This will add the required variables to your path when you start your next session.<br>
To use BioLockJ in the same session, run `source ~/.bash_profile`.

#### 3. Install the software [[Dependencies]] required by the modules you wish to include in your pipeline.

## Notes

### Environments

The main BioLockJ program can be used in these environments: 
* a local machine with a unix-like system
* any machine running docker *
* a cluster, running a scheduler such as torque
* AWS cloud computing *

(* The launch scripts will still be run from your local machine.)

The launch process requires a unix-like environment.  This includes linux, macOS, or an ubuntu environment running on Windows.

If using **docker**, you will need to run the install script to create the variables used by the launch scripts, even though the main BioLockJ program will run within the biolockj_controller container.

If using **AWS**, you will need to run the install script to create the variables used by the launch scripts, even though the main BioLockJ program will run on AWS.

If you are using BioLockJ on a shared system where another user has already installed BioLockJ, you will need to run the install script to create the required variables in your own user profile.
