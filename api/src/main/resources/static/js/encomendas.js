document.addEventListener('DOMContentLoaded', () => {

    const mainModalElement = document.getElementById('formModal');
    if (!mainModalElement) {
        console.error('Modal principal #formModal não encontrado.');
        return;
    }
    const mainModalContent = document.getElementById('modalContent');
    const mainModal = createStaticModal(mainModalElement);
    const listaEncomendas = document.getElementById('lista-encomendas');

    const showLoadingFeedback = (title = 'Processando...') => {
        Swal.fire({
            title: title,
            didOpen: () => { Swal.showLoading(); },
            allowOutsideClick: false, allowEscapeKey: false, allowEnterKey: false
        });
    };

    // MUDANÇA: reload = false por padrão
    const showSuccessFeedback = (message = 'Operação realizada com sucesso.', reload = false) => {
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

            const responseHtml = await response.text(); // Espera HTML ou JSON

            if (response.ok) {
                try {
                    // Tenta parsear como JSON. Se falhar, é porque veio HTML (sucesso)
                    const errorData = JSON.parse(responseHtml);
                    showErrorFeedback(errorData.message || 'Ocorreu um erro.');
                
                } catch (e) {
                    // SUCESSO! Veio HTML.
                    mainModal.hide();
                    showSuccessFeedback('Operação realizada com sucesso.', false); // false = não recarregar

                    const placeholder = document.getElementById('empty-placeholder');
                    if (placeholder) {
                        placeholder.remove();
                    }
                    
                    const tempDiv = document.createElement('div');
                    tempDiv.innerHTML = responseHtml;
                    const newCardElement = tempDiv.firstElementChild;
                    
                    if (!newCardElement || !newCardElement.dataset.encomendaId) {
                         console.error("Fragmento de HTML inválido recebido.");
                         window.location.reload(); // Fallback
                         return;
                    }
                    
                    // CORREÇÃO AQUI: Verifica se a lista existe antes de manipular
                    if (listaEncomendas) {
                        const id = newCardElement.dataset.encomendaId;
                        const existingCard = listaEncomendas.querySelector(`.col[data-encomenda-id="${id}"]`);
                        
                        if (existingCard) {
                            existingCard.replaceWith(newCardElement);
                        } else {
                            listaEncomendas.prepend(newCardElement);
                        }
                        addCardListeners(newCardElement); // Adiciona listeners ao card novo/atualizado
                    } else {
                        // Se a lista não existe (primeiro item), recarrega para o servidor renderizar
                        window.location.reload();
                    }
                }
            } else {
                // Erro (400, 500, etc.)
                const errorData = JSON.parse(responseHtml);
                showErrorFeedback(errorData.message || 'Ocorreu um erro.');
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

    // Nova função para adicionar listeners aos botões dos cards
    function addCardListeners(cardElement) {
        cardElement.querySelector('.btn-registrar-retirada')?.addEventListener('click', (e) => {
            const id = e.currentTarget.dataset.encomendaId;
            fetchAndInjectModalContent(`/encomendas/${id}/retirar`);
        });

        cardElement.querySelector('.btn-atualizar-status')?.addEventListener('click', (e) => {
            const id = e.currentTarget.dataset.encomendaId;
            fetchAndInjectModalContent(`/encomendas/${id}/atualizar-status`);
        });
    }

    // Listener para o botão "Nova Encomenda"
    document.getElementById('btnNovaEncomenda')?.addEventListener('click', () => {
        const urlParams = new URLSearchParams(window.location.search);
        const condominioId = urlParams.get('condominioId') || '';
        fetchAndInjectModalContent(`/encomendas/novo?condominioId=${condominioId}`);
    });

    // Adiciona listeners para todos os cards já existentes na página
    document.querySelectorAll('#lista-encomendas .col').forEach(card => {
        addCardListeners(card);
    });

    // Listener global para quando o conteúdo do modal é carregado
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