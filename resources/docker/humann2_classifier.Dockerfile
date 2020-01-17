# suggested build command:
# name=humnn2_classifier
# cd ${BLJ}
# docker build --build-arg DOCKER_HUB_USER=biolockjdevteam -t biolockjdevteam/${name} . -f resources/docker/${name}.Dockerfile 

ARG DOCKER_HUB_USER=biolockj
ARG FROM_VERSION=v1.2.7
FROM ${DOCKER_HUB_USER}/blj_basic_py2:${FROM_VERSION}
ARG DEBIAN_FRONTEND=noninteractive

#1.) Install HumanN2 + dependencies
RUN pip install numpy && pip install biopython && pip install biom-format && pip install humann2

#2.) Install bowtie 2.3.4.3
ENV BOWTIE_URL="https://github.com/BenLangmead/bowtie2/releases/download/v"
ENV BOWTIE_VER=2.3.4.3
ENV BOWTIE=bowtie2-${BOWTIE_VER}-linux-x86_64
RUN cd $BIN && \
	wget -qO- ${BOWTIE_URL}${BOWTIE_VER}/${BOWTIE}.zip | bsdtar -xf- && \
	chmod 777 -R $BIN/${BOWTIE} && \
	rm -rf $BIN/${BOWTIE}/*-debug && \
	mv $BIN/${BOWTIE}/[bs]* . && \
	rm -rf $BIN/${BOWTIE}

#3.) Install MetaPhlAn2
ENV mpa_dir=$BIN
ENV MP_URL="https://www.dropbox.com/s/ztqr8qgbo727zpn/metaphlan2.zip"
RUN cd /app && wget -qO- $MP_URL | bsdtar -xf- && \
	chmod -R 777 /app/metaphlan2 && mv /app/metaphlan2/* $BIN && \
	rm -rf /app/metaphlan2 && cd $BIN && ln -s metaphlan2.py metaphlan2

#4.) Cleanup
RUN	apt-get clean && \
	rm -rf /tmp/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*

#5.) Remove shares (except npm & ca-certificates)
RUN	mv /usr/share/ca-certificates* ~ && \
	rm -rf /usr/share/* && \
	mv ~/ca-certificates* /usr/share
	