
The first row in the metadata file is assumed to be column names.

The first column (regardless of its name) is assumed to be the sample names.

Any leading or trailing white space in any cell is removed.  Flanking quotes are removed from the sample ids, that is: only values in the first column.

If no metadata table is supplied to the pipeline, then the **[ImportMetaData](../biolockj.module.implicit/ImportMetadata)** module will look at the input samples and create an empty metadata file.  This module is implicitly added to all pipelines. 

The properties `metadata.required` and `metadata.useEveryRow` control how BioLockJ handles a mis-match between the data and the metadata.  If both are set to Y, then BioLockJ will throw an error if there is not a 1-to-1 matchup between sample names in the first column of the metadata and the file names in the `input.dirPaths` (ignoring any files specified by `input.ignoreFiles`).  If there are files that do not have any corresponding metadata, and this ok, use `metadata.required=N`.  If there are rows in the metadata that do not have corresponding files, and this is ok, use `metadata.useEveryRow=N`.  Setting both to Y is recommended because in most cases, we have a perfect 1-to-1 match up, and if BioLockJ thinks otherwise it because of an error in matching the up files with the samples, and its best to fail early and fix the problem.

The `metadata.fileNameColumn` property allows you to explicity state which input file should match up to a given sample row.  This can be a list of columns; for example if you have paired reads you would have a column for the forward reads, a column for the reverse reads, and both column names would be given with a comma separating them. Example: `metadata.fileNameColumn = forwardReadFile, reverseReadFile`, where "forwardReadFile" and "reverseReadFile" are both column names in the metadata file.  Alternatively, you can specify [input](../../GENERATED/Input) properties telling BioLockJ how to trim away the file name to get the sample name.

Some modules look for specific information supplied in the metadata.  Those modules often have a property to supply a column name referencing the metadata.

The metadata can change through the execution of the pipeline.  Some modules add information to the metadata, such as number of reads in each file, or number of reads classified.  Some modules may filter samples, and removed samples are removed from the metadata as well.  The original metadata file is never changed, and a copy of the original is stored in the pipeline folder.  Each time the data is updated, a new file is saved in the current module folder.  
