#! /bin/bash

# $1 - optional pattern to search among dockerfile names; build only matching dockerfiles.
# example usage:
# buildDockerImages.sh
# or 
# buildDockerImages.sh controller

VER=$(cat ${BLJ}/.version)
TAG=${VER//-*}
DIR=${BLJ}/resources/docker/dockerfiles

if [ ${#1} -gt 0 ]; then
	FILES=$(cat ${BLJ}/resources/docker/docker_build_scripts/buildOrder.txt | grep $1 )
else
	FILES=$(cat ${BLJ}/resources/docker/docker_build_scripts/buildOrder.txt)
fi

for f in $FILES; do
	name=${f//.Dockerfile}
	echo "============================"
	echo $name
	docker build -t biolockjdevteam/${name}:${TAG} ${BLJ} -f ${DIR}/${name}.Dockerfile && \
	docker tag biolockjdevteam/${name}:${TAG} biolockjdevteam/${name}:${VER} && \
	docker tag biolockjdevteam/${name}:${TAG} biolockjdevteam/${name}:latest && echo "SUCCESS: $name" 1>&2 || echo "FAILURE: $name" 1>&2
done


# If you encounter this error: no space left on device
# This cleanup is helpful:
# docker rmi $(docker images --filter "dangling=true" -q --no-trunc)

