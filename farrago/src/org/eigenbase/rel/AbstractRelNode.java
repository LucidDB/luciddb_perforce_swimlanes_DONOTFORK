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

import java.io.*;

import java.util.*;
import java.util.logging.*;

import org.eigenbase.rel.metadata.*;
import org.eigenbase.relopt.*;
import org.eigenbase.reltype.*;
import org.eigenbase.rex.*;
import org.eigenbase.sql.*;
import org.eigenbase.trace.*;
import org.eigenbase.util.*;


/**
 * Base class for every relational expression ({@link RelNode}).
 */
public abstract class AbstractRelNode
    implements RelNode
{
    //~ Static fields/initializers ---------------------------------------------

    // TODO jvs 10-Oct-2003:  Make this thread safe.  Either synchronize, or
    // keep this per-VolcanoPlanner.

    /**
     * generator for {@link #id} values
     */
    static int nextId = 0;
    private static final Logger tracer = EigenbaseTrace.getPlannerTracer();

    //~ Instance fields --------------------------------------------------------

    /**
     * Description, consists of id plus digest.
     */
    private String desc;

    /**
     * Cached type of this relational expression.
     */
    protected RelDataType rowType;

    /**
     * A short description of this relational expression's type, inputs, and
     * other properties. The string uniquely identifies the node; another node
     * is equivalent if and only if it has the same value. Computed by {@link
     * #computeDigest}, assigned by {@link #onRegister}, returned by {@link
     * #getDigest()}.
     *
     * @see #desc
     */
    protected String digest;

    private RelOptCluster cluster;

    /**
     * unique id of this object -- for debugging
     */
    protected int id;

    /**
     * The variable by which to refer to rows from this relational expression,
     * as correlating expressions; null if this expression is not correlated on.
     */
    private String correlVariable;

    /**
     * The RelTraitSet that describes the traits of this RelNode.
     */
    protected RelTraitSet traits;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a <code>AbstractRelNode</code>.
     *
     * @pre cluster != null
     */
    public AbstractRelNode(RelOptCluster cluster, RelTraitSet traits)
    {
        super();
        assert (cluster != null);
        this.cluster = cluster;
        this.traits = traits;
        this.id = nextId++;
        this.digest = getRelTypeName() + "#" + id;
        this.desc = digest;
        tracer.finest("new " + digest);
    }

    //~ Methods ----------------------------------------------------------------

    public abstract RelNode clone();

    public RelNode copy(
        RelNode... inputs)
    {
        assert inputs.length == getInputs().length;
        final AbstractRelNode copy = (AbstractRelNode) clone();
        assert copy.getClass() == getClass()
            : "clone of a " + getClass() + " yielded a "
              + copy.getClass();
        for (int i = 0; i < inputs.length; i++) {
            // replaceInput may fail. It is not a required method. If so,
            // override cloneSubstitutingInputs.
            copy.replaceInput(i, inputs[i]);
        }
        return copy;
    }

    public boolean isAccessTo(RelOptTable table)
    {
        return getTable() == table;
    }

    public RexNode [] getChildExps()
    {
        return RexUtil.emptyExpressionArray;
    }

    public RelOptCluster getCluster()
    {
        return cluster;
    }

    public final CallingConvention getConvention()
    {
        return (CallingConvention) traits.getTrait(
            CallingConventionTraitDef.instance);
    }

    public RelTraitSet getTraits()
    {
        return traits;
    }

    /**
     * Sets this relational expression's traits to the same as another
     * relational expression. The other relational expression must be the same
     * type as this.
     *
     * <p>The typical use of this method is in the implementation of a {@link
     * #clone()} method:
     *
     * <blockquote><code>class MyRel {<br/>
     *   public MyRel clone() {<br/>
     *     return new MyRel(<br/>
     *       getCluster(), getChild().clone(),<br/>
     *       fieldX.clone(), fieldY.clone()).inheritTraitsFrom(this);<br/>
     *   }<br/>
     * }</code></blockquote>
     *
     * <p>To enable calls to be chained in this way, this method returns <code>
     * this</code> as a convenience.
     *
     * @param rel Relational expression whose traits to copy
     *
     * @return This relational expression
     */
    @SuppressWarnings({ "unchecked" })
    public <T extends AbstractRelNode> T inheritTraitsFrom(T rel)
    {
        traits = rel.getTraits();
        return (T) this;
    }

    @SuppressWarnings({ "unchecked" })
    public <T extends AbstractRelNode> T inheritTraitsFromExcept(
        RelNode rel,
        RelTraitDef traitDef)
    {
        traits =
            traits.plusAllExcept(
                rel.getTraits(), traitDef);
        return (T) this;
    }

    public void setTraits(RelTraitSet traitSet)
    {
        this.traits = traitSet;
    }

    public void setCorrelVariable(String correlVariable)
    {
        this.correlVariable = correlVariable;
    }

    public String getCorrelVariable()
    {
        return correlVariable;
    }

    public boolean isDistinct()
    {
        return false;
    }

    public int getId()
    {
        return id;
    }

    public RelNode getInput(int i)
    {
        RelNode [] inputs = getInputs();
        return inputs[i];
    }

    public String getOrCreateCorrelVariable()
    {
        if (correlVariable == null) {
            correlVariable = getQuery().createCorrel();
            getQuery().mapCorrel(correlVariable, this);
        }
        return correlVariable;
    }

    public RelOptQuery getQuery()
    {
        return cluster.getQuery();
    }

    /**
     * Registers any special rules specific to this kind of relational
     * expression.
     *
     * <p>The planner calls this method this first time that it sees a
     * relational expression of this class. The derived class should call {@link
     * RelOptPlanner#addRule} for each rule, and then call {@link #register} on
     * its base class.</p>
     */
    public static void register(RelOptPlanner planner)
    {
        Util.discard(planner);
    }

    public final String getRelTypeName()
    {
        String className = getClass().getName();
        int i = className.lastIndexOf("$");
        if (i >= 0) {
            return className.substring(i + 1);
        }
        i = className.lastIndexOf(".");
        if (i >= 0) {
            return className.substring(i + 1);
        }
        return className;
    }

    public boolean isValid(boolean fail)
    {
        return getTraits().isValid(fail);
    }

    public List<RelCollation> getCollationList()
    {
        return Collections.emptyList();
    }

    public final RelDataType getRowType()
    {
        if (rowType == null) {
            rowType = deriveRowType();
            assert rowType != null : this;
        }
        return rowType;
    }

    protected RelDataType deriveRowType()
    {
        // This method is only called if rowType is null, so you don't NEED to
        // implement it if rowType is always set.
        throw new UnsupportedOperationException();
    }

    public RelDataType getExpectedInputRowType(int ordinalInParent)
    {
        return getRowType();
    }

    public RelNode [] getInputs()
    {
        return emptyArray;
    }

    public double getRows()
    {
        return 1.0;
    }

    public Set<String> getVariablesStopped()
    {
        return Collections.emptySet();
    }

    public void collectVariablesUsed(Set<String> variableSet)
    {
        // for default case, nothing to do
    }

    public void collectVariablesSet(Set<String> variableSet)
    {
        if (correlVariable != null) {
            variableSet.add(correlVariable);
        }
    }

    public void childrenAccept(RelVisitor visitor)
    {
        RelNode [] inputs = getInputs();
        for (int i = 0; i < inputs.length; i++) {
            visitor.visit(inputs[i], i, this);
        }
    }

    public RelOptCost computeSelfCost(RelOptPlanner planner)
    {
        // by default, assume cost is proportional to number of rows
        double rowCount = RelMetadataQuery.getRowCount(this);
        double bytesPerRow = 1;
        return planner.makeCost(
            rowCount,
            rowCount,
            0);
    }

    public void explain(RelOptPlanWriter pw)
    {
        pw.explain(this, Util.emptyStringArray, Util.emptyObjectArray);
    }

    public void onRegister(RelOptPlanner planner, RelOptRuleCall call)
    {
        RelNode [] inputs = getInputs();
        for (int i = 0; i < inputs.length; i++) {
            final RelNode input = inputs[i];
            RelNode e = planner.ensureRegistered(input, null, call);
            if (e != input) {
                // TODO: change 'equal' to 'eq', which is stronger.
                assert RelOptUtil.equal(
                    "rowtype of rel before registration",
                    input.getRowType(),
                    "rowtype of rel after registration",
                    e.getRowType(),
                    true);
                replaceInput(i, e);
            }
        }
        assert isValid(true);
        recomputeDigest();
    }

    public String recomputeDigest()
    {
        String tempDigest = computeDigest();
        assert tempDigest != null : "post: return != null";
        String prefix = "rel#" + id + ":";

        // Substring uses the same underlying array of chars, so saves a bit
        // of memory.
        this.desc = prefix + tempDigest;
        this.digest = this.desc.substring(prefix.length());
        return this.digest;
    }

    public void registerCorrelVariable(String correlVariable)
    {
        assert (this.correlVariable == null);
        this.correlVariable = correlVariable;
        getQuery().mapCorrel(correlVariable, this);
    }

    public void replaceInput(
        int ordinalInParent,
        RelNode p)
    {
        throw Util.newInternal("replaceInput called on " + this);
    }

    public String toString()
    {
        return desc;
    }

    public final String getDescription()
    {
        return desc;
    }

    public final String getDigest()
    {
        return digest;
    }

    public RelOptTable getTable()
    {
        return null;
    }

    /**
     * Computes the digest. Does not modify this object.
     *
     * @post return != null
     */
    protected String computeDigest()
    {
        StringWriter sw = new StringWriter();
        RelOptPlanWriter pw =
            new RelOptPlanWriter(
                new PrintWriter(sw),
                SqlExplainLevel.DIGEST_ATTRIBUTES)
            {
                public void explain(
                    RelNode rel,
                    String [] terms,
                    Object [] values)
                {
                    RelNode [] inputs = rel.getInputs();
                    RexNode [] childExps = rel.getChildExps();
                    assert terms.length
                        == (inputs.length + childExps.length + values.length)
                        : "terms.length="
                        + terms.length
                        + " inputs.length=" + inputs.length
                        + " childExps.length=" + childExps.length
                        + " values.length=" + values.length;
                    write(getRelTypeName());

                    for (int i = 0; i < traits.size(); i++) {
                        write(".");
                        write(traits.getTrait(i).toString());
                    }

                    write("(");
                    int j = 0;
                    for (int i = 0; i < inputs.length; i++) {
                        if (j > 0) {
                            write(",");
                        }
                        write(terms[j++] + "=" + inputs[i].getDigest());
                    }
                    for (int i = 0; i < childExps.length; i++) {
                        if (j > 0) {
                            write(",");
                        }
                        RexNode childExp = childExps[i];
                        write(terms[j++] + "=" + childExp.toString());
                    }
                    for (int i = 0; i < values.length; i++) {
                        Object value = values[i];
                        if (j > 0) {
                            write(",");
                        }
                        write(terms[j++] + "=" + value);
                    }
                    write(")");
                }
            };
        explain(pw);
        pw.flush();
        return sw.toString();
    }

    /**
     * Returns a list of system fields that will be prefixed to the output row
     * type.
     *
     * <p>Never returns null. The default implementation returns the empty
     * list.</p>
     *
     * @return list of system fields
     */
    public List<RelDataTypeField> getSystemFieldList()
    {
        return Collections.emptyList();
    }
}

// End AbstractRelNode.java
