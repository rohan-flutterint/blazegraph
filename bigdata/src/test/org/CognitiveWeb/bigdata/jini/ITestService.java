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
 * Created on Mar 18, 2007
 */

package org.CognitiveWeb.bigdata.jini;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The public interface for a test service.
 * <p>
 * Note: The interface extends {@link Remote} so that both the implementation of
 * this service and the proxy generated by jeri for the service will implement
 * this interface. This is necessary for service discovery or the resulting
 * proxy will NOT implement the interface and both service discovery (based on
 * the interface name) and use of the interface via the proxy will fail.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @download
 */
public interface ITestService extends Remote
{

    /**
     * Method for testing remote invocation.
     * <p>
     * Note: The methods on a {@link Remote} interface MUST throw
     * {@link RemoteException} (or {@link IOException} if you want to reduce
     * your dependency on JINI for an interface that is also used outside of
     * JINI).
     */
    public void invoke() throws RemoteException;
            
}
