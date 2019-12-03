### Failed Pipelines
* Failed pipelines can be restarted to save the progress made by successfully completed modules.  To restart a failed pipeline, add -r parameter:

       > biolockj -r <pipeline root>

* Check the BioLockJ execution summary after any failure occurs via the [blj_summary](../wiki/Commands) command. 

       > blj_summary

* Review the BioLockJ log file in your pipeline root directory.  This is the most complete source of information on pipeline execution and may contain useful error messages that help resolve the root cause of the failure.

* If your pipeline directory is not created, you likely have invalid file paths or missing/invalid properties

  - Check the FATAL_ERROR file your $HOME directory.  This is where BioLockJ saves error messages for failures occurring prior to the creation of your pipeline root directory.

        > cat "$HOME/biolockj_FATAL_ERROR_*"

  - The most common culprits are:  
    + *pipeline.defaultProps*
    + Missing/invalid $BLJ_PROJ directory set by the [install](https://github.com/msioda/BioLockJ/blob/master/install?raw=true) script

        > echo $BLJ_PROJ

* Failures that occur in a module/script are not logged to the Java log file since these run outside of the Java application.  The failed module directory may have an indicator file that gives either the sample ID or bash script line that failed.

*  If the failed module was running a bash script on the cluster, check the module/qsub directory for the cluster job output and error files which may contain additional information. 

