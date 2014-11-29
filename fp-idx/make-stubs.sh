#!/bin/sh

# Builds self contained unix executables
cat src/main/resources/smi2fps-stub.sh target/fp-idx-0.1-standalone.jar > smi2fps && chmod +x smi2fps
cat src/main/resources/fpsscan-stub.sh target/fp-idx-0.1-standalone.jar > fpsscan && chmod +x fpsscan
cat src/main/resources/mkidx-stub.sh target/fp-idx-0.1-standalone.jar > mkidx && chmod +x mkidx
cat src/main/resources/simsearch-stub.sh target/fp-idx-0.1-standalone.jar > simsearch && chmod +x simsearch
