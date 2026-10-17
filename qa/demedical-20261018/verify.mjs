// All application API writes go only to an isolated in-memory server on 10099.
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
const out = 'D:/CodexQA/xinan-demedical-20261018', base = 'http://127.0.0.1:10099';
const checks = [], errors = [], apiErrors = [], username = 'copyqa-' + Date.now();
async function api(path, data) {
  const r = await fetch(base + path, { method: data ? 'POST' : 'GET', headers: { 'Content-Type': 'application/json' }, ...(data ? { body: JSON.stringify(data) } : {}) });
  assert(r.ok); const json = await r.json(); assert.equal(String(json.code), '0', JSON.stringify(json)); return json.data;
}
await api('/user/register', { username, password: '123456' });
const awareness = await api('/awarenessRecords/fromPrediction', { username, sourceType: 'video', emotionLabel: 'sad', keepRecord: true });
await api('/analysisRecords', {
  sessionId: 'demedical-' + Date.now(), awarenessRecordId: awareness.id, username, sourceType: 'video',
  analysisMode: 'combined', keepRecord: true, keepMedia: false, complaint: '测试示例：考试前有些紧张，想梳理自己的感受。',
  emotionResults: [{ emotionType: 'sad', frameCount: 8, averageConfidence: .72, maxConfidence: .88 }], bfrbEvents: [],
});
const summary = await api('/preVisitReportSummaries/generate/' + awareness.id, {});
assert.equal(summary.provider, 'local-fallback'); assert(summary.visitPreparation[3].includes('服务者'));
assert(!/预诊|患者|医生|医患/.test(JSON.stringify(summary)));
checks.push({ name: 'isolated-summary', version: summary.promptVersion });
const target = (await fetch('http://127.0.0.1:9446/json/list').then(r => r.json())).find(t => t.type === 'page' && (t.url === 'about:blank' || t.url.startsWith('http://127.0.0.1:8100/')));
assert(target, 'Dedicated browser required');
const ws = new WebSocket(target.webSocketDebuggerUrl);
await new Promise(resolve => ws.addEventListener('open', resolve, { once: true }));
let sequence = 0, cookie = '';
const pending = new Map(), pause = ms => new Promise(r => setTimeout(r, ms));
function send(method, params = {}) {
  const id = ++sequence; return new Promise((resolve, reject) => {
    const timer = setTimeout(() => { pending.delete(id); reject(new Error(method + ' timed out')); }, 30000);
    pending.set(id, { resolve, reject, timer }); ws.send(JSON.stringify({ id, method, params }));
  });
}
async function evaluate(expression) {
  const r = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
  assert(!r.exceptionDetails, JSON.stringify(r.exceptionDetails)); return r.result.value;
}
async function until(expression) {
  for (let i = 0; i < 120; i++) { if (await evaluate(expression)) return; await pause(150); }
  throw new Error('Not ready: ' + expression);
}
async function forward(event) {
  const url = new URL(event.request.url);
  assert.equal(url.origin, 'http://127.0.0.1:8100'); assert(url.pathname.startsWith('/api/'));
  let body = event.request.postData;
  if (!body && event.request.hasPostData && event.networkId) body = (await send('Network.getRequestPostData', { requestId: event.networkId })).postData;
  const r = await fetch(base + url.pathname.slice(4) + url.search, { method: event.request.method,
    headers: { 'Content-Type': 'application/json;charset=UTF-8', ...(cookie ? { Cookie: cookie } : {}) }, ...(body ? { body } : {}) });
  const headers = [{ name: 'Content-Type', value: 'application/json;charset=UTF-8' }];
  const setCookie = r.headers.get('set-cookie');
  if (setCookie) { cookie = setCookie.split(';')[0]; headers.push({ name: 'Set-Cookie', value: setCookie }); }
  if (!r.ok) apiErrors.push({ path: url.pathname, status: r.status });
  await send('Fetch.fulfillRequest', { requestId: event.requestId, responseCode: r.status, responseHeaders: headers, body: Buffer.from(await r.arrayBuffer()).toString('base64') });
}
ws.addEventListener('message', event => {
  const r = JSON.parse(event.data);
  if (r.method === 'Runtime.exceptionThrown') errors.push(r.params.exceptionDetails.exception?.description || r.params.exceptionDetails.text);
  if (r.method === 'Fetch.requestPaused') { forward(r.params).catch(async e => { errors.push(e.message); await send('Fetch.failRequest', { requestId: r.params.requestId, errorReason: 'Failed' }); }); return; }
  const entry = pending.get(r.id); if (!entry) return; clearTimeout(entry.timer); pending.delete(r.id);
  r.error ? entry.reject(new Error(JSON.stringify(r.error))) : entry.resolve(r.result);
});
async function screenshot(name) {
  const m = await send('Page.getLayoutMetrics');
  const r = await send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: true, clip: { x: 0, y: 0, width: m.cssContentSize.width, height: m.cssContentSize.height, scale: 1 } });
  await fs.writeFile(out + '/' + name + '.png', Buffer.from(r.data, 'base64'));
}
async function visit(path, selector) {
  await evaluate('location.hash=' + JSON.stringify('#' + path));
  await until('Boolean(document.querySelector(' + JSON.stringify(selector) + '))'); await pause(550);
}
async function inspect(name) {
  const r = await evaluate(`(()=>{const text=document.body.innerText;return {path:location.hash,oldCopy:text.match(/预诊|患者|医生|医患联动/g)||[],navigation:[...document.querySelectorAll('.desktop-navigation .el-menu > .curtain-open')].map(x=>({name:x.getAttribute('aria-label'),active:x.classList.contains('is-active')})),overflow:document.documentElement.scrollWidth>innerWidth+1}})()`);
  assert.deepEqual(r.oldCopy, [], name + ' old copy'); assert(!r.overflow, name + ' overflow'); checks.push({ name, ...r });
}
try {
  await send('Page.enable'); await send('Runtime.enable'); await send('Network.enable');
  await send('Page.bringToFront'); await send('Emulation.setFocusEmulationEnabled', { enabled: true });
  await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 900, deviceScaleFactor: 1, mobile: false });
  await send('Fetch.enable', { patterns: [{ urlPattern: 'http://127.0.0.1:8100/api/*', requestStage: 'Request' }] });
  for (const name of ['token', 'role', 'userName', 'JSESSIONID']) await send('Network.deleteCookies', { name, url: 'http://127.0.0.1:8100/' });
  await send('Page.navigate', { url: 'http://127.0.0.1:8100/#/login' }); await pause(400);
  await evaluate('localStorage.clear();sessionStorage.clear()'); await send('Page.reload');
  await send('Page.navigate', { url: 'http://127.0.0.1:8100/#/login' });
  await until('!!document.querySelector(".login-btn")');
  await evaluate(`(()=>{const input=document.querySelector('input');Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set.call(input,${JSON.stringify(username)});input.dispatchEvent(new Event('input',{bubbles:true}));})()`);
  await evaluate('document.querySelector(".login-btn").click()');
  await until('location.hash!=="#/login" && !!document.querySelector(".desktop-navigation")'); await pause(800);
  await evaluate('[...document.querySelectorAll(".choice-actions button")].find(x=>x.textContent.includes("先进去看看"))?.click()');
  for (const [path, selector, name] of [
    ['/homePage', '.home-about-section', 'home'], ['/imgPredict', '.pre-visit-notes', 'image'],
    ['/videoPredict', '.pre-visit-notes', 'video'], ['/cameraPredict', '.pre-visit-notes', 'camera'],
    ['/dataView', '.diary-calendar', 'diary'], ['/trashMap', '.map-hero', 'map'],
    ['/trashRecords', '.report-btn', 'records'], ['/smartChat', '.safety-note', 'chat'], ['/spaConnect', '.guide-card', 'connect'],
  ]) {
    await visit(path, selector); await evaluate('document.querySelector(".diary-later")?.click()');
    await inspect(name); await screenshot(name + '-1440');
    assert(await evaluate('document.querySelector(".desktop-navigation .curtain-open")?.classList.contains("is-active")'), name + ' selected nav');
  }
  await visit('/identityGuide', '.choice-row');
  await evaluate('[...document.querySelectorAll(".choice-card button")].find(x=>x.textContent.includes("认证"))?.click()');
  await until('!!document.querySelector(".form-card")'); await inspect('identity'); await screenshot('identity-1440');
  // Exercise the authenticated user branch against the isolated database only.
  await api('/spa/identity/apply', { username, identityType: 'patient', realName: '浏览器验收示例', idCard: '110101199003077713' });
  await api('/spa/identity/review', { adminUsername: 'demo', username, approve: true, note: '隔离测试资料' });
  await evaluate('location.hash="#/spaConnect"'); await send('Page.reload');
  await until('!!document.querySelector(".practitioner-section")');
  await inspect('connect-approved'); await screenshot('connect-approved-1440');
  await visit('/preVisitReport/' + awareness.id, '.view-switch');
  assert(await evaluate('document.querySelector(".view-switch").innerText.includes("用户视角")'));
  await evaluate('document.querySelectorAll(".view-switch button")[1].click()'); await until('!!document.querySelector(".report-header h1")');
  assert.equal(await evaluate('document.querySelector(".report-header h1").innerText'), '心理感知报告');
  await inspect('report'); await screenshot('report-1440');
  const printTitle = await evaluate(`(()=>{window.print=()=>{window.__qaPrintTitle=document.title};document.querySelector('.page-actions .el-button--primary').click();return window.__qaPrintTitle})()`);
  assert(printTitle.includes('心理感知报告')); assert(!printTitle.includes('预诊'));
  const pdf = await send('Page.printToPDF', { printBackground: true, preferCSSPageSize: true });
  await fs.writeFile(out + '/psychological-perception-report.pdf', Buffer.from(pdf.data, 'base64'));
  checks.push({ name: 'report-export', title: printTitle }); await pause(1200);
  await send('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: true });
  await pause(400); await inspect('report-mobile'); await screenshot('report-390');
  assert.deepEqual(errors, []); assert.deepEqual(apiErrors, []);
  await fs.writeFile(out + '/verification.json', JSON.stringify({ checks, errors, apiErrors, isolatedBackend: base }, null, 2));
  console.log(JSON.stringify({ passed: checks.length, errors, apiErrors, out }));
} catch (error) {
  const state = await evaluate('JSON.stringify({hash:location.hash,text:document.body.innerText.slice(-2500)})');
  await fs.writeFile(out + '/failure.json', JSON.stringify({ message: error.message, state, checks, errors, apiErrors }, null, 2));
  await screenshot('failure'); throw error;
} finally { ws.close(); }
