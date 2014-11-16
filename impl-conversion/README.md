# Usage

The project is built using [SBT](http://www.scala-sbt.org/). The JNI shared libraries are
included from the RDKit lucene project for multiple operating systems and architectures. I have
only tested OS X x86_64.
 
To run the [`BlogExamples`](src/main/scala/BlogExamples.scala) simple type `sbt run` on the
command line.
                             
# License

The CDK is licensed under LGPL 2.1.
The RDKit / RDKit Lucene is licensed under BSD. 
The package "org.rdkit" contains code from https://github.com/rdkit/org.rdkit.lucene/ and maintains the original copyright headers.
