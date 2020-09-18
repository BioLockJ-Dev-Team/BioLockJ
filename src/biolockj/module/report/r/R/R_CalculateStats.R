# Module script for: biolockj.module.report.r.R_CalculateStats

main <- function() {

   countTable = getCountMetaTable( level )
   metaTable = getMetaData( level )
   if ( is.null(countTable) || is.null(metaTable) ) return( NULL )
   
   numDataCols = ncol(countTable) - ncol(metaTable);
   measuredFeatures = names(countTable)[1:numDataCols]
   reportFeatures = c()
   basicDF = data.frame(matrix(data=measuredFeatures, nrow=numDataCols, ncol=1 ) )
   row.names(basicDF) = measuredFeatures
   parametricPvals = basicDF
   nonParametricPvals = basicDF
   rSquaredVals = basicDF
   
   adjParPvals = basicDF
   adjNonParPvals = basicDF
   
   message( "binaryCols: ", getBinaryFields() )
   message( "nominalCols: ", getNominalFields() )
   message( "numericCols: ", getNumericFields() )

   # if r.rareOtuThreshold > 1, cutoffValue is an absolute threshold, otherwise it's a % of countTable rows
   cutoffValue = getProperty("r.rareOtuThreshold", 1)
   message("Using property r.rareOtuThreshold = ", cutoffValue)
   if( cutoffValue < 1 ) cutoffValue = cutoffValue * nrow(countTable)
   message("cutoffValue = ", cutoffValue)
   
   for( feature in measuredFeatures ){
      if( sum( countTable[,feature] > 0 ) < cutoffValue ) {
         message( c( "Excluding: ", feature, "; only found in ", sum( countTable[,feature] > 0 ), "samples.") )
      }else{
         reportFeatures = c(reportFeatures, feature)
         if( length( getBinaryFields() ) > 0 ) {
            for( field in getBinaryFields() ) {
               message( c( "Calculate pvalues for: ", feature, " ~ ", field ) )
               att = as.factor( metaTable[,field] )
               vals = levels( att )
               if( everyGroupHasData( countTable, feature, field, vals ) ) { 
                  myLm = lm( countTable[,feature] ~ att, na.action=na.exclude )
                  parametricPvals[feature, field] = t.test( countTable[att==vals[1], feature], countTable[att==vals[2], feature] )$p.value
                  nonParametricPvals[feature, field] = pvalue( wilcox_test( countTable[att==vals[1], feature], countTable[att==vals[2], feature] ) )
                  rSquaredVals[feature, field] = summary( myLm )$r.squared
               } else {
                  parametricPvals[feature, field] = 1
                  nonParametricPvals[feature, field] = 1
                  rSquaredVals[feature, field] = 0
               }
            }
         }

         if( length( getNominalFields() ) > 0 ) {
            for( field in getNominalFields() ) {
               message( c( "Calculate pvalues for: ", feature, " ~ ", field ) )
               att = as.factor( metaTable[,field] )
               vals = levels( att )
               if( everyGroupHasData( countTable, feature, field, vals ) ) { 
                     myLm = lm( countTable[,feature] ~ att, na.action=na.exclude )
                     myAnova = anova( myLm )
                     parametricPvals[feature, field] = myAnova$"Pr(>F)"[1]
                     nonParametricPvals[feature, field] = kruskal.test( countTable[,feature] ~ att, na.action=na.exclude )$p.value
                     rSquaredVals[feature, field] = summary( myLm )$r.squared
               } else {
                     parametricPvals[feature, field] = 1
                     nonParametricPvals[feature, field] = 1
                     rSquaredVals[feature, field] = 0
               }
            }
         }
         
         if( length( getNumericFields() ) > 0 ) {
            for( field in getNumericFields() ) {
               message( c( "Calculate pvalues for: ", feature, " ~ ", field ) )
               att = as.numeric( metaTable[,field] )
               parametricPvals[feature, field] = Kendall( countTable[,feature], att)$sl[1]
               nonParametricPvals[feature, field] = cor.test( countTable[,feature], att, na.action=na.exclude )$p.value
               rSquaredVals[feature, field] = cor( countTable[,feature], att, use="na.or.complete", method="kendall" )^2 
            }
         }
      }
   }

   if( length( reportFeatures ) == 0 ) {
      message( "No OTU Names found, verify empty vector: ", reportFeatures )
      return( NULL )
   }
   
   # remove the starter column
   parametricPvals = parametricPvals[reportFeatures,2:length(parametricPvals)]
   nonParametricPvals = nonParametricPvals[reportFeatures,2:length(nonParametricPvals)]
   rSquaredVals = rSquaredVals[reportFeatures,2:length(rSquaredVals)]

   message( "Calculate ADJUSTED P_VALS" )
   adjParDF = data.frame(matrix(data=NA, nrow=nrow(parametricPvals), ncol=ncol(parametricPvals) ), row.names=row.names(parametricPvals) )
   names(adjParDF) = names(parametricPvals)
   adjNonParDF = data.frame(matrix(data=NA, nrow=nrow(nonParametricPvals), ncol=ncol(nonParametricPvals) ), row.names=row.names(nonParametricPvals)  )
   names(adjNonParDF) = names(nonParametricPvals)
   
   for( rf in getReportFields() ) {
      message( "Calculate adjusted pval for report field: ", rf )
      adjParDF[,rf] = p.adjust( parametricPvals[,rf], method=getProperty("r_CalculateStats.pAdjustMethod"), n=getP_AdjustLen(reportFeatures) )
      adjNonParDF[,rf] = p.adjust( nonParametricPvals[,rf], method=getProperty("r_CalculateStats.pAdjustMethod"), n=getP_AdjustLen(reportFeatures) )
   }

   message( "measuredFeatures: ", paste(measuredFeatures,sep=", ") )
   message( "reportFeatures: ", paste(reportFeatures, sep=", ") )

   buildSummaryTables( parametricPvals, level, statsFileSuffix( TRUE, FALSE ) )
   buildSummaryTables( nonParametricPvals, level, statsFileSuffix( FALSE, FALSE ) )
   buildSummaryTables( rSquaredVals, level, statsFileSuffix() )
   buildSummaryTables( adjParDF, level, statsFileSuffix( TRUE ) )
   buildSummaryTables( adjNonParDF, level, statsFileSuffix( FALSE ) )
   

}

