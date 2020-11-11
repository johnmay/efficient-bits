CXX=g++
CXX_FLAGS=-O3

MAVEN=mvn

JNI_INC_DIR=$(JAVA_HOME)/include
JNI_LIB_DIR=$(JAVA_HOME)/jre/lib
JNI_FLAGS=-I$(JNI_INC_DIR) -I$(JNI_INC_DIR)/darwin -L$(JNI_LIB_DIR)/jli -L$(JNI_LIB_DIR)/server -ljvm -ljli

.PHONY: cdkjar

all: cdkjar cdk2fps

cdkjar:
	$(MAVEN) install -q

cdk2fps: cdk2fps.cpp cdkfp.h
	$(CXX) $(CXX_FLAGS) $(JNI_FLAGS) cdk2fps.cpp -o cdk2fps

clean:
	$(MAVEN) clean -q && rm -rf cdk2fps
