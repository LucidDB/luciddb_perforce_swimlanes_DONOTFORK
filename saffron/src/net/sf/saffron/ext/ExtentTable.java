/*
// $Id$
// Saffron preprocessor and data engine
// (C) Copyright 2002-2003 Disruptive Technologies, Inc.
// (C) Copyright 2003-2004 John V. Sichi
// You must accept the terms in LICENSE.html to use this software.
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public License
// as published by the Free Software Foundation; either version 2.1
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/

package net.sf.saffron.ext;

import org.eigenbase.relopt.RelOptConnection;
import org.eigenbase.relopt.RelOptSchema;
import org.eigenbase.reltype.RelDataType;
import org.eigenbase.relopt.*;
import org.eigenbase.rel.RelNode;


/**
 * <code>ExtentTable</code> is a relational expression formed by all of the
 * instances of a given class. It is transformed into an {@link ExtentRel}.
 */
public class ExtentTable extends RelOptAbstractTable
{
    //~ Constructors ----------------------------------------------------------

    public ExtentTable(RelOptSchema schema,String name,RelDataType rowType)
    {
        super(schema,name,rowType);
    }

    //~ Methods ---------------------------------------------------------------

    public RelNode toRel(RelOptCluster cluster,RelOptConnection schemaExp)
    {
        // ignore schemaExp -- we give the same results, regardless of
        // which 'connection' we have
        return new ExtentRel(cluster,getRowType(),this);
    }
}


// End ExtentTable.java
