# suggested build command:
# name=knead_data_dbfree
# cd ${BLJ}
# docker build --build-arg DOCKER_HUB_USER=biolockjdevteam -t biolockjdevteam/${name} . -f resources/docker/${name}.Dockerfile 

ARG DOCKER_HUB_USER=biolockj
ARG FROM_VERSION=v1.2.7
FROM ${DOCKER_HUB_USER}/blj_basic_py2:${FROM_VERSION}
ARG DEBIAN_FRONTEND=noninteractive

#1.) Install Java
RUN apt-get install -y software-properties-common && \
	apt-get upgrade -y && \
   	apt-get install -y openjdk-8-jre-headless

#2.) Install kneaddata
RUN pip install kneaddata

#3.) Install Trimmomatic
ENV TRIMM_VER=0.38
ENV TRIM_APP=Trimmomatic-${TRIMM_VER}
RUN cd /app && \
wget -qO- "http://www.usadellab.org/cms/uploads/supplementary/Trimmomatic/Trimmomatic-${TRIMM_VER}.zip" | bsdtar -xf- 

#4.) Install bowtie 2.3.4.3
ENV BASE_URL="https://github.com/BenLangmead/bowtie2/releases/download/v"
ENV BOWTIE_VER=2.3.4.3
ENV BOWTIE=bowtie2-${BOWTIE_VER}-linux-x86_64
ENV BOWTIE_URL=${BASE_URL}${BOWTIE_VER}/${BOWTIE}.zip
RUN cd $BIN && \
	wget -qO- $BOWTIE_URL | bsdtar -xf- && \
	chmod o+x -R $BIN/$BOWTIE && \
	rm -rf $BIN/$BOWTIE/*-debug && \
	mv $BIN/$BOWTIE/[bs]* . && \
	rm -rf $BIN/$BOWTIE
	
#5.) Update $PATH
RUN echo 'export PATH=/app/$TRIM_APP:$PATH' >> ~/.bashrc

#6.) Cleanup
RUN	apt-get clean && \
	rm -rf /tmp/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*

#7.) Remove shares (except ca-certificates) to allow internet downloads
RUN	mv /usr/share/ca-certificates* ~ && \
	rm -rf /usr/share/* && \
	mv ~/ca-certificates* /usr/share
