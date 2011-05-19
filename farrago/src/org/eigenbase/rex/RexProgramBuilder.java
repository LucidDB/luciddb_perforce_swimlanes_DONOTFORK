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
package org.eigenbase.rex;

import java.util.*;

import org.eigenbase.relopt.*;
import org.eigenbase.reltype.*;
import org.eigenbase.sql.fun.*;
import org.eigenbase.util.*;

/**
 * Workspace for constructing a {@link RexProgram}.
 *
 * <p>RexProgramBuilder is necessary because a {@link RexProgram} is immutable.
 * (The {@link String} class has the same problem: it is immutable, so they
 * introduced {@link StringBuffer}.)
 *
 * @author jhyde
 * @version $Id$
 * @since Aug 18, 2005
 */
public class RexProgramBuilder
{
    //~ Instance fields --------------------------------------------------------

    private final RexBuilder rexBuilder;
    private final RelDataType inputRowType;
    private final List<RexNode> exprList = new ArrayList<RexNode>();
    private final Map<String, RexLocalRef> exprMap =
        new HashMap<String, RexLocalRef>();
    private final List<RexLocalRef> localRefList = new ArrayList<RexLocalRef>();
    private final List<RexLocalRef> projectRefList =
        new ArrayList<RexLocalRef>();
    private final List<String> projectNameList = new ArrayList<String>();
    private RexLocalRef conditionRef = null;
    private boolean validating;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a program-builder.
     */
    public RexProgramBuilder(RelDataType inputRowType, RexBuilder rexBuilder)
    {
        assert inputRowType != null;
        assert rexBuilder != null;
        this.inputRowType = inputRowType;
        this.rexBuilder = rexBuilder;
        this.validating = assertionsAreEnabled();

        // Pre-create an expression for each input field.
        if (inputRowType.isStruct()) {
            final RelDataTypeField [] fields = inputRowType.getFields();
            for (int i = 0; i < fields.length; i++) {
                RelDataTypeField field = fields[i];
                registerInternal(new RexInputRef(
                        i,
                        field.getType()),
                    false);
            }
        }
    }

