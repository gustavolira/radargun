package org.radargun.http.service.http2;

import org.radargun.traits.CacheInformation;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Providing cache info over resteasy-client.
 * @author Anna Manukyan
 */
public class RESTHttp2CacheInfo implements CacheInformation {
   private static final String CONTENT_TYPE = "text/plain";
   private RESTHttp2CacheService service;

   public RESTHttp2CacheInfo(RESTHttp2CacheService service) {
      this.service = service;
   }

   @Override
   public String getDefaultCacheName() {
      return service.cacheName;
   }

   @Override
   public Collection<String> getCacheNames() {
      return Arrays.asList(service.cacheName);
   }

   @Override
   public Cache getCache(String cacheName) {
      return new Cache(service.getHttpClientBuilder().build());
   }

   protected class Cache implements CacheInformation.Cache {
      protected HttpClient httpClient;

      public Cache(HttpClient httpClient) {
         this.httpClient = httpClient;
      }

      @Override
      public long getOwnedSize() {
         return -1;
      }

      @Override
      public long getLocallyStoredSize() {
         return -1;
      }

      @Override
      public long getMemoryStoredSize() {
         return -1;
      }

      @Override
      public long getTotalSize() {
         long size = 0;
         if (service.isRunning()) {
            String target = service.buildUrl(null);
            String responseStr = null;
            try {

               HttpRequest.Builder httpRequestBuilder = service.getHttpRequestBuilder();
               URI uri = URI.create(target);
               httpRequestBuilder.uri(uri);
               httpRequestBuilder.header("Accept", CONTENT_TYPE);

               HttpResponse<String> httpResponse = service.getHttpClientBuilder().build().send(httpRequestBuilder.build(), HttpResponse.BodyHandlers.ofString());

               responseStr = httpResponse.body();
               size = Long.parseLong(responseStr);    //if the call was done to nodejs app, then the long size is returned

            } catch (NumberFormatException ex) {
               //If the call was done to Infinispan Server, then the response is the list of all entries
               //WARNING: As this way the REST API retrieves all keys from the cache and returns them as a response,
               //this part of block may have an impact on the performance of the stage which rely on this trait.
               String[] entries = responseStr.split(System.getProperty("line.separator"));
               size = entries.length;
            } catch (Exception e) {
               throw new RuntimeException("RESTHttp2CacheInfo::size request threw exception: " + target, e);
            }
         }
         return size;
      }

      @Override
      public Map<?, Long> getStructuredSize() {
         return new HashMap<>();
      }

      @Override
      public int getNumReplicas() {
         return -1;
      }

      @Override
      public int getEntryOverhead() {
         return -1;
      }
   }
}

