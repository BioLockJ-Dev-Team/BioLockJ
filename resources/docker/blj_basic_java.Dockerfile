# suggested build command:
# name=blj_basic_java
# cd ${BLJ}
# docker build --build-arg DOCKER_HUB_USER=biolockjdevteam -t biolockjdevteam/${name} . -f resources/docker/${name}.Dockerfile 

ARG DOCKER_HUB_USER=biolockj
ARG FROM_VERSION=v1.2.7
FROM ${DOCKER_HUB_USER}/blj_basic:${FROM_VERSION}
ARG DEBIAN_FRONTEND=noninteractive

#1.) Install Java 
RUN apt-get update && apt-get install -y software-properties-common && \
	apt-get upgrade -y && apt-get install -y openjdk-8-jre-headless

#2.) Cleanup
RUN	apt-get clean && \
	find / -name *python* | xargs rm -rf && \
	rm -rf /tmp/* && \
	rm -rf /var/log/* 
	