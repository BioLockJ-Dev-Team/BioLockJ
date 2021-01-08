
$BLJ_JAR="$BLJ\dist\BioLockJ.jar"
if (Test-Path $BLJ_JAR) {
  java -jar $BLJ_JAR -f --blj_proj $BLJ_PROJ $args 
}else{
  Write-Warning "Error: BioLockJ Jar file [ $BLJ_JAR ] not found."
}


## Setup instructions:
#
### cd into the BioLockJ folder and type:
#
# Set-Variable -Name BLJ -Value $PWD
# Set-Alias -Name biolockj -Value $BLJ\script\run-biolockj.ps1
# Add-Content $profile "Set-Variable -Name BLJ -Value $BLJ"
# Add-Content $profile "Set-Alias -Name biolockj -Value $biolockj"
#
### cd into folder of your choice, such as C:Users\Documents\biolockj_pipelines
#
# Set-Variable -Name BLJ_PROJ -Value $PWD
# Add-Content $profile "Set-Variable -Name BLJ_PROJ -Value $BLJ_PROJ"
#

