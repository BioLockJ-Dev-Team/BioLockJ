# Module script for: biolockj.module.report.r.R_CalculateStats

# There are 5 summary tables are output for each taxaLevels()
# 1. Parametric P-Value table
# 2. Non-parametric P-Value table 
# 3. Adjusted Parametric P-Value table
# 4. Adjusted Non-parametric P-Value table 
# 5. R^2 Value table
buildSummaryTables <- function( reportStats, level ) {
   fields = unique( names(reportStats[[2]]) )
   for( i in 2:length( reportStats ) ) {
      df = data.frame( vector( mode="double", length=length( reportStats[[1]] ) ) )
      df[, 1] = reportStats[[1]]
      names(df)[1] = names( reportStats )[1]
      for( j in 1:length( fields ) ) {
         df[, length(df)+1] = getValuesByName( reportStats[[i]], fields[j] )
         names(df)[length(df)] = fields[j]
      }
      fileName = getPath( getOutputDir(), paste0( level, "_", names(reportStats)[i] ) )
      message( paste( "Saving output file", fileName ) )
      write.table( df, file=fileName, sep="\t", row.names=FALSE )
   }
}  

# Build list of reportStats for the specific taxonomy/humann2 countTable with the following columns:
# "OTU", "parametricPvals", "nonParametricPvals", "adjParPvals", "adjNonParPvals", "rSquaredVals"
# First, parametric & non-parametric p-values are calculated for each attribute type
# Then the p-values are adjusted using method defined in r.pAdjust
calculateStats <- function( level ) {

   countTable = getCountMetaTable( level )
   metaTable = getMetaData( level )
   if ( is.null(countTable) || is.null(metaTable) ) return( NULL )
   names = vector( mode="character" )
   parametricPvals = vector( mode="double" )
   nonParametricPvals = vector( mode="double" )
   rSquaredVals = vector( mode="double" )
   adjParPvals = vector( mode="double" )
   adjNonParPvals = vector( mode="double" )
   message( "binaryCols: ", getBinaryFields() )
   message( "nominalCols: ", getNominalFields() )
   message( "numericCols: ", getNumericFields() )

   # if r.rareOtuThreshold > 1, cutoffValue is an absolute threshold, otherwise it's a % of countTable rows
   cutoffValue = getProperty("r.rareOtuThreshold", 1)
   if( cutoffValue < 1 ) cutoffValue = cutoffValue * nrow(countTable)
   numDataCols = ncol(countTable) - ncol(metaTable);
   for( countCol in 1:numDataCols ){
      if( sum( countTable[,countCol] > 0 ) < cutoffValue ) {
         message( c( "Excluding: ", names(countTable)[countCol], "; only found in ", sum( countTable[,countCol] > 0 ), "samples.") )
      }else{
         names[ length(names)+1 ] = names(countTable)[countCol]
         if( length( getBinaryFields() ) > 0 ) {
            for( field in getBinaryFields() ) {
               message( c( "Calculate pvalues for:", names[ length(names) ], " ~ ", field ) )
               att = as.factor( metaTable[,field] )
               vals = levels( att )
               if( everyGroupHasData( countTable, countCol, field, vals ) ) { 
                     myLm = lm( countTable[,countCol] ~ att, na.action=na.exclude )
                     parametricPvals = addNamedVectorElement( parametricPvals, field, t.test( countTable[att==vals[1], countCol], countTable[att==vals[2], countCol] )$p.value )
                     nonParametricPvals = addNamedVectorElement( nonParametricPvals, field, pvalue( wilcox_test( countTable[att==vals[1], countCol], countTable[att==vals[2], countCol] ) ) )
                     rSquaredVals = addNamedVectorElement( rSquaredVals, field, summary( myLm )$r.squared )
               } else {
                     parametricPvals = addNamedVectorElement( parametricPvals, field, 1 )
                     nonParametricPvals = addNamedVectorElement( nonParametricPvals, field, 1 )
                     rSquaredVals = addNamedVectorElement( rSquaredVals, field, 0 )
               }
            }
         }

         if( length( getNominalFields() ) > 0 ) {
            for( field in getNominalFields() ) {
               message( c( "Calculate pvalues for:", names[ length(names) ], " ~ ", field ) )
               att = as.factor( metaTable[,field] )
               vals = levels( att )
               if( everyGroupHasData( countTable, countCol, field, vals ) ) { 
                     myLm = lm( countTable[,countCol] ~ att, na.action=na.exclude )
                     myAnova = anova( myLm )
                     parametricPvals = addNamedVectorElement( parametricPvals, field, myAnova$"Pr(>F)"[1] )
                     nonParametricPvals = addNamedVectorElement( nonParametricPvals, field, kruskal.test( countTable[,countCol] ~ att, na.action=na.exclude )$p.value )
                     rSquaredVals = addNamedVectorElement( rSquaredVals, field, summary( myLm )$r.squared )
               } else {
                     parametricPvals = addNamedVectorElement( parametricPvals, field, 1 )
                     nonParametricPvals = addNamedVectorElement( nonParametricPvals, field, 1 )
                     rSquaredVals = addNamedVectorElement( rSquaredVals, field, 0 )
               }
            }
         }
         
         if( length( getNumericFields() ) > 0 ) {
            for( field in getNumericFields() ) {
               message( c( "Calculate pvalues for:", names[ length(names) ], " ~ ", field ) )
               att = as.numeric( metaTable[,field] )
               parametricPvals = addNamedVectorElement( parametricPvals, field, Kendall( countTable[,countCol], att)$sl[1] )
               nonParametricPvals = addNamedVectorElement( nonParametricPvals, field, cor.test( countTable[,countCol], att, na.action=na.exclude )$p.value )
               rSquaredVals = addNamedVectorElement( rSquaredVals, field, cor( countTable[,countCol], att, use="na.or.complete", method="kendall" )^2 )
            }
         }
      }
   }

   if( length( names ) == 0 ) {
      message( "No OTU Names found, verify empty vector", names )
      return( NULL )
   }

   message( "Calculate ADJUSTED P_VALS" )
   adjParDF = data.frame( vector(mode="double", length=length(names)) )
   adjNonParDF = data.frame( vector(mode="double", length=length(names)) )
   for( i in 1:length( getReportFields() ) ) {
      message( "Calculate adjusted pval for report field:", getReportFields()[i] )
      parPvals = getValuesByName( parametricPvals, getReportFields()[i] )
      npPvals = getValuesByName( nonParametricPvals, getReportFields()[i] )
      adjParDF[,i] = p.adjust( parPvals, method=getProperty("r_CalculateStats.pAdjustMethod"), n=getP_AdjustLen(names) )
      adjNonParDF[,i] = p.adjust( npPvals, method=getProperty("r_CalculateStats.pAdjustMethod"), n=getP_AdjustLen(names) )
      names(adjParDF)[i] = getReportFields()[i]
      names(adjNonParDF)[i] = getReportFields()[i]
   }
   
   message( "Convert ADJUSTED P_VALS data.frame --> named vector" )
   adjParPvals = as.vector( as.matrix(adjParDF) )
   adjNonParPvals = as.vector( as.matrix(adjNonParDF) )
   for( i in 1:length( getReportFields() ) ) {
      y = length( names ) * i
      x = y - length( names ) + 1
      names( adjParPvals )[x:y] = rep( getReportFields()[i], length( names ) )
      names( adjNonParPvals )[x:y] = rep( getReportFields()[i], length( names ) )
   }
   message( "names", names )
   message( "adjParPvals:", adjParPvals )
   message( "adjNonParPvals:", adjNonParPvals )
   reportStats = list( names, parametricPvals, nonParametricPvals, adjParPvals, adjNonParPvals, rSquaredVals )
   names( reportStats ) = c( "OTU", statsFileSuffix( TRUE, FALSE ), statsFileSuffix( FALSE, FALSE ),
                                    statsFileSuffix( TRUE ), statsFileSuffix( FALSE ), statsFileSuffix() )

   return( reportStats )
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
everyGroupHasData <- function( countTable, countCol, field, vals )  {
   for( val in vals ) {
      hasTaxa = !is.na( countTable[, countCol] )
      hasVal = as.vector( countTable[,field] ) == val
      if( length( which( hasTaxa & hasVal == TRUE) ) < 1 ) {
         message( c( "Skip statistical test for", names(countTable)[countCol], "no data for", field, "=", val ) )
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
# Outputs summary tables for each metric at each taxonomyLevel
main <- function() {
   reportStats = calculateStats( level )
   if( is.null( reportStats ) ) {
      message( c( level, "table is empty" ) )
   } else {
      message( "Building summary Tables ... " )
      buildSummaryTables( reportStats, level )
   }
   message( "Done!" )
}


message("Current Working directory: ",getwd())

args = commandArgs(trailingOnly = TRUE)
message("all args: ", args)
level = args[1]
message("Running script for level: ", level)

source("../resources/BioLockJ_Lib.R")
importLibs( c( "coin", "Kendall" ) )

main()

sessionInfo()
