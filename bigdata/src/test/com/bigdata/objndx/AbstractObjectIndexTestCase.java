/**

The Notice below must appear in each file of the Source Code of any
copy you distribute of the Licensed Product.  Contributors to any
Modifications may add their own copyright notices to identify their
own contributions.

License:

The contents of this file are subject to the CognitiveWeb Open Source
License Version 1.1 (the License).  You may not copy or use this file,
in either source code or executable form, except in compliance with
the License.  You may obtain a copy of the License from

  http://www.CognitiveWeb.org/legal/license/

Software distributed under the License is distributed on an AS IS
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.  See
the License for the specific language governing rights and limitations
under the License.

Copyrights:

Portions created by or assigned to CognitiveWeb are Copyright
(c) 2003-2003 CognitiveWeb.  All Rights Reserved.  Contact
information for CognitiveWeb is available at

  http://www.CognitiveWeb.org

Portions Copyright (c) 2002-2003 Bryan Thompson.

Acknowledgements:

Special thanks to the developers of the Jabber Open Source License 1.0
(JOSL), from which this License was derived.  This License contains
terms that differ from JOSL.

Special thanks to the CognitiveWeb Open Source Contributors for their
suggestions and support of the Cognitive Web.

Modifications:

*/
/*
 * Created on Nov 6, 2006
 */

package com.bigdata.objndx;

import com.bigdata.journal.IObjectIndex;
import com.bigdata.journal.ISlotAllocation;
import com.bigdata.journal.SlotMath;
import com.bigdata.journal.SimpleObjectIndex.IObjectIndexEntry;

