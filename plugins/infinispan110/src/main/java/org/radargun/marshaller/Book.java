package org.radargun.marshaller;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoDoc;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gustavo Lira (glira@redhat.com)
 */

@ProtoDoc("@Indexed")
public class Book {

   @ProtoDoc("@Field(index=Index.YES, analyze = Analyze.YES, store = Store.NO)")
   @ProtoField(number = 1)
   final String title;

   @ProtoDoc("@Field(index=Index.YES, analyze = Analyze.YES, store = Store.NO)")
   @ProtoField(number = 2)
   final String description;

   @ProtoDoc("@Field(index=Index.YES, analyze = Analyze.YES, store = Store.NO)")
   @ProtoField(number = 3, defaultValue = "0")
   final int publicationYear;

   @ProtoDoc("@Field(index=Index.YES, analyze = Analyze.YES, store = Store.NO)")
   @ProtoField(number = 4, collectionImplementation = ArrayList.class)
   final List<Author> authors;

   @ProtoFactory
   public Book(String title, String description, int publicationYear, List<Author> authors) {
      this.title = title;
      this.description = description;
      this.publicationYear = publicationYear;
      this.authors = authors;
   }

   public String getTitle() {
      return title;
   }

   public String getDescription() {
      return description;
   }

   public int getPublicationYear() {
      return publicationYear;
   }

   public List<Author> getAuthors() {
      return authors;
   }

   @Override
   public String toString() {
      return "Book{" +
              "title='" + title + '\'' +
              ", description='" + description + '\'' +
              ", publicationYear=" + publicationYear +
              ", authors=" + authors +
              '}';
   }
}
