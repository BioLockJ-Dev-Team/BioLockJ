# BioLockJ_Lib.R contains the library of functions shared by multiple BioLockJ R script modules. 

# Add value to vector v and assign name 
addNamedVectorElement <- function( v, name, value ) {
	v[length(v) + 1] = value
	names(v)[length(v)] = name
	return( v )
}

# Add a page number in the lower right corner of the page
addPageNumber <- function( pageNum ){
	mtext (pageNum, side=1, outer=TRUE, adj=1 )
}

# Add text to the bottom of the page, centered
addPageFooter <- function(text, line=0){
	if( text == "pAbund" ) text = "Pathway Abundance"
	if( text == "pCovg" ) text = "Pathway Coverage"
	if( text == "geneFam" ) text = "Gene Family"
	mtext(text, side=1, outer=TRUE, line=line, adj=0.5)
}

# Add a page title
addPageTitle <- function( main ) {
	mtext(main, side=3, outer=TRUE, font=par("font.main"), cex=par("cex.main"), line=1)
}

# Return P value formated with sprintf as defined in MASTER Config r.pValFormat, otherwise use %1.2g default
displayCalc <- function( pval ) {
	return( paste( sprintf(getProperty("r.pValFormat", "%1.2g"), pval) ) )
}

# Return TRUE if BioLock property r.debug=Y, otherwise return FALSE
doDebug <- function() {
	return( getProperty( "r.debug", FALSE ) )
}

# Return vector of binary fields or an empty vector
getBinaryFields <- function() {
	return( getProperty("R_internal.binaryFields", vector( mode="character" ) ) )
}

# Return countTable column indexes for the given colNames
getColIndexes <- function( countTable, colNames ) {
	cols = vector( mode="integer" )
	if( length(colNames) > 0 ) {
		for( i in 1:length(colNames) ) {
			cols[i] = grep(TRUE, colnames(countTable)==colNames[i])
		}
	}
	return( cols )
}

# Parse MASTER config for property value, if undefined return default defaultVal
# save all properties in propCache upon 1st request
getConfig <- function( name, defaultVal=NULL ) {
	if( is.null( propCache ) ) {
		propCache = read.properties( getMasterConfigFile() )
	}
	
	prop = propCache[[ name ]]
	value=str_trim( prop )

	if( is.null( prop ) ) return( defaultVal )
	
	if( value == "Y" ) return( TRUE )
	if( value == "N" ) return( FALSE )
	
	if( !is.na( as.numeric( value ) ) && grepl( ",", prop ) ) {
		return( as.numeric( unlist( strsplit( prop, "," ) ) ) )
	}
	if( is.character( value ) && grepl( ",", value ) ) {
		return( str_trim( unlist( strsplit( value, "," ) ) ) )
	}
	if( !is.na( as.numeric( value ) ) ) return( as.numeric( value ) )
	
	return( value )	
}

# Return the data columns (no metadata)
getCountTable <- function( level ){
	fullTable = getCountMetaTable( level )
	if( is.null( fullTable ) ) return( NULL )
	lastCountCol = ncol(fullTable) - numMetaCols()
	return ( fullTable[1:lastCountCol] )
}

# Returns BioLockJ generated Count + Metadata Table with a standard format:
# 1. 1st column contains Sample IDs
# 2. Next group of columns contain numeric count data (derived from sample analysis)
# 3. Last group of columns contain the metadata columns ( call numMetaCols() to find out how many )
getCountMetaTable <- function( level ) {
	if( !is.null( countMetaTable ) ) {
		return( countMetaTable )
	}

	countMetaFile = pipelineFile( paste0( level, "_metaMerged.tsv$" ) )
	if( is.null( countMetaFile )  ) {
		logInfo( c( "BioLockJ_Lib.R getCountMetaTable(", level, ") found none!" ) )
		return( NULL )
	}
	
	countMetaTable = readBljTable( countMetaFile )
	if( nrow( countMetaTable ) == 0 ) {
		logInfo( c( "BioLockJ_Lib.R getCountMetaTable(", level, ") returned an empty table with header row:", colnames( countMetaTable ) ) )
		countMetaTable = NULL
		return( NULL )
	}
	
	logInfo( "Read count table", countMetaFile )
	return( countMetaTable )
}

