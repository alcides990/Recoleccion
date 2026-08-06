$(document).ready(function () {

    var pathname = window.location.pathname;
    var cuentaCorriente = $("#cuentaCorriente").val();
    if (pathname === '/servicio/editar/' + cuentaCorriente) {
        $("#cuentaCorriente").prop('readonly', true);
        $("#fechaInicio").prop('readonly', true);
    }
});



function formatearCuentaCorriente(e) {
    if (e.key != 'Backspace') {
        var cuentaCorriente = $("#cuentaCorriente").val();
        var longitudCuentaCorriente = cuentaCorriente.length;

        switch (longitudCuentaCorriente) {
            case 2:
                $("#cuentaCorriente").val(cuentaCorriente + '-');

                break;
            case 7:
                $("#cuentaCorriente").val(cuentaCorriente + '-');

                break;
            case 11:
                if (e.key != '/') {
                    $("#cuentaCorriente").val(cuentaCorriente.substring(0, cuentaCorriente.length - 1) + '/' + cuentaCorriente.substring(cuentaCorriente.length - 1));
                }
                break;

            default:

                break;
        }
    }
}


