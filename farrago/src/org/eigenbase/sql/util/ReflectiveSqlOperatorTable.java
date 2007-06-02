/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2005-2007 The Eigenbase Project
// Copyright (C) 2004-2007 Disruptive Tech
// Copyright (C) 2005-2007 LucidEra, Inc.
// Portions Copyright (C) 2004-2007 John V. Sichi
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
package org.eigenbase.sql.util;

import java.lang.reflect.*;

import java.util.*;

import org.eigenbase.sql.*;
import org.eigenbase.util.*;


/**
 * ReflectiveSqlOperatorTable implements the {@link SqlOperatorTable } interface
 * by reflecting the public fields of a subclass.
 *
 * @author John V. Sichi
 * @version $Id$
 */
public abstract class ReflectiveSqlOperatorTable
    implements SqlOperatorTable
{
    //~ Instance fields --------------------------------------------------------

    private final MultiMap<String, SqlOperator> operators =
        new MultiMap<String, SqlOperator>();

    private final Map<String, SqlOperator> mapNameToOp =
        new HashMap<String, SqlOperator>();

    //~ Constructors -----------------------------------------------------------

    protected ReflectiveSqlOperatorTable()
    {
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Performs post-constructor initialization of an operator table. It can't
     * be part of the constructor, because the subclass constructor needs to
     * complete first.
     */
    public final void init()
    {
        // Use reflection to register the expressions stored in public fields.
        Field [] fields = getClass().getFields();
        for (int i = 0; i < fields.length; i++) {
            try {
                Field field = fields[i];
                if (SqlFunction.class.isAssignableFrom(field.getType())) {
                    SqlFunction op = (SqlFunction) field.get(this);
                    if (op != null) {
                        register(op);
                    }
                } else if (
                    SqlOperator.class.isAssignableFrom(field.getType()))
                {
                    SqlOperator op = (SqlOperator) field.get(this);
                    register(op);
                }
            } catch (IllegalArgumentException e) {
                throw Util.newInternal(
                    e,
                    "Error while initializing operator table");
            } catch (IllegalAccessException e) {
                throw Util.newInternal(
                    e,
                    "Error while initializing operator table");
            }
        }
    }

    // implement SqlOperatorTable
    public List<SqlOperator> lookupOperatorOverloads(
        SqlIdentifier opName,
        SqlFunctionCategory category,
        SqlSyntax syntax)
    {
        // NOTE jvs 3-Mar-2005:  ignore category until someone cares

        List<SqlOperator> overloads = new ArrayList<SqlOperator>();
        String simpleName;
        if (opName.names.length > 1) {
            if (opName.names[opName.names.length - 2].equals(
                    "INFORMATION_SCHEMA"))
            {
                // per SQL99 Part 2 Section 10.4 Syntax Rule 7.b.ii.1
                simpleName = opName.names[opName.names.length - 1];
            } else {
                return overloads;
            }
        } else {
            simpleName = opName.getSimple();
        }
        final List<SqlOperator> list = operators.getMulti(simpleName);
        for (int i = 0, n = list.size(); i < n; i++) {
            SqlOperator op = list.get(i);
            if (op.getSyntax() == syntax) {
                overloads.add(op);
            } else if (
                (syntax == SqlSyntax.Function)
                && (op instanceof SqlFunction))
            {
                // this special case is needed for operators like CAST,
                // which are treated as functions but have special syntax
                overloads.add(op);
            }
        }

        // REVIEW jvs 1-Jan-2005:  why is this extra lookup required?
        // Shouldn't it be covered by search above?
        SqlOperator extra = null;
        switch (syntax) {
        case Binary:
            extra = mapNameToOp.get(simpleName + ":BINARY");
        case Prefix:
            extra = mapNameToOp.get(simpleName + ":PREFIX");
        case Postfix:
            extra = mapNameToOp.get(simpleName + ":POSTFIX");
        default:
            break;
        }

        if ((extra != null) && !overloads.contains(extra)) {
            overloads.add(extra);
        }

        return overloads;
    }

    public void register(SqlOperator op)
    {
        operators.putMulti(
            op.getName(),
            op);
        if (op instanceof SqlBinaryOperator) {
            mapNameToOp.put(op.getName() + ":BINARY", op);
        } else if (op instanceof SqlPrefixOperator) {
            mapNameToOp.put(op.getName() + ":PREFIX", op);
        } else if (op instanceof SqlPostfixOperator) {
            mapNameToOp.put(op.getName() + ":POSTFIX", op);
        }
    }

    /**
     * Registers a function in the table.
     *
     * @param function Function to register
     */
    public void register(SqlFunction function)
    {
        operators.putMulti(
            function.getName(),
            function);
        SqlFunctionCategory funcType = function.getFunctionType();
        assert (funcType != null) : "Function type for " + function.getName()
            + " not set";
    }

    // implement SqlOperatorTable
    public List<SqlOperator> getOperatorList()
    {
        List<SqlOperator> list = new ArrayList<SqlOperator>();

        Iterator<Map.Entry<String, SqlOperator>> it =
            operators.entryIterMulti();
        while (it.hasNext()) {
            list.add(it.next().getValue());
        }

        return list;
    }
}

// End ReflectiveSqlOperatorTable.java
