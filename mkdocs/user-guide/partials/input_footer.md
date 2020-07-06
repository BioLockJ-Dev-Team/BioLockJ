
BioLockJ will assume that the sample name for a given file is the same as the file name after removing the file suffix.  This is often not-quite-enough.  Use `input.trimPrefix` and `input.trimSuffix` to indicate additional text to remove from the file name to get the sample name.  If using paired-end sequences, use `input.suffixFw` and `input.suffixRv` to indicate the forward and reverse reads for a given sample; these will also be removed when deriving the sample name.

## Example

**Sample IDs** = mbs1, mbs2, mbs3, mbs4

**Example File names**
+ gut_mbs1.fq.gz
+ gut_mbs2.fq.gz
+ oral_mbs3.fq
+ oral_mbs4.fq

**Config Properties**
+ *input.trimPrefix*=_
+ *input.trimSuffix*=.fq

All characters before (and including) the 1st "_" in the file name are trimmed

All characters after (and including) the 1st ".fq" in the file name are trimmed

BioLockJ automatically trims extensions ".fasta" and ".fastq" as if configured in *input.trimSuffix*.

Sometimes, there is no way to derive the sample name from the file name; or its simply inconvenient to.  An alternative way to link files to sample names is to list the file names in the metadata in one or more columns (one file name per cell) and list the names of these columns in `metadata.fileNameColumn`; see [Metatdata](../../GENERATED/Metadata). 

If you want process only a subset of the files in your input directories, then specifying the file names in the metadata is much more effecient than list all files to ignore in `input.ignoreFiles`.

Note that BioLockJ determines some information based on the type of data in the input directories.  This is very helpful in determining appropriate sequence pre-processing steps.  However it can be problematic when using an unusual input type.  To avoid this automatic determineation, manually set `pipeline.inputTypes`.  Setting this to "other" will avoid all assumptions that BioLockJ might make based on the input types.
