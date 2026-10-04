import JSZip from 'jszip';
import * as mammoth from 'mammoth';
import * as XLSX from 'xlsx';
import * as pdfjs from 'pdfjs-dist/legacy/build/pdf.mjs';

pdfjs.GlobalWorkerOptions.workerSrc = new URL('pdfjs-dist/legacy/build/pdf.worker.min.mjs', import.meta.url).toString();

export type AgentAttachmentKind = 'text' | 'image' | 'mixed';

export interface ParsedAgentAttachment {
	kind: AgentAttachmentKind;
	mimeType: string;
	textContent: string;
	images: string[];
	summary: string;
	pageCount?: number;
}

export const AGENT_FILE_ACCEPT = [
	'.pdf', '.docx', '.xlsx', '.xls', '.pptx', '.odt', '.ods', '.odp', '.rtf',
	'.txt', '.md', '.csv', '.json', '.log', '.xml', '.yaml', '.yml', '.html', '.htm', '.eml',
	'.toml', '.ini', '.conf', '.properties', '.env', '.tex', '.ipynb', '.svg',
	'.js', '.jsx', '.ts', '.tsx', '.vue', '.java', '.py', '.go', '.rs', '.c', '.h', '.cpp', '.hpp',
	'.cs', '.php', '.rb', '.swift', '.kt', '.kts', '.sql', '.sh', '.ps1', '.bat', '.cmd', '.css', '.scss',
	'.mjs', '.cjs', '.gradle', '.groovy', '.scala', '.r', '.dart', '.lua',
	'.jpg', '.jpeg', '.png', '.webp', '.gif',
	'text/*', 'application/pdf', 'application/json',
	'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
	'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
	'application/vnd.openxmlformats-officedocument.presentationml.presentation',
	'image/jpeg', 'image/png', 'image/webp', 'image/gif',
].join(',');

const TEXT_LIMIT = 16000;
const PDF_PAGE_LIMIT = 12;
const PDF_IMAGE_PAGE_LIMIT = 6;

const extensionOf = (name: string) => {
	const index = name.lastIndexOf('.');
	return index >= 0 ? name.slice(index).toLowerCase() : '';
};

const trimText = (value: string) => value
	.replace(/\u0000/g, '')
	.replace(/[ \t]+\n/g, '\n')
	.replace(/\n{4,}/g, '\n\n\n')
	.trim()
	.slice(0, TEXT_LIMIT);

const readAsDataUrl = (file: Blob) => new Promise<string>((resolve, reject) => {
	const reader = new FileReader();
	reader.onload = () => resolve(String(reader.result || ''));
	reader.onerror = () => reject(reader.error);
	reader.readAsDataURL(file);
});

const loadImage = (url: string) => new Promise<HTMLImageElement>((resolve, reject) => {
	const image = new Image();
	image.onload = () => resolve(image);
	image.onerror = () => reject(new Error('无法读取图片'));
	image.src = url;
});

async function prepareImage(file: File): Promise<ParsedAgentAttachment> {
	const original = await readAsDataUrl(file);
	const extension = extensionOf(file.name);
	const inferredType = file.type || ({ '.png': 'image/png', '.webp': 'image/webp', '.gif': 'image/gif' }[extension] || 'image/jpeg');
	if (inferredType === 'image/gif') {
		if (file.size > 6 * 1024 * 1024) throw new Error('GIF 超过 6 MB，请压缩或转换为 JPG/PNG');
		return { kind: 'image', mimeType: inferredType, textContent: '', images: [original.replace(/^data:[^;,]*;/, `data:${inferredType};`)], summary: 'GIF 图片 · 将使用多模态视觉理解' };
	}
	if (file.size <= 2 * 1024 * 1024 && /^data:image\/(jpeg|png|webp);base64,/.test(original)) {
		return { kind: 'image', mimeType: inferredType, textContent: '', images: [original], summary: '图片 · 将使用多模态视觉理解' };
	}
	const image = await loadImage(original);
	const maxSide = 1800;
	const ratio = Math.min(1, maxSide / Math.max(image.naturalWidth, image.naturalHeight));
	const canvas = document.createElement('canvas');
	canvas.width = Math.max(1, Math.round(image.naturalWidth * ratio));
	canvas.height = Math.max(1, Math.round(image.naturalHeight * ratio));
	const context = canvas.getContext('2d');
	if (!context) throw new Error('浏览器无法创建图片处理画布');
	context.drawImage(image, 0, 0, canvas.width, canvas.height);
	return {
		kind: 'image',
		mimeType: 'image/jpeg',
		textContent: '',
		images: [canvas.toDataURL('image/jpeg', 0.84)],
		summary: `图片 · 已优化至 ${canvas.width}×${canvas.height}`,
	};
}

