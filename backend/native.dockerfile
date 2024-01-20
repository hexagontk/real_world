#
# BUILD
#
FROM docker.io/bellsoft/liberica-native-image-kit-container:jdk-17-nik-22-musl as build
USER root
WORKDIR /build

ADD . .
RUN microdnf -y install findutils
RUN ./gradlew -x test nativeCompile

#
# RUNTIME
#
FROM scratch
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
