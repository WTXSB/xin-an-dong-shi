<template>
	<div class="chat-page">
		<header class="chat-topbar">
			<div class="brand-block">
				<div class="brand-mark">安</div>
				<div>
					<strong>安心对话</strong>
					<span><i></i> DeepSeek 陪伴式对话</span>
				</div>
			</div>
			<button class="new-chat" type="button" @click="resetConversation"><span aria-hidden="true">＋</span>新对话</button>
		</header>

		<main ref="messageContainer" class="conversation-scroll">
			<div class="conversation-column">
				<section v-if="messages.length === 1" class="welcome-panel">
					<div class="welcome-mark">安</div>
					<p>安心对话</p>
					<h1>今天想从哪里聊起？</h1>
					<span>你可以打字、说话，或附上经过你确认的文件与图片。</span>
					<div class="suggestion-grid">
						<button v-for="question in suggestedQuestions" :key="question.title" type="button" @click="selectQuestion(question.prompt)">
							<strong>{{ question.title }}</strong><small>{{ question.description }}</small>
						</button>
					</div>
				</section>

				<section v-else class="message-list" aria-live="polite">
					<article v-for="message in messages.slice(1)" :key="message.id" :class="['message-row', message.role]">
						<div v-if="message.role === 'assistant'" class="assistant-mark">安</div>
						<div class="message-body">
							<div v-if="message.attachments?.length" class="sent-attachments">
								<div v-for="attachment in message.attachments" :key="attachment.name" class="sent-file">
									<span class="file-icon">{{ attachmentIcon(attachment) }}</span>
									<div><strong>{{ attachment.name }}</strong><small>{{ attachment.summary }} · 已批准发送</small></div>
								</div>
							</div>
							<div class="message-content">{{ message.content }}</div>
							<div v-if="message.role === 'assistant'" class="message-actions">
								<button type="button" title="复制回答" @click="copyMessage(message.content)">复制</button>
								<span v-if="message.provider">{{ providerLabel(message.provider) }}</span>
							</div>
						</div>
					</article>
					<article v-if="loading" class="message-row assistant">
						<div class="assistant-mark">安</div>
						<div class="message-body typing-state"><span></span><span></span><span></span></div>
					</article>
				</section>
			</div>
		</main>

		<footer class="composer-area">
			<div class="composer-wrap">
				<div v-if="attachments.length" class="attachment-tray">
					<div v-for="attachment in attachments" :key="attachment.id" :class="['attachment-card', { approved: attachment.approved }]">
						<span class="file-icon">{{ attachmentIcon(attachment) }}</span>
						<div class="file-summary"><strong>{{ attachment.name }}</strong><small>{{ attachment.summary }} · {{ formatSize(attachment.size) }}</small></div>
						<label class="approval-control">
							<input v-model="attachment.approved" type="checkbox" />
							<span>{{ attachment.approved ? '已批准发送' : '批准后发送' }}</span>
						</label>
						<button class="remove-file" type="button" title="移除附件" @click="removeAttachment(attachment.id)">×</button>
					</div>
				</div>

				<div class="composer-box">
					<textarea ref="textareaRef" v-model="userInput" rows="1" maxlength="2000" placeholder="给安小宁发消息" aria-label="输入消息" @input="resizeTextarea" @keydown.enter.exact.prevent="sendMessage"></textarea>
					<div class="composer-toolbar">
						<div class="left-tools">
							<input ref="fileInput" class="sr-only" type="file" multiple :accept="acceptedFileTypes" @change="handleFileSelection" />
							<button class="round-tool" type="button" title="添加文件或图片" aria-label="添加文件或图片" :disabled="processingFiles" @click="openFilePicker">
								<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5v14M5 12h14" /></svg>
							</button>
							<button :class="['voice-tool', { listening: isListening }]" type="button" :title="isListening ? '停止语音输入' : '语音输入'" :aria-label="isListening ? '停止语音输入' : '语音输入'" @click="toggleVoiceInput">
								<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="9" y="3" width="6" height="11" rx="3" /><path d="M6 10a6 6 0 0 0 12 0M12 16v4M9 20h6" /></svg>
								<span>{{ isListening ? '正在听…' : '语音' }}</span>
							</button>
						</div>
						<button class="send-button" type="button" :disabled="!canSend || loading || processingFiles" title="发送" aria-label="发送消息" @click="sendMessage">
							<svg v-if="!loading" viewBox="0 0 24 24" aria-hidden="true"><path d="M12 19V5M6 11l6-6 6 6" /></svg>
							<span v-else class="send-spinner"></span>
						</button>
					</div>
				</div>

				<div class="composer-meta">
					<label><input v-model="saveConversation" type="checkbox" /> 保存本次对话</label>
					<span v-if="processingFiles" class="voice-hint">正在安全读取文件，请稍候…</span>
					<span v-else-if="voiceHint" class="voice-hint">{{ voiceHint }}</span>
					<span v-else>附件只有经你逐项批准后，才会随消息发送</span>
				</div>
				<p class="safety-note">安小宁用于情绪陪伴和信息梳理，不构成医学诊断或紧急医疗服务。</p>
			</div>
		</footer>
	</div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '/@/utils/request';
