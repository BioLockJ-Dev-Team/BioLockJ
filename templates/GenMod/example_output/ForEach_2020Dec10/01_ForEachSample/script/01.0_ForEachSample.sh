#!/bin/bash

# BioLockJ.v1.3.15-dev: ${scriptDir}/01.0_ForEachSample.sh

export BLJ=/Users/ieclabau/git/BioLockJ

pipeDir="/Users/ieclabau/git/sheepdog_testing_suite/MockMain/pipelines/ForEach_2020Dec10"
modDir="${pipeDir}/01_ForEachSample"
scriptDir="${modDir}/script"
tempDir="${modDir}/temp"
logDir="${modDir}/log"
outputDir="${modDir}/output"

touch "${scriptDir}/01.0_ForEachSample.sh_Started"

exec 1>${logDir}/01.0_ForEachSample.log
exec 2>&1

cd ${scriptDir}

function scriptFailed() {
    echo "Line #${2} failure status code [ ${3} ]:  ${1}" >> "${scriptDir}/01.0_ForEachSample.sh_Failures"
    exit ${3}
}

function executeLine() {
    ${1}
    statusCode=$?
    [ ${statusCode} -ne 0 ] && scriptFailed "${1}" ${2} ${statusCode}
}

executeLine "${modDir}/resources/showSampleMeta.sh Sample_1 " ${LINENO}
executeLine "${modDir}/resources/showSampleMeta.sh Sample_10 " ${LINENO}
executeLine "${modDir}/resources/showSampleMeta.sh Sample_11 " ${LINENO}
executeLine "${modDir}/resources/showSampleMeta.sh Sample_12 " ${LINENO}
executeLine "${modDir}/resources/showSampleMeta.sh Sample_2 " ${LINENO}
executeLine "${modDir}/resources/showSampleMeta.sh Sample_3 " ${LINENO}
executeLine "${modDir}/resources/showSampleMeta.sh Sample_4 " ${LINENO}
executeLine "${modDir}/resources/showSampleMeta.sh Sample_5 " ${LINENO}
executeLine "${modDir}/resources/showSampleMeta.sh Sample_6 " ${LINENO}
executeLine "${modDir}/resources/showSampleMeta.sh Sample_7 " ${LINENO}
executeLine "${modDir}/resources/showSampleMeta.sh Sample_8 " ${LINENO}
executeLine "${modDir}/resources/showSampleMeta.sh Sample_9 " ${LINENO}
touch "${scriptDir}/01.0_ForEachSample.sh_Success"
echo 'Created Success flag.'
