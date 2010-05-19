/*
// $Id$
// Package org.eigenbase is a class library of data management components.
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
package org.eigenbase.sql.type;

import java.util.*;

import org.eigenbase.sql.*;
import org.eigenbase.util.*;


/**
 * This class allows multiple existing {@link SqlOperandTypeChecker} rules to be
 * combined into one rule. For example, allowing an operand to be either string
 * or numeric could be done by:
 *
 * <blockquote>
 * <pre><code>
 *
 * CompositeOperandsTypeChecking newCompositeRule =
 *  new CompositeOperandsTypeChecking(
 *    Composition.OR,
 *    new SqlOperandTypeChecker[]{stringRule, numericRule});
 *
 * </code></pre>
 * </blockquote>
 *
 * Similary a rule that would only allow a numeric literal can be done by:
 *
 * <blockquote>
 * <pre><code>
 *
 * CompositeOperandsTypeChecking newCompositeRule =
 *  new CompositeOperandsTypeChecking(
 *    Composition.AND,
 *    new SqlOperandTypeChecker[]{numericRule, literalRule});
 *
 * </code></pre>
 * </blockquote>
 *
 * <p>Finally, creating a signature expecting a string for the first operand and
 * a numeric for the second operand can be done by:
 *
 * <blockquote>
 * <pre><code>
 *
 * CompositeOperandsTypeChecking newCompositeRule =
 *  new CompositeOperandsTypeChecking(
 *    Composition.SEQUENCE,
 *    new SqlOperandTypeChecker[]{stringRule, numericRule});
 *
 * </code></pre>
 * </blockquote>
 *
 * <p>For SEQUENCE composition, the rules must be instances of
 * SqlSingleOperandTypeChecker, and signature generation is not supported. For
 * AND composition, only the first rule is used for signature generation.
 *
 * @author Wael Chatila
 * @version $Id$
 */
public class CompositeOperandTypeChecker
    implements SqlSingleOperandTypeChecker
{
    //~ Enums ------------------------------------------------------------------

    public enum Composition
    {
        AND, OR, SEQUENCE;
    }

    //~ Instance fields --------------------------------------------------------

    private final SqlOperandTypeChecker [] allowedRules;
    private final Composition composition;

    //~ Constructors -----------------------------------------------------------

    public CompositeOperandTypeChecker(
        Composition composition,
        SqlOperandTypeChecker ... allowedRules)
    {
        Util.pre(null != allowedRules, "null != allowedRules");
        Util.pre(allowedRules.length > 1, "Not a composite type");
        this.allowedRules = allowedRules;
        this.composition = composition;
    }

    //~ Methods ----------------------------------------------------------------

    public SqlOperandTypeChecker [] getRules()
    {
        return allowedRules;
    }

    public String getAllowedSignatures(SqlOperator op, String opName)
    {
        if (composition == Composition.SEQUENCE) {
            throw Util.needToImplement("must override getAllowedSignatures");
        }
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < allowedRules.length; i++) {
            SqlOperandTypeChecker rule = allowedRules[i];
            if (i > 0) {
                ret.append(SqlOperator.NL);
            }
            ret.append(rule.getAllowedSignatures(op, opName));
            if (composition == Composition.AND) {
                break;
            }
        }
        return ret.toString();
    }

    public SqlOperandCountRange getOperandCountRange()
    {
        if (composition == Composition.SEQUENCE) {
            return new SqlOperandCountRange(allowedRules.length);
        } else {
            // TODO jvs 2-June-2005:  technically, this is only correct
            // for OR, not AND; probably not a big deal
            Set<Integer> set = new TreeSet<Integer>();
            for (int i = 0; i < allowedRules.length; i++) {
                SqlOperandTypeChecker rule = allowedRules[i];
                SqlOperandCountRange range = rule.getOperandCountRange();
                if (range.isVariadic()) {
                    return SqlOperandCountRange.Variadic;
                }
                set.addAll(range.getAllowedList());
            }
            return new SqlOperandCountRange(new ArrayList<Integer>(set));
        }
    }

    public boolean checkSingleOperandType(
        SqlCallBinding callBinding,
        SqlNode node,
        int iFormalOperand,
        boolean throwOnFailure)
    {
        Util.pre(allowedRules.length >= 1, "allowedRules.length>=1");

        if (composition == Composition.SEQUENCE) {
            SqlSingleOperandTypeChecker singleRule =
                (SqlSingleOperandTypeChecker) allowedRules[iFormalOperand];
            return singleRule.checkSingleOperandType(
                callBinding,
                node,
                0,
                throwOnFailure);
        }

        int typeErrorCount = 0;

        boolean throwOnAndFailure =
            (composition == Composition.AND)
            && throwOnFailure;

        for (int i = 0; i < allowedRules.length; i++) {
            SqlSingleOperandTypeChecker rule =
                (SqlSingleOperandTypeChecker) allowedRules[i];
            if (!rule.checkSingleOperandType(
                    callBinding,
                    node,
                    iFormalOperand,
                    throwOnAndFailure))
            {
                typeErrorCount++;
            }
        }

        boolean ret = false;
        switch (composition) {
        case AND:
            ret = typeErrorCount == 0;
            break;
        case OR:
            ret = (typeErrorCount < allowedRules.length);
            break;
        default:

            //should never come here
            throw Util.unexpected(composition);
        }

        if (!ret && throwOnFailure) {
            //in the case of a composite OR we want to throw an error
            //describing in more detail what the problem was, hence doing
            //the loop again
            for (int i = 0; i < allowedRules.length; i++) {
                SqlSingleOperandTypeChecker rule =
                    (SqlSingleOperandTypeChecker) allowedRules[i];
                rule.checkSingleOperandType(
                    callBinding,
                    node,
                    iFormalOperand,
                    true);
            }

            //if no exception thrown, just throw a generic validation
            //signature error
            throw callBinding.newValidationSignatureError();
        }

        return ret;
    }

    public boolean checkOperandTypes(
        SqlCallBinding callBinding,
        boolean throwOnFailure)
    {
        int typeErrorCount = 0;

        for (int i = 0; i < allowedRules.length; i++) {
            SqlOperandTypeChecker rule = allowedRules[i];

            if (composition == Composition.SEQUENCE) {
                SqlSingleOperandTypeChecker singleRule =
                    (SqlSingleOperandTypeChecker) rule;
                if (i >= callBinding.getOperandCount()) {
                    break;
                }
                if (!singleRule.checkSingleOperandType(
                        callBinding,
                        callBinding.getCall().operands[i],
                        0,
                        false))
                {
                    typeErrorCount++;
                }
            } else {
                if (!rule.checkOperandTypes(callBinding, false)) {
                    typeErrorCount++;
                }
            }
        }

        boolean failed = true;
        switch (composition) {
        case AND:
        case SEQUENCE:
            failed = typeErrorCount > 0;
            break;
        case OR:
            failed = (typeErrorCount == allowedRules.length);
            break;
        }

        if (failed) {
            if (throwOnFailure) {
                //in the case of a composite OR we want to throw an error
                //describing in more detail what the problem was, hence doing
                //the loop again
                if (composition == Composition.OR) {
                    for (int i = 0; i < allowedRules.length; i++) {
                        allowedRules[i].checkOperandTypes(callBinding, true);
                    }
                }

                //if no exception thrown, just throw a generic validation
                //signature error
                throw callBinding.newValidationSignatureError();
            }
            return false;
        }
        return true;
    }
}

// End CompositeOperandTypeChecker.java
