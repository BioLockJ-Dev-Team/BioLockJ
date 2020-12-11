#!/usr/bin/python

import sys
import os

os.chdir("..")
#os.mkdir("output")

with open("output/PythonScriptResults.txt", "w") as file:
    x="This is the name of the script: "+ str(sys.argv[0])
    y="Number of arguments: " +str(len(sys.argv))
    z="The arguments are: "+ str(sys.argv)

    file.write(str(x)+"\n")
    file.write(str(y)+"\n")
    file.write(str(z)+"\n")
