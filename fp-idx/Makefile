
.PHONY: clean

all: smi2fps searchidx mkidx

target/nfp.jar: pom.xml $(shell find src/**/*)
	mvn clean install -DskipTests

smi2fps: src/main/resources/smi2fps-stub.sh target/nfp.jar
	cat src/main/resources/smi2fps-stub.sh target/nfp.jar > smi2fps && chmod +x smi2fps

mkidx: src/main/resources/mkidx-stub.sh target/nfp.jar
	cat src/main/resources/mkidx-stub.sh target/nfp.jar > mkidx && chmod +x mkidx

searchidx: src/main/resources/searchidx-stub.sh target/nfp.jar
	cat src/main/resources/searchidx-stub.sh target/nfp.jar > searchidx && chmod +x searchidx	

clean:
	rm -rf searchidx mkidx smi2fps


