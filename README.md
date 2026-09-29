<!-- ... existing code ... -->
## ✨ Key Features

### 🧠 Data Structures & Algorithms
*   **Ordered Array (`Inventory.java`):** Menu items are stored in a dynamically resizing array that is strictly maintained in ascending order by Item ID.
*   **Binary Search:** Locating menu items via ID operates in $O(\log n)$ time, ensuring lightning-fast lookups even with massive menus.
*   **Merge Sort:** A divide-and-conquer algorithm operating in $O(n \log n)$ time used to dynamically sort the menu by price (Ascending/Descending) on the fly, without breaking the original ID-based order.
*   **FIFO Queue (`OrderQueue.java`):** Orders are processed strictly First-In-First-Out using a thread-safe `ArrayDeque`.
*   **Thread Safety:** Critical data structures are synchronized to handle concurrent requests from the multithreaded web server and the main CLI thread without race conditions.

### 💻 Dual Interface Design
<!-- ... existing code ... -->
| **Data Structure** | **`ConcurrentHashMap`** | `HTTPServer.java` | Stores active authentication sessions mapping UUID Tokens to User Roles. Provides thread-safe, lock-stripped $O(1)$ lookups and insertions, safely handling simultaneous requests from the multithreaded HTTP worker pool. |
| **Data Structure** | **`Record`** | `Order.java` | An immutable data carrier structure. Used to safely construct and pass order details (Item ID, Quantity, Customer Alias) between the HTTP worker threads, the CLI main thread, and the core Queue without risk of external mutation. |

### ⚙️ Explicit Algorithms Implemented

| Algorithm | Time Complexity | File | Purpose & Usage |
| :--- | :--- | :--- | :--- |
| **Binary Search** | $O(\log n)$ | `Inventory.java` | Rapidly locates menu items by ID. Used by the API (`?id=X`) and CLI for searching, updating, and removing items. Requires the underlying array to be pre-sorted. |
| **Merge Sort** | $O(n \log n)$ | `Inventory.java` | Dynamically creates a price-sorted copy of the menu. Uses a divide-and-conquer approach. Triggered by the Web UI dropdown (`?sortBy=price`) and CLI option 9. |
| **Ordered Insertion** | Amortized $O(n)$ | `Inventory.java` | Maintains the strictly ascending ID order of the Inventory array during new item additions, serving as the mathematically necessary precondition for Binary Search. |

---

## 🚀 Getting Started
<!-- ... existing code ... -->
| `/api/login` | `POST` | *None* | Authenticates user and returns JWT token. | `{"username":"admin", "password":"..."}` |
| `/api/menu` | `GET` | *None* | Returns the full menu array. | N/A |
| `/api/menu?id=X`| `GET` | *None* | Performs a Binary Search and returns a specific item. | N/A |
| `/api/menu?sortBy=price`| `GET` | *None* | Performs a Merge Sort to return the menu ordered by price. Use `&desc=true` for High-to-Low. | N/A |
| `/api/menu` | `POST` | Manager | Adds a new MenuItem to the Inventory. | `{"id":106, "name":"Tea", "price":2.50}` |
| `/api/menu` | `PUT` | Manager | Edits an existing MenuItem. | `{"id":106, "name":"Green Tea", "price":2.75}` |
<!-- ... existing code ... -->
