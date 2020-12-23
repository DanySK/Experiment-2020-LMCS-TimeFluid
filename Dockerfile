FROM adoptopenjdk:15-jdk-hotspot
RUN apt-get -y update
RUN apt-get -y install git python3 python3-pip
RUN mkdir lmcs
COPY . lmcs
WORKDIR lmcs
RUN pip3 install -r requirements.txt
CMD ./gradlew runAllBatch && python3 process.py
