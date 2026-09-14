async function solicitar(ruta, opciones = {}) {
    const respuesta = await fetch('/egresos' + ruta, {...opciones, headers: {Accept: 'application/json', ...opciones.headers}});
    const esJson = respuesta.headers.get('Content-Type')?.includes('application/json');
    if (!respuesta.ok || !esJson) {
        const error = esJson ? await respuesta.json() : {};
        throw new Error(error.mensaje || 'No se pudo completar la operación. Verifique su sesión y los datos.');
    }
    return respuesta.json();
}

function guardar(ruta, datos) {
    return solicitar(ruta, {
        method: 'POST',
        headers: {'Content-Type': 'application/json', 'X-CSRF-TOKEN': $('#token').val()},
        body: JSON.stringify(datos)
    });
}

export const egresosApi = {
    listar: rango => solicitar('/datos?' + new URLSearchParams(rango)),
    detalle: id => solicitar('/' + id + '/datos'),
    guardar: (id, dato) => guardar('/guardar' + (id ? '?id=' + id : ''), dato),
    anular: id => guardar('/' + id + '/anular', {})
};

export const catalogosApi = {
    detalle: (clase, id) => solicitar('/catalogos/' + clase + '/' + id + '/datos'),
    eliminar: (clase, id) => guardar('/catalogos/' + clase + '/' + id + '/eliminar', {}),
    buscar: (clase, q = '') => solicitar('/catalogos/' + clase + '?' + new URLSearchParams({q})),
    guardar: (clase, dato) => guardar('/catalogos/' + clase, dato)
};
