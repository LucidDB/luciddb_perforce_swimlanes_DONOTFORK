/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2005-2005 The Eigenbase Project
// Copyright (C) 2005-2005 Disruptive Tech
// Copyright (C) 2005-2005 LucidEra, Inc.
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

import org.eigenbase.resource.*;
import org.eigenbase.sql.*;
import org.eigenbase.reltype.*;
import org.eigenbase.util.*;

/**
 * SqlTypeStrategies defines singleton instances of strategy objects for
 * operand type checking (member prefix <code>otc</code>), operand type
 * inference (member prefix <code>oti</code>), and operator return type
 * inference (member prefix <code>rti</code>).  For otc members, the convention
 * <code>otcSometypeX2</code> means two operands of type <code>Sometype</code>.
 * The convention <code>otcSometypeLit</code> means a literal operand of type
 * <code>Sometype</code>.
 *
 *<p>
 *
 * NOTE: avoid anonymous inner classes here except for unique,
 * non-generalizable strategies; anything else belongs in a reusable top-level
 * class.  If you find yourself copying and pasting an existing strategy's
 * anonymous inner class, you're making a mistake.
 *
 * @author Wael Chatila
 * @version $Id$
 */
public abstract class SqlTypeStrategies
{
    // ----------------------------------------------------------------------
    // SqlOperandTypeChecker definitions
    // ----------------------------------------------------------------------
    
    /**
     * Operand type-checking strategy for an operator which takes no
     * operands.
     */
    public static final SqlSingleOperandTypeChecker
        otcNiladic =
        new FamilyOperandTypeChecker(new SqlTypeFamily [] {});

    /**
     * Operand type-checking strategy for an operator with
     * no restrictions on number or type of operands.
     */
    public static final SqlOperandTypeChecker
        otcVariadic =
        new SqlOperandTypeChecker()
        {
            public boolean checkOperandTypes(
                SqlCallBinding callBinding,
                boolean throwOnFailure)
            {
                return true;
            }

            public SqlOperandCountRange getOperandCountRange()
            {
                return SqlOperandCountRange.Variadic;
            }

            public String getAllowedSignatures(SqlOperator op, String opName)
            {
                return opName + "(...)";
            }
        };
        
    public static final SqlSingleOperandTypeChecker
        otcBool =
        new FamilyOperandTypeChecker(new SqlTypeFamily [] {
            SqlTypeFamily.Boolean
        });

    public static final SqlSingleOperandTypeChecker
        otcBoolX2 =
        new FamilyOperandTypeChecker(new SqlTypeFamily [] {
            SqlTypeFamily.Boolean,
            SqlTypeFamily.Boolean
        });

    public static final SqlSingleOperandTypeChecker
        otcNumeric =
        new FamilyOperandTypeChecker(new SqlTypeFamily [] {
            SqlTypeFamily.Numeric });

    public static final SqlSingleOperandTypeChecker
        otcNumericX2 =
        new FamilyOperandTypeChecker(new SqlTypeFamily [] {
            SqlTypeFamily.Numeric,
            SqlTypeFamily.Numeric
        });

public static final SqlSingleOperandTypeChecker
    otcExactNumeric =
    new FamilyOperandTypeChecker(new SqlTypeFamily [] {
        SqlTypeFamily.ExactNumeric });

public static final SqlSingleOperandTypeChecker
    otcExactNumericX2 =
    new FamilyOperandTypeChecker(new SqlTypeFamily [] {
        SqlTypeFamily.ExactNumeric,
        SqlTypeFamily.ExactNumeric
    });

    public static final SqlSingleOperandTypeChecker
        otcBinary =
        new FamilyOperandTypeChecker(new SqlTypeFamily [] {
            SqlTypeFamily.Binary
        });

    public static final SqlSingleOperandTypeChecker
        otcString =
        new FamilyOperandTypeChecker(new SqlTypeFamily [] {
            SqlTypeFamily.String
        });

    public static final SqlSingleOperandTypeChecker
        otcCharString =
        new FamilyOperandTypeChecker(new SqlTypeFamily [] {
            SqlTypeFamily.Character
        });

    public static final SqlSingleOperandTypeChecker
        otcDatetime =
        new FamilyOperandTypeChecker(new SqlTypeFamily [] {
            SqlTypeFamily.Datetime
        });
    
    public static final SqlSingleOperandTypeChecker
        otcInterval =
        new FamilyOperandTypeChecker(new SqlTypeFamily [] {
            SqlTypeFamily.DatetimeInterval
        });

    public static final SqlSingleOperandTypeChecker
        otcMultiset =
        new FamilyOperandTypeChecker(new SqlTypeFamily [] {
            SqlTypeFamily.Multiset
        });

    /**
     * Operand type-checking strategy where
     * type must be a literal or NULL.
     */
    public static final SqlSingleOperandTypeChecker
        otcNullableLit =
        new LiteralOperandTypeChecker(true);

    /**
     * Operand type-checking strategy
     * type must be a non-NULL literal.
     */
    public static final SqlSingleOperandTypeChecker
        otcNotNullLit =
        new LiteralOperandTypeChecker(false);

