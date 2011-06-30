/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2007 The Eigenbase Project
// Copyright (C) 2007 SQLstream, Inc.
// Copyright (C) 2007 Dynamo BI Corporation
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
package org.eigenbase.sql.validate;

import java.util.*;

import org.eigenbase.reltype.*;
import org.eigenbase.sql.*;
import org.eigenbase.util.*;


/**
 * An implementation of {@link SqlValidatorNamespace} that delegates all methods
 * to an underlying object.
 *
 * @author jhyde
 * @version $Id$
 */
public abstract class DelegatingNamespace
    implements SqlValidatorNamespace
{
    //~ Instance fields --------------------------------------------------------

    protected final SqlValidatorNamespace namespace;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a DelegatingNamespace.
     *
     * @param namespace Underlying namespace, to delegate to
     */
    protected DelegatingNamespace(SqlValidatorNamespace namespace)
    {
        this.namespace = namespace;
    }

    //~ Methods ----------------------------------------------------------------

    public SqlValidator getValidator()
    {
        return namespace.getValidator();
    }

    public SqlValidatorTable getTable()
    {
        return namespace.getTable();
    }

    public RelDataType getRowType()
    {
        return namespace.getRowType();
    }

    public void setRowType(RelDataType rowType, RelDataType rowTypeAsWritten)
    {
        namespace.setRowType(rowType, rowTypeAsWritten);
    }

    public RelDataType getRowTypeAsWritten()
    {
        return namespace.getRowTypeAsWritten();
    }

    public void validate()
    {
        namespace.validate();
    }

    public SqlNode getNode()
    {
        return namespace.getNode();
    }

    public SqlNode getEnclosingNode()
    {
        return namespace.getEnclosingNode();
    }

    public SqlValidatorNamespace lookupChild(
        String name)
    {
        return namespace.lookupChild(name);
    }

    public boolean fieldExists(String name)
    {
        return namespace.fieldExists(name);
    }

    public List<Pair<SqlNode, SqlMonotonicity>> getMonotonicExprs()
    {
        return namespace.getMonotonicExprs();
    }

    public SqlMonotonicity getMonotonicity(String columnName)
    {
        return namespace.getMonotonicity(columnName);
    }

    public void makeNullable()
    {
        namespace.makeNullable();
    }

    public String translate(String name)
    {
        return namespace.translate(name);
    }

    public <T> T unwrap(Class<T> clazz)
    {
        if (clazz.isInstance(this)) {
            return clazz.cast(this);
        } else {
            return namespace.unwrap(clazz);
        }
    }

    public boolean isWrapperFor(Class<?> clazz)
    {
        return clazz.isInstance(this)
            || namespace.isWrapperFor(clazz);
    }
}

// End DelegatingNamespace.java
