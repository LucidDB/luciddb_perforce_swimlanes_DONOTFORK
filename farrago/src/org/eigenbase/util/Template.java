/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2010 The Eigenbase Project
// Copyright (C) 2010 SQLstream, Inc.
// Copyright (C) 2010 Dynamo BI Corporation
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

import java.text.MessageFormat;
import java.util.*;


/**
 * String template.
 *
 * <p>It is extended from {@link java.text.MessageFormat} to allow parameters
 * to be substituted by name as well as by position.
 *
 * <p>The following example, using MessageFormat, yields "Happy 64th birthday,
 * Ringo!":
 *
 * <blockquote>MessageFormat f =
 * new MessageFormat("Happy {0,number}th birthday, {1}!");<br/>
 * Object[] args = {64, "Ringo"};<br/>
 * System.out.println(f.format(args);</blockquote>
 *
 * <p>Here is the same example using a Template and named parameters:
 *
 * <blockquote>Template f =
 * new Template("Happy {age,number}th birthday, {name}!");<br/>
 * Map&lt;Object, Object&gt; args = new HashMap&lt;Object, Object&gt;();<br/>
 * args.put("age", 64);<br/>
 * args.put("name", "Ringo");<br/>
 * System.out.println(f.format(args);</blockquote>
 *
 * <p>Using a Template you can also use positional parameters:
 *
 * <blockquote>Template f =
 * new Template("Happy {age,number}th birthday, {name}!");<br/>
 * Object[] args = {64, "Ringo"};<br/>
 * System.out.println(f.format(args);</blockquote>
 *
 * <p>Or a hybrid; here, one argument is specified by name, another by position:
 *
 * <blockquote>Template f =
 * new Template("Happy {age,number}th birthday, {name}!");<br/>
 * Map&lt;Object, Object&gt; args = new HashMap&lt;Object, Object&gt;();<br/>
 * args.put(0, 64);<br/>
 * args.put("name", "Ringo");<br/>
 * System.out.println(f.format(args);</blockquote>
 *
 * @author jhyde
 * @version $Id$
 */
public class Template extends MessageFormat
{
    private final List<String> parameterNames = new ArrayList<String>();

    /**
     * Creates a Template for the default locale and the
     * specified pattern.
     *
     * @param pattern the pattern for this message format
     * @exception IllegalArgumentException if the pattern is invalid
     */
    public Template(String pattern)
    {
        this(pattern, Locale.getDefault());
    }

    /**
     * Creates a Template for the specified locale and
     * pattern.
     *
     * @param pattern the pattern for this message format
     * @param locale the locale for this message format
     * @exception IllegalArgumentException if the pattern is invalid
     */
    public Template(String pattern, Locale locale)
    {
        super(process(pattern, new ArrayList<String>()), locale);
        final String processedPattern = process(pattern, parameterNames);
        Util.discard(processedPattern);
    }

    /**
     * Parses the pattern, populates the parameter names, and returns the
     * pattern with parameter names converted to parameter ordinals.
     *
     * <p>To ensure that the same parsing rules apply, this code is copied from
     * {@link java.text.MessageFormat#applyPattern(String)} but with different
     * actions when a parameter is recognized.
     *
     * @param pattern Pattern
     * @param parameterNames Names of parameters (output)
     * @return Pattern with named parameters substituted with ordinals
     */
    private static String process(String pattern, List<String> parameterNames)
    {
        StringBuffer[] segments = new StringBuffer[4];
        for (int i = 0; i < segments.length; ++i) {
            segments[i] = new StringBuffer();
        }
        int part = 0;
        boolean inQuote = false;
        int braceStack = 0;
        for (int i = 0; i < pattern.length(); ++i) {
            char ch = pattern.charAt(i);
            if (part == 0) {
                if (ch == '\'') {
                    segments[part].append(ch); // jhyde: don't lose orig quote
                    if (i + 1 < pattern.length()
                        && pattern.charAt(i + 1) == '\'')
                    {
                        segments[part].append(ch);  // handle doubles
                        ++i;
                    } else {
                        inQuote = !inQuote;
                    }
                } else if (ch == '{' && !inQuote) {
                    part = 1;
                } else {
                    segments[part].append(ch);
                }
            } else if (inQuote) {              // just copy quotes in parts
                segments[part].append(ch);
                if (ch == '\'') {
                    inQuote = false;
                }
            } else {
                switch (ch) {
                case ',':
                    if (part < 3) {
                        part += 1;
                    } else {
                        segments[part].append(ch);
                    }
                    break;
                case '{':
                    ++braceStack;
                    segments[part].append(ch);
                    break;
                case '}':
                    if (braceStack == 0) {
                        part = 0;
                        makeFormat(segments, parameterNames);
                    } else {
                        --braceStack;
                        segments[part].append(ch);
                    }
                    break;
                case '\'':
                    inQuote = true;
                    // fall through, so we keep quotes in other parts
                default:
                    segments[part].append(ch);
                    break;
                }
            }
        }
        if (braceStack == 0 && part != 0) {
            throw new IllegalArgumentException(
                "Unmatched braces in the pattern.");
        }
        return segments[0].toString();
    }

