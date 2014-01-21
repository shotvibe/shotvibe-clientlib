#!/usr/bin/env python

import optparse
import os
import subprocess
import sys


def find_java_files(indir):
    indir = os.path.normpath(indir)

    java_files = []
    for root, dirs, files in os.walk(indir):
        for file in files:
            if file.endswith(".java"):
                fullname = os.path.join(root, file)
                rel_name = fullname[len(indir) + 1:]
                java_files.append(rel_name)
    return java_files


def is_file_newer(source_file, target_file):
    source_modification = os.path.getmtime(source_file)

    try:
        target_modification = os.path.getmtime(target_file)
    except OSError:
        # If target doesn't exist then by definition the source is newer
        return True

    return source_modification >= target_modification


def build(j2objc_exe, j2objc_opts, input_dir, output_dir, no_package_directories=False):
    """
    Returns a 2 sized tuple:

    * First element is the number of changed source files.
    * Second element is the return value from the j2objc command. 0 indicates
      success.

    If no source files have changed, will not perform any build and will always
    return (0, 0)
    """
    java_files = find_java_files(input_dir)

    newer_source_files = []

    for f in java_files:
        source_file = os.path.join(input_dir, f)

        no_ext = os.path.splitext(f)[0]
        target_file_h = os.path.join(output_dir, no_ext) + ".h"
        target_file_m = os.path.join(output_dir, no_ext) + ".m"

        if no_package_directories:
            target_file_h = os.path.join(output_dir, os.path.basename(target_file_h))
            target_file_m = os.path.join(output_dir, os.path.basename(target_file_h))

        source_file_no_ext = os.path.splitext(source_file)[0]
        objc_override_exists = os.path.isfile(source_file_no_ext + ".h") \
                and os.path.isfile(source_file_no_ext + ".m")

        if objc_override_exists:
            print("Skipping due to Objective-C Override: " + source_file)
        elif is_file_newer(source_file, target_file_h) \
                or is_file_newer(source_file, target_file_m):
            newer_source_files.append(source_file)

    if not newer_source_files:
        return (0, 0)

    command = [j2objc_exe]
    command += j2objc_opts

    if no_package_directories:
        command += ["--no-package-directories"]

    command += ["-sourcepath", input_dir]

    command += ["-d", output_dir]
    command += newer_source_files

    result = subprocess.call(command)

    return (len(newer_source_files), result)


def main():
    usage = "usage: %prog [OPTIONS] [-j OPT [-j OPT [..]]] [--output-dir=DIR] INPUT_DIR"
    parser = optparse.OptionParser(usage=usage)

    parser.add_option("--output-dir",
                      action="store",
                      type="string",
                      dest="output_dir",
                      metavar="DIR",
                      default=".",
                      help=("Where to place generated files"))
    parser.add_option("--no-package-directories",
                      action="store_true",
                      dest="no_package_directories",
                      default=False,
                      help=("Generate output files to specified directory,"
                            "without creating package sub-directories"))
    parser.add_option("-j",
                      action="append",
                      type="string",
                      dest="j2objc_opts",
                      metavar="OPT",
                      default=[],
                      help=("Pass OPT to j2objc"))

    options, args = parser.parse_args()

    if len(args) != 1:
        parser.error("You must supply a single input directory")

    input_dir = args[0]

    if not os.path.isdir(input_dir):
        parser.error(options.input_dir + " is not a directory")

    num, return_val = build(j2objc_exe=os.environ.get("J2OBJC", "j2objc"),
                            j2objc_opts=options.j2objc_opts,
                            input_dir=input_dir,
                            output_dir=options.output_dir,
                            no_package_directories=options.no_package_directories)

    if num == 0:
        print("No changes")

    return return_val


if __name__ == "__main__":
    main()
