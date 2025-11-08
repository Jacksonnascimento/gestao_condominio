// Handler unificado para os formulários do modal de perfil
const handlePerfilFormSubmit = async (event) => {
    event.preventDefault();
    const form = event.target;
    const formModal = bootstrap.Modal.getInstance(document.getElementById('formModal'));
    
    Swal.fire({
        title: 'Salvando...',
        didOpen: () => { Swal.showLoading(); },
        allowOutsideClick: false, allowEscapeKey: false
    });

    try {
        const response = await fetch(form.action, {
            method: 'POST',
            body: new URLSearchParams(new FormData(form))
        });

        const responseData = await response.json();

        if (response.ok) {
            
            await Swal.fire({
                icon: 'success', title: 'Sucesso!', text: responseData.message || 'Operação realizada com sucesso.',
                timer: 2000, showConfirmButton: false
            });
            
            // Se foi o form de dados, atualiza o nome na sidebar em tempo real
            if (form.id === 'perfilDadosForm') {
                document.querySelector('.username').textContent = form.querySelector('#pesNome').value;
            } else {
                // Se foi o form de senha, fecha o modal
                formModal.hide();
            }
        } else {
            Swal.fire({
                icon: 'error', title: 'Erro!', text: responseData.message || 'Ocorreu um erro.'
            });
        }
    } catch (error) {
        console.error('Erro ao salvar perfil:', error);
        Swal.fire('Erro', 'Não foi possível comunicar com o servidor.', 'error');
    }
};


document.addEventListener('DOMContentLoaded', function() {
    // --- LÓGICA DO TOGGLE DO MENU MOBILE ---
    const sidebar = document.getElementById('sidebar');
    const toggleButton = document.getElementById('sidebar-toggle-btn');

    if (sidebar && toggleButton) {
        toggleButton.addEventListener('click', function() {
            sidebar.classList.toggle('show');
        });

        document.addEventListener('click', function(event) {
            const isClickInsideSidebar = sidebar.contains(event.target);
            const isClickOnToggleButton = toggleButton.contains(event.target);

            if (!isClickInsideSidebar && !isClickOnToggleButton && sidebar.classList.contains('show')) {
                sidebar.classList.remove('show');
            }
        });
    }

    // --- LÓGICA DO MODAL "MEU PERFIL" ---
    const btnMeuPerfil = document.getElementById('btnMeuPerfil');
    const formModalElement = document.getElementById('formModal');
    
    if (btnMeuPerfil && formModalElement) {
        const modalContent = document.getElementById('modalContent');
        
        btnMeuPerfil.addEventListener('click', async (event) => {
            event.preventDefault();
            
            // Garante que a função de app.js esteja disponível
            if (typeof createStaticModal !== 'function') {
                console.error('createStaticModal (app.js) não foi encontrada.');
                Swal.fire('Erro', 'Função essencial (app.js) não carregada.', 'error');
                return;
            }
            
            const formModal = createStaticModal(formModalElement);

            try {
                // Busca o conteúdo do modal
                const response = await fetch('/perfil/modal');
                if (!response.ok) throw new Error('Falha ao carregar o formulário de perfil.');
                
                modalContent.innerHTML = await response.text();
                formModal.show();
                
                // Adiciona os listeners de submit aos formulários corretos
                const perfilDadosForm = modalContent.querySelector('#perfilDadosForm');
                if (perfilDadosForm) {
                    perfilDadosForm.addEventListener('submit', handlePerfilFormSubmit);
                }
                
                const perfilSenhaForm = modalContent.querySelector('#perfilSenhaForm');
                if (perfilSenhaForm) {
                    perfilSenhaForm.addEventListener('submit', handlePerfilFormSubmit);
                }
                
            } catch (error) {
                console.error('Erro ao abrir o modal de perfil:', error);
                Swal.fire('Erro', 'Não foi possível carregar seu perfil.', 'error');
            }
        });
    }
});