    /**
     * Operand type-checking strategy
     * type must be a positive integer non-NULL literal.
     */
    public static final SqlSingleOperandTypeChecker
        otcPositiveIntLit =
        new FamilyOperandTypeChecker(new SqlTypeFamily [] {
            SqlTypeFamily.Integer
        })
        {
            public boolean checkSingleOperandType(
                SqlCallBinding callBinding,
                SqlNode node,
                int iFormalOperand, boolean throwOnFailure)
            {
                if (!otcNotNullLit.checkSingleOperandType(callBinding, node,
                    iFormalOperand, throwOnFailure))
                {
                    return false;
                }

                if (!super.checkSingleOperandType(callBinding, node,
                    iFormalOperand, throwOnFailure))
                {
                    return false;
                }

                final SqlLiteral arg = ((SqlLiteral) node);
                final int value = arg.intValue(true);
                if (value < 0) {
                    if (throwOnFailure) {
                        throw EigenbaseResource.instance()
                            .ArgumentMustBePositiveInteger.ex(
                                callBinding.getOperator().getName());
                    }
                    return false;
                }
                return true;
            }
        };

    /**
     * Operand type-checking strategy where two operands
     * must both be in the same type family.
     */
    public static final SqlOperandTypeChecker
        otcSameX2 =
        new SameOperandTypeChecker(2);

    /**
     * Operand type-checking strategy where three operands
     * must all be in the same type family.
     */
    public static final SqlOperandTypeChecker
        otcSameX3 =
        new SameOperandTypeChecker(3);

    /**
     * Operand type-checking strategy where operand types must allow
     * ordered comparisons.
     */
    public static final SqlOperandTypeChecker
        otcComparableOrderedX2 =
        new ComparableOperandTypeChecker(
            2,
            RelDataTypeComparability.All);

    /**
     * Operand type-checking strategy where operand type must allow
     * ordered comparisons. Used when instance comparisons are made
     * on single operand functions
     */
    public static final SqlOperandTypeChecker
        otcComparableOrdered =
        new ComparableOperandTypeChecker(
            1,
            RelDataTypeComparability.All);

    /**
     * Operand type-checking strategy where operand types must allow
     * unordered comparisons.
     */
    public static final SqlOperandTypeChecker
        otcComparableUnorderedX2 =
        new ComparableOperandTypeChecker(
            2,
            RelDataTypeComparability.Unordered);

    /**
     * Operand type-checking strategy where two operands
     * must both be in the same string type family.
     */
    public static final SqlSingleOperandTypeChecker
        otcStringSameX2 =
        new CompositeOperandTypeChecker(
            CompositeOperandTypeChecker.AND, 
            new SqlOperandTypeChecker[] {
                new FamilyOperandTypeChecker(new SqlTypeFamily [] {
                    SqlTypeFamily.String,
                    SqlTypeFamily.String
                }),
                otcSameX2
            });

    /**
     * Operand type-checking strategy where three operands
     * must all be in the same string type family.
     */
    public static final SqlSingleOperandTypeChecker
        otcStringSameX3 =
        new CompositeOperandTypeChecker(
            CompositeOperandTypeChecker.AND, 
            new SqlOperandTypeChecker[] {
                new FamilyOperandTypeChecker(new SqlTypeFamily [] {
                    SqlTypeFamily.String,
                    SqlTypeFamily.String,
                    SqlTypeFamily.String

                }),
                otcSameX3
            });

    public static final SqlSingleOperandTypeChecker
        otcStringX2Int =
        new FamilyOperandTypeChecker(new SqlTypeFamily [] {
            SqlTypeFamily.String,
            SqlTypeFamily.String,
            SqlTypeFamily.Integer
        });

    public static final SqlSingleOperandTypeChecker
        otcStringX2IntX2 =
        new FamilyOperandTypeChecker(new SqlTypeFamily [] {
            SqlTypeFamily.String,
            SqlTypeFamily.String,
            SqlTypeFamily.Integer,
            SqlTypeFamily.Integer
        });

    public static final SqlSingleOperandTypeChecker
        otcAny =
        new FamilyOperandTypeChecker(new SqlTypeFamily [] {
            SqlTypeFamily.Any
        });

    public static final SqlSingleOperandTypeChecker
        otcAnyX2 =
        new FamilyOperandTypeChecker(new SqlTypeFamily [] {
            SqlTypeFamily.Any ,
            SqlTypeFamily.Any
        });

    /**
     * Parameter type-checking strategy
     * type must a nullable time interval, nullable time interval
     */
    public static final SqlSingleOperandTypeChecker
        otcIntervalSameX2 =
        new CompositeOperandTypeChecker(
            CompositeOperandTypeChecker.AND, 
            new SqlOperandTypeChecker[] {
                new FamilyOperandTypeChecker(
                    new SqlTypeFamily [] {
                        SqlTypeFamily.DatetimeInterval,
                        SqlTypeFamily.DatetimeInterval
                    }),
                otcSameX2
            });

    public static final SqlSingleOperandTypeChecker
        otcNumericInterval =
        new FamilyOperandTypeChecker(new SqlTypeFamily [] {
            SqlTypeFamily.Numeric,
            SqlTypeFamily.DatetimeInterval
        });

    public static final SqlSingleOperandTypeChecker
        otcIntervalNumeric =
        new FamilyOperandTypeChecker(new SqlTypeFamily [] {
            SqlTypeFamily.DatetimeInterval,
            SqlTypeFamily.Numeric
        });

