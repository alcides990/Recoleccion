$(document).ready(function () {
    var numeroPagina = 0;

    //filtrar tabla
    $("#txtBuscarServicio").keyup(function (e) {
        var filtro = $("#txtBuscarServicio").val().trim();
        if (filtro !== null) {


            var catidadRegistro = 5;
            var url = '/servicio/buscar';
            var datos = {filtro: filtro, numeroPagina: numeroPagina, catidadRegistro: catidadRegistro};
            $.ajax({
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
                            <td> ${valor.usuario.nombre} ${valor.usuario.apellido}</td>
                            <td> ${valor.fechaInicio}</td>
                            <td> ${valor.categoria.categoriaConTarifa}</td>
                            <td> ${valor.estado.estado}</td>
                            <td>
                                 <a href='/servicio/estadoCuenta/${valor.cuentaCorriente}' 
                                     class='btn btn-primario btn-sm' />EstadoCuenta 
                                  </a>
                            </td>
                             <td> 
                                 <a href='/comprobante/facturar/${valor.cuentaCorriente}' 
                                     class='btn btn-primario btn-sm' />Facturar
                                 </a>
                            </td>
                            <td> 
                                 <a 
                                    id="editar" data-id=${valor.cuentaCorriente}
                                    class="btn btn-info btn-sm"
                                    data-bs-toggle="modal"
                                    data-bs-target="#servicioModal"> 
                                    <i class="fa-regular fa-pen-to-square fa-lg"></i>
                                </a> 
                                <a  id="eliminar" data-url="/servicio/eliminar/" 
                                    th:data-id=${valor.cuentaCorriente}
                                    class='btn btn-danger eliminar btn-sm' > 
                                    <i class='fa-solid fa-trash-can'></i> 
                                </a> 
                            </td>
                        </tr>`;
                        $("#tbody").append(cargaTabla);
                    });
                },
                error: function (jqXHR, textStatus, errorThrown) {
//                alert('Error...');
                }
            });
        }
    });
//esta funciona redireciona a la lagina de inicio con el cambiando la cantidad de
// registro que con el valor seleccionado en el select 
    $("#cantElemento").change(function () {
        var cantElemento = $(this).val();
        var url = new URL(window.location.href);
        var searchParams = new URLSearchParams(url.search);
        searchParams.set('page', 0);
        searchParams.set('cantElemento', cantElemento);
        url.search = searchParams.toString();
        window.location.href = url;
    });


});


