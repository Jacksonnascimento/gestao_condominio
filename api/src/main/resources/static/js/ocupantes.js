document.addEventListener('DOMContentLoaded', function () {
    const formModalElement = document.getElementById('formModal');
    if (!formModalElement) return;

    const formModal = createStaticModal(formModalElement);
    const modalContent = document.getElementById('modalContent');
    const listaOcupantes = document.getElementById('lista-ocupantes');

    const handleFormSubmit = async (event) => {
        event.preventDefault();
        const form = event.target;
        const url = form.action;

        const inicioOcupacaoInput = form.querySelector('#inicioOcupacao');
        const fimOcupacaoInput = form.querySelector('#fimOcupacao');

        if (inicioOcupacaoInput && fimOcupacaoInput) {
            const inicioVal = inicioOcupacaoInput.value;
            const fimVal = fimOcupacaoInput.value;

            if (inicioVal && fimVal && inicioVal > fimVal) {
                Swal.fire({
                    icon: 'error',
                    title: 'Datas Inválidas!',
                    text: 'A data de início da ocupação não pode ser posterior à data de fim.'
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
                    // Tenta parsear como JSON. Se falhar, é porque veio HTML (sucesso)
                    const errorData = JSON.parse(responseHtml);
                    Swal.fire({ icon: 'error', title: 'Erro!', text: errorData.message || 'Ocorreu um erro ao salvar.' });
                
                } catch (e) {
                    // SUCESSO! Veio HTML.
                    formModal.hide();
                    await Swal.fire({ icon: 'success', title: 'Sucesso!', text: 'Ocupante salvo com sucesso.', timer: 2000, showConfirmButton: false });

                    const placeholder = document.getElementById('empty-placeholder');
                    if (placeholder) {
                        placeholder.remove();
                    }

                    const tempDiv = document.createElement('div');
                    tempDiv.innerHTML = responseHtml;
                    const newCardElement = tempDiv.firstElementChild;

                    if (!newCardElement || !newCardElement.dataset.ocupanteId) {
                         console.error("Fragmento de HTML inválido recebido:", responseHtml);
                         window.location.reload(); // Fallback
                         return;
                    }

                    let isEdit = url.includes("/editar/");
                    const ocupanteId = newCardElement.dataset.ocupanteId;
                    
                    if (isEdit) {
                        const existingCard = listaOcupantes?.querySelector(`.col[data-ocupante-id="${ocupanteId}"]`);
                        if (existingCard) {
                            existingCard.replaceWith(newCardElement);
                        } else if (listaOcupantes) {
                            listaOcupantes.prepend(newCardElement);
                        }
                    } else if (listaOcupantes) {
                        listaOcupantes.prepend(newCardElement);
                    }
                    
                    if (newCardElement) {
                        addCardListeners(newCardElement);
                    }
                }
            } else {
                 const errorData = await response.json();
                 Swal.fire({ icon: 'error', title: 'Erro!', text: errorData.message || 'Ocorreu um erro.' });
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
            
            document.dispatchEvent(new CustomEvent('modalContentLoaded'));
        } catch (error) {
            console.error('Erro ao abrir o modal:', error);
            Swal.fire({ icon: 'error', title: 'Erro!', text: 'Não foi possível carregar o formulário.' });
        }
    };
    
    // Função helper para adicionar listener ao botão 'Editar' de um card
    function addCardListeners(cardElement) {
        cardElement.querySelector('.btn-edit')?.addEventListener('click', (e) => {
            const url = e.currentTarget.dataset.url;
            openFormModal(url);
        });
    }
    
    document.getElementById('btnNovoOcupante')?.addEventListener('click', () => {
        const urlParams = new URLSearchParams(window.location.search);
        const condominioId = urlParams.get('condominioId') || '';
        openFormModal(`/ocupantes/novo?condominioId=${condominioId}`);
    });
    
    // Adiciona listeners para os cards que carregam com a página
    document.querySelectorAll('#lista-ocupantes .col').forEach(card => {
        addCardListeners(card);
    });

    const carregarUnidadesPorCondominio = async (condominioId) => {
        const unidadeSelect = document.getElementById('unidadeId');
        if (!unidadeSelect) return;
        unidadeSelect.innerHTML = '<option value="">Carregando...</option>';
        if (!condominioId) {
            unidadeSelect.innerHTML = '<option value="">Selecione um condomínio</option>';
            return;
        }
        try {
            const response = await fetch(`/api/unidades/por-condominio/${condominioId}`);
            if (!response.ok) throw new Error('Falha ao buscar unidades.');
            const unidades = await response.json();
            unidadeSelect.innerHTML = '<option value="">Selecione...</option>';
            unidades.forEach(unidade => {
                unidadeSelect.innerHTML += `<option value="${unidade.uniCod}">${unidade.uniNumero}</option>`;
            });
        } catch (error) {
            console.error('Erro:', error);
            unidadeSelect.innerHTML = '<option value="">Erro ao carregar</option>';
        }
    };

    const buscarPessoaPorCpf = async (cpf) => {
        const h2 = modalContent.querySelector('.modal-title');
        if ((h2 && h2.textContent.startsWith('Editar')) || !cpf || cpf.length < 11) return;
        
        const fields = {
            nome: document.getElementById('pesNome'),
            email: document.getElementById('pesEmail'),
            telefone: document.getElementById('pesTelefone')
        };

        try {
            const response = await fetch(`/api/pessoas/por-cpf/${cpf}`);
            if (response.ok) {
                const pessoa = await response.json();
                fields.nome.value = pessoa.pesNome;
                fields.email.value = pessoa.pesEmail;
                fields.telefone.value = pessoa.pesTelefone;
                Object.values(fields).forEach(field => field.readOnly = true);
            } else {
                Object.values(fields).forEach(field => {
                    field.value = '';
                    field.readOnly = false;
                });
            }
        } catch (error) {
            console.error('Erro ao buscar pessoa:', error);
        }
    };

    const toggleMultipropriedadeFields = () => {
        const vinculoSelect = document.getElementById('vinculo');
        const fields = document.getElementById('multipropriedade-fields');
        if (vinculoSelect && fields) {
            fields.style.display = (vinculoSelect.value === 'MULTIPROPRIETARIO') ? 'flex' : 'none';
        }
    };

    
    modalContent.addEventListener('submit', (event) => {
        if (event.target.matches('#ocupanteForm')) {
            handleFormSubmit(event);
        }
    });

    modalContent.addEventListener('change', (event) => {
        if (event.target.matches('#vinculo')) toggleMultipropriedadeFields();
        if (event.target.matches('#condominioId')) carregarUnidadesPorCondominio(event.target.value);
    });

    modalContent.addEventListener('blur', (event) => {
        if (event.target.matches('#pesCpfCnpj')) {
            buscarPessoaPorCpf(event.target.value);
        }
    }, true);

    
    document.addEventListener('modalContentLoaded', toggleMultipropriedadeFields);
});