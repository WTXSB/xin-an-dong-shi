import fs from 'node:fs/promises';
const target = (await fetch('http://127.0.0.1:9444/json/list').then(r => r.json())).find(t => t.type === 'page');
const socket = new WebSocket(target.webSocketDebuggerUrl);
const pending = new Map(); let id = 0;
socket.addEventListener('message', e => { const r = JSON.parse(e.data); if (!pending.has(r.id)) return; const p = pending.get(r.id); pending.delete(r.id); r.error ? p.reject(r.error) : p.resolve(r.result); });
await new Promise(r => socket.addEventListener('open', r, { once: true }));
const send = (method, params = {}) => new Promise((resolve, reject) => { const next = ++id; pending.set(next, { resolve, reject }); socket.send(JSON.stringify({ id: next, method, params })); });
const wait = ms => new Promise(r => setTimeout(r, ms));
const evaluate = async expression => (await send('Runtime.evaluate', { expression, returnByValue: true })).result.value;
const report = [];
for (const width of [1440, 390]) {
 await send('Emulation.setDeviceMetricsOverride', { width, height: width === 1440 ? 900 : 844, deviceScaleFactor: 1, mobile: width === 390 });
 for (const route of ['aboutProduct', 'imgPredict', 'dataView', 'trashMap', 'trashRecords', 'smartChat']) {
  await send('Page.navigate', { url: 'http://127.0.0.1:8100/#/' + route }); await wait(1600);
  const result = await evaluate(`(() => {
   const scene = document.querySelector('.connected-landscape');
   const style = getComputedStyle(scene); const rect = scene.getBoundingClientRect();
   const groups = [...document.querySelectorAll('.emotion-family')];
   return { route: location.hash, viewport: innerWidth, horizontalOverflow: document.documentElement.scrollWidth > innerWidth,
    sceneCount: document.querySelectorAll('.connected-landscape').length, source: style.backgroundImage, position: style.backgroundPosition,
    ratio: rect.width / rect.height, sceneWidth: rect.width,
    bottomGap: scene.parentElement.getBoundingClientRect().top - (scene.parentElement.previousElementSibling?.getBoundingClientRect().bottom || 0),
    captions: groups.map(e => ({title:e.querySelector('h2').textContent, labels:[...e.querySelectorAll('figcaption')].map(e => e.textContent)})),
    connectedStrips: document.querySelectorAll('.scene-connection').length };
  })()`);
  if (result.sceneCount !== 1 || Math.abs(result.ratio - 4) > .01 || result.horizontalOverflow) throw new Error(JSON.stringify(result));
  if (route === 'dataView' && result.captions.reduce((n,g)=>n+g.labels.length,0) !== 12) throw new Error('Missing animal emotion labels');
  if (route === 'trashRecords' && result.bottomGap > 14) throw new Error('Records footer has unnecessary whitespace: ' + result.bottomGap);
  report.push(result);
  await evaluate(`document.querySelector('.connected-landscape').parentElement.scrollIntoView({ block:'center' })`); await wait(100);
  const screenshot = await send('Page.captureScreenshot', {format:'png',fromSurface:true});
  await fs.writeFile('qa/connected-bottom-' + route + '-' + width + '.png', Buffer.from(screenshot.data,'base64'));
 }
}
if (new Set(report.filter(r=>r.viewport===1440).map(r=>r.position)).size !== 6) throw new Error('The six section scenes should be different');
await send('Emulation.setDeviceMetricsOverride', { width:1440,height:900,deviceScaleFactor:1,mobile:false });
await send('Page.navigate', {url:'http://127.0.0.1:8100/#/trashMap'}); await wait(1400);
await evaluate(`document.querySelector('.el-menu-horizontal-warp [aria-label="心灵 SPA"]').click()`); await wait(3700);
const curtain = await evaluate(`({perspective:getComputedStyle(document.querySelector('.nav-drapery')).perspective, foldCount:document.querySelectorAll('.fabric-fold').length, transforms:[...document.querySelectorAll('.fabric-fold')].map(e=>getComputedStyle(e).transform)})`);
if (curtain.foldCount !== 14 || curtain.perspective === 'none' || curtain.transforms.some(t=>!t.startsWith('matrix3d'))) throw new Error('Curtain depth transforms missing');
await fs.writeFile('qa/connected-scenes-report.json',JSON.stringify({pages:report,curtain},null,2));
console.log(JSON.stringify({passed:true,pages:report.length,uniqueScenes:6,curtain},null,2)); socket.close();
