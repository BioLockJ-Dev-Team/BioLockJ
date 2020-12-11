#! /bin/bash

# $1 - a sample id

MOD=$(dirname $PWD)
PIPE=$(dirname $MOD)
OUT="$MOD/output"
OUTFILE="$OUT/$1.txt"
META="../../*ImportMetadata/output/*.txt"

echo "Current working directory: $PWD"

echo "Script args: $@"

while read line; do
  #echo "$line"
  ID=$(echo $line | awk '{print $1;}')
  echo "SAMPLE ID: $ID"
  [ $ID == $1 ] && echo "Thats it!" && echo $line > $OUTFILE 
done < $META

echo "Done with $1."
#grep $1 $META > $OUTFILE