import { AGENT_FILE_ACCEPT, parseAgentAttachment, type AgentAttachmentKind } from '/@/utils/attachmentParser';
import { useUserInfo } from '/@/stores/userInfo';
import { storeToRefs } from 'pinia';

type SentAttachment = { name: string; size: number; kind: AgentAttachmentKind; summary: string };
type PendingAttachment = SentAttachment & { id: string; mimeType: string; textContent: string; images: string[]; approved: boolean };
type ChatMessage = { id: number; role: 'assistant' | 'user'; content: string; provider?: string; attachments?: SentAttachment[] };

const MAX_FILE_SIZE = 20 * 1024 * 1024;
const MAX_ATTACHMENTS = 5;
const acceptedFileTypes = AGENT_FILE_ACCEPT;
const stores = useUserInfo();
const { userInfos } = storeToRefs(stores);
const messageContainer = ref<HTMLElement>();
const textareaRef = ref<HTMLTextAreaElement>();
const fileInput = ref<HTMLInputElement>();
const userInput = ref('');
const loading = ref(false);
const processingFiles = ref(false);
const saveConversation = ref(true);
const isListening = ref(false);
const voiceHint = ref('');
const attachments = ref<PendingAttachment[]>([]);
let recognition: any = null;
let messageSeed = Date.now();
const createMessageId = () => ++messageSeed;

const messages = ref<ChatMessage[]>([
	{ id: createMessageId(), role: 'assistant', content: '你好，我是安小宁。你可以把此刻的感受说给我听，我们先不急着解决，先一起看见它。' },
]);

const suggestedQuestions = [
	{ title: '整理此刻的感受', description: '我有点乱，不知道从哪里说起', prompt: '我现在心里有点乱，可以陪我梳理一下吗？' },
	{ title: '把任务拆小一点', description: '一想到任务就紧绷', prompt: '我一想到手头的任务就紧绷，能陪我把它拆小一点吗？' },
	{ title: '做一次短暂放松', description: '用三分钟让身体慢下来', prompt: '请带我做一次三分钟的放松练习。' },
	{ title: '阅读文件或图片', description: '添加后逐项批准发送', prompt: '我附带了一份资料，请帮我梳理其中最需要关注的内容。' },
];

const approvedAttachments = computed(() => attachments.value.filter((item) => item.approved));
const hasUnapprovedAttachments = computed(() => attachments.value.some((item) => !item.approved));
const canSend = computed(() => Boolean(userInput.value.trim()) || approvedAttachments.value.length > 0);

const selectQuestion = (question: string) => {
	userInput.value = question;
	nextTick(() => { resizeTextarea(); textareaRef.value?.focus(); });
};
const providerLabel = (provider?: string) => (provider === 'deepseek' ? 'DeepSeek' : provider === 'local-fallback' ? '本地备用回复' : '');
const formatSize = (size: number) => size < 1024 ? `${size} B` : size < 1024 * 1024 ? `${(size / 1024).toFixed(1)} KB` : `${(size / 1024 / 1024).toFixed(1)} MB`;
const attachmentIcon = (attachment: SentAttachment) => {
	if (attachment.kind === 'image') return '图';
	if (attachment.kind === 'mixed') return '图文';
	const extension = attachment.name.split('.').pop()?.toLowerCase();
	if (extension === 'pdf') return 'PDF';
	if (['xls', 'xlsx', 'ods', 'csv'].includes(extension || '')) return '表';
	if (['pptx', 'odp'].includes(extension || '')) return '演';
	return '文';
};

