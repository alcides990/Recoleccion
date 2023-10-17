$(document).ready(function () {

//validar formulario de servicio--------------------------------------------
//    Desabilitar cuenta corriente si la url es para editar
    var url = window.location.pathname; //obtener url
    var cuentaCorriente = $("#cuentaCorriente").val();
    if (url === '/servicio/editar/' + cuentaCorriente) {
        $("#cuentaCorriente").prop('readonly', true);
        $("#fechaInicio").prop('readonly', true);
    }
//    $("#fechaInicio").datepicker({
//        dateFormat: "dd-mm-yy" // Formato de fecha personalizado
//    });

    $.datepicker.setDefaults($.datepicker.regional["es"]);
    $("#fechaInicio").datepicker({
        changeYear: true
    }
    );

});


