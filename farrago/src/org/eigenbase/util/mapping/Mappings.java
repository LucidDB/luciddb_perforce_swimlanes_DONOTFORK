/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2006-2009 The Eigenbase Project
// Copyright (C) 2006-2009 SQLstream, Inc.
// Copyright (C) 2006-2009 LucidEra, Inc.
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
package org.eigenbase.util.mapping;

import java.util.*;

import org.eigenbase.util.*;


/**
 * Utility functions related to mappings.
 *
 * @author jhyde
 * @version $Id$
 * @see MappingType
 * @see Mapping
 * @see Permutation
 * @since Mar 24, 2006
 */
public abstract class Mappings
{
    //~ Constructors -----------------------------------------------------------

    private Mappings()
    {
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Creates a mapping with required properties.
     */
    public static Mapping create(
        MappingType mappingType,
        int sourceCount,
        int targetCount)
    {
        switch (mappingType) {
        case Bijection:
            assert sourceCount == targetCount;
            return new Permutation(sourceCount);
        case InverseSurjection:
        case PartialSurjection:
            return new Mappings.PartialMapping(
                sourceCount,
                targetCount,
                mappingType);
        case PartialFunction:
        case Function:
            return new PartialFunctionImpl(
                sourceCount,
                targetCount,
                mappingType);
        case InverseFunction:
        case InversePartialFunction:
            return new InverseMapping(
                create(mappingType.inverse(), targetCount, sourceCount));
        default:
            throw Util.needToImplement(
                "no known implementation for mapping type " + mappingType);
        }
    }

    //~ Inner Interfaces -------------------------------------------------------

    /**
     * Mapping where every source has a target. But:
     *
     * <ul>
     * <li>A target may not have a source.
     * <li>May not be finite.
     * </ul>
     */
    public static interface FunctionMapping
    {
        /**
         * Returns the target that a source maps to, or -1 if it is not mapped.
         */
        int getTargetOpt(int source);

        /**
         * Returns the target that a source maps to.
         *
         * @param source source
         *
         * @return target
         *
         * @throws IndexOutOfBoundsException if source is not mapped
         */
        int getTarget(int source);

        MappingType getMappingType();

        int getSourceCount();
    }

    /**
     * Mapping suitable for sourcing columns.
     *
     * <p>Properties:
     *
     * <ul>
     * <li>It has a finite number of sources and targets
     * <li>Each target has exactly one source
     * <li>Each source has at most one target
     * </ul>
     *
     * <p>TODO: figure out which interfaces this should extend
     */
    public static interface SourceMapping
    {
        int getSourceCount();

        int getSource(int target);

        int getSourceOpt(int target);

        int getTargetCount();

        int getTargetOpt(int source);

        MappingType getMappingType();

        boolean isIdentity();

        Mapping inverse();
    }

    /**
     * Mapping suitable for mapping columns to a target.
     *
     * <p>Properties:
     *
     * <ul>
     * <li>It has a finite number of sources and targets
     * <li>Each target has at most one source
     * <li>Each source has exactly one target
     * </ul>
     *
     * <p>TODO: figure out which interfaces this should extend
     */
    public static interface TargetMapping
    {
        int getSourceCount();

        int getSourceOpt(int target);

        int getTargetCount();

        int getTarget(int target);

        int getTargetOpt(int source);

        void set(int source, int target);

        MappingType getMappingType();

        Mapping inverse();
    }

    //~ Inner Classes ----------------------------------------------------------

    public static abstract class AbstractMapping
        implements Mapping
    {
        public void set(int source, int target)
        {
            throw new UnsupportedOperationException();
        }

        public int getTargetOpt(int source)
        {
            throw new UnsupportedOperationException();
        }

        public int getTarget(int source)
        {
            int target = getTargetOpt(source);
            if (target == -1) {
                throw new IndexOutOfBoundsException(
                    "source #" + source + " has no target in mapping "
                    + toString());
            }
            return target;
        }

        public int getSourceOpt(int target)
        {
            throw new UnsupportedOperationException();
        }

        public int getSource(int target)
        {
            int source = getSourceOpt(target);
            if (source == -1) {
                throw new IndexOutOfBoundsException(
                    "target #" + source + " has no source in mapping "
                    + toString());
            }
            return source;
        }

        public int getSourceCount()
        {
            throw new UnsupportedOperationException();
        }

        public int getTargetCount()
        {
            throw new UnsupportedOperationException();
        }

        public boolean isIdentity()
        {
            int sourceCount = getSourceCount();
            int targetCount = getTargetCount();
            if (sourceCount != targetCount) {
                return false;
            }
            for (int i = 0; i < sourceCount; i++) {
                if (getSource(i) != i) {
                    return false;
                }
            }
            return true;
        }
    }

    public static abstract class FiniteAbstractMapping
        extends AbstractMapping
    {
        public Iterator<IntPair> iterator()
        {
            return new FunctionMappingIter(this);
        }

        /**
         * Returns a string representation of this mapping.
         *
         * <p>For example, the mapping
         *
         * <table border="1">
         * <tr>
         * <th>source</th>
         * <td>0</td>
         * <td>1</td>
         * <td>2</td>
         * </tr>
         * <tr>
         * <th>target</th>
         * <td>-1</td>
         * <td>3</td>
         * <td>2</td>
         * </tr>
         * </table>
         *
         * <table border="1">
         * <tr>
         * <th>target</th>
         * <td>0</td>
         * <td>1</td>
         * <td>2</td>
         * <td>3</td>
         * </tr>
         * <tr>
         * <th>source</th>
         * <td>-1</td>
         * <td>-1</td>
         * <td>2</td>
         * <td>1</td>
         * </tr>
         * </table>
         *
         * is represented by the string "[1:3, 2:2]".
         *
         * <p>This method relies upon the optional method {@link #iterator()}.
         */
        public String toString()
        {
            StringBuilder buf = new StringBuilder();
            buf.append("[");
            int i = 0;
            for (final Iterator<IntPair> iter = iterator(); iter.hasNext();) {
                IntPair pair = iter.next();
                if (i++ > 0) {
                    buf.append(", ");
                }
                buf.append(pair.source).append(':').append(pair.target);
            }
            buf.append("]");
            return buf.toString();
        }

        public int hashCode()
        {
            // not very efficient
            return toString().hashCode();
        }

        public boolean equals(Object obj)
        {
            // not very efficient
            return (obj instanceof Permutation)
                && toString().equals(obj.toString());
        }
    }

    static class FunctionMappingIter
        implements Iterator<IntPair>
    {
        private int i = 0;
        private final FunctionMapping mapping;

        FunctionMappingIter(FunctionMapping mapping)
        {
            this.mapping = mapping;
        }

        public boolean hasNext()
        {
            return (i < mapping.getSourceCount())
                || (mapping.getSourceCount() == -1);
        }

        public IntPair next()
        {
            int x = i++;
            return new IntPair(
                x,
                mapping.getTarget(x));
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Thrown when a mapping is expected to return one element but returns
     * several.
     */
    public static class TooManyElementsException
        extends RuntimeException
    {
    }

    /**
     * Thrown when a mapping is expected to return one element but returns none.
     */
    public static class NoElementException
        extends RuntimeException
    {
    }

    /**
     * A mapping where a source at most one target, and every target has at most
     * one source.
     */
    public static class PartialMapping
        extends FiniteAbstractMapping
        implements Mapping,
            FunctionMapping,
            TargetMapping
    {
        protected final int [] sources;
        protected final int [] targets;
        private final MappingType mappingType;

        /**
         * Creates a partial mapping.
         *
         * <p>Initially, no element is mapped to any other:
         *
         * <table border="1">
         * <tr>
         * <th>source</th>
         * <td>0</td>
         * <td>1</td>
         * <td>2</td>
         * </tr>
         * <tr>
         * <th>target</th>
         * <td>-1</td>
         * <td>-1</td>
         * <td>-1</td>
         * </tr>
         * </table>
         *
         * <table border="1">
         * <tr>
         * <th>target</th>
         * <td>0</td>
         * <td>1</td>
         * <td>2</td>
         * <td>3</td>
         * </tr>
         * <tr>
         * <th>source</th>
         * <td>-1</td>
         * <td>-1</td>
         * <td>-1</td>
         * <td>-1</td>
         * </tr>
         * </table>
         *
         * @param sourceCount Number of source elements
         * @param targetCount Number of target elements
         * @param mappingType Mapping type; must not allow multiple sources per
         * target or multiple targets per source
         */
        public PartialMapping(
            int sourceCount,
            int targetCount,
            MappingType mappingType)
        {
            this.mappingType = mappingType;
            assert mappingType.isSingleSource() : mappingType;
            assert mappingType.isSingleTarget() : mappingType;
            this.sources = new int[targetCount];
            this.targets = new int[sourceCount];
            Arrays.fill(sources, -1);
            Arrays.fill(targets, -1);
        }

        /**
         * Creates a partial mapping from a list. For example, <code>
         * PartialMapping({1, 2, 4}, 6)</code> creates the mapping
         *
         * <table border="1">
         * <tr>
         * <th>source</th>
         * <td>0</td>
         * <td>1</td>
         * <td>2</td>
         * <td>3</td>
         * <td>4</td>
         * <td>5</td>
         * </tr>
         * <tr>
         * <th>target</th>
         * <td>-1</td>
         * <td>0</td>
         * <td>1</td>
         * <td>-1</td>
         * <td>2</td>
         * <td>-1</td>
         * </tr>
         * </table>
         *
         * @param sourceList List whose i'th element is the source of target #i
         * @param sourceCount Number of elements in the source domain
         * @param mappingType Mapping type, must be {@link
         * org.eigenbase.util.mapping.MappingType#PartialSurjection} or
         * stronger.
         */
        public PartialMapping(
            List<Integer> sourceList,
            int sourceCount,
            MappingType mappingType)
        {
            this.mappingType = mappingType;
            assert mappingType.isSingleSource();
            assert mappingType.isSingleTarget();
            int targetCount = sourceList.size();
            this.targets = new int[sourceCount];
            this.sources = new int[targetCount];
            Arrays.fill(sources, -1);
            for (int i = 0; i < sourceList.size(); ++i) {
                final int source = sourceList.get(i);
                sources[i] = source;
                if (source >= 0) {
                    targets[source] = i;
                } else {
                    assert !this.mappingType.isMandatorySource();
                }
            }
        }

        private PartialMapping(
            int [] sources,
            int [] targets,
            MappingType mappingType)
        {
            this.sources = sources;
            this.targets = targets;
            this.mappingType = mappingType;
        }

        public MappingType getMappingType()
        {
            return mappingType;
        }

        public int getSourceCount()
        {
            return targets.length;
        }

        public int getTargetCount()
        {
            return sources.length;
        }

        public Mapping inverse()
        {
            return new PartialMapping(
                targets.clone(),
                sources.clone(),
                mappingType.inverse());
        }

        public Iterator<IntPair> iterator()
        {
            return new MappingItr();
        }

        protected boolean isValid()
        {
            assertPartialValid(this.sources, this.targets);
            assertPartialValid(this.targets, this.sources);
            return true;
        }

        private static void assertPartialValid(int [] sources, int [] targets)
        {
            for (int i = 0; i < sources.length; i++) {
                final int source = sources[i];
                assert source >= -1;
                assert source < targets.length;
                assert (source == -1) || (targets[source] == i);
            }
        }

        public void set(int source, int target)
        {
            assert isValid();
            final int prevTarget = targets[source];
            targets[source] = target;
            final int prevSource = sources[target];
            sources[target] = source;
            if (prevTarget != -1) {
                sources[prevTarget] = prevSource;
            }
            if (prevSource != -1) {
                targets[prevSource] = prevTarget;
            }
            assert isValid();
        }

        public int getSourceOpt(int target)
        {
            return sources[target];
        }

        public int getTargetOpt(int source)
        {
            return targets[source];
        }

        public int getTarget(int source)
        {
            int target = getTargetOpt(source);
            if (target == -1) {
                throw new IndexOutOfBoundsException(
                    "source #" + source + " has no target in mapping "
                    + toString());
            }
            return target;
        }

        public boolean isIdentity()
        {
            if (sources.length != targets.length) {
                return false;
            }
            for (int i = 0; i < sources.length; i++) {
                int source = sources[i];
                if (source != i) {
                    return false;
                }
            }
            return true;
        }

        private class MappingItr
            implements Iterator<IntPair>
        {
            int i = 0;

            public boolean hasNext()
            {
                return i < targets.length;
            }

            public IntPair next()
            {
                int x = i++;
                return new IntPair(x, targets[x]);
            }

            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        }
    }

    /**
     * A surjection with inverse has precisely one source for each target.
     * (Whereas a general surjection has at least one source for each target.)
     * Every source has at most one target.
     *
     * <p>If you call {@link #set} on a target, the target's previous source
     * will be lost.
     */
    static class SurjectionWithInverse
        extends PartialMapping
    {
        SurjectionWithInverse(int sourceCount, int targetCount)
        {
            super(sourceCount, targetCount, MappingType.InverseSurjection);
        }

        /**
         * Creates a mapping between a source and a target.
         *
         * <p>It is an error to map a target to a source which already has a
         * target.
         *
         * <p>If you map a source to a target which already has a source, the
         * old source becomes an orphan.
         *
         * @param source source
         * @param target target
         */
        public void set(int source, int target)
        {
            assert isValid();
            final int prevTarget = targets[source];
            if (prevTarget != -1) {
                throw new IllegalArgumentException(
                    "source #" + source
                    + " is already mapped to target #" + target);
            }
            targets[source] = target;
            final int prevSource = sources[target];
            sources[target] = source;
            sources[prevTarget] = prevSource;
        }

        public int getSource(int target)
        {
            return sources[target];
        }
    }

    public static class IdentityMapping
        extends AbstractMapping
        implements FunctionMapping,
            TargetMapping,
            SourceMapping
    {
        private final int size;

        /**
         * Creates an identity mapping.
         *
         * @param size Size, or -1 if infinite.
         */
        public IdentityMapping(int size)
        {
            this.size = size;
        }

        public Mapping inverse()
        {
            return this;
        }

        public boolean isIdentity()
        {
            return true;
        }

        public void set(int source, int target)
        {
            throw new UnsupportedOperationException();
        }

        public MappingType getMappingType()
        {
            return MappingType.Bijection;
        }

        public int getSourceCount()
        {
            return size;
        }

        public int getTargetCount()
        {
            return size;
        }

        public int getTarget(int source)
        {
            return source;
        }

        public int getTargetOpt(int source)
        {
            return source;
        }

        public int getSource(int target)
        {
            return target;
        }

        public int getSourceOpt(int target)
        {
            return target;
        }

        public Iterator<IntPair> iterator()
        {
            return new Iterator<IntPair>() {
                int i = 0;

                public boolean hasNext()
                {
                    return (size < 0) || (i < size);
                }

                public IntPair next()
                {
                    int x = i++;
                    return new IntPair(x, x);
                }

                public void remove()
                {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    public static class OverridingSourceMapping
        extends AbstractMapping
        implements SourceMapping
    {
        private final SourceMapping parent;
        private final int source;
        private final int target;

        public OverridingSourceMapping(
            SourceMapping parent,
            int source,
            int target)
        {
            this.parent = parent;
            this.source = source;
            this.target = target;
        }

        public Mapping inverse()
        {
            return new OverridingTargetMapping(
                (TargetMapping) parent.inverse(),
                target,
                source);
        }

        public MappingType getMappingType()
        {
            // FIXME: Mapping type might be weaker than parent.
            return parent.getMappingType();
        }

        public int getSource(int target)
        {
            if (target == this.target) {
                return this.source;
            } else {
                return parent.getSource(target);
            }
        }

        public boolean isIdentity()
        {
            // FIXME: It's possible that parent was not the identity but that
            // this overriding fixed it.
            return (source == target)
                && parent.isIdentity();
        }

        public Iterator<IntPair> iterator()
        {
            throw Util.needToImplement(this);
        }
    }

    public static class OverridingTargetMapping
        extends AbstractMapping
        implements TargetMapping
    {
        private final TargetMapping parent;
        private final int target;
        private final int source;

        public OverridingTargetMapping(
            TargetMapping parent,
            int target,
            int source)
        {
            this.parent = parent;
            this.target = target;
            this.source = source;
        }

        public void set(int source, int target)
        {
            parent.set(source, target);
        }

        public Mapping inverse()
        {
            return new OverridingSourceMapping(
                parent.inverse(),
                source,
                target);
        }

        public MappingType getMappingType()
        {
            // FIXME: Mapping type might be weaker than parent.
            return parent.getMappingType();
        }

        public boolean isIdentity()
        {
            // FIXME: Possible that parent is not identity but this overriding
            // fixes it.
            return (source == target)
                && ((Mapping) parent).isIdentity();
        }

        public int getTarget(int source)
        {
            if (source == this.source) {
                return this.target;
            } else {
                return parent.getTarget(source);
            }
        }

        public Iterator<IntPair> iterator()
        {
            throw Util.needToImplement(this);
        }
    }

    /**
     * Implementation of {@link Mapping} where a source can have at most one
     * target, and a target can have any number of sources. The source count
     * must be finite, but the target count may be infinite.
     *
     * <p>The implementation uses an array for the forward-mapping, but does not
     * store the backward mapping.
     */
    private static class PartialFunctionImpl
        extends AbstractMapping
        implements TargetMapping
    {
        private final int sourceCount;
        private final int targetCount;
        private final MappingType mappingType;
        private final int [] targets;

        public PartialFunctionImpl(
            int sourceCount,
            int targetCount,
            MappingType mappingType)
        {
            super();
            if (sourceCount < 0) {
                throw new IllegalArgumentException("Sources must be finite");
            }
            this.sourceCount = sourceCount;
            this.targetCount = targetCount;
            this.mappingType = mappingType;
            if (!mappingType.isSingleTarget()) {
                throw new IllegalArgumentException(
                    "Must have at most one target");
            }
            this.targets = new int[sourceCount];
            Arrays.fill(targets, -1);
        }

        public int getSourceCount()
        {
            return sourceCount;
        }

        public int getTargetCount()
        {
            return targetCount;
        }

        public Iterator<IntPair> iterator()
        {
            return new Iterator<IntPair>() {
                int i = -1;

                {
                    advance();
                }

                private void advance()
                {
                    while (true) {
                        ++i;
                        if (i >= sourceCount) {
                            break; // end
                        }
                        if (targets[i] >= 0) {
                            break; // found one
                        }
                    }
                }

                public boolean hasNext()
                {
                    return i < sourceCount;
                }

                public IntPair next()
                {
                    final IntPair pair = new IntPair(i, targets[i]);
                    advance();
                    return pair;
                }

                public void remove()
                {
                    throw new UnsupportedOperationException();
                }
            };
        }

        public MappingType getMappingType()
        {
            return mappingType;
        }

        public Mapping inverse()
        {
            // todo: implement
            throw new UnsupportedOperationException();
        }

        public void set(int source, int target)
        {
            if ((target < 0) && mappingType.isMandatorySource()) {
                throw new IllegalArgumentException("Target is required");
            }
            if ((target >= targetCount) && (targetCount >= 0)) {
                throw new IllegalArgumentException(
                    "Target must be less than " + targetCount);
            }
            targets[source] = target;
        }

        public void setAll(Mapping mapping)
        {
            for (IntPair pair : mapping) {
                set(pair.source, pair.target);
            }
        }

        public int getTargetOpt(int source)
        {
            return targets[source];
        }
    }

    /**
     * Decorator which converts any {@link Mapping} into the inverse of itself.
     *
     * <p>If the mapping does not have an inverse -- for example, if a given
     * source can have more than one target -- then the corresponding method
     * call of the underlying mapping will raise a runtime exception.
     */
    private static class InverseMapping
        implements Mapping
    {
        private final Mapping parent;

        InverseMapping(Mapping parent)
        {
            this.parent = parent;
        }

        public Iterator<IntPair> iterator()
        {
            final Iterator<IntPair> parentIter = parent.iterator();
            return new Iterator<IntPair>() {
                public boolean hasNext()
                {
                    return parentIter.hasNext();
                }

                public IntPair next()
                {
                    IntPair parentPair = parentIter.next();
                    return new IntPair(parentPair.target, parentPair.source);
                }

                public void remove()
                {
                    parentIter.remove();
                }
            };
        }

        public int getSourceCount()
        {
            return parent.getTargetCount();
        }

        public int getTargetCount()
        {
            return parent.getSourceCount();
        }

        public MappingType getMappingType()
        {
            return parent.getMappingType().inverse();
        }

        public boolean isIdentity()
        {
            return parent.isIdentity();
        }

        public int getTargetOpt(int source)
        {
            return parent.getSourceOpt(source);
        }

        public int getTarget(int source)
        {
            return parent.getSource(source);
        }

        public int getSource(int target)
        {
            return parent.getTarget(target);
        }

        public int getSourceOpt(int target)
        {
            return parent.getTargetOpt(target);
        }

        public Mapping inverse()
        {
            return parent;
        }

        public void set(int source, int target)
        {
            parent.set(target, source);
        }
    }
}

// End Mappings.java
