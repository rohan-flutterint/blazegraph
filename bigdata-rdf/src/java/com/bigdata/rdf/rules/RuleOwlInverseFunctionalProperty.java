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
 * Created on Nov 1, 2007
 */

package com.bigdata.rdf.rules;

import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;

import com.bigdata.bop.IConstraint;
import com.bigdata.bop.constraint.Constraint;
import com.bigdata.bop.constraint.NE;
import com.bigdata.rdf.spo.SPOPredicate;
import com.bigdata.rdf.vocab.Vocabulary;
import com.bigdata.relation.rule.Rule;

/**
 * owl:InverseFunctionalProperty
 * 
 * <pre>
 *   (p rdf:type owl:InverseFunctionalProperty), (b p a), (c p a) -&gt; 
 *   (b owl:sameAs c)
 * </pre>
 */
@SuppressWarnings("rawtypes")
public class RuleOwlInverseFunctionalProperty extends Rule {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6688762355076324400L;

	/**
     * @param vocab
     */
    public RuleOwlInverseFunctionalProperty(String relationName, Vocabulary vocab) {

        super( "owlInverseFunctionalProperty", //
                new SPOPredicate(relationName,var("b"), vocab.getConstant(OWL.SAMEAS), var("c")),//
                new SPOPredicate[] {//
                    new SPOPredicate(relationName,var("x"), vocab.getConstant(RDF.TYPE), vocab.getConstant(OWL.INVERSEFUNCTIONALPROPERTY)),//
                    new SPOPredicate(relationName,var("b"), var("x"), var("a")),//
                    new SPOPredicate(relationName,var("c"), var("x"), var("a"))//
                },
                new IConstraint[] {
					Constraint.wrap(new NE(var("b"),var("c")))
		        }
                );
        
    }
    
//    /**
//     * If this rule ever becomes consistent in the data then the rule will
//     * throw a {@link ConstraintViolationException} and the closure operation
//     * will fail.
//     */
//    @Override
//	public boolean isConsistent(final IBindingSet bset) {
//
//		boolean ret = super.isConsistent(bset);
//
//		if (ret && isFullyBound(bset)) {
//
//			throw new ConstraintViolationException(getName());
//
//		}
//
//		return ret;
//
//	}

}
