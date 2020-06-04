# Module script for: biolockj.module.report.r.R_PlotEffectSize

### NOTE: The header printed on the reference table serves as built-in documentation for this module. 

# Get the raw counts and calculate simple relative abundance of the binary data fields
getNormTaxaTable <- function( level ){
	normTable = NULL
	if( !getProperty( "r_PlotEffectSize.disableFoldChange", FALSE ) && !getProperty( "R_internal.runHumann2" ) ) {
		normTaxa = pipelineFile( paste0( "_taxaCount_norm_", level, ".tsv$" ) )
		message( c( "Looking for normalized taxa table count table", paste0( "_taxaCount_norm_", level, ".tsv$" ) ) )
		if( !is.null( normTaxa ) ) normTable = readBljTable( normTaxa )
	}
    return( normTable )
}

# The main method is designed to integrate this module with BiolockJ.  
# It handles pulling data from other modules and options from the BiolockJ properties.
main <- function(){
  
  doCohensD = !getProperty("r_PlotEffectSize.disableCohensD", FALSE)
  doRSquared = !getProperty("r_PlotEffectSize.disableRSquared", FALSE)
  
  # get normalized taxa vals plus metadata
  countTable = getCountTable( level )
  metaTable = getMetaData( level )
  if( is.null(countTable) ) { stop("Stop. Counts table is null. Exiting.") }
  if( is.null(metaTable) ) { stop("Stop. Metadata table is null. Exiting.") }

  # get stats
  pvalTable = getStatsTable( level, getProperty("r_PlotEffectSize.parametricPval", FALSE), !getProperty("r_PlotEffectSize.disablePvalAdj", FALSE) )
  if( doRSquared ) r2Table = getStatsTable( level )
  
  message( c( "Processing level table[", level, "] has", nrow( countTable ), "rows and", ncol(countTable), "columns") )
  message( c( "Stat tables have", nrow( pvalTable ), "rows and", ncol( pvalTable ), "columns." ) )
  
  # make a new pdf output file, specify page size
  outFileName = getPath( getOutputDir(), paste0(level, "_EffectSizePlots.pdf") )
  pdf(file=outFileName, paper="letter", width=7.5, height=10 )
  
  for( field in getReportFields() ){
    
    isBinaryAtt = field %in% getBinaryFields()
    pvals = pvalTable[,field]
    names(pvals) = row.names(pvalTable)
    
    # rSquared piggy-backs on effects size for selection and ordering, 
    # so IF both are plotted, they are ploted in the same order.
    # Even if it is not a binary attribute, the normalized P-values should have AT LEAST 2 tables
    if( doCohensD || doRSquared ){ 
      
      r2vals=r2Table[,field]
      names(r2vals) = row.names(r2Table)
      normPvals = split(countTable[row.names(metaTable),], f=metaTable[,field])
      
      saveRefTable = NULL
      if( doCohensD && isBinaryAtt ) saveRefTable=getPath( getTempDir(), paste(level, field, "effectSize.tsv", sep="_") )
      type = c( "CohensD", "rSquared" )
      if( !doCohensD ) type = "rSquared" else if( !doRSquared ) type = "CohensD"
      data = calcBarSizes( type, normPvals, pvals, r2vals, saveRefTable )
      
      if( doRSquared ){ 
        drawPlot( data[["toPlot"]][,c("pvalue","rSquared")], field, "rSquared", "R-squared", data[["comments"]][c(1,3)] )
        message( c("Completed r-squared plot for level:", level, "and report field:", field) )
      }
      
      if( doCohensD && isBinaryAtt ) {
        message( c( "Effect size: Calling drawPlot for level:", level, ":", field ) )
        drawPlot( data[["toPlot"]], field, "CohensD", "Effect Size (Cohen's D)", data[["comments"]][c(1,2)], data[["xAxisLab2"]] )
      }
    }
    
    if( isBinaryAtt && !getProperty("r_PlotEffectSize.disableFoldChange", FALSE) ) {
      plotFoldChange( countTable, metaTable, level, field, pvals )
    }
  }
  dev.off()
}