    public static final SqlSingleOperandTypeChecker
        otcDatetimeInterval =
        new FamilyOperandTypeChecker(new SqlTypeFamily [] {
            SqlTypeFamily.Datetime,
            SqlTypeFamily.DatetimeInterval
        });

    public static final SqlSingleOperandTypeChecker
        otcIntervalDatetime =
        new FamilyOperandTypeChecker(new SqlTypeFamily [] {
            SqlTypeFamily.DatetimeInterval,
            SqlTypeFamily.Datetime
        });

    public static final SqlSingleOperandTypeChecker
        otcPlusOperator =
        new CompositeOperandTypeChecker(
            CompositeOperandTypeChecker.OR,
            new SqlOperandTypeChecker[] {
                  otcNumericX2,
                  otcIntervalSameX2,
                  otcDatetimeInterval,
                  otcIntervalDatetime
                  //todo datetime+interval checking missing
                  //todo interval+datetime checking missing
            });

    /**
     * Type checking strategy for the "*" operator
     */
    public static final SqlSingleOperandTypeChecker
        otcMultiplyOperator =
        new CompositeOperandTypeChecker(
            CompositeOperandTypeChecker.OR,
            new SqlOperandTypeChecker[] {
                  otcNumericX2,
                  otcIntervalNumeric,
                  otcNumericInterval
            });

    /**
     * Type checking strategy for the "/" operator
     */
    public static final SqlSingleOperandTypeChecker
        otcDivisionOperator =
        new CompositeOperandTypeChecker(
            CompositeOperandTypeChecker.OR,
            new SqlOperandTypeChecker[] {
                  otcNumericX2,
                  otcIntervalNumeric
            });

    public static final SqlSingleOperandTypeChecker
        otcMinusOperator =
        new CompositeOperandTypeChecker(
            CompositeOperandTypeChecker.OR,
            new SqlOperandTypeChecker[] {
                  otcNumericX2,
                  otcIntervalSameX2,
                  otcDatetimeInterval // TODO:  compatibility check
            });

    public static final SqlSingleOperandTypeChecker
        otcMinusDateOperator =
        new FamilyOperandTypeChecker(new SqlTypeFamily [] {
            SqlTypeFamily.Datetime,
            SqlTypeFamily.Datetime,
            SqlTypeFamily.DatetimeInterval
        }) {
            public boolean checkOperandTypes(
                SqlCallBinding callBinding,
                boolean throwOnFailure)
            {
                if (!super.checkOperandTypes(callBinding, throwOnFailure)) {
                    return false;
                }
                if (!otcSameX2.checkOperandTypes(
                        callBinding, throwOnFailure))
                {
                    return false;
                }
                return true;
            }
        };

    public static final SqlSingleOperandTypeChecker
        otcNumericOrInterval =
        new CompositeOperandTypeChecker(
            CompositeOperandTypeChecker.OR,
            new SqlOperandTypeChecker[] {
                  otcNumeric,
                  otcInterval
            });

    public static final SqlSingleOperandTypeChecker
        otcRecordMultiset =
        new SqlSingleOperandTypeChecker()
        {
            public boolean checkSingleOperandType(
                SqlCallBinding callBinding,
                SqlNode node,
                int iFormalOperand,
                boolean throwOnFailure)
            {
                assert(0 == iFormalOperand);
                RelDataType type = callBinding.getValidator().deriveType(
                    callBinding.getScope(), node);
                boolean validationError = false;
                if (!type.isStruct()) {
                    validationError = true;
                } else if (type.getFieldList().size() != 1) {
                    validationError = true;
                } else if (!SqlTypeName.Multiset.equals(
                    type.getFields()[0].getType().getSqlTypeName()))
                {
                    validationError = true;
                }

                if (validationError && throwOnFailure) {
                    throw callBinding.newValidationSignatureError();
                }
                return !validationError;
            }

            public boolean checkOperandTypes(
                SqlCallBinding callBinding,
                boolean throwOnFailure)
            {
                return checkSingleOperandType(
                    callBinding,
                    callBinding.getCall().operands[0],
                    0,
                    throwOnFailure);
            }

            public SqlOperandCountRange getOperandCountRange()
            {
                return SqlOperandCountRange.One;
            }

            public String getAllowedSignatures(SqlOperator op, String opName)
            {
                return "UNNEST(<MULTISET>)";
            }
        };

    public static final SqlSingleOperandTypeChecker
        otcMultisetOrRecordTypeMultiset =
        new CompositeOperandTypeChecker(
            CompositeOperandTypeChecker.OR,
            new SqlOperandTypeChecker[]{
                otcMultiset,otcRecordMultiset});

    public static final SqlOperandTypeChecker
        otcMultisetX2 =
        new MultisetOperandTypeChecker();

    /**
     * Operand type-checking strategy for a set operator (UNION, INTERSECT,
     * EXCEPT).
     */
    public static final SqlOperandTypeChecker
        otcSetop =
        new SetopOperandTypeChecker();
    
    // ----------------------------------------------------------------------
    // SqlReturnTypeInference definitions
    // ----------------------------------------------------------------------
    
    /**
     * Type-inference strategy whereby the result type of a call is the type of
     * the first operand.
     */
    public static final SqlReturnTypeInference
        rtiFirstArgType =
        new OrdinalReturnTypeInference(0);

