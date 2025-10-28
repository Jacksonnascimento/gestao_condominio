document.addEventListener('DOMContentLoaded', () => {

    const mainModalElement = document.getElementById('formModal');
    if (!mainModalElement) {
        console.error('Modal principal #formModal não encontrado.');
        return;
    }
    const mainModalContent = document.getElementById('modalContent');
    const mainModal = createStaticModal(mainModalElement);

    let finalizarModalInstance = null; // Para guardar a instância do sub-modal

    // --- Funções Auxiliares ---

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
        Swal.fire({ icon: 'error', title: 'Erro!', text: message });
    };

    const fetchAndInjectModalContent = async (url, modalInstance, contentElement) => {
        contentElement.innerHTML = '<div class="modal-body text-center"><span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Carregando...</div>';
        modalInstance.show();
        try {
            const response = await fetch(url);
            if (!response.ok) {
                throw new Error(`Falha ao carregar conteúdo: ${response.statusText}`);
            }
            contentElement.innerHTML = await response.text();
            return true; // Sucesso
        } catch (error) {
            console.error('Erro ao buscar/injetar conteúdo do modal:', error);
            contentElement.innerHTML = `<div class="modal-body"><p class="text-danger">Erro ao carregar o conteúdo. Tente novamente.</p></div>`;
            // Não fecha o modal automaticamente para o usuário ver o erro
            return false; // Falha
        }
    };

     const handleAjaxFormSubmit = async (form, successCallback, errorCallback) => {
        showLoadingFeedback();
        const url = form.action;
        const method = form.method;
        const formData = new FormData(form);

        try {
            const response = await fetch(url, { method: method.toUpperCase(), body: formData });
            const responseData = await response.json();

            if (response.ok) {
                if(successCallback) successCallback(responseData);
            } else {
                 showErrorFeedback(responseData.message || 'Erro ao processar a solicitação.');
                 if(errorCallback) errorCallback(responseData);
            }
        } catch (error) {
            console.error(`Erro no submit AJAX para ${url}:`, error);
            showErrorFeedback('Erro de comunicação com o servidor.');
             if(errorCallback) errorCallback(error);
        } finally {
             // Garante que o Swal de loading feche se não houver outro feedback
             if (Swal.isLoading()) { Swal.close(); }
        }
    };

    // --- Abertura de Modais ---

    document.getElementById('btnNovaOcorrencia')?.addEventListener('click', async () => {
        const success = await fetchAndInjectModalContent('/ocorrencias/novo', mainModal, mainModalContent);
        if (success) {
            const form = mainModalContent.querySelector('#ocorrenciaForm');
            form?.addEventListener('submit', handleNovaOcorrenciaSubmit);
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

    // --- Submissão de Formulários ---

    const handleNovaOcorrenciaSubmit = async (event) => {
        event.preventDefault();
        const form = event.target;
        // Usa URLSearchParams para forms simples sem anexo
        const formData = new URLSearchParams(new FormData(form));
        showLoadingFeedback();

         try {
            const response = await fetch(form.action, { method: 'POST', body: formData });
            if (response.ok) {
                mainModal.hide();
                showSuccessFeedback('Ocorrência registrada com sucesso!');
            } else {
                const errorData = await response.json();
                showErrorFeedback(errorData.message || 'Erro ao registrar ocorrência.');
            }
        } catch (error) {
             console.error('Erro ao registrar ocorrência:', error);
             showErrorFeedback('Erro de comunicação.');
        } finally {
             if (Swal.isLoading()) { Swal.close(); }
        }
    };

     const handleComentarioSubmit = async (event) => {
         event.preventDefault();
         const form = event.target;
         const input = form.querySelector('input[name="comentario"]');
         if (!input || !input.value.trim()) return; // Não envia vazio

         await handleAjaxFormSubmit(form, (responseData) => {
             // Limpa o campo e atualiza a lista dinamicamente
             input.value = '';
             addComentarioToList(responseData);
             Swal.close(); // Fecha o loading, sem mensagem de sucesso explícita
         });
     };

     const handleAnexoSubmit = async (event) => {
         event.preventDefault();
         const form = event.target;
         const fileInput = form.querySelector('input[type="file"]');
         if (!fileInput || fileInput.files.length === 0) return;

         await handleAjaxFormSubmit(form, (responseData) => {
            fileInput.value = ''; // Limpa o input
            addAnexoToList(responseData);
            Swal.close(); // Fecha o loading
         });
     };

     const handleFinalizarSubmit = async (event) => {
         event.preventDefault();
         const form = event.target;

         await handleAjaxFormSubmit(form, (responseData) => {
             if (finalizarModalInstance) finalizarModalInstance.hide();
             mainModal.hide();
             showSuccessFeedback(responseData.message || 'Ocorrência finalizada com sucesso!');
         }, () => {
             // Em caso de erro ao finalizar, apenas fecha o Swal de erro, mantém sub-modal aberto
             // showErrorFeedback já foi chamado
         });
     };

    // --- Funções de Atualização Dinâmica da UI (Modal Detalhes) ---

     const addComentarioToList = (comentarioDTO) => {
        const listElement = document.getElementById('comentariosList');
        if (!listElement) return;

        // Remove mensagem de "Nenhum comentário" se existir
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
        // Adiciona no início da lista (mais recente primeiro)
        listElement.insertBefore(newItem, listElement.firstChild);
    };

    const addAnexoToList = (anexoDTO) => {
        const listElement = document.getElementById('anexosList');
        if (!listElement) return;

        const emptyMsg = listElement.querySelector('.text-muted');
        if (emptyMsg) emptyMsg.remove();

        const newItem = document.createElement('div');
        newItem.className = 'anexo-item d-flex justify-content-between align-items-center';
        newItem.dataset.anexoId = anexoDTO.id; // Para facilitar a remoção
        newItem.innerHTML = `
            <div>
                 <a href="/ocorrencias/${anexoDTO.ocorrenciaId}/anexo/${anexoDTO.id}" target="_blank" class="text-decoration-none">${escapeHtml(anexoDTO.nomeOriginal || 'anexo')}</a>
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

         // Adiciona listener ao novo botão de excluir
          const deleteButton = newItem.querySelector('.btn-excluir-anexo');
          deleteButton?.addEventListener('click', handleExcluirAnexoClick);
    };

    // --- Handlers de Eventos (Modal Detalhes) ---

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
                    const responseData = await response.json();
                    if (response.ok) {
                        // Remove o item da lista visualmente
                        const itemToRemove = document.querySelector(`.anexo-item[data-anexo-id="${anexoId}"]`);
                        itemToRemove?.remove();
                        Swal.close(); // Fecha loading
                        // Adiciona mensagem se a lista ficar vazia
                        const listElement = document.getElementById('anexosList');
                        if (listElement && !listElement.querySelector('.anexo-item')) {
                             listElement.innerHTML = '<div class="text-muted text-center p-3">Nenhum anexo adicionado.</div>';
                        }
                    } else {
                        showErrorFeedback(responseData.message || 'Erro ao excluir anexo.');
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
            // Cria a instância do sub-modal se ainda não existir
            if (!finalizarModalInstance) {
                 finalizarModalInstance = new bootstrap.Modal(finalizarModalElement);
                 const formFinalizar = finalizarModalElement.querySelector('#finalizarForm');
                 formFinalizar?.addEventListener('submit', handleFinalizarSubmit);
            }
            finalizarModalInstance.show();
        }
     };

     // --- Inicialização de Listeners (Modal Detalhes) ---
     const initializeDetalhesModalListeners = (ocorrenciaId) => {
         const comentarioForm = mainModalContent.querySelector('#comentarioForm');
         comentarioForm?.addEventListener('submit', handleComentarioSubmit);

         const anexoForm = mainModalContent.querySelector('#anexoForm');
         anexoForm?.addEventListener('submit', handleAnexoSubmit);

         mainModalContent.querySelectorAll('.btn-excluir-anexo').forEach(button => {
            button.removeEventListener('click', handleExcluirAnexoClick); // Evita duplicidade
            button.addEventListener('click', handleExcluirAnexoClick);
         });

         const btnAbrirFinalizar = mainModalContent.querySelector('#btnAbrirModalFinalizar');
         btnAbrirFinalizar?.addEventListener('click', handleAbrirModalFinalizarClick);

         // Limpa a instância do sub-modal quando o modal principal é fechado
         mainModalElement.addEventListener('hidden.bs.modal', () => {
             if (finalizarModalInstance) {
                 finalizarModalInstance.dispose();
                 finalizarModalInstance = null;
             }
         }, { once: true }); // Executa apenas uma vez para evitar leaks
     };


    // --- Funções Utilitárias ---
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
            return date.toLocaleDateString('pt-BR') + ' ' + date.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
        } catch (e) { return dateTimeString; }
    }

     const formatBytes = (bytes, decimals = 1) => {
        if (!+bytes) return '0 Bytes'
        const k = 1024
        const dm = decimals < 0 ? 0 : decimals
        const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB']
        const i = Math.floor(Math.log(bytes) / Math.log(k))
        return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`
    }

}); 