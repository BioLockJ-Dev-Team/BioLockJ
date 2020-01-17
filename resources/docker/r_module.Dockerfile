# suggested build command:
# name=r_module
# cd ${BLJ}
# docker build --build-arg DOCKER_HUB_USER=biolockjdevteam -t biolockjdevteam/${name} . -f resources/docker/${name}.Dockerfile 

ARG DOCKER_HUB_USER=biolockj
ARG FROM_VERSION=v1.2.7
FROM ${DOCKER_HUB_USER}/blj_basic:${FROM_VERSION}
ARG DEBIAN_FRONTEND=noninteractive

#1.) Install Ubuntu Software 
RUN apt-get install -y software-properties-common libcurl4-openssl-dev libssl-dev && \
	apt-key adv --keyserver keyserver.ubuntu.com --recv-keys E298A3A825C0D65DFD57CBB651716619E084DAB9 && \
	add-apt-repository 'deb https://cloud.r-project.org/bin/linux/ubuntu bionic-cran35/' && \
	apt update && \
	apt install -y r-base-dev

#2.) Install R Packages
ENV REPO="http://cran.us.r-project.org"
RUN Rscript -e "install.packages('Kendall', dependencies=TRUE, repos='$REPO')" && \
	Rscript -e "install.packages('coin', dependencies=TRUE, repos='$REPO')" && \
	Rscript -e "install.packages('vegan', dependencies=TRUE, repos='$REPO')" && \
	Rscript -e "install.packages('ggpubr', dependencies=TRUE, repos='$REPO')" && \
	Rscript -e "install.packages('properties', dependencies=TRUE, repos='$REPO')" && \
	Rscript -e "install.packages('htmltools', dependencies=TRUE, repos='$REPO')" && \
	Rscript -e "install.packages('stringr', dependencies=TRUE, repos='$REPO')"

#3.) Cleanup
RUN	apt-get clean && \
	find / -name *python* | xargs rm -rf && \
	rm -rf /tmp/* && \
	rm -rf /usr/share/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*
