FROM rust:1.74-slim

RUN apt update && \
    apt install -y \
      openjdk-17-jdk-headless \
      sdkmanager \
    && rm -rf /var/lib/apt/lists/*

RUN useradd -m uniffi-builder
USER uniffi-builder

ENV ANDROID_HOME=/home/uniffi-builder/android-sdk
RUN mkdir -p $ANDROID_HOME
RUN sdkmanager --install \
    "build-tools;34.0.0" \
    "platforms;android-34"
RUN yes | sdkmanager --licenses

COPY --chown=uniffi-builder:uniffi-builder . /home/uniffi-builder/uniffi-kotlin-multiplatform-bindings

WORKDIR /home/uniffi-builder/uniffi-kotlin-multiplatform-bindings
# Build the project to warm the cache
RUN ./gradlew dependencies
# Delete the project files so they don't mess with future builds
RUN rm -rf [!.]* *