# Return list, each record contains the count-data  associated with a unique value for the given nominal metadata field (metaCol)
getFactorGroups <- function( countMetaTable, metaCol, taxaCol ) {
	vals = list()
	options = levels( metaCol )
	for( i in 1:length(options) ) {
		vals[[i]] = countMetaTable[metaCol==options[i], taxaCol]
	}
	return( vals )
}

# Return  name of the R module log file using the given name
getLogFile <- function( name ) {
	return( file.path( getTempDir(), paste0( moduleScriptName(), "_", name, ".log") ) )
}

# Return the name of the BioLockJ MASTER Config file
getMasterConfigFile <- function() {
	testDir = dirname( getwd() )
	propFile = vector( mode="character" )
	while( length( propFile ) == 0 && testDir != "/" ) {
		propFile = list.files( testDir, "MASTER.*.properties", full.names=TRUE )
		testDir = dirname( testDir )
	}
	if( length( propFile ) == 0 ) {
		stop( "MASTER property file not found!" )
	}
	return( propFile )
}

# Return a data frame of the metadata from a biolockj data table with merged metadata.
getMetaData <- function( level=taxaLevels()[1] ){
	fullTable = getCountMetaTable( level )
	if( is.null( fullTable ) ) return( NULL )
	firstMetaCol = ncol(fullTable) - numMetaCols() + 1
	return( fullTable[firstMetaCol:ncol(fullTable)] )
}

# If downloaded with scp, all files share 1 directory, so return getPipelineDir() 
# Otherwise, script path like: piplineDir/moduleDir/script/MAIN*.R, so return moduleDir (the dir 2 levels above script)  
getModuleDir <- function() {
	if( getPipelineDir() == dirname( getModuleScript() ) ) {
		return( getPipelineDir() )
	}
	return( dirname( dirname( getModuleScript() ) ) )
}

# Return vector of nominal fields or an empty vector
getNominalFields <- function() {
	return( getProperty("R_internal.nominalFields", vector( mode="character" ) ) )
}

# Return vector of numeric fields or an empty vector
getNumericFields <- function() {
	return( getProperty("R_internal.numericFields", vector( mode="character" ) ) )
}

# Get the temp dir for the current module, if it does not exist, create it.
getOutputDir <- function(){
	path = file.path( file.path(getModuleDir(), "output") )
	if ( !dir.exists(path)){
		dir.create( path )
	}
	return( path )
}

# Return file path of file in rootDir, with the pipeline name appended as a prefix to name
getPath <- function( rootDir, name ) {
	return( file.path( rootDir, paste0( basename( getPipelineDir() ), "_", name ) ) )
}

# Return the pipeline root directory
getPipelineDir <- function() {
	return( dirname( getMasterConfigFile() ) )
}

# Return property value from MASTER Config file, otherwise return the defaultVal
getProperty <- function( name, defaultVal=NULL ) {
	return ( suppressWarnings( getConfig( name, defaultVal ) ) )
}

# Return vector that includs all binary, nominal, and numeric fields or an empty vector
getReportFields <- function() {
	return( c( getBinaryFields(), getNominalFields(), getNumericFields() ) )
}

# Display R^2 label and value
displayR2 <- function( val ) {
	return( bquote( paste( R^2, ": ", .( displayCalc( val ) ) ) ) )
}

# Return the most recent stats file at the given level based on the suffix returned by statsFileSuffix()
getStatsTable <- function( level, parametric=NULL, adjusted=TRUE ) {
	statsFile = pipelineFile( paste0( level, "_", statsFileSuffix( parametric, adjusted ), "$" ) )
	if( is.null( statsFile )  ) {
		logInfo( paste0( "BioLockJ_Lib.R function --> getStatsTable( level=", level, 
										 ", parametric=", parametric, ", adjusted=", adjusted, " ) returned NULL" ) )
		return( NULL )
	}
	
	statsTable = read.table( statsFile, header=TRUE, sep="\t", row.names=1, check.names=FALSE )
	if( nrow( statsTable ) == 0 ) {
		logInfo( paste0( "BioLockJ_Lib.R function --> getStatsTable( level=", level, ", parametric=", parametric, ", adjusted=", adjusted, " ) returned an empty table with only the header row:", colnames( statsTable ) ) )
		return( NULL )
	}
	
	logInfo( "Read stats table", statsFile )
	return( statsTable )
}

