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
 * Created on Nov 2, 2007
 */

package com.bigdata.rdf.rules;

import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.OWL;

import com.bigdata.rdf.rio.IStatementBuffer;
import com.bigdata.rdf.rio.StatementBuffer;
import com.bigdata.rdf.store.AbstractTripleStore;
import com.bigdata.rdf.vocab.Vocabulary;
import com.bigdata.relation.rule.Rule;

/**
 * Test suite for {@link RuleOwlEquivalentClass}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestRuleOwlEquivalentClass extends AbstractRuleTestCase {

    /**
     * 
     */
    public TestRuleOwlEquivalentClass() {
        super();
    }

    /**
     * @param name
     */
    public TestRuleOwlEquivalentClass(String name) {
        super(name);
    }

    /**
     * Test where the data satisifies the rule exactly once.
     * 
     * <pre>
     *  (a owl:equivalentClass b) -&gt; (b owl:equivalentClass a) 
     * </pre>
     * @throws Exception 
     */
    public void test_owlEquivalentClass() throws Exception {

        AbstractTripleStore store = getStore();

        try {

            URI A = new URIImpl("http://www.foo.org/A");
            URI B = new URIImpl("http://www.foo.org/B");

            IStatementBuffer buffer = new StatementBuffer(store, 100/* capacity */);
            
            buffer.add(A, OWL.EQUIVALENTCLASS, B);

            // write on the store.
            buffer.flush();

            // verify statement(s).
            assertTrue(store.hasStatement(A, OWL.EQUIVALENTCLASS, B));
            final long nbefore = store.getStatementCount();

            final Vocabulary vocab = store.getVocabulary();

            final Rule r = new RuleOwlEquivalentClass(store.getSPORelation()
                    .getNamespace(), vocab);
            
            // apply the rule.
            applyRule(store,r, -1/*solutionCount*/,1/*mutationCount*/);

            /*
             * validate the state of the primary store.
             */

            // told
            assertTrue(store.hasStatement(A, OWL.EQUIVALENTCLASS, B));

            // entailed
            assertTrue(store.hasStatement(B, OWL.EQUIVALENTCLASS, A));

            // final #of statements in the store.
            assertEquals(nbefore + 1, store.getStatementCount());

        } finally {

            store.__tearDownUnitTest();

        }
        
    }
    
}
