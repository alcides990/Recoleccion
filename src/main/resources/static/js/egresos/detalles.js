import {catalogosApi} from './api.js';
import {escapar, moneda} from './vista.js';

const normalizar = nombre => nombre.trim().replace(/\s+/g, ' ').toLocaleLowerCase('es');

export function iniciarDetalles() {
    const cuerpo = $('#lineasEgreso');
    const formulario = $('#formDetalleEgreso');
    let lineas = [];
    let seleccionado = null;
    let edicion = null;
    let bloqueado = false;

    function dibujar() {
        cuerpo.html(lineas.map((linea, indice) => `<tr>
            <td>${escapar(linea.productoId || 'Nuevo')}</td><td>${escapar(linea.producto)}</td>
            <td>${linea.cantidad}</td><td class="text-nowrap">${moneda(linea.precio)}</td>
            <td class="text-end text-nowrap">${moneda(subtotal(linea))}</td>
            <td>${bloqueado ? '' : `<button type="button" class="btn btn-info btn-sm editarLinea" data-indice="${indice}" aria-label="Editar detalle"><i class="fa-solid fa-pen"></i> Editar</button>
                <button type="button" class="btn btn-danger btn-sm quitar" data-indice="${indice}" aria-label="Quitar detalle"><i class="fa-solid fa-trash-can"></i> Quitar</button>`}</td>
        </tr>`).join(''));
        $('#totalEgreso').text(moneda(lineas.reduce((total, linea) => total + subtotal(linea), 0)));
    }

    function subtotal(linea) { return Math.round(linea.cantidad * linea.precio * 100) / 100; }

    function limpiar() {
        formulario[0].reset();
        seleccionado = null;
        edicion = null;
        $('#agregarLinea').text('Agregar');
    }

    function agregar(evento) {
        evento.preventDefault();
        if (bloqueado || !formulario[0].reportValidity()) return;
        const producto = $('#insumoEgreso').val().trim().replace(/\s+/g, ' ');
        if (!producto) return;
        const nueva = {producto, productoId: seleccionado?.id,
            cantidad: Number($('#cantidadInsumo').val()), precio: Number($('#montoInsumo').val())};
        if (edicion !== null) {
            lineas[edicion] = nueva;
        } else {
            const existente = lineas.find(linea => normalizar(linea.producto) === normalizar(producto));
            if (existente) {
                existente.cantidad = Math.round((existente.cantidad + nueva.cantidad) * 1000) / 1000;
                existente.precio = nueva.precio;
            } else lineas.push(nueva);
        }
        limpiar();
        dibujar();
        $('#insumoEgreso').trigger('focus');
    }

    $('#insumoEgreso').autocomplete({
        minLength: 1, delay: 250,
        source: (consulta, responder) => catalogosApi.buscar('PRODUCTO', consulta.term)
            .then(datos => responder(datos.map(dato => ({label: dato.nombre, value: dato.nombre, dato}))))
            .catch(() => responder([])),
        select: (_, opcion) => {
            seleccionado = opcion.item.dato;
            $('#insumoEgreso').val(seleccionado.nombre);
            $('#montoInsumo').val(Number(seleccionado.monto) > 0 ? seleccionado.monto : '');
            $('#cantidadInsumo').trigger('focus');
            return false;
        }
    }).on('input', () => { seleccionado = null; });
    $('#montoInsumo').on('keydown', evento => {
        if (evento.key === 'Enter') { evento.preventDefault(); $('#cantidadInsumo').trigger('focus'); }
    });
    formulario.on('submit', agregar);
    cuerpo.on('click', '.quitar', function () {
        lineas.splice(Number($(this).data('indice')), 1);
        limpiar(); dibujar();
    }).on('click', '.editarLinea', function () {
        edicion = Number($(this).data('indice'));
        const linea = lineas[edicion];
        seleccionado = linea.productoId ? {id: linea.productoId} : null;
        $('#insumoEgreso').val(linea.producto);
        $('#montoInsumo').val(linea.precio);
        $('#cantidadInsumo').val(linea.cantidad);
        $('#agregarLinea').text('Actualizar');
        $('#insumoEgreso').trigger('focus');
    });

    return {
        leer: () => lineas.map(({producto, cantidad, precio}) => ({producto, cantidad, precio})),
        pendiente: () => Boolean($('#insumoEgreso').val().trim() || $('#montoInsumo').val() || $('#cantidadInsumo').val()),
        cargar: (datos, soloLectura = false) => {
            lineas = datos.map(dato => ({...dato}));
            bloqueado = soloLectura;
            limpiar();
            formulario.toggleClass('d-none', bloqueado);
            dibujar();
        }
    };
}
