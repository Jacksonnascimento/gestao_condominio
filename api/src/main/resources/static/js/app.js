/**
 * Cria e retorna uma instância de um modal do Bootstrap com configurações padrão
 * para não fechar ao clicar fora ou pressionar a tecla ESC.
 * @param {HTMLElement|string} modalElement - O elemento do modal ou seu seletor CSS.
 * @returns {bootstrap.Modal} A instância do modal.
 */
function createStaticModal(modalElement) {
    if (!modalElement) {
        throw new Error('Elemento do modal não fornecido.');
    }
    return new bootstrap.Modal(modalElement, {
        backdrop: 'static',
        keyboard: false
    });
}