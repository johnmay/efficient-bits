#!/bin/sh

# location of index file
index="../chembl_19.idx"
queries="matt-mongodb-queries.smi"
resfile="benchmark-res.tsv"

rm -rf ${resfile}
echo "time    threshold       hits    checked total   smiles" >> ${resfile}
../simmer ${index} 0.98 ${queries} >> ${resfile}
../simmer ${index} 0.96 ${queries} >> ${resfile}
../simmer ${index} 0.94 ${queries} >> ${resfile}
../simmer ${index} 0.92 ${queries} >> ${resfile}
../simmer ${index} 0.90 ${queries} >> ${resfile}
../simmer ${index} 0.88 ${queries} >> ${resfile}
../simmer ${index} 0.86 ${queries} >> ${resfile}
../simmer ${index} 0.84 ${queries} >> ${resfile}
../simmer ${index} 0.82 ${queries} >> ${resfile}
../simmer ${index} 0.80 ${queries} >> ${resfile}
../simmer ${index} 0.78 ${queries} >> ${resfile}
../simmer ${index} 0.76 ${queries} >> ${resfile}
../simmer ${index} 0.74 ${queries} >> ${resfile}
../simmer ${index} 0.72 ${queries} >> ${resfile}
../simmer ${index} 0.70 ${queries} >> ${resfile}
../simmer ${index} 0.68 ${queries} >> ${resfile}
../simmer ${index} 0.66 ${queries} >> ${resfile}
../simmer ${index} 0.64 ${queries} >> ${resfile}
../simmer ${index} 0.62 ${queries} >> ${resfile}
../simmer ${index} 0.60 ${queries} >> ${resfile}
../simmer ${index} 0.58 ${queries} >> ${resfile}
../simmer ${index} 0.56 ${queries} >> ${resfile}
../simmer ${index} 0.54 ${queries} >> ${resfile}
../simmer ${index} 0.52 ${queries} >> ${resfile}
../simmer ${index} 0.50 ${queries} >> ${resfile}
gzip ${resfile}
