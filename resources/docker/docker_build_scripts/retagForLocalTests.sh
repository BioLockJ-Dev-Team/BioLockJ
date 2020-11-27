#! /bin/bash

## Retag existing docker images with new version tag for the purpose of testing
# $1 - name of existing tag
# $2 - name of tag to apply
# example: retagForLocalTests.sh v1.2.8 v1.2.9

FILES=$(cat ${BLJ}/resources/docker/docker_build_scripts/buildOrder.txt)
for f in $FILES; do
	name=${f//.Dockerfile}
	echo "============================"
	echo $name
	docker run --rm biolockjdevteam/${name}:$1 echo hello
	docker tag biolockjdevteam/${name}:$1 biolockjdevteam/${name}:$2
done
