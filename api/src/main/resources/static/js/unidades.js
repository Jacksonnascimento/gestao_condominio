document.addEventListener('DOMContentLoaded', function () {
    const formModalElement = document.getElementById('formModal');
    if (!formModalElement) {
        console.error('Elemento do modal #formModal não encontrado.');
        return;
    }
    const formModal = createStaticModal(formModalElement);
    const modalContent = document.getElementById('modalContent');
    const listaUnidades = document.getElementById('lista-unidades');

    const handleFormSubmit = async (event) => {
        event.preventDefault();
        const form = event.target;
        const url = form.action;
        const formData = new FormData(form);

        try {
            const response = await fetch(url, {
                method: 'POST',
                body: new URLSearchParams(formData)
            });

            if (response.ok) {
                const responseHtml = await response.text();
                // Adicionado 'await' aqui para garantir que erros dentro da função sejam capturados corretamente
                await processarSucesso(responseHtml, url);

            } else if (response.status === 409) {
                const errorData = await response.json();
                if (errorData.unidadeId) {
                    Swal.fire({
                        title: 'Registro Localizado',
                        text: errorData.message,
                        icon: 'info',
                        showCancelButton: true,
                        confirmButtonColor: '#3085d6',
                        cancelButtonColor: '#d33',
                        confirmButtonText: 'Restaurar Unidade',
                        cancelButtonText: 'Cancelar'
                    }).then((result) => {
                        if (result.isConfirmed) {
                            reativarUnidade(errorData.unidadeId);
                        }
                    });
                } else {
                    Swal.fire('Erro!', errorData.message || 'Conflito de dados.', 'error');
                }
            } else {
                const errorData = await response.json();
                Swal.fire('Erro!', errorData.message || 'Ocorreu um erro ao salvar a unidade.', 'error');
            }
        } catch (error) {
            console.error('Erro no fetch:', error);
            // Exibe a mensagem real do erro no console para facilitar debug, mas mantém mensagem amigável no alert
            Swal.fire('Erro!', 'Ocorreu um erro de comunicação ou processamento.', 'error');
        }
    };

    const reativarUnidade = async (unidadeId) => {
        try {
            Swal.fire({
                title: 'Restaurando...',
                didOpen: () => { Swal.showLoading(); },
                allowOutsideClick: false
            });

            const response = await fetch(`/unidades/${unidadeId}/reativar`, { method: 'POST' });
            
            if (response.ok) {
                const responseHtml = await response.text();
                // Fecha o loading antes de processar
                if (Swal.isLoading()) { Swal.close(); }
                
                await processarSucesso(responseHtml, ''); // URL vazia trata como novo item na lista

            } else {
                const errorData = await response.json();
                Swal.fire('Erro!', errorData.message || 'Erro ao reativar unidade.', 'error');
            }
        } catch (error) {
            console.error('Erro ao reativar:', error);
            Swal.fire('Erro!', 'Erro de comunicação ao reativar unidade.', 'error');
        }
    };

    const processarSucesso = async (responseHtml, urlOriginal) => {
        formModal.hide();
        
        // Fecha qualquer swal de loading aberto
        if (Swal.isLoading()) { Swal.close(); }

        await Swal.fire({
            title: 'Sucesso!',
            text: 'Operação realizada com sucesso.',
            icon: 'success',
            timer: 2000,
            showConfirmButton: false
        });
        
        const placeholder = document.getElementById('empty-placeholder');
        if (placeholder) {
            placeholder.remove();
        }

        const tempDiv = document.createElement('div');
        tempDiv.innerHTML = responseHtml;
        const newCardElement = tempDiv.firstElementChild;

        // Verificação de segurança: se o HTML retornado estiver vazio ou inválido
        if (!newCardElement) {
            console.warn("HTML retornado pelo servidor parece inválido ou vazio. Recarregando a página.");
            window.location.reload();
            return;
        }

        let isEdit = urlOriginal && urlOriginal.includes("/editar/");

        if (isEdit) {
            const titleElement = newCardElement.querySelector('.clickable-title');
            if (titleElement) {
                const unidadeId = titleElement.dataset.unidadeId;
                const existingCard = listaUnidades ? listaUnidades.querySelector(`.clickable-title[data-unidade-id="${unidadeId}"]`)?.closest('.col') : null;
                
                if (existingCard) {
                    existingCard.replaceWith(newCardElement);
                } else if (listaUnidades) {
                    listaUnidades.prepend(newCardElement);
                } else {
                    window.location.reload();
                    return;
                }
            } else {
                // Se não encontrar o título clicável (estrutura inesperada), recarrega
                window.location.reload();
                return;
            }
        } else {
            if (listaUnidades) {
                listaUnidades.prepend(newCardElement);
            } else {
                // Se a lista não existe (ex: era a primeira unidade e só tinha o placeholder), recarrega para criar a estrutura
                window.location.reload();
                return;
            }
        }
        
        addCardListeners(newCardElement);
    };

    const openFormModal = async (url) => {
        try {
            const response = await fetch(url);
            if (!response.ok) throw new Error('Falha ao carregar o conteúdo do formulário.');

            modalContent.innerHTML = await response.text();
            formModal.show();

            const form = modalContent.querySelector('form');
            if (form) {
                form.addEventListener('submit', handleFormSubmit);
            }
        } catch (error) {
            console.error('Erro ao abrir o modal de formulário:', error);
            Swal.fire('Erro!', 'Não foi possível carregar o formulário.', 'error');
        }
    };

    const openDetailsModal = async (unidadeId) => {
        modalContent.innerHTML = `<div class="modal-body"><p class="text-center">Carregando...</p></div>`;
        formModal.show();

        try {
            const [unidadeResponse, ocupantesResponse] = await Promise.all([
                fetch(`/api/unidades/${unidadeId}`),
                fetch(`/api/ocupantes?unidadeId=${unidadeId}`)
            ]);

            if (!unidadeResponse.ok) throw new Error('Falha ao carregar dados da unidade.');
            if (!ocupantesResponse.ok) throw new Error('Falha ao carregar dados dos ocupantes.');

            const unidade = await unidadeResponse.json();
            const ocupantes = await ocupantesResponse.json();

            let ocupantesHtml = '<p class="text-muted">Nenhum ocupante encontrado.</p>';
            if (ocupantes.length > 0) {
                ocupantesHtml = '<ul class="list-group">' + ocupantes.map(o => `
                    <li class="list-group-item">
                        <div class="d-flex w-100 justify-content-between">
                            <h6 class="mb-1">${o.nomeCompleto || 'N/A'}</h6>
                            <small>${o.vinculo ? o.vinculo.descricao : ''}</small>
                        </div>
                        <p class="mb-1">${o.email || 'N/A'} | ${o.telefone || 'N/A'}</p>
                        <small>Ocupação: ${new Date(o.inicioOcupacao).toLocaleDateString()} até ${o.fimOcupacao ? new Date(o.fimOcupacao).toLocaleDateString() : 'Atual'}</small>
                    </li>
                `).join('') + '</ul>';
            }

            const fracaoIdeal = unidade.fracaoIdeal ? `${parseFloat(unidade.fracaoIdeal).toLocaleString('pt-BR', {minimumFractionDigits: 2, maximumFractionDigits: 4})}%` : 'N/A';
            const areaPrivada = unidade.areaPrivada ? `${parseFloat(unidade.areaPrivada).toLocaleString('pt-BR', {minimumFractionDigits: 2, maximumFractionDigits: 2})} m²` : 'N/A';
            const statusOcupacao = unidade.uniStatusOcupacao ? (unidade.uniStatusOcupacao.descricao || unidade.uniStatusOcupacao) : 'N/A';
            const tipoUnidade = unidade.unidadeTipo ? (unidade.unidadeTipo.descricao || unidade.unidadeTipo) : 'N/A';

            const detailsHtml = `
                <div class="modal-header">
                    <h5 class="modal-title">Detalhes da Unidade ${unidade.uniNumero || ''}</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <div class="details-section mb-4">
                        <h6 class="section-title">Informações da Unidade</h6>
                        <div class="row g-3">
                            <div class="col-md-6 info-item"><strong>Bloco:</strong> <span>${unidade.bloco || 'N/A'}</span></div>
                            <div class="col-md-6 info-item"><strong>Andar:</strong> <span>${unidade.andar || 'N/A'}</span></div>
                            <div class="col-md-6 info-item"><strong>Fração Ideal:</strong> <span>${fracaoIdeal}</span></div>
                            <div class="col-md-6 info-item"><strong>Área:</strong> <span>${areaPrivada}</span></div>
                            <div class="col-md-6 info-item"><strong>Tipo:</strong> <span>${tipoUnidade}</span></div>
                            <div class="col-md-6 info-item"><strong>Status:</strong> <span>${statusOcupacao}</span></div>
                            <div class="col-12 info-item"><strong>Observações:</strong> <span>${unidade.observacao || 'Nenhuma'}</span></div>
                        </div>
                    </div>
                    <div class="details-section">
                        <h6 class="section-title">Ocupantes</h6>
                        ${ocupantesHtml}
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Fechar</button>
                </div>
            `;

            modalContent.innerHTML = detailsHtml;

        } catch (error) {
            console.error('Erro ao carregar detalhes:', error);
            modalContent.innerHTML = `
                <div class="modal-header">
                    <h5 class="modal-title">Erro</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <p>Não foi possível carregar os detalhes da unidade. Por favor, tente novamente.</p>
                </div>
            `;
        }
    };

    const handleExcluirClick = async (event) => {
        const btn = event.currentTarget;
        const url = btn.dataset.url;

        const result = await Swal.fire({
            title: 'Tem certeza que deseja excluir esta Unidade?',
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
                // Exibe loading enquanto processa
                Swal.fire({
                    title: 'Excluindo...',
                    didOpen: () => { Swal.showLoading(); },
                    allowOutsideClick: false
                });

                const response = await fetch(url, { method: 'POST' });

                if (response.ok) {
                    // Remove o card da interface sem recarregar a página
                    btn.closest('.col').remove();
                    
                    Swal.fire({
                        title: 'Excluído!',
                        text: 'A unidade foi excluída com sucesso.',
                        icon: 'success',
                        timer: 2000,
                        showConfirmButton: false
                    });
                } else {
                    let errorMessage = 'Não foi possível excluir a unidade.';
                    try {
                        const errorData = await response.json();
                        if(errorData.message) errorMessage = errorData.message;
                    } catch(e) {
                        // Se não for JSON, pode ser HTML de erro ou texto simples
                    }
                    
                    Swal.fire('Erro!', errorMessage, 'error');
                }
            } catch (error) {
                console.error('Erro ao excluir:', error);
                Swal.fire('Erro de Conexão!', 'Não foi possível conectar ao servidor.', 'error');
            }
        }
    };
    
    function addCardListeners(cardElement) {
        cardElement.querySelector('.btn-edit')?.addEventListener('click', (e) => {
            const url = e.currentTarget.dataset.url;
            openFormModal(url);
        });

        cardElement.querySelector('.clickable-title')?.addEventListener('click', (e) => {
            const unidadeId = e.currentTarget.dataset.unidadeId;
            openDetailsModal(unidadeId);
        });

        cardElement.querySelector('.btn-excluir')?.addEventListener('click', handleExcluirClick);
    }

    document.getElementById('btnNovaUnidade')?.addEventListener('click', () => {
        openFormModal('/unidades/novo');
    });

    document.querySelectorAll('.col').forEach(cardElement => {
        addCardListeners(cardElement);
    });
});