    /**
     * Type-inference strategy whereby the result type of a call is the type of
     * the first operand, with nulls always allowed.
     */
    public static final SqlReturnTypeInference
        rtiFirstArgTypeForceNullable =
        new SqlTypeTransformCascade(
            rtiFirstArgType, SqlTypeTransforms.forceNullable);

    /**
     * Type-inference strategy whereby the result type of a call is the type of
     * the first operand. If any of the other operands are nullable the returned
     * type will also be nullable.
     */
    public static final SqlReturnTypeInference
        rtiNullableFirstArgType =
        new SqlTypeTransformCascade(
            rtiFirstArgType, SqlTypeTransforms.toNullable);

    public static final SqlReturnTypeInference
        rtiFirstInterval =
        new MatchReturnTypeInference(0, SqlTypeName.timeIntervalTypes);

    public static final SqlReturnTypeInference
        rtiNullableFirstInterval =
        new SqlTypeTransformCascade(
            rtiFirstInterval, SqlTypeTransforms.toNullable);

    /**
     * Type-inference strategy whereby the result type of a call is VARYING
     * the type of the first argument.
     * The length returned is the same as length of the
     * first argument.
     * If any of the other operands are nullable the returned
     * type will also be nullable.
     * First Arg must be of string type.
     */
    public static final SqlReturnTypeInference
        rtiNullableVaryingFirstArgType =
        new SqlTypeTransformCascade(
            rtiFirstArgType,
            SqlTypeTransforms.toNullable,
            SqlTypeTransforms.toVarying);

    /**
     * Type-inference strategy whereby the result type of a call is the type of
     * the second operand.
     */
    public static final SqlReturnTypeInference
        rtiSecondArgType =
        new OrdinalReturnTypeInference(1);

    /**
     * Type-inference strategy whereby the result type of a call is the type of
     * the second operand. If any of the other operands are nullable the returned
     * type will also be nullable.
     */
    public static final SqlReturnTypeInference
        rtiNullableSecondArgType =
        new SqlTypeTransformCascade(
            rtiSecondArgType, SqlTypeTransforms.toNullable);

    /**
     * Type-inference strategy whereby the result type of a call is the type of
     * the third operand.
     */
    public static final SqlReturnTypeInference
        rtiThirdArgType =
        new OrdinalReturnTypeInference(2);

    /**
     * Type-inference strategy whereby the result type of a call is Boolean.
     */
    public static final SqlReturnTypeInference
        rtiBoolean =
        new ExplicitReturnTypeInference(SqlTypeName.Boolean);

    /**
     * Type-inference strategy whereby the result type of a call is Boolean,
     * with nulls allowed if any of the operands allow nulls.
     */
    public static final SqlReturnTypeInference
        rtiNullableBoolean =
        new SqlTypeTransformCascade(
            rtiBoolean, SqlTypeTransforms.toNullable);

    /**
     * Type-inference strategy whereby the result type of a call is Date.
     */
    public static final SqlReturnTypeInference
        rtiDate =
        new ExplicitReturnTypeInference(SqlTypeName.Date);

    /**
     * Type-inference strategy whereby the result type of a call is Time(0).
     */
    public static final SqlReturnTypeInference
        rtiTime =
        new ExplicitReturnTypeInference(SqlTypeName.Time, 0);

    /**
     * Type-inference strategy whereby the result type of a call is nullable
     * Time(0).
     */
     public static final SqlReturnTypeInference
         rtiNullableTime =
        new SqlTypeTransformCascade(
            rtiTime, SqlTypeTransforms.toNullable);

    /**
     * Type-inference strategy whereby the result type of a call is Double.
     */
    public static final SqlReturnTypeInference
        rtiDouble =
        new ExplicitReturnTypeInference(SqlTypeName.Double);

    /**
     * Type-inference strategy whereby the result type of a call is Double
     * with nulls allowed if any of the operands allow nulls.
     */
    public static final SqlReturnTypeInference
        rtiNullableDouble =
        new SqlTypeTransformCascade(
            rtiDouble, SqlTypeTransforms.toNullable);

    /**
     * Type-inference strategy whereby the result type of a call is an Integer.
     */
    public static final SqlReturnTypeInference
        rtiInteger =
        new ExplicitReturnTypeInference(SqlTypeName.Integer);

    /**
     * Type-inference strategy whereby the result type of a call is an Bigint
     */
    public static final SqlReturnTypeInference
        rtiBigint =
        new ExplicitReturnTypeInference(SqlTypeName.Bigint);

    /**
     * Type-inference strategy whereby the result type of a call is an Integer
     * with nulls allowed if any of the operands allow nulls.
     */
    public static final SqlReturnTypeInference
        rtiNullableInteger =
        new SqlTypeTransformCascade(
            rtiInteger, SqlTypeTransforms.toNullable);

    /**
     * Type-inference strategy which always returns "VARCHAR(2000)".
     */
    public static final SqlReturnTypeInference
        rtiVarchar2000 =
        new ExplicitReturnTypeInference(SqlTypeName.Varchar, 2000);

    /**
     * Type-inference strategy which always returns "CURSOR".
     */
    public static final SqlReturnTypeInference
        rtiCursor =
        new ExplicitReturnTypeInference(SqlTypeName.Cursor);

