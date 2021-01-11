# suggested build command:
# name=build_and_deploy
# cd ${BLJ}
# docker build -t biolockjdevteam/${name} . -f resources/docker/dockerfiles/${name}.Dockerfile 

# Use this image to build and deploy BioLockJ:
# docker run --rm -v $BLJ:/biolockj biolockjdevteam/build_and_deploy

FROM biolockjdevteam/build_with_ant:1.10.9

# Python & pip & mkdocs
RUN apt-get update && \
	apt-get install -y python3 python3-pip mkdocs && \
	rm /usr/bin/python && \ 
	rm /usr/bin/py*2* && \ 
	ln -s /usr/bin/python3 /usr/bin/python
	
# mkdocs extension
RUN pip3 install python-markdown-math
	
# set up build process
RUN mkdir -p /biolockj/resources
WORKDIR /biolockj/resources
CMD ant deploy