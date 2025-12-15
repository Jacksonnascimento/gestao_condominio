document.addEventListener('DOMContentLoaded', function () {
    const formModalElement = document.getElementById('formModal');
    if (!formModalElement) return;

    const formModal = createStaticModal(formModalElement);
    const modalContent = document.getElementById('modalContent');
    const listaContratos = document.getElementById('lista-contratos');

    const handleFormSubmit = async (event) => {
        event.preventDefault();
        const form = event.target;
        const url = form.action;

        const dataInicioInput = form.querySelector('#dataInicio');
        const dataFimInput = form.querySelector('#dataFim');

        if (dataInicioInput && dataFimInput) {
            const inicioVal = dataInicioInput.value;
            const fimVal = dataFimInput.value;

            if (inicioVal && fimVal && inicioVal > fimVal) {
                Swal.fire({
                    icon: 'error',
                    title: 'Datas Inválidas!',
                    text: 'A data de início do contrato não pode ser posterior à data de fim.'
                });
                return; 
            }
        }

        try {
            const response = await fetch(form.action, {
                method: 'POST',
                body: new URLSearchParams(new FormData(form))
            });

            if (response.ok) {
                const responseHtml = await response.text();
                
                try {
                    const errorData = JSON.parse(responseHtml);
                    Swal.fire({ icon: 'error', title: 'Erro!', text: errorData.message || 'Ocorreu um erro ao salvar o contrato.' });
                
                } catch (e) {
                    formModal.hide();
                    await Swal.fire({ icon: 'success', title: 'Sucesso!', text: 'Contrato salvo com sucesso.', timer: 2000, showConfirmButton: false });

                    const placeholder = document.getElementById('empty-placeholder');
                    if (placeholder) {
                        placeholder.remove();
                    }

                    const tempDiv = document.createElement('div');
                    tempDiv.innerHTML = responseHtml;
                    const newCardElement = tempDiv.firstElementChild;

                    if (!newCardElement || !newCardElement.querySelector('.btn-edit')) {
                         console.error("Fragmento de HTML inválido recebido.");
                         window.location.reload();
                         return;
                    }

                    if (listaContratos) {
                        let isEdit = url.includes("/editar/");
                        if (isEdit) {
                            const contratoId = newCardElement.querySelector('.btn-edit').dataset.url.split('/').pop();
                            const existingCard = listaContratos.querySelector(`.btn-edit[data-url$="${contratoId}"]`)?.closest('.col');
                            if (existingCard) {
                                existingCard.replaceWith(newCardElement);
                            } else {
                                listaContratos.prepend(newCardElement);
                            }
                        } else {
                            listaContratos.prepend(newCardElement);
                        }
                        
                        if (newCardElement) {
                            addCardListeners(newCardElement);
                        }
                    } else {
                        window.location.reload();
                    }
                }
            } else {
                 const errorData = await response.json();
                 Swal.fire({ icon: 'error', title: 'Erro!', text: errorData.message || 'Ocorreu um erro ao salvar o contrato.' });
            }
        } catch (error) {
            console.error('Erro no submit do formulário:', error);
            Swal.fire({ icon: 'error', title: 'Erro de Comunicação!', text: 'Não foi possível conectar ao servidor.' });
        }
    };

    const handleExcluirClick = async (event) => {
        const btn = event.currentTarget;
        const url = btn.dataset.url;

        const result = await Swal.fire({
            title: 'Tem certeza?',
            text: "Você não poderá reverter esta ação!",
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#d33',
            cancelButtonColor: '#3085d6',
            confirmButtonText: 'Sim, excluir!',
            cancelButtonText: 'Cancelar'
        });

        if (result.isConfirmed) {
            try {
                Swal.fire({
                    title: 'Excluindo...',
                    didOpen: () => { Swal.showLoading(); },
                    allowOutsideClick: false
                });

                const response = await fetch(url, { method: 'POST' });

                if (response.ok) {
                    // Verifica se está removendo um card (.col) ou uma linha de tabela (tr)
                    const card = btn.closest('.col');
                    const row = btn.closest('tr');

                    if (card) card.remove();
                    if (row) row.remove();
                    
                    Swal.fire({
                        title: 'Excluído!',
                        text: 'Contrato excluído com sucesso.',
                        icon: 'success',
                        timer: 2000,
                        showConfirmButton: false
                    });
                } else {
                    let errorMessage = 'Não foi possível excluir o contrato.';
                    try {
                        const errorData = await response.json();
                        if(errorData.message) errorMessage = errorData.message;
                    } catch(e) {}
                    
                    Swal.fire('Erro!', errorMessage, 'error');
                }
            } catch (error) {
                console.error('Erro ao excluir:', error);
                Swal.fire('Erro de Conexão!', 'Não foi possível conectar ao servidor.', 'error');
            }
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

    function addCardListeners(cardElement) {
        cardElement.querySelector('.btn-edit')?.addEventListener('click', (e) => {
            const url = e.currentTarget.dataset.url;
            openFormModal(url);
        });

        cardElement.querySelector('.btn-excluir')?.addEventListener('click', handleExcluirClick);
    }

    document.getElementById('btnNovoContrato')?.addEventListener('click', (e) => {
        const urlParams = new URLSearchParams(window.location.search);
        const condominioId = urlParams.get('condominioId') || '';
        openFormModal(`/contratos/novo?condominioId=${condominioId}`);
    });

    // Adiciona listeners aos cards já existentes na página
    document.querySelectorAll('#lista-contratos .col').forEach(cardElement => {
        addCardListeners(cardElement);
    });
    
    // Adiciona listeners aos botões da tabela de histórico (caso a tabela esteja visível)
    document.querySelectorAll('.responsive-table-container .btn-edit').forEach(button => {
         button.addEventListener('click', (e) => {
             const url = e.currentTarget.dataset.url;
             openFormModal(url);
         });
    });

    // Adiciona listeners aos botões de excluir da tabela de histórico
    document.querySelectorAll('.responsive-table-container .btn-excluir').forEach(button => {
        button.addEventListener('click', handleExcluirClick);
    });

    modalContent.addEventListener('submit', (event) => {
        if (event.target.matches('#contratoForm')) {
            handleFormSubmit(event);
        }
    });
});