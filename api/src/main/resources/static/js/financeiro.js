document.addEventListener('DOMContentLoaded', function() {
    // Inicializa modais estáticos se a função global existir (conforme padrão do projeto)
    if (typeof createStaticModal === 'function') {
        const modalEl = document.getElementById('modalGerarBoleto');
        if (modalEl) createStaticModal(modalEl);
    }
});

function submeterBoleto() {
    const form = document.getElementById('formGerarBoleto');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }

    const formData = new FormData(form);
    
    // Obtém o token CSRF das meta tags (Obrigatório na arquitetura)
    const csrfToken = document.querySelector('meta[name="_csrf"]').content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

    fetch(form.action, {
        method: 'POST',
        body: formData,
        headers: {
            [csrfHeader]: csrfToken
        }
    })
    .then(response => {
        if (response.ok) {
            Swal.fire({
                icon: 'success',
                title: 'Boleto Gerado!',
                text: 'O boleto foi salvo na sessão e adicionado à lista.',
                confirmButtonText: 'OK'
            }).then(() => {
                window.location.reload();
            });
        } else {
            return response.json().then(data => {
                Swal.fire('Erro', data.message || 'Erro ao processar solicitação', 'error');
            });
        }
    })
    .catch(error => {
        console.error('Erro:', error);
        Swal.fire('Erro', 'Falha na comunicação com o servidor.', 'error');
    });
}

function imprimirBoleto(unidade, valor, dataVencimento, linhaDigitavel, descricao) {
    // 1. Preenche os dados na área de impressão
    document.getElementById('printUnidade').textContent = unidade;
    document.getElementById('printDescricao').textContent = descricao;
    document.getElementById('printLinha').textContent = linhaDigitavel;
    
    // Formatação de Moeda
    const valorFormatado = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(valor);
    document.getElementById('printValor').textContent = valorFormatado;
    
    // Formatação de Data
    let dataFmt = dataVencimento;
    // O Thymeleaf pode passar como string ISO 'yyyy-MM-dd' direto, ou array se não convertido. 
    // O template HTML já usa #temporals.format no display, mas aqui passamos o objeto cru.
    // Vamos garantir o parse correto:
    if (typeof dataVencimento === 'string') {
        const parts = dataVencimento.split('-');
        if(parts.length === 3) {
            dataFmt = `${parts[2]}/${parts[1]}/${parts[0]}`;
        }
    }
    
    document.getElementById('printVencimento').textContent = dataFmt;
    document.getElementById('printDataDoc').textContent = new Date().toLocaleDateString('pt-BR');

    // 2. Exibe a área (remove d-none) para o navegador renderizar
    const areaPrint = document.getElementById('area-impressao');
    areaPrint.classList.remove('d-none');

    // 3. Aciona a impressão do navegador
    window.print();

    // 4. Esconde novamente após um pequeno delay para garantir que o print dialog capturou o conteúdo
    setTimeout(() => {
        areaPrint.classList.add('d-none');
    }, 1000);
}