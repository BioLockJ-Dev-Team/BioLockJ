# suggested build command:
# name=build_with_ant
# cd ${BLJ}
# docker build -t biolockjdevteam/${name}:1.10.9 . -f resources/docker/dockerfiles/${name}.Dockerfile 

FROM openjdk:8

ENV ANT_DIST=/apache-ant-1.10.9
RUN wget https://www.apache.org/dist/ant/binaries/$ANT_DIST-bin.tar.bz2
RUN tar xfj $ANT_DIST-bin.tar.bz2
ENV PATH=$ANT_DIST/bin:$PATH
