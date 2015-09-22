#!/bin/bash

OS_NAME="linux-i586"

SCRIPT_DIR=$(cd "$(dirname "$0")"; pwd)
LIB_DIR=""$SCRIPT_DIR"/lib/$OS_NAME"
JAR_PATH=""$SCRIPT_DIR"/jar/Fits3D.jar"

java -jar  -Djava.library.path="$LIB_DIR" "$JAR_PATH"