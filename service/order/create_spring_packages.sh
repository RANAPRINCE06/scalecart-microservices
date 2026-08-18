#!/bin/bash

# base package path, change this to your project base
BASE_PACKAGE="com/nahid/order"

# under src/main/java
BASE_DIR="src/main/java/$(echo $BASE_PACKAGE | tr '.' '/')"

# create base directory if not exists
mkdir -p $BASE_DIR

# list of sub-packages to create
PACKAGES=("entity" "dto" "enums" "controller" "service" "exception" "repository" "mapper")

for p in "${PACKAGES[@]}"
do
    mkdir -p "$BASE_DIR/$p"
    echo "Created $BASE_DIR/$p"
done

# also create resources directory if needed
mkdir -p src/main/resources

echo "✅ Package structure created under $BASE_DIR"