    /**
     * Type-inference strategy whereby the result type of a call is using its
     * operands biggest type, using the SQL:1999 rules described in
     * "Data types of results of aggregations".
     * These rules are used in union, except, intercept, case and other places.
     *
     * @sql.99 Part 2 Section 9.3
     */
    public static final SqlReturnTypeInference
        rtiLeastRestrictive =
        new SqlReturnTypeInference() {
            public RelDataType inferReturnType(
                SqlOperatorBinding opBinding)
            {
                return opBinding.getTypeFactory().leastRestrictive(
                    opBinding.collectOperandTypes());
            }
        };

    /**
     * Type-inference strategy for a call where the first argument is a decimal.
     * The result type of a call is a decimal with a scale of 0, and the same
     * precision and nullibility as the first argument
     */
    public static final SqlReturnTypeInference
        rtiDecimalNoScale =
            new SqlReturnTypeInference()
            {
                public RelDataType inferReturnType(
                    SqlOperatorBinding opBinding)
                {
                    RelDataType type1 = opBinding.getOperandType(0);
                    if (SqlTypeUtil.isDecimal(type1))
                    {
                        if (type1.getScale() == 0) {
                            return type1;
                        } else {
                            int p = type1.getPrecision();
                            RelDataType ret;
                            ret = opBinding.getTypeFactory().createSqlType(
                                SqlTypeName.Decimal, p, 0);
                            if (type1.isNullable()) {
                                ret = opBinding.getTypeFactory().
                                    createTypeWithNullability(ret, true);
                            }
                            return ret;
                        }
                    }
                    return null;
                }
            };

    /**
     * Type-inference strategy whereby the result type of a call is
     * {@link #rtiDecimalNoScale} with a fallback to
     * {@link #rtiFirstArgType}
     *
     * This rule is used for floor, ceiling. 
     */
    public static final SqlReturnTypeInference
        rtiFirstArgTypeOrExactNoScale =
        new SqlReturnTypeInferenceChain(
            new SqlReturnTypeInference[] {
                rtiDecimalNoScale,
                rtiFirstArgType} );

    /**
     * Type-inference strategy whereby the result type of a call is
     * the decimal product of two exact numeric operands where at
     * least one of the operands is a decimal.
     *
     * Let p1, s1 be the precision and scale of the first operand
     * Let p2, s2 be the precision and scale of the second operand
     * Let p, s be the precision and scale of the result,
     *
     * Then the result type is a decimal with:
     * <ul>
     * <li>p = p1 + p2</li>
     * <li>s = s1 + s2</li>
     * </ul>
     * p and s are capped at their maximum values
     *
     * @sql.2003 Part 2 Section 6.26
     */
    public static final SqlReturnTypeInference
        rtiDecimalProduct =
            new SqlReturnTypeInference()
            {
                public RelDataType inferReturnType(
                    SqlOperatorBinding opBinding)
                {
                    RelDataType type1 = opBinding.getOperandType(0);
                    RelDataType type2 = opBinding.getOperandType(1);
                    if (SqlTypeUtil.isExactNumeric(type1)
                            && SqlTypeUtil.isExactNumeric(type2))
                    {
                        if (SqlTypeUtil.isDecimal(type1) || SqlTypeUtil.isDecimal(type2))
                        {
                            int p1 = type1.getPrecision();
                            int p2 = type2.getPrecision();
                            int s1 = type1.getScale();
                            int s2 = type2.getScale();

                            int scale = s1 + s2;
                            scale = Math.min(scale, SqlTypeName.MAX_NUMERIC_SCALE);
                            int precision = p1 + p2;
                            precision = Math.min(precision, SqlTypeName.MAX_NUMERIC_PRECISION);

                            RelDataType ret;
                            ret = opBinding.getTypeFactory().createSqlType(
                                    SqlTypeName.Decimal, precision, scale);

                            return ret;
                        }
                    }

                    return null;
                }
            };

    /**
     * Same as {@link #rtiNullableDecimalProduct} but returns with nullablity
     * if any of the operands is nullable by using
     * {@link SqlTypeTransforms#toNullable}
     */
    public static final SqlReturnTypeInference
        rtiNullableDecimalProduct =
            new SqlTypeTransformCascade(
                rtiDecimalProduct, SqlTypeTransforms.toNullable);

    /**
     * Type-inference strategy whereby the result type of a call is
     * {@link #rtiNullableDecimalProduct} with a fallback to
     * {@link #rtiNullableFirstInterval}  and {@link #rtiLeastRestrictive}
     *
     * These rules are used for multiplication.
     */
    public static final SqlReturnTypeInference
        rtiNullableProduct =
        new SqlReturnTypeInferenceChain(
            new SqlReturnTypeInference[] {
                rtiNullableDecimalProduct,
                rtiNullableFirstInterval,
                rtiLeastRestrictive} );

