/**

Copyright (C) SYSTAP, LLC 2006-2015.  All rights reserved.

Contact:
     SYSTAP, LLC
     2501 Calvert ST NW #106
     Washington, DC 20008
     licenses@systap.com

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
 * Created on Mar 10, 2012
 */

package com.bigdata.rdf.sparql.ast;

import java.util.Map;

import org.openrdf.query.algebra.StatementPattern.Scope;

import com.bigdata.bop.BOp;

/**
 * A SPARQL Update operation.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
abstract public class Update extends GroupMemberNodeBase<IGroupMemberNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    interface Annotations extends GroupMemberNodeBase.Annotations {
        
        /**
         * The {@link UpdateType}.
         */
        String UPDATE_TYPE = "updateType";

        /**
         * The source graph -or- solution set (for operations which have this
         * concept).
         * <p>
         * When the value is a {@link ConstantNode}, then the annotation is the
         * source <em>graph</em>.
         * <p>
         * When the value is a {@link String}, then the annotation is the source
         * <em>solution set</em>.
         */
        String SOURCE = "source";
        
        /**
         * The target graph -or- solution set (for operations which have this
         * concept). If there is only one graph (or solution set) on which the
         * operation will have an effect, then it is modeled by this annotation.
         * <p>
         * When the value is a {@link ConstantNode}, then the annotation is the
         * target <em>graph</em>.
         * <p>
         * When the value is a {@link String}, then the annotation is the target
         * <em>solution set</em>.
         */
        String TARGET = "target";
     
        /**
         * The "SILENT" option (default <code>false</code>) (for operations
         * which have this concept).
         */
        String SILENT = "silent";
        
        boolean DEFAULT_SILENT = false;

        /**
         * The {@link Scope} (required for operations which have this concept).
         */
        String SCOPE = "scope";
 
    }
    
    /**
     * 
     */
    public Update(final UpdateType updateType) {

        setProperty(Annotations.UPDATE_TYPE, updateType);

    }

    /**
     * @param op
     */
    public Update(Update op) {
        super(op);
    }

    /**
     * @param args
     * @param anns
     */
    public Update(BOp[] args, Map<String, Object> anns) {
        super(args, anns);
    }
    
    final public UpdateType getUpdateType() {
        
        return (UpdateType) getRequiredProperty(Annotations.UPDATE_TYPE);
        
    }

    /**
     * The {@link ConstantNode} for the source graph (for operations which have
     * this concept).
     * 
     * @throws UnsupportedOperationException
     *             if this concept is not supported by this type of
     *             {@link Update} operation.
     */
    public ConstantNode getSourceGraph() {

        throw new UnsupportedOperationException();
        
    }

    public void setSourceGraph(final ConstantNode sourceGraph) {

        throw new UnsupportedOperationException();
        
    }

    /**
     * The {@link ConstantNode} for the target graph (for operations which have
     * this concept). If there is only one graph on which the operation will
     * have an effect, then it is modeled by this annotation.
     * 
     * @throws UnsupportedOperationException
     *             if this concept is not supported by this type of
     *             {@link Update} operation.
     */
    public ConstantNode getTargetGraph() {

        throw new UnsupportedOperationException();
        
    }
    
    public void setTargetGraph(final ConstantNode targetGraph) {

        throw new UnsupportedOperationException();
        
    }

    /**
     * The "SILENT" option (default <code>false</code>) (for operations
     * which have this concept).
     * 
     * @throws UnsupportedOperationException
     *             if this concept is not supported by this type of
     *             {@link Update} operation.
     */
    public boolean isSilent() {
       
        throw new UnsupportedOperationException();
        
    }

    public void setSilent(final boolean silent) {
        
        throw new UnsupportedOperationException();
        
    }
    
}
