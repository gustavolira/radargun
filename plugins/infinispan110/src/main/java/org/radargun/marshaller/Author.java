package org.radargun.marshaller;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * @author Gustavo Lira (glira@redhat.com)
 */

@ProtoDoc("@Indexed")
public class Author {

   @ProtoDoc("@Field(index=Index.YES, analyze = Analyze.YES, store = Store.NO)")
   @ProtoField(number = 1)
   final String name;

   @ProtoDoc("@Field(index=Index.YES, analyze = Analyze.YES, store = Store.NO)")
   @ProtoField(number = 2)
   final String surname;

   @ProtoFactory
   public Author(String name, String surname) {
      this.name = name;
      this.surname = surname;
   }

   public String getName() {
      return name;
   }

   public String getSurname() {
      return surname;
   }

   @Override
   public String toString() {
      return "Author{" +
              "name='" + name + '\'' +
              ", surname='" + surname + '\'' +
              '}';
   }

}