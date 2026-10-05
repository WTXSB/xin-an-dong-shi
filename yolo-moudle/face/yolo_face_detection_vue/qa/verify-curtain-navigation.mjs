import fs from 'node:fs/promises';
const target = (await fetch('http://127.0.0.1:9444/json/list').then(r => r.json())).find(t => t.type === 'page');
const socket = new WebSocket(target.webSocketDebuggerUrl);
const pending = new Map(); let id = 0;
socket.addEventListener('message', e => { const r = JSON.parse(e.data); if (!pending.has(r.id)) return; const p = pending.get(r.id); pending.delete(r.id); r.error ? p.reject(r.error) : p.resolve(r.result); });
await new Promise(r => socket.addEventListener('open', r, { once: true }));
const send = (method, params = {}) => new Promise((resolve, reject) => { const next = ++id; pending.set(next, { resolve, reject }); socket.send(JSON.stringify({ id: next, method, params })); });
const wait = ms => new Promise(r => setTimeout(r, ms));
const evaluate = async expression => (await send('Runtime.evaluate', { expression, returnByValue: true })).result.value;
await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 900, deviceScaleFactor: 1, mobile: false });
await send('Page.navigate', { url: 'http://127.0.0.1:8100/#/trashMap' });
await send('Page.reload'); await wait(2300);
const initial = await evaluate(`[...document.querySelectorAll('.el-menu-horizontal-warp .curtain-label')].map(e => ({text:e.textContent,width:e.getBoundingClientRect().width}))`);
if (initial.some(e => e.width > 1)) throw new Error('Navigation is not icon-only initially.');
if (await evaluate(`document.querySelectorAll('.nav-drapery').length`) !== 0) throw new Error('Curtains should not exist before clicking.');
const results = [];
const animationFrames = [];
await evaluate(`document.querySelector('.el-menu-horizontal-warp [aria-label="关于产品"]').click()`);
for (const [phase, delay] of [[300,300],[1100,800],[1900,800],[2700,800],[3700,1000]]) {
	await wait(delay);
	const frame = await evaluate(`({ phase: ${phase}, layers: [...document.querySelectorAll('.nav-drapery > i')].map(e => ({className:e.className,clipPath:getComputedStyle(e).clipPath})), curtainCount:document.querySelectorAll('.nav-drapery').length })`);
	animationFrames.push(frame);
	const screenshot = await send('Page.captureScreenshot',{format:'png',fromSurface:true,clip:{x:0,y:0,width:1000,height:70,scale:1}});
	await fs.writeFile('qa/curtain-opening-' + phase + 'ms.png',Buffer.from(screenshot.data,'base64'));
}
if (animationFrames[0].layers[0].clipPath === animationFrames[3].layers[0].clipPath || animationFrames[0].layers[2].clipPath === animationFrames[3].layers[2].clipPath) throw new Error('Side and top curtains did not animate.');
for (const [label, route] of [['关于产品','aboutProduct'], ['温柔感知','imgPredict'], ['情绪画像','dataView'], ['心灵 SPA','trashMap'], ['觉察记录','trashRecords'], ['安心对话','smartChat']]) {
	await evaluate(`(() => { const item = [...document.querySelectorAll('.el-menu-horizontal-warp [aria-label]')].find(e => e.getAttribute('aria-label') === ${JSON.stringify(label)}); if (!item) throw new Error('Menu not found'); (item.querySelector('.el-sub-menu__title') || item).click(); })()`);
	await wait(3700);
	if (label === '温柔感知') { await evaluate(`(() => { const item = [...document.querySelectorAll('.el-menu--popup .el-menu-item')].find(e => e.textContent.includes('图片')); if (!item) throw new Error('Sensing submenu not available'); item.click(); })()`); await wait(700); }
	const result = await evaluate(`({hash: location.hash, expanded: [...document.querySelectorAll('.el-menu-horizontal-warp .curtain-label')].filter(e=>e.getBoundingClientRect().width>1).map(e=>e.textContent), horizontal: getComputedStyle(document.querySelector('.el-menu-horizontal-warp .el-menu')).flexDirection, background: getComputedStyle(document.querySelector('.el-menu-horizontal-warp .curtain-open .el-sub-menu__title') || document.querySelector('.el-menu-horizontal-warp .curtain-open')).backgroundColor, ties: [...document.querySelectorAll('.drapery-tie')].map(e=>getComputedStyle(e).opacity)})`);
	if (result.hash !== '#/' + route || result.expanded.length !== 1 || result.expanded[0] !== label || result.horizontal !== 'row') throw new Error(JSON.stringify({label,...result}));
	if (result.background !== 'rgba(0, 0, 0, 0)' || result.ties.length !== 2 || result.ties.some(opacity=>opacity!=='1')) throw new Error('Button background or curtain ties incorrect: '+JSON.stringify(result));
	results.push({label,...result});
	const screenshot = await send('Page.captureScreenshot', {format:'png',fromSurface:true});
	await fs.writeFile(`qa/curtain-nav-${route}-1440.png`, Buffer.from(screenshot.data,'base64'));
}
await evaluate(`document.querySelector('.el-menu-horizontal-warp [aria-label="心灵 SPA"]').click()`);
await wait(700);
await fs.writeFile('qa/curtain-navigation-report.json',JSON.stringify({initial,animationFrames,results},null,2));
console.log(JSON.stringify({initial,results},null,2)); socket.close();
