#syntax=docker/dockerfile:1.3

FROM gradle:7.4.2-jdk17 as builder
LABEL stage=builder-graph-based_policy-machine
WORKDIR /app
RUN echo 
COPY /graph-based-policy-machine/ /app/

RUN apt update && apt -y install  curl
RUN gradle build -x test

FROM eclipse-temurin:17-jre-alpine
LABEL description="Fine Grained Access Control"
RUN mkdir /app
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar /app/app.jar
RUN apk add curl
RUN curl -sfL $(curl -s https://api.github.com/repos/powerman/dockerize/releases/latest | grep -i /dockerize-$(uname -s)-$(uname -m)\" | cut -d\" -f4) | install /dev/stdin /usr/local/bin/dockerize
