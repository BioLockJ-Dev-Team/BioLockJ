
# standard err/out are saved to log file in module log dir
message("Hello R !")

# Use relative paths to access files in this instance of this pipeline.
workingDir <- getwd()
moduleDir <- dirname(workingDir)
outputDir <- file.path(moduleDir, "output")
pipeRoot <- dirname(moduleDir)

message("Current working directory: ", workingDir)
message("This is module: ", basename(moduleDir))
message("The pipeline name is: ", basename(pipeRoot))
message("The pipeline root folder path is: ", pipeRoot)

# Access other modules' output
alias="Hello_World"
genModModule <- dir(pipeRoot, full.names=TRUE, pattern=alias)
outFiles <- dir(path=file.path(genModModule, "output"))

message("The ", alias, " module has folder: ", basename(genModModule))
message("Its output directory has ", length(outFiles), " file(s):")
message(paste(outFiles, sep=", "))

#fileName="time.txt"
file = outFiles[1]

message()
message("The ", file, " file reads: ")
message(readLines(file.path(genModModule, "output", file)))

writeLines("Hello R module output !", file.path(outputDir, "output.txt"))
