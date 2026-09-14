import {limpiar} from '/js/utils.js';
import {cargarDatosComprobante, limpiarComprobante} from '/js/comprobante.js';

$("#txtBuscarServicio").keyup(function (e) {
    filtrarTablaServicio();
});

$("#cuentaCorriente").autocomplete({
    minLength: 4,
    source: function (request, response) {

        buscarServicio(request.term)
                .then(data => {

                    response($.map(data.content, function (item) {
                        return {
                            label: `${item.cuentaCorriente} ${item.usuario.nombre} ${item.usuario.apellido?? ''}`, // lo que se muestra
                            value: item.cuentaCorriente
                        };
                    }));

                })
                .catch(() => response([]));
    },
    select: function (event, ui) {
        limpiarComprobante();
        let cuentaCorriente = ui.item.value;
        let datos = {cuentaCorriente: cuentaCorriente};
        cargarDatosComprobante(datos, '/comprobante/facturar');
    }
});

$("#razonSocial").autocomplete({
    minLength: 2,
    source: function (request, response) {

        buscarServicio(request.term)
                .then(data => {

                    response($.map(data.content, function (item) {
                        return {
                            label: `${item.cuentaCorriente} ${item.usuario.nombre} ${item.usuario.apellido?? ''}`, // lo que se muestra
                            value: item.cuentaCorriente

                        };
                    }));

                })
                .catch(() => response([]));
    },
    select: function (event, ui) {
        limpiarComprobante();
        let cuentaCorriente = ui.item.value;
        let datos = {cuentaCorriente: cuentaCorriente};
        cargarDatosComprobante(datos, '/comprobante/facturar');
        $("#cuentaCorriente").val(cuentaCorriente);
    }
});

function filtrarTablaServicio() {
    var numeroPagina = 0;
    var filtro = $("#txtBuscarServicio").val().trim();
    if (filtro !== null) {
        let catidadRegistro = $("#cantidadRegistro").val();
        let url = '/servicio/buscar';
        let datos = {filtro: filtro, numeroPagina: numeroPagina, catidadRegistro: catidadRegistro};
        let token = $("#token").val();
        $.ajax({
            headers: {
                'X-CSRF-TOKEN': token
            },
            url: url,
            data: JSON.stringify(datos),
            type: "post",
            dataType: "json",
            contentType: 'application/json',
            success: function (data) {
                $("td").closest('td').remove();
                $.each(data.content, function (llave, valor) {
                    var cargaTabla = `
                        <tr>
                            <td> ${valor.cuentaCorriente} </td>
                            <td> ${valor.usuario.nombre} ${valor.usuario.apellido??''}</td>
                            <td> ${valor.fechaInicio}</td>
                            <td> ${valor.categoria.nombreCategoria}-${valor.categoria.tarifa}</td>
                            <td> ${valor.estado.estado}</td>
                            <td>
                                 <a href="#"
   onclick="window.location.href='/servicio/estadoCuenta?cuentaCorriente=' + encodeURIComponent('${valor.cuentaCorriente}')" 
                                     class='btn btn-primario btn-sm' />EstadoCuenta 
                                  </a>
  
                                 <a 
                                    id="editar" data-id=${valor.cuentaCorriente}
                                    class="btn btn-info btn-sm"
                                    data-bs-toggle="modal"
                                    data-bs-target="#servicioModal"> 
                                    <i class="fa-regular fa-pen-to-square fa-lg"></i>
                                </a> 
                                <a  id="eliminar" data-url="/servicio/eliminar" 
                                    data-id=${valor.cuentaCorriente}
                                    class='btn btn-danger eliminar btn-sm' > 
                                    <i class='fa-solid fa-trash-can'></i> 
                                </a> 
                            </td>
                        </tr>`;
                    $("#tbody").append(cargaTabla);
                });
            },
            error: function (jqXHR, textStatus, errorThrown) {
            }
        });
    }
}

export function buscarServicio(filtro) {
    if (filtro !== null) {
        return new Promise((resolve, reject) => {
            let token = $("#token").val();
            let url = '/servicio/buscar';
            let datos = {filtro: filtro, numeroPagina: 0, catidadRegistro: 30};
            $.ajax({
                headers: {
                    'X-CSRF-TOKEN': token
                },
                url: url,
                data: JSON.stringify(datos),
                type: "post",
                dataType: "json",
                contentType: 'application/json',
                success: function (data) {
                    resolve(data);
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    reject({
                        jqXHR,
                        textStatus,
                        errorThrown
                    });
                }
            });
        });
    }
}






