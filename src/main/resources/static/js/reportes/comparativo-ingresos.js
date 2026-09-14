import {datePickerInit} from '/js/utils.js';

$(function () {
    const meses = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
        'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];
    let grafico;
    let respuestaActual = null;

    const moneda = valor => 'Gs. ' + Number(valor || 0).toLocaleString('es-PY', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 0
    });
    const entero = valor => Number(valor || 0).toLocaleString('es-PY');
    const iso = fecha => fecha.getFullYear() + '-' + String(fecha.getMonth() + 1).padStart(2, '0')
            + '-' + String(fecha.getDate()).padStart(2, '0');
    const ym = fecha => fecha.getFullYear() + '-' + String(fecha.getMonth() + 1).padStart(2, '0');

    function valorFila(fila) {
        return $('#comparativoMetrica').val() === 'cantidad'
                ? Number(fila.cantidad || 0)
                : Number(fila.importe || 0);
    }

    function formato(valor) {
        return $('#comparativoMetrica').val() === 'cantidad' ? entero(valor) : moneda(valor);
    }

    function etiquetaPeriodo(valor, escala) {
        if (escala === 'MES') {
            return meses[Number(valor) - 1] || valor;
        }
        return String(valor).padStart(2, '0');
    }

    function etiquetaRango(desde, hasta) {
        if (!desde || !hasta) {
            return '';
        }
        return desde === hasta ? desde : desde + ' / ' + hasta;
    }

    function fechaIso(fecha) {
        const partes = String(fecha || '').split('-').map(Number);
        return partes.length === 3 ? new Date(partes[0], partes[1] - 1, partes[2]) : null;
    }

    function esMesCompleto(desde, hasta) {
        const inicio = fechaIso(desde);
        const fin = fechaIso(hasta);
        if (!inicio || !fin || inicio.getDate() !== 1) {
            return false;
        }
        return fin.getFullYear() === inicio.getFullYear()
                && fin.getMonth() === inicio.getMonth()
                && fin.getDate() === new Date(inicio.getFullYear(), inicio.getMonth() + 1, 0).getDate();
    }

    function etiquetaPeriodoSeleccionado(desde, hasta) {
        const inicio = fechaIso(desde);
        const fin = fechaIso(hasta);
        if (!inicio || !fin) {
            return '';
        }
        if (inicio.getMonth() === 0 && inicio.getDate() === 1
                && fin.getMonth() === 11 && fin.getDate() === 31
                && inicio.getFullYear() === fin.getFullYear()) {
            return String(inicio.getFullYear());
        }
        if (esMesCompleto(desde, hasta)) {
            return meses[inicio.getMonth()] + ' ' + inicio.getFullYear();
        }
        return etiquetaRango(desde, hasta);
    }

    function iniciarPeriodos() {
        const hoy = new Date();
        const mesAnterior = new Date(hoy.getFullYear(), hoy.getMonth() - 1, 1);
        $('#comparativoMesA').val(ym(hoy));
        $('#comparativoMesB').val(ym(mesAnterior));
        $('#comparativoAnioA').val(hoy.getFullYear());
        $('#comparativoAnioB').val(hoy.getFullYear() - 1);
        $('#comparativoDesdeA').val(iso(new Date(hoy.getFullYear(), hoy.getMonth(), 1)));
        $('#comparativoHastaA').val(iso(hoy));
        $('#comparativoDesdeB').val(iso(new Date(hoy.getFullYear(), hoy.getMonth() - 1, 1)));
        $('#comparativoHastaB').val(iso(new Date(hoy.getFullYear(), hoy.getMonth(), 0)));
    }

    function actualizarCamposPeriodo() {
        const tipo = $('#comparativoTipo').val();
        $('.comparativo-periodos').addClass('d-none');
        $('.comparativo-periodos-' + tipo).removeClass('d-none');
    }

    function parametros() {
        const tipo = $('#comparativoTipo').val();
        const datos = {
            tipo: tipo,
            agrupacion: $('#comparativoAgrupacion').val()
        };
        if (tipo === 'mes') {
            const [anioA, mesA] = ($('#comparativoMesA').val() || '').split('-');
            const [anioB, mesB] = ($('#comparativoMesB').val() || '').split('-');
            Object.assign(datos, {anioA: anioA, mesA: mesA, anioB: anioB, mesB: mesB});
        } else if (tipo === 'anio') {
            Object.assign(datos, {
                anioA: $('#comparativoAnioA').val(),
                anioB: $('#comparativoAnioB').val()
            });
        } else {
            Object.assign(datos, {
                desdeA: $('#comparativoDesdeA').val(),
                hastaA: $('#comparativoHastaA').val(),
                desdeB: $('#comparativoDesdeB').val(),
                hastaB: $('#comparativoHastaB').val()
            });
        }
        return datos;
    }

    function filasFiltradas(serie) {
        const entidad = $('#comparativoEntidad').val();
        return (respuestaActual?.filas || []).filter(fila => fila.serie === serie
            && (!entidad || fila.nombre === entidad));
    }

    function sumar(filas) {
        return filas.reduce((total, fila) => total + valorFila(fila), 0);
    }

    function actualizarSelectorEntidades(filas) {
        const selector = $('#comparativoEntidad');
        const actual = selector.val();
        const entidades = [...new Set(filas.map(fila => fila.nombre || 'Sin especificar'))].sort();
        selector.empty().append($('<option>', {value: '', text: 'Total general'}));
        entidades.forEach(nombre => selector.append($('<option>', {value: nombre, text: nombre})));
        if (actual && entidades.includes(actual)) {
            selector.val(actual);
        }
    }

    function mapaPorPeriodo(filas) {
        const mapa = new Map();
        filas.forEach(fila => mapa.set(Number(fila.periodo), (mapa.get(Number(fila.periodo)) || 0) + valorFila(fila)));
        return mapa;
    }

    function etiquetasPeriodos() {
        if (respuestaActual?.escala === 'MES') {
            return Array.from({length: 12}, (_, indice) => indice + 1);
        }
        const periodos = new Set((respuestaActual?.filas || []).map(fila => Number(fila.periodo)));
        const maximo = Math.max(...periodos, 31);
        return Array.from({length: maximo}, (_, indice) => indice + 1);
    }

    function redibujarGrafico() {
        if (!respuestaActual) {
            return;
        }
        const etiquetaA = etiquetaPeriodoSeleccionado(respuestaActual.desdeA, respuestaActual.hastaA);
        const etiquetaB = etiquetaPeriodoSeleccionado(respuestaActual.desdeB, respuestaActual.hastaB);
        const filasA = filasFiltradas('A');
        const filasB = filasFiltradas('B');
        const totalA = sumar(filasA);
        const totalB = sumar(filasB);
        const periodoA = mapaPorPeriodo(filasA);
        const periodoB = mapaPorPeriodo(filasB);
        const periodos = etiquetasPeriodos();
        const contexto = document.getElementById('graficoComparativoIngresos');
        const hayDatos = totalA > 0 || totalB > 0;

        $('#sinComparativoIngresos').toggleClass('d-none', hayDatos);
        $('#comparativoEtiquetaTotalA').text('Total ' + etiquetaA);
        $('#comparativoEtiquetaTotalB').text('Total ' + etiquetaB);
        $('#comparativoTablaA').text(etiquetaA);
        $('#comparativoTablaB').text(etiquetaB);
        $('#comparativoTotalA').text(formato(totalA));
        $('#comparativoTotalB').text(formato(totalB));
        $('#comparativoDiferencia').text(formato(totalA - totalB));

        if (grafico) {
            grafico.destroy();
        }
        grafico = new Chart(contexto, {
            type: 'bar',
            data: {
                labels: periodos.map(valor => etiquetaPeriodo(valor, respuestaActual.escala)),
                datasets: [{
                    label: etiquetaA,
                    data: periodos.map(valor => periodoA.get(valor) || 0),
                    backgroundColor: $('#comparativoColorA').val(),
                    borderRadius: 4,
                    maxBarThickness: 34
                }, {
                    label: etiquetaB,
                    data: periodos.map(valor => periodoB.get(valor) || 0),
                    backgroundColor: $('#comparativoColorB').val(),
                    borderRadius: 4,
                    maxBarThickness: 34
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {position: 'bottom'},
                    tooltip: {callbacks: {label: item => item.dataset.label + ': ' + formato(item.raw)}}
                },
                scales: {
                    x: {grid: {display: false}},
                    y: {
                        beginAtZero: true,
                        ticks: {callback: valor => formato(valor)},
                        grid: {color: 'rgba(108, 117, 125, .14)'}
                    }
                }
            }
        });
    }

    function actualizarTabla() {
        const cuerpo = $('#comparativoTablaCuerpo').empty();
        const totales = new Map();
        (respuestaActual?.filas || []).forEach(function (fila) {
            const nombre = fila.nombre || 'Sin especificar';
            if (!totales.has(nombre)) {
                totales.set(nombre, {a: 0, b: 0});
            }
            const item = totales.get(nombre);
            if (fila.serie === 'A') {
                item.a += valorFila(fila);
            } else {
                item.b += valorFila(fila);
            }
        });
        $('#comparativoColumnaGrupo').text($('#comparativoAgrupacion').val() === 'ZONA' ? 'Zona' : 'Cobrador');
        if (!totales.size) {
            cuerpo.append($('<tr>').append($('<td>', {colspan: 4, class: 'text-center text-muted py-3'})
                    .text('Sin datos para comparar.')));
            return;
        }
        [...totales.entries()].sort((a, b) => (b[1].a + b[1].b) - (a[1].a + a[1].b))
                .forEach(function ([nombre, item]) {
                    cuerpo.append($('<tr>')
                            .append($('<td>').text(nombre))
                            .append($('<td>', {class: 'text-end'}).text(formato(item.a)))
                            .append($('<td>', {class: 'text-end'}).text(formato(item.b)))
                            .append($('<td>', {class: 'text-end'}).text(formato(item.a - item.b))));
                });
    }

    function mostrarError(texto) {
        $('#mensajeComparativoIngresos').html('<div class="alert alert-danger mb-0">' + texto + '</div>');
    }

    function cargarComparativo() {
        actualizarCamposPeriodo();
        $('#mensajeComparativoIngresos').empty();
        $.getJSON('/reporte/ingresos-v2/comparativo/datos', parametros())
                .done(function (respuesta) {
                    respuestaActual = respuesta;
                    actualizarSelectorEntidades(respuesta.filas || []);
                    redibujarGrafico();
                    actualizarTabla();
                })
                .fail(function () {
                    mostrarError('No fue posible recuperar el comparativo. Verifique los períodos seleccionados.');
                });
    }

    datePickerInit('#comparativoDesdeA');
    datePickerInit('#comparativoHastaA');
    datePickerInit('#comparativoDesdeB');
    datePickerInit('#comparativoHastaB');
    iniciarPeriodos();
    actualizarCamposPeriodo();
    $('#comparativoTipo, #comparativoAgrupacion, #comparativoMetrica, #comparativoMesA, #comparativoMesB, '
            + '#comparativoAnioA, #comparativoAnioB, #comparativoDesdeA, #comparativoHastaA, '
            + '#comparativoDesdeB, #comparativoHastaB').on('change', cargarComparativo);
    $('#comparativoEntidad, #comparativoColorA, #comparativoColorB').on('change', function () {
        redibujarGrafico();
        actualizarTabla();
    });
    cargarComparativo();
});