const copyMessage = async (text: string) => {
	try { await navigator.clipboard.writeText(text); ElMessage.success('回答已复制'); }
	catch { ElMessage.error('复制失败，可以手动选择文字复制'); }
};
const scrollToBottom = async () => {
	await nextTick();
	if (messageContainer.value) messageContainer.value.scrollTop = messageContainer.value.scrollHeight;
};
const resizeTextarea = () => {
	const textarea = textareaRef.value;
	if (!textarea) return;
	textarea.style.height = 'auto';
	textarea.style.height = `${Math.min(textarea.scrollHeight, 168)}px`;
};

const openFilePicker = () => fileInput.value?.click();

const handleFileSelection = async (event: Event) => {
	const input = event.target as HTMLInputElement;
	const selectedFiles = Array.from(input.files || []);
	input.value = '';
	if (!selectedFiles.length) return;
	const remainingSlots = MAX_ATTACHMENTS - attachments.value.length;
	if (remainingSlots <= 0) { ElMessage.warning(`每次最多添加 ${MAX_ATTACHMENTS} 份资料`); return; }

	processingFiles.value = true;
	try {
		for (const file of selectedFiles.slice(0, remainingSlots)) {
			if (file.size > MAX_FILE_SIZE) { ElMessage.warning(`${file.name} 超过 20 MB，暂未添加`); continue; }
			try {
				const parsed = await parseAgentAttachment(file);
				attachments.value.push({
					id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
					name: file.name,
					size: file.size,
					kind: parsed.kind,
					summary: parsed.summary,
					mimeType: parsed.mimeType,
					textContent: parsed.textContent,
					images: parsed.images,
					approved: false,
				});
			} catch (error) {
				ElMessage.error(`${file.name}：${error instanceof Error ? error.message : '读取失败'}`);
			}
		}
	} finally {
		processingFiles.value = false;
	}
	if (selectedFiles.length > remainingSlots) ElMessage.info(`已保留前 ${remainingSlots} 份资料`);
};
const removeAttachment = (id: string) => { attachments.value = attachments.value.filter((item) => item.id !== id); };

const toggleVoiceInput = () => {
	if (isListening.value && recognition) { recognition.stop(); return; }
	const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
	if (!SpeechRecognition) { ElMessage.warning('当前浏览器不支持语音转文字，请使用最新版 Chrome 或 Edge'); return; }
	const startingText = userInput.value.trim();
	recognition = new SpeechRecognition();
	recognition.lang = 'zh-CN';
	recognition.continuous = false;
	recognition.interimResults = true;
	recognition.maxAlternatives = 1;
	recognition.onstart = () => { isListening.value = true; voiceHint.value = '正在听你说话，再点一次麦克风即可停止'; };
	recognition.onresult = (event: any) => {
		let transcript = '';
		for (let index = event.resultIndex; index < event.results.length; index += 1) transcript += event.results[index][0].transcript;
		userInput.value = `${startingText}${startingText && transcript ? ' ' : ''}${transcript}`;
		nextTick(resizeTextarea);
	};
	recognition.onerror = (event: any) => {
		if (event.error === 'not-allowed' || event.error === 'service-not-allowed') ElMessage.warning('需要你允许浏览器使用麦克风，才能进行语音输入');
		else if (event.error !== 'no-speech' && event.error !== 'aborted') ElMessage.error('这次没有听清，可以再试一次');
	};
	recognition.onend = () => { isListening.value = false; voiceHint.value = userInput.value.trim() ? '语音已转成文字，请确认内容后再发送' : ''; recognition = null; };
	recognition.start();
};

