import fs from 'node:fs/promises';

const qaDir = 'D:/xin-an-dong-shi-github-fresh-20261005/qa';
const targets = await (await fetch('http://127.0.0.1:9444/json/list')).json();
const target = targets.find((item) => item.type === 'page' && item.url.startsWith('http://127.0.0.1:8100/'));
if (!target) throw new Error('Local app page was not found.');

const socket = new WebSocket(target.webSocketDebuggerUrl);
await new Promise((resolve, reject) => {
	socket.addEventListener('open', resolve, { once: true });
	socket.addEventListener('error', reject, { once: true });
});

let nextId = 0;
const pending = new Map();
socket.addEventListener('message', (event) => {
	const message = JSON.parse(event.data);
	if (!message.id || !pending.has(message.id)) return;
	const request = pending.get(message.id);
	pending.delete(message.id);
	if (message.error) request.reject(new Error(JSON.stringify(message.error)));
	else request.resolve(message.result);
});
const send = (method, params = {}) => new Promise((resolve, reject) => {
	const id = ++nextId;
	pending.set(id, { resolve, reject });
	socket.send(JSON.stringify({ id, method, params }));
});

await send('Page.enable');
await send('Emulation.setDeviceMetricsOverride', {
	width: 1440,
	height: 900,
	deviceScaleFactor: 1,
	mobile: false,
});
await send('Page.navigate', { url: 'http://127.0.0.1:8100/#/trashMap' });
await new Promise((resolve) => setTimeout(resolve, 1800));

await send('Runtime.evaluate', {
	expression: `(() => {
		const scroller = document.querySelector('.layout-main-scroll');
		if (!scroller) return false;
		const contentHeight = scroller.scrollHeight;
		scroller.style.height = contentHeight + 'px';
		scroller.style.maxHeight = 'none';
		scroller.style.overflow = 'visible';
		let parent = scroller.parentElement;
		while (parent && parent !== document.documentElement) {
			parent.style.height = 'auto';
			parent.style.maxHeight = 'none';
			parent.style.overflow = 'visible';
			parent = parent.parentElement;
		}
		document.documentElement.style.height = 'auto';
		document.documentElement.style.overflow = 'visible';
		window.scrollTo(0, 0);
		return true;
	})()`,
	returnByValue: true,
});
await new Promise((resolve) => setTimeout(resolve, 300));

const dimensions = await send('Runtime.evaluate', {
	expression: `JSON.stringify({
		width: Math.max(document.documentElement.scrollWidth, document.body.scrollWidth),
		height: Math.max(document.documentElement.scrollHeight, document.body.scrollHeight),
		scrollables: [...document.querySelectorAll('*')]
			.filter((element) => element.scrollHeight > element.clientHeight + 5)
			.map((element) => ({
				tag: element.tagName,
				className: typeof element.className === 'string' ? element.className : '',
				clientHeight: element.clientHeight,
				scrollHeight: element.scrollHeight,
				overflowY: getComputedStyle(element).overflowY
			}))
			.sort((a, b) => b.scrollHeight - a.scrollHeight)
			.slice(0, 8)
	})`,
	returnByValue: true,
});
const { width, height, scrollables } = JSON.parse(dimensions.result.value);
const screenshot = await send('Page.captureScreenshot', {
	format: 'png',
	fromSurface: true,
	captureBeyondViewport: true,
	clip: { x: 0, y: 0, width, height, scale: 1 },
});

await fs.writeFile(`${qaDir}/trashMap-fullpage-1440.png`, Buffer.from(screenshot.data, 'base64'));
await fs.writeFile(`${qaDir}/trashMap-fullpage-report.json`, JSON.stringify({ width, height, scrollables }, null, 2));
console.log(JSON.stringify({ screenshot: `${qaDir}/trashMap-fullpage-1440.png`, width, height, scrollables }, null, 2));
socket.close();
