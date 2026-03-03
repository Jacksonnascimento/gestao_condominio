let modalAreaComum;

document.addEventListener('DOMContentLoaded', function() {
    const modalEl = document.getElementById('modalAreaComum');
    if (modalEl) {
        modalAreaComum = createStaticModal(modalEl);
    }
});

function abrirModalNovaArea() {
    document.getElementById('formAreaComum').reset();
    document.getElementById('areCod').value = '';
    document.getElementById('turnosContainer').innerHTML = '';
    modalAreaComum.show();
}

// NOVO: Função para buscar o JSON do Backend e preencher a tela para o Síndico
function editarArea(id) {
    fetch(`/areas-comuns/${id}`)
    .then(res => {
        if(!res.ok) throw new Error();
        return res.json();
    })
    .then(data => {
        document.getElementById('formAreaComum').reset();
        document.getElementById('turnosContainer').innerHTML = '';
        
        document.getElementById('areCod').value = data.areCod;
        
        const conCodEl = document.getElementById('conCod');
        if(conCodEl) conCodEl.value = data.conCod;
        
        document.getElementById('nome').value = data.nome;
        document.getElementById('capacidadeMaxima').value = data.capacidadeMaxima || '';
        document.getElementById('descricao').value = data.descricao || '';
        document.getElementById('termosUso').value = data.termosUso || '';
        document.getElementById('permiteConvidados').checked = data.permiteConvidados;
        document.getElementById('limiteConvidados').value = data.limiteConvidados || '';
        document.getElementById('taxaValor').value = data.taxaValor || '';
        document.getElementById('diasAntecedenciaMin').value = data.diasAntecedenciaMin;
        document.getElementById('diasAntecedenciaMax').value = data.diasAntecedenciaMax;
        document.getElementById('ativa').checked = data.ativa;
        
        if(data.turnos && data.turnos.length > 0) {
            data.turnos.forEach(t => {
                adicionarTurno();
                const items = document.querySelectorAll('.turno-item');
                const last = items[items.length - 1];
                
                last.querySelector('.turno-nome').value = t.nome;
                // O substring(0,5) corta os segundos do formato HH:mm:ss do Java para os inputs type="time"
                last.querySelector('.turno-inicio').value = t.horaInicio ? t.horaInicio.substring(0, 5) : '';
                last.querySelector('.turno-fim').value = t.horaFim ? t.horaFim.substring(0, 5) : '';
            });
        }
        
        modalAreaComum.show();
    })
    .catch(e => Swal.fire('Erro', 'Não foi possível carregar os dados da área comum.', 'error'));
}

function adicionarTurno() {
    const container = document.getElementById('turnosContainer');
    const html = `
        <div class="row g-2 align-items-end mb-2 turno-item">
            <div class="col-md-4">
                <label class="form-label small">Nome (Ex: Manhã)</label>
                <input type="text" class="form-control form-control-sm turno-nome" required>
            </div>
            <div class="col-md-3">
                <label class="form-label small">Início</label>
                <input type="time" class="form-control form-control-sm turno-inicio" required>
            </div>
            <div class="col-md-3">
                <label class="form-label small">Fim</label>
                <input type="time" class="form-control form-control-sm turno-fim" required>
            </div>
            <div class="col-md-2">
                <button type="button" class="btn btn-sm btn-outline-danger w-100" onclick="this.closest('.turno-item').remove()">Remover</button>
            </div>
        </div>
    `;
    container.insertAdjacentHTML('beforeend', html);
}

function salvarArea() {
    const form = document.getElementById('formAreaComum');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }

    const conCodEl = document.getElementById('conCod');
    if(!conCodEl || !conCodEl.value) {
        Swal.fire('Atenção', 'Selecione o Condomínio antes de salvar.', 'warning');
        return;
    }

    const dto = {
        areCod: document.getElementById('areCod').value || null,
        conCod: conCodEl.value,
        nome: document.getElementById('nome').value,
        descricao: document.getElementById('descricao').value,
        termosUso: document.getElementById('termosUso').value,
        capacidadeMaxima: document.getElementById('capacidadeMaxima').value || null,
        permiteConvidados: document.getElementById('permiteConvidados').checked,
        limiteConvidados: document.getElementById('limiteConvidados').value || null,
        taxaValor: document.getElementById('taxaValor').value || null,
        diasAntecedenciaMin: document.getElementById('diasAntecedenciaMin').value,
        diasAntecedenciaMax: document.getElementById('diasAntecedenciaMax').value,
        ativa: document.getElementById('ativa').checked,
        turnos: []
    };

    const turnoItems = document.querySelectorAll('.turno-item');
    turnoItems.forEach(item => {
        dto.turnos.push({
            nome: item.querySelector('.turno-nome').value,
            horaInicio: item.querySelector('.turno-inicio').value,
            horaFim: item.querySelector('.turno-fim').value,
            ativo: true
        });
    });

    fetch('/areas-comuns/salvar', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(dto)
    })
    .then(response => response.text())
    .then(responseHtml => {
        try {
            const json = JSON.parse(responseHtml);
            Swal.fire('Erro', json.message, 'error');
        } catch (e) {
            Swal.fire('Sucesso', 'Área comum salva com sucesso.', 'success');
            modalAreaComum.hide();
            
            const containerId = dto.areCod ? `area-container-${dto.areCod}` : null;
            if (containerId && document.getElementById(containerId)) {
                document.getElementById(containerId).innerHTML = responseHtml;
            } else {
                const newDiv = document.createElement('div');
                newDiv.className = 'col';
                newDiv.innerHTML = responseHtml;
                document.getElementById('listaAreas').prepend(newDiv);
            }
        }
    })
    .catch(error => Swal.fire('Erro', 'Ocorreu um erro ao salvar.', 'error'));
}

function excluirArea(id) {
    Swal.fire({
        title: 'Tem certeza?',
        text: "Esta ação não poderá ser desfeita!",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Sim, excluir!'
    }).then((result) => {
        if (result.isConfirmed) {
            fetch(`/areas-comuns/${id}/excluir`, {
                method: 'POST'
            })
            .then(response => {
                if (response.ok) {
                    document.getElementById(`area-card-${id}`).parentElement.remove();
                    Swal.fire('Excluído!', 'A área foi excluída.', 'success');
                } else {
                    response.json().then(json => Swal.fire('Erro', json.message, 'error'));
                }
            });
        }
    });
}

document.addEventListener('DOMContentLoaded', function() {
    const inputBusca = document.getElementById('inputBuscaAreas');
    if (inputBusca) {
        inputBusca.addEventListener('keyup', function(e) {
            const termo = e.target.value.toLowerCase();
            const cards = document.querySelectorAll('#listaAreas .item-card');
            
            cards.forEach(card => {
                const texto = card.textContent.toLowerCase();
                if (texto.includes(termo)) {
                    card.style.display = '';
                } else {
                    card.style.display = 'none';
                }
            });
        });
    }
});