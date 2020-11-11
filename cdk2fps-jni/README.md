# Prerequisites

- Java Development Kit (HotSpot or OpenJDK 1.8 recommended)
- [Apache Maven](https://maven.apache.org/) build tool
- Set the ``JAVA_HOME`` path

```
make
```

# Running

Before running the ``cdk2fps`` binary the shared library path needs to be set,
on Mac OS X this is ``DYLD_LIBRARY_PATH`` on Linux ``LD_LIBRARY_PATH``. The
``env.sh`` is provided to set this up for you.

```
source env.sh
```

You should then be able to process a SMILES file as follows.

```
./cdk2fps --ecfp4 /data/chembl_27.smi chembl_27.fps
```