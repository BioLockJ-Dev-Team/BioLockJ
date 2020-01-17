# suggested build command:
# name=metaphlan2_classifier_dbfree
# cd ${BLJ}
# docker build --build-arg DOCKER_HUB_USER=biolockjdevteam -t biolockjdevteam/${name} . -f resources/docker/${name}.Dockerfile 

ARG DOCKER_HUB_USER=biolockj
ARG FROM_VERSION=v1.2.7
FROM ${DOCKER_HUB_USER}/blj_basic_py2:${FROM_VERSION}

#1.) Install dependencies
RUN pip install numpy biopython && pip install biom-format

#2.) Install bowtie 2.3.4.3
ENV BASE_URL="https://github.com/BenLangmead/bowtie2/releases/download/v"
ENV BOWTIE_VER=2.3.4.3
ENV BOWTIE=bowtie2-${BOWTIE_VER}-linux-x86_64
ENV BOWTIE_URL=${BASE_URL}${BOWTIE_VER}/${BOWTIE}.zip
RUN cd $BIN && wget -qO- $BOWTIE_URL | bsdtar -xf- && \
	chmod o+x -R $BIN/$BOWTIE && \
	rm -rf $BIN/$BOWTIE/*-debug && \
	mv $BIN/$BOWTIE/[bs]* . && \
	rm -rf $BIN/$BOWTIE

#3.) Install MetaPhlAn2
ENV mpa_dir=$BIN
ENV MP_URL="https://bitbucket.org/biobakery/metaphlan2/get/default.zip"
RUN cd $BIN && \
	wget -qO- $MP_URL | bsdtar -xf- && \
	mv biobakery*/* . && \
	rm -rf biobakery*  && \
	chmod o+x -R *.py && \
	ln -s metaphlan2.py metaphlan2
	
#4.) Cleanup
RUN	apt-get clean && \
	rm -rf /tmp/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*

#5.) Remove shares (except ca-certificates) to allow internet downloads
RUN	mv /usr/share/ca-certificates* ~ && \
	rm -rf /usr/share/* && \
	mv ~/ca-certificates* /usr/share