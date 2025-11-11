document.addEventListener('DOMContentLoaded', function () {
    const formModalElement = document.getElementById('formModal');
    if (!formModalElement) return;

    const formModal = createStaticModal(formModalElement);
    const modalContent = document.getElementById('modalContent');
    const listaDesktop = document.getElementById('lista-usuarios-desktop');
    const listaMobile = document.getElementById('lista-usuarios-mobile');

    const showLoading = (title = 'Processando...') => {
        Swal.fire({
            title: title,
            didOpen: () => { Swal.showLoading(); },
            allowOutsideClick: false, allowEscapeKey: false, allowEnterKey: false
        });
    };

    const showSuccess = (message, reload = false) => { // MUDANÇA: reload = false
        Swal.fire({
            icon: 'success', title: 'Sucesso!', text: message, timer: 2000, showConfirmButton: false
        }).then(() => { if (reload) window.location.reload(); });
    };

    const showError = (message) => {
        if (Swal.isLoading()) { Swal.close(); }
        Swal.fire({ icon: 'error', title: 'Erro!', text: message });
    };

    const handleFormSubmit = async (event) => {
        event.preventDefault();
        showLoading('Salvando...');
        const form = event.target;
        const isEdit = form.id === 'usuarioEditForm';
        
        try {
            const response = await fetch(form.action, {
                method: 'POST',
                body: new URLSearchParams(new FormData(form))
            });

            const responseHtml = await response.text(); // Espera HTML ou JSON

            if (response.ok) {
                try {
                    // Tenta parsear como JSON. Se falhar, é HTML (sucesso)
                    const errorData = JSON.parse(responseHtml);
                    // Caso especial: "Nenhuma alteração detectada" ainda é um sucesso
                    if (errorData.message && errorData.message.includes("Nenhuma alteração")) {
                         formModal.hide();
                         showSuccess(errorData.message, false);
                    } else {
                        showError(errorData.message || 'Ocorreu um erro.');
                    }
                } catch (e) {
                    // SUCESSO! Veio HTML.
                    formModal.hide();
                    showSuccess('Usuário salvo com sucesso.', false);

                    const placeholder = document.getElementById('empty-placeholder');
                    if (placeholder) {
                        placeholder.remove();
                    }

                    // Renderiza o HTML recebido
                    const tempTable = document.createElement('tbody');
                    tempTable.innerHTML = responseHtml;
                    const newRow = tempTable.firstElementChild;
                    
                    const tempDiv = document.createElement('div');
                    tempDiv.innerHTML = responseHtml.replace("fragment-row", "fragment-card"); // Adapta para o card
                    const newCard = tempDiv.firstElementChild;
                    
                    if (!newRow || !newCard || !newRow.dataset.usuarioId) {
                        console.error("Fragmento de HTML inválido recebido.");
                        window.location.reload(); // Fallback
                        return;
                    }
                    
                    const userId = newRow.dataset.usuarioId;

                    if (isEdit) {
                        // Atualiza ou adiciona (caso o filtro tenha mudado)
                        const existingRow = listaDesktop?.querySelector(`tr[data-usuario-id="${userId}"]`);
                        if(existingRow) existingRow.replaceWith(newRow);
                        else if(listaDesktop) listaDesktop.prepend(newRow);

                        const existingCard = listaMobile?.querySelector(`.col[data-usuario-id="${userId}"]`);
                        if(existingCard) existingCard.replaceWith(newCard);
                        else if(listaMobile) listaMobile.prepend(newCard);
                    } else {
                        // Adiciona novo
                        if(listaDesktop) listaDesktop.prepend(newRow);
                        if(listaMobile) listaMobile.prepend(newCard);
                    }
                    
                    // Adiciona listeners aos botões do novo item
                    addCardListeners(newRow);
                    addCardListeners(newCard);
                }
            } else {
                const responseData = JSON.parse(responseHtml);
                showError(responseData.message || 'Ocorreu um erro.');
            }
        } catch (error) {
            console.error('Erro no submit do formulário:', error);
            showError('Erro de Comunicação!');
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
            showError('Não foi possível carregar o formulário.');
        }
    };
    
    document.getElementById('btnNovoUsuario')?.addEventListener('click', () => {
        const condominioId = document.querySelector('select[name="condominioId"]')?.value || '';
        openFormModal(`/usuarios/novo?condominioId=${condominioId}`);
    });

    // --- Lógica específica do formulário de usuários ---
    const toggleFormFields = () => {
        const papelSelect = document.getElementById('papel');
        if (!papelSelect) return; 

        const blocoMorador = document.getElementById('blocoMorador');
        const blocoNovoUsuario = document.getElementById('blocoNovoUsuario');
        const acaoSenhaSelect = document.getElementById('acaoSenha');
        const blocoSenhaManual = document.getElementById('blocoSenhaManual');
        const selectOcupante = document.getElementById('selectOcupante');
        
        const pessoaIdInput = document.getElementById('pessoaId');
        const cpfInput = document.getElementById('pesCpfCnpj');
        const nomeInput = document.getElementById('pesNome');
        const emailInput = document.getElementById('pesEmail');
        
        if (!blocoMorador || !blocoNovoUsuario || !acaoSenhaSelect || !blocoSenhaManual || !selectOcupante) return;

        const papel = papelSelect.value;

        if (papel === 'MORADOR') {
            blocoMorador.style.display = 'block';
            blocoNovoUsuario.style.display = 'none';
            [cpfInput, nomeInput, emailInput].forEach(input => {
                if(input) {
                    input.value = '';
                    input.required = false;
                }
            });
        } else if (papel) {
            blocoMorador.style.display = 'none';
            blocoNovoUsuario.style.display = 'block';
            [cpfInput, nomeInput, emailInput].forEach(input => {
                if(input) input.required = true;
            });
            selectOcupante.value = '';
            if(pessoaIdInput) pessoaIdInput.value = '';
        } else {
            blocoMorador.style.display = 'none';
            blocoNovoUsuario.style.display = 'none';
        }

        if (acaoSenhaSelect.value === 'CRIAR_SENHA' && papel !== 'MORADOR') {
            blocoSenhaManual.style.display = 'block';
            document.getElementById('pesSenhaLogin').required = true;
        } else {
            blocoSenhaManual.style.display = 'none';
            document.getElementById('pesSenhaLogin').required = false;
            document.getElementById('pesSenhaLogin').value = '';
        }
    };

    const handleSelectOcupante = (event) => {
        const select = event.target;
        const pessoaIdInput = document.getElementById('pessoaId');
        const acaoSenhaSelect = document.getElementById('acaoSenha');

        if (select.value) {
            pessoaIdInput.value = select.value;
            acaoSenhaSelect.value = 'ENVIAR_LINK';
            acaoSenhaSelect.disabled = true;
            document.getElementById('blocoSenhaManual').style.display = 'none';
        } else {
            pessoaIdInput.value = '';
            acaoSenhaSelect.disabled = false;
        }
    };

    const buscarPessoaPorCpf = async (cpf) => {
        if (!cpf || cpf.length < 11 || document.getElementById('papel').value === 'MORADOR') return;
        
        const fields = {
            nome: document.getElementById('pesNome'),
            email: document.getElementById('pesEmail'),
            telefone: document.getElementById('pesTelefone'),
            pessoaId: document.getElementById('pessoaId')
        };

        try {
            const response = await fetch(`/api/pessoas/por-cpf/${cpf}`);
            if (response.ok) {
                const pessoa = await response.json();
                fields.nome.value = pessoa.pesNome;
                fields.email.value = pessoa.pesEmail;
                fields.telefone.value = pessoa.pesTelefone || '';
                fields.pessoaId.value = pessoa.pesCod;
                Object.values(fields).forEach(field => field.readOnly = true);
            } else {
                Object.values(fields).forEach(field => field.readOnly = false);
                fields.pessoaId.value = '';
            }
        } catch (error) {
            console.error('Erro ao buscar pessoa:', error);
        }
    };

    // Função para adicionar listeners aos botões
    function addCardListeners(element) {
        element.querySelector('.btn-excluir-vinculo')?.addEventListener('click', handleExcluirClick);
        element.querySelector('.btn-enviar-link')?.addEventListener('click', handleEnviarLinkClick);
        element.querySelector('.btn-edit-vinculo')?.addEventListener('click', handleEditarClick);
    }
    
    // Handlers dos botões
    const handleExcluirClick = async (event) => {
        const btnExcluir = event.currentTarget;
        const { pessoaId, condominioId, papel } = btnExcluir.dataset;
        
        const result = await Swal.fire({
            title: 'Confirmar Exclusão', text: "Deseja realmente remover o acesso deste usuário?", icon: 'warning',
            showCancelButton: true, confirmButtonColor: '#d33', cancelButtonText: 'Cancelar', confirmButtonText: 'Sim, Excluir!'
        });

        if (result.isConfirmed) {
            showLoading('Excluindo...');
            const formData = new URLSearchParams();
            formData.append('pessoaId', pessoaId);
            formData.append('condominioId', condominioId);
            formData.append('papel', papel);

            try {
                const response = await fetch('/usuarios/excluir-vinculo', { method: 'POST', body: formData });
                const responseData = await response.json();
                if(response.ok) {
                    // MUDANÇA: Remove do DOM em vez de recarregar
                    const userId = `${pessoaId}-${condominioId}`;
                    listaDesktop?.querySelector(`tr[data-usuario-id="${userId}"]`)?.remove();
                    listaMobile?.querySelector(`.col[data-usuario-id="${userId}"]`)?.remove();
                    showSuccess(responseData.message, false);
                } else {
                    showError(responseData.message || 'Erro ao excluir.');
                }
            } catch(e) {
                showError('Erro de comunicação.');
            }
        }
    };

    const handleEnviarLinkClick = async (event) => {
        const btnEnviarLink = event.currentTarget;
        const { pessoaId } = btnEnviarLink.dataset;
        showLoading('Enviando link...');
        const formData = new URLSearchParams();
        formData.append('pessoaId', pessoaId);

        try {
            const response = await fetch('/usuarios/enviar-link-reset', { method: 'POST', body: formData });
            const responseData = await response.json();
            if(response.ok) {
                showSuccess(responseData.message, false); // Não recarrega
            } else {
                showError(responseData.message || 'Erro ao enviar.');
            }
        } catch(e) {
            showError('Erro de comunicação.');
        }
    };

    const handleEditarClick = (event) => {
        const btnEditar = event.currentTarget;
        const { pessoaId, condominioId, papel } = btnEditar.dataset;
        const url = `/usuarios/editar?pessoaId=${pessoaId}&condominioId=${condominioId}&papel=${papel}`;
        openFormModal(url);
    };

    // Adiciona listeners para os itens já na tela
    document.querySelectorAll('.btn-excluir-vinculo').forEach(btn => btn.addEventListener('click', handleExcluirClick));
    document.querySelectorAll('.btn-enviar-link').forEach(btn => btn.addEventListener('click', handleEnviarLinkClick));
    document.querySelectorAll('.btn-edit-vinculo').forEach(btn => btn.addEventListener('click', handleEditarClick));

    // Listeners do Modal
    modalContent.addEventListener('submit', (event) => {
        if (event.target.matches('#usuarioForm') || event.target.matches('#usuarioEditForm')) {
            handleFormSubmit(event);
        }
    });

    modalContent.addEventListener('change', (event) => {
        if (event.target.matches('#papel')) toggleFormFields();
        if (event.target.matches('#acaoSenha')) toggleFormFields();
        if (event.target.matches('#selectOcupante')) handleSelectOcupante(event);
    });

    modalContent.addEventListener('blur', (event) => {
        if (event.target.matches('#pesCpfCnpj')) {
            buscarPessoaPorCpf(event.target.value);
        }
    }, true);

    document.addEventListener('modalContentLoaded', () => {
        if (document.getElementById('papel')) {
            toggleFormFields();
        }
    });
});