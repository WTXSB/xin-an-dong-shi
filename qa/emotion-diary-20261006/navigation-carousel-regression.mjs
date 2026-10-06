import fs from 'node:fs/promises';
import assert from 'node:assert/strict';

const out = 'D:/CodexProjects/xin-an-dong-shi-20261005/qa/emotion-diary-20261006';
const targets = await fetch('http://127.0.0.1:9446/json/list').then(r => r.json());
const target = targets.find(t => t.type === 'page' && (t.url === 'about:blank' || t.url.startsWith('http://127.0.0.1:8100/')));
assert(target, 'A dedicated QA browser must be running on CDP 9446');
const socket = new WebSocket(target.webSocketDebuggerUrl);
await new Promise(resolve => socket.addEventListener('open', resolve, { once: true }));
let sequence = 0, cookie = '';
const pending = new Map(), exceptions = [], checks = [];
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
function send(method, params = {}) {
  const id = ++sequence;
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => { pending.delete(id); reject(new Error('Timeout: ' + method)); }, 25000);
    pending.set(id, { resolve, reject, timer });
    socket.send(JSON.stringify({ id, method, params }));
  });
}
async function evaluate(expression) {
  const result = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
  assert(!result.exceptionDetails, JSON.stringify(result.exceptionDetails));
  return result.result.value;
}
async function until(expression) {
  for (let i = 0; i < 100; i++) { if (await evaluate(expression)) return; await delay(120); }
  throw new Error('Not ready: ' + expression);
}
async function forward(event) {
  const url = new URL(event.request.url);
  assert.equal(url.origin, 'http://127.0.0.1:8100');
  assert(url.pathname.startsWith('/api/'));
  let body = event.request.postData;
  if (!body && event.request.hasPostData && event.networkId) body = (await send('Network.getRequestPostData', { requestId: event.networkId })).postData;
  // Never forward test traffic to the real database/backend.
  const response = await fetch('http://127.0.0.1:10099' + url.pathname.slice(4) + url.search, {
    method: event.request.method,
    headers: { 'Content-Type': 'application/json;charset=UTF-8', ...(cookie ? { Cookie: cookie } : {}) },
    ...(body ? { body } : {}),
  });
  const headers = [{ name: 'Content-Type', value: 'application/json;charset=UTF-8' }];
  const setCookie = response.headers.get('set-cookie');
  if (setCookie) { cookie = setCookie.split(';')[0]; headers.push({ name: 'Set-Cookie', value: setCookie }); }
  await send('Fetch.fulfillRequest', { requestId: event.requestId, responseCode: response.status, responseHeaders: headers, body: Buffer.from(await response.arrayBuffer()).toString('base64') });
}
socket.addEventListener('message', event => {
  const message = JSON.parse(event.data);
  if (message.method === 'Runtime.exceptionThrown') exceptions.push(message.params.exceptionDetails.exception?.description || message.params.exceptionDetails.text);
  if (message.method === 'Fetch.requestPaused') {
    forward(message.params).catch(async error => {
      exceptions.push(error.message);
      await send('Fetch.fulfillRequest', { requestId: message.params.requestId, responseCode: 500, body: Buffer.from('Isolated QA request failed').toString('base64') });
    });
    return;
  }
  const request = pending.get(message.id);
  if (!request) return;
  clearTimeout(request.timer); pending.delete(message.id);
  message.error ? request.reject(new Error(JSON.stringify(message.error))) : request.resolve(message.result);
});

async function expectNavigation(path, label) {
  await until(`location.hash.split('?')[0] === '#${path}'`);
  await until(`(()=>{const items=[...document.querySelectorAll('.desktop-navigation .el-menu > .curtain-open')];return items.length===1 && items[0].getAttribute('aria-label')===${JSON.stringify(label)} && items[0].classList.contains('is-active')})()`);
  const state = await evaluate(`(()=>{const item=document.querySelector('.desktop-navigation .el-menu > .curtain-open');return {path:location.hash,label:item.getAttribute('aria-label'),active:item.classList.contains('is-active'),curtain:!!item.querySelector('.nav-drapery')}})()`);
  assert(state.curtain);
  checks.push(state);
}
async function home() {
  await evaluate("location.hash='#/homePage'");
  await expectNavigation('/homePage', '首页');
  await until("!!document.querySelector('.home-about-section .quick-card')");
}

