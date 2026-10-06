import fs from 'node:fs/promises';
import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
import path from 'node:path';

const root = 'D:/CodexProjects/xin-an-dong-shi-20261005';
const front = root + '/yolo-moudle/face/yolo_face_detection_vue';
const output = root + '/qa/warm-connect-20261005';
const require = createRequire(front + '/package.json');
const { parse } = require('@vue/compiler-sfc');
const { baseParse } = require('@vue/compiler-dom');
const read = file => fs.readFile(file, 'utf8');
const before = parse(await read(output + '/spaConnect-before.vue')).descriptor;
const after = parse(await read(front + '/src/views/spaConnect/index.vue')).descriptor;
assert.equal(after.scriptSetup.content, before.scriptSetup.content, 'All SPA business script must remain byte-for-byte identical');
const templateEvidence = template => {
 const texts = [], bindings = [];
 const walk = node => {
  if (node.type === 2 && node.content.trim()) texts.push(node.content.trim());
  if (node.type === 5) texts.push('{{' + node.content.content + '}}');
  for (const prop of node.props || []) {
   if (prop.type === 7 && ['on', 'model'].includes(prop.name)) bindings.push(prop.loc.source);
   if (prop.type === 6 && ['placeholder', 'title', 'element-loading-text', 'label'].includes(prop.name)) texts.push(prop.value?.content);
  }
  for (const child of node.children || []) walk(child);
 };
 walk(baseParse(template));
 return { texts, bindings };
};
assert.deepEqual(templateEvidence(after.template.content), templateEvidence(before.template.content), 'Original visible copy, dynamic expressions and events must stay unchanged');
assert.equal(await read(front + '/src/views/aboutProduct/index.vue'), await read(output + '/aboutProduct-before.vue'));
const sourceAudit = { spaScriptIdentical: true, spaTextAndEventBindingsIdentical: true, aboutComponentIdentical: true };
await fs.writeFile(output + '/source-audit.json', JSON.stringify(sourceAudit, null, 2));

