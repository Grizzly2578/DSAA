import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

/**
 * Manages the Cafe's catalog of items.
 * DSA Concepts Utilized:
 * - Ordered Array / ArrayList (Kept sorted by Item ID)
 * - Binary Search (O(log n) lookup time)
 * - Insertion Logic (Maintaining sort order on addition)
 */
public class Inventory {

    private static final String FILE_NAME = "json_files/inventory.json";

    private static final Gson gson = new GsonBuilder()
                        .registerTypeAdapter(MenuItem.class, new MenuItemDeserializer())
                        .setPrettyPrinting()
                        .create();

    private List<MenuItem> menuCatalog;

    public Inventory() {
        load();
    }

    /**
     * Inserts an item while maintaining sorted order.
     */
    public void addItem(MenuItem item) {
        menuCatalog.add(item);
        Collections.sort(menuCatalog);
        save();
    }

    /**
     * Optional helper if you later want to remove items.
     */
    public boolean removeItem(int itemId) {
        boolean removed = menuCatalog.removeIf(item -> item.getItemId() == itemId);
        if (removed) save();
        return removed;
    }

    /**
     * Saves the entire inventory to inventory.json.
     */
    private void save() {
        try (FileWriter writer = new FileWriter(FILE_NAME)) {
            gson.toJson(menuCatalog, writer);
        } catch (IOException e) {
            System.err.println("Failed to save inventory.");
            e.printStackTrace();
        }
    }

    /**
     * Loads inventory.json if it exists.
     */
    private void load() {
        File file = new File(FILE_NAME);

        if (!file.exists()) {
            menuCatalog = new ArrayList<>();
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            JsonElement root = JsonParser.parseReader(reader);

            menuCatalog = new ArrayList<>();

            if (root != null && root.isJsonArray()) {
                JsonArray arr = root.getAsJsonArray();

                for (JsonElement el : arr) {
                    if (!el.isJsonObject()) continue;

                    JsonObject obj = el.getAsJsonObject();
                    String category = obj.has("category") ? obj.get("category").getAsString() : "";

                    if ("Beverage".equalsIgnoreCase(category)) {
                        Beverage b = gson.fromJson(obj, Beverage.class);
                        menuCatalog.add(b);
                    } else if ("Pastry".equalsIgnoreCase(category)) {
                        Pastry p = gson.fromJson(obj, Pastry.class);
                        menuCatalog.add(p);
                    }
                }
            }

            // Deduplicate by itemId (preserve first occurrence)
            Map<Integer, MenuItem> unique = new LinkedHashMap<>();
            for (MenuItem it : menuCatalog) unique.putIfAbsent(it.getItemId(), it);
            menuCatalog = new ArrayList<>(unique.values());

            Collections.sort(menuCatalog);

        } catch (IOException e) {
            System.err.println("Failed to load inventory.");
            e.printStackTrace();
            menuCatalog = new ArrayList<>();
        }
    }

    /**
     * Binary Search implementation to find a MenuItem by its Item ID.
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

        return null;
    }

    public void displayCatalog() {
        System.out.println("=== CAFE MENU CATALOG ===");

        for (MenuItem item : menuCatalog) System.out.println(item);
    }

    public List<MenuItem> getMenuCatalog() { return menuCatalog; }

    /**
     * Manually save after modifying existing objects.
     */
    public void saveInventory() {
        save();
    }
}

