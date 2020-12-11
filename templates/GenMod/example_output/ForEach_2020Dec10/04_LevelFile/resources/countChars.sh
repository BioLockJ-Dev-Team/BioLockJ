#! /bin/bash

# $1 - a file

MOD=$(dirname $PWD)
PIPE=$(dirname $MOD)
OUT="$MOD/output"
BASE=$(basename $1)
FNAME="nChars-$BASE" 
OUTFILE="$OUT/$FNAME"

echo "Current working directory: $PWD"

echo "Script args: $@"

wc -c $1 > $OUTFILE
