// --- Global State ---
let currentMenu = [];
let shoppingCart = [];
let selectedItem = null;
let authToken = localStorage.getItem('token');
let currentUserRole = localStorage.getItem('role');

// --- Initialization ---
document.addEventListener('DOMContentLoaded', () => {
    initTheme();

    if (authToken && currentUserRole) {
        showApp();
    } else {
        showSection('login-section');
    }

    // Set up polling for the order queue (updates every 3 seconds)
    setInterval(() => {
        if (!document.getElementById('orders-section').classList.contains('hidden-section')) {
            loadOrders();
        }
    }, 3000);
});

async function fetchAuth(url, options = {}) {
    if (!options.headers) options.headers = {};
    if (authToken) options.headers['Authorization'] = 'Bearer ' + authToken;

    const response = await fetch(url, options);
    if (response.status === 401 || response.status === 403) {
        logout();
        showToast('Session expired or access denied.', 'error');
    }
    return response;
}

function requireAuthentication() {
    const validRole = currentUserRole === 'MANAGER' || currentUserRole === 'BARISTA';
    if (authToken && validRole) return true;

    logout();
    showToast('Please log in before placing an order.', 'error');
    return false;
}

// --- Navigation & Auth ---
async function handleLogin(e) {
    e.preventDefault();
    const user = document.getElementById('login-user').value;
    const pass = document.getElementById('login-pass').value;

    try {
        const res = await fetch('/api/login', {
            method: 'POST',
            body: JSON.stringify({ username: user, password: pass })
        });

        if (res.ok) {
            const data = await res.json();
            authToken = data.token;
            currentUserRole = data.role;
            localStorage.setItem('token', authToken);
            localStorage.setItem('role', currentUserRole);
            showToast('Login successful', 'success');
            showApp();
        } else {
            showToast('Invalid credentials', 'error');
        }
    } catch (err) {
        showToast('Network error', 'error');
    }
}

function logout() {
    if (authToken) {
        fetch('/api/logout', {
            method: 'POST',
            headers: { Authorization: 'Bearer ' + authToken }
        }).catch(() => {});
    }
    authToken = null;
    currentUserRole = null;
    shoppingCart = [];
    localStorage.removeItem('token');
    localStorage.removeItem('role');

    document.getElementById('login-user').value = '';
    document.getElementById('login-pass').value = '';

    document.getElementById('main-header').classList.add('hidden-section');
    document.getElementById('main-nav').classList.add('hidden');
    document.getElementById('mobile-bottom-nav').classList.add('hidden');
    renderCart();
    showSection('login-section');
}

function showApp() {
    document.getElementById('main-header').classList.remove('hidden-section');
    document.getElementById('main-nav').classList.remove('hidden');
    document.getElementById('mobile-bottom-nav').classList.remove('hidden');
    showSection('menu-section');
    applyRBAC();
}

function applyRBAC() {
    const managerElements = document.querySelectorAll('.manager-only');
    managerElements.forEach(el => {
        if (currentUserRole === 'MANAGER') {
            el.classList.remove('hidden');
        } else {
            el.classList.add('hidden');
        }
    });
}

function showSection(sectionId) {
    if (sectionId === 'summary-section' && currentUserRole !== 'MANAGER') {
        showToast('Summary is available to managers only.', 'error');
        sectionId = 'menu-section';
    }

    // Hide all sections
    document.getElementById('login-section').classList.add('hidden-section');
    document.getElementById('login-section').classList.remove('active-section');
    document.getElementById('menu-section').classList.add('hidden-section');
    document.getElementById('menu-section').classList.remove('active-section');
    document.getElementById('orders-section').classList.add('hidden-section');
    document.getElementById('orders-section').classList.remove('active-section');
    document.getElementById('summary-section').classList.add('hidden-section');
    document.getElementById('summary-section').classList.remove('active-section');

    // Reset nav links (only if showing app sections)
    if(sectionId !== 'login-section') {
        document.getElementById('nav-menu').classList.remove('active');
        document.getElementById('nav-orders').classList.remove('active');
        document.getElementById('nav-summary').classList.remove('active');
        document.getElementById('mobile-nav-menu').classList.remove('active');
        document.getElementById('mobile-nav-orders').classList.remove('active');
        document.getElementById('mobile-nav-summary').classList.remove('active');
    }

    // Show target section
    document.getElementById(sectionId).classList.remove('hidden-section');
    document.getElementById(sectionId).classList.add('active-section');

    // Update nav link
    if (sectionId === 'menu-section') {
        document.getElementById('nav-menu').classList.add('active');
        document.getElementById('mobile-nav-menu').classList.add('active');
        loadMenu();
    } else if (sectionId === 'orders-section') {
        document.getElementById('nav-orders').classList.add('active');
        document.getElementById('mobile-nav-orders').classList.add('active');
        loadOrders();
    } else if (sectionId === 'summary-section') {
        document.getElementById('nav-summary').classList.add('active');
        document.getElementById('mobile-nav-summary').classList.add('active');
        loadSummary();
    }
}

