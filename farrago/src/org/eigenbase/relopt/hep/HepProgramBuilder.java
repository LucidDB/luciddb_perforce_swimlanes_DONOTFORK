/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2006-2007 The Eigenbase Project
// Copyright (C) 2006-2007 Disruptive Tech
// Copyright (C) 2006-2007 LucidEra, Inc.
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
package org.eigenbase.relopt.hep;

import java.util.*;

import org.eigenbase.rel.convert.*;
import org.eigenbase.relopt.*;


/**
 * HepProgramBuilder creates instances of {@link HepProgram}.
 *
 * @author John V. Sichi
 * @version $Id$
 */
public class HepProgramBuilder
{
    //~ Instance fields --------------------------------------------------------

    private List<HepInstruction> instructions;

    private HepInstruction.BeginGroup group;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new HepProgramBuilder with an initially empty program. The
     * program under construction has an initial match order of {@link
     * HepMatchOrder#ARBITRARY}, and an initial match limit of {@link
     * HepProgram#MATCH_UNTIL_FIXPOINT}.
     */
    public HepProgramBuilder()
    {
        clear();
    }

    //~ Methods ----------------------------------------------------------------

    private void clear()
    {
        instructions = new ArrayList<HepInstruction>();
        group = null;
    }

    /**
     * Adds an instruction to attempt to match any rules of a given class. The
     * order in which the rules within a class will be attempted is arbitrary,
     * so if more control is needed, use addRuleInstance instead.
     *
     * <p>Note that when this method is used, it is also necessary to add the
     * actual rule objects of interest to the planner via {@link
     * RelOptPlanner#addRule}. If the planner does not have any rules of the
     * given class, this instruction is a nop.
     *
     * <p>TODO: support classification via rule annotations.
     *
     * @param ruleClass class of rules to fire, e.g. ConverterRule.class
     */
    public <R extends RelOptRule> void addRuleClass(Class<R> ruleClass)
    {
        HepInstruction.RuleClass instruction =
            new HepInstruction.RuleClass<R>();
        instruction.ruleClass = ruleClass;
        instructions.add(instruction);
    }

    /**
     * Adds an instruction to attempt to match any rules in a given collection.
     * The order in which the rules within a collection will be attempted is
     * arbitrary, so if more control is needed, use addRuleInstance instead. The
     * collection can be "live" in the sense that not all rule instances need to
     * have been added to it at the time this method is called. The collection
     * contents are reevaluated for each execution of the program.
     *
     * <p>Note that when this method is used, it is NOT necessary to add the
     * rules to the planner via {@link RelOptPlanner#addRule}; the instances
     * supplied here will be used. However, adding the rules to the planner
     * redundantly is good form since other planners may require it.
     *
     * @param rules collection of rules to fire
     */
    public void addRuleCollection(Collection<RelOptRule> rules)
    {
        HepInstruction.RuleCollection instruction =
            new HepInstruction.RuleCollection();
        instruction.rules = rules;
        instructions.add(instruction);
    }

    /**
     * Adds an instruction to attempt to match a specific rule object.
     *
     * <p>Note that when this method is used, it is NOT necessary to add the
     * rule to the planner via {@link RelOptPlanner#addRule}; the instance
     * supplied here will be used. However, adding the rule to the planner
     * redundantly is good form since other planners may require it.
     *
     * @param rule rule to fire
     */
    public void addRuleInstance(RelOptRule rule)
    {
        HepInstruction.RuleInstance instruction =
            new HepInstruction.RuleInstance();
        instruction.rule = rule;
        instructions.add(instruction);
    }

    /**
     * Adds an instruction to attempt to match a specific rule identified by its
     * unique description.
     *
     * <p>Note that when this method is used, it is necessary to also add the
     * rule object of interest to the planner via {@link RelOptPlanner#addRule}.
     * This allows for some decoupling between optimizers and plugins: the
     * optimizer only knows about rule descriptions, while the plugins supply
     * the actual instances. If the planner does not have a rule matching the
     * description, this instruction is a nop.
     *
     * @param ruleDescription description of rule to fire
     */
    public void addRuleByDescription(String ruleDescription)
    {
        HepInstruction.RuleInstance instruction =
            new HepInstruction.RuleInstance();
        instruction.ruleDescription = ruleDescription;
        instructions.add(instruction);
    }

    /**
     * Adds an instruction to begin a group of rules. All subsequent rules added
     * (until the next endRuleGroup) will be collected into the group rather
     * than firing individually. After addGroupBegin has been called, only
     * addRuleXXX methods may be called until the next addGroupEnd.
     */
    public void addGroupBegin()
    {
        assert (group == null);
        HepInstruction.BeginGroup instruction = new HepInstruction.BeginGroup();
        instructions.add(instruction);
        group = instruction;
    }

    /**
     * Adds an instruction to end a group of rules, firing the group
     * collectively. The order in which the rules within a group will be
     * attempted is arbitrary. Match order and limit applies to the group as a
     * whole.
     */
    public void addGroupEnd()
    {
        assert (group != null);
        HepInstruction.EndGroup instruction = new HepInstruction.EndGroup();
        instructions.add(instruction);
        group.endGroup = instruction;
        group = null;
    }

    /**
     * Adds an instruction to attempt to match instances of {@link
     * ConverterRule}, but only where a conversion is actually required.
     *
     * @param guaranteed if true, use only guaranteed converters; if false, use
     * only non-guaranteed converters
     */
    public void addConverters(boolean guaranteed)
    {
        assert (group == null);
        HepInstruction.ConverterRules instruction =
            new HepInstruction.ConverterRules();
        instruction.guaranteed = guaranteed;
        instructions.add(instruction);
    }

    /**
     * Adds an instruction to change the order of pattern matching for
     * subsequent instructions. The new order will take effect for the rest of
     * the program (not counting subprograms) or until another match order
     * instruction is encountered.
     *
     * @param order new match direction to set
     */
    public void addMatchOrder(HepMatchOrder order)
    {
        assert (group == null);
        HepInstruction.MatchOrder instruction = new HepInstruction.MatchOrder();
        instruction.order = order;
        instructions.add(instruction);
    }

    /**
     * Adds an instruction to limit the number of pattern matches for subsequent
     * instructions. The limit will take effect for the rest of the program (not
     * counting subprograms) or until another limit instruction is encountered.
     *
     * @param limit limit to set; use {@link HepProgram#MATCH_UNTIL_FIXPOINT} to
     * remove limit
     */
    public void addMatchLimit(int limit)
    {
        assert (group == null);
        HepInstruction.MatchLimit instruction = new HepInstruction.MatchLimit();
        instruction.limit = limit;
        instructions.add(instruction);
    }

    /**
     * Adds an instruction to execute a subprogram. Note that this is different
     * from adding the instructions from the subprogram individually. When added
     * as a subprogram, the sequence will execute repeatedly until a fixpoint is
     * reached, whereas when the instructions are added individually, the
     * sequence will only execute once (with a separate fixpoint for each
     * instruction).
     *
     * <p>The subprogram has its own state for match order and limit
     * (initialized to the defaults every time the subprogram is executed) and
     * any changes it makes to those settings do not affect the parent program.
     *
     * @param program subprogram to execute
     */
    public void addSubprogram(HepProgram program)
    {
        assert (group == null);
        HepInstruction.Subprogram instruction = new HepInstruction.Subprogram();
        instruction.subprogram = program;
        instructions.add(instruction);
    }

    /**
     * Returns the constructed program, clearing the state of this program
     * builder as a side-effect.
     *
     * @return immutable program
     */
    public HepProgram createProgram()
    {
        assert (group == null);
        HepProgram program = new HepProgram(instructions);
        clear();
        return program;
    }
}

// End HepProgramBuilder.java