# Plot fold changes for normalized counts
plotFoldChange <- function( countTable, metaTable, level, field, pvals ){
  normCountTable = getNormTaxaTable( level )
  if( is.null( normCountTable ) ) {
    message( "Normalized data not found by analyzing file names, using same table used for other plots" )
    normCountTable = countTable
  } else {
    message( "Using normalized data for fold change plot" )
  }
  
  splitRelAbund = split(normCountTable[row.names(metaTable),], f=metaTable[,field])
  message( c("Fold change: Calling calcBarSizes for level:", level, ":", field ) )
  
  saveRefTable=getPath( getTempDir(), paste(level, field,"foldChange.tsv", sep="_") ) 
  data = calcBarSizes( "foldChange", splitRelAbund, pvals, saveRefTable=saveRefTable )
  
  message( c( "Fold change: Calling drawPlot for level:", level, ":", field ) )
  drawPlot( data[["toPlot"]], field, "foldChange", "Fold Change", data[["comments"]], data[["xAxisLab2"]] )
}

# error handler designed for calcBarSizes and drawPlot
errorHandler1 = function(err, level, field) {
  if( doDebug() ){print(err)}
  origErr = as.character(err)
  # error messages that I create in calcBarSizes and drawPlot start with "Stop Plotting"
  trimmedErr=gsub("Error.*Stop Plotting:", "", origErr) 
  msg = paste0("Failed to create plot for taxonomy level: ", level, 
               "\nfor field: ", field)
  if( doDebug() || nchar(trimmedErr) < nchar(origErr) ){
    msg = paste0(msg, "\n", trimmedErr)
    plotPlainText(msg)
  }else{
    # pass error to biolockj to fail module
    msg = paste0(msg, "\n", origErr)
    stop( c( msg ) )
  }
}

