FROM rust:1.74-slim

RUN apt-get update \
    && apt-get install -y openjdk-17-jdk-headless \
    && rm -rf /var/lib/apt/lists/*

RUN useradd -m uniffi-builder
USER uniffi-builder

COPY --chown=uniffi-builder:uniffi-builder . /home/uniffi-builder/uniffi-kotlin-multiplatform-bindings

WORKDIR /home/uniffi-builder/uniffi-kotlin-multiplatform-bindings
# Build the project to warm the cache
RUN ./gradlew build
# Delete the project files so they don't mess with future builds
RUN rm -rf [!.]* *
