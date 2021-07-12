#! /usr/bin/bash

# To be executed on the root of the compiled tree
# Requires one argument: the peer id

# Check number input arguments
argc=$#

if [ $argc -eq 1 ];
then
	peer_id=$1
else
	echo "Usage: $0 [<peer_id>]]"
	exit 1
fi

# Build the directory tree for storing files

if [ $peer_id -eq 1 ];
then
	cd ../../
	mkdir peerFiles
	cd peerFiles
	mkdir $peer_id
	cd $peer_id
  mkdir originals
fi

