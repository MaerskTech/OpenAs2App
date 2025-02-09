#!/bin/bash
set -e
# purpose: runs the OpenAS2 application     
x=$(basename "$0")

binDir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

EXTRA_PARMS="$@"
PWD_OVERRIDE=""

if [ -n "$PID_FILE" ] && [ -z "$OPENAS2_PID" ]; then
  export OPENAS2_PID="$PID_FILE"
fi

if [ -z "$OPENAS2_PID" ]; then
  export OPENAS2_PID="$binDir/OpenAS2.pid"
fi

EXTRA_PARMS="$EXTRA_PARMS -Xms32m -Xmx384m"

if [ -z "$OPENAS2_CONFIG_FILE" ]; then
  OPENAS2_CONFIG_FILE="${binDir}/../config/config.xml"
fi
EXTRA_PARMS="$EXTRA_PARMS -Dopenas2.config.file=${OPENAS2_CONFIG_FILE}"

if [ -z "$OPENAS2_CONFIG_DIR" ]; then
  OPENAS2_CONFIG_DIR=$(dirname "$OPENAS2_CONFIG_FILE")
fi

if [ -z "$OPENAS2_LOG_DIR" ]; then
  OPENAS2_LOG_DIR="${binDir}/../logs"
fi
EXTRA_PARMS="$EXTRA_PARMS -DOPENAS2_LOG_DIR=${OPENAS2_LOG_DIR}"

if [ ! -z "$OPENAS2_PROPERTIES_FILE" ] && [ -f "$OPENAS2_PROPERTIES_FILE" ]; then
  EXTRA_PARMS="$EXTRA_PARMS -Dopenas2.properties.file=${OPENAS2_PROPERTIES_FILE}"
fi

if [ -z "$JAVA_HOME" ]; then
  OS=$(uname -s)
  if [[ "${OS}" == *Darwin* ]]; then
    JAVA_HOME=$(/usr/libexec/java_home)
  elif [[ "${OS}" == *Linux* ]]; then
    JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
  elif [[ "${OS}" == *MINGW* ]]; then
    echo "Windows not supported by this script"
  fi
fi

if [ -z "$JAVA_HOME" ]; then
  echo "ERROR: Cannot find JAVA_HOME"
  exit 1
fi

# Determine the target classes directory.
# From Server/src/bin, go up two levels (to Server) then into target/classes.
TARGET_CLASSES=$(cygpath -w "$(cd "$binDir/../../target/classes" && pwd)")
# Convert the config directory to Windows format.
CONFIG_DIR=$(cygpath -w "$OPENAS2_CONFIG_DIR")
# Build the full classpath. Use semicolon as the separator (for Windows).
FULL_CLASSPATH="${TARGET_CLASSES};${CONFIG_DIR}"

echo "TARGET_CLASSES: $TARGET_CLASSES"
echo "CONFIG_DIR: $CONFIG_DIR"
echo "FULL_CLASSPATH: $FULL_CLASSPATH"
echo ""

JAVA_HOME_WIN=$(cygpath -w "$JAVA_HOME")

CMD=(
  "${JAVA_HOME_WIN}/bin/java"
  -Xms32m
  -Xmx384m
  -Dopenas2.config.file="${OPENAS2_CONFIG_FILE}"
  -DOPENAS2_LOG_DIR="${OPENAS2_LOG_DIR}"
  -cp "$FULL_CLASSPATH"
  org.openas2.app.OpenAS2Server
)

echo
echo "Running command:"
printf '%q ' "${CMD[@]}"
echo
echo ""

# Execute the command
"${CMD[@]}"
