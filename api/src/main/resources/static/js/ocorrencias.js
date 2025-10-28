document.addEventListener('DOMContentLoaded', () => {

    const mainModalElement = document.getElementById('formModal');
    if (!mainModalElement) {
        console.error('Modal principal #formModal não encontrado.');
        return;
    }
    const mainModalContent = document.getElementById('modalContent');
    const mainModal = createStaticModal(mainModalElement);

    let finalizarModalInstance = null;

    const showLoadingFeedback = (title = 'Processando...') => {
        Swal.fire({
            title: title,
            didOpen: () => { Swal.showLoading(); },
            allowOutsideClick: false, allowEscapeKey: false, allowEnterKey: false
        });
    };

    const showSuccessFeedback = (message = 'Operação realizada com sucesso.', reload = true) => {
        Swal.fire({
            icon: 'success', title: 'Sucesso!', text: message, timer: 2000, showConfirmButton: false
        }).then(() => { if (reload) window.location.reload(); });
    };

    const showErrorFeedback = (message = 'Ocorreu um erro.') => {
        if (Swal.isLoading()) { Swal.close(); }
        Swal.fire({ icon: 'error', title: 'Erro!', text: message });
    };

    const fetchAndInjectModalContent = async (url, modalInstance, contentElement) => {
        contentElement.innerHTML = '<div class="modal-body text-center"><span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Carregando...</div>';
        modalInstance.show();
        try {
            const response = await fetch(url);
            if (!response.ok) {
                let errorBody = '';
                try { errorBody = await response.text(); } catch (e) { }
                throw new Error(`Falha ao carregar conteúdo: ${response.statusText}. ${errorBody}`);
            }
            contentElement.innerHTML = await response.text();
            document.dispatchEvent(new CustomEvent('modalContentLoaded'));
            return true;
        } catch (error) {
            console.error('Erro ao buscar/injetar conteúdo do modal:', error);
            contentElement.innerHTML = `<div class="modal-body"><p class="text-danger">Erro ao carregar o conteúdo. Tente novamente.</p></div>`;
            return false;
        }
    };

    const handleAjaxFormSubmit = async (form, successCallback, errorCallback) => {
        showLoadingFeedback();
        const url = form.action;
        const method = form.method;
        const formData = new FormData(form);

        try {
            const response = await fetch(url, { method: method.toUpperCase(), body: formData });

            let responseData = {};
            let responseText = '';
            try {
                responseText = await response.text();
                // Tenta parsear como JSON, mas se falhar, usa o texto como mensagem
                try {
                    responseData = JSON.parse(responseText);
                } catch (parseError) {
                    responseData = { message: responseText || 'Resposta inválida do servidor.' };
                }
            } catch (e) {
                // Caso a leitura do texto falhe
                responseData = { message: 'Não foi possível ler a resposta do servidor.' };
            }

            if (response.ok) {
                if (Swal.isLoading()) { Swal.close(); }
                if (successCallback) successCallback(responseData);
            } else {
                const message = responseData.message || responseData.detail || 'Erro ao processar a solicitação.';
                 // Extrai a mensagem de erro específica do backend se disponível
                let extractedMessage = message;
                if (typeof message === 'string') {
                    const match = message.match(/"([^"]*)"/); // Tenta pegar a mensagem dentro de aspas
                    if (match && match[1]) {
                        extractedMessage = match[1];
                    }
                }
                showErrorFeedback(extractedMessage);
                if (errorCallback) errorCallback(responseData);
            }
        } catch (error) {
            console.error(`Erro no submit AJAX para ${url}:`, error);
            showErrorFeedback('Erro de comunicação com o servidor.');
            if (errorCallback) errorCallback(error);
        }
    };


    const carregarUnidadesPorCondominio = async (condominioId, unidadeSelectElement) => {
        if (!unidadeSelectElement) return;
        unidadeSelectElement.innerHTML = '<option value="">Carregando...</option>';
        unidadeSelectElement.disabled = true;

        if (!condominioId) {
            unidadeSelectElement.innerHTML = '<option value="">Selecione um condomínio</option>';
            unidadeSelectElement.disabled = false;
            return;
        }

        try {
            const response = await fetch(`/api/unidades/por-condominio/${condominioId}`);
            if (!response.ok) throw new Error('Falha ao buscar unidades.');
            const unidades = await response.json();

            unidadeSelectElement.innerHTML = '<option value="">Selecione...</option>';
            unidades.forEach(unidade => {
                const optionText = `${unidade.uniNumero}${unidade.bloco ? ' - ' + unidade.bloco : ''}`;
                unidadeSelectElement.innerHTML += `<option value="${unidade.uniCod}">${optionText}</option>`;
            });
            unidadeSelectElement.disabled = false;
        } catch (error) {
            console.error('Erro ao carregar unidades:', error);
            unidadeSelectElement.innerHTML = '<option value="">Erro ao carregar</option>';
            unidadeSelectElement.disabled = true;
        }
    };

    document.getElementById('btnNovaOcorrencia')?.addEventListener('click', async () => {
        const success = await fetchAndInjectModalContent('/ocorrencias/novo', mainModal, mainModalContent);
        if (success) {
            initializeNovaOcorrenciaListeners();
        }
    });

    document.querySelectorAll('.btn-ver-detalhes').forEach(button => {
        button.addEventListener('click', async (event) => {
            const ocorrenciaId = event.currentTarget.dataset.ocorrenciaId;
            if (!ocorrenciaId) return;
            const success = await fetchAndInjectModalContent(`/ocorrencias/detalhes/${ocorrenciaId}`, mainModal, mainModalContent);
            if (success) {
                initializeDetalhesModalListeners(ocorrenciaId);
            }
        });
    });

    const handleNovaOcorrenciaSubmit = async (event) => {
        event.preventDefault();
        const form = event.target;
        await handleAjaxFormSubmit(form, (responseData) => {
            mainModal.hide();
            showSuccessFeedback('Ocorrência registrada com sucesso!');
        });
    };

    const handleComentarioSubmit = async (event) => {
        event.preventDefault();
        const form = event.target;
        const input = form.querySelector('input[name="comentario"]');
        if (!input || !input.value.trim()) return;

        await handleAjaxFormSubmit(form, (responseData) => {
            input.value = '';
            addComentarioToList(responseData);
            // Não fecha o modal nem recarrega a página
        });
    };

    const handleAnexoSubmit = async (event) => {
        event.preventDefault();
        const form = event.target;
        const fileInput = form.querySelector('input[type="file"]');
        if (!fileInput || fileInput.files.length === 0) return;

        await handleAjaxFormSubmit(form, (responseData) => {
            fileInput.value = ''; // Limpa o input de arquivo
            addAnexoToList(responseData);
             // Não fecha o modal nem recarrega a página
        });
    };

    const handleFinalizarSubmit = async (event) => {
        event.preventDefault();
        const form = event.target;
        await handleAjaxFormSubmit(form, (responseData) => {
            if (finalizarModalInstance) finalizarModalInstance.hide();
            mainModal.hide();
            showSuccessFeedback(responseData.message || 'Ocorrência finalizada com sucesso!'); // Recarrega a página por padrão
        });
    };

    const addComentarioToList = (comentarioDTO) => {
        const listElement = document.getElementById('comentariosList');
        if (!listElement) return;

        const emptyMsg = listElement.querySelector('.text-muted');
        if (emptyMsg) emptyMsg.remove();

        const newItem = document.createElement('div');
        newItem.className = 'comentario-item';
        newItem.innerHTML = `
            <p class="mb-1 preserve-line-breaks">${escapeHtml(comentarioDTO.comentario)}</p>
            <small class="comentario-meta">
                Por <strong>${escapeHtml(comentarioDTO.nomeUsuario)}</strong>
                em ${formatDateTime(comentarioDTO.dataComentario)}
            </small>
        `;
        listElement.insertBefore(newItem, listElement.firstChild); // Insere no início
    };

    const addAnexoToList = (anexoDTO) => {
        const listElement = document.getElementById('anexosList');
        if (!listElement) return;

        const emptyMsg = listElement.querySelector('.text-muted');
        if (emptyMsg) emptyMsg.remove();

        const newItem = document.createElement('div');
        newItem.className = 'anexo-item d-flex justify-content-between align-items-center';
        newItem.dataset.anexoId = anexoDTO.id; // Adiciona ID para facilitar remoção
        newItem.innerHTML = `
            <div>
                 <a href="/ocorrencias/${anexoDTO.ocorrenciaId}/anexo/${anexoDTO.id}" target="_blank" class="text-decoration-none anexo-link">${escapeHtml(anexoDTO.nomeOriginal || 'anexo')}</a>
                 <small class="anexo-meta d-block">
                     Por <strong>${escapeHtml(anexoDTO.nomeUsuario)}</strong>
                     em ${formatDateTime(anexoDTO.dataAnexo)}
                     (${formatBytes(anexoDTO.tamanhoArquivo)})
                 </small>
             </div>
             <div class="anexo-actions">
                 <button class="btn btn-sm btn-outline-danger btn-excluir-anexo"
                         data-ocorrencia-id="${anexoDTO.ocorrenciaId}"
                         data-anexo-id="${anexoDTO.id}"
                         type="button">
                     <i class='bx bxs-trash'></i>
                 </button>
             </div>
        `;
        listElement.insertBefore(newItem, listElement.firstChild); // Insere no início

        // Adiciona listener ao novo botão de excluir
        const deleteButton = newItem.querySelector('.btn-excluir-anexo');
        deleteButton?.addEventListener('click', handleExcluirAnexoClick);
    };

    const handleExcluirAnexoClick = (event) => {
        const button = event.currentTarget;
        const ocorrenciaId = button.dataset.ocorrenciaId;
        const anexoId = button.dataset.anexoId;

        Swal.fire({
            title: 'Confirmar Exclusão', text: "Deseja realmente excluir este anexo?", icon: 'warning',
            showCancelButton: true, confirmButtonColor: '#d33', cancelButtonText: 'Cancelar', confirmButtonText: 'Sim, Excluir!'
        }).then(async (result) => {
            if (result.isConfirmed) {
                showLoadingFeedback('Excluindo...');
                try {
                    const response = await fetch(`/ocorrencias/${ocorrenciaId}/anexo/${anexoId}/excluir`, { method: 'POST' });

                    let responseData = {};
                    let responseText = '';
                    try {
                        // Tenta ler como texto primeiro
                        responseText = await response.text();
                        // Tenta parsear como JSON
                        try {
                           responseData = JSON.parse(responseText);
                        } catch (parseError) {
                            // Se falhar o parse, usa o texto como mensagem (caso de respostas não-JSON)
                           responseData = { message: responseText || 'Resposta inválida do servidor.' };
                        }
                    } catch (readError) {
                        responseData = { message: 'Não foi possível ler a resposta do servidor.' };
                    }


                    if (response.ok) {
                        const itemToRemove = document.querySelector(`.anexo-item[data-anexo-id="${anexoId}"]`);
                        itemToRemove?.remove();

                        // Verifica se a lista ficou vazia após remover
                        const listElement = document.getElementById('anexosList');
                        if (listElement && !listElement.querySelector('.anexo-item')) {
                            listElement.innerHTML = '<div class="text-muted text-center p-3">Nenhum anexo adicionado.</div>';
                        }
                         if (Swal.isLoading()) { Swal.close(); } // Fecha o loading
                         // Não mostra alerta de sucesso para exclusão, apenas remove o item
                    } else {
                        const message = responseData.message || 'Erro ao excluir anexo.';
                         // Extrai a mensagem de erro específica do backend se disponível
                        let extractedMessage = message;
                        if (typeof message === 'string') {
                            const match = message.match(/"([^"]*)"/);
                            if (match && match[1]) {
                                extractedMessage = match[1];
                            }
                        }
                        showErrorFeedback(extractedMessage);
                    }
                } catch (error) {
                    console.error('Erro ao excluir anexo:', error);
                    showErrorFeedback('Erro de comunicação ao excluir anexo.');
                }
            }
        });
    };

    const handleAbrirModalFinalizarClick = (event) => {
        const finalizarModalElement = document.getElementById('finalizarOcorrenciaModal');
        if (finalizarModalElement) {
            if (!finalizarModalInstance) {
                finalizarModalInstance = createStaticModal(finalizarModalElement); // Usa a função global
                const formFinalizar = finalizarModalElement.querySelector('#finalizarForm');
                formFinalizar?.addEventListener('submit', handleFinalizarSubmit);
            }
            finalizarModalInstance.show();
        }
    };

    // --- CORREÇÃO FINAL ---
    const handleAnexoLinkClick = async (event) => {
        const link = event.target.closest('a.anexo-link');
        if (!link) return;

        event.preventDefault();
        const url = link.href;
        showLoadingFeedback('Verificando arquivo...');

        try {
            // Usa 'HEAD' para verificar a existência sem baixar o arquivo
            // Se o servidor não suportar HEAD para esses links, volte para 'GET'
            const response = await fetch(url, { method: 'HEAD' });

            if (response.ok) {
                // Se HEAD funcionou e deu OK, o arquivo existe
                if (Swal.isLoading()) { Swal.close(); }
                window.open(url, '_blank');
            } else if (response.status === 404) {
                 // Se o status for 404, o arquivo não foi encontrado
                 showErrorFeedback("Arquivo não encontrado. Pode ter sido excluído.");
            } else {
                // Outro erro inesperado ao verificar o arquivo
                // Tenta obter alguma informação do status/texto para o erro
                 let errorMsg = `Erro ${response.status} ao verificar o arquivo.`;
                 try {
                     const errorText = await response.text();
                     if (errorText) {
                         // Tenta extrair mensagem mais específica se houver
                         const jsonMatch = errorText.match(/"detail":"([^"]*)"/i) || errorText.match(/"message":"([^"]*)"/i);
                          if (jsonMatch && jsonMatch[1]) {
                             const detailMsgMatch = jsonMatch[1].match(/"([^"]*)"/); // Tenta pegar a msg dentro da msg
                             errorMsg = (detailMsgMatch && detailMsgMatch[1]) ? detailMsgMatch[1] : jsonMatch[1];
                         } else {
                             const titleMatch = errorText.match(/<title>(.*?)<\/title>/i); // Para páginas HTML de erro
                             errorMsg = (titleMatch && titleMatch[1]) ? titleMatch[1] : `Erro ${response.status}: ${response.statusText}`;
                         }
                     }
                 } catch (e) {
                     // Ignora erro ao ler corpo, usa mensagem baseada no status
                     errorMsg = `Erro ${response.status}: ${response.statusText} ao verificar o arquivo.`;
                 }
                 showErrorFeedback(errorMsg);
            }
        } catch (error) {
            // Erro de rede/conexão
            console.error('Erro de rede ao verificar/acessar anexo:', error);
            showErrorFeedback('Erro de comunicação ao tentar acessar o anexo.');
        }
    };
    // --- FIM DA CORREÇÃO FINAL ---

    const initializeNovaOcorrenciaListeners = () => {
        const form = mainModalContent.querySelector('#ocorrenciaForm');
        form?.addEventListener('submit', handleNovaOcorrenciaSubmit);

        const condominioSelect = mainModalContent.querySelector('#condominioId');
        const unidadeSelect = mainModalContent.querySelector('#unidadeId');

        // Adiciona listener para carregar unidades quando o condomínio muda
        if (condominioSelect && unidadeSelect) {
            condominioSelect.addEventListener('change', (event) => {
                carregarUnidadesPorCondominio(event.target.value, unidadeSelect);
            });
            // Carrega unidades iniciais se um condomínio já estiver selecionado (ex: admin não global)
            if (condominioSelect.value) {
                carregarUnidadesPorCondominio(condominioSelect.value, unidadeSelect);
            }
        }
    };

    const initializeDetalhesModalListeners = (ocorrenciaId) => {
        // Adiciona listeners para os formulários de comentário e anexo
        const comentarioForm = mainModalContent.querySelector('#comentarioForm');
        comentarioForm?.addEventListener('submit', handleComentarioSubmit);

        const anexoForm = mainModalContent.querySelector('#anexoForm');
        anexoForm?.addEventListener('submit', handleAnexoSubmit);

        // (Re)Adiciona listeners aos botões de excluir anexo (importante para itens adicionados dinamicamente)
        mainModalContent.querySelectorAll('.btn-excluir-anexo').forEach(button => {
            button.removeEventListener('click', handleExcluirAnexoClick); // Remove listener antigo para evitar duplicação
            button.addEventListener('click', handleExcluirAnexoClick);
        });

        // Adiciona listener ao botão de abrir modal de finalização
        const btnAbrirFinalizar = mainModalContent.querySelector('#btnAbrirModalFinalizar');
        btnAbrirFinalizar?.addEventListener('click', handleAbrirModalFinalizarClick);

        // Adiciona listener à lista de anexos para tratar cliques nos links (delegação de evento)
        const anexosList = mainModalContent.querySelector('#anexosList');
        if (anexosList) {
            anexosList.removeEventListener('click', handleAnexoLinkClick); // Garante que não haja listeners duplicados
            anexosList.addEventListener('click', handleAnexoLinkClick);
        }

        // Limpeza ao fechar o modal principal (evita problemas com modal aninhado)
        mainModalElement.addEventListener('hidden.bs.modal', () => {
            if (finalizarModalInstance) {
                // Remove listener do formulário de finalização
                const formFinalizar = document.getElementById('finalizarForm');
                formFinalizar?.removeEventListener('submit', handleFinalizarSubmit);
                finalizarModalInstance.dispose(); // Destroi a instância do modal de finalização
                finalizarModalInstance = null;
                 // Remove manualmente o backdrop se ainda existir (bug do bootstrap com modais aninhados)
                 const backdrops = document.querySelectorAll('.modal-backdrop');
                 backdrops.forEach(bd => bd.remove());
                 // Garante que o body volte ao normal
                 document.body.classList.remove('modal-open');
                 document.body.style.overflow = '';
                 document.body.style.paddingRight = '';
            }
        }, { once: true }); // Executa o listener apenas uma vez
    };

    // Função auxiliar para escapar HTML (evita XSS)
    const escapeHtml = (unsafe) => {
        if (!unsafe) return '';
        return unsafe
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    // Função auxiliar para formatar data/hora
    const formatDateTime = (dateTimeString) => {
        if (!dateTimeString) return '';
        try {
            const date = new Date(dateTimeString);
            // Verifica se a data é válida
            if (isNaN(date.getTime())) {
                throw new Error("Data inválida");
            }
            return date.toLocaleDateString('pt-BR') + ' ' + date.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
        } catch (e) {
            console.error("Erro ao formatar data:", dateTimeString, e);
            return dateTimeString; // Retorna a string original em caso de erro
        }
    }

    // Função auxiliar para formatar bytes
     const formatBytes = (bytes, decimals = 1) => {
        if (!+bytes || bytes === 0) return '0 Bytes' // Trata 0 ou não numérico
        const k = 1024
        const dm = decimals < 0 ? 0 : decimals
        const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'] // Adicionado 'Bytes'

        // Calcula o índice correto (logaritmo na base k)
        const i = (bytes === 0) ? 0 : Math.floor(Math.log(bytes) / Math.log(k));

        // Validação adicional para o índice
        if (isNaN(i) || i < 0) return '0 Bytes'; // Retorna '0 Bytes' se o cálculo falhar

        // Formata o número e adiciona a unidade
        return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`
    }


    // Listener para inicializar listeners específicos quando o conteúdo do modal é carregado
    document.addEventListener('modalContentLoaded', () => {
        // Verifica se é o formulário de nova ocorrência
        if (mainModalContent.querySelector('#ocorrenciaForm')) {
            initializeNovaOcorrenciaListeners();
        }
        // Verifica se é o modal de detalhes (pela presença de um elemento específico)
        if (mainModalContent.querySelector('.ocorrencia-detalhes-modal')) {
             // O ID não é estritamente necessário aqui se os listeners internos já pegam via data-attributes
            initializeDetalhesModalListeners(null);
        }
    });

});