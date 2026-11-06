const parts = window.location.pathname.split('/').filter(Boolean);
const isEdit = parts[parts.length - 1] === 'edit';
const subId = isEdit ? Number(parts[1]) : null;

const form = document.querySelector('.form');
const byId = (id) => document.getElementById(id);

async function loadCategories() {
    const res = await fetch('/api/category/listAll');
    if (!res.ok) return;
    const categories = await res.json();
    const select = byId("categoryId");
    categories.forEach(c => {
        const opt = document.createElement('option');
        opt.value = c.id;
        opt.textContent = c.name;
        select.appendChild(opt);
    });
}

async function prefill() {
    const res = await fetch('/api/subscriptions');
    if (!res.ok) return;
    const subs = await res.json();
    const sub = subs.map(s => s.subscription).find(s => s.id === subId);
    if (!sub) {
        alert('Subscription not found');
        return;
    }
    byId('name').value = sub.serviceName;
    byId('price').value = sub.price;
    byId('categoryId').value = sub.categoryId;
    byId('cycle').value = sub.billingCycle;
    byId('nextPayment').value = sub.nextPaymentDate;
}

form.addEventListener('submit', async (e) => {
    e.preventDefault();

    let res
    if (isEdit) {
        const payload = {
            price: byId('price').value,
            billingCycle: byId('cycle').value,
            nextPaymentDate: byId('nextPayment').value,
            freeTrial: byId('freeTrial').checked,
        };
        res = await fetch(`/api/subscriptions/${subId}`, {
            method: 'PUT',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload),
        });
    } else {
        const payload = {
            name: byId('name').value,
            categoryId: Number(byId('categoryId').value),
            price: byId('price').value,
            billingCycle: byId('cycle').value,
            nextPaymentDate: byId('nextPayment').value,
            freeTrial: byId('freeTrial').checked,
        };
        res = await fetch('/api/subscriptions/custom', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload),
        });
    }
    if (res.ok) {
        window.location.href = '/subscriptions';
    } else {
        alert('Save failed (status ' + res.status + ')');
    }
});

async function init() {
    await loadCategories();
    if (isEdit) await prefill();
}

init();
