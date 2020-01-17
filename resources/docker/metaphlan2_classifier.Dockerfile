# suggested build command:
# name=metaphlan2_classifier
# cd ${BLJ}
# docker build --build-arg DOCKER_HUB_USER=biolockjdevteam -t biolockjdevteam/${name} . -f resources/docker/${name}.Dockerfile 

ARG DOCKER_HUB_USER=biolockj
ARG FROM_VERSION=v1.2.7
FROM ${DOCKER_HUB_USER}/metaphlan2_classifier_dbfree:${FROM_VERSION}

#1.) Remove DB-less MetaPhlAn2
RUN	cd $BIN && rm -rf strain* && rm -rf [_u]* && rm -rf metaphlan2.py 
	
#2.) Download MetaPhlAn2 with DB
ENV MP_URL="https://www.dropbox.com/s/ztqr8qgbo727zpn/metaphlan2.zip"
RUN cd /app && wget -qO- $MP_URL | bsdtar -xf- && chmod -R 777 * && \
	mv /app/metaphlan2/* $BIN && rm -rf /app/*

#3.) Cleanup
RUN	rm -rf /usr/share/*
