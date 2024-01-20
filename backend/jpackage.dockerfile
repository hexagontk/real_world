#
# BUILD
#
FROM container-registry.oracle.com/graalvm/native-image:21-muslib-ol9 as build
USER root
WORKDIR /build

ADD . .
RUN microdnf -y install findutils
RUN ./gradlew -x test jpackage

#
# RUNTIME
#
FROM docker.io/bellsoft/alpaquita-linux-base:stream-glibc
LABEL description="Realworld API"

# Project setup
ENV PROJECT backend
ENV TZ UTC

EXPOSE 2010

# Machine setup
VOLUME /tmp
#RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Project install
USER 1000
COPY --from=build --chown=1000:1000 /build/$PROJECT/build/$PROJECT/ /opt/$PROJECT

# Process execution
WORKDIR /opt/$PROJECT
ENTRYPOINT /opt/backend/bin/backend
