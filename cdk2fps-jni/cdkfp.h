/*
 * =====================================
 *  Copyright (c) 2020 John Mayfield
 * =====================================
 */
#ifndef CDKFP_H
#define CDKFP_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <sys/time.h>
#include <jni.h>

static JNIEnv* create_vm(JavaVM **jvm) {    
  JNIEnv *env;
  JavaVMInitArgs vm_args;

    JavaVMOption options; 
    options.optionString = (char*)"-Djava.class.path=cdk-fputil.jar";
    vm_args.version = JNI_VERSION_1_8;
    vm_args.nOptions = 1;
    vm_args.options = &options;
    vm_args.ignoreUnrecognized = 0;    
    int ret = JNI_CreateJavaVM(jvm, (void**)&env, &vm_args);
    if(ret != JNI_OK)
      fputs("Error: Unable to Launch JVM!", stderr);       
    return env;
}

namespace CDK
{
	struct VM {
		JavaVM *jvm;
		JNIEnv *env;

		jclass cCdkFp;
		
		jmethodID mEncode;

		VM() {
			env = create_vm(&jvm);
			cCdkFp = env->FindClass("org/openscience/cdk/fputil/CdkFp");
			if (!cCdkFp) {
				fputs("Error: Could not find org.openscience.cdk.fputil.CdkFp!\n", stderr);
				exit(1);
			}
			
			mEncode = env->GetStaticMethodID(cCdkFp, "encode", "([BLjava/lang/String;II)Z");
			if (!mEncode) {
			fputs("Error: Could not find org.openscience.cdk.chemfp.CdkFp:encode!\n", stderr);
				exit(1);				
			}
		}

		~VM() {
			jvm->DestroyJavaVM();
		}
	};

	struct Fp {
		VM &vm;
		
		Fp(VM &_vm): vm(_vm) {
			
		}
		
		~Fp() {
			
		}

		bool Encode(std::string &fp, const char* str, unsigned int flav, unsigned int len) 
		{
			JNIEnv     *env = vm.env;
			jstring    jstr = env->NewStringUTF(str);
			jbyteArray jarr = env->NewByteArray((len+7)/8);
			bool res = env->CallStaticBooleanMethod(vm.cCdkFp, vm.mEncode, jarr, jstr, flav, len);
			jboolean iscopy;
			jbyte *b = env->GetByteArrayElements(jarr, &iscopy);
			fp.resize((len+7)/8);
			memcpy((unsigned char*)fp.data(), b, (len+7)/8);
			if (iscopy)
				env->ReleaseByteArrayElements(jarr, b, 0);
			env->DeleteLocalRef(jstr);
			env->DeleteLocalRef(jarr);
			return res;
		}
	};
}

namespace FpFlavor {

	const unsigned int ECFP0    = 0x10000;
	const unsigned int ECFP2    = 0x10001;
	const unsigned int ECFP4    = 0x10002;
	const unsigned int ECFP6    = 0x10003;
	const unsigned int FCFP0    = 0x20000;
	const unsigned int FCFP2    = 0x20001;
	const unsigned int FCFP4    = 0x20002;
	const unsigned int FCFP6    = 0x20003;
	const unsigned int PATH5    = 0x30005;
	const unsigned int PATH6    = 0x30006;
	const unsigned int PATH7    = 0x30007;
	const unsigned int EXTPATH5 = 0x40005;
	const unsigned int EXTPATH6 = 0x40006;
	const unsigned int EXTPATH7 = 0x40007;
	const unsigned int PUBCHEM  = 0x50000;
	const unsigned int MACCS    = 0x60000;
	const unsigned int LINGOS   = 0x70000;

}

#endif