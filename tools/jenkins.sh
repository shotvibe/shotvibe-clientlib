#!/bin/sh -e

# Run checkstyle

mkdir -p reports
rm -f reports/checkstyle-result.xml

find src -name '*.java' -print0 | xargs -0 checkstyle -c checkstyle.xml -f xml -o reports/checkstyle-result.xml || true

# Test the Obj-C Build
./tools/test_objc_build.sh