const resetComposer = () => { userInput.value = ''; attachments.value = []; voiceHint.value = ''; nextTick(resizeTextarea); };
const resetConversation = async () => {
	if (messages.value.length > 1 || userInput.value.trim() || attachments.value.length) {
		try {
			await ElMessageBox.confirm('开始新对话后，当前页面里的消息会被清空。已经保存到记录中的内容不会受影响。', '开始新对话？', { confirmButtonText: '开始新对话', cancelButtonText: '继续当前对话', type: 'warning' });
		} catch { return; }
	}
	messages.value = [{ id: createMessageId(), role: 'assistant', content: '你好，我是安小宁。你可以把此刻的感受说给我听，我们先不急着解决，先一起看见它。' }];
	resetComposer();
};

const sendMessage = async () => {
	const typedContent = userInput.value.trim();
	const approved = approvedAttachments.value;
	if ((!typedContent && !approved.length) || loading.value || processingFiles.value) return;
	if (hasUnapprovedAttachments.value) { ElMessage.warning('仍有附件未批准。请先批准发送或将它移除'); return; }
	const content = typedContent || '请阅读我附带的资料，并帮我梳理其中最需要关注的内容。';
	const attachmentSnapshot = approved.map((item) => ({ name: item.name, size: item.size, kind: item.kind, summary: item.summary }));
	const contextBeforeCurrent = messages.value.slice(-8).map((item) => ({ role: item.role, content: item.content }));
	messages.value.push({ id: createMessageId(), role: 'user', content, attachments: attachmentSnapshot });
	const payloadAttachments = approved.map((item) => ({ name: item.name, size: item.size, kind: item.kind, mimeType: item.mimeType, textContent: item.textContent, images: item.images, approved: true }));
	resetComposer();
	loading.value = true;
	scrollToBottom();

	try {
		const res = await request.post('/api/ai/chat', { message: content, username: userInfos.value.userName, saveConversation: saveConversation.value, messages: contextBeforeCurrent, attachments: payloadAttachments }, { timeout: 120000 });
		if (String(res?.code) !== '0') throw new Error(res?.msg || '附件没有被正确接收');
		messages.value.push({ id: createMessageId(), role: 'assistant', content: res?.data?.reply || '我在这里。我们先慢慢呼吸一次，再把事情拆小一点。', provider: res?.data?.provider });
	} catch (error) {
		const reason = error instanceof Error ? error.message : '安小宁暂时没有连上';
		ElMessage.error(reason);
		messages.value.push({ id: createMessageId(), role: 'assistant', content: `刚刚的发送没有成功：${reason}。你可以重新添加附件后再试。`, provider: 'local-fallback' });
	} finally { loading.value = false; scrollToBottom(); }
};

onBeforeUnmount(() => { if (recognition) recognition.abort(); });
</script>

