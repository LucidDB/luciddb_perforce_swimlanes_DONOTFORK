/*
// $Id$
// Package org.eigenbase is a class library of database components.
// Copyright (C) 2002-2004 Disruptive Tech
// Copyright (C) 2003-2004 John V. Sichi
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.eigenbase.util;

import java.math.BigInteger;


/**
 * String of bits.
 *
 * <p>A bit string logically consists of a set of '0' and '1' values, of a
 * specified length. The length is preserved even if this means that the
 * bit string has leading '0's.
 *
 * <p>You can create a bit string
 * from a string of 0s and 1s ({@link #BitString(String,int)}
 * or {@link #createFromBitString}), or
 * from a string of hex digits ({@link #createFromHexString}).
 * You can convert it
 * to a byte array ({@link #getAsByteArray}),
 * to a bit string ({@link #toBitString}), or
 * to a hex string ({@link #toHexString}).
 * A utility method {@link #toByteArrayFromBitString} converts a bit string
 * directly to a byte array.
 *
 * <p>This class is immutable: once created, none of the methods modify the
 * value.
 *
 * @testcase {@link UtilTest#testBitString}
 * @author Wael Chatila
 * @since May 28, 2004
 * @version $Id$
 **/
public class BitString
{
    //~ Instance fields -------------------------------------------------------

    private String bits;
    private int bitCount;

    //~ Constructors ----------------------------------------------------------

    protected BitString(
        String bits,
        int bitCount)
    {
        assert bits.replaceAll("1", "").replaceAll("0", "").length() == 0 : "bit string '"
        + bits + "' contains digits other than {0, 1}";
        this.bits = bits;
        this.bitCount = bitCount;
    }

    //~ Methods ---------------------------------------------------------------

    /**
     * Creates a BitString representation out of a Hex String.
     * Initial zeros are be preserved.
     * Hex String is defined in the SQL standard to be a string with odd number of
     * hex digits.  An even number of hex digits is in the standard a Binary String.
     * @param s a string, in hex notation
     * @throws NumberFormatException if <code>s</code> is invalid.
     */
    public static BitString createFromHexString(String s)
    {
        int bitCount = s.length() * 4;
        String bits = (bitCount == 0) ? "" : new BigInteger(s, 16).toString(2);
        return new BitString(bits, bitCount);
    }

    /**
     * Creates a BitString representation out of a Bit String.
     * Initial zeros are be preserved.
     * @param s a string of 0s and 1s.
     * @throws NumberFormatException if <code>s</code> is invalid.
     */
    public static BitString createFromBitString(String s)
    {
        int n = s.length();
        if (n > 0) { // check that S is valid
            Util.discard(new BigInteger(s, 2));
        }
        return new BitString(s, n);
    }

    public String toString()
    {
        return toBitString();
    }

    public int getBitCount()
    {
        return bitCount;
    }

    public byte [] getAsByteArray()
    {
        return toByteArrayFromBitString(bits, bitCount);
    }

    /**
     * Returns this bit string as a bit string, such as "10110".
     */
    public String toBitString()
    {
        return bits;
    }

    /**
     * Converts this bit string to a hex string, such as "7AB".
     */
    public String toHexString()
    {
        byte [] bytes = getAsByteArray();
        String s = Util.toStringFromByteArray(bytes, 16);
        switch (bitCount % 8) {
        case 1: // B'1' -> X'1'
        case 2: // B'10' -> X'2'
        case 3: // B'100' -> X'4'
        case 4: // B'1000' -> X'8'
            return s.substring(1);
        case 5: // B'10000' -> X'10'
        case 6: // B'100000' -> X'20'
        case 7: // B'1000000' -> X'40'
        case 0: // B'10000000' -> X'80', and B'' -> X''
            return s;
        }
        if ((bitCount % 8) == 4) {
            return s.substring(1);
        } else {
            return s;
        }
    }

    /**
     * Converts a bit string to an array of bytes.
     *
     * @post return.length = (bitCount + 7) / 8
     */
    public static byte [] toByteArrayFromBitString(
        String bits,
        int bitCount)
    {
        if (bitCount < 0) {
            return new byte[0];
        }
        int byteCount = (bitCount + 7) / 8;
        byte [] srcBytes;
        if (bits.length() > 0) {
            BigInteger bigInt = new BigInteger(bits, 2);
            srcBytes = bigInt.toByteArray();
        } else {
            srcBytes = new byte[0];
        }
        byte [] dest = new byte[byteCount];

        // If the number started with 0s, the array won't be very long. Assume
        // that ret is already initialized to 0s, and just copy into the
        // RHS of it.
        int bytesToCopy = Math.min(byteCount, srcBytes.length);
        System.arraycopy(srcBytes, srcBytes.length - bytesToCopy, dest,
            dest.length - bytesToCopy, bytesToCopy);
        return dest;
    }

    /** Concatenates some BitStrings.
     *  Concatenates all at once, not pairwise, to avoid string copies.
     * @param args BitString[]
     */
    static public BitString concat(BitString [] args)
    {
        if (args.length < 2) {
            return args[0];
        }
        int length = 0;
        for (int i = 0; i < args.length; i++) {
            length += args[i].bitCount;
        }
        StringBuffer sb = new StringBuffer(length);
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i].bits);
        }
        return new BitString(
            sb.toString(),
            length);
    }
}


// End BitString.java
