# suggested build command:
# name=biolockj_controller
# cd ${BLJ}
# docker build -t biolockjdevteam/${name} . -f resources/docker/dockerfiles/${name}.Dockerfile 

ARG BUILDER_IMG=biolockjdevteam/build_with_ant:1.10.9
ARG DOCKER_HUB_USER=biolockjdevteam
FROM ${BUILDER_IMG} AS builder

COPY . /blj
RUN ls /blj
RUN $ANT_DIST/bin/ant -buildfile blj/resources/build.xml build-jar

ARG DOCKER_HUB_USER=biolockjdevteam
FROM ${DOCKER_HUB_USER}/blj_basic_py2:v1.2.9
ARG DEBIAN_FRONTEND=noninteractive

#1.) Install Ubuntu Software
ENV NODE_VERSION 8.11.3
RUN apt-get update && \
	apt-get install -y ca-certificates software-properties-common nodejs aptitude npm && \
	apt-get upgrade -y && \
   	apt-get install -y openjdk-8-jre-headless && \
    wget "https://deb.nodesource.com/setup_8.x" | bash -

#2.) Install Nextflow Client
#NF_URL="https://get.nextflow.io"
ENV BIN=/usr/bin
RUN NF_URL="https://github.com/nextflow-io/nextflow/releases/download/v19.04.0/nextflow" && \
	cd $BIN && wget -qO- $NF_URL | bash

#3.) Install Docker Client
ARG DOCKER_CLIENT=docker-18.09.2
RUN DOCK_URL="https://download.docker.com/linux/static/stable/x86_64/${DOCKER_CLIENT}.tgz" && \
	cd $BIN && \
	wget -qO- $DOCK_URL  | bsdtar -xzf- && \
	mv docker tempDocker && mv tempDocker/* . && rm -rf tempDocker

#4.) Install BioLockJ
ENV BLJ="/app/biolockj" 
ENV BLJ_MODS="/app/external_modules"
RUN mkdir -p "${BLJ}" && \
	mkdir -p "${BLJ}/pipelines" && \
	mkdir -p "${BLJ_MODS}"
COPY --from=builder /blj/dist/BioLockJ.jar $BLJ/dist/BioLockJ.jar
COPY --from=builder /blj/resources $BLJ/resources
COPY --from=builder /blj/pipelines/README $BLJ/pipelines/README
COPY --from=builder /blj/script $BLJ/script
COPY --from=builder /blj/templates $BLJ/templates
COPY --from=builder /blj/.version /blj/install $BLJ/

RUN $BLJ/install

ENV PATH="${BLJ}/script:$PATH"

#5.) Cleanup
RUN	apt-get clean && \
	rm -rf /tmp/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*

#6.) Remove shares (except npm & ca-certificates)
RUN	mv /usr/share/ca-certificates* ~ && mv /usr/share/npm ~ && \
	rm -rf /usr/share/* && \
	mv ~/npm /usr/share && mv ~/ca-certificates* /usr/share
		
#7.) Setup environment and assign default command
CMD java -cp $BLJ/dist/BioLockJ.jar:$BLJ_MODS/* biolockj.BioLockJ $BLJ_OPTIONS