    /**
     * Called when a complete parameter has been seen.
     *
     * @param segments Comma-separated segments of the parameter definition
     * @param parameterNames List of parameter names seen so far
     */
    private static void makeFormat(
        StringBuffer[] segments,
        List<String> parameterNames)
    {
        final String parameterName = segments[1].toString();
        final int parameterOrdinal = parameterNames.size();
        parameterNames.add(parameterName);
        segments[0].append("{");
        segments[0].append(parameterOrdinal);
        final String two = segments[2].toString();
        if (two.length() > 0) {
            segments[0].append(",").append(two);
        }
        final String three = segments[3].toString();
        if (three.length() > 0) {
            segments[0].append(",").append(three);
        }
        segments[0].append("}");
        segments[1].setLength(0);   // throw away other segments
        segments[2].setLength(0);
        segments[3].setLength(0);
    }

    /**
     * Formats a set of arguments to produce a string.
     *
     * <p>Arguments may appear in the map using named keys (of type String), or
     * positional keys (0-based ordinals represented either as type String or
     * Integer).
     *
     * @param argMap A map containing the arguments as (key, value) pairs
     * @return       Formatted string.
     * @exception IllegalArgumentException if the Format cannot format the given
     *            object
     */
    public String format(Map<Object, Object> argMap)
    {
        Object[] args = new Object[parameterNames.size()];
        for (int i = 0; i < parameterNames.size(); i++) {
            args[i] = getArg(argMap, i);
        }
        return format(args);
    }

    /**
     * Returns the value of the {@code ordinal}th argument.
     *
     * @param argMap Map of argument values
     * @param ordinal Ordinal of argument
     * @return Value of argument
     */
    private Object getArg(Map<Object, Object> argMap, int ordinal)
    {
        // First get by name.
        String parameterName = parameterNames.get(ordinal);
        Object arg = argMap.get(parameterName);
        if (arg != null) {
            return arg;
        }
        // Next by integer ordinal.
        arg = argMap.get(ordinal);
        if (arg != null) {
            return arg;
        }
        // Next by string ordinal.
        return argMap.get(ordinal + "");
    }

    /**
     * Creates a Template with the given pattern and uses it
     * to format the given arguments. This is equivalent to
     * <blockquote>
     *     <code>new {@link #Template(String) Template}(pattern).{@link #format}(args)</code>
     * </blockquote>
     *
     * @exception IllegalArgumentException if the pattern is invalid,
     *            or if an argument in the <code>arguments</code> array
     *            is not of the type expected by the format element(s)
     *            that use it.
     */
    public static String formatByName(
        String pattern,
        Map<Object, Object> argMap)
    {
        return new Template(pattern).format(argMap);
    }

    /**
     * Returns the names of the parameters, in the order that they appeared in
     * the template string.
     *
     * @return List of parameter names
     */
    public List<String> getParameterNames()
    {
        return Collections.unmodifiableList(parameterNames);
    }
}

// End Template.java
