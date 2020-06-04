# Module script for: biolockj.module.report.r.R_PlotPvalHistograms

# save table output
# save favMetacols
# color by test type


# Add page footer and page number at bottom of page
addReportFooter <- function( level, pageNum ) {
   addPageNumber( pageNum )
   addPageFooter( paste( "P-Value Histograms [", str_to_title( level ), "]" ) )
}

# Add one histogram to the report
addHistogram <- function( v, xLabel, col ) {
   if ( !all(is.nan( v )) && !all(is.na( v )) ) {
      pvalCutoff = getProperty("r.pvalCutoff")
      hist( v, breaks=seq(0, 1, 0.05), xlab=xLabel, main="", cex.main=getCexMain( xLabel ), col=col, xlim=c(0,1) )
      abline(v=pvalCutoff, col=gray(.5), lty=2)
      mtext(at=pvalCutoff, side=3, text=pvalCutoff, col=gray(.5))
   }else{
      plotPlainText( "All values are NA or NaN." )
   }
}

# return the fraction of pvalues that are under a threshold, don't count NAs
calcSigFraction <- function(pvals, pvalCutoff){
   belowCutoff = sum(pvals < pvalCutoff, na.rm=TRUE)
   total = sum(!is.na(pvals))
   return( belowCutoff / total )
}

# Return main.cex parameter between 0.65 and 1.2 based on longest report field name
getCexMain<- function( labels=NULL ) {
   cexMax = 1.2
   cexMin = 0.65

   maxLabelSize = 1
   for( i in 1:length(labels) ) {
      if( nchar(labels[i]) > maxLabelSize ) maxLabelSize = nchar(labels[i])
   }

   size = 25/maxLabelSize
   if ( size > cexMax ) size = cexMax
   if ( size < cexMin ) size = cexMin

   return( size )
}

# This graphic can be printed with the histograms,
# used for documentation, or just as a reference check when chaning the colors.
printColorCode <- function() {
   parTests = getTestName() 
   nonParTests = getTestName( isParametric=FALSE )
   colors = c( names(parTests), names(nonParTests) )
   plot(1,1, ylim=c(0,4), xlim=c(.5,2.5), type="n", axes=FALSE, xlab="", ylab="")
   title(main="Color Coding", line=0.5)
   x=rep(1:2, each=3)
   y=rep(3:1, 2)
   symbols(x=x, y=y, rectangles = matrix(ncol=2,byrow=TRUE,data=rep(c(1,1),6)), fg=colors, bg=colors, inches=FALSE, add=TRUE)
   text(x=x, y=y, labels=c(parTests,nonParTests) )
   text(x=1, y=3.5, labels="parametric", pos=3)
   text(x=2, y=3.5, labels="non-parametric", pos=3)
}


# Main function generates reports for each each taxaLevels()
# Each taxonomy report includes 2 histograms for each report field (1 parametric, 1 non-parametric)
main <- function() {
   parStats = getStatsTable( level, TRUE, FALSE )
   nonParStats = getStatsTable( level, FALSE, FALSE )
   if( is.null(parStats) ) { stop("Stop. Parametric stats table is null. Exiting.") }
   if( is.null(nonParStats) ) { stop("Stop. Non-parametric stats table is null. Exiting.") }

   # create empty pdf
   outputFile = getPath( file.path(getModuleDir(), "output"), paste0(level, "_histograms.pdf") )
   message("Saving plots to: ", outputFile)
   pdf( outputFile, paper="letter", width=7.5, height=10.5  )
   par( mfrow=c(3, 2), las=1, mar=c(5,4,3,1)+.1, oma=c(1,1,1,0), cex=1)
   pageNum = 1
   position = 1
   
   # create ranks table
   fields = getReportFields()
   message( "fields", getReportFields() )
   ranks = data.frame(AttributeName=fields,
                      Parametric.FractionPvalsUnderCutoff=rep(NA, length(getReportFields())),
                      NonParametric.FractionPvalsUnderCutoff=rep(NA, length(getReportFields())))
   
   row.names(ranks) = ranks$AttributeName
   for( field in getReportFields() ) {
      ranks[field,"Parametric.FractionPvalsUnderCutoff"] = calcSigFraction(parStats[, field], getProperty("r.pvalCutoff"))
      ranks[field,"NonParametric.FractionPvalsUnderCutoff"] = calcSigFraction(nonParStats[, field], getProperty("r.pvalCutoff"))
   }
   # order attributes based on the fraction of tests with a p-value below <pvalCutoff>
   orderBy = apply(ranks[2:3],1, max, na.rm=TRUE)
   ranks = ranks[order(orderBy, decreasing=TRUE),]
   fname = getPath( getTempDir(), paste0(level, "_fractionBelow-", getProperty("r.pvalCutoff"), ".tsv") )
   write.table(ranks, file=fname, sep="\t", quote=FALSE, row.names=FALSE)
   
   # plot histograms in the same order they have in the ranks table
   for( field in ranks$AttributeName ) {
      
      message("processing attribute:", field )
      stopifnot( field %in% names(nonParStats) && field %in% names(parStats) )
      
      parTestName = getTestName( field )
      xLabelPar = paste( parTestName, "P-Values" )
      addHistogram( parStats[, field], xLabelPar, names(parTestName) )
      
      nonParTestName = getTestName( field, FALSE )
      xLabelNonPar = paste( nonParTestName, "P-Values" )
      addHistogram( nonParStats[, field], xLabelNonPar, names(nonParTestName) )
      
      position = position + 2
      
      # shared title
      plotPointPerInch = (par("usr")[2] - par("usr")[1]) / par("pin")[1]
      shiftByPoints = par("mai")[2] * plotPointPerInch
      centerAt = par("usr")[1] - shiftByPoints
      mtext( field, side=3, line=1.5, at=centerAt, adj=.5, xpd=NA, font=par("font.main"), cex=par("cex.main"))
      
      if( position == 3 ) { 
         addReportFooter( level, pageNum ) 
      } else if( position > prod( par("mfrow") ) ) {
         addReportFooter( level, pageNum )
         position = 1
         pageNum = pageNum + 1
      }
   }
   par()
   
   printColorCode()
   plotPlainText( c("Unadjusted P-value histograms are", "ordered by % tests below",
                    paste( "P-value significance threshold:", getProperty("r.pvalCutoff") ) ), align="right")
   addReportFooter( level, pageNum )
   
   dev.off()
}

message("Current Working directory: ",getwd())

args = commandArgs(trailingOnly = TRUE)
message("all args: ", args)
level = args[1]
message("Running script for level: ", level)

source("../resources/BioLockJ_Lib.R")

main()

sessionInfo()
