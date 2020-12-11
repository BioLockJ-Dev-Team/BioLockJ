#!/bin/bash

source ../resources/functions.sh
source ../resources/functions2.sh

NAME=$(outName)
OUTFILE=../output/$NAME

captainsLog > $OUTFILE

echo "To boldly go where no man has gone before!" >> $OUTFILE
