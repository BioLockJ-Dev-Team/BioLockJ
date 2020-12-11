#!/bin/bash

# BioLockJ.v1.3.15-dev: ${scriptDir}/05.0_ForEachLoop.sh

export BLJ=/Users/ieclabau/git/BioLockJ

pipeDir="/Users/ieclabau/git/sheepdog_testing_suite/MockMain/pipelines/ForEach_2020Dec10"
modDir="${pipeDir}/05_ForEachLoop"
scriptDir="${modDir}/script"
tempDir="${modDir}/temp"
logDir="${modDir}/log"
outputDir="${modDir}/output"

touch "${scriptDir}/05.0_ForEachLoop.sh_Started"

exec 1>${logDir}/05.0_ForEachLoop.log
exec 2>&1

cd ${scriptDir}

function scriptFailed() {
    echo "Line #${2} failure status code [ ${3} ]:  ${1}" >> "${scriptDir}/05.0_ForEachLoop.sh_Failures"
    exit ${3}
}

function executeLine() {
    ${1}
    statusCode=$?
    [ ${statusCode} -ne 0 ] && scriptFailed "${1}" ${2} ${statusCode}
}

executeLine "${modDir}/resources/echoArgs.sh Mars has two moons " ${LINENO}
executeLine "${modDir}/resources/echoArgs.sh Phobos " ${LINENO}
executeLine "${modDir}/resources/echoArgs.sh Deimos " ${LINENO}
touch "${scriptDir}/05.0_ForEachLoop.sh_Success"
echo 'Created Success flag.'
