Example code and command line util for rendering structure diagrams with the CDK.

```bash
# In the project root set the following alias
$: alias render='mvn exec:java -Dexec.mainClass=Main'

# Using OPSIN to load porphyrin and generate a PDF
$: render -Dexec.args="-name porphyrin -pdf"

# Highlight the one of the pyrrole SMARTS in porphyrin
$: render -Dexec.args="-name porphyrin -pdf -sma n1cccc1"

# Show atom numbers
$: render -Dexec.args="-name porphyrin -pdf -atom-numbers"

# Show CIP labels
$: render -Dexec.args="-name '(2R)-butan-2-ol' -pdf -cip-labels"

# Generate a PDF / SVG for ethanol SMILES
$: render -Dexec.args="-smi CCO -pdf ethanol.pdf -svg ethanol.svg"

# Load a molfile
$: render -Dexec.args="-mol ChEBI_6701.mol -pdf chebi-6701.pdf"
```