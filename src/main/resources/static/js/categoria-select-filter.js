(function () {
    'use strict';

    const normalizar = valor => String(valor || '')
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '')
            .toLowerCase()
            .trim();

    function aplicarFiltro(entrada) {
        const selector = entrada.dataset.filtroCategorias;
        const select = selector ? document.querySelector(selector) : null;
        if (!select) return;

        const busqueda = normalizar(entrada.value);
        let visibles = 0;
        Array.from(select.options).forEach(opcion => {
            const coincide = !busqueda || normalizar(opcion.textContent + ' ' + opcion.value).includes(busqueda);
            opcion.hidden = !coincide;
            opcion.style.display = coincide ? '' : 'none';
            if (coincide) visibles++;
        });

        const resultado = entrada.dataset.resultadoFiltro
                ? document.querySelector(entrada.dataset.resultadoFiltro) : null;
        if (resultado) {
            resultado.textContent = busqueda
                    ? visibles + (visibles === 1 ? ' categoría encontrada' : ' categorías encontradas')
                    : '';
        }
    }

    document.querySelectorAll('[data-filtro-categorias]').forEach(entrada => {
        entrada.addEventListener('input', () => aplicarFiltro(entrada));
        entrada.addEventListener('keydown', evento => {
            if (evento.key === 'Escape') {
                entrada.value = '';
                aplicarFiltro(entrada);
            }
        });
    });

    document.querySelectorAll('[data-limpiar-filtro-categorias]').forEach(boton => {
        boton.addEventListener('click', () => {
            const entrada = document.querySelector(boton.dataset.limpiarFiltroCategorias);
            if (!entrada) return;
            entrada.value = '';
            aplicarFiltro(entrada);
            entrada.focus();
        });
    });
})();
