#!/bin/bash

# BioLockJ.v1.3.15-dev: ${scriptDir}/04.0_ForEachFile.sh

export BLJ=/Users/ieclabau/git/BioLockJ

pipeDir="/Users/ieclabau/git/sheepdog_testing_suite/MockMain/pipelines/ForEach_2020Dec10"
modDir="${pipeDir}/04_LevelFile"
scriptDir="${modDir}/script"
tempDir="${modDir}/temp"
logDir="${modDir}/log"
outputDir="${modDir}/output"

touch "${scriptDir}/04.0_ForEachFile.sh_Started"

exec 1>${logDir}/04.0_ForEachFile.log
exec 2>&1

cd ${scriptDir}

function scriptFailed() {
    echo "Line #${2} failure status code [ ${3} ]:  ${1}" >> "${scriptDir}/04.0_ForEachFile.sh_Failures"
    exit ${3}
}

function executeLine() {
    ${1}
    statusCode=$?
    [ ${statusCode} -ne 0 ] && scriptFailed "${1}" ${2} ${statusCode}
}

executeLine "${modDir}/resources/countChars.sh ${pipeDir}/03_ForEachLevel/output/class.txt " ${LINENO}
executeLine "${modDir}/resources/countChars.sh ${pipeDir}/03_ForEachLevel/output/family.txt " ${LINENO}
executeLine "${modDir}/resources/countChars.sh ${pipeDir}/03_ForEachLevel/output/genus.txt " ${LINENO}
executeLine "${modDir}/resources/countChars.sh ${pipeDir}/03_ForEachLevel/output/order.txt " ${LINENO}
touch "${scriptDir}/04.0_ForEachFile.sh_Success"
echo 'Created Success flag.'
