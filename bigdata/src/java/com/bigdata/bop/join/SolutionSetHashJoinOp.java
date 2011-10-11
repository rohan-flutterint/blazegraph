/**

Copyright (C) SYSTAP, LLC 2006-2011.  All rights reserved.

Contact:
     SYSTAP, LLC
     4501 Tower Road
     Greensboro, NC 27410
     licenses@bigdata.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
/*
 * Created on Aug 30, 2011
 */

package com.bigdata.bop.join;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.apache.log4j.Logger;

import com.bigdata.bop.BOp;
import com.bigdata.bop.BOpContext;
import com.bigdata.bop.IBindingSet;
import com.bigdata.bop.IConstraint;
import com.bigdata.bop.IQueryAttributes;
import com.bigdata.bop.IVariable;
import com.bigdata.bop.NV;
import com.bigdata.bop.PipelineOp;
import com.bigdata.bop.controller.NamedSolutionSetRef;
import com.bigdata.bop.controller.NamedSubqueryOp;
import com.bigdata.bop.controller.SubqueryJoinAnnotations;
import com.bigdata.bop.engine.IRunningQuery;
import com.bigdata.htree.HTree;
import com.bigdata.rawstore.IRawStore;
import com.bigdata.relation.accesspath.AbstractUnsynchronizedArrayBuffer;
import com.bigdata.relation.accesspath.IBlockingBuffer;
import com.bigdata.relation.accesspath.UnsyncLocalOutputBuffer;
import com.bigdata.striterator.Dechunkerator;
import com.bigdata.striterator.ICloseableIterator;

/**
 * Operator joins a solution set into the pipeline.  The solution set can be
 * constructed by a {@link NamedSubqueryOp} or a 
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: NamedSubqueryIncludeOp.java 5178 2011-09-12 19:09:23Z
 *          thompsonbry $
 */
public class SolutionSetHashJoinOp extends PipelineOp {

    static private final transient Logger log = Logger
            .getLogger(SolutionSetHashJoinOp.class);

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public interface Annotations extends PipelineOp.Annotations,
            HashJoinAnnotations {

        /**
         * The {@link NamedSolutionSetRef} used to locate the {@link HTree}
         * having the data for the named solution set. The query UUID must be
         * extracted and used to lookup the {@link IRunningQuery} to which the
         * solution set was attached. The {@link HTree} is then resolved against
         * the {@link IQueryAttributes} on that {@link IRunningQuery}.
         * 
         * @see NamedSolutionSetRef
         * @see NamedSubqueryOp.Annotations#NAMED_SET_REF
         */
        final String NAMED_SET_REF = NamedSubqueryOp.Annotations.NAMED_SET_REF;

        /**
         * An optional {@link IVariable}[] identifying the variables to be
         * retained in the {@link IBindingSet}s written out by the operator. All
         * variables are retained unless this annotation is specified. This is
         * normally set to the <em>projection</em> of the subquery, in which
         * case the lexical scope of the variables is will be properly managed
         * for the subquery INCLUDE join.
         * 
         * @see JoinAnnotations#SELECT
         */
        final String SELECT = JoinAnnotations.SELECT;

        /**
         * An {@link IConstraint}[] to be applied to solutions when they are
         * joined (optional).
         */
        final String CONSTRAINTS = JoinAnnotations.CONSTRAINTS;
        
        /**
         * Boolean annotation is <code>true</code> iff the join has OPTIONAL
         * semantics.
         */
        final String OPTIONAL = SubqueryJoinAnnotations.OPTIONAL;

        final boolean DEFAULT_OPTIONAL = SubqueryJoinAnnotations.DEFAULT_OPTIONAL;
        
        /**
         * When <code>true</code> the hash index identified by
         * {@link #NAMED_SET_REF} will be released when this operator is done
         * (default {@value #DEFAULT_RELEASE}).
         * <p>
         * Note: Whether or not the hash index can be released depends on
         * whether or not the hash index will be consumed by more than one
         * operator in the query plan. For example, a named solution set can be
         * consumed by more than one operator and thus must not be released
         * until all such operators are done.
         * 
         * TODO Alternatively, we could specify the #of different locations in
         * the query plan where the named solution set will be consumed.
         */
        final String RELEASE = SolutionSetHashJoinOp.class + ".release";

        final boolean DEFAULT_RELEASE = true;
        
    }

    /**
     * Deep copy constructor.
     */
    public SolutionSetHashJoinOp(SolutionSetHashJoinOp op) {

        super(op);
        
    }

    /**
     * Shallow copy constructor.
     * 
     * @param args
     * @param annotations
     */
    public SolutionSetHashJoinOp(final BOp[] args,
            final Map<String, Object> annotations) {

        super(args, annotations);

        if (isRelease() && !isLastPassRequested()) {
            /*
             * In order to release the hash index, this operator needs to be
             * notified when no more source solutions will become available.
             */
            throw new IllegalArgumentException(Annotations.RELEASE
                    + " requires " + Annotations.LAST_PASS);
        }
        
    }

    public SolutionSetHashJoinOp(final BOp[] args, NV... annotations) {

        this(args, NV.asMap(annotations));
        
    }

    /**
     * @see Annotations#OPTIONAL
     */
    public boolean isOptional() {
        
        return getProperty(Annotations.OPTIONAL,Annotations.DEFAULT_OPTIONAL);
        
    }
    
