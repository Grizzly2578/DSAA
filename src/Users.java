
import java.util.HashSet;
import java.util.Set;

/**
 * VIP Membership & Loyalty Management System.
 * DSA Concepts Utilized:
 * - Set Data Structure (Enforces strict uniqueness for Customer IDs and Promo Codes)
 */
public class Users {
    private Set<String> registeredMemberIds;
    private Set<String> activePromoCodes;

    public Users() {
        this.registeredMemberIds = new HashSet<>();
        this.activePromoCodes = new HashSet<>();
    }

    /**
     * Registers a new member ensuring no duplicate IDs exist (Set Uniqueness constraint).
     */
    public boolean registerMember(String memberId) {
        if (registeredMemberIds.contains(memberId)) {
            System.out.println("Error: Member ID " + memberId + " already exists!");
            return false;
        }
        registeredMemberIds.add(memberId);
        System.out.println("Success: Member ID " + memberId + " registered.");
        return true;
    }

    /**
     * Redeems a promo code ensuring it cannot be used twice.
     */
    public boolean redeemPromoCode(String promoCode) {
        if (!activePromoCodes.contains(promoCode)) {
            System.out.println("Error: Promo code " + promoCode + " is invalid or already used.");
            return false;
        }
        activePromoCodes.remove(promoCode); // Enforce single-use
        System.out.println("Success: Promo code " + promoCode + " applied!");
        return true;
    }

    public void addPromoCode(String promoCode) {
        activePromoCodes.add(promoCode);
    }
}

