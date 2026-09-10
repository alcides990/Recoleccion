$(function () {
    const nombresMeses = [
        'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
        'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
    ];
    const colores = ['#6c757d', '#bca007', '#495057', '#8d99ae', '#6b705c', '#9467bd', '#d77a61'];
    let periodo = 'dia';
    let graficoPeriodo;
    let graficoMedioPago;
    let graficoCobrador;


    function formatoGuaranies(valor) {
        return 'Gs. ' + Number(valor || 0).toLocaleString('es-PY', {
            minimumFractionDigits: 0,
            maximumFractionDigits: 0
        });
    }

    function etiquetaPeriodo(valor) {
        if (periodo === 'mes') {
            return nombresMeses[Number(valor) - 1] || valor;
        }
        return String(valor);
    }

    function mostrarControles() {
        $('#bloqueMesIngresosV2').toggleClass('d-none', periodo !== 'dia');
        $('#bloqueAnioIngresosV2').toggleClass('d-none', periodo === 'anio');
    }

    function crearGraficoPeriodo(datos) {
        const contexto = document.getElementById('graficoIngresosPeriodo');
        if (graficoPeriodo) {
            graficoPeriodo.destroy();
        }
        graficoPeriodo = new Chart(contexto, {
            type: 'bar',
            data: {
                labels: datos.map(item => etiquetaPeriodo(item.periodo)),
                datasets: [{
                    label: 'Ingresos',
                    data: datos.map(item => Number(item.importe || 0)),
                    backgroundColor: '#6c757d',
                    borderColor: '#555d64',
                    borderWidth: 1,
                    borderRadius: 5,
                    maxBarThickness: 42
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {display: false},
                    tooltip: {
                        callbacks: {
                            label: contextoTooltip => ' ' + formatoGuaranies(contextoTooltip.raw)
                        }
                    }
                },
                scales: {
                    x: {grid: {display: false}},
                    y: {
                        beginAtZero: true,
                        ticks: {callback: valor => formatoGuaranies(valor)},
                        grid: {color: 'rgba(108, 117, 125, .14)'}
                    }
                }
            }
        });
    }

    function crearGraficoMedioPago(datos, medios) {
        const periodos = [...new Set([...datos.map(item => Number(item.periodo)),
            ...medios.map(item => Number(item.periodo))])].sort((a, b) => a - b);
        const codigos = [...new Set(medios.map(item => Number(item.codigoMedio)))].sort((a, b) => a - b);
        const datasets = codigos.map(codigo => {
            const filas = medios.filter(item => Number(item.codigoMedio) === codigo);
            const importes = new Map(filas.map(item => [Number(item.periodo), Number(item.importe || 0)]));
            return {
                label: filas[0].nombre,
                data: periodos.map(valor => importes.get(valor) || 0),
                backgroundColor: colores[Math.abs(codigo) % colores.length],
                borderRadius: 4,
                maxBarThickness: 32
            };
        });
        const contexto = document.getElementById('graficoIngresosMedioPago');
        if (graficoMedioPago) {
            graficoMedioPago.destroy();
        }
        graficoMedioPago = new Chart(contexto, {
            type: 'bar',
            data: {
                labels: periodos.map(etiquetaPeriodo),
                datasets: datasets
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {position: 'bottom'},
                    tooltip: {
                        callbacks: {
                            label: contextoTooltip => contextoTooltip.dataset.label + ': ' + formatoGuaranies(contextoTooltip.raw)
                        }
                    }
                },
                scales: {
                    x: {grid: {display: false}},
                    y: {
                        beginAtZero: true,
                        ticks: {callback: valor => formatoGuaranies(valor)},
                        grid: {color: 'rgba(108, 117, 125, .14)'}
                    }
                }
            }
        });
    }

    function crearGraficoCobrador(datos) {
        const contexto = document.getElementById('graficoIngresosCobrador');
        if (graficoCobrador) {
            graficoCobrador.destroy();
        }
        graficoCobrador = new Chart(contexto, {
            type: 'doughnut',
            data: {
                labels: datos.map(item => item.cobrador || 'Sin cobrador'),
                datasets: [{
                    data: datos.map(item => Number(item.importe || 0)),
                    backgroundColor: datos.map((item, indice) => colores[indice % colores.length]),
                    borderColor: '#fff',
                    borderWidth: 3
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '58%',
                plugins: {
                    legend: {position: 'bottom', labels: {boxWidth: 12, padding: 14}},
                    tooltip: {
                        callbacks: {
                            label: contextoTooltip => contextoTooltip.label + ': ' + formatoGuaranies(contextoTooltip.raw)
                        }
                    }
                }
            }
        });
    }

    function actualizarResumen(respuesta) {
        $('#totalIngresosV2').text(formatoGuaranies(respuesta.total));
        $('#promedioIngresosV2').text(formatoGuaranies(respuesta.promedio));
        $('#maximoIngresosV2').text(formatoGuaranies(respuesta.maximo));
    }

    function mostrarError() {
        $('#mensajeIngresosV2').html('<div class="alert alert-danger mb-0">No fue posible recuperar los ingresos. Verifique su sesión e intente nuevamente.</div>');
    }

    function cargarIngresos() {
        mostrarControles();
        $('#mensajeIngresosV2').empty();
        $.getJSON('/reporte/ingresos-v2/datos', {
            periodo: periodo,
            anio: $('#ingresoAnio').val(),
            mes: $('#ingresoMes').val()
        }).done(function (respuesta) {
            const ingresos = respuesta.ingresos || [];
            const cobradores = respuesta.ingresosPorCobrador || [];
            $('#sinIngresosV2').toggleClass('d-none', ingresos.length > 0);
            $('#sinCobradoresIngresosV2').toggleClass('d-none', cobradores.length > 0);

            crearGraficoCobrador(cobradores);
            const medios = respuesta.ingresosPorMedioPago || [];
            $('#sinMediosIngresosV2').toggleClass('d-none', medios.length > 0);
            crearGraficoPeriodo(ingresos);
            crearGraficoMedioPago(ingresos, medios);
            actualizarResumen(respuesta);
        }).fail(mostrarError);
    }

    $('.ingresos-v2-tab').on('click', function () {
        periodo = $(this).data('periodo');
        $('.ingresos-v2-tab').removeClass('active');
        $(this).addClass('active');
        cargarIngresos();
    });

    $('#ingresoMes, #ingresoAnio').on('change', cargarIngresos);
    cargarIngresos();
});
