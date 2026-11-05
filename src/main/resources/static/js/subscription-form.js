
const parts = window.location.pathname.split('/').filter(Boolean);
const isEdit = parts[parts.length - 1] === 'edit';
const subId = isEdit ? Number(parts[1]) : null;

const form = document.querySelector('.form');
const byId = (id) => document.getElementById(id);

async function prefill() {
    const res = await fetch('/api/subscriptions');
    if (!res.ok) return;
    const subs = await res.json();
    const sub = subs.find(s => s.id === subId);
    if (!sub) { alert('Subscription not found'); return; }
    byId('name').value = sub.serviceName;
    byId('price').value = sub.price;
    byId('cycle').value = sub.billingCycle.toLowerCase();
    byId('nextPayment').value = sub.nextPaymentDate;
}


form.addEventListener('submit', async (e) => {
    e.preventDefault();   // не даём форме делать обычный POST

    if (!isEdit) {
        alert('Add is not connected yet — backend needs GET /api/categories first.');
        return;
    }

    const payload = {
        price: byId('price').value,
        billingCycle: byId('cycle').value.toUpperCase(),  // MONTHLY / YEARLY
        nextPaymentDate: byId('nextPayment').value,
        freeTrial: false
    };

    const res = await fetch(`/api/subscriptions/${subId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });

    if (res.ok) {
        window.location.href = '/subscriptions';   // назад к списку
    } else {
        alert('Save failed (status ' + res.status + ')');
    }
});

if (isEdit) prefill();