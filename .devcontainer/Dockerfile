FROM mcr.microsoft.com/devcontainers/base:ubuntu-24.04

ARG JAVA_VERSION=17

# Install Java 17
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        openjdk-${JAVA_VERSION}-jdk \
        git-lfs \
        unzip \
        wget \
        ca-certificates \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk

ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/build-tools/35.0.1

# Android command-line tools
ARG CMDLINE_TOOLS_URL=https://dl.google.com/android/repository/commandlinetools-linux-15859902_latest.zip

RUN mkdir -p ${ANDROID_HOME}/cmdline-tools \
    && wget -q ${CMDLINE_TOOLS_URL} -O /tmp/cmdline-tools.zip \
    && mkdir -p ${ANDROID_HOME}/cmdline-tools/latest \
    && unzip -q /tmp/cmdline-tools.zip -d /tmp/android-cmdline-tools \
    && mv /tmp/android-cmdline-tools/cmdline-tools/* ${ANDROID_HOME}/cmdline-tools/latest/ \
    && rm -rf /tmp/cmdline-tools.zip /tmp/android-cmdline-tools \
    && yes | sdkmanager --licenses >/dev/null \
    && sdkmanager \
        "platform-tools" \
        "platforms;android-37.0" \
        "build-tools;35.0.1" \
    && chmod -R a+rX ${ANDROID_HOME}

WORKDIR /workspaces/wallet-sms-importer