#!/bin/bash

# Linux version

echo -n "Using core file pattern: "
echo "$PWD/core/core-dump-%e-%p-%t" | sudo tee /proc/sys/kernel/core_pattern
mkdir core
