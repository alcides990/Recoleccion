const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const {chromium} = require(process.env.PLAYWRIGHT_MODULE || 'playwright');

const root = process.cwd();
const staticRoot = path.join(root, 'src/main/resources/static');
const output = path.join(root, 'target/egresos-qa');
const layout = fs.readFileSync(path.join(root, 'src/main/resources/templates/layout/layout.html'), 'utf8');
const template = fs.readFileSync(path.join(root, 'src/main/resources/templates/egresos/inicio.html'), 'utf8');
const read = relative => fs.readFileSync(path.join(staticRoot, relative));

function fixture() {
    const head = `<head><meta charset="UTF-8">${[
        'bootstrap.min.css', 'jquery-ui.min.css', 'jquery.dataTables.min.css',
        'estilo.css', 'diseno-global.css', 'menu-layout.css'
    ].map(file => `<link rel="stylesheet" href="/css/${file}">`).join('')}</head>`;
    const aside = layout.match(/<aside[^>]*>[\s\S]*?<\/aside>/)[0].replaceAll('th:href=', 'href=');
    const scripts = ['jquery-3.7.0.min.js', 'jquery-ui.min.js', 'jquery.dataTables.min.js', 'menu-lateral.js']
        .map(file => `<script src="/js/${file}"></script>`).join('');
    return template.replace(/<head[^>]*><\/head>/, head)
        .replace(/<header[^>]*><\/header>/, '<header><button id="menu-toggle"><span class="barra"></span><span class="barra"></span><span class="barra"></span></button></header>')
        .replace(/<aside[^>]*><\/aside>/, aside)
        .replace(/<footer[^>]*><\/footer>/, '<footer><input id="token" value="test"></footer>' + scripts);
}

