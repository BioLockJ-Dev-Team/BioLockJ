#! /bin/bash

# standard err/out are automatically sent to ../log/<name>.log
echo "Hello log file, I'm starting."

# scripts are run with the "current workign directory" set to a module subfolder
echo "Working dir: $PWD"

# outputs should be saved in the ../output directory
echo "Hello Output File!" > ../output/hello.txt
echo $(date) > ../output/time.txt

# always end with code that will have an exit status of 0, or the module will fail.
echo "Goodbye log file, I'm done."
