package gov.nist.p25.issi.utils;

import java.util.Iterator;

/**
 * A utility Iterator class that supresses the remove operation - for read only
 * collections. This class hides the remove operation by throwing an Unsupported
 * operation exception.
 * 
 * @author M. Ranganathan
 * 
 * @param <T> -
 *            base type.
 */
public class ReadOnlyIterator<T> implements Iterator<T> {

   private Iterator<T> myIterator;

   public ReadOnlyIterator(Iterator<T> myIterator) {
      this.myIterator = myIterator;
   }

   public boolean hasNext() {
      return myIterator.hasNext();
   }

   public T next() {
      return myIterator.next();
   }

   public void remove() {
      throw new UnsupportedOperationException("operation not supported");
   }
}
