# ID Utils 1.2.1

## Summary
ID Utils is a small library to help perform Protex identifications in bulk fashion.
During the run a manifest file will be created that contains all paths that were changed.
That manifest file can be used to reverse the operation (see Usage).

## Build ##

[![Build Status](https://travis-ci.org/blackducksoftware/idutils.svg?branch=master)](https://travis-ci.org/blackducksoftware/idutils)
[![Coverage Status](https://coveralls.io/repos/github/blackducksoftware/idutils/badge.svg?branch=master)](https://coveralls.io/github/blackducksoftware/idutils?branch=master)

## Building

mvn install is all that is required

## Usage

### Basic run
./idutils.sh (location of configuration file) 

### Basic run with manifest file 
./idutils.sh (location of configuration file) (location of manifest file)

You must provide Protex connection configuration and the Protex project.
Note:  This is currently using the Protex 6.X SDK, which *should* work with all 7.X systems.

### Sample configuration file

For a sample file, please see: https://github.com/blackducksoftware/idutils/blob/master/sample_config.txt
