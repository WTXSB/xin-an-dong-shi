import fs from 'node:fs/promises';
import path from 'node:path';

const endpoint = 'http://127.0.0.1:9444/json/list';
const outputDir = path.resolve('qa');
const pages = [
	['aboutProduct', 'aboutProduct'],
	['imgPredict', 'imgPredict'],
	['videoPredict', 'videoPredict'],
	['cameraPredict', 'cameraPredict'],
	['dataView', 'dataView'],
	['trashMap', 'trashMap'],
	['trashRecords', 'trashRecords'],
	['smartChat', 'smartChat'],
].filter(([name]) => !process.argv.includes('--records-only') || name === 'trashRecords');

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function captureFullPage(name, width) {
	await send('Runtime.evaluate', { expression: `(() => {
		window.__qaStyles = [];
		const scroller = document.querySelector('.layout-main-scroll');
		if (!scroller) return;
		const height = scroller.scrollHeight;
		let element = scroller;
		while (element) {
			window.__qaStyles.push([element, element.getAttribute('style')]);
			element.style.height = element === scroller ? height + 'px' : 'auto';
			element.style.maxHeight = 'none';
			element.style.overflow = 'visible';
			element = element.parentElement;
		}
		window.scrollTo(0, 0);
	})()` });
	await sleep(150);
	const dimensions = await send('Runtime.evaluate', { expression: `({ width: document.documentElement.clientWidth, height: Math.max(document.documentElement.scrollHeight, document.body.scrollHeight) })`, returnByValue: true });
	const screenshot = await send('Page.captureScreenshot', {
		format: 'png', fromSurface: true, captureBeyondViewport: true,
		clip: { x: 0, y: 0, ...dimensions.result.value, scale: 1 },
	});
	await fs.writeFile(path.join(outputDir, `healing-cards-${name}-full-${width}.png`), Buffer.from(screenshot.data, 'base64'));
	await send('Runtime.evaluate', { expression: `window.__qaStyles.forEach(([element, style]) => style === null ? element.removeAttribute('style') : element.setAttribute('style', style)); delete window.__qaStyles` });
}

async function getTarget() {
	for (let attempt = 0; attempt < 20; attempt += 1) {
		try {
			const targets = await fetch(endpoint).then((response) => response.json());
			const page = targets.find((target) => target.type === 'page');
			if (page) return page;
		} catch {}
		await sleep(250);
	}
	throw new Error('Edge DevTools target was not available.');
}

const target = await getTarget();
const socket = new WebSocket(target.webSocketDebuggerUrl);
const pending = new Map();
let nextId = 0;

socket.addEventListener('message', (event) => {
	const payload = JSON.parse(event.data);
	if (!payload.id || !pending.has(payload.id)) return;
	const { resolve, reject } = pending.get(payload.id);
	pending.delete(payload.id);
	if (payload.error) reject(new Error(payload.error.message));
	else resolve(payload.result);
});

await new Promise((resolve, reject) => {
	socket.addEventListener('open', resolve, { once: true });
	socket.addEventListener('error', reject, { once: true });
});

function send(method, params = {}) {
	const id = ++nextId;
	socket.send(JSON.stringify({ id, method, params }));
	return new Promise((resolve, reject) => pending.set(id, { resolve, reject }));
}

await send('Page.enable');
await send('Runtime.enable');
await send('Network.enable');
await send('Network.setCookies', { cookies: [
	{ name: 'token', value: 'codex-visual-qa', url: 'http://127.0.0.1:8100/', path: '/' },
	{ name: 'role', value: 'admin', url: 'http://127.0.0.1:8100/', path: '/' },
	{ name: 'userName', value: 'visual_qa', url: 'http://127.0.0.1:8100/', path: '/' },
] });
await send('Page.reload');
await sleep(1200);
await send('Emulation.setDeviceMetricsOverride', {
	width: 1440,
	height: 900,
	deviceScaleFactor: 1,
	mobile: false,
});
await sleep(1000);
await send('Runtime.evaluate', { expression: `sessionStorage.clear()` });

