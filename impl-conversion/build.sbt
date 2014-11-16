name := "implicit-conversions"

version := "0.1"

scalaVersion := "2.11.0"

libraryDependencies += "org.openscience.cdk" % "cdk-silent" % "1.5.8"

libraryDependencies += "org.openscience.cdk" % "cdk-smiles" % "1.5.8"

libraryDependencies += "org.openscience.cdk" % "cdk-inchi" % "1.5.8"

libraryDependencies += "org.openscience.cdk" % "cdk-standard" % "1.5.8"

libraryDependencies += "org.openscience.cdk" % "cdk-fingerprint" % "1.5.8"

libraryDependencies += "uk.ac.cam.ch.opsin" % "opsin-core" % "1.6.0"


resolvers += "ebi-releases"   at "http://www.ebi.ac.uk/intact/maven/nexus/content/repositories/ebi-repo/"

resolvers += "ebi-snapshots"  at "http://www.ebi.ac.uk/intact/maven/nexus/content/repositories/ebi-repo-snapshots/"

resolvers += "jni-inchi-repo" at "http://jni-inchi.sourceforge.net/m2repo"

resolvers += "jnati" at "http://jnati.sourceforge.net/m2repo"