/*
// $Id$
// Saffron preprocessor and data engine
// (C) Copyright 2003-2003 Disruptive Technologies, Inc.
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
package org.eigenbase.rex;

import org.eigenbase.rex.RexVariable;
import org.eigenbase.reltype.RelDataType;

/**
 * Reference to the current row of a correlating relational expression.
 *
 * <p>Correlating variables are introduced when performing nested loop joins.
 * Each row is received from one side of the join, a correlating variable is
 * assigned a value, and the other side of the join is restarted.</p>
 *
 * @author jhyde
 * @since Nov 24, 2003
 * @version $Id$
 */
public class RexCorrelVariable extends RexVariable {

    RexCorrelVariable(String varName, RelDataType type) {
        super(varName, type);
    }

    public Object clone() {
        return new RexCorrelVariable(name,type);
    }

    public void accept(RexVisitor visitor) {
        visitor.visitCorrelVariable(this);
    }
}

// End RexCorrelVariable.java
