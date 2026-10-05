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
await send('Page.navigate', { url: 'http://127.0.0.1:8100/#/homePage' });
await new Promise((resolve) => setTimeout(resolve, 1800));

const report = [];
for (const [width, height] of [[1440, 900], [390, 844]]) {
	await send('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile: false });
	await new Promise((resolve) => setTimeout(resolve, 450));
	const evaluated = await send('Runtime.evaluate', {
			expression: `JSON.stringify((() => ({
			href: location.href,
			innerWidth,
			scrollWidth: document.documentElement.scrollWidth,
			horizontalOverflow: document.documentElement.scrollWidth > innerWidth,
			slideCount: document.querySelectorAll('.hero-carousel .el-carousel__item').length,
			imageAlt: document.querySelector('.hero-carousel .is-active .hero-image')?.alt || '',
			imageSource: document.querySelector('.hero-carousel .is-active .hero-image')?.currentSrc || '',
			images: [...document.querySelectorAll('.hero-image')].map((image) => ({
				alt: image.alt,
				loaded: image.complete && image.naturalWidth === 3840 && image.naturalHeight === 2160
			})),
			companionOverlayExists: Boolean(document.querySelector('.companion-illustration')),
			desktopNavVisible: getComputedStyle(document.querySelector('.desktop-navigation')).display !== 'none',
			mobileNavButtonVisible: getComputedStyle(document.querySelector('.mobile-menu-button')).display !== 'none'
		}))())`,
		returnByValue: true,
	});
	report.push({ viewport: `${width}x${height}`, ...JSON.parse(evaluated.result.value) });
	const screenshot = await send('Page.captureScreenshot', { format: 'png', fromSurface: true, captureBeyondViewport: false });
	await fs.writeFile(`${qaDir}/homePage-${width}x${height}.png`, Buffer.from(screenshot.data, 'base64'));
}

const firstAlt = report[0]?.imageAlt;
await new Promise((resolve) => setTimeout(resolve, 5600));
const afterAutoplay = await send('Runtime.evaluate', {
	expression: `document.querySelector('.hero-carousel .is-active .hero-image')?.alt || ''`,
	returnByValue: true,
});
report.push({ autoplayAdvanced: afterAutoplay.result.value !== firstAlt, activeAltAfterInterval: afterAutoplay.result.value });

await fs.writeFile(`${qaDir}/homePage-responsive-report.json`, JSON.stringify(report, null, 2));
console.log(JSON.stringify(report, null, 2));
socket.close();
