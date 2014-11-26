package org.openscience.cdk.nfp;

import java.util.Arrays;

final class FpsFmt {

    static final char[] hex = "0123456789abcdef".toCharArray();

    static boolean readHex(String str, int len, long[] words) {
        assert str != null;
        assert words != null;

        int nWords = 0;
        int nBytes = words.length * 16;
        for (int i = 0; i < nBytes; ++i, nWords++) {
            words[nWords] = ((long) hexToByte(str.charAt(i), str.charAt(++i)));
            words[nWords] |= ((long) hexToByte(str.charAt(++i), str.charAt(++i))) << 8L;
            words[nWords] |= ((long) hexToByte(str.charAt(++i), str.charAt(++i))) << 16L;
            words[nWords] |= ((long) hexToByte(str.charAt(++i), str.charAt(++i))) << 24L;
            words[nWords] |= ((long) hexToByte(str.charAt(++i), str.charAt(++i))) << 32L;
            words[nWords] |= ((long) hexToByte(str.charAt(++i), str.charAt(++i))) << 40L;
            words[nWords] |= ((long) hexToByte(str.charAt(++i), str.charAt(++i))) << 48L;
            words[nWords] |= ((long) hexToByte(str.charAt(++i), str.charAt(++i))) << 56L;
        }

        return true;
    }

    static boolean writeHex(StringBuilder sb, int len, long[] words) {
        int nWords = 0;
        if (words.length * 64 < len)
            words = Arrays.copyOf(words, len / 64);
        for (int i = 0; i < len; i += 64) {
            long word = words[nWords++];
            sb.append(hex[((int) (word >>> 4)) & 0xf]);
            sb.append(hex[((int) word) & 0xf]);
            word >>>= 8L;
            sb.append(hex[((int) (word >>> 4)) & 0xf]);
            sb.append(hex[((int) word) & 0xf]);
            word >>>= 8L;
            sb.append(hex[((int) (word >>> 4)) & 0xf]);
            sb.append(hex[((int) word) & 0xf]);
            word >>>= 8L;
            sb.append(hex[((int) (word >>> 4)) & 0xf]);
            sb.append(hex[((int) word) & 0xf]);
            word >>>= 8L;
            sb.append(hex[((int) (word >>> 4)) & 0xf]);
            sb.append(hex[((int) word) & 0xf]);
            word >>>= 8L;
            sb.append(hex[((int) (word >>> 4)) & 0xf]);
            sb.append(hex[((int) word) & 0xf]);
            word >>>= 8L;
            sb.append(hex[((int) (word >>> 4)) & 0xf]);
            sb.append(hex[((int) word) & 0xf]);
            word >>>= 8L;
            sb.append(hex[((int) (word >>> 4)) & 0xf]);
            sb.append(hex[((int) word) & 0xf]);
        }

        return true;
    }

    static int hexToByte(char hi, char lo) {
        return (hexToNibble(hi) << 4) | hexToNibble(lo);
    }

    static int hexToNibble(char c) {
        switch (c) {
            case '0':
                return 0; // 0000
            case '1':
                return 1; // 0001
            case '2':
                return 2; // 0010
            case '3':
                return 3; // 0011
            case '4':
                return 4; // 0100
            case '5':
                return 5; // 0101
            case '6':
                return 6; // 0110
            case '7':
                return 7; // 0111
            case '8':
                return 8; // 1000
            case '9':
                return 9; // 1001
            case 'a':
                return 10; // 1010
            case 'b':
                return 11; // 1011
            case 'c':
                return 12; // 1100
            case 'd':
                return 13; // 1101
            case 'e':
                return 14; // 1110
            case 'f':
                return 15; // 1111
            case 'A':
                return 10;
            case 'B':
                return 11;
            case 'C':
                return 12;
            case 'D':
                return 13;
            case 'E':
                return 14;
            case 'F':
                return 15;
            default:
                throw new RuntimeException();
        }
    }


}