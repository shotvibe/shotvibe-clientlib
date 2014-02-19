#!/bin/sh
# Convert j2objc error messages on stderr to Xcode-friendly messages

# output from j2objc
# error: src/main/java/com/shotvibe/shotvibelib/AlbumBase.java:15: The blank final field mId may not have been initialized

# what xcode needs
# shotvibelib/src/main/java/com/shotvibe/shotvibelib/AlbumBase.java:5: error: name cannot be resolved to a variable

# usage: xCodeWrapperScript <j2obc command> <path prefix for source files>

j2objcCmd=$1
$j2objcCmd 2>&1 | awk -v pathPrefix=$2 '
/^error:/ {print pathPrefix $2 " error: " substr($0, index($0,$3)) }
/^warning:/ {print "shotvibelib/" $2 " warning: " substr($0, index($0,$3)) }
!/^error:|warning:/ {print}'
exitCode=${PIPESTATUS[0]}
echo NOTE: order of build_xcode.py output gets messed up by redirection
exit $exitCode