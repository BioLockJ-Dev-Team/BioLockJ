
### 1. Complete Program [Installation](../Installation)

### 2. Configure Program Input
* See [Configuration](../Configuration)

### 3. Start Your 1st Pipeline
* To start your pipeline, pass your configuration file path $CONFIG_PATH to the [biolockj](https://github.com/msioda/BioLockJ/blob/master/script/biolockj?raw=true) command: 

```
biolockj $CONFIG_PATH
```

* Verify biolockj command output is accurate:
   - BioLockJ JAR file path is valid 
   - BioLockJ Configuration file path is valid
   - Verify message "BioLockJ started successfully!" is printed.


### 4. Check Pipeline Status 
* Check pipeline progress by tailing the pipeline Java log file with [blj_go](https://github.com/msioda/BioLockJ/blob/master/script/blj_go?raw=true) and [blj_log](https://github.com/msioda/BioLockJ/blob/master/script/blj_log?raw=true):

```
blj_go   # Go to your newest pipeline
blj_log  # Tail pipeline log file 
```

* Verify java is running:

```
ps -u $USER
# psu
# PID    TTY      TIME      CMD
# 16499  pts/45   00:36:21  java
# 188510 pts/45   00:00:00  bash
# ...
```
* If running modules on the cluster, check status of BioLockJ scripts on the job queue (command depends on job queue implementation):

```
qstat -u $USER                                                                                    Req'd       Req'd          Elap
      Job ID                  Username    Queue    Jobname          SessID  NDS   TSK   Memory      Time    S      Time
      ----------------------- ----------- -------- ---------------- ------ ----- ------ --------- --------- - ---------
      530799.cph-m1.uncc.edu  msioda      cph_shor 04.0_PearMergeRe  44312     1      8      64gb  01:00:00 C       00:19:23 
      530800.cph-m1.uncc.edu  msioda      cph_shor 04.1_PearMergeRe  36453     1      8      64gb  01:00:00 R       00:05:23 

```

### 5. Investigate Failed Pipelines
* Failed pipelines can be restarted to save the progress made by successful modules.
* See [Failure Recovery](../Failure-Recovery) for more information.
* Failure Recovery should be avoided until you have successfully completed your 1st pipeline.  
