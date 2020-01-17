# suggested build command:
# name=qiime_classifier
# cd ${BLJ}
# docker build --build-arg DOCKER_HUB_USER=biolockjdevteam -t biolockjdevteam/${name} . -f resources/docker/${name}.Dockerfile 

ARG DOCKER_HUB_USER=biolockj
ARG FROM_VERSION=v1.2.7
FROM ${DOCKER_HUB_USER}/blj_basic_py2:${FROM_VERSION}
ARG DEBIAN_FRONTEND=noninteractive

#1.) Install numpy/QIIME + QIIME Default DB
ENV QIIME_VERSION=1.9.1
RUN pip install numpy && pip install --upgrade qiime==$QIIME_VERSION

#2.) Install vSearch
ENV BASE_URL="https://github.com/torognes/vsearch/releases/download/v"
ENV VSEARCH_VER="2.8.1"
ENV VSEARCH="vsearch-${VSEARCH_VER}-linux-x86_64"
ENV V_URL="${BASE_URL}${VSEARCH_VER}/${VSEARCH}.tar.gz"
RUN cd $BIN && wget -qO- ${V_URL} | bsdtar -xzf- && \
	mv vsearch*/bin/* . && rm -rf ${VSEARCH}

#3.) Cleanup
RUN	apt-get clean && \
	rm -rf /tmp/* && \
	rm -rf /usr/share/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*
