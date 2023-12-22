#
# BUILD
#
#FROM docker.io/gradle:8.5-jdk21-graal-jammy as build
#FROM container-registry.oracle.com/graalvm/native-image:21-muslib-ol9 as build
#USER root
#WORKDIR /build

#ADD . .
#RUN microdnf -y install findutils
#RUN ./gradlew installDist
#RUN ls /build

#
# RUNTIME
#
FROM docker.io/eclipse-temurin:21-jre-alpine
LABEL description="Realworld API"
ARG PROJECT=backend

# Project setup
ENV TZ UTC

EXPOSE 2010

# Machine setup
VOLUME /tmp
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Project install
USER 1000
ADD --chown=1000:1000 build/distributions/$PROJECT*.tar /opt

# Process execution
WORKDIR /opt/$PROJECT*
ENTRYPOINT /opt/$PROJECT*/bin/$PROJECT
