$(document).ready(function () {

    var url = window.location.pathname;
    var cuentaCorriente = $("#cuentaCorriente").val();
    if (url === '/servicio/editar/' + cuentaCorriente) {
        $("#cuentaCorriente").prop('readonly', true);
        $("#fechaInicio").prop('readonly', true);
    }
   


    $.datepicker.setDefaults($.datepicker.regional["es"]);
    $("#fechaInicio").datepicker({
        changeYear: true
    }
    );

});

 function formaterarCuentaCorriente() {
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
                $("#cuentaCorriente").val(cuentaCorriente.substring(0, cuentaCorriente.length - 1) + '/' + cuentaCorriente.substring(cuentaCorriente.length - 1));

                break;

            default:

                break;
        }
    }


