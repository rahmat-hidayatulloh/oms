#!/bin/sh

# Maven Wrapper
# https://github.com/takari/maven-wrapper

# Determine the project base directory
BASEDIR=$(dirname "$0")
cd "$BASEDIR"
BASEDIR=$(pwd)

# Find the Maven Wrapper JAR file
MAVEN_WRAPPER_JAR="$BASEDIR/.mvn/wrapper/maven-wrapper.jar"

# Download the Maven Wrapper JAR if it doesn't exist
if [ ! -f "$MAVEN_WRAPPER_JAR" ]; then
    echo "Downloading Maven Wrapper JAR..."
    curl -sS -o "$MAVEN_WRAPPER_JAR" "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
    if [ $? -ne 0 ]; then
        echo "Failed to download Maven Wrapper JAR"
        exit 1
    fi
fi

# Set JAR file and properties file
MAVEN_WRAPPER_PROPERTIES="$BASEDIR/.mvn/wrapper/maven-wrapper.properties"
MAVEN_WRAPPER_JAR="$BASEDIR/.mvn/wrapper/maven-wrapper.jar"

# Execute Maven Wrapper
exec java \
    $MAVEN_OPTS \
    -classpath "$MAVEN_WRAPPER_JAR" \
    -Dbasedir="$BASEDIR" \
    -Dmaven.home="$BASEDIR/.mvn/wrapper" \
    -Dmaven.multiModuleProjectDirectory="$BASEDIR" \
    org.apache.maven.wrapper.MavenWrapperMain \
    "$@"