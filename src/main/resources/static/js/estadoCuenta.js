$(document).on('change', '#cuentaCorriente', function(event) {

    //    Obtener cuenta corriente seleccionada
        var cuentaCorriente = $("#cuentaCorriente option:selected").text();
//     mandar url con la cuenta corriente
        $(location).attr('href', '/servicio/estadoCuenta/' + cuentaCorriente);
        });


