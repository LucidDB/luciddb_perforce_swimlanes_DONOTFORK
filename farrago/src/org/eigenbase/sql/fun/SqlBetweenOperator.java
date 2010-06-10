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
package org.eigenbase.sql.fun;

import java.util.*;

import org.eigenbase.reltype.*;
import org.eigenbase.resource.*;
import org.eigenbase.sql.*;
import org.eigenbase.sql.parser.*;
import org.eigenbase.sql.type.*;
import org.eigenbase.sql.util.*;
import org.eigenbase.sql.validate.*;
import org.eigenbase.util.*;


/**
 * Defines the BETWEEN operator.
 *
 * <p>Syntax:
 *
 * <blockquote><code>X [NOT] BETWEEN [ASYMMETRIC | SYMMETRIC] Y AND
 * Z</code></blockquote>
 *
 * <p>If the asymmetric/symmeteric keywords are left out ASYMMETRIC is default.
 *
 * <p>This operator is always expanded (into something like <code>Y &lt;= X AND
 * X &lt;= Z</code>) before being converted into Rex nodes.
 *
 * @author Wael Chatila
 * @version $Id$
 * @since Jun 9, 2004
 */
public class SqlBetweenOperator
    extends SqlInfixOperator
{
    //~ Static fields/initializers ---------------------------------------------

    private static final String [] betweenNames =
        new String[] { "BETWEEN", "AND" };
    private static final String [] notBetweenNames =
        new String[] { "NOT BETWEEN", "AND" };

    /**
     * Ordinal of the 'value' operand.
     */
    public static final int VALUE_OPERAND = 0;

    /**
     * Ordinal of the 'lower' operand.
     */
    public static final int LOWER_OPERAND = 1;

    /**
     * Ordinal of the 'upper' operand.
     */
    public static final int UPPER_OPERAND = 2;

    /**
     * Ordinal of the 'symmetric' operand.
     */
    public static final int SYMFLAG_OPERAND = 3;

    /**
     * Custom operand-type checking strategy.
     */
    private static final SqlOperandTypeChecker otcCustom =
        new ComparableOperandTypeChecker(
            3,
            RelDataTypeComparability.All);
    private static final SqlWriter.FrameType BetweenFrameType =
        SqlWriter.FrameTypeEnum.create("BETWEEN");

    //~ Enums ------------------------------------------------------------------

    /**
     * Defines the "SYMMETRIC" and "ASYMMETRIC" keywords.
     */
    public enum Flag
        implements SqlLiteral.SqlSymbol
    {
        ASYMMETRIC, SYMMETRIC;
    }

    //~ Instance fields --------------------------------------------------------

    /**
     * todo: Use a wrapper 'class SqlTempCall(SqlOperator,SqlParserPos) extends
     * SqlNode' to store extra flags (neg and asymmetric) to calls to BETWEEN.
     * Then we can obsolete flag. SqlTempCall would never have any SqlNodes as
     * children, but it can have flags.
     */
    private final Flag flag;

    /**
     * If true the call represents 'NOT BETWEEN'.
     */
    private final boolean negated;

    //~ Constructors -----------------------------------------------------------

    public SqlBetweenOperator(
        Flag flag,
        boolean negated)
    {
        super(
            negated ? notBetweenNames : betweenNames,
            SqlKind.BETWEEN,
            30,
            null,
            null,
            otcCustom);
        this.flag = flag;
        this.negated = negated;
    }

    //~ Methods ----------------------------------------------------------------

    public boolean isNegated()
    {
        return negated;
    }

    private RelDataType [] collectOperandTypes(
        SqlValidator validator,
        SqlValidatorScope scope,
        SqlCall call)
    {
        RelDataType [] argTypes =
            SqlTypeUtil.deriveAndCollectTypes(
                validator,
                scope,
                call.operands);
        RelDataType [] newArgTypes =
        {
            argTypes[VALUE_OPERAND],
            argTypes[LOWER_OPERAND],
            argTypes[UPPER_OPERAND]
        };
        return newArgTypes;
    }

    public RelDataType inferReturnType(
        SqlOperatorBinding opBinding)
    {
        SqlCallBinding callBinding = (SqlCallBinding) opBinding;
        ExplicitOperatorBinding newOpBinding =
            new ExplicitOperatorBinding(
                opBinding,
                collectOperandTypes(
                    callBinding.getValidator(),
                    callBinding.getScope(),
                    callBinding.getCall()));
        return SqlTypeStrategies.rtiNullableBoolean.inferReturnType(
            newOpBinding);
    }

    public String getSignatureTemplate(final int operandsCount)
    {
        Util.discard(operandsCount);
        return "{1} {0} {2} AND {3}";
    }

    public SqlOperandCountRange getOperandCountRange()
    {
        //exp1 [ASYMMETRIC|SYMMETRIC] BETWEEN exp4 AND exp4
        return SqlOperandCountRange.Four;
    }

    public void unparse(
        SqlWriter writer,
        SqlNode [] operands,
        int leftPrec,
        int rightPrec)
    {
        final SqlWriter.Frame frame =
            writer.startList(BetweenFrameType, "", "");
        operands[VALUE_OPERAND].unparse(
            writer,
            getLeftPrec(),
            0);
        writer.sep(getName());
        operands[SYMFLAG_OPERAND].unparse(writer, 0, 0);

        // If the expression for the lower bound contains a call to an AND
        // operator, we need to wrap the expression in parentheses to prevent
        // the AND from associating with BETWEEN. For example, we should
        // unparse
        //    a BETWEEN b OR (c AND d) OR e AND f
        // as
        //    a BETWEEN (b OR c AND d) OR e) AND f
        // If it were unparsed as
        //    a BETWEEN b OR c AND d OR e AND f
        // then it would be interpreted as
        //    (a BETWEEN (b OR c) AND d) OR (e AND f)
        // which would be wrong.
        int lowerPrec =
            new AndFinder().containsAnd(operands[LOWER_OPERAND]) ? 100 : 0;
        operands[LOWER_OPERAND].unparse(writer, lowerPrec, lowerPrec);
        writer.sep("AND");
        operands[UPPER_OPERAND].unparse(
            writer,
            0,
            getRightPrec());
        writer.endList(frame);
    }

    public int reduceExpr(
        int opOrdinal,
        List<Object> list)
    {
        final SqlParserUtil.ToTreeListItem betweenNode =
            (SqlParserUtil.ToTreeListItem) list.get(opOrdinal);
        SqlOperator op = betweenNode.getOperator();
        assert op == this;

        // Break the expression up into expressions. For example, a simple
        // expression breaks down as follows:
        //
        //            opOrdinal   endExp1
        //            |           |
        //     a + b BETWEEN c + d AND e + f
        //    |_____|       |_____|   |_____|
        //     exp0          exp1      exp2
        // Create the expression between 'BETWEEN' and 'AND'.
        final SqlParserPos pos =
            ((SqlNode) list.get(opOrdinal + 1)).getParserPosition();
        SqlNode exp1 =
            SqlParserUtil.toTreeEx(list, opOrdinal + 1, 0, SqlKind.AND);
        if ((opOrdinal + 2) >= list.size()) {
            SqlParserPos lastPos =
                ((SqlNode) list.get(list.size() - 1)).getParserPosition();
            final int line = lastPos.getEndLineNum();
            final int col = lastPos.getEndColumnNum() + 1;
            SqlParserPos errPos = new SqlParserPos(line, col, line, col);
            throw SqlUtil.newContextException(
                errPos,
                EigenbaseResource.instance().BetweenWithoutAnd.ex());
        }
        final Object o = list.get(opOrdinal + 2);
        if (!(o instanceof SqlParserUtil.ToTreeListItem)) {
            SqlParserPos errPos = ((SqlNode) o).getParserPosition();
            throw SqlUtil.newContextException(
                errPos,
                EigenbaseResource.instance().BetweenWithoutAnd.ex());
        }
        if (((SqlParserUtil.ToTreeListItem) o).getOperator().getKind()
            != SqlKind.AND)
        {
            SqlParserPos errPos = ((SqlParserUtil.ToTreeListItem) o).getPos();
            throw SqlUtil.newContextException(
                errPos,
                EigenbaseResource.instance().BetweenWithoutAnd.ex());
        }

        // Create the expression after 'AND', but stopping if we encounter an
        // operator of lower precedence.
        //
        // For example,
        //   a BETWEEN b AND c + d OR e
        // becomes
        //   (a BETWEEN b AND c + d) OR e
        // because OR has lower precedence than BETWEEN.
        SqlNode exp2 =
            SqlParserUtil.toTreeEx(
                list,
                opOrdinal + 3,
                getRightPrec(),
                SqlKind.OTHER);

        // Create the call.
        SqlNode exp0 = (SqlNode) list.get(opOrdinal - 1);
        SqlCall newExp =
            createCall(
                betweenNode.getPos(),
                exp0,
                exp1,
                exp2,
                SqlLiteral.createSymbol(flag, SqlParserPos.ZERO));

        // Replace all of the matched nodes with the single reduced node.
        SqlParserUtil.replaceSublist(
            list,
            opOrdinal - 1,
            opOrdinal + 4,
            newExp);

        // Return the ordinal of the new current node.
        return opOrdinal - 1;
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * Finds an AND operator in an expression.
     */
    private static class AndFinder
        extends SqlBasicVisitor<Void>
    {
        public Void visit(SqlCall call)
        {
            final SqlOperator operator = call.getOperator();
            if (operator == SqlStdOperatorTable.andOperator) {
                throw new Found();
            }
            return super.visit(call);
        }

        boolean containsAnd(SqlNode node)
        {
            try {
                node.accept(this);
                return false;
            } catch (AndFinder.Found e) {
                return true;
            }
        }

        private static class Found
            extends RuntimeException
        {
        }
    }
}

// End SqlBetweenOperator.java
