
import java.util.ArrayList;
import java.util.List;

/**
 * Holds the café's menu items as an ORDERED ARRAY (kept sorted by ID at all
 * times) so that Binary Search can be used to locate items in O(log n).
 */
public class Inventory {

    public final List<MenuItem> items;

    public Inventory() {
        items = new ArrayList<>();
        seedDefaultItems();
    }

    // Default Menu Items
    private void seedDefaultItems() {
        addItem(new MenuItem(101, "Espresso", 3.50));
        addItem(new MenuItem(102, "Latte", 4.25));
        addItem(new MenuItem(103, "Cappuccino", 4.00));
        addItem(new MenuItem(104, "Americano", 3.00));
        addItem(new MenuItem(105, "Mocha", 4.75));
    }

    public int size(){
        return items.size();
    }

    // Display Menu Items
    public void displayCatalog() {
        if (items.isEmpty()) {
            IO.println("Menu is empty.");
            return;
        }
        IO.println("\n----- MENU (Ordered Array by ID) -----");
        for (MenuItem item : items) {
            IO.println(item);
        }
    }

    /**
     * Binary Search over the ID-sorted array.
     * Returns the index of the item with the given id, or -1 if not found.
     */
    private int binarySearch(int id) {
        int lo = 0;
        int hi = items.size() - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            int midId = items.get(mid).getId();
            if (midId == id) {
                return mid;
            } else if (midId < id) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return -1;
    }

    public boolean itemExists(int id) {
        return binarySearch(id) != -1;
    }

    public MenuItem getItemById(int id) {
        int index = binarySearch(id);
        return (index == -1) ? null : items.get(index);
    }

    public MenuItem get(int index){
        return items.get(index);
    }

    public boolean addItem(MenuItem newItem) {
        if (itemExists(newItem.getId())) {
            return false; // "Item Already Exists"
        }
        int insertPos = 0;
        while (insertPos < items.size() && items.get(insertPos).getId() < newItem.getId()) {
            insertPos++;
        }
        items.add(insertPos, newItem);
        return true;
    }

    public boolean removeItem(int id) {
        int index = binarySearch(id);
        if (index == -1) {
            return false; // "Item does not exist"
        }
        items.remove(index);
        return true;
    }

    public boolean editItem(int id, String newName, double newPrice) {
        MenuItem item = getItemById(id);
        if (item == null) {
            return false; // "Item Not Found"
        }
        item.setName(newName);
        item.setPrice(newPrice);
        return true;
    }

    /**
     * Returns a copy of the inventory sorted by price using Merge Sort.
     * Time Complexity: O(n log n)
     */
    public List<MenuItem> getMenuSortedByPrice(boolean ascending) {
        List<MenuItem> sortedList = new ArrayList<>(this.items);
        if (sortedList.size() <= 1) return sortedList;

        MenuItem[] temp = new MenuItem[sortedList.size()];
        mergeSort(sortedList, temp, 0, sortedList.size() - 1, ascending);
        return sortedList;
    }

    private void mergeSort(List<MenuItem> list, MenuItem[] temp, int left, int right, boolean ascending) {
        if (left < right) {
            int mid = left + (right - left) / 2;
            mergeSort(list, temp, left, mid, ascending);
            mergeSort(list, temp, mid + 1, right, ascending);
            merge(list, temp, left, mid, right, ascending);
        }
    }

    private void merge(List<MenuItem> list, MenuItem[] temp, int left, int mid, int right, boolean ascending) {
        for (int i = left; i <= right; i++) {
            temp[i] = list.get(i);
        }

        int i = left;
        int j = mid + 1;
        int k = left;

        while (i <= mid && j <= right) {
            boolean condition = ascending
                    ? temp[i].getPrice() <= temp[j].getPrice()
                    : temp[i].getPrice() >= temp[j].getPrice();

            if (condition) {
                list.set(k, temp[i]);
                i++;
            } else {
                list.set(k, temp[j]);
                j++;
            }
            k++;
        }

        while (i <= mid) {
            list.set(k, temp[i]);
            k++;
            i++;
        }
    }

}