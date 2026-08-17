
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages the Cafe's catalog of items.
 * DSA Concepts Utilized:
 * - Ordered Array / ArrayList (Kept sorted by Item ID)
 * - Binary Search (O(log n) lookup time)
 * - Insertion Logic (Maintaining sort order on addition)
 */
public class Inventory {
    private List<MenuItem> menuCatalog;

    public Inventory() {
        this.menuCatalog = new ArrayList<>();
    }

    /**
     * Inserts an item while maintaining sorted order (Ordered Array).
     */
    public void addItem(MenuItem item) {
        menuCatalog.add(item);
        Collections.sort(menuCatalog); // Keeps catalog sorted for Binary Search
    }

    /**
     * Binary Search implementation to find a MenuItem by its Item ID.
     * Time Complexity: O(log n)
     */
    public MenuItem binarySearchById(int targetId) {
        int low = 0;
        int high = menuCatalog.size() - 1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            int midId = menuCatalog.get(mid).getItemId();

            if (midId == targetId) {
                return menuCatalog.get(mid);
            } else if (midId < targetId) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return null; // Item not found
    }

    public void displayCatalog() {
        System.out.println("=== CAFE MENU CATALOG ===");
        for (MenuItem item : menuCatalog) {
            System.out.println(item);
        }
    }

    public List<MenuItem> getMenuCatalog() {
        return menuCatalog;
    }
}

