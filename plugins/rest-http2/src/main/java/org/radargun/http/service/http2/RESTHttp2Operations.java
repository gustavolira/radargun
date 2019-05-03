package org.radargun.http.service.http2;

import org.radargun.logging.Log;
import org.radargun.logging.LogFactory;
import org.radargun.traits.IRESTHttpOperations;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Random;

/**
 *
 * @author Martin Gencur
 */
public class RESTHttp2Operations implements IRESTHttpOperations {
   private static final Log log = LogFactory.getLog(RESTHttp2Operations.class);
   private final RESTHttp2Service service;

   public RESTHttp2Operations(RESTHttp2Service service) {
      this.service = service;
   }

   public IRESTHttpOperations.RESTOperationInvoker getRESTInvoker(String contextPath) {
      if (service.isRunning()) {
         return new RESTOperationInvokerImpl(contextPath);
      }
      return null;
   }

   protected class RESTOperationInvokerImpl implements IRESTHttpOperations.RESTOperationInvoker {

      private String uri;

      public RESTOperationInvokerImpl(String contextPath) {
         this.uri = buildApplicationUrl(contextPath);
      }

      private String buildApplicationUrl(String contextPath) {
         InetSocketAddress node = pickServer();
         StringBuilder s = new StringBuilder("http://");
         if (service.getUsername() != null) {
            try {
               s.append(URLEncoder.encode(service.getUsername(), "UTF-8")).append(":")
                  .append(URLEncoder.encode(service.getPassword(), "UTF-8")).append("@");
            } catch (UnsupportedEncodingException e) {
               throw new RuntimeException("Could not encode the supplied username and password", e);
            }
         }
         s.append(node.getHostName()).append(":").append(node.getPort()).append("/");
         s.append(contextPath);
         log.info("buildApplicationUrl = " + s.toString());
         return s.toString();
      }

      /* There's one server picked for each thread at the beginning. Subsequent requests from this
      thread go to the same server. */
      private InetSocketAddress pickServer() {
         return service.getServers().get(service.getServersLoadBalance().next(new Random()));
      }

      private HttpCookie parseCookies(Cookie cookie) {
         HttpCookie httpCookie = new HttpCookie(cookie.getName(), cookie.getValue());
         httpCookie.setDomain(cookie.getDomain());
         httpCookie.setPath(cookie.getPath());
         httpCookie.setVersion(cookie.getVersion());
         return httpCookie;

      }

   @Override
      public HttpResponse<String> get(List<Cookie> cookiesToPass, MultivaluedMap<String, Object> headersToPass) {
      HttpResponse<String> response = null;
         if (service.isRunning()) {
            try {

               CookieManager cm = new CookieManager();
               HttpRequest.Builder httpRequestBuilder = service.getHttpRequestBuilder();
               URI uri = new URI(this.uri);
               httpRequestBuilder.uri(uri);

               for (Cookie cookie : cookiesToPass) {
                  cm.getCookieStore().add(uri, parseCookies(cookie));
               }

               service.getHttpClientBuilder().cookieHandler(cm);
               response = service.getHttpClientBuilder().build().send(httpRequestBuilder.build(), HttpResponse.BodyHandlers.ofString());

               if (response.statusCode() == Status.NOT_FOUND.getStatusCode()) {
                  log.warn("The requested URI does not exist");
               }
            } catch (Exception e) {
               throw new RuntimeException("RESTHttp2Operations::get request threw exception: " + uri, e);
            }
         }
         return response;
      }
   }
}