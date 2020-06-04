# MAIN.R (MAIN_<Rmodule>.R)

# SET TO SCRIPT DIRECTORY WITH THIS MAIN.R SCRIPT TO RUN IN INTERACTIVE MORE
R_PATH = NULL

getInteractiveMain <- function() {
	if( !is.null( R_PATH ) ) {
		filePath = list.files( R_PATH,  paste0( "MAIN.*R$" ), full.names=TRUE, recursive=TRUE )
    		if( length( filePath ) == 1 ) return( filePath )
	}
	return( NULL )
}

# Create script status indicator file by appending suffix indicating pass/fail
# BioLockJ Java program checks for existence of new file named R script + status indicator
# Warnings are generally unimportant so are saved as a hidden file: .warning 
# Save MAIN script R data if r.saveRData configured and print session info
checkStatus <- function() {
	conn = file( warningFile(), open="r" )
	warnings = readLines( conn )
	close( conn )
	errors = vector( mode="character" )
	if( length( warnings ) > 0 ) {
		for ( i in 1:length( warnings ) ) {
			if( grepl( "Error", warnings[i] ) ) {
				errors[ length( errors ) + 1 ] = warnings[i]
			}
		}
	}
	if( length( errors ) > 0 ) writeErrors( errors )
}

# Absolute path of the error file - in the same directory as the MAIN script
errorFile <- function() {
	return( paste0( getModuleScript(), "_Failures" ) )
}

# Wrap try( expr ) to sink() all warning & error messages to the warning file
# Warning file is parsed by checkStatus() for "Error" string.
# If no "Error" messages found, return TRUE; else return FALSE
executeTask <- function( name, expr ) {
	print( paste( "Execute Task [", name, "]" ) )
	log = file( warningFile(), open="at" )
	sink( log, type="message" )
	try( expr )
	sink( type="message" )
	close( log )
	checkStatus()
	print( paste( "Task Complete [", name, "]" ) )
}


# Return full path and name of this script
getModuleScript <- function() {
	if( is.null( moduleScript ) ) {
		initial.options = commandArgs(trailingOnly=FALSE)
		script.name <- sub("--file=", "", initial.options[grep("--file=", initial.options)])
		if( length( script.name ) == 0 ) {
			if( init( getInteractiveMain() ) ) {
				script.name = getInteractiveMain() 
			}
			else {
				msg0 = "\n\n---> BioLockJ R scripts are not interactive - getModuleScript() retrieves its absolute path from the \"--file\" runtime parameter.\n"
				msg1 = "---> Run \"Rscript <script.path>\" to execute the script, or hack a temporary solution into your local version of the module MAIN script.\n"
				msg2 = "---> To do so, modify getModuleScript() to return the hard coded absolute file path for your module MAIN script.\n"
				msg3 = "---> For more information, please visit the BioLockJ Wiki:  https://github.com/msioda/BioLockJ/wiki\n\n"
				stop( paste0( msg0, msg1, msg2, msg3 ) )
			}
		}
		moduleScript <<- normalizePath( script.name )
	}
	return( moduleScript )
}

# Load the BioLockJ library of shared R functions functions (BioLockJ_Lib.R)
importLib <- function() {
	source( file.path( dirname( getModuleScript() ), "BioLockJ_Lib.R" ) )
}

# Every R_Module has a template R script stored in $BLJ/resources/R
# This method initializes the template to load its functions before calling main()
importModuleScript <- function() {
	source( file.path( dirname( getModuleScript() ), moduleScriptName() ) )
}

# Initialize MAIN script core functions, failures will return FALSE.
init <- function( expr ) {
	return( !is.null( tryCatch( withCallingHandlers( expr ), error=function(e) { FALSE } ) ) )
}

# Every R_Module has a template R script stored in $BLJ/resources/R
# This method returns the template needed based on the MAIN script name
moduleScriptName <- function() {
	return( sub( "^MAIN_", "", basename( getModuleScript() ) ) )
}

# Remove old error/warning files
# Initialize BioLockJ_Lib.R
# Initialize BioModule R Template
prepRun <- function() {
	errMsg = "Initialization failure in MAIN script function  -->"
	if( !init( getModuleScript() ) ) stop( paste( errMsg, "getModuleScript()" ) )
	if( !init( warningFile() ) ) stop( paste( errMsg, "warningFile()" ) )
	if( !init( moduleScriptName() ) ) stop( paste( errMsg, "moduleScriptName()" ) )
	print( paste( "All", basename( getModuleScript() ), "functions successfully initialized" ) )
	if( file.exists( warningFile() ) ) file.remove( warningFile() )
	if( file.exists( errorFile() ) ) file.remove( errorFile() )
	executeTask( "Import BioLockJ_Lib.R", importLib() )
	executeTask( paste( "Import", moduleScriptName() ), importModuleScript() )
	print( paste( "All", basename( getModuleScript() ), "subscripts successfully imported from:", dirname( getModuleScript() ) ) )
}

# Execute prepRun()
# Execute BioModule R Template main()
# Execution errors at any point will abort the job and kill the pipeline
runModule <- function()
{
	prepRun()
	executeTask( paste0( "Execute[ ", moduleScriptName(), ":main() ]" ), main() )
	file.create( paste0( getModuleScript(), "_Success" ) )
	if ( getProperty( "r.saveRData", FALSE ) ) save.image( paste0( getModuleScript(), "Data" ) )
	print( paste( getModuleScript(), "status --> SUCCESS!" ) )
}

# Absolute path of the warning file - in the same directory as the MAIN script
warningFile <- function() {
	return( file.path( dirname( getModuleScript() ), ".warnings" ) )
}

# Method writes error msgs to the script _Failures file and aborts R program
writeErrors <- function( msgs ) {
	errorConn = file( errorFile(), open="at" )
	writeLines( msgs, errorConn )
	close( errorConn )
	stop( paste( getModuleScript(), "status --> FAILED!  Error details saved to:", errorFile() ) )
}

moduleScript = NULL

if( is.null( R_PATH ) ) runModule()
