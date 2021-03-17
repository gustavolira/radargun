package org.radargun.service;

import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.radargun.Service;
import org.radargun.marshaller.LibraryInitializerImpl;

@Service(doc = Infinispan60HotrodService.SERVICE_DESCRIPTION)
public class Infinispan110HotrodService extends Infinispan100HotrodService {

   @Override
   protected ConfigurationBuilder getDefaultHotRodConfig() {
      ConfigurationBuilder builder = super.getDefaultHotRodConfig();
      builder.addContextInitializer(new LibraryInitializerImpl());
      return builder;
   }

   @Override
   protected Infinispan110HotrodQueryable createQueryable() {
      return new Infinispan110HotrodQueryable(this);
   }
}
