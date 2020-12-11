#! /bin/bash

# $1 - a sample id

MOD=$(dirname $PWD)
PIPE=$(dirname $MOD)

echo "Current working directory: $PWD"

echo "Script args: $@"

cp ../../*/log/*_*.log ../output/.

echo "Done grabbing logs."