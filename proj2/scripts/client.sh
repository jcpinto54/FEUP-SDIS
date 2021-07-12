#!/usr/bin/bash

cd build || exit
java application.Client "$@"