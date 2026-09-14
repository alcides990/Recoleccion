$(document).ready(function () {

    $("#buscarUsuario").on("input", function () {
        $("#codigoUsuario").val("");
        $("#numeroDocumento").val("");
    });

    $("#buscarUsuario").autocomplete({
        source: function (request, response) {
            let token = $("#token").val();
            $.ajax({
                headers: {
                    'X-CSRF-TOKEN': token
                },
                url: "/usuario/buscar/" + request.term,
                dataType: "json",
                data: {
                    term: request.term
                },
                success: function (data) {
                    response($.map(data, function (item) {
                        return {
                            codigoUsuario: item.codigoUsuario,
                            numeroDocumento: item.numeroDocumento,
                            label: item.numeroDocumento + ' ' + item.nombre + ' ' + item.apellido,
                            usuario: item.nombre + ' ' + item.apellido
                        };
                    }));
                }
            });
        },
        minLength: 1,
        select: function (event, ui) {
            $("#buscarUsuario").val(ui.item.usuario);
            $("#numeroDocumento").val(ui.item.numeroDocumento);
            $("#codigoUsuario").val(ui.item.codigoUsuario);
            return false;
        }
    });

//Filtar tabla de usuario

    $("#txtBuscar").keyup(function (e) {
        var filtro = $("#txtBuscar").val();
        if (filtro.length > 1 || filtro.length === 0) {
            var numeroPagina = 0;
            var catidadRegistro = 5;
            var url = '/usuario/listar/pagina';
            var datos = {filtro: filtro, numeroPagina: numeroPagina, catidadRegistro: catidadRegistro};
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
//                    console.log(data);
                    $("tbody").empty();
                    $.each(data.content, function (llave, valor) {
                        var cargaTabla =
                                ` <tr> 
                                 <td> ${valor.codigoUsuario}
                                </td><td>  ${valor.numeroDocumento} 
                                 </td><td>  ${valor.nombre} ${valor.apellido??''}
                                </td><td>  ${valor.celular} 
                                 </td><td>  ${valor.correo??''}
                                </td><td>  ${valor.barrio} 
                                 </td><td>  ${valor.direccion} 
                                 </td><td>  ${valor.nombreSucursal} 
                                </td> <td> <a href='/usuario/editar/${valor.codigoUsuario}'   class='btn btn-info btn-md' /> <i class='fa-regular fa-pen-to-square'> </i> 
                     <a id='eliminar' data-url='/usuario/eliminar/' data-id=${valor.codigoUsuario}  class='btn btn-danger btn-md'>  <i class='fa-solid fa-trash-can'></i> </a> 
                    </td> 
                                 </tr>`;

                        $("#tbody").append(cargaTabla);
                    });



                },
                error: function (jqXHR, textStatus, errorThrown) {
                    alert('Error...' + jqXHR);
                }
            });
        }
    });


});