(async () => {
    fs.mkdirSync(output, {recursive: true});
    const browser = await chromium.launch({headless: true, executablePath: process.env.CHROME_PATH});
    const page = await browser.newPage();
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    const catalogos = {
        TIPO: [{id: 1, nombre: 'Mantenimiento'}],
        PROVEEDOR: [{id: 2, nombre: 'Proveedor existente'}],
        PRODUCTO: [{id: 3, nombre: 'Aceite motor', monto: 10.01}]
    };
    let guardado;
    const gastos = [{id: 1, fecha: '2026-09-08', tipo_id: 1, tipo: 'Mantenimiento', proveedor: 'Proveedor existente',
        comprobante: '001', observacion: '', total: 25.03, anulado: false,
        detalles: [{producto: 'Aceite motor', cantidad: 2.5, precio: 10.01}]}];
    await page.route('**/*', async route => {
        const url = new URL(route.request().url());
        const json = body => route.fulfill({contentType: 'application/json', body: JSON.stringify(body)});
        if (url.hostname !== 'egresos.test') return route.fulfill({body: ''});
        if (/^\/egresos(?:\/(?:agregar|editar\/\d+|catalogos\/(?:TIPO|PROVEEDOR|PRODUCTO)\/(?:lista|agregar|editar\/\d+)))?$/.test(url.pathname)) return route.fulfill({contentType: 'text/html', body: fixture()});
        if (url.pathname === '/egresos/datos') return json(gastos);
        if (url.pathname === '/egresos/1/datos') return json(gastos[0]);
        if (url.pathname === '/egresos/guardar') {
            guardado = route.request().postDataJSON();
            return json({id: 2});
        }
        if (url.pathname.startsWith('/egresos/catalogos/')) {
            const segmentos = url.pathname.split('/');
            const clase = segmentos[3];
            if (segmentos[5] === 'datos') return json(catalogos[clase].find(dato => dato.id === Number(segmentos[4])));
            if (route.request().method() === 'POST') return json({id: 4});
            const query = (url.searchParams.get('q') || '').toLowerCase();
            return json(catalogos[clase].filter(item => item.nombre.toLowerCase().includes(query)));
        }
        const relative = decodeURIComponent(url.pathname).replace(/^\//, '');
        const resolved = path.resolve(staticRoot, relative);
        if (!resolved.startsWith(staticRoot + path.sep) || !fs.existsSync(resolved)) return route.fulfill({status: 404});
        const types = {'.css': 'text/css', '.js': 'application/javascript', '.json': 'application/json', '.png': 'image/png'};
        return route.fulfill({body: read(relative), contentType: types[path.extname(relative)] || 'application/octet-stream'});
    });

    try {
        for (const width of [390, 768, 1000, 1020, 1021, 1280, 1920]) {
            await page.setViewportSize({width, height: 1000});
            await page.goto('http://egresos.test/egresos');
            await page.waitForSelector('#tablaEgresos tbody .editar');
            const compact = width <= 1020;
            if (compact) await page.click('#menu-toggle');
            else await page.hover('#menu_aside');
            await page.waitForTimeout(250);
            const bounds = await page.evaluate(() => {
                const menu = document.querySelector('#menu_aside').getBoundingClientRect();
                const main = document.querySelector('main').getBoundingClientRect();
                return {menuRight: menu.right, menuBottom: menu.bottom, mainLeft: main.left,
                    mainTop: main.top, mainRight: main.right, scroll: document.documentElement.scrollWidth};
            });
            assert.ok(compact ? bounds.mainTop >= bounds.menuBottom : bounds.mainLeft >= bounds.menuRight,
                `El menú se superpone a ${width}px: ${JSON.stringify(bounds)}`);
            assert.ok(bounds.mainRight <= width + 1 && bounds.scroll <= width + 1, `Desbordamiento a ${width}px`);
            if ([390, 768, 1280].includes(width)) await page.screenshot({path: path.join(output, `menu-${width}.png`), fullPage: true});
            if (compact) {
                await page.keyboard.press('Escape');
                assert.equal(await page.locator('#menu-toggle').getAttribute('aria-expanded'), 'false');
            }
        }

        await page.setViewportSize({width: 1280, height: 1000});
        await page.click('#nuevoEgreso');
        await page.selectOption('#tipoEgreso', '1');
        await page.fill('#proveedorEgreso', 'Proveedor');
        await page.waitForSelector('.ui-autocomplete .ui-menu-item');
        await page.getByText('Proveedor existente', {exact: true}).last().click();
        await page.fill('#proveedorEgreso', 'Proveedor nuevo');
        await page.fill('#insumoEgreso', 'Aceite');
        await page.waitForSelector('.ui-autocomplete .ui-menu-item');
        await page.getByText('Aceite motor', {exact: true}).last().click();
        assert.equal(await page.inputValue('#montoInsumo'), '10.01');
        await page.fill('#cantidadInsumo', '2.5');
        await page.press('#cantidadInsumo', 'Enter');
        assert.equal(await page.locator('#lineasEgreso tr').count(), 1);
        assert.match(await page.locator('#totalEgreso').innerText(), /25,03/);
        await page.fill('#insumoEgreso', 'Aceite motor');
        await page.fill('#montoInsumo', '10.01');
        await page.fill('#cantidadInsumo', '1');
        await page.press('#cantidadInsumo', 'Enter');
        assert.equal(await page.locator('#lineasEgreso tr').count(), 1);
        assert.match(await page.locator('#totalEgreso').innerText(), /35,03/);
        await page.fill('#insumoEgreso', 'Servicio nuevo');
        await page.fill('#montoInsumo', '5');
        await page.fill('#cantidadInsumo', '1');
        await page.click('#guardarEgreso');
        assert.match(await page.locator('#mensajeEgreso').innerText(), /pendiente/);
        await page.press('#cantidadInsumo', 'Enter');
        assert.equal(await page.locator('#lineasEgreso tr').count(), 2);
        await page.screenshot({path: path.join(output, 'gasto-formulario-bodega.png'), fullPage: true});
        await page.click('#guardarEgreso');
        await page.waitForURL('http://egresos.test/egresos');
        assert.equal(guardado.proveedor, 'Proveedor nuevo');
        assert.deepEqual(guardado.detalles, [
            {producto: 'Aceite motor', cantidad: 3.5, precio: 10.01},
            {producto: 'Servicio nuevo', cantidad: 1, precio: 5}
        ]);
        await page.click('#tablaEgresos .editar');
        await page.waitForFunction(() => document.querySelector('#tipoEgreso').value === '1');
        assert.match(await page.locator('#lineasEgreso').innerText(), /Aceite motor/);
        await page.goto('http://egresos.test/egresos/catalogos/PROVEEDOR/lista');
        await page.waitForSelector('#filasCatalogo .editarCatalogo');
        await page.screenshot({path: path.join(output, 'proveedores-bodega.png'), fullPage: true});
        await page.click('#filasCatalogo .editarCatalogo');
        await page.waitForFunction(() => document.querySelector('#nombreCatalogo').value === 'Proveedor existente');
        assert.equal(await page.inputValue('#nombreCatalogo'), 'Proveedor existente');
        await page.screenshot({path: path.join(output, 'proveedor-formulario-bodega.png'), fullPage: true});
        assert.deepEqual(errors, []);
        console.log('OK: 7 resoluciones sin superposición; autocompletado, alta, edición y catálogos sin errores JavaScript.');
    } finally {
        await browser.close();
    }
})().catch(error => {console.error(error); process.exit(1);});
