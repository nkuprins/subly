const providerCombo = document.getElementById('providerCombo');
const providerOptions = document.getElementById('providerOptions');
const providerCurrent = document.getElementById('providerCurrent');
const providerValue = document.getElementById('providerId');

if (providerCombo && providerOptions && providerCurrent && providerValue) {
    const options = [...providerOptions.querySelectorAll('.provider-option')];

    function closeOptions() {
        providerOptions.hidden = true;
        providerCombo.setAttribute('aria-expanded', 'false');
    }

    function makeInitial(name) {
        const initial = document.createElement('span');
        initial.className = 'provider-option-initial';
        initial.setAttribute('aria-hidden', 'true');
        initial.textContent = (name || '?').trim().charAt(0).toLocaleUpperCase();
        return initial;
    }

    function setSelection(option) {
        const name = option.dataset.providerName;
        providerValue.value = option.dataset.providerId;
        const logo = option.querySelector('.provider-option-logo');
        const avatar = logo ? logo.cloneNode() : makeInitial(name);
        if (logo) avatar.onerror = () => avatar.replaceWith(makeInitial(name));
        providerCurrent.replaceChildren(avatar, Object.assign(document.createElement('span'), {textContent: name}));
        options.forEach(item => item.setAttribute('aria-selected', String(item === option)));
        closeOptions();
    }

    providerCombo.addEventListener('click', () => {
        providerOptions.hidden = !providerOptions.hidden;
        providerCombo.setAttribute('aria-expanded', String(!providerOptions.hidden));
    });

    providerCombo.addEventListener('keydown', event => {
        if (event.key === 'ArrowDown' || event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            providerOptions.hidden = false;
            providerCombo.setAttribute('aria-expanded', 'true');
            options[0]?.focus();
        } else if (event.key === 'Escape') {
            closeOptions();
        }
    });

    providerOptions.addEventListener('click', event => {
        const option = event.target.closest('.provider-option');
        if (option) setSelection(option);
    });

    // logic to go through combo box using arrows
    providerOptions.addEventListener('keydown', event => {
        const index = options.indexOf(document.activeElement);
        if (event.key === 'Escape') {
            closeOptions();
            providerCombo.focus();
        } else if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
            event.preventDefault();
            const step = event.key === 'ArrowDown' ? 1 : -1;
            options[(index + step + options.length) % options.length]?.focus();
        }
    });

    document.addEventListener('click', event => {
        if (!event.target.closest('.provider-combobox')) closeOptions();
    });

    providerCombo.closest('form')?.addEventListener('submit', event => {
        if (!providerValue.value) {
            event.preventDefault();
            providerOptions.hidden = false;
            providerCombo.setAttribute('aria-expanded', 'true');
            providerCombo.focus();
        }
    });
}
