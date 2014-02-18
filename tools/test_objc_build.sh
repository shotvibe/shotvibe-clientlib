#!/bin/bash -e

# Download or update "minijobjc" repository

if [ ! -d "minijobjc" ]
then
  git clone https://github.com/shotvibe/minijobjc.git
else
  cd minijobjc; git pull; cd ..
fi

# Build "minijobjc"

cd minijobjc
export J2OBJC=../third_party/j2objc/j2objc
make minijobjc_dist
cd ..

# Finally build the local project

export ARCHS=x86_64
export VALID_ARCHS=x86_64
export HEADER_SEARCH_PATHS=minijobjc/minijobjc_dist
./build_xcode.py
