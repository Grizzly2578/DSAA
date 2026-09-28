// --- Global State ---
let currentMenu = [];

// --- Initialization ---
document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    loadMenu();
    
    // Set up polling for the order queue (updates every 3 seconds)
    setInterval(() => {
        if (!document.getElementById('orders-section').classList.contains('hidden-section')) {
            loadOrders();
        }
    }, 3000);
});

// --- Navigation ---
function showSection(sectionId) {
    // Hide all sections
    document.getElementById('menu-section').classList.add('hidden-section');
    document.getElementById('menu-section').classList.remove('active-section');
    document.getElementById('orders-section').classList.add('hidden-section');
    document.getElementById('orders-section').classList.remove('active-section');
    
    // Reset nav links
    document.getElementById('nav-menu').classList.remove('active');
    document.getElementById('nav-orders').classList.remove('active');
    
    // Show target section
    document.getElementById(sectionId).classList.remove('hidden-section');
    document.getElementById(sectionId).classList.add('active-section');
    
    // Update nav link
    if (sectionId === 'menu-section') {
        document.getElementById('nav-menu').classList.add('active');
        loadMenu();
    } else {
        document.getElementById('nav-orders').classList.add('active');
        loadOrders();
    }
}

// --- Theme Handling ---
function initTheme() {
    const themeToggleBtn = document.getElementById('theme-toggle');
    const currentTheme = localStorage.getItem('theme') || 'light';
    
    document.documentElement.setAttribute('data-theme', currentTheme);
    updateThemeButton(currentTheme, themeToggleBtn);

    themeToggleBtn.addEventListener('click', () => {
        let theme = document.documentElement.getAttribute('data-theme');
        let newTheme = theme === 'dark' ? 'light' : 'dark';
        
        document.documentElement.setAttribute('data-theme', newTheme);
        localStorage.setItem('theme', newTheme);
        updateThemeButton(newTheme, themeToggleBtn);
    });
}

function updateThemeButton(theme, btn) {
    if (theme === 'dark') {
        btn.textContent = '☀️';
    } else {
        btn.textContent = '🌙';
    }
}

// --- API Calls & Rendering ---

async function loadMenu() {
    const grid = document.getElementById('menu-grid');
    
    try {
        const response = await fetch('/api/menu');
        if (!response.ok) throw new Error('Failed to fetch menu');
        
        currentMenu = await response.json();
        
        if (currentMenu.length === 0) {
            grid.innerHTML = '<div class="text-center" style="grid-column: 1 / -1;">No items currently available.</div>';
            return;
        }

        grid.innerHTML = '';
        currentMenu.forEach(item => {
            const card = document.createElement('div');
            card.className = 'card';
            card.innerHTML = `
                <div>
                    <h3 class="card-title">${item.name}</h3>
                    <div class="card-id">ID: ${item.id}</div>
                </div>
                <div>
                    <div class="card-price">$${item.price.toFixed(2)}</div>
                    <button class="btn btn-primary btn-full" onclick="openOrderModal(${item.id}, '${item.name}', ${item.price})">
                        Order Now
                    </button>
                </div>
            `;
            grid.appendChild(card);
        });

    } catch (error) {
        console.error('Error loading menu:', error);
        grid.innerHTML = '<div class="text-center" style="grid-column: 1 / -1; color: red;">Failed to load menu. Is the Java server running?</div>';
        showToast('Failed to load menu data.', 'error');
    }
}

async function loadOrders() {
    const tbody = document.getElementById('orders-table-body');
    const countDisplay = document.getElementById('pending-count');
    
    try {
        const response = await fetch('/api/orders');
        if (!response.ok) throw new Error('Failed to fetch orders');
        
        const orders = await response.json();
        countDisplay.textContent = orders.length;
        
        if (orders.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center">No pending orders.</td></tr>';
            return;
        }

        tbody.innerHTML = '';
        orders.forEach((order, index) => {
            // Try to find the item name from our cached menu
            const menuItem = currentMenu.find(item => item.id === order.itemId);
            const itemName = menuItem ? menuItem.name : 'Unknown Item';

            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>#${index + 1}</td>
                <td>${order.itemId}</td>
                <td>${itemName}</td>
                <td>${order.customerAlias || 'Guest'}</td>
                <td>${order.quantity}</td>
            `;
            tbody.appendChild(tr);
        });

    } catch (error) {
        console.error('Error loading orders:', error);
        tbody.innerHTML = '<tr><td colspan="5" class="text-center" style="color: red;">Failed to load queue.</td></tr>';
    }
}

async function submitOrder(event) {
    event.preventDefault(); // Prevent page reload
    
    const itemId = document.getElementById('order-item-id').value;
    const quantity = document.getElementById('order-quantity').value;
    const customerAlias = document.getElementById('customer-alias').value;
    
    const orderData = {
        itemId: parseInt(itemId),
        quantity: parseInt(quantity),
        customerAlias: customerAlias
    };

    try {
        const response = await fetch('/api/orders', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(orderData)
        });

        if (response.ok) {
            showToast('Order placed successfully!', 'success');
            closeModal();
            // Optional: Auto-switch to orders tab to see it
            // showSection('orders-section'); 
        } else {
            const err = await response.json();
            showToast(err.error || 'Failed to place order.', 'error');
        }
    } catch (error) {
        console.error('Error placing order:', error);
        showToast('Network error. Failed to place order.', 'error');
    }
}

async function fulfillNextOrder() {
    try {
        const response = await fetch('/api/orders', {
            method: 'DELETE'
        });

        if (response.ok) {
            const data = await response.json();
            showToast('Fulfilled order for Item ID: ' + data.itemId, 'success');
            loadOrders(); // Instantly refresh the queue
        } else {
            const err = await response.json();
            showToast(err.error || 'No orders to fulfill.', 'error');
        }
    } catch (error) {
        console.error('Error fulfilling order:', error);
        showToast('Network error. Failed to fulfill order.', 'error');
    }
}

// --- Modal Handling ---

function openOrderModal(id, name, price) {
    document.getElementById('order-item-id').value = id;
    document.getElementById('modal-item-name').textContent = name;
    document.getElementById('modal-item-price').textContent = `$${price.toFixed(2)}`;
    document.getElementById('order-quantity').value = 1; // reset default
    document.getElementById('customer-alias').value = ''; // reset default
    
    document.getElementById('order-modal').classList.remove('hidden');
}

function closeModal() {
    document.getElementById('order-modal').classList.add('hidden');
}

// Close modal if clicking outside the content box
window.onclick = function(event) {
    const modal = document.getElementById('order-modal');
    if (event.target === modal) {
        closeModal();
    }
}

// --- UI Helpers ---

function showToast(message, type = 'success') {
    const toast = document.getElementById('toast-container');
    const msgEl = document.getElementById('toast-message');
    
    msgEl.textContent = message;
    toast.className = `toast-container ${type}`;
    
    // Auto-hide after 3 seconds
    setTimeout(() => {
        toast.classList.add('hidden');
    }, 3000);
}