# GenMod Demo

The most versatile module in BioLockJ is called "GenMod", the General Module.
See the documentation in the user guide, which is under BioLockJ/docs or online at https://biolockj-dev-team.github.io/BioLockJ/GENERATED/biolockj.module.diy/GenMod/

### GenModDemo.config 

The pipeline outlined in 'GenModDemo.config' shows the basics of using this module.  Every module in this pipeline is GenMod.  Each instance is given a distinctive alias using the 'AS' keyword to make it possible to distinguish them.  

To run this pipeline yourself, install biolockj and run:
```
biolockj GenModDemo.config
```

To run this pipeline using docker, run:
```
biolockj --docker GenModDemo.config
```

### ForEach.config

The pipeline outlined 'ForEach.config' demonstrates the collection of GenMod derivatives.  Using any member of the ForEach family is very much like writing a for-loop into a script to use in GenMod but because BioLockJ handles the for-loop, the worker scripts can be run in parallel.  Even when parallel computing is not a goal, these modules can result in a more concise custom script.

To run this pipeline yourself, install biolockj and run:
```
biolockj GenModDemo.config
```

To run this pipeline using docker, run:
```
biolockj --docker GenModDemo.config
```

###

See 'biolockj --help' for additional options.  In either case, this command will create a new instance of the pipeline in your '$BLJ_PROJ' folder.
The 'example_output' folder contains an instance of each pipeline.

The modules demonstrated here include:
[GenMod](https://biolockj-dev-team.github.io/BioLockJ/GENERATED/biolockj.module.diy/GenMod/)
[ForEachSample](https://biolockj-dev-team.github.io/BioLockJ/GENERATED/biolockj.module.diy/ForEachSample/)
[ForEachFile](https://biolockj-dev-team.github.io/BioLockJ/GENERATED/biolockj.module.diy/ForEachFile/)
[ForEachLevel](https://biolockj-dev-team.github.io/BioLockJ/GENERATED/biolockj.module.diy/ForEachLevel/)
[ForEachLoop](https://biolockj-dev-team.github.io/BioLockJ/GENERATED/biolockj.module.diy/ForEachLoop/)
