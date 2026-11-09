function symbol(c) {
        if (c === 'USD') return '$';
        if (c === 'GBP') return '£';
        return '€';
    }

async function loadSummary() {
    const res = await fetch('/api/dashboard/summary',);
    if (!res.ok) return;
    const s = await res.json();
    document.getElementById('monthly-total').textContent = symbol(s.currency) + Number(s.monthlyTotal).toFixed(2);
    document.getElementById('yearly-total').textContent = symbol(s.currency) + Number(s.yearlyTotal).toFixed(2);
}

async function loadActiveCount() {
    const res = await fetch('/api/subscriptions');
    if (!res.ok) return;
    const subs = await res.json();
    document.getElementById('active-count').textContent = subs.length;
}

async function loadUpcoming() {
    const box = document.getElementById('upcoming-payments');
    const res = await fetch('/api/dashboard/upcoming-payments');
    if (!res.ok) { box.innerHTML = '<p class="muted">Could not load payments</p>'; return; }
    const payments = await res.json();

    if (payments.length === 0) {
        box.innerHTML = '<p class="muted">No upcoming payments yet</p>';
        return;
    }

    box.innerHTML = payments.map(p => `
        <div class="upcoming-row">
            <span class="upcoming-name">${p.providerName}</span>
            <span class="upcoming-date">${p.nextPaymentDate} · in ${p.daysLeft} days</span>
            <span class="upcoming-amount">${symbol(p.currency)}${Number(p.amount).toFixed(2)}</span>
        </div>`).join('');
}

loadSummary();
loadActiveCount();
loadUpcoming();