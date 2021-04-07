package org.radargun.stages.cache.generators;

import org.radargun.config.DefinitionElement;
import org.radargun.config.Init;
import org.radargun.config.Property;
import org.radargun.logging.Log;
import org.radargun.logging.LogFactory;
import org.radargun.utils.RandomHelper;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * @author Gustavo Lira (glira@redhat.com)
 */
@DefinitionElement(name = "book", doc = "Creates book object (containing title, publication year, author, description).")
public class BookObjectGenerator implements ValueGenerator {

   private static final String bookObjectClassName ="org.radargun.marshaller.Book";
   private static final String authorObjectClassName ="org.radargun.marshaller.Author";
   private static final Integer MIN_LENGTH = 1;
   private static final Integer MAX_LENGTH = 20;
   private static Log log = LogFactory.getLog(BookObjectGenerator.class);
   private Class<?> bookObjectClass;
   private Class<?> authorObjectClass;
   private List listAuthor;

   @Property(doc = "Comma separeted list of titles")
   private List<String> titleList;
   @Property(doc = "Size of nested collection. Default is 1. If the value is smaller than 0, collection reference remains null.")
   private int nestedAuthorCollectionSize = 1;

   private Constructor<?> ctorBook;
   private Constructor<?> ctorAuthor;

   @Init
   public void init() {
      try {
         ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

         bookObjectClass = classLoader.loadClass(bookObjectClassName);
         authorObjectClass = classLoader.loadClass(authorObjectClassName);

         ctorBook = bookObjectClass.getConstructor(String.class, String.class, int.class, List.class);
         ctorAuthor = authorObjectClass.getConstructor(String.class, String.class);

         listAuthor = new ArrayList<>(nestedAuthorCollectionSize);
      } catch (Exception e) {
         // trace as this can happen on main node
         log.tracef(e, "Could not initialize generator %s", this);
      }
   }


   @Override
   public Object generateValue(Object key, int size, Random random) {
      if (ctorAuthor == null || ctorBook == null)
         throw new IllegalStateException("The generator was not properly initialized");

      int publicationYear = new Random().ints(2000, 2020).findFirst().getAsInt();
      String description = RandomHelper.randomString(MIN_LENGTH, MAX_LENGTH);

      try {
         if (nestedAuthorCollectionSize >= 0) {
            for (int i = 0; i < nestedAuthorCollectionSize; i++) {
               Object authorObj = ctorAuthor.newInstance(RandomHelper.randomString(MIN_LENGTH, MAX_LENGTH), RandomHelper.randomString(MIN_LENGTH, MAX_LENGTH));
               listAuthor.add(authorObj);
            }
         }
//         System.out.println("randomTitle: " +getRandomTitle());
//         System.out.println("description: " + description);
//         System.out.println("publication year: "+ publicationYear);
//         System.out.println("listauthor: " + listAuthor);

         return ctorBook.newInstance(getRandomTitle(), description, publicationYear, listAuthor);
      } catch (Exception e) {
         throw new IllegalStateException("Failed to generate value", e);
      }
   }

   @Override
   public int sizeOf(Object value) {
      // FIXME
      return -1;
   }

   @Override
   public boolean checkValue(Object value, Object key, int expectedSize) {
      // FIXME
      return value.getClass().equals(bookObjectClass);
   }

   private String getRandomTitle() {
      Random r = new Random();
      List<String> titles = titleList == null ? getSomeMovieTitles() : titleList;
      int randomItem = r.nextInt(titles.size());
      return titles.get(randomItem);
   }

   private List<String> getSomeMovieTitles() {
      return Arrays.asList("x-men", "blade", "batman", "king kong", "star wars", "the avengers");
   }
}