const report = [];
for (const [name, route] of pages) {
	await send('Page.navigate', { url: `http://127.0.0.1:8100/#/${route}` });
	await sleep(route === 'trashMap' ? 4500 : 3000);
	const metrics = await send('Runtime.evaluate', {
		expression: `(() => {
			const root = document.documentElement;
			const body = document.body;
			return {
				hash: location.hash,
				title: document.title,
				width: Math.max(root.scrollWidth, body.scrollWidth),
				viewport: root.clientWidth,
				height: Math.max(root.scrollHeight, body.scrollHeight),
				innerOverflow: [...document.querySelectorAll('[class]')].filter(e => e.clientWidth > 0 && e.scrollWidth > e.clientWidth + 3 && getComputedStyle(e).overflowX === 'visible').map(e => String(e.className)).slice(0, 20),
				navText: Array.from(document.querySelectorAll('.layout-navbars-container a, .layout-navbars-container .el-menu-item')).map((item) => item.textContent.trim()).filter(Boolean).slice(0, 20),
			};
		})()`,
		returnByValue: true,
	});
	const screenshot = await send('Page.captureScreenshot', {
		format: 'png',
		captureBeyondViewport: false,
		fromSurface: true,
	});
	await fs.writeFile(path.join(outputDir, `healing-cards-${name}-authenticated-1440.png`), Buffer.from(screenshot.data, 'base64'));
	if (metrics.result.value.hash !== '#/' + route) throw new Error('Unexpected route: ' + metrics.result.value.hash);
	await captureFullPage(name, 1440);
	report.push({ name, ...metrics.result.value, horizontalOverflow: metrics.result.value.width > metrics.result.value.viewport });
}

await send('Emulation.setDeviceMetricsOverride', {
	width: 390,
	height: 844,
	deviceScaleFactor: 1,
	mobile: true,
});
for (const [name, route] of pages) {
	await send('Page.navigate', { url: `http://127.0.0.1:8100/#/${route}` });
	await sleep(route === 'trashMap' ? 3500 : 2200);
	const metrics = await send('Runtime.evaluate', {
		expression: `(() => {
			const root = document.documentElement;
			const body = document.body;
			return {
				hash: location.hash,
				title: document.title,
				width: Math.max(root.scrollWidth, body.scrollWidth),
				viewport: root.clientWidth,
				height: Math.max(root.scrollHeight, body.scrollHeight),
				innerOverflow: [...document.querySelectorAll('[class]')].filter(e => e.clientWidth > 0 && e.scrollWidth > e.clientWidth + 3 && getComputedStyle(e).overflowX === 'visible').map(e => String(e.className)).slice(0, 20),
			};
		})()`,
		returnByValue: true,
	});
	const screenshot = await send('Page.captureScreenshot', {
		format: 'png',
		captureBeyondViewport: false,
		fromSurface: true,
	});
	await fs.writeFile(path.join(outputDir, `healing-cards-${name}-authenticated-390.png`), Buffer.from(screenshot.data, 'base64'));
	if (metrics.result.value.hash !== '#/' + route) throw new Error('Unexpected route: ' + metrics.result.value.hash);
	await captureFullPage(name, 390);
	report.push({ name: `${name}-mobile`, ...metrics.result.value, horizontalOverflow: metrics.result.value.width > metrics.result.value.viewport });
}

for (const width of (process.argv.includes('--records-only') ? [] : [1920, 1024, 768])) {
	await send('Emulation.setDeviceMetricsOverride', { width, height: width === 1920 ? 1080 : 900, deviceScaleFactor: 1, mobile: false });
	await send('Page.navigate', { url: 'http://127.0.0.1:8100/#/trashMap' });
	await sleep(2200);
	await captureFullPage('trashMap', width);
	const metrics = await send('Runtime.evaluate', { expression: `({ viewport: innerWidth, width: document.documentElement.scrollWidth, scrollerWidth: document.querySelector('.layout-main-scroll').scrollWidth })`, returnByValue: true });
	report.push({ name: 'trashMap-' + width, ...metrics.result.value });
}

await fs.writeFile(path.join(outputDir, process.argv.includes('--records-only') ? 'healing-records-report.json' : 'healing-cards-report.json'), `${JSON.stringify(report, null, 2)}\n`);
console.log(JSON.stringify(report, null, 2));
socket.close();