calcBarSizes <- function( type, data, pvals, r2vals=NULL, saveRefTable=NULL ) {
  
    # type - type of info in data, options: CohensD, rSquared, foldChange
  ##   CohensD (default) is the difference of the means devided by the pooled standard deviation
  ##   foldChange is the ratio of the means
  ##   rSquared is taken from the calculate stats module
    # data - data.frame with effect size + OTU/Pathway data
    # pvals - named vector of -values from calcStats
    ##   names of pvals are OTUs/Pathways, and should match data column names.
    # r2vals - (optional) named vector of r-squared values from a statistical test to use for bar sizes.
    # saveRefTable - file name to save a reference table corresponding to the plot
    
    denGroupVals = data[[1]]
    denGroupName = names(data)[1]
    numGroupVals = data[[2]]
    numGroupName = names(data)[2]
    orderByColumn = type[1]
    
  # Keep a running header of documentation
  header = "Plot Reference"
  # Select viable OTUs to plot
  viableOTUs = selectViableOTUs(names(numGroupVals), names(denGroupVals), pvals)
  plotOTUs = viableOTUs[["plotOTUs"]]
  comments = viableOTUs[["comment"]]
  numGroupVals = numGroupVals[plotOTUs]
  denGroupVals = denGroupVals[plotOTUs]
  
  # assemble data frame of plot values.
  numGroupN = sapply(numGroupVals, function(x){sum(!is.na(x))})
  denGroupN = sapply(denGroupVals, function(x){sum(!is.na(x))})
  numMeans = colMeans(numGroupVals, na.rm=TRUE)
  denMeans = colMeans(denGroupVals, na.rm=TRUE)
  toPlot = data.frame(OTU=plotOTUs, row.names=plotOTUs,
                      numMeans = numMeans, denMeans = denMeans,
                      infUp = numMeans > 0 && denMeans == 0, #In the table, these are Inf
                      infDown = numMeans == 0 && denMeans > 0) #In the table, these are 0
  header = c(header, "<group name>.mean: the mean value for each group.")
  header = c(header, paste("infUp: was the OTU flagged for having all-0-counts only in the", denGroupName, "group."))
  header = c(header, paste("infDown: was the OTU flagged for having all-0-counts only in the", numGroupName, "group."))
  if (!is.null(pvals)){
    toPlot$pvalue = pvals[row.names(toPlot)]
    header = c(header, paste0("pvalue: the -value used to determine if the OTU was included (if under ", getProperty("r_PlotEffectSize.excludePvalAbove", 1),
                              ") and if OTU got a star (if under <pvalStar>thresholds controlled by r_PlotEffectSize.excludePvalAbove and r.pvalCutoff properties respectively.",
                              " See also: r_PlotEffectSize.parametricPval property"))
  }
  xAxisLab2="" #just make sure it exists; it is defined in if statements
  if ("CohensD" %in% type){
    numGroupSD = sapply(numGroupVals, sd, na.rm=TRUE)
    denGroupSD = sapply(denGroupVals, sd, na.rm=TRUE)
    pooledSD = mapply(calc2GroupPooledSD, 
                      group1.n=numGroupN, group2.n=denGroupN, 
                      group1.sd=numGroupSD, group2.sd=denGroupSD, 
                      USE.NAMES = TRUE)
    toPlot$CohensD = (denMeans - numMeans) / pooledSD
    xAxisLab2 = paste0("difference of the means, ", denGroupName, " (n=", max(numGroupN), ") minus ", numGroupName, " (n=", max(denGroupN), "), over pooled sd")
    if (length(type) > 1){
      comments = c(comments, paste("Cohen's d is", xAxisLab2))
    }
    header = c(header, paste("CohensD:", xAxisLab2))
  }
  
  if ("foldChange" %in% type){
    toPlot$foldChange = numMeans / denMeans
    toPlot$scaledFC = do.call("log2", list(x=toPlot$foldChange))
    xAxisLab2 = paste0(numGroupName, " (n=", max(numGroupN), ") relative to ", denGroupName, " (n=", max(denGroupN), ")")
    if (length(type) > 1){
      comments = c(comments, paste("Fold change is", xAxisLab2, "on a log2 scale."))
    }
    header = c(header, paste("foldChange:", xAxisLab2))
    header = c(header, paste("scaledFC:", xAxisLab2, "on a log2 scale."))
  }
  if ("rSquared" %in% type){
    if (is.null(r2vals)){
      stop("r2vals must be supplied.")
    }else{
      toPlot$rSquared = r2vals[row.names(toPlot)]
      xAxisLab2 = ""
      r2comment = "r-squared values are taken from the CalculateStats module."
      if (length(type) > 1){
        comments = c(comments, r2comment)
      }
      header = c(header, paste("rSquared:", r2comment))
    }
  }
  
  # select top [maxBars] most changed OTUs
  # cases where one group is all-zeros is treated at most changed
  toPlot = toPlot[order(abs(toPlot[,orderByColumn]), decreasing = T),] #highest abs on top
  maxBars = min(c(getProperty("r_PlotEffectSize.maxNumTaxa", 40), nrow(toPlot)))
  toPlot$plotPriority = 1:nrow(toPlot)
  header = c(header, paste0('plotPriority: the rank of this OTU when determineing the "most changed" using abs(',orderByColumn,'); number of OTUs plotted can configured with r_PlotEffectSize.maxNumTaxa or over-riden using r_PlotEffectSize.taxa.'))
  toPlot$includeInPlot = toPlot$plotPriority <= maxBars
  comments[1] = paste0("Showing top ", maxBars, " most changed OTUs ", viableOTUs[["comment"]])
  header = c(header, "includeInPlot: will this otu be included in the plot.")
  
  # order OTUs to plot
  ordNames = row.names(toPlot)[order(toPlot[,orderByColumn])]
  toPlot = toPlot[ordNames,] #lowest values at top, barplot plots from bottom
  #
  # save a table the user can reference
  if (!is.null(saveRefTable)){
    toPrint=toPlot
    names(toPrint)[2:3] = paste(c(numGroupName, denGroupName), "mean", sep=".")
    header = c(header, "plot.location: vertical location of the bar in the plot")
    toPrint$plot.location = NA
    toPrint[1:sum(toPrint$includeInPlot),"plot.location"] = sum(toPrint$includeInPlot):1
    toPrint = toPrint[order(toPrint$plot.location),]
    header = paste("#", header)
    writeLines(header, con=saveRefTable)
    suppressWarnings(write.table(toPrint, file=saveRefTable, quote=FALSE, sep="\t", row.names = FALSE, append = TRUE))
    message( "Saved reference table to", saveRefTable )
  }
  
  # get rid of the rows that will not be plotted
  toPlot = toPlot[toPlot$includeInPlot,]
  return(list(toPlot=toPlot, comments=comments, xAxisLab2=xAxisLab2))
}

