/*
// $Id$
// Farrago is an extensible data management system.
// Copyright (C) 2005 The Eigenbase Project
// Copyright (C) 2005 SQLstream, Inc.
// Copyright (C) 2005 Dynamo BI Corporation
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

import java.util.*;

import net.sf.farrago.namespace.*;
import net.sf.farrago.namespace.impl.*;

import org.eigenbase.rel.*;
import org.eigenbase.relopt.*;
import org.eigenbase.reltype.*;


/**
 * MedMqlColumnSet provides an implementation of the {@link
 * FarragoMedColumnSet} interface for MQL.
 *
 * @author John V. Sichi
 * @version $Id$
 */
class MedMqlColumnSet
    extends MedAbstractColumnSet
{
    //~ Instance fields --------------------------------------------------------

    final MedMqlDataServer server;
    final String metawebType;
    final String udxSpecificName;

    //~ Constructors -----------------------------------------------------------

    MedMqlColumnSet(
        MedMqlDataServer server,
        String [] localName,
        RelDataType rowType,
        String metawebType,
        String udxSpecificName)
    {
        super(
            localName, null, rowType, Collections.<RelDataTypeField>emptyList(),
            null, null);
        this.server = server;
        this.udxSpecificName = udxSpecificName;
        this.metawebType = metawebType;
    }

    //~ Methods ----------------------------------------------------------------

    // implement RelOptTable
    public RelNode toRel(
        RelOptCluster cluster,
        RelOptConnection connection)
    {
        return new MedMqlTableRel(
            cluster, this, connection);
    }
}

// End MedMqlColumnSet.java
