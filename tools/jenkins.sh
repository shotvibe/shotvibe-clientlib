#!/bin/sh -e

# Compile the Java source into class files
echo "Compiling Java source into class files"
javac -classpath third_party/jars/android.jar `find src -name '*.java'`

# Run Checkstyle
echo "Running Checkstyle"
mkdir -p reports
rm -f reports/checkstyle-result.xml

find src -name '*.java' -print0 | xargs -0 checkstyle -c checkstyle.xml -f xml -o reports/checkstyle-result.xml || true

# Test the Obj-C Build
./tools/test_objc_build.sh
