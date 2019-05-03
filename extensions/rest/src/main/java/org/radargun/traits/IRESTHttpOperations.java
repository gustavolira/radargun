package org.radargun.traits;

import org.radargun.Operation;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MultivaluedMap;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * @author Martin Gencur
 */
@Trait(doc = "Http operations.")
public interface IRESTHttpOperations {
   String TRAIT = IRESTHttpOperations.class.getSimpleName();

   Operation GET = Operation.register(TRAIT + ".Get");

   RESTOperationInvoker getRESTInvoker(String contextPath);

   interface RESTOperationInvoker {
      HttpResponse<String> get(List<Cookie> cookies, MultivaluedMap<String, Object> headers);
   }
}
