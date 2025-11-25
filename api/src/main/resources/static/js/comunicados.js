document.addEventListener('DOMContentLoaded', () => {

    const modalElement = document.getElementById('formModal');
    if (!modalElement) {
        console.error('Modal genérico #formModal não encontrado.');
        return;
    }
    const modalContent = document.getElementById('modalContent');
    const formModal = createStaticModal(modalElement);
    const listaComunicados = document.getElementById('lista-comunicados');

    const handleFormSubmit = async (event) => {
        event.preventDefault();
        const form = event.target;
        const url = form.action;

        const formData = new FormData();

        const anexoInput = form.querySelector('#comunicadoAnexo');
        if (anexoInput && anexoInput.files.length > 0) {
            formData.append('anexo', anexoInput.files[0]);
        }

        const selectCondominios = form.querySelector('#comunicadoCondominios');
        let condominioIds = [];
        if (selectCondominios) {
            condominioIds = Array.from(selectCondominios.selectedOptions).map(opt => parseInt(opt.value));
        }

        const comunicadoDto = {
            id: form.querySelector('input[type="hidden"][name="id"]')?.value || null,
            titulo: form.querySelector('#comunicadoTitulo').value,
            mensagem: form.querySelector('#comunicadoMensagem').value,
            publicoDestino: form.querySelector('#comunicadoPublico').value,
            isUrgente: form.querySelector('#comunicadoUrgente').value === 'true',
            condominioIds: condominioIds
        };
        formData.append('comunicado', new Blob([JSON.stringify(comunicadoDto)], { type: 'application/json' }));

        try {
            const response = await fetch(url, {
                method: 'POST',
                body: formData
            });

            if (response.ok) {
                const responseHtml = await response.text();
                
                try {
                    // Tenta parsear como JSON. Se falhar, é porque veio HTML (sucesso)
                    const errorData = JSON.parse(responseHtml);
                    Swal.fire('Erro!', errorData.message || 'Não foi possível salvar.', 'error');
                
                } catch(e) {
                    // SUCESSO! Veio HTML.
                    formModal.hide();
                    await Swal.fire({
                        title: 'Sucesso!', text: 'Comunicado salvo com sucesso.', icon: 'success',
                        timer: 2000, showConfirmButton: false
                    });

                    const placeholder = document.getElementById('empty-placeholder');
                    if (placeholder) {
                        placeholder.remove();
                    }
                    
                    const tempDiv = document.createElement('div');
                    tempDiv.innerHTML = responseHtml;
                    const newCardElement = tempDiv.firstElementChild;

                    if (!newCardElement || !newCardElement.dataset.comunicadoId) {
                        console.error("Fragmento de HTML inválido recebido.");
                        location.reload(); // Fallback
                        return;
                    }
                    
                    // CORREÇÃO AQUI: Verifica se a lista existe
                    if (listaComunicados) {
                        const id = newCardElement.dataset.comunicadoId;
                        const existingCard = listaComunicados.querySelector(`.card[data-comunicado-id="${id}"]`);
                        
                        if (existingCard) {
                            // Se está editando, substitui o card antigo
                            existingCard.replaceWith(newCardElement);
                        } else {
                            // Se é novo, adiciona no topo da lista
                            listaComunicados.prepend(newCardElement);
                        }
                    } else {
                        // Se a lista não existe (primeiro item), recarrega
                        window.location.reload();
                    }
                }
            } else {
                // Trata erros 400/500
                const errorMessage = await response.text();
                try {
                    const errorJson = JSON.parse(errorMessage);
                    Swal.fire('Erro!', errorJson.message || 'Não foi possível salvar.', 'error');
                } catch {
                    Swal.fire('Erro!', errorMessage || 'Não foi possível salvar.', 'error');
                }
            }
        } catch (error) {
            console.error('Erro ao submeter:', error);
            Swal.fire('Erro de Conexão!', 'Não foi possível conectar ao servidor.', 'error');
        }
    };

    const openComunicadoFormModal = async (url) => {
        try {
            modalContent.innerHTML = '<div class="modal-body text-center"><span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Carregando...</div>';
            formModal.show();

            const response = await fetch(url);
            if (!response.ok) throw new Error('Falha ao carregar formulário.');

            modalContent.innerHTML = await response.text();

            const form = modalContent.querySelector('#comunicadoForm');
            if (form) {
                form.addEventListener('submit', handleFormSubmit);
            } else {
                console.error("Formulário #comunicadoForm não encontrado no fragmento carregado.");
                Swal.fire('Erro Interno', 'Não foi possível encontrar o formulário no conteúdo carregado.', 'error');
                formModal.hide();
            }

        } catch (error) {
            console.error('Erro ao abrir modal:', error);
            Swal.fire('Erro!', 'Não foi possível carregar o formulário de comunicado.', 'error');
            formModal.hide();
        }
    };

    const handleAnexoClick = async (id, action) => {
        try {
            const response = await fetch(`/comunicados/anexo/${id}`);

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || 'Arquivo não encontrado.');
            }

            const disposition = response.headers.get('Content-Disposition');
            let filename = 'anexo';
            if (disposition) {
                const filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
                const matches = filenameRegex.exec(disposition);
                if (matches != null && matches[1]) {
                    filename = matches[1].replace(/['"]/g, '');
                }
            }

            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.style.display = 'none';
            a.href = url;

            if (action === 'download') {
                a.download = filename;
            } else { // 'view'
                a.target = '_blank';
            }

            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);

        } catch (error) {
            Swal.fire('Erro!', error.message, 'error');
        }
    };

    document.addEventListener('click', (event) => {
        const target = event.target;

        if (target.matches('#btnNovoComunicado') || target.closest('#btnNovoComunicado')) {
            openComunicadoFormModal('/comunicados/novo');
            return;
        }

        const btnEdit = target.closest('.btn-edit');
        if (btnEdit) {
            const id = btnEdit.dataset.id;
            if (id) openComunicadoFormModal(`/comunicados/editar/${id}`);
            return;
        }

        const btnVerAnexo = target.closest('.btn-ver-anexo');
        if (btnVerAnexo) {
            const id = btnVerAnexo.dataset.id;
            if (id) handleAnexoClick(id, 'view');
            return;
        }

        const btnBaixarAnexo = target.closest('.btn-baixar-anexo');
        if (btnBaixarAnexo) {
            const id = btnBaixarAnexo.dataset.id;
            if (id) handleAnexoClick(id, 'download');
            return;
        }

        const btnExcluir = target.closest('.btn-excluir');
        if (btnExcluir) {
            const id = btnExcluir.dataset.id;
            if (!id) return;

            Swal.fire({
                title: 'Tem certeza?',
                text: "Você não poderá reverter esta ação!",
                icon: 'warning',
                showCancelButton: true,
                confirmButtonColor: '#d33',
                cancelButtonColor: '#3085d6',
                confirmButtonText: 'Sim, excluir!',
                cancelButtonText: 'Cancelar'
            }).then(async (result) => {
                if (result.isConfirmed) {
                    try {
                        const response = await fetch(`/comunicados/excluir/${id}`, { method: 'POST' });

                        if (response.ok) {
                            // MUDANÇA: Remove o card do DOM em vez de recarregar
                            btnExcluir.closest('.card').remove(); 
                            
                            Swal.fire({
                                title: 'Excluído!',
                                text: 'O comunicado foi excluído com sucesso.',
                                icon: 'success',
                                timer: 2000,
                                showConfirmButton: false
                            });
                        } else {
                            const errorMessage = await response.text();
                             try {
                                const errorJson = JSON.parse(errorMessage);
                                Swal.fire('Erro!', errorJson.message || 'Não foi possível excluir.', 'error');
                            } catch {
                                Swal.fire('Erro!', errorMessage || 'Não foi possível excluir.', 'error');
                            }
                        }
                    } catch (error) {
                        console.error('Erro ao excluir:', error);
                        Swal.fire('Erro de Conexão!', 'Não foi possível conectar ao servidor.', 'error');
                    }
                }
            });
        }
    });
});