# suggested build command:
# name=kraken_classifier
# cd ${BLJ}
# docker build --build-arg DOCKER_HUB_USER=biolockjdevteam -t biolockjdevteam/${name} . -f resources/docker/${name}.Dockerfile 

ARG DOCKER_HUB_USER=biolockj
ARG FROM_VERSION=v1.2.7
FROM ${DOCKER_HUB_USER}/kraken_classifier_dbfree:${FROM_VERSION}

#1.) Download 8GB Dustmasked miniKraken DB
RUN cd "${BLJ_DEFAULT_DB}" && \
	wget -qO- "https://ccb.jhu.edu/software/kraken/dl/minikraken_20171101_8GB_dustmasked.tgz" | bsdtar -xzf- && \
	mv minikraken*/* . && rm -rf minikraken* && chmod -R 777 "${BLJ_DEFAULT_DB}"

#2.) Cleanup
RUN	 rm -rf /usr/share/* 
