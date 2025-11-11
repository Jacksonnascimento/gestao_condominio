document.addEventListener('DOMContentLoaded', () => {

    const mainModalElement = document.getElementById('formModal');
    if (!mainModalElement) {
        console.error('Modal principal #formModal não encontrado.');
        return;
    }
    const mainModalContent = document.getElementById('modalContent');
    const mainModal = createStaticModal(mainModalElement);
    
    // Adicionado para o novo card
    const listaOcorrencias = document.getElementById('lista-ocorrencias');

    let finalizarModalInstance = null;

    const showLoadingFeedback = (title = 'Processando...') => {
        Swal.fire({
            title: title,
            didOpen: () => { Swal.showLoading(); },
            allowOutsideClick: false, allowEscapeKey: false, allowEnterKey: false
        });
    };

    // MUDANÇA: reload = false por padrão
    const showSuccessFeedback = (message = 'Operação realizada com sucesso.', reload = false) => {
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

    // Esta função genérica continua sendo usada pelos formulários de *detalhes*
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
                try {
                    responseData = JSON.parse(responseText);
                } catch (parseError) {
                    responseData = { message: responseText || 'Resposta inválida do servidor.' };
                }
            } catch (e) {
                responseData = { message: 'Não foi possível ler a resposta do servidor.' };
            }

            if (response.ok) {
                if (Swal.isLoading()) { Swal.close(); }
                if (successCallback) successCallback(responseData);
            } else {
                const message = responseData.message || responseData.detail || 'Erro ao processar a solicitação.';
                let extractedMessage = message;
                if (typeof message === 'string') {
                    const match = message.match(/"([^"]*)"/);
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

    // Listener para o botão 'Nova Ocorrência'
    document.getElementById('btnNovaOcorrencia')?.addEventListener('click', async () => {
        const success = await fetchAndInjectModalContent('/ocorrencias/novo', mainModal, mainModalContent);
        if (success) {
            initializeNovaOcorrenciaListeners();
        }
    });

    // Função helper para adicionar listener ao novo card
    const addCardListeners = (cardElement) => {
         cardElement.querySelector('.btn-ver-detalhes')?.addEventListener('click', async (event) => {
            const ocorrenciaId = event.currentTarget.dataset.ocorrenciaId;
            if (!ocorrenciaId) return;
            const success = await fetchAndInjectModalContent(`/ocorrencias/detalhes/${ocorrenciaId}`, mainModal, mainModalContent);
            if (success) {
                initializeDetalhesModalListeners(ocorrenciaId);
            }
        });
    }

    // Adiciona listeners aos cards já existentes na página
    document.querySelectorAll('.btn-ver-detalhes').forEach(button => {
        addCardListeners(button.closest('.col'));
    });

    // MUDANÇA: Este submit agora é customizado para receber HTML
    const handleNovaOcorrenciaSubmit = async (event) => {
        event.preventDefault();
        const form = event.target;
        showLoadingFeedback();
        
        try {
            const response = await fetch(form.action, {
                method: 'POST',
                body: new URLSearchParams(new FormData(form))
            });

            const responseHtml = await response.text();
            
            if (response.ok) {
                try {
                    // Tenta parsear como JSON. Se falhar, é porque veio HTML (sucesso)
                    JSON.parse(responseHtml);
                    // Se chegou aqui, é um JSON de erro que o controller mandou com status 200 (improvável, mas seguro)
                    showErrorFeedback(responseHtml.message || 'Erro ao processar.');
                } catch (e) {
                    // SUCESSO! Veio HTML.
                    mainModal.hide();
                    showSuccessFeedback('Ocorrência registrada com sucesso!', false); // false = não recarregar

                    const placeholder = document.getElementById('empty-placeholder');
                    if (placeholder) {
                        placeholder.remove();
                    }
                    
                    const tempDiv = document.createElement('div');
                    tempDiv.innerHTML = responseHtml;
                    const newCardElement = tempDiv.firstElementChild;

                    if (!newCardElement || !newCardElement.dataset.ocorrenciaId) {
                         console.error("Fragmento de HTML inválido recebido.");
                         window.location.reload(); // Fallback
                         return;
                    }
                    
                    if(listaOcorrencias) {
                        listaOcorrencias.prepend(newCardElement);
                        addCardListeners(newCardElement); // Adiciona listener ao novo card
                    } else {
                        window.location.reload(); // Fallback se a lista não for encontrada
                    }
                }
            } else {
                 // Erros 400, 500, etc.
                try {
                    const errorJson = JSON.parse(responseHtml);
                    showErrorFeedback(errorJson.message || 'Erro ao salvar.');
                } catch {
                     showErrorFeedback(responseHtml || 'Erro ao salvar.');
                }
            }
        } catch (error) {
             console.error(`Erro no submit AJAX para ${form.action}:`, error);
             showErrorFeedback('Erro de comunicação com o servidor.');
        }
    };

    const handleComentarioSubmit = async (event) => {
        event.preventDefault();
        const form = event.target;
        const input = form.querySelector('input[name="comentario"]');
        if (!input || !input.value.trim()) return;

        await handleAjaxFormSubmit(form, (responseData) => {
            input.value = '';
            addComentarioToList(responseData);
        });
    };

    const handleAnexoSubmit = async (event) => {
        event.preventDefault();
        const form = event.target;
        const fileInput = form.querySelector('input[type="file"]');
        if (!fileInput || fileInput.files.length === 0) return;

        await handleAjaxFormSubmit(form, (responseData) => {
            fileInput.value = '';
            addAnexoToList(responseData);
        });
    };

    const handleFinalizarSubmit = async (event) => {
        event.preventDefault();
        const form = event.target;
        await handleAjaxFormSubmit(form, (responseData) => {
            if (finalizarModalInstance) finalizarModalInstance.hide();
            mainModal.hide();
            // MUDANÇA: Força o reload aqui
            showSuccessFeedback(responseData.message || 'Ocorrência finalizada com sucesso!', true);
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
        listElement.insertBefore(newItem, listElement.firstChild);
    };

    const addAnexoToList = (anexoDTO) => {
        const listElement = document.getElementById('anexosList');
        if (!listElement) return;

        const emptyMsg = listElement.querySelector('.text-muted');
        if (emptyMsg) emptyMsg.remove();

        const newItem = document.createElement('div');
        newItem.className = 'anexo-item d-flex justify-content-between align-items-center';
        newItem.dataset.anexoId = anexoDTO.id;
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
        listElement.insertBefore(newItem, listElement.firstChild);

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
                        responseText = await response.text();
                        try {
                            responseData = JSON.parse(responseText);
                        } catch (parseError) {
                            responseData = { message: responseText || 'Resposta inválida do servidor.' };
                        }
                    } catch (readError) {
                        responseData = { message: 'Não foi possível ler a resposta do servidor.' };
                    }

                    if (response.ok) {
                        const itemToRemove = document.querySelector(`.anexo-item[data-anexo-id="${anexoId}"]`);
                        itemToRemove?.remove();

                        const listElement = document.getElementById('anexosList');
                        if (listElement && !listElement.querySelector('.anexo-item')) {
                            listElement.innerHTML = '<div class="text-muted text-center p-3">Nenhum anexo adicionado.</div>';
                        }
                         if (Swal.isLoading()) { Swal.close(); }
                    } else {
                        const message = responseData.message || 'Erro ao excluir anexo.';
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
                finalizarModalInstance = createStaticModal(finalizarModalElement);
                const formFinalizar = finalizarModalElement.querySelector('#finalizarForm');
                formFinalizar?.addEventListener('submit', handleFinalizarSubmit);
            }
            finalizarModalInstance.show();
        }
    };

    const handleAnexoLinkClick = async (event) => {
        const link = event.target.closest('a.anexo-link');
        if (!link) return;

        event.preventDefault();
        const url = link.href;
        showLoadingFeedback('Verificando arquivo...');

        try {
            // Usa HEAD para verificar a existência sem baixar o arquivo
            const response = await fetch(url, { method: 'HEAD' });

            if (response.ok) {
                if (Swal.isLoading()) { Swal.close(); }
                window.open(url, '_blank');
            } else if (response.status === 404) {
                 // Trata o 404 com a mensagem amigável
                 showErrorFeedback("Arquivo não encontrado. Pode ter sido excluído.");
            } else {
                 // Trata outros erros (500, 403, etc.)
                 let errorMsg = `Erro ${response.status} ao verificar o arquivo.`;
                 try {
                    const errorText = await response.text();
                    if (errorText) {
                        const jsonMatch = errorText.match(/"detail":"([^"]*)"/i) || errorText.match(/"message":"([^"]*)"/i);
                        if (jsonMatch && jsonMatch[1]) {
                            const detailMsgMatch = jsonMatch[1].match(/"([^"]*)"/);
                            errorMsg = (detailMsgMatch && detailMsgMatch[1]) ? detailMsgMatch[1] : jsonMatch[1];
                        } else {
                            const titleMatch = errorText.match(/<title>(.*?)<\/title>/i);
                            errorMsg = (titleMatch && titleMatch[1]) ? titleMatch[1] : `Erro ${response.status}: ${response.statusText}`;
                        }
                    }
                 } catch (e) {
                     errorMsg = `Erro ${response.status}: ${response.statusText} ao verificar o arquivo.`;
                 }
                 showErrorFeedback(errorMsg);
            }
        } catch (error) {
            console.error('Erro de rede ao verificar/acessar anexo:', error);
            showErrorFeedback('Erro de comunicação ao tentar acessar o anexo.');
        }
    };

    const initializeNovaOcorrenciaListeners = () => {
        const form = mainModalContent.querySelector('#ocorrenciaForm');
        // MUDANÇA: Aponta para a nova função de submit
        form?.addEventListener('submit', handleNovaOcorrenciaSubmit); 

        const condominioSelect = mainModalContent.querySelector('#condominioId');
        const unidadeSelect = mainModalContent.querySelector('#unidadeId');

        if (condominioSelect && unidadeSelect) {
            condominioSelect.addEventListener('change', (event) => {
                carregarUnidadesPorCondominio(event.target.value, unidadeSelect);
            });
            if (condominioSelect.value) {
                carregarUnidadesPorCondominio(condominioSelect.value, unidadeSelect);
            }
        }
    };

    const initializeDetalhesModalListeners = (ocorrenciaId) => {
        const comentarioForm = mainModalContent.querySelector('#comentarioForm');
        comentarioForm?.addEventListener('submit', handleComentarioSubmit);

        const anexoForm = mainModalContent.querySelector('#anexoForm');
        anexoForm?.addEventListener('submit', handleAnexoSubmit);

        mainModalContent.querySelectorAll('.btn-excluir-anexo').forEach(button => {
            button.removeEventListener('click', handleExcluirAnexoClick);
            button.addEventListener('click', handleExcluirAnexoClick);
        });

        const btnAbrirFinalizar = mainModalContent.querySelector('#btnAbrirModalFinalizar');
        btnAbrirFinalizar?.addEventListener('click', handleAbrirModalFinalizarClick);

        const anexosList = mainModalContent.querySelector('#anexosList');
        if (anexosList) {
            anexosList.removeEventListener('click', handleAnexoLinkClick);
            anexosList.addEventListener('click', handleAnexoLinkClick);
        }

        mainModalElement.addEventListener('hidden.bs.modal', () => {
            if (finalizarModalInstance) {
                const formFinalizar = document.getElementById('finalizarForm');
                formFinalizar?.removeEventListener('submit', handleFinalizarSubmit);
                finalizarModalInstance.dispose();
                finalizarModalInstance = null;
                 const backdrops = document.querySelectorAll('.modal-backdrop');
                 backdrops.forEach(bd => bd.remove());
                 document.body.classList.remove('modal-open');
                 document.body.style.overflow = '';
                 document.body.style.paddingRight = '';
            }
        }, { once: true });
    };

    const escapeHtml = (unsafe) => {
        if (!unsafe) return '';
        return unsafe
             .replace(/&/g, "&amp;")
             .replace(/</g, "&lt;")
             .replace(/>/g, "&gt;")
             .replace(/"/g, "&quot;")
             .replace(/'/g, "&#039;");
    }

    const formatDateTime = (dateTimeString) => {
        if (!dateTimeString) return '';
        try {
            const date = new Date(dateTimeString);
            if (isNaN(date.getTime())) {
                throw new Error("Data inválida");
            }
            return date.toLocaleDateString('pt-BR') + ' ' + date.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
        } catch (e) {
            console.error("Erro ao formatar data:", dateTimeString, e);
            return dateTimeString;
        }
    }

     const formatBytes = (bytes, decimals = 1) => {
         if (!+bytes || bytes === 0) return '0 Bytes'
         const k = 1024
         const dm = decimals < 0 ? 0 : decimals
         const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB']

         const i = (bytes === 0) ? 0 : Math.floor(Math.log(bytes) / Math.log(k));

         if (isNaN(i) || i < 0) return '0 Bytes';

         return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`
     }

    document.addEventListener('modalContentLoaded', () => {
        if (mainModalContent.querySelector('#ocorrenciaForm')) {
            initializeNovaOcorrenciaListeners();
        }
        if (mainModalContent.querySelector('.ocorrencia-detalhes-modal')) {
            initializeDetalhesModalListeners(null);
        }
    });

});