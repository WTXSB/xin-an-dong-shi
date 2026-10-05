import fs from 'node:fs/promises';
import { parse as parseSfc } from '@vue/compiler-sfc';
import { baseParse } from '@vue/compiler-dom';

const configs = {
 aboutProduct: { frames:['hero-content','evidence-panel','care-main','care-side'], top:'about-hero', middle:'quick-grid' },
 imgPredict: { frames:['hero','privacy-panel','panel','body-signal-band'], top:'hero', middle:'workbench' },
 videoPredict: { frames:['hero','privacy-panel','preview-panel','feedback-panel'], top:'hero', middle:'status-row' },
 cameraPredict: { frames:['hero','privacy-panel','preview-panel','feedback-panel'], top:'hero', middle:'status-row' },
 dataView: { frames:['page-heading','records-card'], top:'page-heading', middle:'stat-grid' },
 trashMap: { frames:['map-hero','control-section','fallback-card'], top:'map-hero', middle:'content' },
 trashRecords: { frames:['hero','empty-state','record-main'], top:'hero', middle:'summary-row' },
 smartChat: { frames:['welcome-panel'], top:'suggestion-grid', middle:'' },
};
const result=[];
for(const [page,config] of Object.entries(configs)) {
 const file=`src/views/${page}/index.vue`;
 const source=await fs.readFile(file,'utf8');
 const template=parseSfc(source).descriptor.template.content;
 const ast=baseParse(template);
 const edits=[]; let frameIndex=0;
 const walk=node=>{
  if(node.type===1) {
   const classes=(node.props.find(p=>p.type===6&&p.name==='class')?.value?.content || '').split(/\s+/);
   if(classes.some(c=>config.frames.includes(c))) {
    const closing=node.loc.source.lastIndexOf('</'+node.tag+'>');
    if(closing<0) throw new Error('No closing tag for '+node.tag);
    const variant=['clouds','garden','rest'][frameIndex++ % 3];
    edits.push({at:node.loc.start.offset+closing,text:`\n\t\t\t<HealingIllustratedFrame variant="${variant}" />\n\t\t\t`});
   }
   if(classes.includes(config.top)) {
    const at=page==='smartChat' ? node.loc.start.offset : node.loc.end.offset;
    edits.push({at,text:'\n\t\t\t<HealingDecorationStrip variant="sky" />\n'});
    if(page==='smartChat') edits.push({at:node.loc.end.offset,text:'\n\t\t\t<HealingDecorationStrip variant="garden" />\n'});
   }
   if(config.middle && classes.includes(config.middle)) edits.push({at:node.loc.end.offset,text:'\n\t\t\t<HealingDecorationStrip variant="garden" />\n'});
  }
  for(const child of node.children||[]) walk(child);
 };
 walk(ast);
 if(page!=='smartChat') {
  const root=ast.children.find(n=>n.type===1);
  // Footer art stays in normal flow, not over any original content.
  edits.push({at:root.loc.start.offset+root.loc.source.lastIndexOf('</'+root.tag+'>'),text:'\n\t\t<HealingDecorationStrip variant="rest" />\n\t'});
 }
 result.push({file,template,edits});
}
console.log(JSON.stringify(result));
