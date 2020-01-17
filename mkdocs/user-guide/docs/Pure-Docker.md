
## Pure Docker (experimental)

This option is still in the early experimental stages.

If you are running from any system that supports docker, you can run all commands through docker containers.

#### 1. Install docker and make sure the hello world image runs correctly. (see above)

#### 2. Enable file sharing. (see above)

#### 3. Create a directory where your pipelines will be saved; this directory will be referred to as `$BLJ_PROJ` and it must be under a share-enabled directory.

```bash
mkdir pipelines
BLJ_PROJ=`pwd`/pipelines
```

#### 4. Run the test pipeline in pure docker
To test your all-docker BioLockJ, run:
```bash
docker run --rm \
-v /var/run/docker.sock:/var/run/docker.sock \
-v $BLJ_PROJ:/mnt/efs/pipelines \
biolockjdevteam/biolockj_controller:v1.2.7 \
biolockj -df /app/biolockj/templates/myFirstPipeline/myFirstPipeline.properties
```

You should see a pipeline named `myFirstPipeline_<DATE>` in your `$BLJ_PROJ` folder.

The above example uses a config file that is in the docker container.  To use a file from your own machine, mount the parent directory, and use the in-container path to reach it.  To test this, extract the `myFirstPipeline.properties` file from the docker container and use it from the outside:
```bash
ID=$(docker run -d biolockjdevteam/biolockj_controller:v1.2.7 /bin/bash)
docker cp $ID:/app/biolockj/templates/myFirstPipeline/myFirstPipeline.properties ~/Downloads/origTest.properties
```

Alternatively, create a file, and copy this text into it:
```text
#BioModule biolockj.module.implicit.RegisterNumReads
#BioModule biolockj.module.seq.RarefySeqs
#BioModule biolockj.module.seq.Multiplexer

input.dirPaths=${BLJ}/templates/myFirstPipeline/rhizosphere_16S_data

rarefySeqs.max=500
```

Run biolockj using this config file.
```bash
docker run --rm \
-v /var/run/docker.sock:/var/run/docker.sock \
-e HOME=~ \
biolockjdevteam/biolockj_controller:v1.2.7 \
biolockj -df --blj_proj $BLJ_PROJ /Downloads/origTest.properties
```
This should make a new pipeline called `origTest_<DATE>` in your `$BLJ_PROJ` folder.

Finally, run the container interactively:
```bash
docker run --rm \
-v /var/run/docker.sock:/var/run/docker.sock \
-v $BLJ_PROJ:/mnt/efs/pipelines \
-it biolockjdevteam/biolockj_controller:v1.2.7 /bin/bash
```


A general difficulty of the _Pure Docker_ path is managing the volume mounts.  Calling `biolockj` locally allows the biolockj script to handle this for you.  Variable path support may make this eaiser. (TODO: add link to config variable descriptions)
