# Java DSA Cafe Management System

![NetBeans Cafe Logo](web/assets/images/logo.jpg)

## ☕ Fuel For Your Code

Welcome to the Java DSA Cafe Management System! This project is a dual-interface application demonstrating the practical application of Data Structures and Algorithms (DSA) in Java. It features a robust Command-Line Interface (CLI) for system administrators and a beautiful, fully functional Web Dashboard for cafe staff, both powered by a custom-built Java HTTP Server.

This project was built from scratch with **zero external dependencies** (no Spring Boot, no Maven/Gradle required) to showcase core Java networking and algorithmic capabilities.

---

## ✨ Key Features

### 🧠 Data Structures & Algorithms
*   **Ordered Array (`Inventory.java`):** Menu items are stored in a dynamically resizing array that is strictly maintained in ascending order by Item ID.
*   **Binary Search:** Locating menu items via ID operates in $O(\log n)$ time, ensuring lightning-fast lookups even with massive menus.
*   **FIFO Queue (`OrderQueue.java`):** Orders are processed strictly First-In-First-Out using a thread-safe `ArrayDeque`.
*   **Thread Safety:** Critical data structures are synchronized to handle concurrent requests from the multithreaded web server and the main CLI thread without race conditions.

### 💻 Dual Interface Design
*   **Console CLI:** A comprehensive terminal interface offering raw, immediate access to all system functions. Perfect for backend management and server monitoring.
*   **Web Dashboard:** A responsive, modern Single Page Application (SPA) built with vanilla HTML/CSS/JS. It communicates with the Java backend via REST APIs.

### 🔒 Role-Based Access Control (RBAC) & Security
The web application features a secure, token-based authentication system built directly into the custom Java HTTP Server.
*   **Session Management:** Generates unique `UUID` Bearer tokens upon successful login, stored securely in a `ConcurrentHashMap`.
*   **Manager Role:** Full access. Can view queues, fulfill orders, and manage the menu (Add/Edit/Delete items).
*   **Barista Role:** Operational access. Can view the menu, place orders, and fulfill orders in the queue.
*   **Path Traversal Protection:** The static file server includes robust normalization checks to prevent unauthorized access to files outside the `web` directory.

### 🎨 Modern Web UI
*   **Light/Dark Mode:** Built-in theme toggling that remembers user preference via `localStorage`.
*   **Real-time Polling:** The Order Queue automatically updates every 3 seconds to reflect new orders placed via the CLI or other web clients.
*   **Toast Notifications:** Clean, unobtrusive UI feedback for API success/error states.
*   **Responsive Grid:** The menu adapts beautifully to any screen size.

---

## 🏗️ Data Structures & ADTs Architecture

This project intentionally isolates Abstract Data Types (ADTs) and relies on specific concrete Data Structures to ensure optimal performance, memory efficiency, and thread safety across the dual-interface system.

| Category | Name | File / Component | Purpose & Usage |
| :--- | :--- | :--- | :--- |
| **ADT** | **Ordered List** | `Inventory.java` | Manages the menu catalog. Strictly enforces sorted order by Item ID upon insertion to guarantee preconditions for algorithms like Binary Search. |
| **ADT** | **FIFO Queue** | `OrderQueue.java` | Manages pending customer orders. Ensures strictly First-In-First-Out (FIFO) processing for fair and chronological order fulfillment. |
| **Data Structure** | **Dynamic Array (`ArrayList`)** | `Inventory.java` | The underlying storage for the Ordered List ADT. Provides memory-contiguous $O(1)$ random access, which is mathematically required to achieve $O(\log n)$ efficiency during Binary Search. |
| **Data Structure** | **`ArrayDeque`** | `OrderQueue.java` | The underlying storage for the Queue ADT. Provides amortized $O(1)$ enqueue and dequeue operations at both ends. Chosen over `LinkedList` for superior memory efficiency and CPU cache locality (no node allocation overhead). |
| **Data Structure** | **`ConcurrentHashMap`** | `HTTPServer.java` | Stores active authentication sessions mapping UUID Tokens to User Roles. Provides thread-safe, lock-stripped $O(1)$ lookups and insertions, safely handling simultaneous requests from the multithreaded HTTP worker pool. |
| **Data Structure** | **`Record`** | `Order.java` | An immutable data carrier structure. Used to safely construct and pass order details (Item ID, Quantity, Customer Alias) between the HTTP worker threads, the CLI main thread, and the core Queue without risk of external mutation. |

