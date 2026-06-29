#!/bin/sh
# Maven Wrapper script for Unix/macOS
MAVEN_PROJECTBASEDIR=$(dirname "$0")
WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

if [ -r "$WRAPPER_JAR" ]; then
    :
else
    echo "Downloading Maven Wrapper..."
    DOWNLOAD_URL=$(grep wrapperUrl "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties" | cut -d= -f2)
    curl -o "$WRAPPER_JAR" "$DOWNLOAD_URL" 2>/dev/null || wget -O "$WRAPPER_JAR" "$DOWNLOAD_URL"
fi

exec "$JAVA_HOME/bin/java" \
    -classpath "$WRAPPER_JAR" \
    "-Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR" \
    $MAVEN_OPTS \
    $WRAPPER_LAUNCHER "$@"
