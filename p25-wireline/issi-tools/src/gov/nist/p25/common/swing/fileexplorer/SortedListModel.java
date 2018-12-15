//
package gov.nist.p25.common.swing.fileexplorer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.AbstractListModel;

/**
 * Download from:
 * http://www.java2s.com/Tutorial/Java/0240__Swing/SortedListModelsortableJList.htm
 */
@SuppressWarnings("unchecked")
public class SortedListModel extends AbstractListModel {

   private static final long serialVersionUID = -1L;
   private SortedSet<Object> model;

   // constructor
   public SortedListModel() {
      model = new TreeSet<Object>();
   }

   public int getSize() {
      return model.size();
   }

   public Object getElementAt(int index) {
      return model.toArray()[index];
   }

   public void add(Object element) {
      if (model.add(element)) {
         fireContentsChanged(this, 0, getSize());
      }
   }

   public void addAll(Object elements[]) {
      Collection<Object> c = Arrays.asList(elements);
      model.addAll(c);
      fireContentsChanged(this, 0, getSize());
   }

   public void clear() {
      model.clear();
      fireContentsChanged(this, 0, getSize());
   }

   public boolean contains(Object element) {
      return model.contains(element);
   }

   public Object firstElement() {
      return model.first();
   }

   public Iterator iterator() {
      return model.iterator();
   }

   public Object lastElement() {
      return model.last();
   }

   public boolean remove(Object element) {
      return removeElement(element); 
   }
   public boolean removeElement(Object element) {
      boolean removed = model.remove(element);
      if (removed) {
         fireContentsChanged(this, 0, getSize());
      }
      return removed;
   }
}