<style scoped>
.chat-page { height: calc(100vh - 84px); min-height: 620px; display: flex; flex-direction: column; background: #f9faf8; color: #2f312f; -webkit-user-select: text; user-select: text; }
.chat-topbar { height: 62px; padding: 0 24px; display: flex; align-items: center; justify-content: space-between; flex: 0 0 62px; background: rgba(255,255,255,.9); border-bottom: 1px solid #e7e9e5; backdrop-filter: blur(14px); }
.brand-block,.brand-block>div:last-child { display: flex; align-items: center; }
.brand-block { gap: 11px; }
.brand-block>div:last-child { align-items: flex-start; flex-direction: column; gap: 2px; }
.brand-mark,.welcome-mark,.assistant-mark { display: grid; place-items: center; background: linear-gradient(145deg,#c7ddb9,#e6d79c); color: #3e493a; font-weight: 800; }
.brand-mark { width: 34px; height: 34px; border-radius: 11px; }
.brand-block strong { font-size: 15px; }
.brand-block span { display: flex; align-items: center; gap: 5px; font-size: 11px; color: #858a83; }
.brand-block i { width: 6px; height: 6px; border-radius: 50%; background: #63a673; box-shadow: 0 0 0 3px rgba(99,166,115,.12); }
.new-chat { height: 36px; padding: 0 13px; display: inline-flex; align-items: center; gap: 6px; border: 1px solid #dfe3dc; border-radius: 10px; background: #fff; color: #555b54; cursor: pointer; }
.new-chat:hover { background: #f3f5f1; }
.conversation-scroll { min-height: 0; flex: 1; overflow-y: auto; scroll-behavior: smooth; }
.conversation-column { width: min(860px,calc(100% - 40px)); min-height: 100%; margin: 0 auto; }
.welcome-panel { min-height: 100%; padding: 68px 0 34px; display: flex; align-items: center; flex-direction: column; text-align: center; }
.welcome-mark { width: 54px; height: 54px; margin-bottom: 18px; border-radius: 18px; font-size: 22px; box-shadow: 0 12px 30px rgba(82,111,72,.14); }
.welcome-panel>p { margin: 0 0 8px; color: #73806f; font-size: 13px; font-weight: 700; letter-spacing: .12em; }
.welcome-panel h1 { margin: 0; font-size: clamp(27px,3.2vw,38px); font-weight: 650; letter-spacing: -.04em; }
.welcome-panel>span { margin-top: 13px; color: #858a83; font-size: 14px; }
.suggestion-grid { width: 100%; margin-top: 36px; display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 12px; }
.suggestion-grid button { padding: 17px 18px; display: flex; align-items: flex-start; flex-direction: column; gap: 5px; text-align: left; border: 1px solid #e2e5df; border-radius: 16px; background: rgba(255,255,255,.75); color: #414640; cursor: pointer; transition: .2s ease; }
.suggestion-grid button:hover { transform: translateY(-1px); border-color: #b9cab3; background: #fff; box-shadow: 0 10px 26px rgba(65,80,59,.07); }
.suggestion-grid small { color: #8a8e88; }
.message-list { padding: 34px 0 50px; }
.message-row { display: flex; gap: 13px; margin-bottom: 28px; align-items: flex-start; }
.message-row.user { justify-content: flex-end; }
.assistant-mark { width: 31px; height: 31px; flex: 0 0 31px; border-radius: 10px; font-size: 13px; }
.message-body { max-width: min(720px,calc(100% - 48px)); color: #343734; line-height: 1.78; white-space: pre-wrap; word-break: break-word; }
.message-row.user .message-body { padding: 11px 15px; border-radius: 18px 18px 4px 18px; background: #e9eee6; }
.message-content { font-size: 15px; }
.message-actions { margin-top: 8px; display: flex; align-items: center; gap: 10px; font-size: 11px; color: #969b94; }
.message-actions button { padding: 0; border: 0; background: transparent; color: #7c827b; cursor: pointer; }
.sent-attachments { margin-bottom: 9px; display: flex; flex-wrap: wrap; gap: 7px; }
.sent-file { min-width: 210px; padding: 9px 10px; display: flex; align-items: center; gap: 9px; border: 1px solid #dce2d8; border-radius: 12px; background: rgba(255,255,255,.72); }
.sent-file>div,.file-summary { min-width: 0; display: flex; flex-direction: column; }
.sent-file strong,.file-summary strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; }
.sent-file small,.file-summary small { color: #8c918b; font-size: 10px; }
.file-icon { width: 30px; height: 30px; display: grid; place-items: center; flex: 0 0 30px; border-radius: 8px; background: #edf3e9; color: #6b8066; font-size: 11px; font-weight: 800; }
.typing-state { padding-top: 8px; display: flex; gap: 5px; }
.typing-state span { width: 7px; height: 7px; border-radius: 50%; background: #879984; animation: typing 1.2s infinite ease-in-out; }
.typing-state span:nth-child(2) { animation-delay: .15s; }
.typing-state span:nth-child(3) { animation-delay: .3s; }
@keyframes typing { 0%,60%,100% { opacity: .3; transform: translateY(0); } 30% { opacity: 1; transform: translateY(-3px); } }
.composer-area { position: relative; flex: 0 0 auto; padding: 14px 20px 17px; background: linear-gradient(180deg,rgba(249,250,248,0),#f9faf8 22%); }
.composer-wrap { width: min(860px,100%); margin: 0 auto; }
.attachment-tray { margin-bottom: 8px; display: grid; grid-template-columns: repeat(3,minmax(0,1fr)); gap: 8px; }
.attachment-card { position: relative; min-width: 0; padding: 9px 30px 9px 9px; display: grid; grid-template-columns: 30px minmax(0,1fr); gap: 8px; align-items: center; border: 1px solid #ead8c7; border-radius: 13px; background: #fffaf5; }
.attachment-card.approved { border-color: #c8d9c2; background: #f5f9f3; }
.approval-control { grid-column: 1/-1; display: flex; align-items: center; gap: 6px; color: #766d64; font-size: 11px; cursor: pointer; }
.approval-control input,.composer-meta input { accent-color: #637a5f; }
.remove-file { position: absolute; top: 6px; right: 7px; width: 22px; height: 22px; border: 0; border-radius: 50%; background: transparent; color: #8e938d; font-size: 17px; cursor: pointer; }
.remove-file:hover { background: rgba(75,84,72,.08); }
.composer-box { padding: 13px 13px 10px 16px; border: 1px solid #d9ddd7; border-radius: 24px; background: #fff; box-shadow: 0 10px 32px rgba(52,63,49,.1); transition: border-color .2s,box-shadow .2s; }
.composer-box:focus-within { border-color: #aabca4; box-shadow: 0 12px 34px rgba(52,63,49,.13); }
.composer-box textarea { width: 100%; height: 26px; max-height: 168px; padding: 0 2px; resize: none; overflow-y: auto; border: 0; outline: none; background: transparent; color: #303330; font: inherit; font-size: 15px; line-height: 1.7; }
.composer-box textarea::placeholder { color: #a0a59f; }
.composer-toolbar { margin-top: 8px; display: flex; align-items: center; justify-content: space-between; }
.left-tools { display: flex; align-items: center; gap: 7px; }
.round-tool,.voice-tool,.send-button { display: inline-flex; align-items: center; justify-content: center; border: 0; cursor: pointer; }
.round-tool { width: 34px; height: 34px; border: 1px solid #e0e3de; border-radius: 50%; background: #fff; color: #555b55; }
.round-tool:hover,.voice-tool:hover { background: #f1f4ef; }
.round-tool:disabled { opacity: .5; cursor: wait; }
.round-tool svg,.voice-tool svg,.send-button svg { fill: none; stroke: currentColor; stroke-width: 1.9; stroke-linecap: round; stroke-linejoin: round; }
.round-tool svg { width: 18px; height: 18px; }
.voice-tool { height: 34px; padding: 0 10px; gap: 5px; border-radius: 17px; background: transparent; color: #666c65; }
.voice-tool svg { width: 17px; height: 17px; }
.voice-tool span { font-size: 12px; }
.voice-tool.listening { background: #fff0eb; color: #c45d4b; animation: listeningPulse 1.5s infinite; }
@keyframes listeningPulse { 50% { box-shadow: 0 0 0 5px rgba(196,93,75,.08); } }
.send-button { width: 36px; height: 36px; border-radius: 50%; background: #344238; color: #fff; }
.send-button:disabled { background: #d9ddd8; color: #fff; cursor: not-allowed; }
.send-button svg { width: 18px; height: 18px; stroke-width: 2.2; }
.send-spinner { width: 15px; height: 15px; border: 2px solid rgba(255,255,255,.45); border-top-color: #fff; border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.composer-meta { min-height: 25px; padding: 7px 4px 0; display: flex; align-items: center; justify-content: space-between; gap: 12px; color: #8b908a; font-size: 11px; }
.composer-meta label { display: flex; align-items: center; gap: 5px; white-space: nowrap; }
.voice-hint { color: #7a6a58; }
.safety-note { margin: 1px 0 0; text-align: center; color: #a0a49f; font-size: 10px; }
.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0,0,0,0); white-space: nowrap; border: 0; }
@media (max-width:760px) {
	.chat-page { height: calc(100vh - 64px); min-height: 540px; }
	.chat-topbar { padding: 0 14px; }
	.conversation-column { width: calc(100% - 28px); }
	.welcome-panel { padding-top: 42px; }
	.suggestion-grid { grid-template-columns: 1fr; margin-top: 26px; }
	.message-list { padding-top: 24px; }
	.message-body { max-width: calc(100% - 42px); }
	.composer-area { padding: 10px 10px 12px; }
	.attachment-tray { grid-template-columns: 1fr; }
	.composer-meta { align-items: flex-start; flex-direction: column; gap: 3px; }
	.new-chat { padding: 0 10px; }
}
</style>
