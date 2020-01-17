# suggested build command:
# name=blj_bash
# cd ${BLJ}
# docker build --build-arg DOCKER_HUB_USER=biolockjdevteam -t biolockjdevteam/${name} . -f resources/docker/${name}.Dockerfile 

ARG DOCKER_HUB_USER=biolockj
ARG FROM_VERSION=v1.2.7
FROM ${DOCKER_HUB_USER}/blj_basic:${FROM_VERSION}

#1.) Install PEAR
ENV P_URL="https://github.com/msioda/BioLockJ/releases/download/pear-0.9.10/pear.zip"
RUN cd $BIN && wget -qO- $P_URL | bsdtar -xf- && chmod 777 $BIN/pear
	
#2.) Cleanup
RUN	apt-get clean && find / -name *python* | xargs rm -rf && \
	rm -rf /usr/share/* && rm -rf /var/cache/* && rm -rf /var/lib/apt/lists/* 
