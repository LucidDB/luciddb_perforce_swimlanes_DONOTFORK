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
package org.eigenbase.rel;

import org.eigenbase.relopt.*;
import org.eigenbase.reltype.*;
import org.eigenbase.sql.*;


/**
 * A relational expression which unnests its input's sole column into a
 * relation.
 *
 * <p>Like its inverse operation {@link CollectRel}, UncollectRel is generally
 * invoked in a nested loop, driven by {@link CorrelatorRel} or similar.
 *
 * @author Wael Chatila
 * @version $Id$
 * @since Dec 12, 2004
 */
public final class UncollectRel
    extends SingleRel
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates an UncollectRel.
     *
     * <p>The row type of the child relational expression must contain precisely
     * one column, that column must be a multiset of records.
     *
     * @param cluster Cluster the relational expression belongs to
     * @param child Child relational expression
     */
    public UncollectRel(
        RelOptCluster cluster,
        RelNode child)
    {
        super(
            cluster,
            CallingConvention.NONE.singletonSet,
            child);
        assert deriveRowType() != null : "invalid child rowtype";
    }

    //~ Methods ----------------------------------------------------------------

    // override Object (public, does not throw CloneNotSupportedException)
    public UncollectRel clone()
    {
        UncollectRel clone =
            new UncollectRel(
                getCluster(),
                getChild().clone());
        clone.inheritTraitsFrom(this);
        return clone;
    }

    protected RelDataType deriveRowType()
    {
        return deriveUncollectRowType(getChild());
    }

    /**
     * Returns the row type returned by applying the 'UNNEST' operation to a
     * relational expression. The relational expression must have precisely one
     * column, whose type must be a multiset of structs. The return type is the
     * type of that column.
     */
    public static RelDataType deriveUncollectRowType(RelNode rel)
    {
        RelDataType inputType = rel.getRowType();
        assert inputType.isStruct() : inputType + " is not a struct";
        final RelDataTypeField [] fields = inputType.getFields();
        assert 1 == fields.length : "expected 1 field";
        RelDataType ret = fields[0].getType().getComponentType();
        assert null != ret;
        if (!ret.isStruct()) {
            // Element type is not a record. It may be a scalar type, say
            // "INTEGER". Wrap it in a struct type.
            ret =
                rel.getCluster().getTypeFactory().createStructType(
                    new RelDataType[] { ret },
                    new String[] { SqlUtil.deriveAliasFromOrdinal(0) });
        }
        return ret;
    }
}

// End UncollectRel.java
