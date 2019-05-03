package org.radargun.http.service.http2;

import org.radargun.config.Property;
import org.radargun.http.service.AbstractRESTEasyService;
import org.radargun.logging.Log;
import org.radargun.logging.LogFactory;
import org.radargun.traits.Lifecycle;
import org.radargun.traits.ProvidesTrait;
import org.radargun.utils.KeyValueProperty;
import org.radargun.utils.RESTAddressListConverter;
import org.radargun.utils.TimeConverter;

import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

/**
 * Abstract REST client service using the JAX-RS 2.0 client api
 *
 */
public abstract class AbstractRESTHttp2Service implements Lifecycle {

   private static final Log log = LogFactory.getLog(AbstractRESTEasyService.class);

   private HttpClient.Builder httpBuilder = null;

   private HttpRequest.Builder httpRequestBuilder = null;

   @Property(doc = "The username to use on an authenticated server. Defaults to null.")
   private String username;

   @Property(doc = "The password of the username to use on an authenticated server. Defaults to null.")
   private String password;

   @Property(doc = "The content type used for put and get operations. Defaults to application/octet-stream.")
   private String contentType = "application/octet-stream";

   @Property(doc = "Timeout for socket. Default is 30 seconds.", converter = TimeConverter.class)
   private long socketTimeout = 30000;

   @Property(doc = "Timeout for connection. Default is 30 seconds.", converter = TimeConverter.class)
   private long connectionTimeout = 30000;

   @Property(doc = "The size of the connection pool. Default is unlimited.")
   private int maxConnections = 0;

   @Property(doc = "The number of connections to pool per url. Default is equal to <code>maxConnections</code>.")
   private int maxConnectionsPerHost = 0;

   @Property(doc = "Semicolon-separated list of server addresses.", converter = RESTAddressListConverter.class)
   private List<InetSocketAddress> servers;

   @Property(doc = "Http headers for request. Default is null", complexConverter = KeyValueProperty.KeyValuePairListConverter.class)
   private List<KeyValueProperty> httpHeaders;

   @ProvidesTrait
   public Lifecycle getLifecycle() {
      return this;
   }

   @Override
   public synchronized void start() {
      if (httpBuilder != null) {
         log.warn("Service already started");
         return;
      }

      this.httpRequestBuilder = HttpRequest.newBuilder();
      this.httpRequestBuilder.header("Accept", this.contentType);
      if (httpHeaders != null) {
         httpHeaders.forEach(keyValue ->
                 this.httpRequestBuilder.header(keyValue.getKey(), keyValue.getValue())
         );
      }

      this.httpBuilder = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectionTimeout));

      if (username != null) {
         httpBuilder.authenticator(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
               return new PasswordAuthentication(username, password.toCharArray());
            }
         });
      }
   }

   private static String basicAuth(String username, String password) {
      return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
   }

   @Override
   public synchronized void stop() {
      if (httpBuilder == null) {
         log.warn("Service not started");
         return;
      }

      httpBuilder = null;
   }

   @Override
   public synchronized boolean isRunning() {
      return httpBuilder != null;
   }

   public String getUsername() {
      return username;
   }

   public String getPassword() {
      return password;
   }

   public String getContentType() {
      return contentType;
   }

   public long getSocketTimeout() {
      return socketTimeout;
   }

   public long getConnectionTimeout() {
      return connectionTimeout;
   }

   public int getMaxConnections() {
      return maxConnections;
   }

   public int getMaxConnectionsPerHost() {
      return maxConnectionsPerHost;
   }

   public List<InetSocketAddress> getServers() {
      return servers;
   }

   public HttpClient.Builder getHttpClientBuilder() { return httpBuilder;
   }

   public HttpRequest.Builder getHttpRequestBuilder() { return  httpRequestBuilder;}
}