const targets = await fetch('http://127.0.0.1:9445/json/list').then(r => r.json());
const target = targets.find(t => t.type === 'page' && t.url === 'about:blank') || targets.find(t => t.type === 'page' && t.url.startsWith('http://127.0.0.1:8100/'));
assert(target, 'Dedicated QA browser target');
const socket = new WebSocket(target.webSocketDebuggerUrl);
await new Promise((resolve, reject) => { socket.addEventListener('open', resolve, { once: true }); socket.addEventListener('error', reject, { once: true }); });
const pending = new Map();
let id = 0, phase = 'guest', apiDelay = 0;
const exceptions = [], apiPosts = [], report = [];
const practitioner = { username: 'qa_practitioner', realName: '测试倾听者', hospital: '测试机构', department: '测试科室', title: '测试身份', bio: '仅在测试浏览器中展示的样例，未写入任何后端数据。' };
const request = { id: 990001, patientUsername: 'visual_qa', practitionerUsername: 'qa_practitioner', counterpartName: '测试陪伴者', initialMessage: '测试书信，仅存在于浏览器验收环境。', status: 'open', createdAt: '2026-10-05T09:00:00' };
let messages = [{ id: 1, senderUsername: 'qa_practitioner', content: '测试回信：在这里慢慢说，我会认真听。', createdAt: '2026-10-05T09:01:00' }];
function send(method, params = {}) {
 const requestId = ++id;
 return new Promise((resolve, reject) => {
  const timer = setTimeout(() => { pending.delete(requestId); reject(new Error('Timed out: ' + method)); }, 30000);
  pending.set(requestId, { resolve, reject, timer });
  socket.send(JSON.stringify({ id: requestId, method, params }));
 });
}
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
const evaluate = async expression => {
 const result = await send('Runtime.evaluate', { expression, returnByValue: true });
 assert(!result.exceptionDetails, JSON.stringify(result.exceptionDetails));
 return result.result.value;
};
async function mock(event) {
 const url = new URL(event.request.url);
 const route = url.pathname;
 let data = [], code = '0';
 if (apiDelay) await delay(apiDelay);
 if (event.request.method !== 'GET') apiPosts.push({ path: route, method: event.request.method });
 if (route.endsWith('/identity/mine')) {
  if (phase === 'guest' || phase === 'empty') code = '404';
  else if (phase === 'error') code = '500';
  else data = { identityType: phase === 'practitioner' ? 'practitioner' : 'patient', auditStatus: ['pending', 'rejected'].includes(phase) ? phase : 'approved', realName: '测试身份', auditNote: '测试审核备注' };
 } else if (route.endsWith('/practitioners')) data = phase === 'empty' ? [] : [practitioner];
 else if (route.endsWith('/requests/mine')) data = [request];
 else if (route.endsWith('/identity/pending')) data = [];
 else if (route.endsWith('/messages')) {
  if (event.request.method === 'POST') {
   let postData = event.request.postData;
   if (!postData && event.networkId) postData = (await send('Network.getRequestPostData', { requestId: event.networkId })).postData;
   const body = JSON.parse(postData || '{}');
   assert.equal(typeof body.content, 'string', 'Mock receives submitted content');
   messages.push({ id: messages.length + 1, senderUsername: body.username, content: body.content, createdAt: '2026-10-05T09:02:00' });
  }
  data = messages.filter(m => m.id > Number(url.searchParams.get('afterId') || 0));
 } else if (route.endsWith('/patient-records')) data = [{ id: 990002, emotionLabel: '平静', gentleSummary: '浏览器内的测试记录。', createdAt: '2026-10-05T09:00:00' }];
 await send('Fetch.fulfillRequest', { requestId: event.requestId, responseCode: 200, responseHeaders: [{ name: 'Content-Type', value: 'application/json' }], body: Buffer.from(JSON.stringify({ code, data })).toString('base64') });
}
socket.addEventListener('message', event => {
 const data = JSON.parse(event.data);
 if (data.method === 'Runtime.exceptionThrown') exceptions.push(data.params.exceptionDetails.exception?.description || data.params.exceptionDetails.text);
 if (data.method === 'Fetch.requestPaused') { mock(data.params).catch(async error => {
  exceptions.push(error.message); console.error('Mock error: ' + error.message);
  await send('Fetch.fulfillRequest', { requestId: data.params.requestId, responseCode: 200, responseHeaders: [{ name: 'Content-Type', value: 'application/json' }], body: Buffer.from(JSON.stringify({ code: '500', msg: 'QA mock error' })).toString('base64') });
 }); return; }
 const handler = pending.get(data.id);
 if (!handler) return;
 clearTimeout(handler.timer); pending.delete(data.id);
 if (data.error) handler.reject(new Error(JSON.stringify(data.error))); else handler.resolve(data.result);
});
await send('Page.enable'); await send('Runtime.enable'); await send('Network.enable');
await send('Page.addScriptToEvaluateOnNewDocument', { source: `localStorage.removeItem('themeConfigStyle');` });
// These browser-only responses never modify the real project or backend.
await send('Fetch.enable', { patterns: [{ urlPattern: 'http://127.0.0.1:8100/api/*', requestStage: 'Request' }] });
await send('Network.setCookies', { cookies: [
 { name: 'token', value: 'codex-visual-qa', url: 'http://127.0.0.1:8100/', path: '/' },
 { name: 'role', value: 'admin', url: 'http://127.0.0.1:8100/', path: '/' },
 { name: 'userName', value: 'visual_qa', url: 'http://127.0.0.1:8100/', path: '/' },
] });
async function navigate(route) {
 await send('Page.navigate', { url: 'http://127.0.0.1:8100/#/' + route });
 await delay(150); await send('Page.reload'); await delay(1600);
 for (let i = 0; i < 30; i++) {
  if (await evaluate(`Boolean(document.querySelector('.layout-main-scroll')) && Boolean(document.querySelector(${JSON.stringify(route === 'spaConnect' ? '.spa-page .letter-garden img' : route === 'homePage' || route === 'aboutProduct' ? '.home-about-section' : '.layout-parent > div')}))`)) { await delay(500); return; }
  await delay(250);
 }
 throw new Error('Page did not mount: ' + route);
}
async function metrics(name) {
 const value = await evaluate(`(() => {
  const scroll = document.querySelector('.layout-main-scroll');
  const main = document.querySelector('.spa-page') || document.querySelector('.layout-main-scroll > div');
  const menu = document.querySelector('.desktop-navigation .el-menu');
  return { hash: location.hash, viewport: innerWidth, overflow: document.documentElement.scrollWidth > innerWidth || scroll.scrollWidth > scroll.clientWidth + 1,
   navLabels: [...menu.children].map(e => e.getAttribute('aria-label')),
   navDirection: getComputedStyle(menu).flexDirection,
   allIllustrationsLoaded: [...document.querySelectorAll('.spa-page img, .hero-image')].every(e => e.complete && e.naturalWidth > 0),
   artAspectPreserved: [...document.querySelectorAll('.letter-garden img, .letter-companions')].every(e => Math.abs(e.clientWidth / e.clientHeight - e.naturalWidth / e.naturalHeight) < .03),
   background: main ? getComputedStyle(main).backgroundImage : '',
   mainViewportHeight: document.querySelector('main.layout-main').getBoundingClientRect().height,
   bodyPreview: document.querySelector('.layout-main-scroll').textContent.replace(/\\s+/g, ' ').trim().slice(0, 130) };
 })()`);
 assert(!value.overflow, name + ': horizontal overflow');
 assert(value.mainViewportHeight > 500, name + ': main area visible');
 assert(!value.navLabels.includes('关于产品'));
 assert.equal(value.navDirection, 'row');
 assert(value.allIllustrationsLoaded, name + ': illustrations load');
 assert(value.artAspectPreserved, name + ': illustration ratio');
 report.push({ name, ...value });
}
async function capture(name, full = false) {
 if (process.argv.includes('--no-screenshots')) return;
 if (full && process.argv.includes('--viewport-only')) { full = false; name = name.replace('-full-', '-viewport-'); }
 console.log('Capturing ' + name);
 let dimensions;
 if (full) {
  await evaluate(`(() => { window.__qaStyles = []; const scroll = document.querySelector('.el-scrollbar__view.layout-main-scroll'); document.querySelector('.el-scrollbar__wrap.layout-main-scroll').scrollTop = 0; const height = scroll.scrollHeight;
   document.querySelectorAll('.desktop-navigation, .desktop-navigation .el-scrollbar').forEach(e => { window.__qaStyles.push([e, e.getAttribute('style')]); e.style.height = '66px'; });
   let e = scroll; while (e && e !== document.documentElement) { window.__qaStyles.push([e, e.getAttribute('style')]); e.style.height = e === scroll ? height + 'px' : 'auto'; e.style.maxHeight = 'none'; e.style.overflow = 'visible'; e = e.parentElement; }
   const style = document.createElement('style'); style.id = 'qa-fullpage-override'; style.textContent = 'html { height: auto !important; max-height: none !important; overflow: visible !important; }'; document.head.appendChild(style); window.scrollTo(0, 0); })()`);
  dimensions = await evaluate(`({ width: document.documentElement.clientWidth, height: Math.max(document.documentElement.scrollHeight, document.body.scrollHeight) })`);
  console.log(JSON.stringify({ name, ...dimensions }));
 }
 let shot;
 try { shot = await send('Page.captureScreenshot', { format: 'jpeg', quality: 85, fromSurface: true, captureBeyondViewport: full, ...(full ? { clip: { x: 0, y: 0, ...dimensions, scale: 1 } } : {}) }); }
 catch { shot = await send('Page.captureScreenshot', { format: 'jpeg', quality: 80, fromSurface: true, captureBeyondViewport: full, ...(full ? { clip: { x: 0, y: 0, ...dimensions, scale: 1 } } : {}) }); }
 await fs.writeFile(output + '/' + name + '.jpg', Buffer.from(shot.data, 'base64'));
 if (full) await evaluate(`document.querySelector('#qa-fullpage-override')?.remove(); window.__qaStyles?.reverse().forEach(([e, s]) => s === null ? e.removeAttribute('style') : e.setAttribute('style', s)); delete window.__qaStyles`);
}
for (const [width, height] of [[1440, 900], [1920, 1080], [768, 900], [390, 844]]) {
 await send('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile: false });
 phase = 'guest'; await navigate('spaConnect'); await metrics('spaConnect-guest-' + width);
 if (width === 1440 || width === 390) await capture('spaConnect-full-' + width, true);
 await evaluate(`document.querySelectorAll('.guide-actions button')[1].click()`); await delay(250);
 const dialog = await evaluate(`(() => { const d = document.querySelector('.warm-dialog.el-dialog'); return { width: d.getBoundingClientRect().width, right: d.getBoundingClientRect().right, title: d.textContent.includes('倾诉前，先完成实名') }; })()`);
 assert(dialog.title && dialog.right <= width + 1 && dialog.width <= width, 'Real-name dialog fits');
 await evaluate(`document.querySelector('.warm-dialog .el-dialog__footer button').click()`);
 await navigate('homePage'); await metrics('homePage-' + width);
 const home = await evaluate(`({ aboutBelowCarousel: document.querySelector('.home-about-section').previousElementSibling.classList.contains('carousel-shell'), heroButtonsGone: !document.querySelector('.carousel-shell .hero-actions'), aboutText: document.querySelector('.home-about-section').textContent.includes('让科技安静地工作，让陪伴温柔地发生') })`);
 assert(home.aboutBelowCarousel && home.heroButtonsGone && home.aboutText);
 if (width === 1440 || width === 390) await capture('homePage-full-' + width, true);
}
await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 900, deviceScaleFactor: 1, mobile: false });
for (const state of ['empty', 'pending', 'rejected', 'patient', 'practitioner', 'error']) {
 phase = state; await navigate('spaConnect'); await metrics('spaConnect-' + state); await capture('spaConnect-' + state);
 if (state === 'patient' || state === 'practitioner') {
  await evaluate(`document.querySelector('.request-card').click()`); await delay(350);
  assert(await evaluate(`Boolean(document.querySelector('.letter-card'))`));
  if (state === 'practitioner') assert(await evaluate(`Boolean(document.querySelector('.records-aside .record-item'))`));
  await capture('spaConnect-conversation-' + state);
  await evaluate(`(() => { const t = document.querySelector('.reply-box textarea'); t.value = '仅用于浏览器验收的书信'; t.dispatchEvent(new Event('input', { bubbles: true })); })()`); await delay(100);
  await evaluate(`document.querySelector('.send-btn').click()`);
  for (let attempt = 0; attempt < 25; attempt++) {
   if (await evaluate(`document.querySelector('.letters').textContent.includes('仅用于浏览器验收的书信')`)) break;
   await delay(200);
  }
  assert(await evaluate(`document.querySelector('.letters').textContent.includes('仅用于浏览器验收的书信')`));
 }
}
phase = 'guest'; await navigate('spaConnect');
await evaluate(`document.querySelectorAll('.module-tabs button')[1].click()`); await delay(250);
assert(await evaluate(`document.querySelector('.review-panel').textContent.includes('暂时没有等待审核的资料')`));
await capture('spaConnect-review');
await navigate('aboutProduct'); assert.equal(await evaluate('location.hash'), '#/homePage');
for (const route of ['imgPredict', 'videoPredict', 'cameraPredict', 'dataView', 'trashMap', 'trashRecords', 'smartChat']) {
 await navigate(route); await metrics(route + '-1440');
 if (route === 'trashMap' || route === 'dataView') await capture(route + '-warm', true);
}
await send('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: false });
for (const route of ['dataView', 'trashMap', 'trashRecords', 'smartChat']) { await navigate(route); await metrics(route + '-390'); }
phase = 'guest'; await navigate('spaConnect');
await evaluate(`document.querySelector('.mobile-menu-button').click()`); await delay(300);
const mobileMenu = await evaluate(`[...document.querySelectorAll('.mobile-nav-drawer .el-drawer__body > .el-menu > li')].map(e => e.textContent.trim())`);
assert(mobileMenu.length === 7 && !mobileMenu.includes('关于产品') && mobileMenu.includes('心有灵犀'));
await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 900, deviceScaleFactor: 1, mobile: false });
await navigate('spaConnect'); await capture('spaConnect-final');
assert.equal(exceptions.length, 0, JSON.stringify(exceptions));
await fs.writeFile(output + '/verification.json', JSON.stringify({ sourceAudit, report, mobileMenu, uncaughtExceptions: exceptions, mockedPosts: apiPosts, backendWrites: 0, legacyAboutRedirect: true }, null, 2));
console.log(JSON.stringify({ sourceAudit, checks: report.length, overflows: report.filter(r => r.overflow), mobileMenu, uncaughtExceptions: exceptions, backendWrites: 0, screenshotDirectory: output }, null, 2));
await send('Fetch.disable');
socket.close();
