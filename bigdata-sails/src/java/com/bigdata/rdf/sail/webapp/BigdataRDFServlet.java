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
 * Created on Apr 13, 2011
 */

package com.bigdata.rdf.sail.webapp;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.Properties;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFWriterFactory;
import org.openrdf.rio.RDFWriterRegistry;

import com.bigdata.journal.IAtomicStore;
import com.bigdata.rdf.properties.PropertiesFormat;
import com.bigdata.rdf.properties.PropertiesWriter;
import com.bigdata.rdf.properties.PropertiesWriterRegistry;
import com.bigdata.rdf.rules.ConstraintViolationException;
import com.bigdata.rdf.sparql.ast.QuadsOperationInTriplesModeException;
import com.bigdata.util.InnerCause;

/**
 * Abstract base class for {@link Servlet}s which interact with the bigdata RDF
 * data and/or SPARQL query layers.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
abstract public class BigdataRDFServlet extends BigdataServlet {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    static private final transient Logger log = Logger.getLogger(BigdataRDFServlet.class);

    /**
     * The name of the <code>UTF-8</code> character encoding.
     */
    protected static final String UTF8 = "UTF-8";
    
    protected static final String NA = "N/A";
    
    /**
     * A SPARQL results set in XML.
     * 
     * @see http://www.w3.org/TR/rdf-sparql-XMLres/
     */
    static public final transient String MIME_SPARQL_RESULTS_XML = "application/sparql-results+xml";

    /**
     * A SPARQL results set in JSON.
     * 
     * @see http://www.w3.org/TR/rdf-sparql-json-res/
     */
    static public final transient String MIME_SPARQL_RESULTS_JSON = "application/sparql-results+json";

    /**
     * RDF/XML.
     */
    static public final transient String MIME_RDF_XML = "application/rdf+xml";

	public static final String MIME_SPARQL_QUERY = "application/sparql-query";

	public static final String MIME_SPARQL_UPDATE = "application/sparql-update";
	
	public static final String OUTPUT_FORMAT_QUERY_PARAMETER = "format";
	
	public static final String OUTPUT_FORMAT_JSON = "sparql-results+json";
	
	public static final String OUTPUT_FORMAT_XML = "sparql-results+xml";
	
	public static final String OUTPUT_FORMAT_JSON_SHORT = "json";
	
	public static final String OUTPUT_FORMAT_XML_SHORT = "xml";

    /**
     * 
     */
    public BigdataRDFServlet() {
        
    }

