#!/usr/bin/env sh

##############################################################################
## Gradle start up script for UN*X
##############################################################################

APP_BASE_NAME=`basename "$0"`
APP_HOME=`dirname "$0"`

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS="-Xmx64m -Xms64m"

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

warn () {
    echo "$*"
}

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME\n\nPlease set the JAVA_HOME variable in your environment to match the\nlocation of your Java installation."
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.\n\nPlease set the JAVA_HOME variable in your environment to match the\nlocation of your Java installation."
fi

# Determine the location of the Gradle wrapper jar
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# Determine the Java command line options to pass to the JVM
GRADLE_OPTS="$GRADLE_OPTS $DEFAULT_JVM_OPTS"

# For Cygwin, ensure paths are in UNIX format before anything is touched.
# For macOS, set the JDK to use

exec "$JAVACMD" $GRADLE_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"