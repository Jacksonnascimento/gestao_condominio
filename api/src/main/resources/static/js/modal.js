const modal = document.getElementById('formModal');
const modalContent = document.getElementById('modalContent');

async function openForm(url, formId, submitCallback) {
    try {
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error('Falha ao carregar o formulário.');
        }
        modalContent.innerHTML = await response.text();

        const form = document.getElementById(formId);
        if (form) {
            form.addEventListener('submit', function(event) {
                handleFormSubmit(event, submitCallback);
            });
        }

        modal.style.display = 'flex';
        
        // Dispara um evento para notificar que o conteúdo do modal foi carregado
        document.dispatchEvent(new Event('modalContentLoaded'));

    } catch (error) {
        console.error('Erro ao carregar o formulário:', error);
        Swal.fire('Erro', 'Não foi possível carregar o formulário.', 'error');
    }
}

async function handleFormSubmit(event, successCallback) {
    event.preventDefault();
    const form = event.target;
    const url = form.action;
    const formData = new FormData(form);
    const method = form.method;

    try {
        const response = await fetch(url, {
            method: method.toUpperCase(),
            body: new URLSearchParams(formData)
        });
        
        const responseData = await response.json();

        if (response.ok) {
            closeModal();
            Swal.fire({
                icon: 'success',
                title: 'Sucesso!',
                text: responseData.message || 'Operação realizada com sucesso.',
                timer: 2000,
                showConfirmButton: false
            }).then(() => {
                if (successCallback) {
                    successCallback(responseData);
                } else {
                    window.location.reload();
                }
            });
        } else {
            Swal.fire({
                icon: 'error',
                title: 'Erro ao Salvar',
                text: responseData.message || 'Ocorreu um erro desconhecido.',
            });
        }
    } catch (error) {
        console.error('Erro ao enviar formulário:', error);
        Swal.fire('Erro', 'Não foi possível comunicar com o servidor.', 'error');
    }
}

function closeModal() {
    if(modal) {
        modal.style.display = 'none';
    }
    if(modalContent) {
        modalContent.innerHTML = '';
    }
}