# Get the temp dir for the current module, if it does not exist, create it.
getTempDir <- function(){
	path = file.path( file.path(getModuleDir(), "temp") )
	if ( !dir.exists(path)){
		dir.create( path )
	}
	return( path )
}


# Return name of statistical test used to generate P-Values 
# If att=NULL, return all tests as vector with test-color as element names
getTestName <- function( field=NULL, isParametric=TRUE ) {
   tests = c( "T-Test", "Wilcox", "ANOVA", "Kruskal", "Pearson", "Kendall" )
   names( tests ) = c( "coral", "dodgerblue2", "darkgoldenrod1", "cornflowerblue", "tan1", "aquamarine3" )
   if( is.null( field ) && isParametric ) return( tests[ c(1,3,5) ] )
   if( is.null( field ) && !isParametric ) return( tests[ c(2,4,6) ] )
   if( field %in% getBinaryFields() && isParametric ) return( tests[1] )
   if( field %in% getBinaryFields() && !isParametric ) return( tests[2] )
   if( field %in% getNominalFields() && isParametric ) return( tests[3] )
   if( field %in% getNominalFields() && !isParametric ) return( tests[4] )
   if( field %in% getNumericFields() && isParametric ) return( tests[5] )
   if( field %in% getNumericFields() && !isParametric ) return( tests[6] )
}

# Return named vector values for the given name
getValuesByName <- function( vals, name ) {
	return( as.vector( vals[names(vals)==name] ) )
}

# Import libraries and abort program with descriptive error
importLibs <- function( libs ) {
	errors = vector( mode="character" )
	for( i in 1:length( libs ) ) {
		if ( !library( libs[i], logical.return=TRUE, character.only=TRUE ) ) {
			errors[ length( errors ) + 1 ] = paste0( "Missing R library, please run install.packages(\"", libs[i], "\") from within R" )
		}
	}   
	
	if( length( errors ) > 0 ) {
		writeErrors( errors )
	}
}

# Log the msg if Config property r.debug=Y, otherwise do nothing
# Append semicolon to label, unless it already exists
logInfo <- function( info, msg=NULL ) {
	if( doDebug() ) {
		if( is.null( msg ) ){
			msg = info
		} else {
			if( !endsWith( trimws( info ), ":" ) ) info = paste0( trimws( info ), ":" )
			msg = c( trimws( info ), msg )
		}
		cat( msg, "\n" )
	}
}

# Return number of metadata columns appended to sample count tables
numMetaCols <- function() {
	return( getProperty( "R_internal.numMetaCols" ) )
}


# Given a vector of files, return the most recently modified
pickLatestFile <- function( files ) {
	newestFile = NULL
	for( i in 1:length( files ) ) {
		if( !is.null( files[i] ) && (is.null( newestFile ) || file.info( files[i] )[ "mtime" ] > file.info( newestFile )[ "mtime" ]) ) { 
			newestFile = files[i] 
		}
	}
	return( newestFile )
}

# Return a file matching the pattern underwhere under the pipeline root directory
# If multiple results are found, return the most recent version
# If dir is undefined, look in pipeline dir
# If no results, check in Config property input.dirPaths
pipelineFile <- function( pattern, dir=getPipelineDir() ) {
	
	if( length( dir ) > 1 ) {
		for( i in 1:length( dir ) ) {
			inputDirResults = pipelineFile( pattern, dir[i] )
			if( !is.null( inputDirResults ) ) {
				return( inputDirResults )
			}
		}
		writeErrors( c( paste0( "pipelineFile(", pattern, ",", dir, ") returned NULL" ) ) )
	}
	
	logInfo( c( "Search:", dir, "for input files matching pattern:", pattern ) )
	results = list.files( dir, pattern, full.names=TRUE, recursive=TRUE )
	if( length( results ) == 0 ) {
		if( ! dir %in% getProperty("input.dirPaths") ) {
			return( pipelineFile( pattern, getProperty("input.dirPaths") ) ) 
		}
		else {
			return( NULL )
		}
	}
	
	return( pickLatestFile( results ) )
}

