document.addEventListener('DOMContentLoaded', () => {

    const mainModalElement = document.getElementById('formModal');
    if (!mainModalElement) {
        console.error('Modal principal #formModal não encontrado.');
        return;
    }
    const mainModalContent = document.getElementById('modalContent');
    const mainModal = createStaticModal(mainModalElement);
    const listaVisitantes = document.getElementById('lista-visitantes');

    const showLoadingFeedback = (title = 'Processando...') => {
        Swal.fire({
            title: title,
            didOpen: () => { Swal.showLoading(); },
            allowOutsideClick: false, allowEscapeKey: false, allowEnterKey: false
        });
    };

    const showSuccessFeedback = (message = 'Operação realizada com sucesso.', reload = false) => {
        Swal.fire({
            icon: 'success', title: 'Sucesso!', text: message, timer: 2000, showConfirmButton: false
        }).then(() => { if (reload) window.location.reload(); });
    };

    const showErrorFeedback = (message = 'Ocorreu um erro.') => {
        if (Swal.isLoading()) { Swal.close(); }
        Swal.fire({ icon: 'error', title: 'Erro!', text: message });
    };

    // --- FUNÇÕES DE MÁSCARA ---
    const applyMasks = (container) => {
        const cpfInput = container.querySelector('#cpf');
        const rgInput = container.querySelector('#rg');
        const telefoneInput = container.querySelector('#telefone');

        if (cpfInput) {
            cpfInput.addEventListener('input', (e) => {
                let value = e.target.value.replace(/\D/g, '');
                if (value.length > 11) value = value.slice(0, 11);
                
                value = value.replace(/(\d{3})(\d)/, '$1.$2');
                value = value.replace(/(\d{3})(\d)/, '$1.$2');
                value = value.replace(/(\d{3})(\d{1,2})$/, '$1-$2');
                
                e.target.value = value;
            });
        }

        if (rgInput) {
            rgInput.addEventListener('input', (e) => {
                // RG varia muito por estado. Mantendo apenas números conforme o Requisito Funcional.
                // Se quiser formatar (ex: SSP-SP), altere aqui.
                e.target.value = e.target.value.replace(/\D/g, ''); 
            });
        }

        if (telefoneInput) {
            telefoneInput.addEventListener('input', (e) => {
                let value = e.target.value.replace(/\D/g, '');
                if (value.length > 11) value = value.slice(0, 11);
                
                // Máscara Híbrida (8 ou 9 dígitos)
                // (11) 99999-9999 ou (11) 4444-4444
                value = value.replace(/^(\d{2})(\d)/g, '($1) $2');
                value = value.replace(/(\d)(\d{4})$/, '$1-$2');
                
                e.target.value = value;
            });
        }
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
        
        // Remove máscaras antes de enviar (opcional, depende de como o backend espera. 
        // O DTO atual espera String, então formatado ou não, vai passar, mas é bom padronizar).
        // Aqui enviaremos com a máscara mesmo, pois o DTO recebe String.
        
        try {
            const response = await fetch(form.action, {
                method: form.method.toUpperCase(),
                body: new URLSearchParams(new FormData(form))
            });

            const responseHtml = await response.text();

            if (response.ok) {
                try {
                    const errorData = JSON.parse(responseHtml);
                    showErrorFeedback(errorData.message || 'Ocorreu um erro.');
                } catch (e) {
                    // SUCESSO! Veio HTML.
                    mainModal.hide();
                    showSuccessFeedback('Operação realizada com sucesso.', false);

                    const placeholder = document.getElementById('empty-placeholder');
                    if (placeholder) {
                        placeholder.remove();
                    }
                    
                    const tempDiv = document.createElement('div');
                    tempDiv.innerHTML = responseHtml;
                    const newCardElement = tempDiv.firstElementChild;
                    
                    if (!newCardElement || !newCardElement.dataset.visitanteId) {
                         console.error("Fragmento de HTML inválido recebido.");
                         window.location.reload(); // Fallback
                         return;
                    }
                    
                    const id = newCardElement.dataset.visitanteId;
                    const existingCard = listaVisitantes.querySelector(`.col[data-visitante-id="${id}"]`);
                    
                    if (existingCard) {
                        existingCard.replaceWith(newCardElement);
                    } else {
                        listaVisitantes.prepend(newCardElement);
                    }
                    addCardListeners(newCardElement);
                }
            } else {
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

    const carregarMoradoresPorUnidade = async (unidadeId, moradorSelectElement, selectedMoradorId = null) => {
        if (!moradorSelectElement) return;
        
        if (!unidadeId) {
            moradorSelectElement.innerHTML = '<option value="">Selecione a unidade...</option>';
            moradorSelectElement.disabled = true;
            return;
        }

        moradorSelectElement.innerHTML = '<option value="">Carregando...</option>';
        moradorSelectElement.disabled = true;

        try {
            const response = await fetch(`/api/ocupantes?unidadeId=${unidadeId}`);
            if (!response.ok) throw new Error('Falha ao buscar moradores.');
            const ocupantes = await response.json();

            moradorSelectElement.innerHTML = '<option value="">Selecione...</option>';
            
            if (ocupantes.length === 0) {
                 moradorSelectElement.innerHTML = '<option value="">Nenhum morador encontrado</option>';
            } else {
                ocupantes.forEach(oc => {
                    const selected = (selectedMoradorId && parseInt(selectedMoradorId) === oc.pessoaId) ? 'selected' : '';
                    const vinculoDescricao = oc.vinculo ? oc.vinculo.descricao : '';
                    moradorSelectElement.innerHTML += `<option value="${oc.pessoaId}" ${selected}>${oc.nomeCompleto} (${vinculoDescricao})</option>`;
                });
            }
            moradorSelectElement.disabled = false;
        } catch (error) {
            console.error('Erro ao carregar moradores:', error);
            moradorSelectElement.innerHTML = '<option value="">Erro ao carregar</option>';
            moradorSelectElement.disabled = true;
        }
    };

    function addCardListeners(cardElement) {
        cardElement.querySelector('.btn-editar-visitante')?.addEventListener('click', (e) => {
            const id = e.currentTarget.dataset.visitanteId;
            fetchAndInjectModalContent(`/visitantes/editar/${id}`);
        });

        cardElement.querySelector('.btn-registrar-saida')?.addEventListener('click', (e) => {
            const id = e.currentTarget.dataset.visitanteId;
            fetchAndInjectModalContent(`/visitantes/saida/${id}`);
        });
    }

    document.getElementById('btnNovoVisitante')?.addEventListener('click', () => {
        const urlParams = new URLSearchParams(window.location.search);
        const condominioId = urlParams.get('condominioId') || '';
        fetchAndInjectModalContent(`/visitantes/novo?condominioId=${condominioId}`);
    });

    document.querySelectorAll('#lista-visitantes .col').forEach(card => {
        addCardListeners(card);
    });

    document.addEventListener('modalContentLoaded', (event) => {
        const modalContent = event.detail.modalContent;

        // Aplica as máscaras assim que o conteúdo do modal é carregado
        applyMasks(modalContent);

        const formNova = modalContent.querySelector('#visitanteForm');
        if (formNova) {
            formNova.addEventListener('submit', handleAjaxFormSubmit);
            const condominioSelect = modalContent.querySelector('#condominioId');
            const unidadeSelect = modalContent.querySelector('#unidadeId');
            const moradorSelect = modalContent.querySelector('#moradorId');
            const selectedMoradorId = modalContent.querySelector('#selectedMoradorId')?.value;

            if (condominioSelect && unidadeSelect) {
                condominioSelect.addEventListener('change', (e) => {
                    carregarUnidadesPorCondominio(e.target.value, unidadeSelect);
                    if(moradorSelect) {
                        moradorSelect.innerHTML = '<option value="">Selecione a unidade...</option>';
                        moradorSelect.disabled = true;
                    }
                });
            }

            if (unidadeSelect && moradorSelect) {
                unidadeSelect.addEventListener('change', (e) => {
                    carregarMoradoresPorUnidade(e.target.value, moradorSelect);
                });
                
                if (unidadeSelect.value) {
                    carregarMoradoresPorUnidade(unidadeSelect.value, moradorSelect, selectedMoradorId);
                }
            }
        }

        const formSaida = modalContent.querySelector('#visitanteSaidaForm');
        if (formSaida) {
            formSaida.addEventListener('submit', handleAjaxFormSubmit);
        }
    });

});