// --- Theme Handling ---
function initTheme() {
    const themeToggleBtn = document.getElementById('theme-toggle');
    const currentTheme = localStorage.getItem('theme') || 'dark';

    document.documentElement.setAttribute('data-theme', currentTheme);
    updateThemeButton(currentTheme, themeToggleBtn);
    updateBrandLogo(currentTheme);

    themeToggleBtn.addEventListener('click', () => {
        let theme = document.documentElement.getAttribute('data-theme');
        let newTheme = theme === 'dark' ? 'light' : 'dark';

        document.documentElement.setAttribute('data-theme', newTheme);
        localStorage.setItem('theme', newTheme);
        updateThemeButton(newTheme, themeToggleBtn);
        updateBrandLogo(newTheme);
    });
}

function updateThemeButton(theme, btn) {
    if (theme === 'dark') {
        btn.textContent = '☀️';
    } else {
        btn.textContent = '🌙';
    }
}

function updateBrandLogo(theme) {
    const logo = document.getElementById('brand-logo');
    if (logo) {
        logo.src = theme === 'dark'
            ? '/assets/images/logo-dark-mode.png'
            : '/assets/images/logo-light-mode.png';
    }
}

// --- API Calls & Rendering ---

async function searchItem() {
    const searchId = document.getElementById('search-id').value;
    if (!searchId) {
        loadMenu(); // Reset to full menu if input is empty
        return;
    }

    try {
        const response = await fetchAuth('/api/menu?id=' + searchId);
        if (!response.ok) throw new Error('Failed to search item');

        const searchData = await response.json();
        renderMenu(searchData); // Render only the matching item
    } catch (error) {
        console.error('Error searching:', error);
        showToast('Failed to search item.', 'error');
    }
}

function renderMenu(itemsToRender) {
    const grid = document.getElementById('menu-grid');
    if (itemsToRender.length === 0) {
        grid.innerHTML = '<div class="text-center" style="grid-column: 1 / -1; padding: 2rem;">No items found.</div>';
        return;
    }

    grid.innerHTML = '';
    itemsToRender.forEach(item => {
        const card = document.createElement('div');
        card.className = 'card';

        const sizeOptions = item.type === 'Drink'
            ? ['SMALL', 'MEDIUM', 'LARGE']
            : ['STANDARD'];
        const sizeSelect = `<select id="size-${item.id}" class="size-select">${sizeOptions.map(size =>
            `<option value="${size}">${size === 'STANDARD' ? 'Standard' : size.charAt(0) + size.slice(1).toLowerCase()} - ₱${item.prices[size].toFixed(2)}</option>`).join('')}</select>`;

        let managerActions = '';
        if (currentUserRole === 'MANAGER') {
            managerActions = `
            <div style="margin-top: 0.5rem; display: flex; gap: 0.5rem;">
                <button class="btn btn-secondary btn-full" onclick="openManageModal(${item.id})" style="font-size: 0.8rem; padding: 0.25rem;">Edit</button>
                <button class="btn btn-primary btn-full" onclick="deleteMenuItem(${item.id})" style="font-size: 0.8rem; padding: 0.25rem; background: var(--error-color);">Delete</button>
            </div>
            `;
        }

        card.innerHTML = `
            <div>
                <h3 class="card-title">${item.name}</h3>
                <div class="card-id">ID: ${item.id}</div>
            </div>
            <div style="margin-top:1rem;">
                
                ${sizeSelect}
                <button class="btn btn-primary btn-full" onclick="addToCart(${item.id})">Add to Cart</button>
                ${managerActions}
            </div>
        `;
        grid.appendChild(card);
    });
    renderCart();
}

