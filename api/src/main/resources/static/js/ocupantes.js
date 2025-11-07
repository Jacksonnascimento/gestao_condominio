document.addEventListener('DOMContentLoaded', function () {
    const formModalElement = document.getElementById('formModal');
    if (!formModalElement) return;

    const formModal = createStaticModal(formModalElement);
    const modalContent = document.getElementById('modalContent');

    const handleFormSubmit = async (event) => {
        event.preventDefault();
        const form = event.target;

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
                formModal.hide();
                await Swal.fire({ icon: 'success', title: 'Sucesso!', text: 'Ocupante salvo com sucesso.', timer: 2000, showConfirmButton: false });
                window.location.reload();
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
    
    document.getElementById('btnNovoOcupante')?.addEventListener('click', () => openFormModal('/ocupantes/novo'));
    document.querySelectorAll('.btn-edit').forEach(btn => btn.addEventListener('click', () => openFormModal(btn.dataset.url)));

    

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