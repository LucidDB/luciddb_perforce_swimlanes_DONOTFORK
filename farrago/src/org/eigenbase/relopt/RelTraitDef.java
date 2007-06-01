/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2005-2005 The Eigenbase Project
// Copyright (C) 2002-2005 Disruptive Tech
// Copyright (C) 2005-2005 LucidEra, Inc.
// Portions Copyright (C) 2003-2005 John V. Sichi
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
package org.eigenbase.relopt;

import java.lang.ref.*;

import java.util.*;

import org.eigenbase.rel.*;
import org.eigenbase.rel.convert.*;


/**
 * RelTraitDef represents a class of {@link RelTrait}s. Implementations of
 * RelTraitDef may be singletons under the following conditions:
 *
 * <ol>
 * <li>if the set of all possible associated RelTraits is finite and fixed (e.g.
 * all RelTraits for this RelTraitDef are known at compile time). For example,
 * the CallingConvention trait meets this requirement, because CallingConvention
 * is effectively an enumeration.</li>
 * <li>Either
 *
 * <ul>
 * <li> {@link #canConvert(RelOptPlanner, RelTrait, RelTrait)} and {@link
 * #convert(RelOptPlanner, RelNode, RelTrait, boolean)} do not require
 * planner-instance-specific information, <b>or</b></li>
 * <li>the RelTraitDef manages separate sets of conversion data internally. See
 * {@link CallingConventionTraitDef} for an example of this.</li>
 * </ul>
 * </li>
 * </ol>
 *
 * <p>Otherwise, a new instance of RelTraitDef must be constructed and
 * registered with each new planner instantiated.</p>
 *
 * @author Stephan Zuercher
 * @version $Id$
 */
public abstract class RelTraitDef
{
    //~ Instance fields --------------------------------------------------------

    private final WeakHashMap<RelTrait, WeakReference<RelTrait>> canonicalMap;

    //~ Constructors -----------------------------------------------------------

    public RelTraitDef()
    {
        this.canonicalMap =
            new WeakHashMap<RelTrait, WeakReference<RelTrait>>();
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * @return the specific RelTrait type associated with this RelTraitDef.
     */
    public abstract Class getTraitClass();

    /**
     * @return a simple name for this RelTraitDef (for use in {@link
     * org.eigenbase.rel.RelNode#explain(RelOptPlanWriter)}).
     */
    public abstract String getSimpleName();

    /**
     * Takes an arbitrary RelTrait and returns the canonical representation of
     * that RelTrait. Canonized RelTrait objects may always be compared using
     * the equality operator (<code>==</code>).
     *
     * <p>If an equal RelTrait has already been canonized and is still in use,
     * it will be returned. Otherwise, the given RelTrait is made canonical and
     * returned.
     *
     * @param trait a possibly non-canonical RelTrait
     *
     * @return a canonical RelTrait.
     */
    public final RelTrait canonize(RelTrait trait)
    {
        assert (getTraitClass().isInstance(trait)) : getClass().getName()
            + " cannot canonize a "
            + trait.getClass().getName();

        if (canonicalMap.containsKey(trait)) {
            WeakReference<RelTrait> canonicalTraitRef = canonicalMap.get(trait);
            if (canonicalTraitRef != null) {
                // Make sure the canonical trait didn't disappear between
                // containsKey and get.
                RelTrait canonicalTrait = canonicalTraitRef.get();
                if (canonicalTrait != null) {
                    // Make sure the canonical trait didn't disappear between
                    // WeakHashMap.get() and WeakReference.get()
                    return canonicalTrait;
                }
            }
        }

        // Canonical trait wasn't in map or was *very* recently removed from
        // the map. Removal, however, indicates that no other references to
        // the canonical trait existed, so the caller's trait becomes
        // canonical.
        canonicalMap.put(
            trait,
            new WeakReference<RelTrait>(trait));

        return trait;
    }

    /**
     * Converts the given RelNode to the given RelTrait.
     *
     * @param planner the planner requesting the conversion
     * @param rel RelNode to convert
     * @param toTrait RelTrait to convert to
     * @param allowInfiniteCostConverters flag indicating whether infinite cost
     * converters are allowe
     *
     * @return a converted RelNode or null if conversion is not possible
     */
    public abstract RelNode convert(
        RelOptPlanner planner,
        RelNode rel,
        RelTrait toTrait,
        boolean allowInfiniteCostConverters);

    /**
     * Tests whether the given RelTrait can be converted to another RelTrait.
     *
     * @param planner the planner requesting the conversion test
     * @param fromTrait the RelTrait to convert from
     * @param toTrait the RelTrait to conver to
     *
     * @return true if fromTrait can be converted to toTrait
     */
    public abstract boolean canConvert(
        RelOptPlanner planner,
        RelTrait fromTrait,
        RelTrait toTrait);

    /**
     * Provides notification of the registration of a particular {@link
     * ConverterRule} with a {@link RelOptPlanner}. The default implementation
     * does nothing.
     *
     * @param planner the planner registering the rule
     * @param converterRule the registered converter rule
     */
    public void registerConverterRule(
        RelOptPlanner planner,
        ConverterRule converterRule)
    {
    }

    /**
     * Provides notification that a particular {@link ConverterRule} has been
     * deregistered from a {@link RelOptPlanner}. The default implementation
     * does nothing.
     *
     * @param planner the planner registering the rule
     * @param converterRule the registered converter rule
     */
    public void deregisterConverterRule(
        RelOptPlanner planner,
        ConverterRule converterRule)
    {
    }
}

// End RelTraitDef.java