# Select the OTU's that qualify to be in the plot
selectViableOTUs <- function(group1, group2, pvals=NULL){
  # group1 - OTUs that are viable for group 1
  # group2 - OTUs that are viable for group 2
  # pvals - named vector of pvalues, taken from calcStats (which test is configurable).
  ##   names of pvals are OTU's, and should match num(den)GroupVals column names.

  sharedOTUs = intersect(group1, group2)
  pvalIncludeBar = getProperty("r_PlotEffectSize.excludePvalAbove", 1)
  userOTUs = getProperty("r_PlotEffectSize.taxa")
  if( !is.null(pvals) && is.null(userOTUs) ){
    sigOTUs = names(pvals)[pvals <= pvalIncludeBar]
    if (length(sigOTUs) == 0){
      stop(paste("Stop Plotting: Provided", length(pvals), "pvalues, \nwith", length(sigOTUs), "below the provided threshold:", pvalIncludeBar))
    }
    plotOTUs = intersect(sigOTUs, sharedOTUs)
    comment=paste0("out of ", length(plotOTUs), " OTUs with -value <= ", pvalIncludeBar, ".")
  }else if (!is.null(userOTUs)){
    plotOTUs = intersect(userOTUs, sharedOTUs)
    comment=paste0("out of ", length(plotOTUs), " user-supplied OTUs.")
  }else{
    plotOTUs = sharedOTUs
    comment=paste0("out of ", length(plotOTUs), " reported OTUs.")
  }
  if(is.null(plotOTUs) || length(plotOTUs) == 0){
    stop("Stop Plotting: No qualifying OTUs to plot.")
  }
  return(list(plotOTUs=plotOTUs, comment=comment))
}


calcPooledSD <- function(group.n, group.sd){
  # group.n - the number of samples in each group
  # group.sh - the within-group standard deviation for each group
  # formula taken from https://en.wikipedia.org/wiki/Pooled_variance#Computation
  group.var = group.sd^2
  pooled.var = sum( (group.n - 1) * group.var) / sum(group.n - 1)
  return(sqrt(pooled.var))
}

calc2GroupPooledSD <- function(group1.n, group2.n, group1.sd, group2.sd){
  return(calcPooledSD(group.n=c(group1.n, group2.n), group.sd=c(group1.sd, group2.sd)))
}


