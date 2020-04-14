

# if (!requireNamespace("BiocManager", quietly = TRUE))
#   install.packages("BiocManager")
# 
# BiocManager::install("DESeq2")
# browseVignettes("DESeq2")
library(DESeq2)

source("../BioLockJ_Lib.R")

## optional
#library("BiocParallel")
## if using BiocParallel
# register(MulticoreParam( getProperty("script.numThreads", 1) ))


##############################################

writeLines(c("", "Finding inputs..."))

# arg 1 - counts table file
# arg 2 - metadata file
# arg 3 - scriptString - a string to distinguish the output of this script from others in the same module
args = commandArgs(trailingOnly=TRUE)

writeLines(paste("counts table:", args[1]))
writeLines(paste("metadata:", args[2]))
writeLines(paste("script string:", args[3]))

designString = getProperty("deseq2.designFormula")
writeLines(paste("design:", designString))
design = as.formula(designString) 


##############################################

writeLines(c("", "Reading inputs..."))

countData = t(readBljTable(args[1]))
colData = readBljTable(args[2])


##############################################

writeLines(c("", "Launching DESeq..."))

dds1 <- DESeqDataSetFromMatrix(countData = countData,
                               colData = colData,
                               design= design)
dds2 <- DESeq(dds1)


##############################################

writeLines(c("", "Extracting and writting results..."))

for (s in resultsNames(dds2)){
  writeLines(paste("Contrast:", s))
  
  res = results(dds2, name=s)
  resOrdered <- res[order(res$pvalue),]
  
  outfile = paste0(c("../output/", args[3], s, ".tsv"), collapse = "")
  write.table(cbind(row.names(resOrdered), as.data.frame(resOrdered)),
              file=outfile,
              sep="\t",
              quote=FALSE,
              row.names = FALSE,
              col.names = TRUE)
  
  writeLines(paste("Saving to file:", outfile))
  writeLines("Summary:")
  summary(resOrdered)
}


# or to shrink log fold changes association with condition:
# res <- lfcShrink(dds, coef="condition_trt_vs_untrt", type="apeglm")

##############################################

writeLines(c("", "Getting citation info ..."))

citeR = citation()
cite = citation("DESeq2")
writeLines(c(citeR$textVersion, cite$textVersion))
writeLines(c(citeR$textVersion, cite$textVersion), "../temp/citation.txt")

writeLines(c("", "", "Logging session info..."))
sessionInfo()
writeLines(c("", "", "Done!"))
