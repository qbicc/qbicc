#!/bin/bash

# Mac OS version

PATTERN="$PWD/core/core-dump-%P"

echo "Using core file pattern: $PATTERN"
sudo sysctl "kern.corefile=$PATTERN"
mkdir core
