Some left over code from a potential nfp (new fingerprint) [CDK](http://github.com/cdk/cdk) module.

## Build

The project uses Maven to build and will download all required dependencies and bundle the classes as Jar.

`mvn install`

On unix system (Linux/OS X) standalone executable stubs can be made (make sure `mvn install` is run first).

`sh make-stubs.sh`

## Usage

The module provides several command line utilities. If you are on windows you will not be able run the stubs
(unless you have a shell env like Cygwin) and should see the section [Running from the Jar](#Running-from-the-jar).

`smi2fps` runs through a SMILES file and outputs an ECFP2 (CDK CircularFingerprint) in FPS format. If no second argument
 is provided the output is to standard out. The ~1.4 million entries in ChEMBL 19 should take a few of minutes (YMMV).

`./smi2fps /data/chembl_19.smi chembl_19.fps`

`mkidx` converts the FPS file generated in the previous step into an index. The index will allow search performance
less similarity calculations. If the second argument is omitted the index name will be based on the input FPS file
 (e.g. chembl_19.fps.idx).

`./fps2idx chembl_19.fps chembl_19.idx`

`fpsscan` runs through an FPS file and *greps* out the entries that are similar to a provided query structure. The
optional third argument specifies the threshold at which entries should be selected (default: 0.8).

`./fpsscan /data/chembl_19.fps 'COc1cccc(c1)C1(O)CCCCC1CN(C)C' 0.7`

`simsearch` use the index to search for queries. **Todo - WIP**.

SMILES queries can be provided on the command line

`./simsearch /data/chembl_19.idx 'COc1cccc(c1)C1(O)CCCCC1CN(C)C' 'c1ccccc1'`

or in a file

`./simsearch /data/chembl_19.idx queries.smi`

## Running from the jar

The above commands can also be run from the jar without the stubs

```
java -cp target/nfp.jar SmiToFps /data/chembl_19.smi chembl_19.fps
java -cp target/nfp.jar FpsToIdx chembl_19.fps chembl_19.idx
java -cp target/nfp.jar FpsScan /data/chembl_19.fps 'COc1cccc(c1)C1(O)CCCCC1CN(C)C' 0.7
java -cp target/nfp.jar SimSearch 'COc1cccc(c1)C1(O)CCCCC1CN(C)C' 'c1ccccc1'
```