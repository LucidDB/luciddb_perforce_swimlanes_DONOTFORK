/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2005-2006 The Eigenbase Project
// Copyright (C) 2002-2006 Disruptive Tech
// Copyright (C) 2005-2006 LucidEra, Inc.
// Portions Copyright (C) 2003-2006 John V. Sichi
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
package org.eigenbase.util14;

import java.sql.*;
import java.text.*;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Utility functions for datetime types: date, time, timestamp. Refactored 
 * from SqlParserUtil because they are required by the Jdbc driver.
 * 
 * TODO: review methods for performance. Due to allocations required, it 
 * may be preferable to introduce a "formatter" with the required state.
 *
 * @version $Id$
 * @since Sep 28, 2006
 */
public class DateTimeUtil
{

    //~ Static fields/initializers ---------------------------------------------

    /**
     * the SimpleDateFormat string for ISO dates, "yyyy-MM-dd"
     */
    public static final String DateFormatStr = "yyyy-MM-dd";

    /**
     * the SimpleDateFormat string for ISO times, "HH:mm:ss"
     */
    public static final String TimeFormatStr = "HH:mm:ss";

    /**
     * the SimpleDateFormat string for ISO timestamps, "yyyy-MM-dd HH:mm:ss"
     */
    public static final String TimestampFormatStr = 
        DateFormatStr + " " + TimeFormatStr;

    /**
     * the GMT time zone
     */
    public static final TimeZone gmtZone = TimeZone.getTimeZone("GMT");

    /**
     * the Java default time zone
     */
    public static final TimeZone defaultZone = TimeZone.getDefault();

    //~ Methods ----------------------------------------------------------------

    /**
     * Parses a string using {@link SimpleDateFormat} and a given pattern.
     * This method parses a string at the specified parse position and if
     * successful, updates the parse position to the index after the last 
     * character used. The parsing is strict and requires months to be 
     * less than 12, days to be less than 31, etc.
     *
     * @param s string to be parsed
     * @param pattern {@link SimpleDateFormat} pattern
     * @param tz time zone in which to interpret string. Defaults to the 
     *   Java default time zone
     * @param pp position to start parsing from
     *
     * @return a Calendar initialized with the parsed value, or null if 
     *   parsing failed. If returned, the Calendar is configured to the 
     *   GMT time zone.
     *
     * @pre pattern != null
     */
    private static Calendar parseDateFormat(
        String s,
        String pattern,
        TimeZone tz,
        ParsePosition pp)
    {
        assert(pattern != null);
        SimpleDateFormat df = new SimpleDateFormat(pattern);
        if (tz == null) {
            tz = defaultZone;
        }
        Calendar ret = Calendar.getInstance(tz);
        df.setCalendar(ret);
        df.setLenient(false);

        java.util.Date d = df.parse(s, pp);
        if (null == d) {
            return null;
        }
        ret.setTime(d);
        ret.setTimeZone(gmtZone);
        return ret;
    }

    /**
     * Parses a string using {@link SimpleDateFormat} and a given pattern.
     * The entire string must match the pattern specified.
     *
     * @param s string to be parsed
     * @param pattern {@link SimpleDateFormat} pattern
     * @param tz time zone in which to interpret string. Defaults to the 
     *   Java default time zone
     *
     * @return a Calendar initialized with the parsed value, or null if 
     *   parsing failed. If returned, the Calendar is configured to the 
     *   GMT time zone.
     *
     * @pre pattern != null
     */
    public static Calendar parseDateFormat(
        String s,
        String pattern,
        TimeZone tz)
    {
        assert(pattern != null);
        ParsePosition pp = new ParsePosition(0);
        Calendar ret = parseDateFormat(s, pattern, tz, pp);
        if (pp.getIndex() != s.length()) {
            // Didn't consume entire string - not good
            return null;
        }
        return ret;
    }

