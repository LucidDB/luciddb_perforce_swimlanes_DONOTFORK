# $Id$
# Define variables needed by runtime scripts such as farragoServer.
# This script is meant to be sourced from other scripts, not
# executed directly.

MAIN_DIR=$(cd `dirname $0`/..; pwd)

if [ ! -e "$MAIN_DIR/bin/classpath.gen" ]; then
    echo "Error:  $MAIN_DIR/install/install.sh has not been run yet."
    exit -1
fi

# If you are trying to give additional memory usable by queries
# see this doc: http://pub.eigenbase.org/wiki/LucidDbBufferPoolSizing
# Upping Java Heap will unlikely help queries on "large" datasets
JAVA_MEM="-Xms256m -Xmx256m -XX:MaxPermSize=128m"
JAVA_ARGS_CLIENT="-cp `cat $MAIN_DIR/bin/classpath.gen` \
  -Dnet.sf.farrago.home=$MAIN_DIR \
  -Dorg.eigenbase.util.AWT_WORKAROUND=off \
  -Djava.util.logging.config.file=$MAIN_DIR/trace/Trace.properties"
JAVA_ARGS="$JAVA_MEM $JAVA_ARGS_CLIENT"

SQLLINE_JAVA_ARGS="sqlline.SqlLine"

JAVA_EXEC=${JAVA_HOME}/bin/java

if [ `uname` = "Darwin" ]; then
    export DYLD_LIBRARY_PATH=$MAIN_DIR/plugin:$MAIN_DIR/lib/fennel
    JAVA_ARGS="$JAVA_ARGS -d32"
else
    export LD_LIBRARY_PATH=$MAIN_DIR/plugin:$MAIN_DIR/lib/fennel
fi
