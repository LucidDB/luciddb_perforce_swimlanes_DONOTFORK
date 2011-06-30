/*
// Saffron preprocessor and data engine.
// Copyright (C) 2002-2004 Disruptive Tech
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

package net.sf.saffron.ext;

import java.util.Collections;

import org.eigenbase.rel.RelNode;
import org.eigenbase.relopt.*;
import org.eigenbase.relopt.RelOptConnection;
import org.eigenbase.relopt.RelOptSchema;
import org.eigenbase.reltype.*;


/**
 * <code>ExtentTable</code> is a relational expression formed by all of the
 * instances of a given class. It is transformed into an {@link ExtentRel}.
 */
public class ExtentTable extends RelOptAbstractTable
{
    public ExtentTable(
        RelOptSchema schema,
        String name,
        RelDataType rowType)
    {
        super(schema, name, rowType, Collections.<RelDataTypeField>emptyList());
    }

    public RelNode toRel(
        RelOptCluster cluster,
        RelOptConnection schemaExp)
    {
        // ignore schemaExp -- we give the same results, regardless of
        // which 'connection' we have
        return new ExtentRel(
            cluster,
            getRowType(),
            this);
    }
}


// End ExtentTable.java
