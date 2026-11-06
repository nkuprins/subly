
async function loadSubscriptions() {
    const res = await fetch('/api/subscriptions');
    if (!res.ok) {
        console.error('Load failed:', res.status);
        return;
    }
    const subs = await res.json();
    const tbody = document.getElementById('subs-body');
    console.log(subs);
    if (subs.length === 0) {
        tbody.innerHTML =
            `<tr><td colspan="6" class="empty-row">No subscriptions yet.
             <a href="/subscriptions/new" class="link">Add your first one</a>.</td></tr>`;
        return;
    }

    tbody.innerHTML = subs.map(s => `
        <tr>
            <td>${s.subscription.serviceName}</td>
            <td>${s.subscription.price}</td>
            <td>${s.subscription.currency}</td>
            <td><span class="pill ${s.subscription.billingCycle === 'YEARLY' ? 'pill-year' : ''}">${s.subscription.billingCycle}</span></td>
            <td>${s.subscription.nextPaymentDate}</td>
            <td>
                <div class="actions">
                    <a href="/subscriptions/${s.subscription.id}/edit" class="link">Edit</a>
                    <button type="button" class="link link-danger btn-link"
                            onclick="deleteSubscription(${s.subscription.id})">Delete</button>
                </div>
            </td>
        </tr>`).join('');
}


async function deleteSubscription(id) {
    if (!confirm('Delete this subscription?')) return;
    const res = await fetch(`/api/subscriptions/${id}`, { method: 'DELETE' });
    if (res.ok) {
        loadSubscriptions();
    } else {
        alert('Could not delete (status ' + res.status + ')');
    }
}

loadSubscriptions();