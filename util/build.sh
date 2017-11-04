#!/bin/sh

set -e

javac `find . -name *.java`
jar cf util.jar `find . -name *.class`
rm `find . -name *.class`