buildSummaryTables <- function( table, level, fileNameTag ) {
   df=data.frame("OTU"=row.names(table), table, check.names=FALSE)
   fileName = getPath( getOutputDir(), paste0( level, "_", fileNameTag ) )
   message( paste( "Saving output file:", fileName ) )
   write.table( df, file=fileName, sep="\t", row.names=FALSE )
}

# P Adjust Length depends on how many p-values to include in the adjustment
# LOCAL adjusts p-values for all OTUs for 1 attribute at 1 taxonomy level
# TAXA adjusts p-values for a all OTUs and all attributes at 1 taxonomy level
# ATTRIBUTE adjusts p-values for a all OTUs and all taxonomy levels for 1 attribute
# GLOBAL adjusts p-values for a all OTUs and all attributes and all taxonomy levels
getP_AdjustLen <- function( names ) {
   if( getProperty("r_CalculateStats.pAdjustScope") == "GLOBAL" ) {
      return( length(names) * length( taxaLevels() ) * length( getReportFields() ) )
   } else if( getProperty("r_CalculateStats.pAdjustScope") == "ATTRIBUTE" ) {
      return( length(names) * length( taxaLevels() ) )
   } else if( getProperty("r_CalculateStats.pAdjustScope") == "TAXA" ) {
      return( length(names) * length(getReportFields()) )
   } else {
      return( length(names) )
   } 
}

# Verify all groups have 2+ values to avoid statistical test failures
everyGroupHasData <- function( countTable, feature, field, vals )  {
   for( val in vals ) {
      hasTaxa = !is.na( countTable[, feature] )
      hasVal = as.vector( countTable[,field] ) == val
      if( length( which( hasTaxa & hasVal == TRUE) ) < 1 ) {
         message( paste( "Skip statistical test for", feature, "no data for", field, "=", val ) )
         return( FALSE )
      }
   }
   return( TRUE )
}

# Method wilcox_test is from the coin package
# Calculates exact p-values without using heuristic algorithm for better precision
# Otherwise if ties are found the script may fail
wilcox_test.default <- function( x, y, ... ) {
   data = data.frame( values = c(x, y), group = rep( c("x", "y"), c(length(x), length(y)) ) )
   return( wilcox_test( values ~ group, data = data, ... ) )
}

# Main function imports coin and Kendall libraries
# Generates the reportStats list with p-value and R^2 metrics
# # Outputs summary tables for each metric at each taxonomyLevel
# main <- function() {
#    reportStats = calculateStats( level )
#    if( is.null( reportStats ) ) {
#       message( c( level, "table is empty" ) )
#    } else {
#       message( "Building summary Tables ... " )
#       buildSummaryTables( reportStats, level )
#    }
#    message( "Done!" )
# }


message("Current Working directory: ",getwd())

args = commandArgs(trailingOnly = TRUE)
message("all args: ", args)
level = args[1]
message("Running script for level: ", level)

source("../resources/BioLockJ_Lib.R")
importLibs( c( "coin", "Kendall" ) )

main()

sessionInfo()
