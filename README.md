# ShotVibe clientlib

Contains application logic that is shared between the mobile apps.

The code is written in Java and is used as-is in the Android app.

For the iOS app, the Java code is translated into Objective-C using
[j2objc](https://code.google.com/p/j2objc/).


## Build Instructions

### Android

The project is configured to be added as an Android Studio module called
"ShotVibeLib".

### iOS

First you need a Java Runtime Environment emulation layer for Objective-C.

The recommended one is [minijobjc](https://github.com/shotvibe/minijobjc),
which is very minimal (and therefore adds little size to the binary). You are
going to need to add the Objective-C source and header files into your project,
so that Xcode can find them.

Next, configure your Xcode target. In "Build Phases" you should add a "Run
Script" build phase at the beginning (it must be before "Compile Sources").

The script should be:

    cd ShotVibeLib
    ./build_xcode.py

(Assuming that you have added this project to a directory called `ShotVibeLib`)

The `build_xcode.py` script translates the Java source files into Objective-C,
and then compiles a static library to the file `build/current_shotvibelib.a`.
It makes sure that the static library is always for the correct architectures
of the current Xcode build.

Now perform a build in Xcode in order to generate the above static library, so
that we can choose to link with it:

In the "Link Binary with Libraries" stage, add a new library, and find
`build/current_shotvibelib.a`.
