# suggested build command:
# name=kraken_classifier_dbfree
# cd ${BLJ}
# docker build --build-arg DOCKER_HUB_USER=biolockjdevteam -t biolockjdevteam/${name} . -f resources/docker/${name}.Dockerfile 

ARG DOCKER_HUB_USER=biolockj
ARG FROM_VERSION=v1.2.7
FROM ${DOCKER_HUB_USER}/blj_basic:${FROM_VERSION}

#1.) Install Kraken
ENV KRAKEN_VER=0.10.5-beta
ENV BASE_URL="https://github.com/DerrickWood/kraken/archive/v"
ENV KRAKEN_URL=${BASE_URL}${KRAKEN_VER}.tar.gz
ENV KRAKEN=kraken-${KRAKEN_VER}
RUN cd $BIN && wget -qO- $KRAKEN_URL | bsdtar -xf- && \
	chmod o+x -R $KRAKEN && cd $KRAKEN && ./install_kraken.sh $BIN && \
	chmod o+x -R $BIN && rm -rf $KRAKEN

#2.) Cleanup
RUN	apt-get clean && \
	find / -name *python* | xargs rm -rf && \
	rm -rf /tmp/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*

#3.) Remove shares (except ca-certificates) to allow internet downloads
RUN	mv /usr/share/ca-certificates* ~ && \
	rm -rf /usr/share/* && \
	mv ~/ca-certificates* /usr/share