    /**
     * Type-inference strategy whereby the result type of a call is
     * the decimal product of two exact numeric operands where at
     * least one of the operands is a decimal.
     *
     * Let p1, s1 be the precision and scale of the first operand
     * Let p2, s2 be the precision and scale of the second operand
     * Let p, s be the precision and scale of the result,
     * Let d be the number of whole digits in the result
     *
     * Then the result type is a decimal with:
     * <ul>
     * <li>d = p1 - s1 + s2</li>
     * <li>s <= max(6, s1 + p2 + 1)</li>
     * <li>p = d + s</li>
     * </ul>
     * p and s are capped at their maximum values
     *
     * @sql.2003 Part 2 Section 6.26
     */
    public static final SqlReturnTypeInference
        rtiDecimalQuotient =
            new SqlReturnTypeInference()
            {
                public RelDataType inferReturnType(
                    SqlOperatorBinding opBinding)
                {
                    RelDataType type1 = opBinding.getOperandType(0);
                    RelDataType type2 = opBinding.getOperandType(1);
                    if (SqlTypeUtil.isExactNumeric(type1)
                            && SqlTypeUtil.isExactNumeric(type2))
                    {
                        if (SqlTypeUtil.isDecimal(type1) || SqlTypeUtil.isDecimal(type2))
                        {
                            int p1 = type1.getPrecision();
                            int p2 = type2.getPrecision();
                            int s1 = type1.getScale();
                            int s2 = type2.getScale();

                            int dout = Math.min(p1 - s1 + s2, SqlTypeName.MAX_NUMERIC_PRECISION);

                            int scale = Math.max(6, s1 + p2 + 1);
                            scale = Math.min(scale, SqlTypeName.MAX_NUMERIC_PRECISION - dout);
                            scale = Math.min(scale, SqlTypeName.MAX_NUMERIC_SCALE);

                            int precision = dout + scale;
                            assert(precision <= SqlTypeName.MAX_NUMERIC_PRECISION);
                            assert(precision > 0);

                            RelDataType ret;
                            ret = opBinding.getTypeFactory().createSqlType(
                                    SqlTypeName.Decimal, precision, scale);

                            return ret;
                        }
                    }

                    return null;
                }
            };

    /**
     * Same as {@link #rtiNullableDecimalQuotient} but returns with nullablity
     * if any of the operands is nullable by using
     * {@link SqlTypeTransforms#toNullable}
     */
    public static final SqlReturnTypeInference
        rtiNullableDecimalQuotient =
            new SqlTypeTransformCascade(
                rtiDecimalQuotient, SqlTypeTransforms.toNullable);

    /**
     * Type-inference strategy whereby the result type of a call is
     * {@link #rtiNullableDecimalQuotient} with a fallback to
     * {@link #rtiNullableFirstInterval}  and {@link #rtiLeastRestrictive}
     *
     * These rules are used for division.
     */
    public static final SqlReturnTypeInference
        rtiNullableQuotient =
        new SqlReturnTypeInferenceChain(
            new SqlReturnTypeInference[] {
                rtiNullableDecimalQuotient,
                rtiNullableFirstInterval,
                rtiLeastRestrictive} );

    /**
     * Type-inference strategy whereby the result type of a call is
     * the decimal sum of two exact numeric operands where at
     * least one of the operands is a decimal.
     *
     * Let p1, s1 be the precision and scale of the first operand
     * Let p2, s2 be the precision and scale of the second operand
     * Let p, s be the precision and scale of the result,
     *
     * Then the result type is a decimal with:
     * <ul>
     * <li>s = max(s1, s2)</li>
     * <li>p = max(p1 - s1, p2 - s2) + s + 1</li>
     * </ul>
     * p and s are capped at their maximum values
     *
     * @sql.2003 Part 2 Section 6.26
     */
    public static final SqlReturnTypeInference
        rtiDecimalSum =
            new SqlReturnTypeInference()
            {
                public RelDataType inferReturnType(
                    SqlOperatorBinding opBinding)
                {
                    RelDataType type1 = opBinding.getOperandType(0);
                    RelDataType type2 = opBinding.getOperandType(1);
                    if (SqlTypeUtil.isExactNumeric(type1)
                            && SqlTypeUtil.isExactNumeric(type2))
                    {
                        if (SqlTypeUtil.isDecimal(type1) || SqlTypeUtil.isDecimal(type2))
                        {
                            int p1 = type1.getPrecision();
                            int p2 = type2.getPrecision();
                            int s1 = type1.getScale();
                            int s2 = type2.getScale();

                            int scale = Math.max(s1, s2);
                            assert(scale <= SqlTypeName.MAX_NUMERIC_SCALE);
                            int precision = Math.max(p1-s1, p2-s2) + scale + 1;
                            precision = Math.min(precision, SqlTypeName.MAX_NUMERIC_PRECISION);
                            assert(precision > 0);

                            RelDataType ret;
                            ret = opBinding.getTypeFactory().createSqlType(
                                    SqlTypeName.Decimal, precision, scale);

                            return ret;
                        }
                    }

                    return null;
                }
            };

    /**
     * Same as {@link #rtiNullableDecimalSum} but returns with nullablity
     * if any of the operands is nullable by using
     * {@link SqlTypeTransforms#toNullable}
     */
    public static final SqlReturnTypeInference
        rtiNullableDecimalSum =
            new SqlTypeTransformCascade(
                rtiDecimalSum, SqlTypeTransforms.toNullable);

    /**
     * Type-inference strategy whereby the result type of a call is
     * {@link #rtiNullableDecimalSum} with a fallback to
     * {@link #rtiLeastRestrictive}
     *
     * These rules are used for addition and subtraction.
     */
    public static final SqlReturnTypeInference
        rtiNullableSum =
            new SqlReturnTypeInferenceChain(
                    rtiNullableDecimalSum,
                    rtiLeastRestrictive);

