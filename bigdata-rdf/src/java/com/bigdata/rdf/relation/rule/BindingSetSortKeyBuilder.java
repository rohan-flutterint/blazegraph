/*

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
 * Created on Sep 15, 2008
 */

package com.bigdata.rdf.relation.rule;

import com.bigdata.bop.Constant;
import com.bigdata.bop.IBindingSet;
import com.bigdata.bop.IVariable;
import com.bigdata.btree.keys.IKeyBuilder;
import com.bigdata.btree.keys.ISortKeyBuilder;
import com.bigdata.rdf.internal.IV;

/**
 * Builds unsigned byte[] sort keys from {@link IBindingSet}s.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class BindingSetSortKeyBuilder implements ISortKeyBuilder<IBindingSet> {

    private final IKeyBuilder keyBuilder;
    private final IVariable[] vars;
    
    /**
     * 
     * @param keyBuilder
     *            Used to generate the unsigned byte[] key for each bound
     *            variable. In particular, the configuration for the
     *            {@link IKeyBuilder} governs how Unicode fields are handled.
     * @param vars
     *            An array of {@link IVariable}s, all of which MUST be bound.
     */
    public BindingSetSortKeyBuilder(final IKeyBuilder keyBuilder,
            final IVariable[] vars) {

        if (keyBuilder == null)
            throw new IllegalArgumentException();

        if (vars == null || vars.length == 0)
            throw new IllegalArgumentException();

        this.keyBuilder = keyBuilder;

        this.vars = vars;
        
    }
    
    /**
     * @todo This has RDF specific handling of the IVs and treatment of unbound
     *       variables as 0L term identifiers.  This needs to be abstracted out
     *       in order to run against generic relations.
     */
    public byte[] getSortKey(final IBindingSet bindingSet) {
        
        keyBuilder.reset();
        
        for (int i = 0; i < vars.length; i++) {
            
            final IVariable<?> var = vars[i];
            
            Object val = bindingSet.get(var);
            if (val == null) {
                val = Long.valueOf(0);
            } else if (val instanceof Constant<?>) {
                val = ((Constant<?>) val).get();
            }

            if (val instanceof IV) {
            
                final IV iv = (IV) val;
            
                iv.encode(keyBuilder);

            } else {
                
                keyBuilder.append(val);
                
            }
            
        }

        return keyBuilder.getKey();

    }

}
