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

package org.eigenbase.rel;

import org.eigenbase.relopt.*;
import org.eigenbase.reltype.*;
import org.eigenbase.util.Util;


/**
 * A <code>SingleRel</code> is a base class single-input relational
 * expressions.
 *
 * @author jhyde
 * @version $Id$
 *
 * @since 23 September, 2001
 */
public abstract class SingleRel extends AbstractRelNode
{
    //~ Instance fields -------------------------------------------------------

    public RelNode child;

    //~ Constructors ----------------------------------------------------------

    /**
     * Creates a <code>SingleRel</code>.
     *
     * @param cluster {@link RelOptCluster} this relational expression
     *        belongs to
     * @param child input relational expression
     */
    protected SingleRel(RelOptCluster cluster,RelNode child)
    {
        super(cluster);
        this.child = child;
    }

    //~ Methods ---------------------------------------------------------------

    // implement RelNode
    public RelNode [] getInputs()
    {
        return new RelNode [] { child };
    }

    public double getRows()
    {
        // Not necessarily correct, but a better default than Rel's 1.0
        return child.getRows();
    }

    public void childrenAccept(RelVisitor visitor)
    {
        visitor.visit(child,0,this);
    }

    public void explain(RelOptPlanWriter pw)
    {
        pw.explain(this,new String [] { "child" },Util.emptyObjectArray);
    }

    // override Rel
    public void replaceInput(int ordinalInParent,RelNode rel)
    {
        assert(ordinalInParent == 0);
        this.child = rel;
    }

    protected RelDataType deriveRowType()
    {
        return child.getRowType();
    }
}


// End SingleRel.java
