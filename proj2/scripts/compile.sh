#!/usr/bin/bash

find . -name "*.java" > build/sources.txt
javac @build/sources.txt -d ./build