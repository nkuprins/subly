
// customizing dialog window that is used for adding new categories
var dialog = document.getElementById('category-dialog');
var form = dialog.querySelector('form');
var categoryNameInput = document.getElementById('category-title');
var dialogTitle = document.getElementById('category-dialog-title');
var dialogText = dialog.querySelector('.provider-dialog-card > p');
var submitButton = form.querySelector('button[type="submit"]');
var editingCategoryId = null;

function clearCategoryError() {
    var errorMessage = document.getElementById('category-title-error');
    if (errorMessage) {
        errorMessage.parentNode.removeChild(errorMessage);
    }
    categoryNameInput.className = '';
}

document.getElementById('add-category-button').addEventListener('click', function () {
    // fill the window with the information for adding.
    editingCategoryId = null;
    clearCategoryError();
    categoryNameInput.value = '';
    dialogTitle.textContent = 'Add a category';
    dialogText.textContent = 'Enter a name for the new category.';
    submitButton.textContent = 'Create category';
    dialog.showModal();
});

document.getElementById('category-dialog-close').addEventListener('click', function () {
    dialog.close();
});

document.getElementById('category-dialog-cancel').addEventListener('click', function () {
    dialog.close();
});

dialog.addEventListener('click', function (event) {
    if (event.target === dialog) {
        dialog.close();
    }
});

if (dialog.dataset.open === 'true') {
    dialog.showModal();
}

form.addEventListener('submit', async function (event) {
    if (editingCategoryId === null) {
        return;
    }

    event.preventDefault();

    // sending request to Category REST API
    try {
        var response = await fetch('/api/category/edit', {
            method: 'PUT',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                id: Number(editingCategoryId),
                newName: categoryNameInput.value
            })
        });

        if (!response.ok) {
            var error = await response.json();
            alert(error.detail || error.message || 'Could not edit the category.');
            return;
        }

        window.location.href = '/admin';
    } catch (error) {
        alert('Could not edit the category.');
    }
});

var categoryButtons = document.querySelectorAll('.category-action-button');
for (var i = 0; i < categoryButtons.length; i++) {
    categoryButtons[i].addEventListener('click', async function () {
        var categoryId = this.dataset.categoryId;

        if (this.dataset.action === 'edit') {
            editingCategoryId = categoryId;
            clearCategoryError();
            categoryNameInput.value = this.closest('tr').querySelector('td:first-child').textContent.trim();
            dialogTitle.textContent = 'Edit category';
            dialogText.textContent = 'Update the category name.';
            submitButton.textContent = 'Save changes';
            dialog.showModal();
            return;
        }

        // sending request to Category REST API for deletion
        if (this.dataset.action === 'delete') {
            try {
                var response = await fetch('/api/category/delete/' + categoryId, {
                    method: 'DELETE'
                });

                if (!response.ok) {
                    var error = await response.json();
                    alert(error.detail || error.message || 'Could not delete the category.');
                    return;
                }

                // to avoid confusion with stored data,
                // the user is redirected to the page again.
                window.location.href = '/admin';
            } catch (error) {
                alert('Could not delete the category.');
            }
        }
    });
}
