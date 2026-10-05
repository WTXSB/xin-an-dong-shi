import fs from 'node:fs/promises';

const qaDir = 'D:/xin-an-dong-shi-github-fresh-20261005/qa';
const targets = await (await fetch('http://127.0.0.1:9444/json/list')).json();
const target = targets.find((item) => item.type === 'page' && item.url.startsWith('http://127.0.0.1:8100/'));

if (!target) throw new Error('Local app page was not found in the isolated browser.');

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
	const { resolve, reject } = pending.get(message.id);
	pending.delete(message.id);
	if (message.error) reject(new Error(JSON.stringify(message.error)));
	else resolve(message.result);
});

const send = (method, params = {}) =>
	new Promise((resolve, reject) => {
		const id = ++nextId;
		pending.set(id, { resolve, reject });
		socket.send(JSON.stringify({ id, method, params }));
	});

await send('Page.enable');
await send('Network.enable');
await send('Network.clearBrowserCookies');
await send('Page.navigate', { url: 'http://127.0.0.1:8100/#/login?redirect=/trashMap&params={}' });
await new Promise((resolve) => setTimeout(resolve, 1500));
await send('Runtime.evaluate', { expression: "document.querySelector('.login-btn')?.click()" });

let pageReady = false;
for (let attempt = 0; attempt < 30; attempt += 1) {
	await new Promise((resolve) => setTimeout(resolve, 500));
	const readyCheck = await send('Runtime.evaluate', {
		expression: "Boolean(document.querySelector('.map-hero h1'))",
		returnByValue: true,
	});
	if (readyCheck.result.value) {
		pageReady = true;
		break;
	}
}

if (!pageReady) throw new Error('The trashMap page did not finish loading after demo login.');
await new Promise((resolve) => setTimeout(resolve, 3500));

const viewports = [
	[1920, 1080],
	[1440, 900],
	[1024, 900],
	[768, 900],
	[390, 844],
];
const report = [];

for (const [width, height] of viewports) {
	await send('Emulation.setDeviceMetricsOverride', {
		width,
		height,
		deviceScaleFactor: 1,
		mobile: false,
	});
	await new Promise((resolve) => setTimeout(resolve, 350));

	const evaluated = await send('Runtime.evaluate', {
			expression: `JSON.stringify((() => {
			const root = document.documentElement;
			const hero = document.querySelector('.map-hero h1');
			const desktopNav = document.querySelector('.desktop-navigation');
			const mobileNavButton = document.querySelector('.mobile-menu-button');
			const mapSection = document.querySelector('.map-section');
			const fallbackCard = document.querySelector('.fallback-card');
			const controls = document.querySelector('.controls');
			const mapRect = mapSection?.getBoundingClientRect();
			const fallbackRect = fallbackCard?.getBoundingClientRect();
			return {
				href: location.href,
				hero: hero?.textContent?.trim() || null,
				innerWidth,
				scrollWidth: root.scrollWidth,
				horizontalOverflow: root.scrollWidth > innerWidth,
				desktopNavVisible: desktopNav ? getComputedStyle(desktopNav).display !== 'none' : false,
				mobileNavButtonVisible: mobileNavButton ? getComputedStyle(mobileNavButton).display !== 'none' : false,
				mapRect: mapRect ? { width: Math.round(mapRect.width), height: Math.round(mapRect.height) } : null,
				fallbackFitsMap: fallbackRect && mapRect ? fallbackRect.bottom <= mapRect.bottom + 1 : null,
				fallbackCardHeight: fallbackRect ? Math.round(fallbackRect.height) : null,
				controlsRect: controls ? { width: Math.round(controls.getBoundingClientRect().width), top: Math.round(controls.getBoundingClientRect().top) } : null,
			};
		})())`,
		returnByValue: true,
	});
	report.push({ viewport: `${width}x${height}`, ...JSON.parse(evaluated.result.value) });

	const screenshot = await send('Page.captureScreenshot', {
		format: 'png',
		fromSurface: true,
		captureBeyondViewport: false,
	});
	await fs.writeFile(`${qaDir}/trashMap-${width}x${height}.png`, Buffer.from(screenshot.data, 'base64'));
}

await fs.writeFile(`${qaDir}/trashMap-responsive-report.json`, JSON.stringify(report, null, 2));
console.log(JSON.stringify(report, null, 2));
socket.close();
