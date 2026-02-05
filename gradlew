#!/bin/sh

# Minimal Gradle wrapper script placeholder.
# If this file is missing executable permissions, run: chmod +x gradlew

DIR="$(cd "$(dirname "$0")" && pwd)"
java -jar "$DIR/gradle/wrapper/gradle-wrapper.jar" "$@"
