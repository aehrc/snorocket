#!/bin/sh

INSTALL_DIR=`dirname "$0"`

exec java -client -Xmx1024m -jar "$INSTALL_DIR/lib/snorocket-${project.version}.jar" "$@"

