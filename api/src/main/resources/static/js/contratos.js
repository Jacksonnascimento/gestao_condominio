document.addEventListener('DOMContentLoaded', function () {
    const formModalElement = document.getElementById('formModal');
    if (!formModalElement) return;

    const formModal = createStaticModal(formModalElement);
    const modalContent = document.getElementById('modalContent');

    const handleFormSubmit = async (event) => {
        event.preventDefault();
        const form = event.target;
        try {
            const response = await fetch(form.action, {
                method: 'POST',
                body: new URLSearchParams(new FormData(form))
            });

            if (response.ok) {
                formModal.hide();
                await Swal.fire({ icon: 'success', title: 'Sucesso!', text: 'Contrato salvo com sucesso.', timer: 2000, showConfirmButton: false });
                window.location.reload();
            } else {
                const errorData = await response.json();
                Swal.fire({ icon: 'error', title: 'Erro!', text: errorData.message || 'Ocorreu um erro ao salvar o contrato.' });
            }
        } catch (error) {
            console.error('Erro no submit do formulário:', error);
            Swal.fire({ icon: 'error', title: 'Erro de Comunicação!', text: 'Não foi possível conectar ao servidor.' });
        }
    };

    const openFormModal = async (url) => {
        try {
            const response = await fetch(url);
            if (!response.ok) throw new Error('Falha ao carregar o formulário.');
            modalContent.innerHTML = await response.text();
            formModal.show();
        } catch (error) {
            console.error('Erro ao abrir o modal:', error);
            Swal.fire({ icon: 'error', title: 'Erro!', text: 'Não foi possível carregar o formulário.' });
        }
    };

    document.getElementById('btnNovoContrato')?.addEventListener('click', (e) => {
        const urlParams = new URLSearchParams(window.location.search);
        const condominioId = urlParams.get('condominioId') || '';
        openFormModal(`/contratos/novo?condominioId=${condominioId}`);
    });

    document.querySelectorAll('.btn-edit').forEach(btn => {
        btn.addEventListener('click', () => openFormModal(btn.dataset.url));
    });

    modalContent.addEventListener('submit', (event) => {
        if (event.target.matches('#contratoForm')) {
            handleFormSubmit(event);
        }
    });
});