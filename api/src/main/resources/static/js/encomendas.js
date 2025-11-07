document.addEventListener('DOMContentLoaded', () => {

    const mainModalElement = document.getElementById('formModal');
    if (!mainModalElement) {
        console.error('Modal principal #formModal não encontrado.');
        return;
    }
    const mainModalContent = document.getElementById('modalContent');
    const mainModal = createStaticModal(mainModalElement);

    const showLoadingFeedback = (title = 'Processando...') => {
        Swal.fire({
            title: title,
            didOpen: () => { Swal.showLoading(); },
            allowOutsideClick: false, allowEscapeKey: false, allowEnterKey: false
        });
    };

    const showSuccessFeedback = (message = 'Operação realizada com sucesso.', reload = true) => {
        Swal.fire({
            icon: 'success', title: 'Sucesso!', text: message, timer: 2000, showConfirmButton: false
        }).then(() => { if (reload) window.location.reload(); });
    };

    const showErrorFeedback = (message = 'Ocorreu um erro.') => {
        if (Swal.isLoading()) { Swal.close(); }
        Swal.fire({ icon: 'error', title: 'Erro!', text: message });
    };

    const fetchAndInjectModalContent = async (url) => {
        mainModalContent.innerHTML = '<div class="modal-body text-center"><span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Carregando...</div>';
        mainModal.show();
        try {
            const response = await fetch(url);
            if (!response.ok) throw new Error(`Falha ao carregar conteúdo: ${response.statusText}.`);
            mainModalContent.innerHTML = await response.text();
            document.dispatchEvent(new CustomEvent('modalContentLoaded', { detail: { modalContent: mainModalContent } }));
            return true;
        } catch (error) {
            console.error('Erro ao buscar/injetar conteúdo do modal:', error);
            mainModalContent.innerHTML = `<div class="modal-body"><p class="text-danger">Erro ao carregar o conteúdo. Tente novamente.</p></div>`;
            return false;
        }
    };

    const handleAjaxFormSubmit = async (event) => {
        event.preventDefault();
        const form = event.target;
        showLoadingFeedback();
        
        try {
            const response = await fetch(form.action, {
                method: form.method.toUpperCase(),
                body: new URLSearchParams(new FormData(form))
            });

            const responseData = await response.json();

            if (response.ok) {
                mainModal.hide();
                showSuccessFeedback(responseData.message || 'Operação realizada com sucesso.');
            } else {
                showErrorFeedback(responseData.message || 'Ocorreu um erro.');
            }
        } catch (error) {
            console.error('Erro no submit AJAX:', error);
            showErrorFeedback('Erro de comunicação com o servidor.');
        }
    };

    const carregarUnidadesPorCondominio = async (condominioId, unidadeSelectElement) => {
        if (!unidadeSelectElement) return;
        unidadeSelectElement.innerHTML = '<option value="">Carregando...</option>';
        unidadeSelectElement.disabled = true;

        if (!condominioId) {
            unidadeSelectElement.innerHTML = '<option value="">Selecione um condomínio</option>';
            unidadeSelectElement.disabled = false;
            return;
        }

        try {
            const response = await fetch(`/api/unidades/por-condominio/${condominioId}`);
            if (!response.ok) throw new Error('Falha ao buscar unidades.');
            const unidades = await response.json();

            unidadeSelectElement.innerHTML = '<option value="">Selecione...</option>';
            unidades.forEach(unidade => {
                const optionText = `${unidade.uniNumero}${unidade.bloco ? ' - ' + unidade.bloco : ''}`;
                unidadeSelectElement.innerHTML += `<option value="${unidade.uniCod}">${optionText}</option>`;
            });
            unidadeSelectElement.disabled = false;
        } catch (error) {
            console.error('Erro ao carregar unidades:', error);
            unidadeSelectElement.innerHTML = '<option value="">Erro ao carregar</option>';
            unidadeSelectElement.disabled = true;
        }
    };

    document.addEventListener('click', (event) => {
        const target = event.target.closest('button');
        if (!target) return;

        if (target.id === 'btnNovaEncomenda') {
            const urlParams = new URLSearchParams(window.location.search);
            const condominioId = urlParams.get('condominioId') || '';
            fetchAndInjectModalContent(`/encomendas/novo?condominioId=${condominioId}`);
        }

        if (target.classList.contains('btn-registrar-retirada')) {
            const id = target.dataset.encomendaId;
            fetchAndInjectModalContent(`/encomendas/${id}/retirar`);
        }

        if (target.classList.contains('btn-atualizar-status')) {
            const id = target.dataset.encomendaId;
            fetchAndInjectModalContent(`/encomendas/${id}/atualizar-status`);
        }
    });

    document.addEventListener('modalContentLoaded', (event) => {
        const modalContent = event.detail.modalContent;

        const formNova = modalContent.querySelector('#encomendaForm');
        if (formNova) {
            formNova.addEventListener('submit', handleAjaxFormSubmit);
            const condominioSelect = modalContent.querySelector('#condominioId');
            const unidadeSelect = modalContent.querySelector('#unidadeId');
            if (condominioSelect && unidadeSelect) {
                condominioSelect.addEventListener('change', (e) => {
                    carregarUnidadesPorCondominio(e.target.value, unidadeSelect);
                });
            }
        }

        const formRetirada = modalContent.querySelector('#encomendaRetiradaForm');
        if (formRetirada) {
            formRetirada.addEventListener('submit', handleAjaxFormSubmit);
        }

        const formStatus = modalContent.querySelector('#encomendaStatusForm');
        if (formStatus) {
            formStatus.addEventListener('submit', handleAjaxFormSubmit);
        }
    });

});