    /**
     * Parses a string using {@link SimpleDateFormat} and a given pattern,
     * and if present, parses a fractional seconds component. The fractional 
     * seconds component must begin with a decimal point ('.') followed by
     * numeric digits. The precision is rounded to a maximum of 3 digits of 
     * fractional seconds precision (to obtain milliseconds).
     *
     * @param s string to be parsed
     * @param pattern {@link SimpleDateFormat} pattern
     * @param tz time zone in which to interpret string. Defaults to the 
     *   local time zone
     *
     * @return a {@link DateTimeUtil.PrecisionTime PrecisionTime} 
     *   initialized with the parsed value, or null if parsing failed. The 
     *   PrecisionTime contains a GMT Calendar and a precision.
     *
     * @pre pattern != null
     */
    public static PrecisionTime parsePrecisionDateTimeLiteral(
        String s,
        String pattern,
        TimeZone tz)
    {
        ParsePosition pp = new ParsePosition(0);
        Calendar cal = parseDateFormat(s, pattern, tz, pp);
        if (cal == null) {
            return null; // Invalid date/time format
        }

        // Note: the Java SimpleDateFormat 'S' treats any number after 
        // the decimal as milliseconds. That means 12:00:00.9 has 9 
        // milliseconds and 12:00:00.9999 has 9999 milliseconds.
        int p = 0;
        if (pp.getIndex() < s.length()) {
            // Check to see if rest is decimal portion
            if (s.charAt(pp.getIndex()) != '.') {
                return null;
            }

            // Skip decimal sign
            pp.setIndex(pp.getIndex() + 1);

            // Parse decimal portion
            if (pp.getIndex() < s.length()) {
                String secFraction = s.substring(pp.getIndex());
                if (!secFraction.matches("\\d+")) {
                    return null;
                }
                NumberFormat nf = NumberFormat.getIntegerInstance();
                Number num = nf.parse(s, pp);
                if ((num == null) || (pp.getIndex() != s.length())) {
                    // Invalid decimal portion
                    return null;
                }

                // Determine precision - only support prec 3 or lower
                // (milliseconds) Higher precisions are quietly rounded away
                p = Math.min(
                        3,
                        secFraction.length());

                // Calculate milliseconds
                int ms =
                    (int) Math.round(
                        num.longValue()
                        * Math.pow(10,
                            3 - secFraction.length()));
                cal.add(Calendar.MILLISECOND, ms);
            }
        }

        assert (pp.getIndex() == s.length());
        PrecisionTime ret = new PrecisionTime(cal, p);
        return ret;
    }

    /**
     * Helper class for {@link DateTimeUtil#parsePrecisionDateTimeLiteral}
     */
    public static class PrecisionTime
    {
        private final Calendar cal;
        private final int precision;

        public PrecisionTime(Calendar cal, int precision)
        {
            this.cal = cal;
            this.precision = precision;
        }

        public Calendar getCalendar()
        {
            return cal;
        }

        public int getPrecision()
        {
            return precision;
        }
    }

    /**
     * Parses an Timestamp. This method is similar to the java.sql.Timestamp 
     * {@link java.sql.Timestamp#valueOf(java.lang.String) valueOf(String)} 
     * method. However, this method's parsing is strict and may parse 
     * fractional seconds (as opposed to just milliseconds.)
     * 
     * @param s a string representing a date in ISO format, i.e. according 
     *   to the SimpleDateFormat string "yyyy-MM-dd HH:mm:ss"
     * @param zone time zone from which to interpret the string
     * @return the parsed Timestamp, or null if parsing failed
     */
    public static Timestamp parseTimestamp(String s, TimeZone zone)
    {
        DateTimeUtil.PrecisionTime pt =
            DateTimeUtil.parsePrecisionDateTimeLiteral(
                s,
                TimestampFormatStr,
                zone);
        if (pt == null) {
            return null;
        }

        long millis = pt.getCalendar().getTime().getTime();
        return new Timestamp(millis);
    }
}

// End ConversionUtil.java