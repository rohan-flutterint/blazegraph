/* Generated By:JJTree: Do not edit this line. ASTUpdateSequence.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.openrdf.query.parser.sparql.ast;

import java.util.List;

public class ASTUpdateSequence extends SimpleNode {

	public ASTUpdateSequence(int id) {
		super(id);
	}

	public ASTUpdateSequence(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	/** Accept the visitor. **/
	public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data)
		throws VisitorException
	{
		return visitor.visit(this, data);
	}

	public List<ASTUpdateContainer> getUpdateContainers() {
		return super.jjtGetChildren(ASTUpdateContainer.class);
	}

}
/* JavaCC - OriginalChecksum=e4b13eef2d0d6dbe36d25df3ab1d11da (do not edit this line) */
