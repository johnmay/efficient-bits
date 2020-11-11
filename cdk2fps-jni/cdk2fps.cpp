/*
 * =====================================
 *  Copyright (c) 2020 John Mayfield
 * =====================================
 */
#include <stdio.h>
#include <string.h>

#include <iostream>
#include <fstream>
#include <string>

#include "cdkfp.h"

static const char   *inpname;
static const char   *outname;
static unsigned int opt_fpflav;
static unsigned int opt_fplen;


static void WriteHex(unsigned char x, FILE *fp) {
  static const char *hex = "0123456789abcdef";
  fputc(hex[x>>4], fp);
  fputc(hex[x&0xf], fp);
}

  
static void WriteFps(std::string &fpdata, const char *smi, FILE *fp) {
  unsigned int len = fpdata.length();
  const unsigned char *ptr = (const unsigned char*)fpdata.data();
  for (unsigned int i = 0; i < len; i++)
    WriteHex(ptr[i], fp);
  const char *ttl = smi;
  while (ttl[0]) {
    if (ttl[0] == '\t' || ttl[0] == ' ')
      break;
    ttl++;
  }
  if (ttl[0])
    fputs(ttl, fp);
  else {
    fputc(' ', fp);
    fputs(smi, fp);
  }
  fputc('\n', fp);
}


static const char *GetFpType() {
  switch (opt_fpflav) {
    case FpFlavor::ECFP0: return "cdk/ecfp/radius=0";
    case FpFlavor::ECFP2: return "cdk/ecfp/radius=2";
    case FpFlavor::ECFP4: return "cdk/ecfp/radius=4";
    case FpFlavor::ECFP6: return "cdk/ecfp/radius=6";
    case FpFlavor::FCFP0: return "cdk/fcfp/radius=0";
    case FpFlavor::FCFP2: return "cdk/fcfp/radius=2";
    case FpFlavor::FCFP4: return "cdk/fcfp/radius=4";
    case FpFlavor::FCFP6: return "cdk/fcfp/radius=6";
    case FpFlavor::MACCS: return "cdk/maccs";
    case FpFlavor::PATH6: return "cdk/path/depth=6";
    case FpFlavor::PATH7: return "cdk/path/depth=7";
    default: return "???";
  }
}


static void WriteFpsHeader(FILE *fp) {
  char buffer[1024];
  fputs("#FPS1\n", fp);
  sprintf(buffer, "#num_bits=%d\n", opt_fplen);
  fputs(buffer, fp);
  sprintf(buffer, "#type=%s\n", GetFpType());
  fputs(buffer, fp);
  sprintf(buffer, "#software=cdk2fps JNI demo\n");
  fputs(buffer, fp);
  // data=ISO
}


static bool ReadLine(std::string &buffer, FILE *fp)
{
  buffer.clear();
  int ch;
  for (;;) {
    ch = getc_unlocked(fp);
    if (ch == '\n')
      return true;
    if (ch == '\r') {
      ch = getc_unlocked(fp);
      if (ch != '\n') {
        if (ch == -1)
          return false;
        ungetc(ch,fp);
      }
      return true;
    }
    if (ch == -1)
      return false;
    buffer += ch;
  }
}


static void ProcessFile(FILE *ifp, FILE *ofp) {

  CDK::VM vm;
  CDK::Fp fp(vm);

  std::string line;
  std::string fpdata;
  while (ReadLine(line, ifp)) {
    if (fp.Encode(fpdata,line.c_str(),opt_fpflav,opt_fplen))
      WriteFps(fpdata, line.c_str(), ofp);
    else
      fprintf(stderr, "Error: %s\n", line.c_str());    
  }
}


static void DisplayUsage() {
  fputs("Usage: ./cdk2fps [--ecfp4 --path6 --fcpf4 --fplen <num>] {input.smi} [{output.smi}]\n\n",
        stderr);
  exit(1);
}


static void ProcessCommandLine(int argc, const char *argv[])
{
  opt_fpflav = FpFlavor::ECFP4;
  opt_fplen  = 1024;

  int i, j = 0;
  for (i=1; i<argc; i++) {
    const char *ptr = argv[i];
    if (ptr[0] == '-' && ptr[1]) {
      if (!strcmp(ptr, "--ecfp4"))
        opt_fpflav = FpFlavor::ECFP4;
      else if (!strcmp(ptr, "--fcfp4"))
        opt_fpflav = FpFlavor::FCFP4;
      else if (!strcmp(ptr, "--path6"))
        opt_fpflav = FpFlavor::PATH6;
      else if (!strcmp(ptr, "--path7"))
        opt_fpflav = FpFlavor::PATH7;
      else if (!strcmp(ptr, "--maccs") || 
               !strcmp(ptr, "--mdl-maccs") || 
               !strcmp(ptr, "--maccs166")) {
        opt_fpflav = FpFlavor::MACCS;
        opt_fplen  = 166;
      }
      else if (!strcmp(ptr, "--fplen")) {
        if (++i == argc) {
          fputs("Error: --fplen should be followed by a length\n", stderr);
          DisplayUsage();
        }
        opt_fplen = atoi(argv[i]);
      } else {
        fprintf(stderr, "Error: Unexpected argument %s\n", ptr);
        DisplayUsage();
      }
    } else {
      switch (j++) {
        case 0: 
          inpname = ptr; break;
        case 1: 
          outname = ptr; break;
        default:
          fprintf(stderr, "Error: Unexpected argument %s\n", ptr);
          DisplayUsage();
          break;
      }
    }
  }
  if (j == 0) {
    fputs("Error: No input specified\n", stderr);
    DisplayUsage();
  }  
}


int main(int argc, const char *argv[]) {

  ProcessCommandLine(argc, argv);

  FILE *ifp;
  if (inpname && strcmp(inpname, "-")) {
    ifp = fopen(inpname, "rb");
    if (!ifp) {
      fprintf(stderr, "Error: Could not open %s\n", inpname);
    }
  } else {
    ifp = stdin;
  }

  FILE *ofp;
  if (outname && strcmp(outname, "-")) {
    ofp = fopen(outname, "wb");
    if (!ofp) {
      fprintf(stderr, "Error: Could not open %s\n", outname);
    }
  } else {
    ofp = stdout;
  }

  WriteFpsHeader(ofp);
  ProcessFile(ifp, ofp);

  fclose(ifp);
  fclose(ofp);
  
  return 0;
}
