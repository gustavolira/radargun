package org.radargun.http.service.http2;

import org.radargun.logging.Log;
import org.radargun.logging.LogFactory;
import org.radargun.traits.BasicOperations;

import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;

/**
 * Implementation of {@link BasicOperations} through the HTTP protocol, using the JAX-RS 2.0 client
 * api with RESTEasy implementation.
 *
 * @author Alan Field &lt;afield@redhat.com&gt;
 */
public class RESTHttp2CacheOperations implements BasicOperations {
   private static final Log log = LogFactory.getLog(RESTHttp2CacheOperations.class);
   private final RESTHttp2CacheService service;

   public RESTHttp2CacheOperations(RESTHttp2CacheService service) {
      this.service = service;
   }

   @Override
   public <K, V> HTTPCache<K, V> getCache(String cacheName) {
      if (service.isRunning()) {
         if (cacheName != null && (service.cacheName == null || !service.cacheName.equals(cacheName))) {
            throw new UnsupportedOperationException();
         }
         return new HTTPCache<K, V>();
      }
      return null;
   }

   protected class HTTPCache<K, V> implements Cache<K, V> {

      @Override
      public V get(K key) {
         return getInternal(key).value;
      }

      private WrappedValue<V> getInternal(K key) {
         V value = null;
         EntityTag eTag = null;
         if (service.isRunning()) {
            String target = service.buildUrl(key);
            HttpResponse<byte[]> response;
            try {

               HttpRequest.Builder httpRequestBuilder = service.getHttpRequestBuilder();
               URI uri = URI.create(target);
               response = service.getHttpClientBuilder().build().send(httpRequestBuilder.uri(uri).build(), HttpResponse.BodyHandlers.ofByteArray());

               if (response.statusCode() == Status.NOT_FOUND.getStatusCode()) {
                  log.warn("RESTHttp2CacheOperations.getInternal::Key: " + key + " does not exist in cache: "
                        + service.cacheName);
               } else {

                  eTag = null;
                  value = (V) response.body();
               }
            } catch (Exception e) {
               throw new RuntimeException("RESTHttp2CacheOperations::get request threw exception: " + target, e);
            }
         }
         return new WrappedValue<V>(eTag, value);
      }

      private class WrappedValue<W> {
         EntityTag eTag;
         W value;

         public WrappedValue(EntityTag eTag, W value) {
            super();
            this.eTag = eTag;
            this.value = value;
         }
      }

      @Override
      public boolean containsKey(K key) {
         if (service.isRunning()) {
            try {

               String target = service.buildUrl(key);
               HttpResponse<?> response;

               URI uri = new URI(target);;
               HttpRequest httpRequest = service.getHttpRequestBuilder().uri(uri).GET().build();
               response = service.getHttpClientBuilder().build().send(httpRequest, HttpResponse.BodyHandlers.ofString());

               if(response.statusCode() == Status.OK.getStatusCode()) {
                  return true;
               }

               if(response.statusCode() == Status.NOT_FOUND.getStatusCode()) {
                  return false;
               }

               throw new RuntimeException("RESTHttp2CacheOperations.containsKey::Unexpected HttpStatus: "
                       + response.statusCode() +" for request " + target);
            }
            catch (Exception e) {
               e.printStackTrace();
            }
         }
         return false;
      }

      @Override
      public void put(K key, V value) {
         putInternal(key, value, null);
      }

      private void putInternal(K key, V value, EntityTag eTag) {
         if (service.isRunning()) {
            HttpResponse<String> response = null;
            try {
               String target = service.buildUrl(key);
               HttpRequest.Builder putBuilder = service.getHttpRequestBuilder()
                       .uri(URI.create(target))
                       .PUT(BodyPublishers.ofByteArray(encodeObject(value)));

               if (eTag != null) {
                  // If the eTag doesn't match the current value for the key, then the put will fail
                  putBuilder.header(HttpHeaders.IF_MATCH, eTag.getValue());
               }
               response = service.getHttpClientBuilder().build().send(putBuilder.build(), HttpResponse.BodyHandlers.ofString());
               int status = response.statusCode();
               if (status != Status.OK.getStatusCode() && status != Status.CREATED.getStatusCode()
                     && status != Status.NO_CONTENT.getStatusCode()) {
                  throw new RuntimeException("RESTHttp2CacheOperations.put::Unexpected HttpStatus: " + status
                          + " for request " + target);
               }
            } catch (Exception e) {
               throw new RuntimeException(e);
            }
         }
      }

      @Override
      public V getAndPut(K key, V value) {
         V prevValue = null;
         EntityTag eTag = null;
         if (service.isRunning()) {
            if (containsKey(key)) {
               HTTPCache<K, V>.WrappedValue<V> wrap = getInternal(key);
               eTag = wrap.eTag;
               prevValue = wrap.value;
            }
            if (eTag == null) {
               put(key, value);
            } else {
               putInternal(key, value, eTag);
            }
         }
         return prevValue;
      }

      @Override
      public boolean remove(K key) {
         return doDelete(service.buildUrl(key), null);
      }

      public boolean remove(K key, EntityTag eTag) {
         return doDelete(service.buildUrl(key), eTag);
      }

      @Override
      public V getAndRemove(K key) {
         if (service.isRunning()) {
            if (containsKey(key)) {
               HTTPCache<K, V>.WrappedValue<V> wrap = getInternal(key);
               if (wrap.eTag != null) {
                  if (remove(key, wrap.eTag)) {
                     return wrap.value;
                  }
               } else {
                  if (remove(key)) {
                     return wrap.value;
                  }
               }
            }
         }
         return null;
      }

      @Override
      public void clear() {
         doDelete(service.buildCacheUrl(service.cacheName), null);
      }

      private boolean doDelete(String target, EntityTag eTag) {
         if (service.isRunning()) {

            try {
               HttpRequest.Builder deleteBuilder = service.getHttpRequestBuilder()
                       .uri(URI.create(target))
                       .DELETE();

               if (eTag != null) {
                  // If the eTag doesn't match the current value for the key, then the delete will fail
                  deleteBuilder = deleteBuilder.header(HttpHeaders.IF_MATCH, eTag.getValue());
               }
               HttpResponse response = service.getHttpClientBuilder().build().send(deleteBuilder.build(), HttpResponse.BodyHandlers.discarding());
               int status = response.statusCode();
               if (status == Status.OK.getStatusCode() || status == Status.NO_CONTENT.getStatusCode()) {
                  return true;
               }
               if (status == Status.NOT_FOUND.getStatusCode()) {
                  return false;
               }
               throw new RuntimeException("RESTEasyCacheOperations.doDelete::Unexpected HttpStatus: " + status
                       +" for request " + target);
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
         return false;
      }

      private byte[] encodeObject(Object object) throws IOException {
         ByteArrayOutputStream bout = new ByteArrayOutputStream();
         ObjectOutputStream oo = new ObjectOutputStream(bout);
         oo.writeObject(object);
         oo.flush();
         return bout.toByteArray();
      }

   }

}
