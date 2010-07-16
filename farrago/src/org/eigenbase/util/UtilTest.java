/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2005 The Eigenbase Project
// Copyright (C) 2002 SQLstream, Inc.
// Copyright (C) 2005 Dynamo BI Corporation
// Portions Copyright (C) 2003 John V. Sichi
//
// This program is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the Free
// Software Foundation; either version 2 of the License, or (at your option)
// any later version approved by The Eigenbase Project.
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

import java.io.*;

import java.lang.management.*;

import java.math.*;

import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.*;

import junit.framework.*;

import junit.textui.*;

import org.eigenbase.sql.*;
import org.eigenbase.sql.util.*;
import org.eigenbase.test.*;


/**
 * Unit test for {@link Util} and other classes in this package.
 *
 * @author jhyde
 * @version $Id$
 * @since Jul 12, 2004
 */
public class UtilTest
    extends TestCase
{
    //~ Constructors -----------------------------------------------------------

    public UtilTest(String name)
    {
        super(name);
    }

    //~ Methods ----------------------------------------------------------------

    public static Test suite()
        throws Exception
    {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(UtilTest.class);
        return suite;
    }

    public void testPrintEquals()
    {
        assertPrintEquals("\"x\"", "x", true);
    }

    public void testPrintEquals2()
    {
        assertPrintEquals("\"x\"", "x", false);
    }

    public void testPrintEquals3()
    {
        assertPrintEquals("null", null, true);
    }

    public void testPrintEquals4()
    {
        assertPrintEquals("", null, false);
    }

    public void testPrintEquals5()
    {
        assertPrintEquals("\"\\\\\\\"\\r\\n\"", "\\\"\r\n", true);
    }

    public void testScientificNotation()
    {
        BigDecimal bd;

        bd = new BigDecimal("0.001234");
        TestUtil.assertEqualsVerbose(
            "1.234E-3",
            Util.toScientificNotation(bd));
        bd = new BigDecimal("0.001");
        TestUtil.assertEqualsVerbose(
            "1E-3",
            Util.toScientificNotation(bd));
        bd = new BigDecimal("-0.001");
        TestUtil.assertEqualsVerbose(
            "-1E-3",
            Util.toScientificNotation(bd));
        bd = new BigDecimal("1");
        TestUtil.assertEqualsVerbose(
            "1E0",
            Util.toScientificNotation(bd));
        bd = new BigDecimal("-1");
        TestUtil.assertEqualsVerbose(
            "-1E0",
            Util.toScientificNotation(bd));
        bd = new BigDecimal("1.0");
        TestUtil.assertEqualsVerbose(
            "1.0E0",
            Util.toScientificNotation(bd));
        bd = new BigDecimal("12345");
        TestUtil.assertEqualsVerbose(
            "1.2345E4",
            Util.toScientificNotation(bd));
        bd = new BigDecimal("12345.00");
        TestUtil.assertEqualsVerbose(
            "1.234500E4",
            Util.toScientificNotation(bd));
        bd = new BigDecimal("12345.001");
        TestUtil.assertEqualsVerbose(
            "1.2345001E4",
            Util.toScientificNotation(bd));

        //test truncate
        bd = new BigDecimal("1.23456789012345678901");
        TestUtil.assertEqualsVerbose(
            "1.2345678901234567890E0",
            Util.toScientificNotation(bd));
        bd = new BigDecimal("-1.23456789012345678901");
        TestUtil.assertEqualsVerbose(
            "-1.2345678901234567890E0",
            Util.toScientificNotation(bd));
    }

    public void testToJavaId()
        throws UnsupportedEncodingException
    {
        assertEquals(
            "ID$0$foo",
            Util.toJavaId("foo", 0));
        assertEquals(
            "ID$0$foo_20_bar",
            Util.toJavaId("foo bar", 0));
        assertEquals(
            "ID$0$foo__bar",
            Util.toJavaId("foo_bar", 0));
        assertEquals(
            "ID$100$_30_bar",
            Util.toJavaId("0bar", 100));
        assertEquals(
            "ID$0$foo0bar",
            Util.toJavaId("foo0bar", 0));
        assertEquals(
            "ID$0$it_27_s_20_a_20_bird_2c__20_it_27_s_20_a_20_plane_21_",
            Util.toJavaId("it's a bird, it's a plane!", 0));

        // Try some funny non-ASCII charsets
        assertEquals(
            "ID$0$_f6__cb__c4__ca__ae__c1__f9__cb_",
            Util.toJavaId(
                "\u00f6\u00cb\u00c4\u00ca\u00ae\u00c1\u00f9\u00cb",
                0));
        assertEquals(
            "ID$0$_f6cb__c4ca__aec1__f9cb_",
            Util.toJavaId("\uf6cb\uc4ca\uaec1\uf9cb", 0));
        byte [] bytes1 = { 3, 12, 54, 23, 33, 23, 45, 21, 127, -34, -92, -113 };
        assertEquals(
            "ID$0$_3__c_6_17__21__17__2d__15__7f__6cd9__fffd_",
            Util.toJavaId(
                new String(bytes1, "EUC-JP"),
                0));
        byte [] bytes2 =
        { 64, 32, 43, -45, -23, 0, 43, 54, 119, -32, -56, -34 };
        assertEquals(
            "ID$0$_30c__3617__2117__2d15__7fde__a48f_",
            Util.toJavaId(
                new String(bytes1, "UTF-16"),
                0));
    }

    private void assertPrintEquals(
        String expect,
        String in,
        boolean nullMeansNull)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        Util.printJavaString(pw, in, nullMeansNull);
        pw.flush();
        String out = sw.toString();
        assertEquals(expect, out);
    }

    /**
     * Tests whether {@link EnumeratedValues} serialize correctly.
     */
    public void testSerializeEnumeratedValues()
        throws IOException, ClassNotFoundException
    {
        UnserializableEnum unser =
            (UnserializableEnum) serializeAndDeserialize(
                UnserializableEnum.Foo);
        assertFalse(unser == UnserializableEnum.Foo);

        SerializableEnum ser =
            (SerializableEnum) serializeAndDeserialize(SerializableEnum.Foo);
        assertTrue(ser == SerializableEnum.Foo);
    }

    private static Object serializeAndDeserialize(Object e1)
        throws IOException, ClassNotFoundException
    {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bout);

        out.writeObject(e1);
        out.flush();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bin);

        Object o = in.readObject();
        return o;
    }

    /**
     * Unit-test for {@link BitString}.
     */
    public void testBitString()
    {
        // Powers of two, minimal length.
        final BitString b0 = new BitString("", 0);
        final BitString b1 = new BitString("1", 1);
        final BitString b2 = new BitString("10", 2);
        final BitString b4 = new BitString("100", 3);
        final BitString b8 = new BitString("1000", 4);
        final BitString b16 = new BitString("10000", 5);
        final BitString b32 = new BitString("100000", 6);
        final BitString b64 = new BitString("1000000", 7);
        final BitString b128 = new BitString("10000000", 8);
        final BitString b256 = new BitString("100000000", 9);

        // other strings
        final BitString b0_1 = new BitString("", 1);
        final BitString b0_12 = new BitString("", 12);

        // conversion to hex strings
        assertEquals(
            "",
            b0.toHexString());
        assertEquals(
            "1",
            b1.toHexString());
        assertEquals(
            "2",
            b2.toHexString());
        assertEquals(
            "4",
            b4.toHexString());
        assertEquals(
            "8",
            b8.toHexString());
        assertEquals(
            "10",
            b16.toHexString());
        assertEquals(
            "20",
            b32.toHexString());
        assertEquals(
            "40",
            b64.toHexString());
        assertEquals(
            "80",
            b128.toHexString());
        assertEquals(
            "100",
            b256.toHexString());
        assertEquals(
            "0",
            b0_1.toHexString());
        assertEquals(
            "000",
            b0_12.toHexString());

        // to byte array
        assertByteArray("01", "1", 1);
        assertByteArray("01", "1", 5);
        assertByteArray("01", "1", 8);
        assertByteArray("00, 01", "1", 9);
        assertByteArray("", "", 0);
        assertByteArray("00", "0", 1);
        assertByteArray("00", "0000", 2); // bit count less than string
        assertByteArray("00", "000", 5); // bit count larger than string
        assertByteArray("00", "0", 8); // precisely 1 byte
        assertByteArray("00, 00", "00", 9); // just over 1 byte

        // from hex string
        assertReversible("");
        assertReversible("1");
        assertReversible("10");
        assertReversible("100");
        assertReversible("1000");
        assertReversible("10000");
        assertReversible("100000");
        assertReversible("1000000");
        assertReversible("10000000");
        assertReversible("100000000");
        assertReversible("01");
        assertReversible("001010");
        assertReversible("000000000100");
    }

    private static void assertReversible(String s)
    {
        assertEquals(
            s,
            BitString.createFromBitString(s).toBitString(),
            s);
        assertEquals(
            s,
            BitString.createFromHexString(s).toHexString());
    }

    private void assertByteArray(
        String expected,
        String bits,
        int bitCount)
    {
        byte [] bytes = BitString.toByteArrayFromBitString(bits, bitCount);
        final String s = toString(bytes);
        assertEquals(expected, s);
    }

    /**
     * Converts a byte array to a hex string like "AB, CD".
     */
    private String toString(byte [] bytes)
    {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            if (i > 0) {
                buf.append(", ");
            }
            String s = Integer.toString(b, 16);
            buf.append((b < 16) ? ("0" + s) : s);
        }
        return buf.toString();
    }

    /**
     * Tests {@link CastingList} and {@link Util#cast}.
     */
    public void testCastingList()
    {
        final List<Number> numberList = new ArrayList<Number>();
        numberList.add(new Integer(1));
        numberList.add(null);
        numberList.add(new Integer(2));
        List<Integer> integerList = Util.cast(numberList, Integer.class);
        assertEquals(3, integerList.size());
        assertEquals(new Integer(2), integerList.get(2));

        // Nulls are OK.
        assertNull(integerList.get(1));

        // Can update the underlying list.
        integerList.set(1, 345);
        assertEquals(new Integer(345), integerList.get(1));
        integerList.set(1, null);
        assertNull(integerList.get(1));

        // Can add a member of the wrong type to the underlying list.
        numberList.add(new Double(3.1415));
        assertEquals(4, integerList.size());

        // Access a member which is of the wrong type.
        try {
            integerList.get(3);
            fail("expected exception");
        } catch (ClassCastException e) {
            // ok
        }
    }

    public void testIterableProperties()
    {
        Properties properties = new Properties();
        properties.put("foo", "george");
        properties.put("bar", "ringo");
        StringBuilder sb = new StringBuilder();
        for (
            Map.Entry<String, String> entry : Util.toMap(properties).entrySet())
        {
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            sb.append(";");
        }
        assertEquals("bar=ringo;foo=george;", sb.toString());

        assertEquals(2, Util.toMap(properties).entrySet().size());

        properties.put("nonString", 34);
        try {
            for (
                Map.Entry<String, String> entry
                : Util.toMap(properties).entrySet())
            {
                String s = entry.getValue();
                Util.discard(s);
            }
            fail("expected exception");
        } catch (ClassCastException e) {
            // ok
        }
    }

    /**
     * Tests the difference engine, {@link DiffTestCase#diff}.
     */
    public void testDiffLines()
    {
        String [] before =
        {
            "Get a dose of her in jackboots and kilt",
            "She's killer-diller when she's dressed to the hilt",
            "She's the kind of a girl that makes The News of The World",
            "Yes you could say she was attractively built.",
            "Yeah yeah yeah."
        };
        String [] after =
        {
            "Get a dose of her in jackboots and kilt",
            "(they call her \"Polythene Pam\")",
            "She's killer-diller when she's dressed to the hilt",
            "She's the kind of a girl that makes The Sunday Times",
            "seem more interesting.",
            "Yes you could say she was attractively built."
        };
        String diff =
            DiffTestCase.diffLines(
                Arrays.asList(before),
                Arrays.asList(after));
        assertEquals(
            diff,
            TestUtil.fold(
                "1a2\n"
                + "> (they call her \"Polythene Pam\")\n"
                + "3c4,5\n"
                + "< She's the kind of a girl that makes The News of The World\n"
                + "---\n"
                + "> She's the kind of a girl that makes The Sunday Times\n"
                + "> seem more interesting.\n"
                + "5d6\n"
                + "< Yeah yeah yeah.\n"));
    }

    /**
     * Tests the {@link Util#toPosix(TimeZone, boolean)} method.
     */
    public void testPosixTimeZone()
    {
        // NOTE jvs 31-July-2007:  First two tests are disabled since
        // not everyone may have patched their system yet for recent
        // DST change.

        // Pacific Standard Time. Effective 2007, the local time changes from
        // PST to PDT at 02:00 LST to 03:00 LDT on the second Sunday in March
        // and returns at 02:00 LDT to 01:00 LST on the first Sunday in
        // November.
        if (false) {
            assertEquals(
                "PST-8PDT,M3.2.0,M11.1.0",
                Util.toPosix(TimeZone.getTimeZone("PST"), false));

            assertEquals(
                "PST-8PDT1,M3.2.0/2,M11.1.0/2",
                Util.toPosix(TimeZone.getTimeZone("PST"), true));
        }

        // Tokyo has +ve offset, no DST
        assertEquals(
            "JST9",
            Util.toPosix(TimeZone.getTimeZone("Asia/Tokyo"), true));

        // Sydney, Australia lies ten hours east of GMT and makes a one hour
        // shift forward during daylight savings. Being located in the southern
        // hemisphere, daylight savings begins on the last Sunday in October at
        // 2am and ends on the last Sunday in March at 3am.
        // (Uses STANDARD_TIME time-transition mode.)

        // Because australia changed their daylight savings rules, some JVMs
        // have a different (older and incorrect) timezone settings for
        // Australia.  So we test for the older one first then do the
        // correct assert based upon what the toPosix method returns
        String posixTime =
            Util.toPosix(TimeZone.getTimeZone("Australia/Sydney"), true);

        if (posixTime.equals("EST10EST1,M10.5.0/2,M3.5.0/3")) {
            // older JVMs without the fix
            assertEquals("EST10EST1,M10.5.0/2,M3.5.0/3", posixTime);
        } else {
            // newer JVMs with the fix
            assertEquals("EST10EST1,M10.1.0/2,M4.1.0/3", posixTime);
        }

        // Paris, France. (Uses UTC_TIME time-transition mode.)
        assertEquals(
            "CET1CEST1,M3.5.0/2,M10.5.0/3",
            Util.toPosix(TimeZone.getTimeZone("Europe/Paris"), true));

        assertEquals(
            "UTC0",
            Util.toPosix(TimeZone.getTimeZone("UTC"), true));
    }

    /**
     * Tests the methods {@link Util#enumConstants(Class)} and {@link
     * Util#enumVal(Class, String)}.
     */
    public void testEnumConstants()
    {
        final Map<String, MemoryType> memoryTypeMap =
            Util.enumConstants(MemoryType.class);
        assertEquals(2, memoryTypeMap.size());
        assertEquals(MemoryType.HEAP, memoryTypeMap.get("HEAP"));
        assertEquals(MemoryType.NON_HEAP, memoryTypeMap.get("NON_HEAP"));
        try {
            memoryTypeMap.put("FOO", null);
            fail("expected exception");
        } catch (UnsupportedOperationException e) {
            // expected: map is immutable
        }

        assertEquals("HEAP", Util.enumVal(MemoryType.class, "HEAP").name());
        assertNull(Util.enumVal(MemoryType.class, "heap"));
        assertNull(Util.enumVal(MemoryType.class, "nonexistent"));
    }

    /**
     * Tests the method {@link Util#toIter(java.util.BitSet)}.
     */
    public void testToIterBitSet()
    {
        BitSet bitSet = new BitSet();

        assertToIterBitSet("", bitSet);
        bitSet.set(0);
        assertToIterBitSet("0", bitSet);
        bitSet.set(1);
        assertToIterBitSet("0, 1", bitSet);
        bitSet.clear();
        bitSet.set(10);
        assertToIterBitSet("10", bitSet);
    }

    /**
     * Tests that iterating over a BitSet yields the expected string.
     *
     * @param expected Expected string
     * @param bitSet Bit set
     */
    private void assertToIterBitSet(
        final String expected, BitSet bitSet)
    {
        StringBuilder buf = new StringBuilder();
        for (int i : Util.toIter(bitSet)) {
            if (buf.length() > 0) {
                buf.append(", ");
            }
            buf.append(Integer.toString(i));
        }
        assertEquals(expected, buf.toString());
    }

    /**
     * Tests the method {@link Util#toList(java.util.BitSet)}.
     */
    public void testToListBitSet()
    {
        BitSet bitSet = new BitSet(10);
        assertEquals(Util.toList(bitSet), Collections.<Integer>emptyList());
        bitSet.set(5);
        assertEquals(Util.toList(bitSet), Arrays.asList(5));
        bitSet.set(3);
        assertEquals(Util.toList(bitSet), Arrays.asList(3, 5));
    }

    /**
     * Tests the method {@link Util#bitSetOf(int...)}.
     */
    public void testBitSetOf()
    {
        assertEquals(
            Util.toList(Util.bitSetOf(0, 4, 2)), Arrays.asList(0, 2, 4));
        assertEquals(
            Util.toList(Util.bitSetOf()), Collections.<Integer>emptyList());
    }

    /**
     * Tests the method {@link Util#bitSetBetween(int, int)}.
     */
    public void testBitSetBetween()
    {
        assertEquals(
            Util.toList(Util.bitSetBetween(0, 4)), Arrays.asList(0, 1, 2, 3));
        assertEquals(
            Util.toList(Util.bitSetBetween(1, 4)), Arrays.asList(1, 2, 3));
        assertEquals(
            Util.toList(Util.bitSetBetween(2, 2)),
            Collections.<Integer>emptyList());
    }

    /**
     * Tests SQL builders.
     */
    public void testSqlBuilder()
    {
        final SqlBuilder buf = new SqlBuilder(SqlDialect.EIGENBASE);
        assertEquals(0, buf.length());
        buf.append("select ");
        assertEquals("select ", buf.getSql());

        buf.identifier("x");
        assertEquals("select \"x\"", buf.getSql());

        buf.append(", ");
        buf.identifier("y", "a b");
        assertEquals("select \"x\", \"y\".\"a b\"", buf.getSql());

        final SqlString sqlString = buf.toSqlString();
        assertEquals(SqlDialect.EIGENBASE, sqlString.getDialect());
        assertEquals(buf.getSql(), sqlString.getSql());

        assertTrue(buf.getSql().length() > 0);
        assertEquals(buf.getSqlAndClear(), sqlString.getSql());
        assertEquals(0, buf.length());

        buf.clear();
        assertEquals(0, buf.length());

        buf.literal("can't get no satisfaction");
        assertEquals("'can''t get no satisfaction'", buf.getSqlAndClear());

        buf.literal(new Timestamp(0));
        assertEquals("TIMESTAMP '1970-01-01 00:00:00'", buf.getSqlAndClear());

        buf.clear();
        assertEquals(0, buf.length());

        buf.append("hello world");
        assertEquals(2, buf.indexOf("l"));
        assertEquals(-1, buf.indexOf("z"));
        assertEquals(9, buf.indexOf("l", 5));
    }

    /**
     * Unit test for {@link org.eigenbase.util.CompositeList}.
     */
    public void testCompositeList()
    {
        // Made up of zero lists
        CompositeList<String> list = new CompositeList<String>();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
        try {
            final String s = list.get(0);
            fail("expected error, got " + s);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        assertFalse(list.listIterator().hasNext());

        List<String> listEmpty = Collections.emptyList();
        List<String> listAbc = Arrays.asList("a", "b", "c");
        List<String> listEmpty2 = new ArrayList<String>();

        // Made up of two lists, two of which are empty
        list = new CompositeList<String>(listEmpty, listAbc, listEmpty2);
        assertEquals(3, list.size());
        assertFalse(list.isEmpty());
        assertEquals("a", list.get(0));
        assertEquals("c", list.get(2));
        try {
            final String s = list.get(3);
            fail("expected error, got " + s);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            final String s = list.set(0, "z");
            fail("expected error, got " + s);
        } catch (UnsupportedOperationException e) {
            // ok
        }

        // Iterator
        final Iterator<String> iterator = list.iterator();
        assertTrue(iterator.hasNext());
        assertEquals("a", iterator.next());
        assertEquals("b", iterator.next());
        assertTrue(iterator.hasNext());
        try {
            iterator.remove();
            fail("expected error");
        } catch (UnsupportedOperationException e) {
            // ok
        }
        assertEquals("c", iterator.next());
        assertFalse(iterator.hasNext());

        // Extend one of the backing lists, and list grows.
        listEmpty2.add("zz");
        assertEquals(4, list.size());
        assertEquals("zz", list.get(3));

        // Syntactic sugar 'of' method
        String ss = "";
        for (String s : CompositeList.of(list, list)) {
            ss += s;
        }
        assertEquals("abczzabczz", ss);
    }

    /**
     * Unit test for {@link Template}.
     */
    public void testTemplate()
    {
        // Regular java message format.
        assertEquals(
            "Hello, world, what a nice day.",
            MessageFormat.format(
                "Hello, {0}, what a nice {1}.", "world", "day"));

        // Our extended message format. First, just strings.
        final HashMap<Object, Object> map = new HashMap<Object, Object>();
        map.put("person", "world");
        map.put("time", "day");
        assertEquals(
            "Hello, world, what a nice day.",
            Template.formatByName(
                "Hello, {person}, what a nice {time}.", map));

        // String and an integer.
        final Template template =
            new Template("Happy {age,number,#.00}th birthday, {person}!");
        map.clear();
        map.put("person", "Ringo");
        map.put("age", 64.5);
        assertEquals(
            "Happy 64.50th birthday, Ringo!",
            template.format(map));

        // Missing parameters evaluate to null.
        map.remove("person");
        assertEquals(
            "Happy 64.50th birthday, null!",
            template.format(map));

        // Specify parameter by Integer ordinal.
        map.clear();
        map.put(1, "Ringo");
        map.put("0", 64.5);
        assertEquals(
            "Happy 64.50th birthday, Ringo!",
            template.format(map));

        // Too many parameters supplied.
        map.put("lastName", "Starr");
        map.put("homeTown", "Liverpool");
        assertEquals(
            "Happy 64.50th birthday, Ringo!",
            template.format(map));

        // Get parameter names. In order of appearance.
        assertEquals(
            Arrays.asList("age", "person"),
            template.getParameterNames());

        // No parameters; doubled single quotes; quoted braces.
        final Template template2 =
            new Template("Don''t expand 'this {brace}'.");
        assertEquals(
            Collections.<String>emptyList(), template2.getParameterNames());
        assertEquals(
            "Don't expand this {brace}.",
            template2.format(Collections.<Object, Object>emptyMap()));

        // Empty template.
        assertEquals("", Template.formatByName("", map));
    }

    /**
     * Runs the test suite.
     */
    public static void main(String [] args)
        throws Exception
    {
        TestRunner.run(suite());
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * Enumeration which extends BasicValue does NOT serialize correctly.
     */
    private static class UnserializableEnum
        extends EnumeratedValues.BasicValue
    {
        public static final UnserializableEnum Foo =
            new UnserializableEnum("foo", 1);
        public static final UnserializableEnum Bar =
            new UnserializableEnum("bar", 2);

        public UnserializableEnum(String name, int ordinal)
        {
            super(name, ordinal, null);
        }
    }

    /**
     * Enumeration which serializes correctly.
     */
    private static class SerializableEnum
        extends EnumeratedValues.SerializableValue
    {
        public static final SerializableEnum Foo =
            new SerializableEnum("foo", 1);
        public static final SerializableEnum Bar =
            new SerializableEnum("bar", 2);

        public SerializableEnum(String name, int ordinal)
        {
            super(name, ordinal, null);
        }

        protected Object readResolve()
            throws ObjectStreamException
        {
            switch (_ordinal) {
            case 1:
                return Foo;
            case 2:
                return Bar;
            default:
                throw new IllegalArgumentException();
            }
        }
    }
}

// End UtilTest.java
