import fs from 'node:fs/promises';
import { execFileSync } from 'node:child_process';
import { parse as parseSfc } from '@vue/compiler-sfc';
import { baseParse } from '@vue/compiler-dom';

const pages = ['aboutProduct', 'imgPredict', 'videoPredict', 'cameraPredict', 'dataView', 'trashMap', 'trashRecords', 'smartChat'];
// 固定改造前版本，避免提交后用 HEAD 自比对；也可指定其他对照版本。
const baselineRef = process.env.HEALING_BASE_REF || 'b3d61af12e0c68223c5c15824455f4fd4dfe558d';
const normalize = (text) => text.replace(/\r\n/g, '\n');
function templateContract(content) {
	const texts = [];
	const directives = [];
	const walk = (node) => {
		if (node.type === 2 && node.content.trim()) texts.push(node.content.replace(/\s+/g, ' ').trim());
		if (node.type === 5) texts.push('{{' + node.content.content + '}}');
		for (const prop of node.props || []) {
			if (prop.type === 7 && !(prop.name === 'bind' && ['class', 'style'].includes(prop.arg?.content))) directives.push([prop.name, prop.arg?.content || '', prop.exp?.content || '']);
		}
		for (const child of node.children || []) walk(child);
	};
	walk(baseParse(content));
	return { texts, directives };
}
const report = [];
for (const page of pages) {
	const relative = `src/views/${page}/index.vue`;
	const baseline = execFileSync('git', ['show', `${baselineRef}:yolo-moudle/face/yolo_face_detection_vue/${relative}`], { encoding: 'utf8' });
	const current = await fs.readFile(relative, 'utf8');
	const original = parseSfc(baseline).descriptor;
	const updated = parseSfc(current).descriptor;
	const a = templateContract(original.template.content);
	const b = templateContract(updated.template.content);
	const result = {
		page,
		textPreserved: JSON.stringify(a.texts) === JSON.stringify(b.texts),
		directivesPreserved: JSON.stringify(a.directives) === JSON.stringify(b.directives),
		scriptPreserved: normalize(original.script?.content || original.scriptSetup?.content || '') === normalize(updated.script?.content || updated.scriptSetup?.content || ''),
	};
	report.push(result);
}
await fs.writeFile('qa/preservation-report.json', JSON.stringify(report, null, 2));
console.log(JSON.stringify(report, null, 2));
if (report.some(row => !row.textPreserved || !row.directivesPreserved || !row.scriptPreserved)) process.exitCode = 1;
