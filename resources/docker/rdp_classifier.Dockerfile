# suggested build command:
# name=rdp_classifier
# cd ${BLJ}
# docker build --build-arg DOCKER_HUB_USER=biolockjdevteam -t biolockjdevteam/${name} -f resources/docker/${name}.Dockerfile ${BLJ}

ARG DOCKER_HUB_USER=biolockj
ARG FROM_VERSION=v1.2.7
FROM ${DOCKER_HUB_USER}/blj_basic_java:${FROM_VERSION}

#1.) Install RDP
ENV RDP="rdp_classifier_2.12"
ENV RDP_URL="https://sourceforge.net/projects/rdp-classifier/files/rdp-classifier"
RUN cd /app  && \
	wget -qO- $RDP_URL/$RDP.zip | bsdtar -xf- && \
	mv /app/$RDP/dist/classifier.jar /app  && \
	mv /app/$RDP/dist/lib /app  && \
	rm -rf /app/$RDP

#2.) Cleanup
RUN	apt-get clean && \
	rm -rf /tmp/* && \
	rm -rf /usr/share/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*
