import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

/**
 * VIP Membership & Loyalty Management System.
 *
 * DSA Concepts Utilized:
 * - Set Data Structure
 *   (Enforces strict uniqueness for Customer IDs and Promo Codes)
 *
 * Persistence:
 * - Stores registered members and active promo codes in users.json.
 */
public class Users {

    private static final String FILE_NAME = "json_files/users.json";

    private static final Gson gson = new GsonBuilder()
                        .registerTypeAdapter(MenuItem.class, new MenuItemDeserializer())
                        .setPrettyPrinting()
                        .create();

    private Set<String> registeredMemberIds;
    private Set<String> activePromoCodes;

    /**
     * Constructor.
     *
     * Loads existing data from users.json.
     * If the file doesn't exist, creates empty sets.
     */
    public Users() { load(); }

    /**
     * Registers a new member ensuring no duplicate IDs exist.
     */
    public boolean registerMember(String memberId) {

        if (registeredMemberIds.contains(memberId)) {
            System.out.println(
                "Error: Member ID "
                + memberId
                + " already exists!"
            );

            return false;
        }

        registeredMemberIds.add(memberId);

        save();

        System.out.println(
            "Success: Member ID "
            + memberId
            + " registered."
        );

        return true;
    }

    /**
     * Redeems a promo code ensuring it cannot be used twice.
     */
    public boolean redeemPromoCode(String promoCode) {

        if (!activePromoCodes.contains(promoCode)) {
            System.out.println(
                "Error: Promo code "
                + promoCode
                + " is invalid or already used."
            );

            return false;
        }

        activePromoCodes.remove(promoCode);

        save();

        System.out.println(
            "Success: Promo code "
            + promoCode
            + " applied!"
        );

        return true;
    }

    /**
     * Adds a new promo code.
     */
    public void addPromoCode(String promoCode) {
        activePromoCodes.add(promoCode);
        save();
    }

    /**
     * Saves the Users object to users.json.
     */
    private void save() {
        try (FileWriter writer = new FileWriter(FILE_NAME)) {
            gson.toJson(this, writer);
        } catch (IOException e) {
            System.err.println("Failed to save users.");
            e.printStackTrace();
        }
    }

    /**
     * Loads the Users object from users.json.
     */
    private void load() {

        File file = new File(FILE_NAME);

        if (!file.exists()) {
            registeredMemberIds = new HashSet<>();
            activePromoCodes = new HashSet<>();

            return;
        }

        try (FileReader reader = new FileReader(file)) {

            JsonElement root = JsonParser.parseReader(reader);

            if (root == null || !root.isJsonObject()) {
                registeredMemberIds = new HashSet<>();
                activePromoCodes = new HashSet<>();
                return;
            }

            JsonObject obj = root.getAsJsonObject();

            java.lang.reflect.Type setType = new TypeToken<HashSet<String>>() {}.getType();

            if (obj.has("registeredMemberIds")) {
                registeredMemberIds = gson.fromJson(obj.get("registeredMemberIds"), setType);
            } else registeredMemberIds = new HashSet<>();

            if (obj.has("activePromoCodes")) {
                activePromoCodes = gson.fromJson(obj.get("activePromoCodes"), setType);
            } else activePromoCodes = new HashSet<>();

            if (registeredMemberIds == null) registeredMemberIds = new HashSet<>();
            if (activePromoCodes == null) activePromoCodes = new HashSet<>();

        } catch (IOException e) {
            System.err.println("Failed to load users.");
            e.printStackTrace();

            registeredMemberIds = new HashSet<>();
            activePromoCodes = new HashSet<>();
        }
    }
}

