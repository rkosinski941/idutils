## ID Utils 1.2.1

## Summary
ID Utils is a small library to help perform Protex identifications in bulk fashion.
During the run a manifest file will be created that contains all paths that were changed.
That manifest file can be used to reverse the operation (see Usage).

## Building

mvn install is all that is required

## Usage

Basic run
./idutils.sh <location of configuration file> 

Basin run with manifest file 
./idutils.sh <location of configuration file> <location of manifest file>

The behavior is governed by the configuration file that can be found below.  
You must provide Protex connection configuration and the Protex project.
Below is a sample property file, and the explanation behind the keys.

Note:  This is currently using the Protex 6.X SDK, which *should* work with all 7.X systems.

Sample configuration file

For a sample file, please see: https://github.com/blackducksoftware/idutils/blob/master/sample_config.txt