    /**
     * Creates a program builder with the same contents as a program.
     *
     * @param rexBuilder Rex builder
     * @param inputRowType Input row type
     * @param exprList Common expressions
     * @param projectRefList Projections
     * @param conditionRef Condition, or null
     * @param outputRowType Output row type
     * @param normalize Whether to normalize
     */
    private RexProgramBuilder(
        RexBuilder rexBuilder,
        final RelDataType inputRowType,
        final List<RexNode> exprList,
        final List<RexLocalRef> projectRefList,
        final RexLocalRef conditionRef,
        final RelDataType outputRowType,
        boolean normalize)
    {
        this(inputRowType, rexBuilder);

        // Create a shuttle for registering input expressions.
        final RexShuttle shuttle =
            new RegisterMidputShuttle(true, exprList);

        // If we are not normalizing, register all internal expressions. If we
        // are normalizing, expressions will be registered if and when they are
        // first used.
        if (!normalize) {
            for (RexNode expr : exprList) {
                expr.accept(shuttle);
            }
        }

        // Register project expressions
        // and create a named project item.
        int i = 0;
        for (RexLocalRef projectRef : projectRefList) {
            final String name = outputRowType.getFieldList().get(i++).getName();
            final int oldIndex = projectRef.getIndex();
            final RexNode expr = exprList.get(oldIndex);
            final RexLocalRef ref = (RexLocalRef) expr.accept(shuttle);
            addProject(ref.getIndex(), name);
        }

        // Register the condition, if there is one.
        if (conditionRef != null) {
            final RexNode expr = exprList.get(conditionRef.getIndex());
            final RexLocalRef ref = (RexLocalRef) expr.accept(shuttle);
            addCondition(ref);
        }
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Returns whether assertions are enabled in this class.
     */
    private static boolean assertionsAreEnabled()
    {
        boolean assertionsEnabled = false;
        assert assertionsEnabled = true;
        return assertionsEnabled;
    }

    private void validate(final RexNode expr, final int fieldOrdinal)
    {
        final RexVisitor<Void> validator =
            new RexVisitorImpl<Void>(true) {
                public Void visitInputRef(RexInputRef input)
                {
                    final int index = input.getIndex();
                    final RelDataTypeField [] fields = inputRowType.getFields();
                    if (index < fields.length) {
                        final RelDataTypeField inputField = fields[index];
                        if (input.getType() != inputField.getType()) {
                            throw Util.newInternal(
                                "in expression " + expr
                                + ", field reference " + input
                                + " has inconsistent type");
                        }
                    } else {
                        if (index >= fieldOrdinal) {
                            throw Util.newInternal(
                                "in expression " + expr
                                + ", field reference " + input
                                + " is out of bounds");
                        }
                        RexNode refExpr = exprList.get(index);
                        if (refExpr.getType() != input.getType()) {
                            throw Util.newInternal(
                                "in expression " + expr
                                + ", field reference " + input
                                + " has inconsistent type");
                        }
                    }
                    return null;
                }
            };
        expr.accept(validator);
    }

    /**
     * Adds a project expression to the program.
     *
     * <p>The expression specified in terms of the input fields. If not, call
     * {@link #registerOutput(RexNode)} first.
     *
     * @param expr Expression to add
     * @param name Name of field in output row type; if null, a unique name will
     * be generated when the program is created
     *
     * @return the ref created
     */
    public RexLocalRef addProject(RexNode expr, String name)
    {
        final RexLocalRef ref = registerInput(expr);
        return addProject(ref.getIndex(), name);
    }

    /**
     * Adds a projection based upon the <code>index</code>th expression.
     *
     * @param ordinal Index of expression to project
     * @param name Name of field in output row type; if null, a unique name will
     * be generated when the program is created
     *
     * @return the ref created
     */
    public RexLocalRef addProject(int ordinal, final String name)
    {
        final RexLocalRef ref = localRefList.get(ordinal);
        projectRefList.add(ref);
        projectNameList.add(name);
        return ref;
    }

    /**
     * Adds a project expression to the program at a given position.
     *
     * <p>The expression specified in terms of the input fields. If not, call
     * {@link #registerOutput(RexNode)} first.
     *
     * @param at Position in project list to add expression
     * @param expr Expression to add
     * @param name Name of field in output row type; if null, a unique name will
     * be generated when the program is created
     *
     * @return the ref created
     */
    public RexLocalRef addProject(int at, RexNode expr, String name)
    {
        final RexLocalRef ref = registerInput(expr);
        projectRefList.add(at, ref);
        projectNameList.add(at, name);
        return ref;
    }

    /**
     * Adds a projection based upon the <code>index</code>th expression at a
     * given position.
     *
     * @param at Position in project list to add expression
     * @param ordinal Index of expression to project
     * @param name Name of field in output row type; if null, a unique name will
     * be generated when the program is created
     *
     * @return the ref created
     */
    public RexLocalRef addProject(int at, int ordinal, final String name)
    {
        return addProject(
            at,
            localRefList.get(ordinal),
            name);
    }

    /**
     * Sets the condition of the program.
     *
     * <p/>The expression must be specified in terms of the input fields. If
     * not, call {@link #registerOutput(RexNode)} first.
     */
    public void addCondition(RexNode expr)
    {
        assert expr != null;
        if (conditionRef == null) {
            conditionRef = registerInput(expr);
        } else {
            // AND the new condition with the existing condition.
            RexLocalRef ref = registerInput(expr);
            final RexLocalRef andRef =
                registerInput(
                    rexBuilder.makeCall(
                        SqlStdOperatorTable.andOperator,
                        conditionRef,
                        ref));
            conditionRef = andRef;
        }
    }

    /**
     * Registers an expression in the list of common sub-expressions, and
     * returns a reference to that expression.
     *
     * <p/>The expression must be expressed in terms of the <em>inputs</em> of
     * this program.
     */
    public RexLocalRef registerInput(RexNode expr)
    {
        final RexShuttle shuttle = new RegisterInputShuttle(true);
        final RexNode ref = expr.accept(shuttle);
        return (RexLocalRef) ref;
    }

    /**
     * Converts an expression expressed in terms of the <em>outputs</em> of this
     * program into an expression expressed in terms of the <em>inputs</em>,
     * registers it in the list of common sub-expressions, and returns a
     * reference to that expression.
     *
     * @param expr Expression to register
     */
    public RexLocalRef registerOutput(RexNode expr)
    {
        final RexShuttle shuttle = new RegisterOutputShuttle(exprList);
        final RexNode ref = expr.accept(shuttle);
        return (RexLocalRef) ref;
    }

    /**
     * Registers an expression in the list of common sub-expressions, and
     * returns a reference to that expression.
     *
     * <p>If an equivalent sub-expression already exists, creates another
     * expression only if <code>force</code> is true.
     *
     * @param expr Expression to register
     * @param force Whether to create a new sub-expression if an equivalent
     * sub-expression exists.
     */
    private RexLocalRef registerInternal(RexNode expr, boolean force)
    {
        String key = RexUtil.makeKey(expr);
        RexLocalRef ref = exprMap.get(key);
        if ((ref == null) && (expr instanceof RexLocalRef)) {
            ref = (RexLocalRef) expr;
        }
        if (ref == null) {
            if (validating) {
                validate(
                    expr,
                    exprList.size());
            }

            // Add expression to list, and return a new reference to it.
            ref = addExpr(expr);
            exprMap.put(key, ref);
        } else {
            if (force) {
                // Add expression to list, but return the previous ref.
                addExpr(expr);
            }
        }

        while (true) {
            int index = ref.index;
            final RexNode expr2 = exprList.get(index);
            if (expr2 instanceof RexLocalRef) {
                ref = (RexLocalRef) expr2;
            } else {
                return ref;
            }
        }
    }

    /**
     * Adds an expression to the list of common expressions, and returns a
     * reference to the expression. <b>DOES NOT CHECK WHETHER THE EXPRESSION
     * ALREADY EXISTS</b>.
     *
     * @param expr Expression
     *
     * @return Reference to expression
     */
    public RexLocalRef addExpr(RexNode expr)
    {
        RexLocalRef ref;
        final int index = exprList.size();
        exprList.add(expr);
        ref =
            new RexLocalRef(
                index,
                expr.getType());
        localRefList.add(ref);
        return ref;
    }

    /**
     * Converts the state of the program builder to an immutable program,
     * normalizing in the process.
     *
     * <p>It is OK to call this method, modify the program specification (by
     * adding projections, and so forth), and call this method again.
     */
    public RexProgram getProgram()
    {
        return getProgram(true);
    }

    /**
     * Converts the state of the program builder to an immutable program.
     *
     * <p>It is OK to call this method, modify the program specification (by
     * adding projections, and so forth), and call this method again.
     *
     * @param normalize Whether to normalize
     */
    public RexProgram getProgram(boolean normalize)
    {
        assert projectRefList.size() == projectNameList.size();

        // Make sure all fields have a name.
        generateMissingNames();
        RelDataType outputRowType = computeOutputRowType();

        if (normalize) {
            return create(
                rexBuilder,
                inputRowType,
                exprList,
                projectRefList,
                conditionRef,
                outputRowType,
                true)
                .getProgram(false);
        }

        // Clone expressions, so builder can modify them after they have
        // been put into the program. The projects and condition do not need
        // to be cloned, because RexLocalRef is immutable.
        List<RexNode> exprs = new ArrayList<RexNode>(exprList);
        for (int i = 0; i < exprList.size(); i++) {
            exprs.set(i, exprList.get(i).clone());
        }
        return new RexProgram(
            inputRowType,
            exprs,
            projectRefList,
            conditionRef,
            outputRowType);
    }

    private RelDataType computeOutputRowType()
    {
        return rexBuilder.typeFactory.createStructType(
            new RelDataTypeFactory.FieldInfo() {
                public int getFieldCount()
                {
                    return projectRefList.size();
                }

                public String getFieldName(int index)
                {
                    return projectNameList.get(index);
                }

                public RelDataType getFieldType(int index)
                {
                    return projectRefList.get(index).getType();
                }
            });
    }

    private void generateMissingNames()
    {
        int i = -1, j = 0;
        for (String projectName : projectNameList) {
            ++i;
            if (projectName == null) {
                while (true) {
                    final String candidateName = "$" + j++;
                    if (!projectNameList.contains(candidateName)) {
                        projectNameList.set(i, candidateName);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Creates a program builder and initializes it from an existing program.
     *
     * <p>Calling {@link #getProgram()} immediately after creation will return a
     * program equivalent (in terms of external behavior) to the existing
     * program.
     *
     * <p>The existing program will not be changed. (It cannot: programs are
     * immutable.)
     *
     * @param program Existing program
     * @param rexBuilder Rex builder
     * @param normalize Whether to normalize
     * @return A program builder initialized with an equivalent program
     */
    public static RexProgramBuilder forProgram(
        RexProgram program,
        RexBuilder rexBuilder,
        boolean normalize)
    {
        assert program.isValid(true);
        final RelDataType inputRowType = program.getInputRowType();
        final List<RexLocalRef> projectRefs = program.getProjectList();
        final RexLocalRef conditionRef = program.getCondition();
        final List<RexNode> exprs = program.getExprList();
        final RelDataType outputRowType = program.getOutputRowType();
        return create(
            rexBuilder,
            inputRowType,
            exprs,
            projectRefs,
            conditionRef,
            outputRowType,
            normalize);
    }

    /**
     * Creates a program builder with the same contents as a program.
     *
     * <p>If {@code normalize}, converts the program to canonical form. In
     * canonical form, in addition to the usual constraints:
     *
     * <ul>
     * <li>The first N internal expressions are {@link RexInputRef}s to the N
     *     input fields;
     * <li>Subsequent internal expressions reference only preceding expressions;
     * <li>Arguments to {@link RexCall}s must be {@link RexLocalRef}s (that is,
     *     expressions must have maximum depth 1)
     * </ul>
     *
     * there are additional constraints:
     *
     * <ul>
     * <li>Expressions appear in the left-deep order they are needed by
     *     the projections and (if present) the condition. Thus, expression N+1
     *     is the leftmost argument (literal or or call) in the expansion of
     *     projection #0.
     * <li>There are no duplicate expressions
     * <li>There are no unused expressions
     * </ul>
     *
     * @param rexBuilder Rex builder
     * @param inputRowType Input row type
     * @param exprList Common expressions
     * @param projectRefList Projections
     * @param conditionRef Condition, or null
     * @param outputRowType Output row type
     * @param normalize Whether to normalize
     * @return A program builder
     */
    public static RexProgramBuilder create(
        RexBuilder rexBuilder,
        final RelDataType inputRowType,
        final List<RexNode> exprList,
        final List<RexLocalRef> projectRefList,
        final RexLocalRef conditionRef,
        final RelDataType outputRowType,
        boolean normalize)
    {
        return new RexProgramBuilder(
            rexBuilder,
            inputRowType,
            exprList,
            projectRefList,
            conditionRef,
            outputRowType,
            normalize);
    }

    /**
     * Creates a program builder with the same contents as a program, applying a
     * shuttle first.
     *
     * <p>TODO: Refactor the above create method in terms of this one.
     *
     * @param rexBuilder Rex builder
     * @param inputRowType Input row type
     * @param exprList Common expressions
     * @param projectRefList Projections
     * @param conditionRef Condition, or null
     * @param outputRowType Output row type
     * @param shuttle Shuttle to apply to each expression before adding it to
     * the program builder
     * @param updateRefs Whether to update references that changes as a result
     * of rewrites made by the shuttle
     *
     * @return A program builder
     */
    public static RexProgramBuilder create(
        RexBuilder rexBuilder,
        final RelDataType inputRowType,
        final List<RexNode> exprList,
        final List<RexLocalRef> projectRefList,
        final RexLocalRef conditionRef,
        final RelDataType outputRowType,
        final RexShuttle shuttle,
        final boolean updateRefs)
    {
        final RexProgramBuilder progBuilder =
            new RexProgramBuilder(inputRowType, rexBuilder);
        progBuilder.add(
            exprList,
            projectRefList,
            conditionRef,
            outputRowType,
            shuttle,
            updateRefs);
        return progBuilder;
    }

    /**
     * Normalizes a program.
     *
     * @param rexBuilder Rex builder
     * @param program Program
     * @return Normalized program
     */
    public static RexProgram normalize(
        RexBuilder rexBuilder,
        RexProgram program)
    {
        // Normalize program by creating program builder from the program, then
        // converting to a program. getProgram does not need to normalize
        // because the builder was normalized on creation.
        return forProgram(program, rexBuilder, true)
            .getProgram(false);
    }

    /**
     * Adds a set of expressions, projections and filters, applying a shuttle
     * first.
     *
     * @param exprList Common expressions
     * @param projectRefList Projections
     * @param conditionRef Condition, or null
     * @param outputRowType Output row type
     * @param shuttle Shuttle to apply to each expression before adding it to
     * the program builder
     * @param updateRefs Whether to update references that changes as a result
     * of rewrites made by the shuttle
     */
    private void add(
        List<RexNode> exprList,
        List<RexLocalRef> projectRefList,
        RexLocalRef conditionRef,
        final RelDataType outputRowType,
        RexShuttle shuttle,
        boolean updateRefs)
    {
        final RelDataTypeField [] outFields = outputRowType.getFields();
        final RexShuttle registerInputShuttle = new RegisterInputShuttle(false);

        // For each common expression, first apply the user's shuttle, then
        // register the result.
        // REVIEW jpham 28-Apr-2006: if the user shuttle rewrites an input
        // expression, then input references may change
        List<RexLocalRef> newRefs = new ArrayList<RexLocalRef>(exprList.size());
        RexShuttle refShuttle = new UpdateRefShuttle(newRefs);
        int i = 0;
        for (RexNode expr : exprList) {
            RexNode newExpr = expr;
            if (updateRefs) {
                newExpr = expr.accept(refShuttle);
            }
            newExpr = newExpr.accept(shuttle);
            newRefs.add(
                i++,
                (RexLocalRef) newExpr.accept(registerInputShuttle));
        }
        i = -1;
        for (RexLocalRef oldRef : projectRefList) {
            ++i;
            RexLocalRef ref = oldRef;
            if (updateRefs) {
                ref = (RexLocalRef) oldRef.accept(refShuttle);
            }
            ref = (RexLocalRef) ref.accept(shuttle);
            this.projectRefList.add(ref);
            final String name = outFields[i].getName();
            assert name != null;
            projectNameList.add(name);
        }
        if (conditionRef != null) {
            if (updateRefs) {
                conditionRef = (RexLocalRef) conditionRef.accept(refShuttle);
            }
            conditionRef = (RexLocalRef) conditionRef.accept(shuttle);
            addCondition(conditionRef);
        }
    }

    /**
     * Merges two programs together, and normalizes the result.
     *
     * @see #mergePrograms(RexProgram, RexProgram, RexBuilder, boolean)
     *
     * @param topProgram Top program. Its expressions are in terms of the
     * outputs of the bottom program.
     * @param bottomProgram Bottom program. Its expressions are in terms of the
     * result fields of the relational expression's input
     * @param rexBuilder Rex builder
     *
     * @return Merged program
     */
    public static RexProgram mergePrograms(
        RexProgram topProgram,
        RexProgram bottomProgram,
        RexBuilder rexBuilder)
    {
        return mergePrograms(topProgram, bottomProgram, rexBuilder, true);
    }

    /**
     * Merges two programs together.
     *
     * <p>All expressions become common sub-expressions. For example, the query
     *
     * <pre>{@code
     * SELECT x + 1 AS p, x + y AS q FROM (
     *   SELECT a + b AS x, c AS y
     *   FROM t
     *   WHERE c = 6)}</pre>
     *
     * would be represented as the programs
     *
     * <pre>
     *   Calc:
     *       Projects={$2, $3},
     *       Condition=null,
     *       Exprs={$0, $1, $0 + 1, $0 + $1})
     *   Calc(
     *       Projects={$3, $2},
     *       Condition={$4}
     *       Exprs={$0, $1, $2, $0 + $1, $2 = 6}
     * </pre>
     *
     * <p>The merged program is
     *
     * <pre>
     *   Calc(
     *      Projects={$4, $5}
     *      Condition=$6
     *      Exprs={0: $0       // a
     *             1: $1        // b
     *             2: $2        // c
     *             3: ($0 + $1) // x = a + b
     *             4: ($3 + 1)  // p = x + 1
     *             5: ($3 + $2) // q = x + y
     *             6: ($2 = 6)  // c = 6
     * </pre>
     *
     * <p>Another example:</blockquote>
     *
     * <pre>SELECT *
     * FROM (
     *   SELECT a + b AS x, c AS y
     *   FROM t
     *   WHERE c = 6)
     * WHERE x = 5</pre>
     * </blockquote>
     *
     * becomes
     *
     * <blockquote>
     * <pre>SELECT a + b AS x, c AS y
     * FROM t
     * WHERE c = 6 AND (a + b) = 5</pre>
     * </blockquote>
     *
     * @param topProgram Top program. Its expressions are in terms of the
     * outputs of the bottom program.
     * @param bottomProgram Bottom program. Its expressions are in terms of the
     * result fields of the relational expression's input
     * @param rexBuilder Rex builder
     * @param normalize Whether to convert program to canonical form
     * @return Merged program
     */
    public static RexProgram mergePrograms(
        RexProgram topProgram,
        RexProgram bottomProgram,
        RexBuilder rexBuilder,
        boolean normalize)
    {
        // Initialize a program builder with the same expressions, outputs
        // and condition as the bottom program.
        assert bottomProgram.isValid(true);
        assert topProgram.isValid(true);
        final RexProgramBuilder progBuilder =
            RexProgramBuilder.forProgram(bottomProgram, rexBuilder, false);

        // Drive from the outputs of the top program. Register each expression
        // used as an output.
        final List<RexLocalRef> projectRefList =
            progBuilder.registerProjectsAndCondition(topProgram);

        // Switch to the projects needed by the top program. The original
        // projects of the bottom program are no longer needed.
        progBuilder.clearProjects();
        final RelDataType outputRowType = topProgram.getOutputRowType();
        final RelDataTypeField [] outputFields = outputRowType.getFields();
        assert outputFields.length == projectRefList.size();
        for (int i = 0; i < projectRefList.size(); i++) {
            RexLocalRef ref = projectRefList.get(i);
            progBuilder.addProject(
                ref,
                outputFields[i].getName());
        }
        RexProgram mergedProg = progBuilder.getProgram(normalize);
        assert mergedProg.isValid(true);
        return mergedProg;
    }

    private List<RexLocalRef> registerProjectsAndCondition(RexProgram program)
    {
        final List<RexNode> exprList = program.getExprList();
        final List<RexLocalRef> projectRefList = new ArrayList<RexLocalRef>();
        final RexShuttle shuttle = new RegisterOutputShuttle(exprList);

        // For each project, lookup the expr and expand it so it is in terms of
        // bottomCalc's input fields
        for (RexLocalRef topProject : program.getProjectList()) {
            final RexNode topExpr = exprList.get(topProject.getIndex());
            final RexLocalRef expanded = (RexLocalRef) topExpr.accept(shuttle);

            // Remember the expr, but don't add to the project list yet.
            projectRefList.add(expanded);
        }

        // Similarly for the condition.
        final RexLocalRef topCondition = program.getCondition();
        if (topCondition != null) {
            final RexNode topExpr = exprList.get(topCondition.getIndex());
            final RexLocalRef expanded = (RexLocalRef) topExpr.accept(shuttle);

            addCondition(registerInput(expanded));
        }
        return projectRefList;
    }

    /**
     * Removes all project items.
     */
    public void clearProjects()
    {
        projectRefList.clear();
        projectNameList.clear();
    }

    /**
     * Adds a project item for every input field.
     *
     * <p>You cannot call this method if there are other project items.
     *
     * @pre projectRefList.isEmpty()
     */
    public void addIdentity()
    {
        assert projectRefList.isEmpty();
        final RelDataTypeField [] fields = inputRowType.getFields();
        for (int i = 0; i < fields.length; i++) {
            final RelDataTypeField field = fields[i];
            addProject(new RexInputRef(
                    i,
                    field.getType()),
                field.getName());
        }
    }

    /**
     * Creates a reference to a given input field.
     *
     * @param index Ordinal of input field, must be less than the number of
     * fields in the input type
     *
     * @return Reference to input field
     */
    public RexLocalRef makeInputRef(int index)
    {
        final RelDataTypeField [] fields = inputRowType.getFields();
        assert index < fields.length;
        final RelDataTypeField field = fields[index];
        return new RexLocalRef(
            index,
            field.getType());
    }

    /**
     * Returns the rowtype of the input to the program
     */
    public RelDataType getInputRowType()
    {
        return inputRowType;
    }

    /**
     * Returns the list of project expressions.
     */
    public List<RexLocalRef> getProjectList()
    {
        return projectRefList;
    }

    //~ Inner Classes ----------------------------------------------------------

    private abstract class RegisterShuttle
        extends RexShuttle
    {
        public RexNode visitCall(RexCall call)
        {
            final RexNode expr = super.visitCall(call);
            return registerInternal(expr, false);
        }

        public RexNode visitOver(RexOver over)
        {
            final RexNode expr = super.visitOver(over);
            return registerInternal(expr, false);
        }

        public RexNode visitLiteral(RexLiteral literal)
        {
            final RexNode expr = super.visitLiteral(literal);
            return registerInternal(expr, false);
        }

        public RexNode visitFieldAccess(RexFieldAccess fieldAccess)
        {
            final RexNode expr = super.visitFieldAccess(fieldAccess);
            return registerInternal(expr, false);
        }

        public RexNode visitDynamicParam(RexDynamicParam dynamicParam)
        {
            final RexNode expr = super.visitDynamicParam(dynamicParam);
            return registerInternal(expr, false);
        }

        public RexNode visitCorrelVariable(RexCorrelVariable variable)
        {
            final RexNode expr = super.visitCorrelVariable(variable);
            return registerInternal(expr, false);
        }
    }

    /**
     * Shuttle which walks over an expression, registering each sub-expression.
     * Each {@link RexInputRef} is assumed to refer to an <em>input</em> of the
     * program.
     */
    private class RegisterInputShuttle
        extends RegisterShuttle
    {
        private final boolean valid;

        protected RegisterInputShuttle(boolean valid)
        {
            this.valid = valid;
        }

        public RexNode visitInputRef(RexInputRef input)
        {
            final int index = input.getIndex();
            if (valid) {
                // The expression should already be valid. Check that its
                // index is within bounds.
                if ((index < 0) || (index >= inputRowType.getFieldCount())) {
                    assert false : "RexInputRef index " + index
                        + " out of range 0.."
                        + (inputRowType.getFieldCount() - 1);
                }

                // Check that the type is consistent with the referenced
                // field. If it is an object type, the rules are different, so
                // skip the check.
                assert input.getType().isStruct()
                    || RelOptUtil.eq(
                        "type1",
                        input.getType(),
                        "type2",
                        inputRowType.getFields()[index].getType(),
                        true);
            }

            // Return a reference to the N'th expression, which should be
            // equivalent.
            final RexLocalRef ref = localRefList.get(index);
            return ref;
        }

        public RexNode visitLocalRef(RexLocalRef local)
        {
            if (valid) {
                // The expression should already be valid.
                final int index = local.getIndex();
                assert index >= 0 : index;
                assert index < exprList.size() : "index=" + index
                    + ", exprList=" + exprList;
                assert RelOptUtil.eq(
                    "expr type",
                    exprList.get(index).getType(),
                    "ref type",
                    local.getType(),
                    true);
            }

            // Resolve the expression to an input.
            while (true) {
                final int index = local.getIndex();
                final RexNode expr = exprList.get(index);
                if (expr instanceof RexLocalRef) {
                    local = (RexLocalRef) expr;
                    if (local.index >= index) {
                        throw Util.newInternal(
                            "expr " + local
                            + " references later expr " + local.index);
                    }
                } else {
                    // Add expression to the list, just so that subsequent
                    // expressions don't get screwed up. This expression is
                    // unused, so will be eliminated soon.
                    return registerInternal(local, false);
                }
            }
        }
    }

    /**
     * Extension to {@link RegisterInputShuttle} which allows expressions to be
     * in terms of inputs or previous common sub-expressions.
     */
    private class RegisterMidputShuttle
        extends RegisterInputShuttle
    {
        private final List<RexNode> localExprList;

        protected RegisterMidputShuttle(
            boolean valid,
            List<RexNode> localExprList)
        {
            super(valid);
            this.localExprList = localExprList;
        }

        public RexNode visitLocalRef(RexLocalRef local)
        {
            // Convert a local ref into the common-subexpression it references.
            final int index = local.getIndex();
            return localExprList.get(index).accept(this);
        }
    }

    /**
     * Shuttle which walks over an expression, registering each sub-expression.
     * Each {@link RexInputRef} is assumed to refer to an <em>output</em> of the
     * program.
     */
    private class RegisterOutputShuttle
        extends RegisterShuttle
    {
        private final List<RexNode> localExprList;

        public RegisterOutputShuttle(List<RexNode> localExprList)
        {
            super();
            this.localExprList = localExprList;
        }

        public RexNode visitInputRef(RexInputRef input)
        {
            // This expression refers to the Nth project column. Lookup that
            // column and find out what common sub-expression IT refers to.
            final int index = input.getIndex();
            final RexLocalRef local = projectRefList.get(index);
            assert RelOptUtil.eq(
                "type1",
                local.getType(),
                "type2",
                input.getType(),
                true);
            return local;
        }

        public RexNode visitLocalRef(RexLocalRef local)
        {
            // Convert a local ref into the common-subexpression it references.
            final int index = local.getIndex();
            return localExprList.get(index).accept(this);
        }
    }

    /**
     * Shuttle which rewires {@link RexLocalRef} using a list of updated
     * references
     */
    private class UpdateRefShuttle
        extends RexShuttle
    {
        private List<RexLocalRef> newRefs;

        private UpdateRefShuttle(List<RexLocalRef> newRefs)
        {
            this.newRefs = newRefs;
        }

        public RexNode visitLocalRef(RexLocalRef localRef)
        {
            return newRefs.get(localRef.getIndex());
        }
    }
}

// End RexProgramBuilder.java
