import fs from 'node:fs/promises';
import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
const root='D:/CodexProjects/xin-an-dong-shi-20261005',out=root+'/qa/emotion-diary-20261006',front=root+'/yolo-moudle/face/yolo_face_detection_vue';
const require=createRequire(front+'/package.json'),ts=require('typescript');
const calendarSource=await fs.readFile(front+'/src/components/emotionDiary/calendar.ts','utf8');
const compiled=ts.transpileModule(calendarSource,{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText;
const module={exports:{}};new Function('exports','module',compiled)(module.exports,module);const c=module.exports;
assert.equal(c.periodKey(new Date(2026,9,6,11,59)), 'morning');assert.equal(c.periodKey(new Date(2026,9,6,12,0)), 'afternoon');assert.equal(c.periodKey(new Date(2026,9,6,17,59)), 'afternoon');assert.equal(c.periodKey(new Date(2026,9,6,18,0)), 'evening');
assert.equal(c.calendarDays('2024-02').filter(d=>d.current).length,29);assert.equal(c.calendarDays('2026-10').filter(d=>d.current).length,31);assert.equal(c.calendarDays('2026-10').length,42);
const target=(await fetch('http://127.0.0.1:9446/json/list').then(r=>r.json())).find(t=>t.type==='page' && (t.url==='about:blank'||t.url.startsWith('http://127.0.0.1:8100/')));assert(target);
const socket=new WebSocket(target.webSocketDebuggerUrl);await new Promise(resolve=>socket.addEventListener('open',resolve,{once:true}));
let id=0,cookie='',mockInsight=null;const pending=new Map(),exceptions=[],checks=[],writes=[];
function send(method,params={}){const n=++id;return new Promise((resolve,reject)=>{const timer=setTimeout(()=>{pending.delete(n);reject(new Error('Timeout '+method));},25000);pending.set(n,{resolve,reject,timer});socket.send(JSON.stringify({id:n,method,params}));});}
const delay=ms=>new Promise(r=>setTimeout(r,ms));
async function evaluate(expression){const r=await send('Runtime.evaluate',{expression,returnByValue:true,awaitPromise:true});assert(!r.exceptionDetails,JSON.stringify(r.exceptionDetails));return r.result.value;}
async function until(expression){for(let i=0;i<80;i++){if(await evaluate(expression))return;await delay(150);}throw new Error('Not ready: '+expression);}
async function forward(event){
 const url=new URL(event.request.url);assert.equal(url.origin,'http://127.0.0.1:8100');assert(url.pathname.startsWith('/api/'));
 let body=event.request.postData;if(!body && event.request.hasPostData && event.networkId)body=(await send('Network.getRequestPostData',{requestId:event.networkId})).postData;
 if(event.request.method!=='GET')writes.push({path:url.pathname,method:event.request.method});
 let bytes,status=200,headers=[{name:'Content-Type',value:'application/json;charset=UTF-8'}];
 if(mockInsight && url.pathname==='/api/diary/insights')bytes=Buffer.from(JSON.stringify({code:'0',data:[mockInsight]}));
 else{
  // ALL application API traffic in this dedicated browser is forwarded ONLY to the in-memory QA server.
  const r=await fetch('http://127.0.0.1:10099'+url.pathname.slice(4)+url.search,{method:event.request.method,headers:{'Content-Type':'application/json;charset=UTF-8',...(cookie?{Cookie:cookie}:{})},...(body?{body}:{})});
  const setCookie=r.headers.get('set-cookie');if(setCookie){cookie=setCookie.split(';')[0];headers.push({name:'Set-Cookie',value:setCookie});}
  status=r.status;bytes=Buffer.from(await r.arrayBuffer());
 }
 await send('Fetch.fulfillRequest',{requestId:event.requestId,responseCode:status,responseHeaders:headers,body:bytes.toString('base64')});
}
socket.addEventListener('message',event=>{const r=JSON.parse(event.data);if(r.method==='Runtime.exceptionThrown')exceptions.push(r.params.exceptionDetails.exception?.description||r.params.exceptionDetails.text);if(r.method==='Fetch.requestPaused'){forward(r.params).catch(async e=>{exceptions.push(e.message);await send('Fetch.fulfillRequest',{requestId:r.params.requestId,responseCode:500,body:Buffer.from('QA forwarding failed').toString('base64')});});return;}const h=pending.get(r.id);if(!h)return;clearTimeout(h.timer);pending.delete(r.id);r.error?h.reject(new Error(JSON.stringify(r.error))):h.resolve(r.result);});
await send('Page.enable');await send('Runtime.enable');await send('Network.enable');
await send('Page.bringToFront');await send('Emulation.setFocusEmulationEnabled',{enabled:true});
for(const name of ['token','role','userName','JSESSIONID'])await send('Network.deleteCookies',{name,url:'http://127.0.0.1:8100/'});
await send('Page.navigate',{url:'http://127.0.0.1:8100/#/login'});await delay(350);await evaluate('localStorage.clear();sessionStorage.clear()');
await send('Fetch.enable',{patterns:[{urlPattern:'http://127.0.0.1:8100/api/*',requestStage:'Request'}]});
await send('Page.addScriptToEvaluateOnNewDocument',{source:"localStorage.removeItem('themeConfigStyle');"});
await send('Emulation.setDeviceMetricsOverride',{width:1440,height:900,deviceScaleFactor:1,mobile:false});
await send('Page.navigate',{url:'http://127.0.0.1:8100/#/login'});await send('Page.reload');await until("Boolean(document.querySelector('.login-btn'))");
// Use the real login form and the repository's documented demo account, not forged authentication.
await evaluate("document.querySelector('.login-btn').click()");await until("location.hash!=='#/login' && !!document.querySelector('.layout-navbars-breadcrumb-index')");
await delay(1200);await evaluate("[...document.querySelectorAll('.choice-actions button')].find(b=>b.textContent.includes('先进去看看'))?.click()");await until("!!document.querySelector('.desktop-navigation [aria-label=\"情绪日记\"]')");await evaluate("document.querySelector('.desktop-navigation [aria-label=\"情绪日记\"]').click()");await until("!!document.querySelector('.diary-calendar') && !!document.querySelector('#daily-diary-text') && !document.querySelector('#daily-diary-text').disabled");await delay(400);if(!await evaluate("!!document.querySelector('.diary-mood-dialog')"))await evaluate("document.querySelector('.diary-slot-action:not(:disabled)').click()");await until("!!document.querySelector('.diary-mood-dialog')");await delay(500);
async function shot(name,full=false){
 if(process.argv.includes('--no-screenshots'))return;
 if(full){await evaluate(`(()=>{window.__diaryQA=[];const v=document.querySelector('.el-scrollbar__view.layout-main-scroll');document.querySelector('.el-scrollbar__wrap.layout-main-scroll').scrollTop=0;let e=v;while(e&&e!==document.documentElement){window.__diaryQA.push([e,e.getAttribute('style')]);e.style.height=e===v?v.scrollHeight+'px':'auto';e.style.maxHeight='none';e.style.overflow='visible';e=e.parentElement;}const s=document.createElement('style');s.id='diary-qa-full';s.textContent='html{height:auto!important;overflow:visible!important}.desktop-navigation,.desktop-navigation .el-scrollbar{height:66px!important}';document.head.appendChild(s);window.scrollTo(0,0)})()`);}
 try {const dims=full?await evaluate('({width:innerWidth,height:Math.max(document.body.scrollHeight,document.documentElement.scrollHeight)})'):null;const r=await send('Page.captureScreenshot',{format:'jpeg',quality:85,fromSurface:true,captureBeyondViewport:full,...(dims?{clip:{x:0,y:0,...dims,scale:1}}:{})});await fs.writeFile(out+'/'+name+'.jpg',Buffer.from(r.data,'base64'));}
 finally{if(full)await evaluate("document.querySelector('#diary-qa-full')?.remove();window.__diaryQA?.reverse().forEach(([e,s])=>s===null?e.removeAttribute('style'):e.setAttribute('style',s));delete window.__diaryQA");}
}
await shot('mood-dialog-1440');
await evaluate("document.querySelector('.diary-later').click()");await delay(300);
await evaluate("document.querySelector('.desktop-navigation [aria-label=\"情绪日记\"]').click()");await delay(450);
assert(await evaluate("!document.querySelector('.diary-mood-dialog') || getComputedStyle(document.querySelector('.diary-mood-dialog').parentElement).display==='none'"),'Repeat nav click must not prompt again');checks.push('repeat navigation after dismiss does not reopen');
const today=c.dateKey(),yesterday=c.dateKey(new Date(Date.now()-86400000));
async function api(path,method='GET',data){const r=await fetch('http://127.0.0.1:10099/diary/'+path,{method,headers:{Cookie:cookie,'Content-Type':'application/json'},...(data?{body:JSON.stringify(data)}:{})});const result=await r.json();assert.equal(result.code,'0',JSON.stringify(result));return result.data;}
for(const [period,score]of [['morning',4],['afternoon',6],['evening',8]])await api('mood','POST',{date:yesterday,period,score,timezoneOffset:new Date().getTimezoneOffset()});
let old=await api('entry?date='+yesterday+'&timezoneOffset='+new Date().getTimezoneOffset());
old=await api('entry','PUT',{date:yesterday,content:'验收日记：会议前有些紧张，午后和朋友散步后轻松了一些。',revision:old.revision,timezoneOffset:new Date().getTimezoneOffset()});
await evaluate("document.querySelector('.diary-section-heading .el-button').click()");await delay(450);await evaluate(`document.querySelector('.diary-day[aria-label^="${yesterday}"]').click()`);await until(`document.querySelector('#daily-diary-text').value.includes('验收日记')`);
assert.equal(await evaluate("document.querySelectorAll('.diary-day-detail .diary-animal').length"),3);checks.push('three independent mood ratings and historical diary load');
await evaluate("(()=>{let t=document.querySelector('#daily-diary-text');t.value+=' 晚上泡了一杯温热的茶。';t.dispatchEvent(new Event('input',{bubbles:true}));})()");await delay(100);
await evaluate("[...document.querySelectorAll('.diary-writing-actions button')].find(b=>b.textContent.includes('保存日记')).click()");await until("document.querySelector('.diary-save-status').textContent.includes('已保存')");
await evaluate("document.querySelector('.desktop-navigation [aria-label=\"首页\"]').click()");await until("!!document.querySelector('.home-about-section')");await evaluate("document.querySelector('.desktop-navigation [aria-label=\"情绪日记\"]').click()");await until("!!document.querySelector('#daily-diary-text')");await evaluate(`document.querySelector('.diary-day[aria-label^="${yesterday}"]').click()`);await until("document.querySelector('#daily-diary-text').value.includes('温热的茶')");checks.push('typing, saving and history preserved after navigation');
await evaluate("document.querySelector('.diary-ai-consent input').click()");await delay(100);await evaluate("[...document.querySelectorAll('.diary-writing-actions button')].find(b=>b.textContent.includes('分析日记')).click()");await until("document.querySelector('.diary-analysis-error')?.textContent.includes('AI 尚未连接')");
assert((await api('entry?date='+yesterday+'&timezoneOffset='+new Date().getTimezoneOffset())).content.includes('温热的茶'));checks.push('missing AI configuration gives honest error and preserves text');
const menu=await evaluate("[...document.querySelector('.desktop-navigation .el-menu').children].map(e=>e.getAttribute('aria-label'))");assert(menu.includes('情绪日记')&&!menu.includes('情绪画像'));checks.push('horizontal navigation renamed, route unchanged');
async function metrics(label){const result=await evaluate(`(()=>{const scroll=document.querySelector('.el-scrollbar__wrap.layout-main-scroll');return {width:innerWidth,overflow:document.documentElement.scrollWidth>innerWidth+1||scroll.scrollWidth>scroll.clientWidth+1,images:[...document.querySelectorAll('.diary-book-art,.diary-insight-bottom img')].map(e=>({loaded:e.complete&&e.naturalWidth>0,ratio:Math.abs(e.clientWidth/e.clientHeight-e.naturalWidth/e.naturalHeight)<.03})),navDirection:getComputedStyle(document.querySelector('.desktop-navigation .el-menu')).flexDirection}})()`);assert(!result.overflow,label+' overflow');assert(result.images.every(i=>i.loaded&&i.ratio));assert.equal(result.navDirection,'row');checks.push({label,...result});}
for(const [width,height]of [[1920,1080],[1440,900],[1024,900],[768,900],[390,844]]){await send('Emulation.setDeviceMetricsOverride',{width,height,deviceScaleFactor:1,mobile:false});await delay(350);await metrics('diary-'+width);if(width===1440)await shot('diary-full-1440',true);if(width===390)await shot('diary-390');}
await evaluate("document.querySelector('.diary-slot-action').click()");await delay(400);const d=await evaluate("(()=>{const e=document.querySelector('.diary-mood-dialog');return {left:e.getBoundingClientRect().left,right:e.getBoundingClientRect().right,width:e.getBoundingClientRect().width}})()");assert(d.left>=0&&d.right<=391);await shot('mood-dialog-390');
await evaluate("document.querySelector('.diary-animal-choices button:last-child').click()");await delay(100);await evaluate("document.querySelector('.diary-record-mood').click()");await until("!document.querySelector('.diary-mood-dialog')||getComputedStyle(document.querySelector('.diary-mood-dialog').parentElement).display==='none'");checks.push('manual animal mood editing works and mobile dialog fits');
await send('Emulation.setDeviceMetricsOverride',{width:1440,height:900,deviceScaleFactor:1,mobile:false});await evaluate("location.hash='#/trashRecords'");await until("!!document.querySelector('.diary-reflection-empty')");await shot('awareness-empty-1440');
mockInsight={...old,analysis:{summary:'会议前的紧张、散步后的轻松与夜里的放松，出现在同一页中。你没有要求一天只有一种心情，而是留下了变化的过程。',emotions:[{label:'在意与紧张',evidence:'会议前有些紧张',reflection:'这可能反映了你对会议有所在意，不必把紧张当成自己的缺点。'},{label:'轻松与连接',evidence:'和朋友散步',reflection:'和朋友相处、离开忙碌场景，似乎给心情带来了一点空间。'}],suggestion:'下次会议前，先把要说的第一句话写在纸上；结束后给自己留一小段不赶时间的路。',blessing:'愿你明天开会前，也能想起午后那条和朋友并肩走过的路；不需要一下子无所畏惧，先让那杯温热的茶在心里停一会儿。',provider:'deepseek',model:'QA isolated example — not a real AI response',generatedAt:new Date().toISOString()}};
await evaluate("[...document.querySelectorAll('button')].find(b=>b.textContent.includes('刷新日记回顾')).click()");await until("!!document.querySelector('.diary-insight-card')");await metrics('awareness-analysis-1440');await shot('awareness-analysis-example-1440',true);
await send('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:false});await delay(300);await metrics('awareness-analysis-390');await shot('awareness-analysis-example-390');
assert.equal(exceptions.length,0,JSON.stringify(exceptions));await fs.writeFile(out+'/verification.json',JSON.stringify({checks,exceptions,calendarBoundariesPassed:true,realDatabaseWrites:0,backendUsed:'isolated in-memory H2 on 10099',AIVisualExampleOnly:true,apiWrites:writes},null,2));console.log(JSON.stringify({checks:checks.length,exceptions,realDatabaseWrites:0,output:out}));
await send('Fetch.disable');socket.close();
