let modalReserva;
let limiteConvidadosAtual = null;

document.addEventListener('DOMContentLoaded', function() {
    const modalEl = document.getElementById('modalReserva');
    if (modalEl) {
        modalReserva = createStaticModal(modalEl);
    }
});

function abrirModalNovaReserva() {
    const form = document.getElementById('formReserva');
    form.reset();
    document.getElementById('convidadosContainer').innerHTML = '';
    document.getElementById('secaoConvidados').style.display = 'none';
    document.getElementById('secaoTermos').style.display = 'none';
    document.getElementById('taxaContainer').style.display = 'none';
    
    const uniSelect = document.getElementById('uniCod');
    if(uniSelect && uniSelect.options.length === 2) {
        uniSelect.selectedIndex = 1;
    }
    
    modalReserva.show();
}

function carregarDetalhesArea(select) {
    const option = select.options[select.selectedIndex];
    if (!option.value) {
        document.getElementById('secaoConvidados').style.display = 'none';
        document.getElementById('secaoTermos').style.display = 'none';
        document.getElementById('taxaContainer').style.display = 'none';
        return;
    }

    const permite = option.getAttribute('data-permite') === 'true';
    const limite = option.getAttribute('data-limite');
    const termos = option.getAttribute('data-termos');
    const taxa = option.getAttribute('data-taxa');

    limiteConvidadosAtual = limite ? parseInt(limite) : null;

    if (permite) {
        document.getElementById('secaoConvidados').style.display = 'block';
        document.getElementById('limiteTexto').innerText = limite ? `(Máximo: ${limite})` : '(Ilimitado)';
    } else {
        document.getElementById('secaoConvidados').style.display = 'none';
        document.getElementById('convidadosContainer').innerHTML = '';
    }

    if (termos && termos.trim() !== '') {
        document.getElementById('secaoTermos').style.display = 'block';
        document.getElementById('textoTermos').innerText = termos;
    } else {
        document.getElementById('secaoTermos').style.display = 'none';
        document.getElementById('termosAceitos').checked = true;
    }

    // Exibição da Taxa de Uso
    const taxaContainer = document.getElementById('taxaContainer');
    if (taxa && taxa !== 'null' && parseFloat(taxa) > 0) {
        taxaContainer.style.display = 'block';
        document.getElementById('taxaTexto').innerText = `Custo da Reserva: R$ ${parseFloat(taxa).toFixed(2).replace('.', ',')}`;
    } else {
        taxaContainer.style.display = 'none';
    }
}

function adicionarConvidado() {
    const container = document.getElementById('convidadosContainer');
    if (limiteConvidadosAtual !== null && container.children.length >= limiteConvidadosAtual) {
        Swal.fire('Atenção', 'Limite de convidados atingido.', 'warning');
        return;
    }

    const html = `
        <div class="row g-2 align-items-end mb-2 convidado-item">
            <div class="col-md-6">
                <input type="text" class="form-control form-control-sm conv-nome" placeholder="Nome Completo *" required>
            </div>
            <div class="col-md-4">
                <input type="text" class="form-control form-control-sm conv-doc" placeholder="RG ou CPF">
            </div>
            <div class="col-md-2">
                <button type="button" class="btn btn-sm btn-outline-danger w-100" onclick="this.closest('.convidado-item').remove()">Remover</button>
            </div>
        </div>
    `;
    container.insertAdjacentHTML('beforeend', html);
}

function salvarReserva() {
    const form = document.getElementById('formReserva');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }

    const uniCodEl = document.getElementById('uniCod');
    if(!uniCodEl || !uniCodEl.value) {
         Swal.fire('Atenção', 'Você precisa de uma unidade vinculada para fazer a reserva.', 'warning');
         return;
    }

    if (!document.getElementById('termosAceitos').checked) {
        Swal.fire('Atenção', 'Você deve aceitar os termos de uso.', 'warning');
        return;
    }

    const dto = {
        areCod: document.getElementById('areCodSelect').value,
        uniCod: uniCodEl.value,
        data: document.getElementById('dataReserva').value,
        turCod: document.getElementById('turCodSelect').value || null,
        termosAceitos: true,
        convidados: []
    };

    const convItems = document.querySelectorAll('.convidado-item');
    convItems.forEach(item => {
        dto.convidados.push({
            nome: item.querySelector('.conv-nome').value,
            documento: item.querySelector('.conv-doc').value || null
        });
    });

    fetch('/reservas/solicitar', {
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
            Swal.fire('Sucesso', 'Reserva solicitada com sucesso.', 'success');
            modalReserva.hide();
            
            const newDiv = document.createElement('div');
            newDiv.className = 'col';
            newDiv.innerHTML = responseHtml;
            document.getElementById('listaReservas').prepend(newDiv);
        }
    })
    .catch(error => Swal.fire('Erro', 'Ocorreu um erro na requisição.', 'error'));
}

function cancelarReserva(id) {
    Swal.fire({
        title: 'Cancelar Reserva?',
        text: "Deseja realmente cancelar esta solicitação?",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: 'Sim, cancelar'
    }).then((result) => {
        if (result.isConfirmed) {
            fetch(`/reservas/${id}/cancelar`, { method: 'POST' })
            .then(res => res.text())
            .then(html => atualizarCard(id, html));
        }
    });
}

function aprovarReserva(id) {
    fetch(`/reservas/${id}/aprovar`, { method: 'POST' })
    .then(res => res.text())
    .then(html => atualizarCard(id, html));
}

function rejeitarReserva(id) {
    Swal.fire({
        title: 'Rejeitar Reserva',
        input: 'text',
        inputLabel: 'Motivo da rejeição',
        inputRequired: true,
        showCancelButton: true
    }).then((result) => {
        if (result.isConfirmed) {
            fetch(`/reservas/${id}/rejeitar?motivo=${encodeURIComponent(result.value)}`, { 
                method: 'POST'
            })
            .then(res => res.text())
            .then(html => atualizarCard(id, html));
        }
    });
}

function atualizarCard(id, responseHtml) {
    try {
        const json = JSON.parse(responseHtml);
        Swal.fire('Erro', json.message, 'error');
    } catch (e) {
        document.getElementById(`reserva-card-${id}`).parentElement.innerHTML = responseHtml;
        Swal.fire('Sucesso', 'Status atualizado.', 'success');
    }
}

document.addEventListener('DOMContentLoaded', function() {
    const inputBusca = document.getElementById('inputBuscaReservas');
    if (inputBusca) {
        inputBusca.addEventListener('keyup', function(e) {
            const termo = e.target.value.toLowerCase();
            const cards = document.querySelectorAll('#listaReservas .item-card');
            
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