# suggested build command:
# name=r_deseq2
# cd ${BLJ}
# docker build -t biolockjdevteam/${name} . -f resources/docker/dockerfiles/${name}.Dockerfile 

FROM bioconductor/bioconductor_docker:latest
ARG DEBIAN_FRONTEND=noninteractive

#1.) Install R Packages
# Rscript -e 'if (!requireNamespace("BiocManager", quietly = TRUE)) install.packages("BiocManager")' && \
RUN Rscript -e 'BiocManager::install("DESeq2")'

#2.) Install R Packages
RUN REPO="http://cran.us.r-project.org" && \
	Rscript -e "install.packages('ggpubr', dependencies=TRUE, repos='$REPO')" && \
	Rscript -e "install.packages('properties', dependencies=TRUE, repos='$REPO')" && \
	Rscript -e "install.packages('stringr', dependencies=TRUE, repos='$REPO')"
	
#3.) Cleanup
RUN	apt-get clean && \
	find / -name *python* | xargs rm -rf && \
	rm -rf /tmp/* && \
	rm -rf /usr/share/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*
