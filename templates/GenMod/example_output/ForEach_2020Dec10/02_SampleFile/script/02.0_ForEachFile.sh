#!/bin/bash

# BioLockJ.v1.3.15-dev: ${scriptDir}/02.0_ForEachFile.sh

export BLJ=/Users/ieclabau/git/BioLockJ

pipeDir="/Users/ieclabau/git/sheepdog_testing_suite/MockMain/pipelines/ForEach_2020Dec10"
modDir="${pipeDir}/02_SampleFile"
scriptDir="${modDir}/script"
tempDir="${modDir}/temp"
logDir="${modDir}/log"
outputDir="${modDir}/output"

touch "${scriptDir}/02.0_ForEachFile.sh_Started"

exec 1>${logDir}/02.0_ForEachFile.log
exec 2>&1

cd ${scriptDir}

function scriptFailed() {
    echo "Line #${2} failure status code [ ${3} ]:  ${1}" >> "${scriptDir}/02.0_ForEachFile.sh_Failures"
    exit ${3}
}

function executeLine() {
    ${1}
    statusCode=$?
    [ ${statusCode} -ne 0 ] && scriptFailed "${1}" ${2} ${statusCode}
}

executeLine "${modDir}/resources/addMetaHeader.sh ${pipeDir}/01_ForEachSample/output/Sample_1.txt " ${LINENO}
executeLine "${modDir}/resources/addMetaHeader.sh ${pipeDir}/01_ForEachSample/output/Sample_10.txt " ${LINENO}
executeLine "${modDir}/resources/addMetaHeader.sh ${pipeDir}/01_ForEachSample/output/Sample_11.txt " ${LINENO}
executeLine "${modDir}/resources/addMetaHeader.sh ${pipeDir}/01_ForEachSample/output/Sample_12.txt " ${LINENO}
executeLine "${modDir}/resources/addMetaHeader.sh ${pipeDir}/01_ForEachSample/output/Sample_2.txt " ${LINENO}
executeLine "${modDir}/resources/addMetaHeader.sh ${pipeDir}/01_ForEachSample/output/Sample_3.txt " ${LINENO}
executeLine "${modDir}/resources/addMetaHeader.sh ${pipeDir}/01_ForEachSample/output/Sample_4.txt " ${LINENO}
executeLine "${modDir}/resources/addMetaHeader.sh ${pipeDir}/01_ForEachSample/output/Sample_5.txt " ${LINENO}
executeLine "${modDir}/resources/addMetaHeader.sh ${pipeDir}/01_ForEachSample/output/Sample_6.txt " ${LINENO}
executeLine "${modDir}/resources/addMetaHeader.sh ${pipeDir}/01_ForEachSample/output/Sample_7.txt " ${LINENO}
executeLine "${modDir}/resources/addMetaHeader.sh ${pipeDir}/01_ForEachSample/output/Sample_8.txt " ${LINENO}
executeLine "${modDir}/resources/addMetaHeader.sh ${pipeDir}/01_ForEachSample/output/Sample_9.txt " ${LINENO}
touch "${scriptDir}/02.0_ForEachFile.sh_Success"
echo 'Created Success flag.'