try {
  await send('Page.enable'); await send('Runtime.enable'); await send('Network.enable');
  await send('Page.bringToFront'); await send('Emulation.setFocusEmulationEnabled', { enabled: true });
  await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 900, deviceScaleFactor: 1, mobile: false });
  await send('Fetch.enable', { patterns: [{ urlPattern: 'http://127.0.0.1:8100/api/*', requestStage: 'Request' }] });
  for (const name of ['token', 'role', 'userName', 'JSESSIONID']) await send('Network.deleteCookies', { name, url: 'http://127.0.0.1:8100/' });
  await send('Page.navigate', { url: 'http://127.0.0.1:8100/#/login' });
  await delay(250); await evaluate('localStorage.clear();sessionStorage.clear()');
  await send('Page.reload');
  await until("!!document.querySelector('.login-btn')");
  await evaluate("document.querySelector('.login-btn').click()");
  await until("location.hash!=='#/login' && !!document.querySelector('.desktop-navigation')");
  await delay(800);
  await evaluate("[...document.querySelectorAll('.choice-actions button')].find(b=>b.textContent.includes('先进去看看'))?.click()");
  await home();

  // Real page buttons, not menu clicks: child route must expand its parent navigation.
  await evaluate("document.querySelector('.home-about-section .primary-action').click()");
  await expectNavigation('/imgPredict', '温柔感知');
  await home();
  await evaluate("document.querySelector('.home-about-section .secondary-action').click()");
  await expectNavigation('/smartChat', '安心对话');
  await home();
  await evaluate("document.querySelectorAll('.home-about-section .quick-card')[2].click()");
  await expectNavigation('/dataView', '情绪日记');
  await until("!!document.querySelector('.diary-calendar')");
  await evaluate("document.querySelector('.diary-later')?.click()");
  await home();
  await evaluate("document.querySelectorAll('.home-about-section .quick-card')[3].click()");
  await expectNavigation('/trashMap', '心灵 SPA');
  await evaluate('history.back()'); await expectNavigation('/homePage', '首页');
  await evaluate('history.forward()'); await expectNavigation('/trashMap', '心灵 SPA');
  await send('Page.reload'); await expectNavigation('/trashMap', '心灵 SPA');

  // Query-only updates and direct address changes must preserve exactly one selected item.
  for (const [path, label] of [['/trashRecords', '觉察记录'], ['/spaConnect', '心有灵犀'], ['/videoPredict', '温柔感知'], ['/cameraPredict', '温柔感知']]) {
    await evaluate(`location.hash=${JSON.stringify('#' + path)}`);
    await expectNavigation(path, label);
  }
  await evaluate("location.hash='#/cameraPredict?qa=navigation'");
  await expectNavigation('/cameraPredict', '温柔感知');
  await home();
  await until("!!document.querySelector('.slide-dots button.active')");
  await send('Input.dispatchMouseEvent', { type: 'mouseMoved', x: 5, y: 5 });
  // Measure actual DOM changes rather than just inspecting the interval prop.
  const timing = await evaluate(`new Promise(resolve=>{
    const times=[];let previous=[...document.querySelectorAll('.slide-dots button')].findIndex(b=>b.classList.contains('active'));
    const start=performance.now();const timer=setInterval(()=>{
      const current=[...document.querySelectorAll('.slide-dots button')].findIndex(b=>b.classList.contains('active'));
      if(current!==previous){times.push(performance.now());previous=current;}
      if(times.length===3||performance.now()-start>10000){clearInterval(timer);resolve({changes:times.length,intervals:times.slice(1).map((time,index)=>Math.round(time-times[index]))});}
    },25);
  })`);
  assert.equal(timing.changes, 3);
  assert(timing.intervals.every(ms => ms > 2250 && ms < 2750), JSON.stringify(timing));
  checks.push({ carousel: timing });
  const screenshot = await send('Page.captureScreenshot', { format: 'jpeg', quality: 85, captureBeyondViewport: false });
  await fs.writeFile(out + '/navigation-synced-1440.jpg', Buffer.from(screenshot.data, 'base64'));
  assert.equal(exceptions.length, 0, JSON.stringify(exceptions));
  await fs.writeFile(out + '/navigation-carousel-verification.json', JSON.stringify({ checks, exceptions, backend: 'isolated in-memory database on 10099', realDatabaseWrites: 0 }, null, 2));
  console.log(JSON.stringify({ checks: checks.length, timing, exceptions, realDatabaseWrites: 0 }));
} finally {
  await send('Fetch.disable').catch(() => {});
  socket.close();
}
