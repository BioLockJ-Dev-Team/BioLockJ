#!/bin/bash

# BioLockJ.v1.3.15-dev: ${scriptDir}/02.0_GenMod.sh

export BLJ=/Users/ieclabau/git/BioLockJ

pipeDir="/Users/ieclabau/git/sheepdog_testing_suite/MockMain/pipelines/GenModDemo_2020Dec10"
modDir="${pipeDir}/02_Hello_Python"
scriptDir="${modDir}/script"
tempDir="${modDir}/temp"
logDir="${modDir}/log"
outputDir="${modDir}/output"

touch "${scriptDir}/02.0_GenMod.sh_Started"

exec 1>${logDir}/02.0_GenMod.log
exec 2>&1

cd ${scriptDir}

function scriptFailed() {
    echo "Line #${2} failure status code [ ${3} ]:  ${1}" >> "${scriptDir}/02.0_GenMod.sh_Failures"
    exit ${3}
}

function executeLine() {
    ${1}
    statusCode=$?
    [ ${statusCode} -ne 0 ] && scriptFailed "${1}" ${2} ${statusCode}
}

executeLine "${modDir}/resources/pythonScript.py" ${LINENO}
touch "${scriptDir}/02.0_GenMod.sh_Success"
echo 'Created Success flag.'
