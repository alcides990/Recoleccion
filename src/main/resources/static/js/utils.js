export function limpiar(campos) {
    if (campos.length > 0) {
        campos.forEach(campo => $(campo).val(""));
    }
}
export function tabular(campoDestino) {
    $(campoDestino).focus();
    if ($(campoDestino).attr("type") === "text") {
        let value = $(campoDestino).val();
        if (value !== '') {
            $(campoDestino)[0].setSelectionRange(value.length, value.length);
        }
    }
}
export function desabilitar(campos = []) {
    if ($(campos).length > 0) {
        campos.forEach((campo) => {
            $(campo).addClass('disabled');
            $(campo).prop('readonly', true);
        });
}
}
export function abilitar(campos = []) {
    if ($(campos).length > 0) {
        campos.forEach((campo) => {
            $(campo).prop('readonly', false);
        });
}
}

export function tabulador(campoActual, campoDestino) {
    $(campoActual).on('keydown', (e) => {
        if (e.keyCode === 13) {
            e.preventDefault();
            $(campoDestino).focus();
            let value = $(campoDestino).val();
            if (value !== '') {
                if ($(campoDestino).attr("type") === "number") {
                    $(campoDestino).attr("type", "text");
                    $(campoDestino)[0].setSelectionRange(value.length, value.length);
                    $(campoDestino).attr("type", "number");
                } else if ($(campoDestino).attr("type") === "text") {
                    $(campoDestino)[0].setSelectionRange(value.length, value.length);
                }
            }
        }
    });
}

export function eliminarTr(tableName) {
    return new Promise((resolve, reject) => {
        $(tableName).on('click', '#delete', function () {
            resolve($(this).closest('tr').remove());
        });
    });

}

export function isTbodyNotEmpty(nombreTabla) {
    let trLength = $(nombreTabla + " tbody tr").length;
    if (trLength > 0) {
        return true;
    } else {
        return false;
    }
}

export function formatPYG(number) {
    return parseInt(number).toLocaleString('es-PY', {style: 'currency', currency: 'PYG'});
}

export function numberFormat(number) {
    const formateador = new Intl.NumberFormat('es-PY', {
        style: 'decimal',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0
    });
    number = soloNumero(number);
    return formateador.format(number);
}

export function soloNumero(value) {
    let valor = value.valueOf();
    return valor.replace(/[^0-9-]/g, '');
}
export function agregarTfoot(tableName, value) {
     $(tableName+" tfoot").empty();
    const totalRow = `
  <tr>
    <td colspan="2" class="text-end">
      <h5>Total:</h5>
    </td>
    <td id="totalVenta" colspan="3" class="text-end fw-bold text-success">
      <h4>${formatPYG(value)}</h4>
    </td>
  </tr>
`;

    $(tableName).find("tfoot").empty().append(totalRow);

}

export function seleccionarAutoComplete(campo) {
    $(campo).on('keyup', function (e) {
        if (e.key === "Enter" || e.keyCode === 13) {
            var autocomplete = $(campo).autocomplete("widget");
            if (autocomplete.is(":visible")) {
                var firstItem = $(campo).autocomplete("widget").find("li.ui-menu-item:first");
                if (firstItem.length) {
                    firstItem.click();
                }
            }

        }
    });
}

import  "/js/dataTables.min.js";
import  "/js/dataTables.bootstrap5.min.js";
export function dataTableInit(campo) {
    $(campo).DataTable({
        language: {
            url: '/i18n/es-ES.json'
        },
        'order': [[0, 'DESC']]
    });
}

import "/js/datepicker-es.js";
export function datePickerInit(campo) {
    $.datepicker.setDefaults($.datepicker.regional["es"]);
    $(campo).datepicker({
        changeYear: true,
        dateFormat: 'yy-mm-dd'
    }
    );
}


