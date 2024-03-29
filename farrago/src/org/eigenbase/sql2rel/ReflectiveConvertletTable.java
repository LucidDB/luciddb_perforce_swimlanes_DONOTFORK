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
package org.eigenbase.sql2rel;

import java.lang.reflect.*;

import java.util.*;

import org.eigenbase.rex.*;
import org.eigenbase.sql.*;
import org.eigenbase.sql.parser.*;
import org.eigenbase.util.*;


/**
 * Implementation of {@link SqlRexConvertletTable} which uses reflection to call
 * any method of the form <code>public RexNode convertXxx(ConvertletContext,
 * SqlNode)</code> or <code>public RexNode convertXxx(ConvertletContext,
 * SqlOperator, SqlCall)</code>.
 *
 * @author jhyde
 * @version $Id$
 * @since 2005/8/3
 */
public class ReflectiveConvertletTable
    implements SqlRexConvertletTable
{
    //~ Instance fields --------------------------------------------------------

    private final Map<Object, Object> map = new HashMap<Object, Object>();

    //~ Constructors -----------------------------------------------------------

    public ReflectiveConvertletTable()
    {
        final Class<? extends Object> clazz = getClass();
        final Method [] methods = clazz.getMethods();
        for (int i = 0; i < methods.length; i++) {
            final Method method = methods[i];
            registerNodeTypeMethod(method);
            registerOpTypeMethod(method);
        }
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Registers method if it: a. is public, and b. is named "convertXxx", and
     * c. has a return type of "RexNode" or a subtype d. has a 2 parameters with
     * types ConvertletContext and SqlNode (or a subtype) respectively.
     */
    private void registerNodeTypeMethod(final Method method)
    {
        if (!Modifier.isPublic(method.getModifiers())) {
            return;
        }
        if (!method.getName().startsWith("convert")) {
            return;
        }
        if (!RexNode.class.isAssignableFrom(method.getReturnType())) {
            return;
        }
        final Class [] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 2) {
            return;
        }
        if (parameterTypes[0] != SqlRexContext.class) {
            return;
        }
        final Class parameterType = parameterTypes[1];
        if (!SqlNode.class.isAssignableFrom(parameterType)) {
            return;
        }
        map.put(
            parameterType,
            new SqlRexConvertlet() {
                public RexNode convertCall(
                    SqlRexContext cx,
                    SqlCall call)
                {
                    try {
                        return (RexNode) method.invoke(
                            ReflectiveConvertletTable.this,
                            cx,
                            call);
                    } catch (IllegalAccessException e) {
                        throw Util.newInternal(
                            e,
                            "while converting " + call);
                    } catch (InvocationTargetException e) {
                        throw Util.newInternal(
                            e,
                            "while converting " + call);
                    }
                }
            });
    }

    /**
     * Registers method if it: a. is public, and b. is named "convertXxx", and
     * c. has a return type of "RexNode" or a subtype d. has a 3 parameters with
     * types: ConvertletContext; SqlOperator (or a subtype), SqlCall (or a
     * subtype).
     */
    private void registerOpTypeMethod(final Method method)
    {
        if (!Modifier.isPublic(method.getModifiers())) {
            return;
        }
        if (!method.getName().startsWith("convert")) {
            return;
        }
        if (!RexNode.class.isAssignableFrom(method.getReturnType())) {
            return;
        }
        final Class [] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 3) {
            return;
        }
        if (parameterTypes[0] != SqlRexContext.class) {
            return;
        }
        final Class opClass = parameterTypes[1];
        if (!SqlOperator.class.isAssignableFrom(opClass)) {
            return;
        }
        final Class parameterType = parameterTypes[2];
        if (!SqlCall.class.isAssignableFrom(parameterType)) {
            return;
        }
        map.put(
            opClass,
            new SqlRexConvertlet() {
                public RexNode convertCall(
                    SqlRexContext cx,
                    SqlCall call)
                {
                    try {
                        return (RexNode) method.invoke(
                            ReflectiveConvertletTable.this,
                            cx,
                            call.getOperator(),
                            call);
                    } catch (IllegalAccessException e) {
                        throw Util.newInternal(
                            e,
                            "while converting " + call);
                    } catch (InvocationTargetException e) {
                        throw Util.newInternal(
                            e,
                            "while converting " + call);
                    }
                }
            });
    }

    public SqlRexConvertlet get(SqlCall call)
    {
        SqlRexConvertlet convertlet;
        final SqlOperator op = call.getOperator();

        // Is there a convertlet for this operator
        // (e.g. SqlStdOperatorTable.plusOperator)?
        convertlet = (SqlRexConvertlet) map.get(op);
        if (convertlet != null) {
            return convertlet;
        }

        // Is there a convertlet for this class of operator
        // (e.g. SqlBinaryOperator)?
        Class<? extends Object> clazz = op.getClass();
        while (clazz != null) {
            convertlet = (SqlRexConvertlet) map.get(clazz);
            if (convertlet != null) {
                return convertlet;
            }
            clazz = clazz.getSuperclass();
        }

        // Is there a convertlet for this class of expression
        // (e.g. SqlCall)?
        clazz = call.getClass();
        while (clazz != null) {
            convertlet = (SqlRexConvertlet) map.get(clazz);
            if (convertlet != null) {
                return convertlet;
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    /**
     * Registers a convertlet for a given operator instance
     *
     * @param op Operator instance, say {@link
     * org.eigenbase.sql.fun.SqlStdOperatorTable#minusOperator}
     * @param convertlet Convertlet
     */
    protected void registerOp(SqlOperator op, SqlRexConvertlet convertlet)
    {
        map.put(op, convertlet);
    }

    /**
     * Registers that one operator is an alias for another.
     *
     * @param alias Operator which is alias
     * @param target Operator to translate calls to
     */
    protected void addAlias(final SqlOperator alias, final SqlOperator target)
    {
        map.put(
            alias,
            new SqlRexConvertlet() {
                public RexNode convertCall(
                    SqlRexContext cx,
                    SqlCall call)
                {
                    Util.permAssert(
                        call.getOperator() == alias,
                        "call to wrong operator");
                    final SqlCall newCall =
                        target.createCall(
                            SqlParserPos.ZERO,
                            call.getOperands());
                    return cx.convertExpression(newCall);
                }
            });
    }
}

// End ReflectiveConvertletTable.java
