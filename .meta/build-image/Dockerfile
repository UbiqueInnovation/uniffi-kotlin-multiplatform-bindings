FROM rust:1.74-slim

RUN apt update && \
    apt install -y \
      openjdk-17-jdk-headless \
      build-essential \
      binutils \
      # For Android
      sdkmanager \
      # For MinGW
      gcc-mingw-w64 \
      mingw-w64-x86-64-dev \
      # For Linux example apps
      libgtk-4-dev \
      # For Zig \
      wget \
      # For packages that builds OpenSSL from the source like blake3 used in examples
      perl \
    && rm -rf /var/lib/apt/lists/*

# Use Zig as cross-compilation linker for Arm64 Linux
ENV ZIG_VERSION=0.12.1
ENV ZIG_BUILD=zig-linux-x86_64-$ZIG_VERSION
RUN mkdir -p /zig
WORKDIR /zig
RUN wget -c https://ziglang.org/download/$ZIG_VERSION/$ZIG_BUILD.tar.xz && \
    tar -xf $ZIG_BUILD.tar.xz && \
    rm $ZIG_BUILD.tar.xz && \
    printf "#! /bin/sh\n/zig/$ZIG_BUILD/zig cc -target aarch64-linux-gnu \"\$@\"" > aarch64-unknown-linux-gnu-cc.sh && \
    chmod 777 aarch64-unknown-linux-gnu-cc.sh && \
    mkdir -p /.cargo && \
    printf "[target.aarch64-unknown-linux-gnu]\nlinker = \"/zig/aarch64-unknown-linux-gnu-cc.sh\"" > /.cargo/config.toml

RUN useradd -m uniffi-builder
USER uniffi-builder

ENV ANDROID_HOME=/home/uniffi-builder/android-sdk
RUN mkdir -p $ANDROID_HOME
RUN sdkmanager --install \
    "cmake;3.22.1" \
    "build-tools;34.0.0" \
    "platforms;android-34" \
    "ndk;26.1.10909125"
RUN yes | sdkmanager --licenses

COPY --chown=uniffi-builder:uniffi-builder . /home/uniffi-builder/uniffi-kotlin-multiplatform-bindings

WORKDIR /home/uniffi-builder/uniffi-kotlin-multiplatform-bindings
# Download dependencies to warm the cache
RUN ./gradlew dependencies
# Delete the project files so they don't mess with future builds
RUN rm -rf [!.]* *
