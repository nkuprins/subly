const parts = window.location.pathname.split('/').filter(Boolean);
const isEdit = parts[parts.length - 1] === 'edit';
const subId = isEdit ? Number(parts[1]) : null;

const form = document.querySelector('.form');
const byId = (id) => document.getElementById(id);
const providerSelect = byId('providerId');
const providerOptions = byId('providerOptions');
const providerCombo = byId('providerCombo');
const providerCurrent = byId('providerCurrent');
const providerDialog = byId('providerDialog');


function providerLogo(provider, className) {
    if (!provider.logoPath) return createProviderInitial(provider.name);
    const image = document.createElement('img');
    image.className = className;
    image.src = provider.logoPath;
    image.alt = '';
    image.onerror = () => image.replaceWith(createProviderInitial(provider.name));
    return image;
}

function closeProviderOptions() {
    providerOptions.hidden = true;
    providerCombo.setAttribute('aria-expanded', 'false');
}

function setProviderSelection(provider) {
    providerSelect.value = String(provider.id);
    providerCurrent.replaceChildren(providerLogo(provider, 'provider-option-logo'));
    const name = document.createElement('span');
    name.textContent = provider.name;
    providerCurrent.appendChild(name);
    syncProviderSelection();
    closeProviderOptions();
}

providerCombo.addEventListener('click', () => {
    providerOptions.hidden = !providerOptions.hidden;
    providerCombo.setAttribute('aria-expanded', String(!providerOptions.hidden));
});
document.addEventListener('click', (event) => {
    if (!event.target.closest('.provider-combobox')) closeProviderOptions();
});
providerCombo.addEventListener('keydown', (event) => {
    if (event.key === 'ArrowDown' || event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        providerOptions.hidden = false;
        providerCombo.setAttribute('aria-expanded', 'true');
        providerOptions.querySelector('.provider-option')?.focus();
    }
    if (event.key === 'Escape') closeProviderOptions();
});
providerOptions.addEventListener('keydown', (event) => {
    const options = [...providerOptions.querySelectorAll('.provider-option')];
    const index = options.indexOf(document.activeElement);
    if (event.key === 'Escape') {
        closeProviderOptions();
        providerCombo.focus();
    } else if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
        event.preventDefault();
        options[(index + (event.key === 'ArrowDown' ? 1 : options.length - 1)) % options.length]?.focus();
    }
});

function addProvider(provider) {
    const option = document.createElement('option');
    option.value = provider.id;
    option.textContent = provider.name;
    providerSelect.appendChild(option);

    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'provider-option';
    button.setAttribute('role', 'option');
    button.dataset.providerId = provider.id;
    button.setAttribute('aria-selected', 'false');
    button.appendChild(providerLogo(provider, 'provider-option-logo'));
    const name = document.createElement('span');
    name.textContent = provider.name;
    button.appendChild(name);
    button.addEventListener('click', () => {
        setProviderSelection(provider);
    });
    providerOptions.appendChild(button);
}

function createProviderInitial(name) {
    const initial = document.createElement('span');
    initial.className = 'provider-option-initial';
    initial.setAttribute('aria-hidden', 'true');
    initial.textContent = (name || '?').trim().charAt(0).toLocaleUpperCase();
    return initial;
}

function syncProviderSelection() {
    for (const button of providerOptions.children) {
        const selected = button.dataset.providerId === providerSelect.value;
        button.setAttribute('aria-selected', String(selected));
    }
}
providerSelect.addEventListener('change', syncProviderSelection);

byId('addProvider').addEventListener('click', () => {
    byId('customProviderName').value = '';
    byId('customProviderCategory').value = '';
    byId('customProviderLogo').value = '';
    providerDialog.showModal();
    byId('customProviderName').focus();
});
byId('closeProviderDialog').addEventListener('click', () => providerDialog.close());
byId('cancelProvider').addEventListener('click', () => providerDialog.close());

byId('providerDialogForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const name = byId('customProviderName').value.trim();
    const categoryId = byId('customProviderCategory').value;
    if (!name || !categoryId) {
        if (!name) byId('customProviderName').reportValidity();
        if (!categoryId) byId('customProviderCategory').reportValidity();
        return;
    }

    const saveButton = byId('saveProvider');
    saveButton.disabled = true;
    try {
        const res = await fetch('/api/providers', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                name,
                categoryId: Number(categoryId),
                logoPath: byId('customProviderLogo').value.trim() || null,
            }),
        });
        if (!res.ok) {
            alert('Could not add provider (status ' + res.status + ')');
            return;
        }
        const provider = await res.json();
        addProvider(provider);
        setProviderSelection(provider);
        providerDialog.close();
    } catch (error) {
        alert('Could not add provider. Please try again.');
    } finally {
        saveButton.disabled = false;
    }
});


// delete
async function loadCategories() {
    const res = await fetch('/api/category/listAll');
    if (!res.ok) return;
    const categories = await res.json();
    const select = byId('customProviderCategory');
    categories.forEach(category => {
        const option = document.createElement('option');
        option.value = category.id;
        option.textContent = category.name;
        select.appendChild(option);
    });
}

// delete
async function loadProviders() {

    // CHANGE API HERE
    const res = await fetch('/api/providers');
    if (!res.ok) return;
    const providers = await res.json();
    providers.forEach(provider => {
        addProvider(provider);
    });
}

// delete
async function prefill() {
    const res = await fetch('/api/subscriptions');
    if (!res.ok) return;
    const subs = await res.json();
    const sub = subs.map(item => item.subscription).find(item => item.id === subId);
    if (!sub) {
        alert('Subscription not found');
        return;
    }
    providerSelect.value = String(sub.providerId);
    const selectedOption = [...providerOptions.children].find(option => option.dataset.providerId === providerSelect.value);
    if (selectedOption) selectedOption.click();
    byId('price').value = sub.price;
    byId('cycle').value = sub.billingCycle;
    byId('nextPayment').value = sub.nextPaymentDate;
    byId('freeTrial').checked = sub.freeTrial;
}

// delete
form.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!providerSelect.value) {
        providerCombo.focus();
        providerOptions.hidden = false;
        providerCombo.setAttribute('aria-expanded', 'true');
        return;
    }
    let res;
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
            providerId: Number(providerSelect.value),
            price: byId('price').value,
            billingCycle: byId('cycle').value,
            nextPaymentDate: byId('nextPayment').value,
            freeTrial: byId('freeTrial').checked,
        };
        res = await fetch('/api/subscriptions', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload),
        });
    }
    if (res.ok) window.location.href = '/subscriptions';
    else alert('Save failed (status ' + res.status + ')');
});

// delete
async function init() {
    await Promise.all([loadCategories(), loadProviders()]);
    if (isEdit) await prefill();
}

init();