async function renderPdfPage(page: any): Promise<string> {
	const baseViewport = page.getViewport({ scale: 1 });
	const scale = Math.min(1.6, 1400 / Math.max(baseViewport.width, 1));
	const viewport = page.getViewport({ scale });
	const canvas = document.createElement('canvas');
	canvas.width = Math.ceil(viewport.width);
	canvas.height = Math.ceil(viewport.height);
	const context = canvas.getContext('2d');
	if (!context) throw new Error('浏览器无法创建 PDF 预览画布');
	await page.render({ canvas, canvasContext: context, viewport }).promise;
	return canvas.toDataURL('image/jpeg', 0.8);
}

async function parsePdf(file: File): Promise<ParsedAgentAttachment> {
	const data = new Uint8Array(await file.arrayBuffer());
	const loadingTask = pdfjs.getDocument({ data });
	const pdfDocument = await loadingTask.promise;
	const pageCount = pdfDocument.numPages;
	const pagesToRead = Math.min(pageCount, PDF_PAGE_LIMIT);
	const textParts: string[] = [];
	const images: string[] = [];
	try {
		for (let index = 1; index <= pagesToRead; index += 1) {
			const page = await pdfDocument.getPage(index);
			const text = await page.getTextContent();
			const pageText = (text.items as any[]).map((item) => String(item.str || '')).join(' ').trim();
			if (pageText) textParts.push(`[PDF 第 ${index} 页]\n${pageText}`);
			if (pageText.length < 30 && images.length < PDF_IMAGE_PAGE_LIMIT) {
				try { images.push(await renderPdfPage(page)); }
				catch (error) {
					if (!pageText) throw error;
				}
			}
			if (typeof page.cleanup === 'function') page.cleanup();
		}
	} finally {
		if (typeof pdfDocument.cleanup === 'function') await pdfDocument.cleanup();
		if (typeof loadingTask.destroy === 'function') await loadingTask.destroy();
	}
	const textContent = trimText(textParts.join('\n\n'));
	if (!textContent && !images.length) throw new Error('PDF 中没有可读取的文字或页面');
	const omitted = pageCount > pagesToRead ? `，仅处理前 ${pagesToRead} 页` : '';
	const mode = images.length ? (textContent ? '文字＋扫描页' : '扫描页视觉识别') : '已提取文字';
	return {
		kind: images.length ? (textContent ? 'mixed' : 'image') : 'text',
		mimeType: 'application/pdf',
		textContent,
		images,
		pageCount,
		summary: `PDF · ${pageCount} 页 · ${mode}${omitted}`,
	};
}

