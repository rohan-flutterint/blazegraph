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
package com.bigdata.rdf.sail.webapp;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bigdata.journal.ITx;
import com.bigdata.rdf.sail.CreateKBTask;
import com.bigdata.rdf.store.AbstractTripleStore;

/**
 * Extended to report the correct HTTP response to the client.
 */
class RestApiCreateKBTask extends AbstractDelegateRestApiTask<Void> {

   public RestApiCreateKBTask(final HttpServletRequest req,
         final HttpServletResponse resp, final String namespace,
         final Properties properties) {

      super(req, resp, namespace, ITx.UNISOLATED, new CreateKBTask(namespace,
            properties));

   }

   @Override
   public Void call() throws Exception {

      // Pre-condition check while holding locks.
      {

         // resolve the namespace.
         final AbstractTripleStore tripleStore = getTripleStore();

         if (tripleStore != null) {
            /*
             * The namespace already exists.
             * 
             * Note: The response code is defined as 409 (Conflict) since 1.3.2.
             */
            throw new HttpOperationException(HttpServletResponse.SC_CONFLICT,
                  BigdataServlet.MIME_TEXT_PLAIN, "EXISTS: " + namespace);
         }

      }

      // Create namespace.
      super.call();

      /*
       * Note: The response code is defined as 201 (Created) since 1.3.2.
       */

      buildResponse(HttpServletResponse.SC_CREATED,
            MultiTenancyServlet.MIME_TEXT_PLAIN, "CREATED: " + namespace);

      return null;

   }

   @Override
   final public boolean isReadOnly() {
      return false;
   }

}