    /**
     * @see Annotations#RELEASE
     */
    public boolean isRelease() {
        
        return getProperty(Annotations.RELEASE,Annotations.DEFAULT_RELEASE);
        
    }
    
    public BaseJoinStats newStats() {

        return new BaseJoinStats();

    }

    @Override
    public FutureTask<Void> eval(BOpContext<IBindingSet> context) {

        return new FutureTask<Void>(new ChunkTask<IBindingSet>(context, this));
        
    }

    /**
     * Task executing on the node.
     */
    private static class ChunkTask<E> implements Callable<Void> {

        private final BOpContext<IBindingSet> context;

        private final SolutionSetHashJoinOp op;

        private final IVariable<E>[] joinVars;
        
        private final IConstraint[] constraints;

        private final IVariable<?>[] selectVars;

        private final boolean optional;
        
        private final boolean release;
        
//        private final String namedSet;
        
        private final BaseJoinStats stats;

        private final IBlockingBuffer<IBindingSet[]> sink;
        
        private final IBlockingBuffer<IBindingSet[]> sink2;

        /**
         * A map whose keys are the bindings on the specified variables. The
         * values in the map are <code>null</code>s.
         * <p>
         * Note: The map is shared state and can not be discarded or cleared
         * until the last invocation!!!
         */
        private final HTree rightSolutions;

        /**
         * This is not an optional join, so this is always <code>null</code>.
         */
        private final HTree joinSet = null;

        @SuppressWarnings("unchecked")
        public ChunkTask(final BOpContext<IBindingSet> context,
                final SolutionSetHashJoinOp op) {

            this.context = context;

            this.stats = (BaseJoinStats) context.getStats();

            this.selectVars = (IVariable<?>[]) op
                    .getProperty(Annotations.SELECT);

            this.joinVars = (IVariable<E>[]) op
                    .getRequiredProperty(Annotations.JOIN_VARS);
            
            this.constraints = (IConstraint[]) op
                    .getProperty(Annotations.CONSTRAINTS);

            this.optional = op.isOptional();

            this.release = op.getProperty(Annotations.RELEASE,
                    Annotations.DEFAULT_RELEASE);

            this.sink = context.getSink();

            this.sink2 = context.getSink2();

            this.op = op;

            // The name of the attribute used to discover the solution set.
            final NamedSolutionSetRef namedSetRef = (NamedSolutionSetRef) op
                    .getRequiredProperty(Annotations.NAMED_SET_REF);

            // Lookup the attributes for the query on which we will hang the
            // solution set.
            final IQueryAttributes attrs = context
                    .getQueryAttributes(namedSetRef.queryId);

            // The HTree holding the solutions.
            rightSolutions = (HTree) attrs.get(namedSetRef);

            if (rightSolutions == null) {
             
                // The solution set was not found!
                
                throw new RuntimeException("Not found: " + namedSetRef);
                
            }

        }

        /**
         * Discard the {@link HTree} data.
         */
        private void release() {

            if (joinSet != null) {

                joinSet.close();

//                joinSet = null;
                
            }

            if (rightSolutions != null) {

                final IRawStore store = rightSolutions.getStore();

                rightSolutions.close();
                
//                sourceSolutions = null;
                
                store.close();

            }

        }
        
        public Void call() throws Exception {

            try {

                doHashJoin();
                
                // Done.
                return null;
                
            } finally {

                if (release && context.isLastInvocation()) {

                    /*
                     * Note: It is possible to INCLUDE the named temporary
                     * solution set multiple times within a query. If we want to
                     * release() the hash tree then we need to know how many
                     * times the temporary solution set is being included and
                     * decrement a counter each time. When the counter reaches
                     * zero, we can release the HTree.
                     */
                    release();

                }
                
                sink.close();

                if (sink2 != null)
                    sink2.close();
                
            }

        }
        
        /**
         * Do a hash join of the buffered solutions with the access path.
         */
        private void doHashJoin() {

            if (rightSolutions.getEntryCount() == 0)
                return;
            
            if (log.isDebugEnabled()) {
                log.debug("rightSolutions=" + rightSolutions.getEntryCount());
                log.debug("joinVars=" + Arrays.toString(joinVars));
            }

            stats.accessPathCount.increment();

            stats.accessPathRangeCount.add(rightSolutions.getEntryCount());

            final UnsyncLocalOutputBuffer<IBindingSet> unsyncBuffer = new UnsyncLocalOutputBuffer<IBindingSet>(
                    op.getChunkCapacity(), sink);

            final ICloseableIterator<IBindingSet> leftItr = new Dechunkerator<IBindingSet>(
                    context.getSource());

            HashJoinUtility.hashJoin(leftItr, unsyncBuffer, joinVars,
                    selectVars, constraints, rightSolutions/* hashIndex */,
                    joinSet, optional, true/* leftIsPipeline */);

            if (optional) {

                // where to write the optional solutions.
                final AbstractUnsynchronizedArrayBuffer<IBindingSet> unsyncBuffer2 = sink2 == null ? unsyncBuffer
                        : new UnsyncLocalOutputBuffer<IBindingSet>(
                                op.getChunkCapacity(), sink2);

                HashJoinUtility.outputOptionals(unsyncBuffer2, rightSolutions,
                        joinSet);

                unsyncBuffer2.flush();
                if (sink2 != null)
                    sink2.flush();

            }

            unsyncBuffer.flush();
            sink.flush();

        }
        
    } // class ChunkTask

}
