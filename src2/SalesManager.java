import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Handles complex analytics, reports, and optimizations.
 *
 * DSA Concepts Utilized:
 * - Insertion Sort
 *   (In-Place Offline Sorting for End-of-Day Sales Reports)
 *
 * - Greedy Algorithm
 *   (Calculating cash change with minimum coins/bills)
 *
 * - Dynamic Programming / Recursion
 *   (0/1 Knapsack for Smart Combo Budget Optimizer)
 *
 * Persistence:
 * - Completed orders are stored in sales.json.
 */
public class SalesManager {

    private static final String FILE_NAME = "json_files/sales.json";

    private static final Gson gson =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    private List<Order> completedOrders;

    /**
     * Loads previously completed orders when the program starts.
     */
    public SalesManager() { load(); }

    /**
     * Records a completed order.
     */
    public void recordCompletedOrder(Order order) {
        completedOrders.add(order);
        save();
    }

    /**
     * Offline Algorithm:
     * End-of-Day Sales Sorting using Insertion Sort.
     *
     * Sorts orders in descending order based on total revenue.
     */
    public void generateEODReportInsertionSort() {
        int n = completedOrders.size();

        for (int i = 1; i < n; i++) {
            Order key = completedOrders.get(i);
            int j = i - 1;

            while (
                j >= 0 &&
                completedOrders.get(j).getTotalAmount()
                    < key.getTotalAmount()
            ) {
                completedOrders.set(
                    j + 1,
                    completedOrders.get(j)
                );

                j = j - 1;
            }

            completedOrders.set(j + 1, key);
        }

        System.out.println(
            "=== END OF DAY SALES REPORT "
            + "(Sorted Highest to Lowest Revenue) ==="
        );

        for (Order o : completedOrders) System.out.println(o);

        save();
    }

    /**
     * Greedy Algorithm:
     * Computes minimal change count for cash payments.
     */
    // Implement later

    /**
     * Dynamic Programming (0/1 Knapsack Approach):
     * Smart Combo Budget Optimizer.
     *
     * Selects items to maximize satisfaction within a given budget.
     */
    public void optimizeBudgetMeal(
            List<MenuItem> catalog,
            double budget) {

        // DP Knapsack implementation placeholder
        System.out.println(
            "DP Budget Optimizer stub ready for implementation."
        );
    }

    /**
     * Saves all completed orders to sales.json.
     */
    private void save() {

        try (FileWriter writer =
                new FileWriter(FILE_NAME)) {

            gson.toJson(completedOrders, writer);

        } catch (IOException e) {

            System.err.println(
                "Failed to save completed orders."
            );

            e.printStackTrace();
        }
    }

    /**
     * Loads completed orders from sales.json.
     */
    private void load() {
        File file = new File(FILE_NAME);

        // No file yet = empty sales history
        if (!file.exists()) {
            completedOrders = new ArrayList<>();
            return;
        }

        try (FileReader reader =
                new FileReader(FILE_NAME)) {

            Type listType =
                    new TypeToken<List<Order>>() {}
                            .getType();

            completedOrders =
                    gson.fromJson(reader, listType);

            // Protect against an empty/null JSON file
            if (completedOrders == null) {
                completedOrders = new ArrayList<>();
            }

        } catch (IOException e) {
            System.err.println(
                "Failed to load completed orders."
            );

            e.printStackTrace();
            completedOrders = new ArrayList<>();
        }
    }

    /**
     * Allows the program to manually save sales data if needed.
     */
    public void saveSales() { save(); }

    /**
     * Returns the completed orders.
     */
    public List<Order> getCompletedOrders() { return completedOrders; }
}