# Create an empty plot and add text. 
# Ideal when explaining why a plot is blank, or plot explanations within a plot document.
plotPlainText <- function(textToPrint, align="center", maxCharWidth=55, verticalLines=NULL, ... ){
	# Args:
	#  textToPrint - a vector of lines to print; long strings are subjected to crude text-wrapping.
	#  align - the horizontal text alignment, options are "left", "center" or "right".
	#  maxCharWidth - max number of character to allow per line, used to impose crude text wrapping.
	#  verticalLines - number of vertical lines of space in the plot
	#  ... - further parameters passed to text()
	alignment = c(left=0, center=0.5, right=1)
	# TODO: find a smarter function to replace this crude text wrapping
	if (is.null(verticalLines)) {
		# This estimates how many lines tall the plot is.
		verticalLines = floor( par("pin")[2] / (max(par("mai") / par("mar"), na.rm=TRUE)) ) - 1
	}
	if (any(nchar(textToPrint) > maxCharWidth)){
		# crude text-wrapping
		bigNumber = max(nchar(textToPrint))*2
		textToPrint = sapply(textToPrint, substring, first=seq(0,bigNumber,maxCharWidth), last = seq(maxCharWidth-1, bigNumber, maxCharWidth))
		textToPrint = as.vector(textToPrint)
		textToPrint = textToPrint[ nchar(textToPrint) > 0 ]
	}
	if (length(textToPrint) > verticalLines) { logInfo("plotPlainText", "textToPrint was truncated.") }
	plot(c(0, 1), c(0, verticalLines+1), ann = FALSE, mar = c(0,0,0,0), 
			 bty = 'n', type = 'n', xaxt = 'n', yaxt = 'n')
	text(labels=textToPrint, adj=alignment[align], x = alignment[align], xpd=TRUE, 
			 y = verticalLines:(verticalLines-length(textToPrint)+1), ... )
}

# Read a table using the biolockj standards
readBljTable <- function( file ){
	return( read.table( file, header=TRUE, sep="\t", row.names=1, na.strings=getProperty( "metadata.nullValue" ), 
		check.names=FALSE, comment.char=getProperty( "metadata.commentChar", "" ) ) )
}

# This method returns 1 of 5 possible CalculateStats.R output file suffix values
# Use TRUE/FALSE params to obtain the correct p-value statistics file
# To retrieve the r^2 effect size file, use: buildStatsFileSuffix()
statsFileSuffix <- function( parametric=NULL, adjusted=TRUE ) {
	if( is.null( parametric ) ) return( "rSquaredVals.tsv" )
	if( parametric && adjusted ) return( "adjParPvals.tsv" )
	if( parametric ) return( "parametricPvals.tsv" )
	if( adjusted ) return( "adjNonParPvals.tsv" )
	return( "nonParametricPvals.tsv" )
}

# Return taxonomy levels or HumanN2 report types based on property R_internal.runHumann2
taxaLevels <- function() {
	levels = c()
	errMsg = "No levels found"
	if( getProperty( "R_internal.runHumann2", FALSE ) ) {
		if( !getProperty( "humann2.disablePathAbundance", FALSE ) ) levels[length(levels) + 1] = "pAbund" 
		if( !getProperty( "humann2.disablePathCoverage", FALSE ) ) levels[length(levels) + 1] = "pCovg" 
		if( !getProperty( "humann2.disableGeneFamilies", FALSE ) ) levels[length(levels) + 1] = "geneFam" 
		errMsg = "No HumanN2 Pathway or Gene Family reports found"
	} else levels = getProperty( "report.taxonomyLevels" )

	logInfo( c( "Found levels --> ", levels ) )
	if( length( levels ) == 0 ) writeErrors( c( errMsg ) )
	return( levels )
}


getColorFile <- function(){
  fileName = file.path(getPipelineDir(), "Colors.tsv")
  return( fileName )
}

readColorFile <- function(){
  fileName = getColorFile()
  if (!file.exists(fileName)){
    initColorFile(fileName)
  }
  return( read.table( fileName, sep = "\t", header=TRUE, comment.char = "", as.is=TRUE) )
}

writeColorFile <- function(df){
  fileName = getColorFile()
  logInfo("Updating color file: ", fileName)
  write.table(df, file=fileName, sep = "\t", col.names = TRUE, row.names = FALSE, quote=FALSE)
}

