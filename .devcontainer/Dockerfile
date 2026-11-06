FROM mcr.microsoft.com/devcontainers/base:ubuntu

RUN apt-get update && apt-get install -y \
    bash \
    curl \
    wget \
    git \
    build-essential \
    git-delta \
    locales \
    ripgrep \
    jq \
 && localedef -i en_US -f UTF-8 UTF-8

# JDK straight from the image CI tests with, not via the java feature: that one installs
# through SDKMAN!, whose version listing broke ("Version 25 not found" with an empty list).
# Gradle comes from ./gradlew, so no separate Gradle install is needed.
COPY --from=eclipse-temurin:25-jdk /opt/java/openjdk /opt/java/openjdk
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

USER vscode
