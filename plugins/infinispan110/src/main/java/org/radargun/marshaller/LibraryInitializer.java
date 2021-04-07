package org.radargun.marshaller;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

/**
 * @author Gustavo Lira (glira@redhat.com)
 */

@AutoProtoSchemaBuilder(
      includeClasses = {
              Book.class,
              Author.class
      },
      schemaFileName = "library.proto",
      schemaFilePath = "proto/",
      schemaPackageName = "book_sample")
interface LibraryInitializer extends SerializationContextInitializer {
}