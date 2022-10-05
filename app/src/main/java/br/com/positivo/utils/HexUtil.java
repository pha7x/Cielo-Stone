//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//
// code based on the source of the Ricardo Rocha send for us!

package br.com.positivo.utils;

public class HexUtil {
    private static final char[] CHARS_TABLES = "0123456789ABCDEF".toCharArray();
    static final byte[] BYTES = new byte[128];
    public HexUtil() {
    }

    public static String toHexString(byte[] aBytes) {
        return aBytes == null ? "" : toHexString(aBytes, 0, aBytes.length);
    }

    public static String toFormattedHexString(byte[] aBytes) {
        return toFormattedHexString(aBytes, 0, aBytes.length);
    }

    public static String toHexString(byte[] aBytes, int aLength) {
        return toHexString(aBytes, 0, aLength);
    }

    public static byte[] parseHex(String aHexString) {
        char[] src = aHexString.replace("\n", "").replace(" ", "").toUpperCase().toCharArray();
        byte[] dst = new byte[src.length / 2];
        int si = 0;
        for(int di = 0; di < dst.length; ++di) {
            byte high = BYTES[src[si++] & 127];
            byte low = BYTES[src[si++] & 127];
            dst[di] = (byte)((high << 4) + low);
        }

        return dst;
    }

    public static int toInt(String aHexString) {
        return Integer.parseInt(aHexString, 16);
    }

    public static String toFormattedHexString(byte[] aBytes, int aOffset, int aLength) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(aLength);
        sb.append("] :");
        int si = aOffset;
        for(int di = 0; si < aOffset + aLength; ++di) {
            byte b = aBytes[si];
            if (di % 4 == 0) {
                sb.append("  ");
            } else {
                sb.append(' ');
            }

            sb.append(CHARS_TABLES[(b & 240) >>> 4]);
            sb.append(CHARS_TABLES[b & 15]);
            ++si;
        }

        return sb.toString();
    }

    public static String toHexString(byte[] aBytes, int aOffset, int aLength) {
        char[] dst = new char[aLength * 2];
        int si = aOffset;
        for(int var5 = 0; si < aOffset + aLength; ++si) {
            byte b = aBytes[si];
            dst[var5++] = CHARS_TABLES[(b & 240) >>> 4];
            dst[var5++] = CHARS_TABLES[b & 15];
        }

        return new String(dst);
    }

    static {
        for(int i = 0; i < 10; ++i) {
            BYTES[48 + i] = (byte)i;
            BYTES[65 + i] = (byte)(10 + i);
            BYTES[97 + i] = (byte)(10 + i);
        }

    }

}
