# cluster properties                   
                   
The `cluster.*` properties are ONLY relevant if `pipeline.env=cluster`.                   
                   
BioLockJ was originally designed to optimize effeciency on a cluster system, specifically one with a torque scheduler.                   
                   
We recomend [chaining configuration properties](../../Configuration/#chaining-configuration-files) across multiple files. The `cluster.*` properties would go in the configuration file for you environement.                    
                   
                   
| Property| Description |
| :--- | :--- |
| *cluster.batchCommand* | _string_ <br>Terminal command used to submit jobs on the cluster<br>*default:*  *null* |
| *cluster.host* | _string_ <br>The remote cluster host URL (used for ssh, scp, rsync, etc)<br>*default:*  *null* |
| *cluster.jobHeader* | _string_ <br>Header written at top of worker scripts<br>*default:*  *null* |
| *cluster.modules* | _list_ <br>List of cluster modules to load at start of worker scripts<br>*default:*  *null* |
| *cluster.prologue* | _string_ <br>To run at the start of every script after loading cluster modules (if any)<br>*default:*  *null* |
| *cluster.statusCommand* | _string_ <br>Terminal command used to check the status of jobs on the cluster<br>*default:*  *null* |
                   
                   
For example, the following values are used for a torque scheduler in the file: cluster.properties                   
                   
```                   
pipeline.env=cluster                   
cluster.batchCommand=qsub -q copperhead                   
cluster.host=hpc.uncc.edu                   
cluster.statusCommand=qstat                   
```                   
                   
Each project's indicidule configuration file includes                              
`pipeline.defaultProps = cluster.properties`                                     
