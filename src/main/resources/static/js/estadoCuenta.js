$(document).on('change', '#cuentaCorriente', function () {
    const url = new URL('/servicio/estadoCuenta', window.location.origin);
    url.searchParams.set('cuentaCorriente', $(this).val());
    url.searchParams.set('page', '0');
    window.location.href = url;
});

