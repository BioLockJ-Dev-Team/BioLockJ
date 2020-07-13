# suggested build command:
# name=r_module
# cd ${BLJ}
# docker build -t biolockjdevteam/${name} . -f resources/docker/dockerfiles/${name}.Dockerfile 

FROM r-base
ARG DEBIAN_FRONTEND=noninteractive

#1.) set shell to bash
SHELL ["/bin/bash", "-c"]

#2.) Copy script that has the BioLockJ assumptions
COPY resources/docker/docker_build_scripts/imageForBioLockJ.sh /root/.

#3.) Build Standard Directories and varibles and assumed software
RUN . /root/imageForBioLockJ.sh ~/.bashrc

#4.1) Install R Packages that are usually good
RUN Rscript -e "install.packages('Kendall', dependencies=TRUE)" && \
	Rscript -e "install.packages('coin', dependencies=TRUE)" && \
	Rscript -e "install.packages('vegan', dependencies=TRUE)" && \
	Rscript -e "install.packages('properties', dependencies=TRUE)"
	
#4.2) Install R Packages packages that got warnings
RUN Rscript -e "install.packages('htmltools', dependencies=TRUE)"
	
#4.3) Install R Packages that have failed
RUN Rscript -e "install.packages('ggplot2', dependencies=TRUE)" && \
	Rscript -e "install.packages('ggpubr', dependencies=TRUE)" && \
	Rscript -e "install.packages('stringr', dependencies=TRUE)"

#5.) check that packages installed
RUN Rscript -e "library('Kendall'); library('coin'); library('vegan'); library('ggpubr'); library('properties'); library('htmltools'); library('stringr')"

#6.) Cleanup
RUN	apt-get clean && \
	find / -name *python* | xargs rm -rf && \
	rm -rf /tmp/* && \
	rm -rf /usr/share/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*
