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

/*!
 * Color mode toggler for Bootstrap's docs (https://getbootstrap.com/)
 * Copyright 2011-2024 The Bootstrap Authors
 * Licensed under the Creative Commons Attribution 3.0 Unported License.
 */
(() => {
    'use strict'

    const getStoredTheme = () => localStorage.getItem('theme');
    const setStoredTheme = theme => localStorage.setItem('theme', theme);

    const getPreferredTheme = () => {
        const storedTheme = getStoredTheme();
        if (storedTheme) {
            return storedTheme;
        }
        return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }

    const setTheme = theme => {
        if (theme === 'auto' && window.matchMedia('(prefers-color-scheme: dark)').matches) {
            document.documentElement.setAttribute('data-bs-theme', 'dark');
        } else {
            document.documentElement.setAttribute('data-bs-theme', theme);
        }
    }

    setTheme(getPreferredTheme());

    const showActiveTheme = (theme, focus = false) => {
        const themeSwitcher = document.querySelector('#theme-toggler');

        if (!themeSwitcher) {
            return;
        }

        const themeSwitcherLight = document.querySelector('#theme-toggler-light');
        const themeSwitcherDark = document.querySelector('#theme-toggler-dark');

        if (theme === 'dark') {
            themeSwitcherLight.classList.add('d-none');
            themeSwitcherDark.classList.remove('d-none');
        } else {
            themeSwitcherLight.classList.remove('d-none');
            themeSwitcherDark.classList.add('d-none');
        }

        if (focus) {
            themeSwitcher.focus();
        }
    }

    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
        const storedTheme = getStoredTheme();
        if (storedTheme !== 'light' && storedTheme !== 'dark') {
            setTheme(getPreferredTheme());
        }
    });

    window.addEventListener('DOMContentLoaded', () => {
        showActiveTheme(getPreferredTheme());

        document.querySelectorAll('[data-bs-theme-value]')
            .forEach(toggle => {
                toggle.addEventListener('click', (event) => {
                    event.preventDefault();
                    const theme = toggle.getAttribute('data-bs-theme-value');
                    setStoredTheme(theme);
                    setTheme(theme);
                    showActiveTheme(theme, true);
                })
            });

        const themeToggler = document.getElementById('theme-toggler');
        if (themeToggler) {
            themeToggler.addEventListener('click', () => {
                const currentTheme = getStoredTheme() || getPreferredTheme();
                const newTheme = currentTheme === 'light' ? 'dark' : 'light';
                setStoredTheme(newTheme);
                setTheme(newTheme);
                showActiveTheme(newTheme, true);
            });
        }
    });
})()