/*
// $Id$
// Farrago is an extensible data management system.
// Copyright (C) 2009 The Eigenbase Project
// Copyright (C) 2009 SQLstream, Inc.
// Copyright (C) 2009 Dynamo BI Corporation
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
package net.sf.farrago.namespace.mql;

import org.eigenbase.rel.*;
import org.eigenbase.relopt.*;

/**
 * MedMqlTableRel represents a logical access to an MQL foreign table
 * before any pushdown.
 *
 * @author John Sichi
 * @version $Id$
 */
class MedMqlTableRel extends TableAccessRelBase
{
    public MedMqlTableRel(
        RelOptCluster cluster,
        RelOptTable table,
        RelOptConnection connection)
    {
        super(
            cluster,
            CallingConvention.NONE.singletonSet,
            table,
            connection);
    }

    MedMqlColumnSet getMedMqlColumnSet()
    {
        return (MedMqlColumnSet) getTable();
    }
}

// End MedMqlTableRel.java