# If this function reaches the end, it returns TRUE.
drawPlot <- function(toPlot, title, barSizeColumn, xAxisLab, comments, xAxisLab2=NULL){
  # toPlot - data frame of values to plot. 
  #  columns in toPlot should include: columns specified by barSizeColumn; 
  #  and optionally: infUp, infDown, pvals, color, OTU
  #  If OTU column is present, that will used for labels, otherwise row.names will be used
  # title - main title for the plot
  # barSizeColumn - character (or potentially integer) giving the column from toPlot to be used to create bars, currently supports length 1.
  # xAxisLab - label to use for x-axis
  # comments - string(s) to add to bottom of the plot to inform the user
  # xAxisLab2 - Optional 2nd string to plot below the x-axis label (added explaination of x-axis)
  
  par( mar=c(6, 5, 2, 5), oma=c(0,0,0,0) )
  
  # Check required values in toPlot
  if (is.null(toPlot[,barSizeColumn])){ stop(paste('Input data frame [toPlot] must include a barSizeColumn ("', barSizeColumn, '") column.')) }

  if (is.null(toPlot$infUp)){ 
    toPlot$infUp=FALSE
    toPlot$infUp[toPlot[,barSizeColumn] == Inf] = TRUE
  }
  if (is.null(toPlot$infDown)){
    toPlot$infDown=FALSE
    toPlot$infDown[toPlot[,barSizeColumn] == -Inf] = TRUE
  }
  if (is.null(toPlot$color)){
    toPlot$color = NA
    toPlot$color[toPlot[,barSizeColumn] > 0] = "darkorange2"
    toPlot$color[toPlot[,barSizeColumn] < 0] = "burlywood1"
  }
  if (is.null(toPlot$OTU)){ toPlot$OTU = row.names(toPlot)}
  
  # determine the direction for each bar (or where "Inf" should be written)
  left = which(toPlot[,barSizeColumn] > 0) #where text goes on the left
  right = which(toPlot[,barSizeColumn] < 0)
  # cases where one group is all-zeros is plotted as 0 
  toPlot[(toPlot$infUp || toPlot$infDown), barSizeColumn] = 0 # bar gets a space, but no visible bar is plotted.
  
    # Determine plot size based on number of bars to plot, and lower lines needed for axis label and comments
    # set mar[1] to fit the axis labels and comments
    # allow a one line gap between axis labels and comments
    mars = par("mar")
    mars[1] = 1.5 + length(xAxisLab) + length(xAxisLab2) + length(comments) + ifelse(length(comments)>0, 1, 0) 
    par(mar=mars)

    fixedBarHeightInches = max(par("mai") / par("mar"), na.rm=TRUE) # 4 vals should all be the same but any could be /0
    # set bar width + space to this many inches
  plotRegionHeightInches = dev.size()[2]
  plotMarginsHeightInches = par("mai")[1] + par("mai")[3] + par("omi")[1] + par("omi")[3]
  inchesForBars = nrow(toPlot) * fixedBarHeightInches
  inchesToRemove = plotRegionHeightInches - plotMarginsHeightInches - inchesForBars
   
    if(inchesToRemove > 0){
	    mais = par("mai")
	    mais[1] = mais[1] + inchesToRemove
	    par(mai=mais)
    }else{
    		message( c( "Not enough space in plot for", nrow(toPlot), "bars with", fixedBarHeightInches, 
                  " for each bar. Bar widths will be set to fit the space.") )
    }
  
  # determine plot dimensions
  xmin = min( toPlot[,barSizeColumn] )
  xmax = max( toPlot[,barSizeColumn] )
  width = max(abs(c(xmin, xmax)))
  if( width == 0 ) width = 1
  xmin = -width
  xmax = width

  barWidth=1
  barSpace=.2
  bp = barplot(toPlot[,barSizeColumn], horiz=TRUE, plot=FALSE, width = barWidth, space = barSpace) # this one is not plotted, its just a reference
  row.names(bp) = row.names(toPlot)

  # plot area and outer text
  plot(x=0, y=1, type='n',  xlim=c(xmin, xmax), ylim=c(0, max(bp)+(barWidth/2)+barSpace), xlab="", ylab="", axes=FALSE)
  title(main=title, line=1)
  lowerLine = 2
  mtext(text=xAxisLab, side=1, line=lowerLine)
  if (!is.null(xAxisLab2)){
    lowerLine = lowerLine + 1
    mtext(text=xAxisLab2, side=1, line=lowerLine)
  }
  lowerLine = lowerLine + 2
  for( comment in comments ){
    mtext(comment, side=1, line=lowerLine, adj=0)
    lowerLine = lowerLine + 1
  }
  
  # x-axis, axis lines and inner text
  # The vertical lines should match the axis tick marks, and they should be behind the bars,
  # and they should not be drawn where text will be drawn.
  ax = axis(side=1)
  abline(v=0)
  
  #bottom of plot, bar midpoints, top
  vertRef = c(0, bp, par("usr")[4])
  vertMidLine = (vertRef[length(right)+1] + vertRef[length(right)+2])/2
  if (length(left)==0){ vertMidLine = par("usr")[4] }
  if (length(right)==0){ vertMidLine = par("usr")[3] }
  if (length(left) > 0){
    segments(x0=ax[ax>0], y0=vertMidLine, y1=par("usr")[4], col=gray(.8), lwd=.5)
    text(x=0, y=bp[left], labels=toPlot$OTU[left], pos=2, xpd=TRUE)
  }
  if (length(right) > 0){
    segments(x0=ax[ax<0], y0=par("usr")[3], y1=vertMidLine, col=gray(.8), lwd=.5)
    text(x=0, y=bp[right], labels=toPlot$OTU[right], pos=4, xpd=TRUE)
  }
  if (sum(toPlot$infUp) > 0){
    text(x=0, y=bp[toPlot$infUp,], labels="Inf", pos=4, xpd=TRUE)
  }
  if (sum(toPlot$infDown) > 0){
    text(x=0, y=bp[toPlot$infDown,], labels="-Inf", pos=2, xpd=TRUE)
  }
  
  bp = barplot(toPlot[,barSizeColumn], horiz=TRUE, width = barWidth, space = barSpace,
               add=TRUE, col=toPlot$color, border="black")
  row.names(bp) = row.names(toPlot)
  
  # plot the stars
  pvalStar = getProperty("r.pvalCutoff")
  if (!is.null(pvalStar) && !is.null(toPlot$pvalue)){
    starOTUs = row.names(toPlot)[toPlot$pvalue <= pvalStar]
    if ( length(starOTUs) > 0 ){
      starBarGap = 0.03 * par("usr")[2]
      xPlusGap = toPlot[starOTUs,barSizeColumn] + ifelse(toPlot[starOTUs,barSizeColumn] > 0, starBarGap, (-1 * starBarGap))
      points(x=xPlusGap, y=bp[starOTUs,], pch="*", col=getProperty("r.colorHighlight", "red"), xpd=TRUE)
    }
    mtext(paste0("( * ) -value <= ", pvalStar), side=3, line=0, adj=0)
  }
}


message("Current Working directory: ",getwd())

args = commandArgs(trailingOnly = TRUE)
message("all args: ", args)
level = args[1]
message("Running script for level: ", level)

source("../resources/BioLockJ_Lib.R")

main()

sessionInfo()

