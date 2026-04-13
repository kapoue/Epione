#!/bin/sh
# Gradle wrapper script (Unix)
# Généré automatiquement — ne pas éditer manuellement.

APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

DIRNAME=$(dirname "$0")
cd "$DIRNAME" || exit

JAVA_EXE="$JAVA_HOME/bin/java"
if [ ! -x "$JAVA_EXE" ]; then
  JAVA_EXE=$(command -v java) || { echo "ERROR: JAVA_HOME not set and java not found in PATH." >&2; exit 1; }
fi

exec "$JAVA_EXE" $DEFAULT_JVM_OPTS -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain "$@"
