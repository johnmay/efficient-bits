#!/bin/sh

# Builds self contained unix executables
cat src/main/resources/smi2fps-stub.sh target/nfp.jar > smi2fps && chmod +x smi2fps
cat src/main/resources/fpsscan-stub.sh target/nfp.jar > fpsscan && chmod +x fpsscan
cat src/main/resources/mkidx-stub.sh target/nfp.jar > mkidx && chmod +x mkidx
cat src/main/resources/simmer-stub.sh target/nfp.jar > simmer && chmod +x simmer
cat src/main/resources/toper-stub.sh target/nfp.jar > toper && chmod +x toper
