document.addEventListener('DOMContentLoaded', () => {

    const modalElement = document.getElementById('formModal');
    if (!modalElement) {
        console.error('Modal genérico #formModal não encontrado.');
        return;
    }
    const modalContent = document.getElementById('modalContent');
    const formModal = createStaticModal(modalElement); 

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
                formModal.hide();
                Swal.fire({
                    title: 'Sucesso!', text: 'Comunicado salvo com sucesso.', icon: 'success',
                    timer: 2000, showConfirmButton: false
                }).then(() => location.reload());
            } else {
                const errorMessage = await response.text();
                Swal.fire('Erro!', errorMessage || 'Não foi possível salvar.', 'error');
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

    const btnNovoComunicado = document.getElementById('btnNovoComunicado');
    if (btnNovoComunicado) {
        btnNovoComunicado.addEventListener('click', () => {
            openComunicadoFormModal('/comunicados/novo');
        });
    }

    document.querySelectorAll('.btn-edit').forEach(button => {
        button.addEventListener('click', (event) => {
            const id = event.currentTarget.dataset.id;
            if (id) {
                openComunicadoFormModal(`/comunicados/editar/${id}`);
            }
        });
    });

    document.querySelectorAll('.btn-excluir').forEach(button => {
        button.addEventListener('click', (event) => {
            const id = event.currentTarget.dataset.id;
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
                        const response = await fetch(`/comunicados/excluir/${id}`, {
                            method: 'POST',
                        });

                        if (response.ok) {
                            Swal.fire({
                                title: 'Excluído!',
                                text: 'O comunicado foi excluído com sucesso.',
                                icon: 'success',
                                timer: 2000,
                                showConfirmButton: false
                            }).then(() => location.reload());
                        } else {
                            const errorMessage = await response.text();
                            Swal.fire('Erro!', errorMessage || 'Não foi possível excluir.', 'error');
                        }
                    } catch (error) {
                        console.error('Erro ao excluir:', error);
                        Swal.fire('Erro de Conexão!', 'Não foi possível conectar ao servidor.', 'error');
                    }
                }
            });
        });
    });
});