async function loadMenu() {
    const grid = document.getElementById('menu-grid');
    if(!authToken) return;

    // Read the dropdown value
    const sortValue = document.getElementById('sort-select') ? document.getElementById('sort-select').value : 'default';
    let url = '/api/menu';

    if (sortValue === 'price-asc') {
        url += '?sortBy=price';
    } else if (sortValue === 'price-desc') {
        url += '?sortBy=price&desc=true';
    }

    try {
        const response = await fetchAuth(url);
        if (!response.ok) throw new Error('Failed to fetch menu');

        currentMenu = await response.json();
        renderMenu(currentMenu);

    } catch (error) {
        console.error('Error loading menu:', error);
        showToast('Failed to load menu data.', 'error');
    }
}

async function loadOrders() {
    const tbody = document.getElementById('orders-table-body');
    const countDisplay = document.getElementById('pending-count');
    if(!authToken) return;

    try {
        const response = await fetchAuth('/api/orders');
        if (!response.ok) throw new Error('Failed to fetch orders');

        // CRITICAL FIX: Parse the JSON response into the 'orders' variable
        const orders = await response.json();

        // Update the pending count UI
        if (countDisplay) countDisplay.textContent = orders.length;

        // Clear the table before injecting new rows (prevents duplicates from polling)
        tbody.innerHTML = '';

        if (orders.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center">No pending orders.</td></tr>';
            return;
        }

        orders.forEach((order, index) => {
            const itemDetails = order.items.map(item => {
                const menuItem = currentMenu.find(menu => menu.id === item.itemId);
                return {
                    id: item.itemId,
                    name: menuItem ? menuItem.name : 'Unknown Item',
                    quantity: item.quantity,
                    size: item.size
                };
            });
            const itemIds = itemDetails.map(item => `${item.id} - ${item.name}`).join('<br>');
            const itemNames = itemDetails.map(item => `${item.quantity} x ${item.name} (${item.size})`).join('<br>');

            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>#${index + 1}</td>
                <td>${itemIds}</td>
                <td>${itemNames}</td>
                <td>${order.customerAlias || 'Guest'}</td>
                <td>₱${Number(order.totalPrice || calculateCartTotal(order.items)).toFixed(2)}</td>
            `;
            tbody.appendChild(tr);
        });

    } catch (error) {
        console.error('Error loading orders:', error);
        tbody.innerHTML = '<tr><td colspan="5" class="text-center" style="color: var(--error-color);">Error loading queue.</td></tr>';
    }
}

function addToCart(id) {
    if (!requireAuthentication()) return;

    const item = currentMenu.find(menuItem => menuItem.id === id);
    if (!item) return;
    const size = document.getElementById(`size-${id}`).value;
    selectedItem = item;
    openOrderModal(id, item.name, item.prices[size], size);
}

function submitOrder(event) {
    event.preventDefault(); // Prevent page reload
    if (!requireAuthentication()) return;

    const itemId = document.getElementById('order-item-id').value;
    const size = document.getElementById('order-size').value;
    const quantity = parseInt(document.getElementById('order-quantity').value, 10);
    if (!selectedItem || selectedItem.prices[size] === undefined || !Number.isInteger(quantity) || quantity < 1) {
        showToast('Choose a valid size and quantity.', 'error');
        return;
    }
    const existing = shoppingCart.find(item => item.itemId === parseInt(itemId) && item.size === size);
    if (existing) existing.quantity += quantity;
    else shoppingCart.push({ itemId: parseInt(itemId, 10), size, quantity });
    closeModal('order-modal');
    renderCart();
    showToast('Item added to cart', 'success');
}

function renderCart() {
    const cart = document.getElementById('cart-items');
    const count = document.getElementById('cart-count');
    if (!cart || !count) return;
    count.textContent = shoppingCart.reduce((total, item) => total + item.quantity, 0);
    const totalDisplay = document.getElementById('cart-total');
    if (totalDisplay) totalDisplay.textContent = `₱${getCartTotal().toFixed(2)}`;
    cart.innerHTML = shoppingCart.length ? shoppingCart.map((item, index) => {
        const menuItem = currentMenu.find(menu => menu.id === item.itemId);
        return `<div class="cart-row"><span>${item.quantity} x ${menuItem ? menuItem.name : 'Item'} (${item.size})</span><button class="btn btn-secondary" onclick="removeFromCart(${index})">Remove</button></div>`;
    }).join('') : '<p>Your cart is empty.</p>';
}

function getCartTotal() {
    return calculateCartTotal(shoppingCart);
}

function calculateCartTotal(items) {
    return items.reduce((total, item) => {
        const menuItem = currentMenu.find(menu => menu.id === item.itemId);
        const unitPrice = menuItem && menuItem.prices[item.size] ? menuItem.prices[item.size] : 0;
        return total + unitPrice * item.quantity;
    }, 0);
}

function removeFromCart(index) {
    shoppingCart.splice(index, 1);
    renderCart();
}

async function checkoutCart() {
    if (!requireAuthentication()) return;
    if (!shoppingCart.length) return showToast('Add an item to the cart first.', 'error');
    const customerAlias = document.getElementById('cart-alias').value.trim() || 'Guest';
    try {
        const response = await fetchAuth('/api/orders', { method: 'POST', body: JSON.stringify({ items: shoppingCart, customerAlias }) });
        if (!response.ok) throw new Error((await response.json()).error || 'Failed to place order');
        shoppingCart = [];
        document.getElementById('cart-alias').value = '';
        renderCart();
        showToast('Order placed successfully!', 'success');
    } catch (error) {
        showToast(error.message || 'Network error. Failed to place order.', 'error');
    }
}

async function fulfillNextOrder() {
    try {
        const response = await fetchAuth('/api/orders', {
            method: 'DELETE'
        });

        if (response.ok) {
            const data = await response.json();
            showToast(`Fulfilled order totaling ₱${Number(data.totalPrice || 0).toFixed(2)}`, 'success');
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


async function deleteMenuItem(id) {
    if(!confirm('Are you sure you want to remove item ID: ' + id + '?')) return;

    try {
        const res = await fetchAuth('/api/menu?id=' + id, { method: 'DELETE' });
        if(res.ok) {
            showToast('Item Removed', 'success');
            loadMenu();
        } else {
            const err = await res.json();
            showToast(err.error, 'error');
        }
    } catch (e) {
        showToast('Error deleting item', 'error');
    }
}

async function submitManagedItem(e) {
    e.preventDefault();
    const mode = document.getElementById('manage-mode').value;
    const id = document.getElementById('manage-id').value;
    const name = document.getElementById('manage-name').value;
    const type = document.getElementById('manage-type').value;
    const prices = type === 'Pastry'
        ? { price: parseFloat(document.getElementById('manage-standard-price').value) }
        : {
            smallPrice: parseFloat(document.getElementById('manage-small-price').value),
            mediumPrice: parseFloat(document.getElementById('manage-medium-price').value),
            largePrice: parseFloat(document.getElementById('manage-large-price').value)
        };

    const method = mode === 'add' ? 'POST' : 'PUT';

    try {
        const res = await fetchAuth('/api/menu', {
            method: method,
            body: JSON.stringify({id: parseInt(id), name: name, type, ...prices})
        });
        if(res.ok) {
            showToast(mode === 'add' ? 'Item Added' : 'Item Updated', 'success');
            closeModal('manage-modal');
            loadMenu();
        } else {
            const err = await res.json();
            showToast(err.error, 'error');
        }
    } catch(err) {
        showToast('Network error saving item', 'error');
    }
}

// --- Modal Handling ---

function openOrderModal(id, name, price, size = 'STANDARD') {
    document.getElementById('order-item-id').value = id;
    document.getElementById('modal-item-name').textContent = name;
    document.getElementById('modal-item-price').textContent = `₱${price.toFixed(2)}`;
    document.getElementById('order-size').innerHTML = size === 'STANDARD'
        ? '<option value="STANDARD">Standard</option>'
        : ['SMALL', 'MEDIUM', 'LARGE'].map(option => `<option value="${option}" ${option === size ? 'selected' : ''}>${option.charAt(0) + option.slice(1).toLowerCase()}</option>`).join('');
    document.getElementById('order-quantity').value = 1;

    const sizeSelect = document.getElementById('order-size');
    const quantityInput = document.getElementById('order-quantity');
    sizeSelect.onchange = updateOrderModalPrice;
    quantityInput.oninput = updateOrderModalPrice;
    updateOrderModalPrice();

    document.getElementById('order-modal').classList.remove('hidden');
}

function updateOrderModalPrice() {
    if (!selectedItem) return;
    const size = document.getElementById('order-size').value;
    const quantity = Math.max(0, parseInt(document.getElementById('order-quantity').value, 10) || 0);
    const unitPrice = selectedItem.prices[size];
    if (unitPrice === undefined) return;
    document.getElementById('modal-item-price').textContent =
        `Unit price: ₱${unitPrice.toFixed(2)} | Total: ₱${(unitPrice * quantity).toFixed(2)}`;
}

function openManageModal(id = '') {
    const isEdit = id !== '';
    document.getElementById('manage-title').textContent = isEdit ? 'Edit Menu Item' : 'Add New Item';
    document.getElementById('manage-mode').value = isEdit ? 'edit' : 'add';

    const idField = document.getElementById('manage-id');
    idField.value = id;
    idField.readOnly = isEdit; // Can't change ID on edit in this data structure

    const item = currentMenu.find(menuItem => menuItem.id === id);
    document.getElementById('manage-name').value = item ? item.name : '';
    document.getElementById('manage-type').value = item ? item.type : 'Drink';
    document.getElementById('manage-small-price').value = item?.prices.SMALL || '';
    document.getElementById('manage-medium-price').value = item?.prices.MEDIUM || '';
    document.getElementById('manage-large-price').value = item?.prices.LARGE || '';
    document.getElementById('manage-standard-price').value = item?.prices.STANDARD || '';

    document.getElementById('manage-modal').classList.remove('hidden');
}

function closeModal(modalId) {
    document.getElementById(modalId).classList.add('hidden');
}

// Close modal if clicking outside the content box
window.onclick = function(event) {
    const orderModal = document.getElementById('order-modal');
    const manageModal = document.getElementById('manage-modal');
    if (event.target === orderModal) closeModal('order-modal');
    if (event.target === manageModal) closeModal('manage-modal');
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

async function loadSummary() {
    if (!authToken) return;

    try {
        const response = await fetchAuth('/api/summary');
        if (!response.ok) throw new Error('Failed to fetch summary');

        const summary = await response.json();
        const metrics = summary.metrics;
        document.getElementById('summary-menu-items').textContent = metrics.menuItems;
        document.getElementById('summary-pending-orders').textContent = metrics.pendingOrders;
        document.getElementById('summary-completed-sales').textContent = metrics.completedSales;
        document.getElementById('summary-items-sold').textContent = metrics.itemsSold;
        document.getElementById('summary-total-revenue').textContent = `₱${Number(metrics.totalRevenue).toFixed(2)}`;

        const tbody = document.getElementById('sales-history-body');
        if (!summary.sales.length) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center">No completed sales yet.</td></tr>';
            return;
        }

        tbody.innerHTML = summary.sales.map(sale => {
            const items = sale.items.map(item => {
                const menuItem = currentMenu.find(menu => menu.id === item.itemId);
                const itemName = escapeHtml(menuItem ? menuItem.name : 'Unknown Item');
                return `${item.itemId} - ${itemName} (${item.quantity} x ${item.size})`;
            }).join('<br>');
            return `<tr>
                <td>${new Date(sale.completedAt).toLocaleString()}</td>
                <td>${escapeHtml(sale.completedBy)}</td>
                <td>${escapeHtml(sale.customerAlias || 'Guest')}</td>
                <td>${items}</td>
                <td>₱${Number(sale.totalPrice).toFixed(2)}</td>
            </tr>`;
        }).join('');
    } catch (error) {
        console.error('Error loading summary:', error);
        document.getElementById('sales-history-body').innerHTML =
            '<tr><td colspan="5" class="text-center" style="color: var(--error-color);">Error loading summary.</td></tr>';
    }
}

function escapeHtml(value) {
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}