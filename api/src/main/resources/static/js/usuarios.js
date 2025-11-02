document.addEventListener('DOMContentLoaded', function () {
    const formModalElement = document.getElementById('formModal');
    if (!formModalElement) return;

    const formModal = createStaticModal(formModalElement);
    const modalContent = document.getElementById('modalContent');

    const showLoading = (title = 'Processando...') => {
        Swal.fire({
            title: title,
            didOpen: () => { Swal.showLoading(); },
            allowOutsideClick: false, allowEscapeKey: false, allowEnterKey: false
        });
    };

    const showSuccess = (message, reload = true) => {
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
        try {
            const response = await fetch(form.action, {
                method: 'POST',
                body: new URLSearchParams(new FormData(form))
            });

            const responseData = await response.json();
            if (response.ok) {
                formModal.hide();
                showSuccess(responseData.message || 'Operação realizada com sucesso.');
            } else {
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
                input.value = '';
                input.required = false;
            });
        } else if (papel) {
            blocoMorador.style.display = 'none';
            blocoNovoUsuario.style.display = 'block';
            [cpfInput, nomeInput, emailInput].forEach(input => {
                input.required = true;
            });
            selectOcupante.value = '';
            pessoaIdInput.value = '';
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

    document.addEventListener('click', async (event) => {
        const btnExcluir = event.target.closest('.btn-excluir-vinculo');
        if (btnExcluir) {
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
                        showSuccess(responseData.message);
                    } else {
                        showError(responseData.message || 'Erro ao excluir.');
                    }
                } catch(e) {
                    showError('Erro de comunicação.');
                }
            }
        }

        const btnEnviarLink = event.target.closest('.btn-enviar-link');
        if (btnEnviarLink) {
            const { pessoaId } = btnEnviarLink.dataset;
            showLoading('Enviando link...');
            const formData = new URLSearchParams();
            formData.append('pessoaId', pessoaId);

            try {
                const response = await fetch('/usuarios/enviar-link-reset', { method: 'POST', body: formData });
                const responseData = await response.json();
                if(response.ok) {
                    showSuccess(responseData.message, false);
                } else {
                    showError(responseData.message || 'Erro ao enviar.');
                }
            } catch(e) {
                showError('Erro de comunicação.');
            }
        }

        const btnEditar = event.target.closest('.btn-edit-vinculo');
        if (btnEditar) {
            const { pessoaId, condominioId, papel } = btnEditar.dataset;
            const url = `/usuarios/editar?pessoaId=${pessoaId}&condominioId=${condominioId}&papel=${papel}`;
            openFormModal(url);
        }
    });

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