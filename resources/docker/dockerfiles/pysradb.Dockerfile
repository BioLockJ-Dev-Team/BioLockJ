# suggested build command:
# name=pysradb
# cd ${BLJ}
# docker build -t biolockjdevteam/${name} . -f resources/docker/dockerfiles/${name}.Dockerfile 

FROM continuumio/miniconda

ENV DEBIAN_FRONTEND=noninteractive

RUN conda create -c bioconda -n pysradb PYTHON=3 pysradb

ENV PATH /opt/conda/envs/pysradb/bin:$PATH
