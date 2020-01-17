# suggested build command:
# name=genome_assembly
# cd ${BLJ}
# docker build --build-arg DOCKER_HUB_USER=biolockjdevteam -t biolockjdevteam/${name} . -f resources/docker/${name}.Dockerfile 

ARG DOCKER_HUB_USER=biolockj
ARG FROM_VERSION=v1.2.7
FROM ${DOCKER_HUB_USER}/blj_basic_py2:${FROM_VERSION}
ARG DEBIAN_FRONTEND=noninteractive

#1.) Install Ubuntu Software + Python libs
RUN apt-get update && \
	apt-get install -y gcc mono-mcs zlib1g-dev libbz2-dev liblzma-dev libncurses5-dev && \
	python -m pip install --upgrade pip && alias pip="$BIN/pip" && \
	pip install numpy scipy matplotlib==1.5.3 setuptools Cython dendropy && \
	pip install pysam checkm-genome

#2.) Set container environment variables
ENV CHECKM_DB_DIR="$BIN/checkm_data"
ENV CHECKM_DB_URL="https://data.ace.uq.edu.au/public/CheckM_databases/checkm_data_2015_01_16.tar.gz"
ENV HMMER_URL="http://eddylab.org/software/hmmer/hmmer.tar.gz"
ENV MASH_URL="https://github.com/marbl/Mash/releases/download/v2.2/mash-Linux64-v2.2.tar"
ENV META_BAT_URL="https://bitbucket.org/berkeleylab/metabat/downloads/metabat-static-binary-linux-x64_v2.12.1.tar.gz"
ENV META_SPADE_URL="https://github.com/ablab/spades/releases/download/v3.13.0/SPAdes-3.13.0-Linux.tar.gz"
ENV PPLACER_URL="https://github.com/matsen/pplacer/releases/download/v1.1.alpha19/pplacer-linux-v1.1.alpha19.zip"
ENV PRODIGAL_URL="https://github.com/hyattpd/Prodigal/releases/download/v2.6.3/prodigal.linux"
ENV PATH="$BIN/SPAdes-3.13.0-Linux/bin:$PATH"

#3.) Download metaspades, metabat, mash, hmmer, pplacer, prodigal
RUN cd $BIN && \
	wget -qO- $MASH_URL | bsdtar -xf- && \
	wget -qO- $META_SPADE_URL | bsdtar -xzf- && \
	wget -qO- $META_BAT_URL | bsdtar -xzf- && \
	wget -qO- $HMMER_URL | bsdtar -xzf- && \
	wget -qO- $PPLACER_URL | bsdtar -xf- && \
	wget -q $PRODIGAL_URL 
	
#4.) Move executables into $BIN (/usr/local/bin)
RUN cd $BIN && chmod -R 777 metabat && \
	mv metabat mb_temp && \
	mv mb_temp/* . && \
	rm -rf mb_temp && \
	mv mash-Linux64-v2.2/* . && \
	rm -rf  mash-Linux64-v2.2 && \
	mv pplacer-Linux-v1.1.alpha19/* . && \
	rm -rf  pplacer-Linux-v1.1.alpha19 && \
	mv prodigal.linux prodigal

#5.) Install HMMER3
RUN cd $BIN/hmmer-3.2.1 && ./configure --prefix=/usr/local/ && make && make install

#6.) Download standard checkm DB & run checkm setRoot to configure DB
RUN cd $BIN && mkdir $CHECKM_DB_DIR && cd $CHECKM_DB_DIR && \
	wget -qO- $CHECKM_DB_URL | bsdtar -xzf- && \
	checkm data setRoot $CHECKM_DB_DIR

#7.) Cleanup
RUN	apt-get clean && \
	rm -rf /tmp/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*

#8.) Remove shares (except ca-certificates - to allow http downloads)
RUN	mv /usr/share/ca-certificates* ~ && \
	rm -rf /usr/share/* && \
	mv ~/ca-certificates* /usr/share

#9.) Add symbolic link to metaspades python script named "metaspades2"
RUN cd $BIN/SPAdes-3.13.0-Linux/bin && \
	ln -s metaspades.py metaspades2
	