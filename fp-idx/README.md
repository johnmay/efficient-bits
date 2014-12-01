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
 is provided the output is to standard out. The ~1.4 million entries in ChEMBL 19 should take a few minutes (YMMV).

`./smi2fps /data/chembl_19.smi chembl_19.fps`

`mkidx` converts the FPS file generated in the previous step into an index. The index will allow search performance
less similarity calculations. If the second argument is omitted the index name will be based on the input FPS file
 (e.g. chembl_19.fps.idx). An Id mapping file is also generated (e.g. chembl_19.fps.idx.id) which maps the hit ids 
back to the original identifiers.

`./mkidx chembl_19.fps chembl_19.idx`

`fpsscan` runs through an FPS file and *greps* out the entries that are similar to a provided query structure. The
optional third argument specifies the threshold at which entries should be selected (default: 0.8).

`./fpsscan chembl_19.fps 'COc1cccc(c1)C1(O)CCCCC1CN(C)C' 0.7`

`simmer` use the index to search for queries. The number of hits, time, and number of entries
 checked is reported.

SMILES queries can be provided on the command line

`./simmer chembl_19.idx 0.8 'COc1cccc(c1)C1(O)CCCCC1CN(C)C' 'c1ccccc1'`

or in a file

`./simmer /data/chembl_19.idx 0.8 queries.smi`

`toper` is like `simmer` but allows the retrieval of the <b><i>k</i></b> most similar hits. For 
example to retrieve the top `50` hits use. The output format is the same as `toper`. 

`./toper /data/chembl_19.idx 50 queries.smi`

## Setting Java Options 

`$ java_args="-Xms2G -Xmx2G"` - to set the start/max heap size
`$ java_args="-Dchunks=true"` - load the index in chunks (i.e. not all at once) 
`$ java_args="-XX-UsePopCountInstruction"` - tell java not to use PopCount instruction (for investigation)

## Running benchmark

The `benchmark/` directory contains the 1000 SMILES that have also been used in benchmark MongoDB
by [Matt Swain](http://blog.matt-swain.com/post/87093745652/chemical-similarity-search-in-mongodb). 
 The script generates a result file that summarises each search.

```
cp chembl_19.idx benchmark/
cd benchmark/
sh run-benchmark.sh
```

## Running from the jar

The above commands can also be run from the jar without the stubs

```
java -cp target/nfp.jar org.openscience.cdk.nfp.SmiToFps /data/chembl_19.smi chembl_19.fps
java -cp target/nfp.jar org.openscience.cdk.nfp.FpsToIdx chembl_19.fps chembl_19.idx
java -cp target/nfp.jar org.openscience.cdk.nfp.FpsScan chembl_19.fps 'COc1cccc(c1)C1(O)CCCCC1CN(C)C' 0.7
java -cp target/nfp.jar org.openscience.cdk.nfp.SimSearch chembl_19.idx 0.8 'COc1cccc(c1)C1(O)CCCCC1CN(C)C' 'c1ccccc1'
java -cp target/nfp.jar org.openscience.cdk.nfp.TopSearch chembl_19.idx 10 'COc1cccc(c1)C1(O)CCCCC1CN(C)C' 'c1ccccc1'
```