//    /**
//     * {@inheritDoc}
//     * <p>
//     * Note: Overridden to support read-only deployments.
//     * 
//     * @see SparqlEndpointConfig#readOnly
//     * @see ConfigParams#READ_ONLY
//     */
//    @Override
//    static boolean isWritable(final HttpServletRequest req,
//            final HttpServletResponse resp) throws IOException {
//        
//        if(getConfig().readOnly) {
//            
//            buildResponse(resp, HTTP_METHOD_NOT_ALLOWED, MIME_TEXT_PLAIN,
//                    "Not writable.");
//
//            // Not writable.  Response has been committed.
//            return false;
//            
//        }
//        
//        return super.isWritable(req, resp);
//        
//    }

    /**
	 * Best effort to write the stack trace onto the output stream so it will
	 * show up in the HTTP response. This code path should be used iff we have
	 * already begun writing the response. Otherwise, an HTTP error status
	 * should be used instead. REST API methods that have defined HTTP status
	 * codes (e.g., for {@link HttpServletResponse#SC_BAD_REQUEST}) should
	 * verify the request before proceeding in order to satisify their API
	 * semantics.
	 * <p>
	 * This method is invoked as follows:
	 * 
	 * <pre>
	 * launderThrowable(...)
	 * </pre>
	 * 
	 * This method MUST be invoked from the top-level where the request is
	 * handled. The servlet container may ABORT the connection if there is an
	 * attempt to write onto a closed response. This can cause EOF errors in the
	 * client.
	 * 
	 * @param t
	 *            The thrown error.
	 * @param os
	 *            The stream on which the response will be written.
	 * @param queryStr
	 *            The SPARQL Query -or- SPARQL Update command (if available)
	 *            -or- a summary of the REST API command -or- an empty string if
	 *            nothing else is more appropriate.
	 * 
	 * @see <a href="http://trac.blazegraph.com/ticket/1075" > LaunderThrowable
	 *      should never throw an exception </a>
	 */
    protected static void launderThrowable(final Throwable t,
            final HttpServletResponse resp, final String queryStr) {
        final boolean isQuery = queryStr != null && queryStr.length() > 0;
        try {
            // log an error for the service.
            log.error("cause=" + t + (isQuery ? ", query=" + queryStr : ""), t);
        } finally {
            // ignore any problems here.
        }
		if (resp == null) {
			// Nothing can be done.
			return;
		}
		if (!resp.isCommitted()) {
			/*
			 * Set appropriate status code.
			 * 
			 * Note: A committed response has already had its status code and
			 * headers written.
			 */
			if (InnerCause.isInnerCause(t, DatasetNotFoundException.class)) {
				/*
				 * The addressed KB does not exist.
				 */
				resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
				resp.setContentType(MIME_TEXT_PLAIN);
			} else if (InnerCause.isInnerCause(t,
					ConstraintViolationException.class)) {
				/*
				 * A constraint violation is a bad request (the data violates
				 * the rules) not a server error.
				 */
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				resp.setContentType(MIME_TEXT_PLAIN);
			} else if (InnerCause
					.isInnerCause(t, MalformedQueryException.class)) {
				/*
				 * Send back a BAD REQUEST (400) along with the text of the
				 * syntax error message.
				 * 
				 * TODO Write unit test for 400 response for bad client request.
				 */
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				resp.setContentType(MIME_TEXT_PLAIN);
			} else if (InnerCause.isInnerCause(t,
					QuadsOperationInTriplesModeException.class)) {
			   /*
             * Nice error when attempting to use quads data in a triples only
             * mode.
             * 
             * @see #1086 Loading quads data into a triple store should strip
             * out the context
             */
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				resp.setContentType(MIME_TEXT_PLAIN);
			} else if (InnerCause.isInnerCause(t, HttpOperationException.class)) {
            /*
             * An AbstractRestApiTask failed and throw out a typed exception to
             * avoid joining a commit group.
             * 
             * TODO The queryStr is ignored on this code path. Review the places
             * where the HttpOperationException is thrown and standardize the
             * information contained in its [content] as we have already done
             * for the [queryStr].
             */
            final HttpOperationException ex = (HttpOperationException) InnerCause
                  .getInnerCause(t, HttpOperationException.class);
            resp.setStatus(ex.status);
            resp.setContentType(ex.mimeType);
            try {
               final Writer w = resp.getWriter();
               if (ex.content != null)
                  w.write(ex.content); // write content iff given.
               w.flush(); // Commit the response.
            } catch (IOException ex2) {
               // ignore any problems here.
            }
            return;
			} else {
				// Internal server error.
				resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				resp.setContentType(MIME_TEXT_PLAIN);
			}
		}

      /*
       * Attempt to write the stack trace on the response.
       * 
       * Note: If the OutputStream (or Writer) has already been closed then an
       * IOException is caught and ignored.
       * 
       * Note: If the PrintWriter was already requested, then we use it directly
       * rather than the OutputStream. This is due to the servlet API which will
       * not allow us to obtain the OutputStream if we have already obtained the
       * Writer. Some of the response handling for our servlets use the Writer
       * (e.g., reportMutationCount()) so this happens if there is a problem
       * while using the writer. Of course, that also makes it likely that we
       * will not be able to inform the client of the error.
       */
      {
			OutputStream os = null;
         PrintWriter w = null;
			try {
			   try {
			      // first, assume OutputStream is available.
			      os = resp.getOutputStream();
				   w = new PrintWriter(os);
			   } catch(IllegalStateException ex) {
			      // Writer was already request, so we need to use it instead.
			      w = resp.getWriter();
			   }
				if (queryStr != null) {
					/*
					 * Write the query onto the output stream.
					 */
					w.write(queryStr);
					w.write("\n");
				}
				/*
				 * Write the stack trace onto the output stream.
				 */
				t.printStackTrace(w);
				w.flush(); // flush the writer.
            if (os != null) {
               os.flush(); // flush the output stream.
            }
			} catch (IOException ex) {
				// Could not write on output stream.
			} finally {
				// ignore any problems here.
			}
         if (w != null) {
            try {
               // ensure writer is closed.
               w.close();
            } catch (Throwable t2) {
               // ignore any problems here.
            }
         }
			if (os != null) {
				try {
					// ensure output stream is closed.
					os.close();
				} catch (Throwable t2) {
					// ignore any problems here.
				}
			}
		}
		/*
		 * Nothing is thrown.
		 */
    }

    /**
     * Return the timestamp which will be used to execute the query. The uri
     * query parameter <code>timestamp</code> may be used to communicate the
     * desired commit time against which the query will be issued. If that uri
     * query parameter is not given then the default configured commit time will
     * be used. Applications may create protocols for sharing interesting commit
     * times as reported by {@link IAtomicStore#commit()} or by a distributed
     * data loader (for scale-out).
     * 
     * @todo the configured timestamp should only be used for the default
     *       namespace (or it should be configured for each graph explicitly, or
     *       we should bundle the (namespace,timestamp) together as a single
     *       object).
     * 
     * @see QueryServlet#ATTR_TIMESTAMP;
     */
    protected long getTimestamp(final HttpServletRequest req) {
        
        final String timestamp = req.getParameter(QueryServlet.ATTR_TIMESTAMP);
        
        if (timestamp == null) {
            
            return getConfig(getServletContext()).timestamp;
            
        }

        return Long.valueOf(timestamp);

    }
    
    /**
     * Return the namespace which will be used to execute the query. The
     * namespace is represented by the first component of the URI. If there is
     * no namespace, then return the configured default namespace.
     * 
     * @param uri
     *            The URI path string.
     * 
     * @return The namespace.
     * 
     * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/689" >
     *      Missing URL encoding in RemoteRepositoryManager </a>
     */
    protected String getNamespace(final HttpServletRequest req) {
        
        final String uri = req.getRequestURI();
        
        final int snmsp = uri.indexOf("/namespace/");

        if (snmsp == -1) {

            String s = req.getParameter(BigdataRDFContext.NAMESPACE);

            if (s != null) {

                s = s.trim();
                
                if (s.length() > 0) {

                    // Specified as a query parameter.
                    return s;
                    
                }

            }
            
            // use the default namespace.
            return getConfig(getServletContext()).namespace;
            
        }

        // locate the next "/" in the URI path.
        final int beginIndex = uri.indexOf('/', snmsp + 1/* fromIndex */);

        // locate the next "/" in the URI path.
        int endIndex = uri.indexOf('/', beginIndex + 1/* fromIndex */);

        if (endIndex == -1) {
            // use the rest of the URI.
            endIndex = uri.length();
        }

        // return the namespace.
        final String t = uri.substring(beginIndex + 1, endIndex);
        String namespace;
        try {
            namespace = URLDecoder.decode(t, UTF8);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return namespace;
    }

    /**
     * Factory for the {@link PipedInputStream}.
     */
	final static protected PipedInputStream newPipedInputStream(
			final PipedOutputStream os) throws IOException {

		return new PipedInputStream(os);

	}

	/**
    * Report that a namespace is not found. The namespace is extracted from the
    * {@link HttpServletRequest}.
    */
   protected void buildAndCommitNamespaceNotFoundResponse(
         final HttpServletRequest req, final HttpServletResponse resp)
         throws IOException {

      buildAndCommitResponse(resp, HttpServletResponse.SC_NOT_FOUND,
            MIME_TEXT_PLAIN, "Not found: namespace=" + getNamespace(req)
      // +", timestamp="+getTimestamp(req)
      );

   }

    /**
     * Report an access path range count and elapsed time back to the user agent.
     * 
     * @param resp
     *            The response.
     * @param rangeCount
     *            The mutation count.
     * @param elapsed
     *            The elapsed time (milliseconds).
     * 
     * @throws IOException
     */
   static protected void buildAndCommitRangeCountResponse(
         final HttpServletResponse resp, final long rangeCount,
         final long elapsed) throws IOException {

        final StringWriter w = new StringWriter();
        
        final XMLBuilder t = new XMLBuilder(w);

        t.root("data").attr("rangeCount", rangeCount)
                .attr("milliseconds", elapsed).close();

        buildAndCommitResponse(resp, HTTP_OK, MIME_APPLICATION_XML, w.toString());

    }
        
    /**
     * Send an RDF Graph as a response using content negotiation.
     * 
     * @param req
     * @param resp
     */
    static protected void sendGraph(final HttpServletRequest req,
            final HttpServletResponse resp, final Graph g) throws IOException {
        /*
         * CONNEG for the MIME type.
         */
		final String acceptStr = ConnegUtil.getMimeTypeForQueryParameter(req
				.getParameter(BigdataRDFServlet.OUTPUT_FORMAT_QUERY_PARAMETER),
				req.getHeader("Accept")); 
		
        final ConnegUtil util = new ConnegUtil(acceptStr);

        // The best RDFFormat for that Accept header.
        RDFFormat format = util.getRDFFormat();

        if (format == null)
            format = RDFFormat.RDFXML;

		RDFWriterFactory writerFactory = RDFWriterRegistry.getInstance().get(
				format);

		if (writerFactory == null) {

			log.warn("No writer for format: format=" + format + ", Accept=\""
					+ acceptStr + "\"");

			format = RDFFormat.RDFXML;
			
			writerFactory = RDFWriterRegistry.getInstance().get(format);
			
		}

//        if (writerFactory == null) {
//
//			buildResponse(resp, HTTP_BADREQUEST, MIME_TEXT_PLAIN,
//					"No writer for format: Accept=\"" + acceptStr
//							+ "\", format=" + format);
//			
//			return;
//
//		}
		
        resp.setStatus(HTTP_OK);

        resp.setContentType(format.getDefaultMIMEType());

        final OutputStream os = resp.getOutputStream();
        try {
            final RDFWriter writer = writerFactory.getWriter(os);
            writer.startRDF();
            final Iterator<Statement> itr = g.iterator();
            while (itr.hasNext()) {
                final Statement stmt = itr.next();
                writer.handleStatement(stmt);
            }
            writer.endRDF();
            os.flush();
        } catch (RDFHandlerException e) {
        	// wrap and rethrow. will be handled by launderThrowable().
            throw new IOException(e);
        } finally {
            os.close();
        }
    }

    /**
     * Send a properties file as a response using content negotiation.
     * 
     * @param req
     * @param resp
     */
    static protected void sendProperties(final HttpServletRequest req,
            final HttpServletResponse resp, final Properties properties)
            throws IOException {
        /*
         * CONNEG for the MIME type.
         */
    	final String acceptStr = ConnegUtil.getMimeTypeForQueryParameter(req
				.getParameter(BigdataRDFServlet.OUTPUT_FORMAT_QUERY_PARAMETER),
				req.getHeader("Accept")); 
        
        final ConnegUtil util = new ConnegUtil(acceptStr);

        // The best format for that Accept header.
        PropertiesFormat format = util.getPropertiesFormat();

        if (format == null)
            format = PropertiesFormat.XML;

        resp.setStatus(HTTP_OK);

        resp.setContentType(format.getDefaultMIMEType());

        final OutputStream os = resp.getOutputStream();
		try {
			final PropertiesWriter writer = PropertiesWriterRegistry
					.getInstance().get(format).getWriter(os);
			writer.write(properties);
			os.flush();
		} finally {
			os.close();
		}
    }

    /**
     * Return <code>true</code> if the <code>Content-disposition</code> header
     * should be set to indicate that the response body should be handled as an
     * attachment rather than presented inline. This is just a hint to the user
     * agent. How the user agent handles this hint is up to it.
     * 
     * @param mimeType
     *            The mime type.
     *            
     * @return <code>true</code> if it should be handled as an attachment.
     */
    protected static boolean isAttachment(final String mimeType) {
        if(mimeType.equals(MIME_TEXT_PLAIN)) {
            return false;
        } else if(mimeType.equals(MIME_SPARQL_RESULTS_XML)) {
            return false;
        } else if(mimeType.equals(MIME_SPARQL_RESULTS_JSON)) {
            return false;
        } else if(mimeType.equals(MIME_APPLICATION_XML)) {
            return false;
        }
        return true;
    }
    
    /**
     * Convert an array of URI strings to an array of URIs.
     */
    static protected Resource[] toURIs(final String[] s) {
    	
    	if (s == null)
    		return null;
    	
    	if (s.length == 0)
    		return new Resource[0];

    	final Resource[] uris = new Resource[s.length];
    	
    	for (int i = 0; i < s.length; i++) {
    		
    		uris[i] = new URIImpl(s[i]);
    		
    	}
    	
    	return uris;
    	
    }
    
}
