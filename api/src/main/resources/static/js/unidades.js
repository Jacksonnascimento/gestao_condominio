function openUnidadeForm(url) {
    openForm(url, 'unidadeForm');
}

async function openDetails(unidadeId) {
    modalContent.innerHTML = '<div class="modal-header"><h2>Carregando Detalhes...</h2></div>';
    modal.style.display = 'flex';

    const NA = 'N/A';

    const formatDate = (dateString) => {
        if (!dateString) return null;
        const date = new Date(dateString);
        const day = String(date.getDate()).padStart(2, '0');
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const year = date.getFullYear();
        return `${day}/${month}/${year}`;
    };

    try {
        const [unidadeResponse, ocupantesResponse] = await Promise.all([
            fetch(`/api/unidades/${unidadeId}`),
            fetch(`/api/ocupantes?unidadeId=${unidadeId}`)
        ]);

        if (!unidadeResponse.ok) throw new Error(`Erro ao buscar unidade: ${unidadeResponse.statusText}`);
        if (!ocupantesResponse.ok) throw new Error(`Erro ao buscar ocupantes: ${ocupantesResponse.statusText}`);

        const unidade = await unidadeResponse.json();
        const ocupantes = await ocupantesResponse.json();
        
        let ocupantesHtml = '<p class="no-ocupantes">Nenhum ocupante encontrado para esta unidade.</p>';
        if (ocupantes.length > 0) {
            ocupantesHtml = '<ul>' + ocupantes.map(o => {
                const vinculo = o.vinculo && o.vinculo.descricao ? `(${o.vinculo.descricao})` : '';
                const ocupacaoFim = formatDate(o.fimOcupacao) || 'Atual';
                const ocupacaoInicio = formatDate(o.inicioOcupacao) || NA;

                return `
                    <li>
                        <span class="ocupante-header">${o.nomeCompleto || NA} ${vinculo}</span>
                        <span class="ocupante-details">${o.email || NA} | ${o.telefone || NA}</span>
                        <span class="ocupante-details">Ocupação: ${ocupacaoInicio} até ${ocupacaoFim}</span>
                    </li>
                `}).join('') + '</ul>';
        }

        const fracaoIdealFormatted = unidade.fracaoIdeal ? new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 4 }).format(unidade.fracaoIdeal) + '%' : NA;
        const areaPrivadaFormatted = unidade.areaPrivada ? new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2 }).format(unidade.areaPrivada) + ' m²' : NA;

        const detailsHtml = `
            <div class="modal-header">
                <h2>Detalhes da Unidade ${unidade.uniNumero || ''}</h2>
                <button onclick="closeModal()" class="close-btn">&times;</button>
            </div>
            <div class="details-section">
                <h3>Informações da Unidade</h3>
                <div class="details-grid">
                    <p class="info-item"><strong>Bloco:</strong> <span>${unidade.bloco || NA}</span></p>
                    <p class="info-item"><strong>Andar:</strong> <span>${unidade.andar || NA}</span></p>
                    <p class="info-item"><strong>Fração Ideal:</strong> <span>${fracaoIdealFormatted}</span></p>
                    <p class="info-item"><strong>Área:</strong> <span>${areaPrivadaFormatted}</span></p>
                    <p class="info-item"><strong>Tipo:</strong> <span>${unidade.unidadeTipo ? unidade.unidadeTipo.descricao : NA}</span></p>
                    <p class="info-item"><strong>Status:</strong> <span>${unidade.uniStatusOcupacao ? unidade.uniStatusOcupacao.descricao : NA}</span></p>
                </div>
                <p class="info-item"><strong>Observações:</strong> <span>${unidade.observacao || 'Nenhuma'}</span></p>
            </div>
            <div class="details-section ocupantes-list">
                <h3>Ocupantes</h3>
                ${ocupantesHtml}
            </div>
        `;
        
        modalContent.innerHTML = detailsHtml;

    } catch (error) {
        console.error('Erro ao abrir detalhes:', error);
        modalContent.innerHTML = `
            <div class="modal-header">
                <h2>Erro ao Carregar</h2>
                <button onclick="closeModal()" class="close-btn">&times;</button>
            </div>
            <p>Não foi possível carregar os detalhes da unidade. Por favor, tente novamente.</p>`;
    }
}