/**
 * Abstract test case defines utility methods for testing the object index and
 * its nodes and leaves.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
abstract public class AbstractObjectIndexTestCase extends AbstractBTreeTestCase {

    /**
     * 
     */
    public AbstractObjectIndexTestCase() {
    }

    /**
     * @param name
     */
    public AbstractObjectIndexTestCase(String name) {
        super(name);
    }

    /**
     * Return a random reference to a child node or leaf. References are an
     * {@link ISlotAllocation} encoded as a long integer.
     * 
     * @param isLeaf
     *            When true, the reference will be to a leaf node. Otherwise the
     *            reference will be to a non-leaf node. (This effects the #of
     *            bytes encoded into the reference - the reference is only
     *            syntactically valid and MUST NOT be dereferenced).
     */
    private long nextNodeRef(boolean isLeaf) {

        // random slot on the journal in [1:n-1].
        int firstSlot = r.nextInt(Integer.MAX_VALUE - 1) + 1;

        // #of bytes in the serialized record.
        int nbytes = r.nextInt(256)+NodeSerializer.OFFSET_KEYS;

        // convert to an encoded slot allocation.
        return SlotMath.toLong(nbytes, firstSlot);

    }

    /**
     * Return a random version counter.
     * 
     * @todo Shape the distribution to make version0 and other low-numbered
     *       versions much more likely.
     */
    private short nextVersionCounter() {

        return (short)r.nextInt((int)Short.MAX_VALUE);

    }

    /**
     * Reference to a random data object.
     * 
     * @return A reference to a random data object. The reference is only
     *         syntactically valid and MUST NOT be dereferenced
     */
    private long nextVersionRef() {

        int nbytes = r.nextInt(512)+1;
        
        int firstSlot = r.nextInt(Integer.MAX_VALUE - 1) + 1;

        return SlotMath.toLong(nbytes, firstSlot);

    }
    
    /**
     * Return a random key value. Keys are always generated in a progressive
     * (sorted) order. A key is an int32 within segment persistent identifier
     * that is mapped by the object index onto an {@link ISlotAllocation}. The
     * key value of zero(0) is reserved to indicate null. Positive and negative
     * infinity are defined by the object index.
     * 
     * @param lastKey
     *            The last key value generated and {@link Node#NEGINF_KEY} on
     *            the first invocation for a node.
     * 
     * @see Node#POSINF_KEY
     * @see Node#NEGINF_KEY
     */
    private int nextKey(int pageSize,int index,int lastKey) {
        
        /*
         * The smallest key that we can generate on this pass.
         */
        int min = lastKey + 1;
        
        /*
         * The #of legal key values remaining.
         */
        int range = IBTree.POSINF-lastKey-1;
        
        assert range>0;
        
        /*
         * Divide up the remaining range by the key index position. This ensures
         * that we never run out of legal keys that we can assign until we have
         * first run out of keys that we need to assign.
         */

        int positionsRemaining = pageSize-index;
        
        assert positionsRemaining >= 0;
        
        range = range / positionsRemaining;
        
        assert range < IBTree.POSINF;
        
        /*
         * Generate a random key within the allowed range of legal keys.
         */
        
        int key = r.nextInt(range)+min;
        
        assert key > lastKey;
        assert key > IBTree.NEGINF;
        assert key < IBTree.POSINF;
        
        return key;
        
    }
    
    /**
     * Generate a random entry for an {@link IObjectIndex}.
     */
    public IndexEntry getRandomEntry(SlotMath slotMath) {
        
        // when true, the entry marks a deleted version.
        boolean isDeleted = r.nextInt(100) < 10;

        // when true, a preExisting version is defined on the journal.
        boolean isPreExisting = r.nextInt(100) < 50;

        short versionCounter = nextVersionCounter();

        long currentVersion = isDeleted ? 0L : nextVersionRef();

        long preExistingVersion = isPreExisting ? nextVersionRef() : 0L;

        return new IndexEntry(slotMath, versionCounter, currentVersion,
                preExistingVersion);

    }

    /**
     * Generates a non-leaf node with random data.
     */
    public Node getRandomNode(BTree ndx) {

        // #of keys per node.
        final int branchingFactor = ndx.branchingFactor;
        
        final long id = nextNodeRef(false); // ref. for this node.

        int nchildren = r.nextInt((branchingFactor+1)/2)+(branchingFactor+1)/2;
        assert nchildren>=(branchingFactor+1)/2;
        assert nchildren<=branchingFactor;
        int nkeys = nchildren-1;
        
        final int[] keys = new int[branchingFactor-1];
        
        final long[] children = new long[branchingFactor];
        
        // node with some valid keys and corresponding child refs.

        int lastKey = IBTree.NEGINF;

        for (int i = 0; i < nkeys ; i++) {

            // reference is to either a leaf or a non-leaf node.
            boolean isLeaf = r.nextBoolean();

            lastKey = keys[i] = nextKey(branchingFactor, i, lastKey);

            children[i] = nextNodeRef(isLeaf);

        }

        // children[nkeys] is always defined.

        boolean isLeaf = r.nextBoolean();
        
        children[nkeys] = nextNodeRef(isLeaf);

        /*
         * create the node and set it as the root to fake out the btree.
         */
        
        Node node = new Node(ndx, id, branchingFactor, nkeys, keys, children);
        
        ndx.root = node;

        return node;
        
    }

    /**
     * Generates a leaf node with random data.
     */
    public Leaf getRandomLeaf(BTree ndx) {

        // #of keys per node.
        final int branchingFactor = ndx.branchingFactor;

        long id = nextNodeRef(true); // ref. for this leaf.

        int nkeys = r.nextInt((branchingFactor+1)/2)+(branchingFactor+1)/2;
        assert nkeys>=(branchingFactor+1)/2;
        assert nkeys<=branchingFactor;

        final int[] keys = new int[branchingFactor];

        final IObjectIndexEntry[] values = new IObjectIndexEntry[branchingFactor];

        // node with some valid keys and corresponding child refs.

        int lastKey = IBTree.NEGINF;

        for (int i = 0; i < nkeys; i++) {

            /*
             * Reference is to a data version.
             */

            // the key.
            lastKey = keys[i] = nextKey(branchingFactor, i, lastKey);

            values[i] = getRandomEntry(ndx.store.getSlotMath());

        }

//        /*
//         * Set up prior/next leaf node references.
//         */
//        
//        // true unless this is the first leaf node of the index.
//        boolean hasPrevious = r.nextInt(100)>0;
//        
//        // true unless this is the last leaf node of the index.
//        boolean hasNext = r.nextInt(100)>0;
//        
//        long previous = hasPrevious ?nextNodeRef(true,nodeSer) : 0L;
//        
//        long next = hasNext ?nextNodeRef(true,nodeSer) : 0L;

        /*
         * create the leaf and set it as the root to fake out the btree.
         */

        Leaf leaf = new Leaf(ndx,id,branchingFactor, nkeys,keys,values); //,previous,next);
        
        ndx.root = leaf;
        
        return leaf;

    }

    /**
     * Generates a node or leaf (randomly) with random data.
     */
    public AbstractNode getRandomNodeOrLeaf(BTree ndx) {

        if( r.nextBoolean() ) {
            
            return getRandomNode(ndx);
            
        } else {
            
            return getRandomLeaf(ndx);
            
        }
        
    }
    
}