    /**
     * Type-inference strategy whereby the
     * result type of a call is
     * <ul>
     * <li>the same type as the input types but with the
     * combined length of the two first types</li>
     * <li>if types are of char type the type with the highest coercibility
     * will be used</li>
     * <li>result is varying if either input is; otherwise fixed
     *</ul>
     *
     * @pre input types must be of the same string type
     * @pre types must be comparable without casting
     */
    public static final SqlReturnTypeInference
        rtiDyadicStringSumPrecision =
        new SqlReturnTypeInference()
        {
            /**
             * @pre SqlTypeUtil.sameNamedType(argTypes[0], (argTypes[1]))
             */
            public RelDataType inferReturnType(
                SqlOperatorBinding opBinding)
            {
                if (!(SqlTypeUtil.inCharFamily(opBinding.getOperandType(0))
                        && SqlTypeUtil.inCharFamily(
                            opBinding.getOperandType(1))))
                {
                    Util.pre(
                        SqlTypeUtil.sameNamedType(
                            opBinding.getOperandType(0),
                            opBinding.getOperandType(1)),
                        "SqlTypeUtil.sameNamedType(argTypes[0], argTypes[1])");
                }
                SqlCollation pickedCollation = null;
                if (SqlTypeUtil.inCharFamily(opBinding.getOperandType(0))) {
                    if (!SqlTypeUtil.isCharTypeComparable(
                            opBinding.collectOperandTypes(), 0, 1)) {
                        throw EigenbaseResource.instance()
                            .TypeNotComparable.ex(
                                opBinding.getOperandType(0).toString(),
                                opBinding.getOperandType(1).toString());
                    }

                    pickedCollation =
                        SqlCollation.getCoercibilityDyadicOperator(
                            opBinding.getOperandType(0).getCollation(),
                            opBinding.getOperandType(1).getCollation());
                    assert (null != pickedCollation);
                }

                // Determine whether result is variable-length
                SqlTypeName typeName =
                    opBinding.getOperandType(0).getSqlTypeName();
                if (SqlTypeUtil.isBoundedVariableWidth(
                        opBinding.getOperandType(1)))
                {
                    typeName =
                        opBinding.getOperandType(1).getSqlTypeName();
                }

                RelDataType ret;
                ret = opBinding.getTypeFactory().createSqlType(
                        typeName,
                        opBinding.getOperandType(0).getPrecision()
                        + opBinding.getOperandType(1).getPrecision());
                if (null != pickedCollation) {
                    RelDataType pickedType;
                    if (opBinding.getOperandType(0).getCollation().equals(
                            pickedCollation))
                    {
                        pickedType = opBinding.getOperandType(0);
                    } else if (opBinding.getOperandType(
                                   1).getCollation().equals(
                                       pickedCollation))
                    {
                        pickedType = opBinding.getOperandType(1);
                    } else {
                        throw Util.newInternal("should never come here");
                    }
                    ret = opBinding.getTypeFactory().
                        createTypeWithCharsetAndCollation(
                            ret,
                            pickedType.getCharset(),
                            pickedType.getCollation());
                }
                return ret;
            }
        };

    /**
     * Same as {@link #rtiDyadicStringSumPrecision} and using
     * {@link SqlTypeTransforms#toNullable}
     */
    public static final SqlReturnTypeInference
        rtiNullableDyadicStringSumPrecision =
        new SqlTypeTransformCascade(
            rtiDyadicStringSumPrecision,
            new SqlTypeTransform [] { SqlTypeTransforms.toNullable });

    /**
     * Same as {@link #rtiDyadicStringSumPrecision} and using
     * {@link SqlTypeTransforms#toNullable},
     * {@link SqlTypeTransforms#toVarying}.
     */
    public static final SqlReturnTypeInference
        rtiNullableVaryingDyadicStringSumPrecision =
        new SqlTypeTransformCascade(
            rtiDyadicStringSumPrecision,
            new SqlTypeTransform [] {
                SqlTypeTransforms.toNullable, SqlTypeTransforms.toVarying
            });

    /**
     * Type-inference strategy where the expression is assumed to be registered
     * as a {@link org.eigenbase.sql.validate.SqlValidatorNamespace}, and
     * therefore the result type of the call is the type of that namespace.
     */
    public static final SqlReturnTypeInference
        rtiScope =
        new SqlReturnTypeInference()
        {
            public RelDataType inferReturnType(
                SqlOperatorBinding opBinding)
            {
                SqlCallBinding callBinding = (SqlCallBinding) opBinding;
                return callBinding.getValidator().getNamespace(
                    callBinding.getCall()).getRowType();
            }
        };

    /**
     * Returns the same type as the multiset carries. The multiset type returned
     * is the least restrictive of the call's multiset operands
     */
    public static final SqlReturnTypeInference
        rtiMultiset =
        new SqlReturnTypeInference()
        {
            public RelDataType inferReturnType(
                SqlOperatorBinding opBinding)
            {
                RelDataType[] argElementTypes =
                    new RelDataType[opBinding.getOperandCount()];
                for (int i = 0; i < opBinding.getOperandCount(); i++) {
                    argElementTypes[i] =
                        opBinding.getOperandType(i).getComponentType();
                    assert argElementTypes[i] != null;
                }

                ExplicitOperatorBinding newBinding = new
                    ExplicitOperatorBinding(opBinding, argElementTypes);
                RelDataType biggestElementType =
                    rtiLeastRestrictive.inferReturnType(newBinding);
                return opBinding.getTypeFactory().createMultisetType(
                    biggestElementType, -1);
            }
        };

