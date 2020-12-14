FROM adoptopenjdk:15-jdk-hotspot
RUN apt-get -y update
RUN apt-get -y install git
RUN mkdir lmcs
COPY . lmcs
WORKDIR lmcs
CMD ./gradlew runAllBatch
