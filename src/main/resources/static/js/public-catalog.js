(function () {
    const STORAGE_KEY = 'ecoagua_public_inquiry_cart_v1';
    const CUSTOMER_KEY = 'ecoagua_public_inquiry_customer_v1';

    const config = window.PUBLIC_CATALOG_CONFIG || {};
    const whatsappNumber = config.whatsappNumber || '51999999999';
    const intro = config.intro || 'Hola, deseo consultar disponibilidad desde el catálogo';
    const productLabel = config.productLabel || 'Producto';
    const businessName = config.businessName || 'Eco del Amazonas';

    function readCart() {
        try {
            const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
            return Array.isArray(parsed) ? parsed : [];
        } catch (error) {
            return [];
        }
    }

    function writeCart(items) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
    }

    function readCustomer() {
        try {
            return JSON.parse(localStorage.getItem(CUSTOMER_KEY) || '{}') || {};
        } catch (error) {
            return {};
        }
    }

    function writeCustomer() {
        const nameInput = document.querySelector('[data-inquiry-customer-name]');
        const zoneInput = document.querySelector('[data-inquiry-delivery-zone]');
        const notesInput = document.querySelector('[data-inquiry-notes]');
        const data = {
            name: nameInput ? nameInput.value.trim() : '',
            zone: zoneInput ? zoneInput.value.trim() : '',
            notes: notesInput ? notesInput.value.trim() : ''
        };
        localStorage.setItem(CUSTOMER_KEY, JSON.stringify(data));
        return data;
    }

    function normalizeProduct(button) {
        const card = button.closest('.product-card');
        return {
            id: button.dataset.id || (card ? card.dataset.id : ''),
            name: button.dataset.name || (card ? card.dataset.name : ''),
            price: button.dataset.price || (card ? card.dataset.price : ''),
            image: button.dataset.image || (card ? card.dataset.image : ''),
            category: button.dataset.category || (card ? card.dataset.category : '') || 'Sin categoría',
            quantity: 1
        };
    }

    function escapeHtml(value) {
        return String(value || '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    function totalCount(items) {
        return items.reduce((total, item) => total + Math.max(1, Number(item.quantity || 1)), 0);
    }

    function showToast(message) {
        let toast = document.querySelector('[data-inquiry-toast]');
        if (!toast) {
            toast = document.createElement('div');
            toast.className = 'inquiry-toast';
            toast.setAttribute('data-inquiry-toast', 'true');
            document.body.appendChild(toast);
        }
        toast.textContent = message;
        toast.classList.add('is-visible');
        window.clearTimeout(showToast.timer);
        showToast.timer = window.setTimeout(() => toast.classList.remove('is-visible'), 1800);
    }

    function updateCounters(items) {
        const count = totalCount(items);
        document.querySelectorAll('[data-inquiry-count]').forEach(counter => {
            counter.textContent = String(count);
        });
    }

    function renderCart() {
        const items = readCart();
        const list = document.querySelector('[data-inquiry-items]');
        const empty = document.querySelector('[data-inquiry-empty]');
        updateCounters(items);

        if (!list) {
            return;
        }

        if (!items.length) {
            list.innerHTML = '';
            if (empty) {
                empty.classList.remove('d-none');
            }
            return;
        }

        if (empty) {
            empty.classList.add('d-none');
        }

        list.innerHTML = items.map(item => {
            const price = item.price ? 'S/ ' + escapeHtml(item.price) : 'Consultar precio';
            const category = item.category ? escapeHtml(item.category) + ' · ' : '';
            const image = item.image || '/img/product-default.svg';
            return `
                <article class="inquiry-cart-item" data-inquiry-item="${escapeHtml(item.id)}">
                    <div class="inquiry-cart-item-image">
                        <img src="${escapeHtml(image)}" alt="${escapeHtml(item.name)}">
                    </div>
                    <div>
                        <h3 class="inquiry-cart-item-title">${escapeHtml(item.name)}</h3>
                        <p class="inquiry-cart-item-meta">${category}${price}</p>
                        <div class="inquiry-cart-item-controls">
                            <input type="number" min="1" step="1" class="form-control form-control-sm inquiry-cart-qty js-inquiry-qty" value="${Math.max(1, Number(item.quantity || 1))}" aria-label="Cantidad">
                            <button type="button" class="inquiry-cart-remove js-inquiry-remove">Quitar</button>
                        </div>
                    </div>
                </article>
            `;
        }).join('');
    }

    function addProduct(button) {
        const product = normalizeProduct(button);
        if (!product.id || !product.name) {
            return;
        }
        const items = readCart();
        const existing = items.find(item => String(item.id) === String(product.id));
        if (existing) {
            existing.quantity = Math.max(1, Number(existing.quantity || 1)) + 1;
        } else {
            items.push(product);
        }
        writeCart(items);
        renderCart();
        showToast(product.name + ' agregado a la consulta');
    }

    function openCart() {
        renderCart();
        document.body.classList.add('inquiry-cart-open');
        const panel = document.querySelector('.inquiry-cart-panel');
        if (panel) {
            panel.setAttribute('aria-hidden', 'false');
        }
    }

    function closeCart() {
        document.body.classList.remove('inquiry-cart-open');
        const panel = document.querySelector('.inquiry-cart-panel');
        if (panel) {
            panel.setAttribute('aria-hidden', 'true');
        }
    }

    function updateQuantity(input) {
        const row = input.closest('[data-inquiry-item]');
        if (!row) {
            return;
        }
        const id = row.getAttribute('data-inquiry-item');
        const quantity = Math.max(1, Number(input.value || 1));
        const items = readCart().map(item => String(item.id) === String(id) ? { ...item, quantity } : item);
        writeCart(items);
        renderCart();
    }

    function removeItem(button) {
        const row = button.closest('[data-inquiry-item]');
        if (!row) {
            return;
        }
        const id = row.getAttribute('data-inquiry-item');
        const items = readCart().filter(item => String(item.id) !== String(id));
        writeCart(items);
        renderCart();
    }

    function clearCart() {
        if (!readCart().length) {
            return;
        }
        if (!window.confirm('¿Vaciar la consulta actual?')) {
            return;
        }
        writeCart([]);
        renderCart();
    }

    function sendInquiry() {
        const items = readCart();
        if (!items.length) {
            alert('Agrega al menos un producto a la consulta.');
            return;
        }
        const customer = writeCustomer();
        const lines = [
            intro,
            '',
            'Quiero consultar disponibilidad de:',
            ...items.map(item => {
                const quantity = Math.max(1, Number(item.quantity || 1));
                const priceText = item.price ? ' - Precio ref.: S/ ' + item.price : '';
                return '- ' + quantity + ' x ' + item.name + priceText;
            }),
            ''
        ];

        if (customer.name) {
            lines.push('Nombre: ' + customer.name);
        }
        if (customer.zone) {
            lines.push('Zona de entrega: ' + customer.zone);
        }
        if (customer.notes) {
            lines.push('Notas: ' + customer.notes);
        }

        lines.push('');
        lines.push('Por favor confirmar stock, presentación, precio final y forma de entrega. Gracias.');
        lines.push('Consulta enviada desde ' + businessName + '.');

        const url = 'https://wa.me/' + whatsappNumber + '?text=' + encodeURIComponent(lines.join('\n'));
        window.open(url, '_blank');
    }

    function restoreCustomer() {
        const data = readCustomer();
        const nameInput = document.querySelector('[data-inquiry-customer-name]');
        const zoneInput = document.querySelector('[data-inquiry-delivery-zone]');
        const notesInput = document.querySelector('[data-inquiry-notes]');
        if (nameInput && data.name) {
            nameInput.value = data.name;
        }
        if (zoneInput && data.zone) {
            zoneInput.value = data.zone;
        }
        if (notesInput && data.notes) {
            notesInput.value = data.notes;
        }
    }

    document.addEventListener('click', function (event) {
        const addButton = event.target.closest('.js-inquiry-add');
        if (addButton) {
            event.preventDefault();
            addProduct(addButton);
            return;
        }

        if (event.target.closest('.js-inquiry-open')) {
            event.preventDefault();
            openCart();
            return;
        }

        if (event.target.closest('.js-inquiry-close')) {
            event.preventDefault();
            closeCart();
            return;
        }

        if (event.target.closest('.js-inquiry-clear')) {
            event.preventDefault();
            clearCart();
            return;
        }

        if (event.target.closest('.js-inquiry-send')) {
            event.preventDefault();
            sendInquiry();
            return;
        }

        const removeButton = event.target.closest('.js-inquiry-remove');
        if (removeButton) {
            event.preventDefault();
            removeItem(removeButton);
        }
    });

    document.addEventListener('change', function (event) {
        if (event.target.matches('.js-inquiry-qty')) {
            updateQuantity(event.target);
        }
    });

    document.addEventListener('input', function (event) {
        if (event.target.matches('[data-inquiry-customer-name], [data-inquiry-delivery-zone], [data-inquiry-notes]')) {
            writeCustomer();
        }
    });

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape') {
            closeCart();
        }
    });

    document.addEventListener('DOMContentLoaded', function () {
        restoreCustomer();
        renderCart();
    });
})();
