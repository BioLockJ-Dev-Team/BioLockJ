#!/bin/bash

# BioLockJ v1.3.15-dev: ${scriptDir}/MAIN_02_SampleFile.sh

export BLJ=/Users/ieclabau/git/BioLockJ

pipeDir="/Users/ieclabau/git/sheepdog_testing_suite/MockMain/pipelines/ForEach_2020Dec10"
modDir="${pipeDir}/02_SampleFile"
scriptDir="${modDir}/script"
tempDir="${modDir}/temp"
logDir="${modDir}/log"
outputDir="${modDir}/output"

touch "${scriptDir}/MAIN_02_SampleFile.sh_Started"

exec 1>${logDir}/MAIN.log
exec 2>&1
cd ${scriptDir}

function scriptFailed() {
    echo "Line #${2} failure status code [ ${3} ]:  ${1}" >> "${scriptDir}/MAIN_02_SampleFile.sh_Failures"
    exit ${3}
}

function executeLine() {
    ${1}
    statusCode=$?
    [ ${statusCode} -ne 0 ] && scriptFailed "${1}" ${2} ${statusCode}
}

executeLine "${scriptDir}/02.0_ForEachFile.sh" ${LINENO}

touch "${scriptDir}/MAIN_02_SampleFile.sh_Success"
