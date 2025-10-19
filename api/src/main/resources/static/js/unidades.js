document.addEventListener('DOMContentLoaded', function () {
    const formModalElement = document.getElementById('formModal');
    if (!formModalElement) {
        console.error('Elemento do modal #formModal não encontrado.');
        return;
    }
    // Utiliza a função global para criar o modal com o comportamento estático
    const formModal = createStaticModal(formModalElement);
    const modalContent = document.getElementById('modalContent');

    // Função para tratar o submit do formulário de NOVO/EDIÇÃO
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
                formModal.hide();
                await Swal.fire({
                    title: 'Sucesso!',
                    text: 'Operação realizada com sucesso.',
                    icon: 'success',
                    timer: 2000,
                    showConfirmButton: false
                });
                window.location.reload();
            } else {
                const errorData = await response.json();
                Swal.fire('Erro!', errorData.message || 'Ocorreu um erro ao salvar a unidade.', 'error');
            }
        } catch (error) {
            console.error('Erro no fetch:', error);
            Swal.fire('Erro!', 'Ocorreu um erro de comunicação.', 'error');
        }
    };

    // Função genérica para abrir o modal de formulário
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

    // Lógica para abrir o modal de DETALHES, consumindo a API
    const openDetailsModal = async (unidadeId) => {
        modalContent.innerHTML = `<div class="modal-body"><p class="text-center">Carregando...</p></div>`;
        formModal.show();

        try {
            // Busca dados da unidade e dos ocupantes em paralelo
            const [unidadeResponse, ocupantesResponse] = await Promise.all([
                fetch(`/api/unidades/${unidadeId}`),
                fetch(`/api/ocupantes?unidadeId=${unidadeId}`)
            ]);

            if (!unidadeResponse.ok) throw new Error('Falha ao carregar dados da unidade.');
            if (!ocupantesResponse.ok) throw new Error('Falha ao carregar dados dos ocupantes.');

            const unidade = await unidadeResponse.json();
            const ocupantes = await ocupantesResponse.json();

            // Monta o HTML dos ocupantes
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

            // Formata os dados da unidade
            const fracaoIdeal = unidade.fracaoIdeal ? `${unidade.fracaoIdeal.toLocaleString('pt-BR', {minimumFractionDigits: 4})}%` : 'N/A';
            const areaPrivada = unidade.areaPrivada ? `${unidade.areaPrivada.toLocaleString('pt-BR', {minimumFractionDigits: 2})} m²` : 'N/A';

            // Monta o HTML final do modal
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
                            <div class="col-md-6 info-item"><strong>Tipo:</strong> <span>${unidade.unidadeTipo ? unidade.unidadeTipo.descricao : 'N/A'}</span></div>
                            <div class="col-md-6 info-item"><strong>Status:</strong> <span>${unidade.unidadeStatusOcupacao ? unidade.uniStatusOcupacao.descricao : 'N/A'}</span></div>
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

    // Adiciona os event listeners aos botões
    document.getElementById('btnNovaUnidade')?.addEventListener('click', () => {
        openFormModal('/unidades/novo');
    });

    document.querySelectorAll('.btn-edit').forEach(button => {
        button.addEventListener('click', () => {
            const url = button.dataset.url;
            openFormModal(url);
        });
    });

    document.querySelectorAll('.clickable-title').forEach(title => {
        title.addEventListener('click', () => {
            const unidadeId = title.dataset.unidadeId;
            openDetailsModal(unidadeId);
        });
    });
});