---

## 🚀 Getting Started

### Prerequisites
*   Java Development Kit (JDK) 25 or higher.
*   An IDE (IntelliJ IDEA recommended).

### Project Structure
```text
DSAA/
├── src/
│   ├── HTTPServer.java       # Custom multi-threaded web server & REST API
│   ├── Inventory.java        # Ordered Array implementation w/ Binary Search
│   ├── Main.java             # Entry point & CLI loop
│   ├── MenuItem.java         # Data model
│   ├── Order.java            # Data model (Record)
│   └── OrderQueue.java       # Thread-safe FIFO Queue implementation
└── web/
    ├── index.html            # SPA Entry point & Layout
    └── assets/
        ├── css/
        │   └── style.css     # Theming, Layouts, and Animations
        ├── js/
        │   └── app.js        # API Calls, DOM Manipulation, State Management
        └── images/
            └── logo.jpg      # Branding
```

### Running the Application

1.  **Clone or Download** the project to your local machine.
2.  **Open the project** in IntelliJ IDEA (or your preferred IDE).
3.  Ensure the `src` folder is marked as your Sources Root.
4.  Run `Main.java`.

You will see the HTTP Server start in the console, followed by the CLI menu:
```text
Cafe Server running at: http://localhost:8080/
==========================================
     WELCOME TO JAVA DSA CAFE SYSTEM      
==========================================

Select an option:
1. Search Item by ID (Binary Search)
...
```

### Accessing the Web Dashboard
1.  Open your web browser and navigate to `http://localhost:8080/`.
2.  Log in using one of the demo accounts:
    *   **Manager:** `admin` / `admin123`
    *   **Barista:** `barista` / `coffee123`

---

## 📡 REST API Documentation

The `HTTPServer.java` exposes the following endpoints:

| Endpoint | Method | Role Required | Description | Request Body Example |
| :--- | :--- | :--- | :--- | :--- |
| `/api/login` | `POST` | *None* | Authenticates user and returns JWT token. | `{"username":"admin", "password":"..."}` |
| `/api/menu` | `GET` | *None* | Returns the full menu array. | N/A |
| `/api/menu?id=X`| `GET` | *None* | Performs a Binary Search and returns a specific item. | N/A |
| `/api/menu` | `POST` | Manager | Adds a new MenuItem to the Inventory. | `{"id":106, "name":"Tea", "price":2.50}` |
| `/api/menu` | `PUT` | Manager | Edits an existing MenuItem. | `{"id":106, "name":"Green Tea", "price":2.75}` |
| `/api/menu?id=X`| `DELETE` | Manager | Removes item ID 'X' from Inventory. | N/A |
| `/api/orders` | `GET` | Barista/Manager | Returns the current pending OrderQueue. | N/A |
| `/api/orders` | `POST` | Barista/Manager | Enqueues a new Order. | `{"itemId":101, "quantity":2, "customerAlias":"John"}` |
| `/api/orders` | `DELETE`| Barista/Manager | Dequeues (fulfills) the next Order. | N/A |

*Note: All endpoints support `OPTIONS` requests for CORS preflight.*

---

## 🛠️ Built With

*   **Java 27:** Application logic, Data Structures, and standard library `com.sun.net.httpserver`.
*   **HTML5/CSS3:** Semantic structure and custom styling (CSS Variables for theming).
*   **Vanilla JavaScript (ES6+):** Asynchronous `fetch` API, DOM manipulation, and LocalStorage.

## 📝 License
This project is open-source and created for educational purposes demonstrating Data Structures and Algorithms. Feel free to use, modify, and distribute it!
