/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2006 The Eigenbase Project
// Copyright (C) 2006 SQLstream, Inc.
// Copyright (C) 2006 Dynamo BI Corporation
// Portions Copyright (C) 2006 John V. Sichi
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
package org.eigenbase.rex;

import org.eigenbase.util.*;


/**
 * Shuttle which applies a permutation to its input fields.
 *
 * @author jhyde
 * @version $Id$
 * @see RexPermutationShuttle
 */
public class RexPermuteInputsShuttle
    extends RexShuttle
{
    //~ Instance fields --------------------------------------------------------

    private final Permutation permutation;

    //~ Constructors -----------------------------------------------------------

    public RexPermuteInputsShuttle(Permutation permutation)
    {
        this.permutation = permutation;
    }

    //~ Methods ----------------------------------------------------------------

    public RexNode visitInputRef(RexInputRef local)
    {
        final int index = local.getIndex();
        int target = permutation.getTarget(index);
        return new RexInputRef(
            target,
            local.getType());
    }
}

// End RexPermuteInputsShuttle.java