async function parseDocx(file: File): Promise<ParsedAgentAttachment> {
	const result = await mammoth.extractRawText({ arrayBuffer: await file.arrayBuffer() });
	const textContent = trimText(result.value || '');
	if (!textContent) throw new Error('Word 文档中没有可读取的文字');
	return { kind: 'text', mimeType: file.type || 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', textContent, images: [], summary: `Word · 已提取 ${textContent.length} 字符` };
}

async function parseSpreadsheet(file: File): Promise<ParsedAgentAttachment> {
	const workbook = XLSX.read(await file.arrayBuffer(), { type: 'array', cellDates: true });
	const parts: string[] = [];
	for (const sheetName of workbook.SheetNames.slice(0, 10)) {
		parts.push(`[工作表：${sheetName}]\n${XLSX.utils.sheet_to_csv(workbook.Sheets[sheetName], { blankrows: false })}`);
	}
	const textContent = trimText(parts.join('\n\n'));
	if (!textContent) throw new Error('表格中没有可读取的单元格');
	return { kind: 'text', mimeType: file.type || 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', textContent, images: [], summary: `表格 · ${workbook.SheetNames.length} 个工作表 · 已提取为结构化文本` };
}

const slideNumber = (name: string) => Number(name.match(/slide(\d+)\.xml$/)?.[1] || 0);

async function parsePptx(file: File): Promise<ParsedAgentAttachment> {
	const zip = await JSZip.loadAsync(await file.arrayBuffer());
	const slideNames = Object.keys(zip.files)
		.filter((name) => /^ppt\/slides\/slide\d+\.xml$/.test(name))
		.sort((left, right) => slideNumber(left) - slideNumber(right));
	const parts: string[] = [];
	for (const name of slideNames.slice(0, 30)) {
		const xml = await zip.file(name)?.async('string');
		if (!xml) continue;
		const doc = new DOMParser().parseFromString(xml, 'application/xml');
		const nodes = Array.from(doc.getElementsByTagNameNS('*', 't'));
		const text = nodes.map((node) => node.textContent || '').filter(Boolean).join(' ').trim();
		if (text) parts.push(`[幻灯片 ${slideNumber(name)}]\n${text}`);
	}
	const textContent = trimText(parts.join('\n\n'));
	if (!textContent) throw new Error('PPTX 中没有可读取的文字');
	return { kind: 'text', mimeType: file.type || 'application/vnd.openxmlformats-officedocument.presentationml.presentation', textContent, images: [], summary: `PPTX · ${slideNames.length} 页 · 已提取文字` };
}

async function parseOpenDocument(file: File): Promise<ParsedAgentAttachment> {
	const zip = await JSZip.loadAsync(await file.arrayBuffer());
	const xml = await zip.file('content.xml')?.async('string');
	if (!xml) throw new Error('开放文档缺少 content.xml');
	const doc = new DOMParser().parseFromString(xml, 'application/xml');
	const textContent = trimText(doc.documentElement.textContent || '');
	if (!textContent) throw new Error('文档中没有可读取的文字');
	return { kind: 'text', mimeType: file.type || 'application/vnd.oasis.opendocument.text', textContent, images: [], summary: `开放文档 · 已提取 ${textContent.length} 字符` };
}

async function parsePlainText(file: File): Promise<ParsedAgentAttachment> {
	let text = await file.text();
	const extension = extensionOf(file.name);
	if (extension === '.html' || extension === '.htm') {
		text = new DOMParser().parseFromString(text, 'text/html').body.textContent || '';
	} else if (extension === '.rtf') {
		text = text.replace(/\\'[0-9a-fA-F]{2}/g, ' ').replace(/\\[a-zA-Z]+-?\d* ?/g, ' ').replace(/[{}]/g, ' ');
	}
	const textContent = trimText(text);
	if (!textContent) throw new Error('文件中没有可读取的文字');
	return { kind: 'text', mimeType: file.type || 'text/plain', textContent, images: [], summary: `文字/代码 · 已读取 ${textContent.length} 字符` };
}

const plainTextExtensions = new Set([
	'.txt', '.md', '.csv', '.json', '.log', '.xml', '.yaml', '.yml', '.html', '.htm', '.eml', '.rtf',
	'.toml', '.ini', '.conf', '.properties', '.env', '.tex', '.ipynb', '.svg',
	'.js', '.jsx', '.ts', '.tsx', '.vue', '.java', '.py', '.go', '.rs', '.c', '.h', '.cpp', '.hpp',
	'.cs', '.php', '.rb', '.swift', '.kt', '.kts', '.sql', '.sh', '.ps1', '.bat', '.cmd', '.css', '.scss',
	'.mjs', '.cjs', '.gradle', '.groovy', '.scala', '.r', '.dart', '.lua',
]);

export async function parseAgentAttachment(file: File): Promise<ParsedAgentAttachment> {
	try {
		const extension = extensionOf(file.name);
		if (['.jpg', '.jpeg', '.png', '.webp', '.gif'].includes(extension) || file.type.startsWith('image/')) return await prepareImage(file);
		if (extension === '.pdf' || file.type === 'application/pdf') return await parsePdf(file);
		if (extension === '.docx') return await parseDocx(file);
		if (extension === '.xlsx' || extension === '.xls') return await parseSpreadsheet(file);
		if (extension === '.pptx') return await parsePptx(file);
		if (['.odt', '.ods', '.odp'].includes(extension)) return await parseOpenDocument(file);
		if (plainTextExtensions.has(extension) || file.type.startsWith('text/')) return await parsePlainText(file);
		if (['.doc', '.ppt'].includes(extension)) throw new Error('旧版 DOC/PPT 暂不支持，请先另存为 DOCX/PPTX');
		if (['.zip', '.rar', '.7z'].includes(extension)) throw new Error('压缩包不会自动解压，请先选择其中需要分析的文件');
		if (file.type.startsWith('audio/') || file.type.startsWith('video/')) throw new Error('当前 DeepSeek 接口不接收音频或视频文件，请先转成文字');
		throw new Error(`暂不支持 ${extension || file.type || '未知'} 类型`);
	} catch (error) {
		const message = error instanceof Error ? error.message : '文件读取失败';
		if (/password|encrypted/i.test(message)) throw new Error('PDF 已加密，请解除密码保护后再上传');
		if (/invalid pdf|missing pdf|format error/i.test(message)) throw new Error('PDF 文件结构异常或已损坏');
		throw error;
	}
}