    /**
     * Returns a multiset of the first column of a multiset. For example,
     * given <code>RECORD(x INTEGER, y DATE) MULTISET</code>, returns
     * <code>INTEGER MULTISET</code>.
     */
    public static final SqlReturnTypeInference
        rtiMultisetFirstColumnMultiset =
        new SqlReturnTypeInference()
        {
            public RelDataType inferReturnType(
                SqlOperatorBinding opBinding)
            {
                assert opBinding.getOperandCount() == 1;
                final RelDataType recordMultisetType =
                    opBinding.getOperandType(0);
                RelDataType multisetType =
                    recordMultisetType.getComponentType();
                assert multisetType != null :
                    "expected a multiset type: " + recordMultisetType;
                final RelDataTypeField[] fields = multisetType.getFields();
                assert fields.length > 0;
                final RelDataType firstColType = fields[0].getType();
                return opBinding.getTypeFactory().createMultisetType(
                    firstColType, -1);
            }
        };

    /**
     * Returns a multiset of the first column of a multiset. For example,
     * given <code>INTEGER MULTISET</code>,
     * returns <code>RECORD(x INTEGER) MULTISET</code>.
     */
    public static final SqlReturnTypeInference
        rtiMultisetRecordMultiset =
        new SqlReturnTypeInference()
        {
            public RelDataType inferReturnType(
                SqlOperatorBinding opBinding)
            {
                assert opBinding.getOperandCount() == 1;
                final RelDataType multisetType = opBinding.getOperandType(0);
                RelDataType componentType = multisetType.getComponentType();
                assert componentType != null :
                    "expected a multiset type: " + multisetType;
                return opBinding.getTypeFactory().createMultisetType(
                    opBinding.getTypeFactory().createStructType(
                        new RelDataType[] {componentType},
                        new String[] {"EXPR$0"}
                    ),
                    -1);
            }
        };

    /**
     * Returns the type of the only column of a multiset.
     *
     * <p>For example, given <code>RECORD(x INTEGER) MULTISET</code>,
     * returns <code>INTEGER MULTISET</code>.
     */
    public static final SqlReturnTypeInference
        rtiMultisetOnlyColumn =
        new SqlTypeTransformCascade(
            rtiMultiset, SqlTypeTransforms.onlyColumn);
    
    /**
     * Same as {@link #rtiMultiset} but returns with nullablity
     * if any of the operands is nullable
     */
    public static final SqlReturnTypeInference
        rtiNullableMultiset =
        new SqlTypeTransformCascade(
            rtiMultiset, SqlTypeTransforms.toNullable);

    /**
     * Returns the element type of a multiset
     */
    public static final SqlReturnTypeInference
        rtiNullableMultisetElementType =
        new SqlTypeTransformCascade(
            rtiMultiset, SqlTypeTransforms.toMultisetElementType);
    
    // ----------------------------------------------------------------------
    // SqlOperandTypeInference definitions
    // ----------------------------------------------------------------------
    
    /**
     * Operand type-inference strategy where an unknown operand
     * type is derived from the first operand with a known type.
     */
    public static final SqlOperandTypeInference
        otiFirstKnown =
        new SqlOperandTypeInference()
        {
            public void inferOperandTypes(
                SqlCallBinding callBinding,
                RelDataType returnType,
                RelDataType [] operandTypes)
            {
                SqlNode [] operands = callBinding.getCall().getOperands();
                final RelDataType unknownType =
                    callBinding.getValidator().getUnknownType();
                RelDataType knownType = unknownType;
                for (int i = 0; i < operands.length; ++i) {
                    knownType = callBinding.getValidator().deriveType(
                        callBinding.getScope(), operands[i]);
                    if (!knownType.equals(unknownType)) {
                        break;
                    }
                }
                assert !knownType.equals(unknownType);
                for (int i = 0; i < operandTypes.length; ++i) {
                    operandTypes[i] = knownType;
                }
            }
        };
    
    /**
     * Operand type-inference strategy where an unknown operand
     * type is derived from the call's return type.  If the return type is
     * a record, it must have the same number of fields as the number
     * of operands.
     */
    public static final SqlOperandTypeInference
        otiReturnType =
        new SqlOperandTypeInference()
        {
            public void inferOperandTypes(
                SqlCallBinding callBinding,
                RelDataType returnType,
                RelDataType [] operandTypes)
            {
                for (int i = 0; i < operandTypes.length; ++i) {
                    if (returnType.isStruct()) {
                        operandTypes[i] = returnType.getFields()[i].getType();
                    } else {
                        operandTypes[i] = returnType;
                    }
                }
            }
        };
    
    /**
     * Operand type-inference strategy where an unknown operand
     * type is assumed to be boolean.
     */
    public static final SqlOperandTypeInference
        otiBoolean =
        new SqlOperandTypeInference()
        {
            public void inferOperandTypes(
                SqlCallBinding callBinding,
                RelDataType returnType,
                RelDataType [] operandTypes)
            {
                RelDataTypeFactory typeFactory = callBinding.getTypeFactory();
                for (int i = 0; i < operandTypes.length; ++i) {
                    operandTypes[i] =
                        typeFactory.createSqlType(SqlTypeName.Boolean);
                }
            }
        };
}

// End SqlTypeStrategies.java
