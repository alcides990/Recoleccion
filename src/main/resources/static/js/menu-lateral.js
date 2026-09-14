$(function () {
    const menu = $('#menu_aside');
    const boton = $('#menu-toggle');
    const pantallaCompacta = window.matchMedia('(max-width: 1020px)');

    function mostrar(abierto) {
        menu.toggleClass('active', abierto);
        boton.toggleClass('active', abierto).attr('aria-expanded', String(abierto));
        if (!abierto) $('#user-session').hide();
        window.dispatchEvent(new Event('resize'));
    }

    boton.attr({'aria-controls': 'menu_aside', 'aria-expanded': 'false', 'aria-label': 'Abrir o cerrar menú'});
    boton.on('click', () => mostrar(!menu.hasClass('active')));
    pantallaCompacta.addEventListener('change', () => mostrar(false));
    menu.on('click', 'a[href]', function () {
        if (pantallaCompacta.matches && $(this).attr('href') !== '#') mostrar(false);
    });
    $(document).on('keydown', evento => {
        if (evento.key === 'Escape' && pantallaCompacta.matches && menu.hasClass('active')) {
            mostrar(false);
            boton.trigger('focus');
        }
    });
});
