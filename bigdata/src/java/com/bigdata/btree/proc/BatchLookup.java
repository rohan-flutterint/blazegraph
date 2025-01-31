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
 * Created on Feb 12, 2007
 */

package com.bigdata.btree.proc;

import com.bigdata.btree.IIndex;
import com.bigdata.btree.proc.AbstractKeyArrayIndexProcedure.ResultBuffer;
import com.bigdata.btree.raba.codec.IRabaCoder;

/**
 * Batch lookup operation.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class BatchLookup extends AbstractKeyArrayIndexProcedure<ResultBuffer> implements
        IParallelizableIndexProcedure<ResultBuffer> {

    /**
     * 
     */
    private static final long serialVersionUID = 8102720738892338403L;

    /**
     * Factory for {@link BatchLookup} procedures.
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     */
    public static class BatchLookupConstructor extends AbstractKeyArrayIndexProcedureConstructor<BatchLookup> {

        public static final BatchLookupConstructor INSTANCE = new BatchLookupConstructor(); 
        
        private BatchLookupConstructor() {
            
        }
        
        /**
         * Values ARE NOT sent.
         */
        @Override
        public final boolean sendValues() {
            
            return false;
            
        }

        @Override
        public BatchLookup newInstance(IRabaCoder keysCoder,
                IRabaCoder valsCoder, int fromIndex, int toIndex,
                byte[][] keys, byte[][] vals) {

            assert vals == null;

            return new BatchLookup(keysCoder, valsCoder, fromIndex, toIndex,
                    keys);
            
        }
        
    }
    
    /**
     * De-serialization ctor.
     *
     */
    public BatchLookup() {
        
    }
    
    /**
     * Create a batch lookup operation.
     * 
     * @param keys
     *            The array of keys (one key per tuple).
     * 
     * @see BatchLookupConstructor
     */
    protected BatchLookup(IRabaCoder keysCoder, IRabaCoder valsCoder,
            int fromIndex, int toIndex, byte[][] keys) {

        super(keysCoder, valsCoder, fromIndex, toIndex, keys, null/* values */);
        
    }

    @Override
    public final boolean isReadOnly() {
       
        return true;
        
    }
    
    /**
     * @return {@link ResultBuffer}
     */
    @Override
    public ResultBuffer apply(final IIndex ndx) {

        final int n = getKeyCount();
        
        final byte[][] ret = new byte[n][];
        
        int i = 0;
        
        while (i < n) {

            ret[i] = ndx.lookup(getKey(i));

            i++;

        }
        
        return new ResultBuffer(n, ret, ndx.getIndexMetadata()
                .getTupleSerializer().getLeafValuesCoder());

    }

}
