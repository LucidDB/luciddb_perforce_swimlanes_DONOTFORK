/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2007 The Eigenbase Project
// Copyright (C) 2007 SQLstream, Inc.
// Copyright (C) 2007 Dynamo BI Corporation
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

import org.eigenbase.sql.*;
import org.eigenbase.sql.fun.*;
import org.eigenbase.sql.parser.*;
import org.eigenbase.sql.type.*;
import org.eigenbase.util.*;


/**
 * Standard implementation of {@link RexSqlConvertletTable}.
 */
public class RexSqlStandardConvertletTable
    extends RexSqlReflectiveConvertletTable
{
    //~ Constructors -----------------------------------------------------------

    public RexSqlStandardConvertletTable()
    {
        super();

        // Register convertlets

        registerEquivOp(SqlStdOperatorTable.greaterThanOrEqualOperator);
        registerEquivOp(SqlStdOperatorTable.greaterThanOperator);
        registerEquivOp(SqlStdOperatorTable.lessThanOrEqualOperator);
        registerEquivOp(SqlStdOperatorTable.lessThanOperator);
        registerEquivOp(SqlStdOperatorTable.equalsOperator);
        registerEquivOp(SqlStdOperatorTable.notEqualsOperator);
        registerEquivOp(SqlStdOperatorTable.andOperator);
        registerEquivOp(SqlStdOperatorTable.orOperator);
        registerEquivOp(SqlStdOperatorTable.notInOperator);
        registerEquivOp(SqlStdOperatorTable.inOperator);
        registerEquivOp(SqlStdOperatorTable.likeOperator);
        registerEquivOp(SqlStdOperatorTable.notLikeOperator);
        registerEquivOp(SqlStdOperatorTable.similarOperator);
        registerEquivOp(SqlStdOperatorTable.notSimilarOperator);
        registerEquivOp(SqlStdOperatorTable.plusOperator);
        registerEquivOp(SqlStdOperatorTable.minusOperator);
        registerEquivOp(SqlStdOperatorTable.multiplyOperator);
        registerEquivOp(SqlStdOperatorTable.divideOperator);

        registerEquivOp(SqlStdOperatorTable.notOperator);

        registerEquivOp(SqlStdOperatorTable.isNotNullOperator);
        registerEquivOp(SqlStdOperatorTable.isNullOperator);

        registerEquivOp(SqlStdOperatorTable.isNotTrueOperator);
        registerEquivOp(SqlStdOperatorTable.isTrueOperator);

        registerEquivOp(SqlStdOperatorTable.isNotFalseOperator);
        registerEquivOp(SqlStdOperatorTable.isFalseOperator);

        registerEquivOp(SqlStdOperatorTable.isNotUnknownOperator);
        registerEquivOp(SqlStdOperatorTable.isUnknownOperator);

        registerEquivOp(SqlStdOperatorTable.prefixMinusOperator);
        registerEquivOp(SqlStdOperatorTable.prefixPlusOperator);

        registerCaseOp(SqlStdOperatorTable.caseOperator);
        registerEquivOp(SqlStdOperatorTable.concatOperator);

        registerEquivOp(SqlStdOperatorTable.betweenOperator);
        registerEquivOp(SqlStdOperatorTable.symmetricBetweenOperator);

        registerEquivOp(SqlStdOperatorTable.notBetweenOperator);
        registerEquivOp(SqlStdOperatorTable.symmetricNotBetweenOperator);

        registerEquivOp(SqlStdOperatorTable.isNotDistinctFromOperator);
        registerEquivOp(SqlStdOperatorTable.isDistinctFromOperator);

        registerEquivOp(SqlStdOperatorTable.minusDateOperator);
        registerEquivOp(SqlStdOperatorTable.extractFunc);

        registerEquivOp(SqlStdOperatorTable.substringFunc);
        registerEquivOp(SqlStdOperatorTable.convertFunc);
        registerEquivOp(SqlStdOperatorTable.translateFunc);
        registerEquivOp(SqlStdOperatorTable.overlayFunc);
        registerEquivOp(SqlStdOperatorTable.trimFunc);
        registerEquivOp(SqlStdOperatorTable.positionFunc);
        registerEquivOp(SqlStdOperatorTable.charLengthFunc);
        registerEquivOp(SqlStdOperatorTable.characterLengthFunc);
        registerEquivOp(SqlStdOperatorTable.upperFunc);
        registerEquivOp(SqlStdOperatorTable.lowerFunc);
        registerEquivOp(SqlStdOperatorTable.initcapFunc);

        registerEquivOp(SqlStdOperatorTable.powerFunc);
        registerEquivOp(SqlStdOperatorTable.sqrtFunc);
        registerEquivOp(SqlStdOperatorTable.modFunc);
        registerEquivOp(SqlStdOperatorTable.lnFunc);
        registerEquivOp(SqlStdOperatorTable.log10Func);
        registerEquivOp(SqlStdOperatorTable.absFunc);
        registerEquivOp(SqlStdOperatorTable.expFunc);
        registerEquivOp(SqlStdOperatorTable.floorFunc);
        registerEquivOp(SqlStdOperatorTable.ceilFunc);

        registerEquivOp(SqlStdOperatorTable.nullIfFunc);
        registerEquivOp(SqlStdOperatorTable.coalesceFunc);

        registerTypeAppendOp(SqlStdOperatorTable.castFunc);
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Converts a call to an operator into a {@link SqlCall} to the same
     * operator.
     *
     * <p>Called automatically via reflection.
     *
     * @param converter Converter
     * @param call Call
     *
     * @return Sql call
     */
    public SqlNode convertCall(
        RexToSqlNodeConverter converter,
        RexCall call)
    {
        if (get(call) == null) {
            return null;
        }

        final SqlOperator op = call.getOperator();
        final RexNode [] operands = call.getOperands();

        final SqlNode [] exprs = convertExpressionList(converter, operands);
        if (exprs == null) {
            return null;
        }
        return new SqlCall(
            op,
            exprs,
            SqlParserPos.ZERO);
    }

    private SqlNode [] convertExpressionList(
        RexToSqlNodeConverter converter,
        RexNode [] nodes)
    {
        final SqlNode [] exprs = new SqlNode[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            RexNode node = nodes[i];
            exprs[i] = converter.convertNode(node);
            if (exprs[i] == null) {
                return null;
            }
        }
        return exprs;
    }

    /**
     * Creates and registers a convertlet for an operator in which
     * the SQL and Rex representations are structurally equivalent.
     *
     * @param op operator instance
     */
    protected void registerEquivOp(final SqlOperator op)
    {
        registerOp(
            op,
            new RexSqlConvertlet() {
                public SqlNode convertCall(
                    RexToSqlNodeConverter converter,
                    RexCall call)
                {
                    SqlNode [] operands =
                        convertExpressionList(converter, call.operands);
                    if (operands == null) {
                        return null;
                    }
                    return new SqlCall(
                        op,
                        operands,
                        SqlParserPos.ZERO);
                }
            });
    }

    /**
     * Creates and registers a convertlet for an operator in which
     * the SQL representation needs the result type appended
     * as an extra argument (e.g. CAST).
     *
     * @param op operator instance
     */
    private void registerTypeAppendOp(final SqlOperator op)
    {
        registerOp(
            op,
            new RexSqlConvertlet() {
                public SqlNode convertCall(
                    RexToSqlNodeConverter converter,
                    RexCall call)
                {
                    SqlNode [] operands =
                        convertExpressionList(converter, call.operands);
                    if (operands == null) {
                        return null;
                    }
                    List<SqlNode> operandList =
                        new ArrayList<SqlNode>(Arrays.asList(operands));
                    SqlDataTypeSpec typeSpec =
                        SqlTypeUtil.convertTypeToSpec(call.getType());
                    operandList.add(typeSpec);
                    return new SqlCall(
                        op,
                        operandList.toArray(new SqlNode[0]),
                        SqlParserPos.ZERO);
                }
            });
    }

    /**
     * Creates and registers a convertlet for the CASE operator,
     * which takes different forms for SQL vs Rex.
     *
     * @param op instance of CASE operator
     */
    private void registerCaseOp(final SqlOperator op)
    {
        registerOp(
            op,
            new RexSqlConvertlet() {
                public SqlNode convertCall(
                    RexToSqlNodeConverter converter,
                    RexCall call)
                {
                    assert(op instanceof SqlCaseOperator);
                    SqlNode [] operands =
                        convertExpressionList(converter, call.operands);
                    if (operands == null) {
                        return null;
                    }
                    SqlNodeList whenList = new SqlNodeList(SqlParserPos.ZERO);
                    SqlNodeList thenList = new SqlNodeList(SqlParserPos.ZERO);
                    int i = 0;
                    while (i < operands.length - 1) {
                        whenList.add(operands[i]);
                        ++i;
                        thenList.add(operands[i]);
                        ++i;
                    }
                    SqlNode elseExpr = operands[i];
                    SqlNode [] newOperands = new SqlNode[3];
                    newOperands[0] = whenList;
                    newOperands[1] = thenList;
                    newOperands[2] = elseExpr;
                    return op.createCall(null, SqlParserPos.ZERO, newOperands);
                }
            });
    }
}

// End RexSqlStandardConvertletTable.java