# Create the color file and add initial values
initColorFile <- function(fileName){
  premadeFile = getProperty("r.colorFile")
  if (!is.null(premadeFile) && file.exists(premadeFile) ){
    # note: the format of this file is checked in the java method: checkColorFile
    logInfo( c("Pulling in color reference file from: ", premadeFile) )
    file.copy(from=getProperty("r.colorFile"), to=getColorFile() )
  }else{
    df = data.frame(key=0, color=0)[0,]
    if (getProperty("r.useUniqueColors", FALSE)){
      df = getAllUniqueColors(df)
    }
    writeColorFile(df)
  }
}

# append rows to the color file giving the colors for the values of a particular metadata column
appendColorRef <- function(metaColumn, metaTable){
  levels = levels(as.factor(metaTable[,metaColumn]))
  keys = paste0(metaColumn, ":", levels)
  colors = setColors( length(levels) )
  df = data.frame(key=keys, color=colors)
  logInfo( c( "Assigning colors: ", paste(colors, "for", keys, collapse= ", ")) )
  df=rbind(readColorFile(), df)
  writeColorFile(df)
  return( readColorFile() )
}

# Return n colors using the palette defined in the MASTER Config
setColors <- function( n ) {
  if (n == 1 ){
    palette = getProperty("r.colorPalette", "#00AF66FF")
  }else if (n < 4 ){
    palette = getProperty("r.colorPalette", c("#5C88DAFF", "#84BD00FF", "#FFCD00FF"))
  }else if (n < 10){
    palette = getProperty("r.colorPalette", "Set1")
  }else{
    palette = getProperty("r.colorPalette", "ucscgb")
  }
  logInfo( c("Using \"", palette , "\" for property r.colorPalette.") )
  colors = get_palette( palette, n )
  return( colors )
}

# get the colors to use for a particular metadata column
getColorsByCategory <- function( metaColumn, metaTable=getMetaData()){
  colorRef = readColorFile()
  rowsWanted = grep(paste0(metaColumn, ":"), colorRef$key)
  if (length(rowsWanted)==0){
    colorRef = appendColorRef(metaColumn, metaTable)
    rowsWanted = grep(paste0(metaColumn, ":"), colorRef$key)
  }
  colorRef = colorRef[rowsWanted,]
  colors = colorRef$color
  names(colors) = gsub(paste0(metaColumn, ":"), "", colorRef$key)
  logInfo( c( "Using colors: ", paste(colors, "for", names(colors), collapse= ", ")) )
  return(colors)
}

# Return r.colorHighlight if any of the input values meet the r.pvalCutoff, otherwise return r.colorBase
getColor <- function( v ) {
  for( i in 1:length(v) ) {
    if( grepl("e", v[i]) || !is.na(v[i]) && !is.nan(v[i]) && ( v[i] <= getProperty("r.pvalCutoff", 0.05) ) ) {
      return( getProperty("r.colorHighlight", "red") )
    } 
  }
  return( getProperty("r.colorBase", "black") )
}

# Select colors for each category so each category is different even across different metadata fields.
# Returns a named list (named for meta data columns) of named vecotors (named for levels in column)
getAllUniqueColors <- function( df ){
  metaTable = getMetaData()
  categoricals = c(getBinaryFields(), getNominalFields())
  metaTable = metaTable[names(metaTable) %in% categoricals]
  if (ncol(metaTable) < 1){
    logInfo( "No categorical metadata fields.  Returning null." )
    return(NULL)
  }
  allLevels = unlist(sapply(metaTable, function(x){levels(as.factor(x))}))
  boxColors = setColors( length(allLevels) )
  levelsPerField = sapply(metaTable, function(x){length(levels(as.factor(x)))})
  logInfo ( paste( "Selected", length(boxColors), "colors to describe", ncol(metaTable), "categorical variables." ) )
  f = mapply(x=names(levelsPerField), each=levelsPerField, rep, SIMPLIFY = FALSE)
  keys = paste0(f, ":", allLevels)
  newRows = data.frame(key=keys, color=boxColors)
  return( rbind(df, newRows) )
}


# Import standard shared libraries
importLibs( c( "properties", "stringr", "ggpubr" ) )
propCache = NULL
countMetaTable = NULL
