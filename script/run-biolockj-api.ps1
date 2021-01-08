
$BLJ_JAR="$BLJ\dist\BioLockJ.jar"
if (Test-Path $BLJ_JAR) {
  java -cp $BLJ_JAR biolockj.api.BioLockJ_API $args 
}else{
  Write-Warning "Error: BioLockJ Jar file [ $BLJ_JAR ] not found."
}
