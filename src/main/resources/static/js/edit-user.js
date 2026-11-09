
// password form
const passwordForm = document.getElementById('password-form');
const passwordSubmit = document.getElementById('password-submit');
const oldPassword = document.getElementById('old-password');
const newPassword = document.getElementById('new-password');

// csrf token that is included in the request headers by default/
const csrfToken = document.querySelector('meta[name="_csrf"]').content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
const requestHeaders = {'Content-Type': 'application/json', [csrfHeader]: csrfToken};

// dialog window elements
const resultDialog = document.getElementById('edit-user-dialog');
const resultDialogTitle = document.getElementById('edit-user-dialog-title');
const resultDialogMessage = document.getElementById('edit-user-dialog-message');
const resultDialogOk = document.getElementById('edit-user-dialog-ok');

let redirectToLoginOnOk = false; // redirect to /login page?

function showResult(title, message, redirectToLogin = false) {
    resultDialogTitle.textContent = title;
    resultDialogMessage.textContent = message;
    redirectToLoginOnOk = redirectToLogin;
    resultDialog.showModal();
}

document.getElementById('edit-user-dialog-close').addEventListener('click', () => resultDialog.close());
resultDialogOk.addEventListener('click', () => {
    if (redirectToLoginOnOk) {
        window.location.href = '/login';
        return;
    }
    resultDialog.close();
});

// mapping of a response field error to a user-friendly label
const fieldLabels = {
    oldPassword: 'Current password',
    newPassword: 'New password',
    newUsername: 'Username',
    newEmail: 'Email',
    newName: 'Name',
    newTimeZone: 'Time zone',
    anyFieldProvided: 'Information'
};

function firstErrorMessage(result, fallback) {
    // shows only the first error message not to overload the user :)
    const error = Array.isArray(result.errors) ? result.errors[0] : null;
    if (error) {
        // if there is no mapping of the error, than return the error directly
        const label = fieldLabels[error.field] || error.field;
        return `${label}: ${error.message}`;
    }
    return result.detail || result.message || result.error || fallback;
}

function updatePasswordButton() {
    passwordSubmit.disabled = !oldPassword.value.trim() || !newPassword.value.trim();
}

oldPassword.addEventListener('input', updatePasswordButton);
newPassword.addEventListener('input', updatePasswordButton);

passwordForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    if (passwordSubmit.disabled) return;

    passwordSubmit.disabled = true;

    try {
        const response = await fetch('/users/update/password/me', {
            method: 'PUT',
            headers: requestHeaders,
            body: JSON.stringify({oldPassword: oldPassword.value, newPassword: newPassword.value})
        });
        const result = response.status === 204 ? {} : await response.json().catch(() => ({}));
        if (!response.ok) {
            showResult('Could not change password', firstErrorMessage(result, 'Please try again.'));
            return;
        }
        showResult('Password changed', 'Please sign in again with your new password.', true);
        passwordForm.reset();
    } catch (error) {
        showResult('Could not change password', 'Could not reach the server. Please try again.');
    } finally {
        updatePasswordButton();
    }
});

// information form (username, email, name and timezone combo box)
const informationForm = document.getElementById('information-form');
const informationSubmit = document.getElementById('information-submit');
const informationFields = [...informationForm.querySelectorAll('[name]')];

// save all previous information to compare each change and disable/enable 'Change' button
const originalInformation = new Map(informationFields.map(field => [field.name, field.value]));

function changedInformation() {
    return informationFields.filter(field => field.value !== originalInformation.get(field.name));
}

function updateInformationButton() {
    informationSubmit.disabled = changedInformation().length === 0;
}

informationFields.forEach(field => field.addEventListener('input', updateInformationButton));
informationFields.forEach(field => field.addEventListener('change', updateInformationButton));

informationForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    const changedFields = changedInformation();
    if (!changedFields.length) return;

    const payload = Object.fromEntries(changedFields.map(field => [field.name, field.value]));
    informationSubmit.disabled = true
    ;
    try {
        const response = await fetch('/users/update/info/me', {
            method: 'PATCH',
            headers: requestHeaders,
            body: JSON.stringify(payload)
        });

        const result = response.status === 204 ? {} : await response.json().catch(() => ({}));

        if (!response.ok) {
            showResult('Could not save information', firstErrorMessage(result, 'Please try again.'));
            return;
        }
        
        showResult('Information saved', 'Please sign in again to apply your changes.', true);
        changedFields.forEach(field => originalInformation.set(field.name, field.value));
    } catch (error) {
        showResult('Could not save information', 'Could not reach the server. Please try again.');
    } finally {
        updateInformationButton();
    }
});
