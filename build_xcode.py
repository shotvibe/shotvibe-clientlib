#!/usr/bin/env python

import os
import shlex
import shutil
import subprocess
import sys

sys.path.append("third_party")
import j2objcbuild

CURRENT_FAT_LIB = "build/current_shotvibelib.a"

JAVA_INPUT_DIR = "src/main/java"
OBJC_FILES_DIR = "build/SL"
CODEGEN_TIMESTAMP_FILE = "build/.codegen"
LIB_PREFIX = "build/shotvibelib"

CLANG_FLAGS = [
    "-x", "objective-c",
    "-std=gnu99",
    "-fobjc-arc",
    "-Os",
    "-fstrict-aliasing"]


def main():
    translate_java_sources()
    copy_objc_override_sources()
    (archs, arch_libs) = build_arch_libs()
    fat_lib = create_fat_lib(archs, arch_libs)

    print("Updating symlink " + CURRENT_FAT_LIB)

    if os.path.lexists(CURRENT_FAT_LIB):
        os.remove(CURRENT_FAT_LIB)

    os.symlink(os.path.relpath(fat_lib, os.path.dirname(CURRENT_FAT_LIB)), CURRENT_FAT_LIB)


def translate_java_sources():
    print("Starting Java Translation")
    num, returnval = j2objcbuild.build(
        j2objc_exe="./third_party/j2objc/j2objc",
        j2objc_opts=[
            "-use-arc",
            "--strip-reflection",
            "--prefix", "com.shotvibe.shotvibelib=SL",
            "-sourcepath", "src/main/java"],
        input_dir=JAVA_INPUT_DIR,
        output_dir=OBJC_FILES_DIR,
        no_package_directories=True)

    # The j2objc command failed. Exit immediately, the errors will be visible in stderr
    if returnval != 0:
        sys.exit(returnval)

    if num > 0:
        # There were changes made during the build
        # Update the timestamp of the tracking file
        touch(CODEGEN_TIMESTAMP_FILE)
    else:
        print("No changes")


def copy_objc_override_sources():
    print("Copying Objective-C Override Sources")

    srcs_dir = "src/main/java"

    any_updated = False

    for root, dirs, files in os.walk(srcs_dir):
        for file in files:
            if file.endswith(".h") or file.endswith(".m"):
                fullname = os.path.join(root, file)
                target_name = os.path.join(OBJC_FILES_DIR, file)

                if j2objcbuild.is_file_newer(fullname, target_name):
                    print("Copying " + file)
                    shutil.copyfile(fullname, target_name)
                    any_updated = True

    if any_updated:
        # Update the timestamp of the tracking file
        touch(CODEGEN_TIMESTAMP_FILE)


def build_arch_libs():
    archs_str = get_xcode_environ("ARCHS")
    valid_archs_str = get_xcode_environ("VALID_ARCHS")

    archs = set(archs_str.split())
    valid_archs = set(valid_archs_str.split())

    # We build only the intersection of ARCHS and VALID_ARCHS
    #
    # See:
    # http://stackoverflow.com/questions/12701188/whats-the-difference-between-architectures-and-valid-architectures-in-xcode

    arch_targets = archs.intersection(valid_archs)

    target_libs = []

    for arch in arch_targets:
        arch_lib_fname = LIB_PREFIX + "_" + arch + ".a"

        if j2objcbuild.is_file_newer(CODEGEN_TIMESTAMP_FILE, arch_lib_fname):
            compile_arch_lib(arch, arch_lib_fname)

        target_libs.append(arch_lib_fname)

    return (arch_targets, target_libs)


def compile_arch_lib(arch, output_file):
    print("Starting build for architecture \"" + arch + "\"")

    BUILD_TMP_DIR = "build/tmp"

    if not os.path.isdir(BUILD_TMP_DIR):
        os.mkdir(BUILD_TMP_DIR)

    def java_name_to_objc_name(java_name):
        basename = os.path.basename(java_name)
        no_ext = os.path.splitext(basename)[0]
        return os.path.join(OBJC_FILES_DIR, no_ext + ".m")

    def objc_name_to_o_name(objc_name):
        basename = os.path.basename(m)
        no_ext = os.path.splitext(basename)[0]
        return os.path.join(BUILD_TMP_DIR, no_ext + ".o")

    java_files = j2objcbuild.find_java_files(JAVA_INPUT_DIR)
    objc_source_files = [java_name_to_objc_name(f) for f in java_files]

    # See:
    # http://stackoverflow.com/questions/12228382/after-install-xcode-where-is-clang
    CLANG = subprocess.check_output(["xcodebuild", "-find", "clang"]).rstrip()
    LIBTOOL = subprocess.check_output(["xcodebuild", "-find", "libtool"]).rstrip()

    sysroot = get_xcode_environ("SDKROOT")

    header_search_paths_str = get_xcode_environ("HEADER_SEARCH_PATHS")
    header_search_paths = shlex.split(header_search_paths_str)

    # First compile all the individual .m files

    for m in objc_source_files:
        command = [CLANG, "-c", m]

        command += CLANG_FLAGS

        command += ["-arch", arch]

        command += ["-isysroot", sysroot]

        objc_abi_version = os.environ.get("OBJC_ABI_VERSION")
        if objc_abi_version:
            command += ["-fobjc-abi-version=" + objc_abi_version]

        if os.environ.get("GCC_OBJC_LEGACY_DISPATCH") == "YES":
            command += ["-fobjc-legacy-dispatch"]

        if os.environ.get("PLATFORM_NAME") == "iphonesimulator":
            command += ["-fexceptions"]

        for h in header_search_paths:
            command += ["-I", h]

        command += ["-o", objc_name_to_o_name(m)]

        print("Compiling " + m)
        returnval = subprocess.call(command)
        if returnval != 0:
            sys.exit(returnval)

    # Now link all the resulting .o files

    command = [LIBTOOL]
    command += ["-static"]
    command += ["-o", output_file]

    for m in objc_source_files:
        command.append(objc_name_to_o_name(m))

    print("Linking " + output_file)
    returnval = subprocess.call(command)
    if returnval != 0:
        sys.exit(returnval)


def create_fat_lib(archs, arch_libs):
    dest_file = LIB_PREFIX

    sorted_archs = list(archs)
    sorted_archs.sort

    for a in sorted_archs:
        dest_file += "__" + a

    dest_file += ".a"

    if not any(j2objcbuild.is_file_newer(l, dest_file) for l in arch_libs):
        return dest_file

    print("Create fatlib: " + dest_file)

    LIPO = subprocess.check_output(["xcodebuild", "-find", "lipo"]).rstrip()

    command = [LIPO, "-create"]
    command += ["-o", dest_file]
    command += arch_libs
    returnval = subprocess.call(command)
    if returnval != 0:
        sys.exit(returnval)

    return dest_file


def get_xcode_environ(name):
    val = os.environ.get(name)
    if not val:
        sys.stderr.write("Environment variable " + name + " not set.\n")
        sys.stderr.write("This script must be run from within an Xcode build\n")
        sys.exit(1)
    return val


# http://stackoverflow.com/questions/1158076/implement-touch-using-python
def touch(fname, times=None):
    """
    Similar to UNIX touch program
    """
    with file(fname, 'a'):
        os.utime(fname, times)


if __name__ == "__main__":
    main()
