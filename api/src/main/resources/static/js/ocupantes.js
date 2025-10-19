function openOcupanteForm(url) {
    openForm(url, 'ocupanteForm');
}

async function carregarUnidadesPorCondominio(condominioId) {
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
        console.error('Erro na função carregarUnidadesPorCondominio:', error);
        unidadeSelect.innerHTML = '<option value="">Erro ao carregar</option>';
    }
}

async function buscarPessoaPorCpf(cpfInput) {
    const cpf = cpfInput.value;
    const h2 = document.querySelector('.modal-content h2');
    if ((h2 && h2.textContent.startsWith('Editar')) || !cpf || cpf.length < 11) {
        return;
    }
    try {
        const response = await fetch(`/api/pessoas/por-cpf/${cpf}`);
        const nomeInput = document.getElementById('pesNome');
        const emailInput = document.getElementById('pesEmail');
        const telefoneInput = document.getElementById('pesTelefone');
        
        const fieldsToToggle = [nomeInput, emailInput, telefoneInput];

        if (response.ok) {
            const pessoa = await response.json();
            nomeInput.value = pessoa.pesNome;
            emailInput.value = pessoa.pesEmail;
            telefoneInput.value = pessoa.pesTelefone;
            fieldsToToggle.forEach(field => field.readOnly = true);
        } else {
            fieldsToToggle.forEach(field => {
                if (field !== cpfInput) field.value = '';
                field.readOnly = false;
            });
        }
    } catch (error) {
        console.error('Erro ao buscar pessoa:', error);
    }
}

function toggleMultipropriedadeFields() {
    const vinculoSelect = document.getElementById('vinculo');
    const fields = document.getElementById('multipropriedade-fields');
    if (vinculoSelect && fields) {
        fields.style.display = (vinculoSelect.value === 'MULTIPROPRIETARIO') ? 'block' : 'none';
    }
}

// --- EVENT LISTENERS ---

// Listener para buscar pessoa por CPF quando o campo perde o foco
document.addEventListener('blur', function(event) {
    if (event.target.matches('#pesCpfCnpj')) {
        buscarPessoaPorCpf(event.target);
    }
}, true); // Use capturing para garantir que o evento seja capturado

// Listener para eventos de 'change' dentro do modal
document.addEventListener('change', function(event) {
    const target = event.target;
    if (target.matches('#vinculo')) {
        toggleMultipropriedadeFields();
    }
    if (target.matches('#condominioId')) {
        carregarUnidadesPorCondominio(target.value);
    }
});

// Listener para quando o formulário do modal for carregado
document.addEventListener('modalContentLoaded', function() {
    toggleMultipropriedadeFields();
});