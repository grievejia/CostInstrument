#!/bin/sh

set -e

javac `find . -name *.java`
jar cf cost.jar `find . -name *.class`
rm `find . -name *.class`
