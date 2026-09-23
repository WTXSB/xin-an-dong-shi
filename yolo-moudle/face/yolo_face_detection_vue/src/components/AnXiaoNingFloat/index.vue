<template>
	<div
		class="anxiaoning-float"
		:class="{ dragging: dragState.dragging }"
		:style="{ left: `${position.x}px`, top: `${position.y}px` }"
	>
		<button
			class="float-core"
			type="button"
			aria-label="打开安小宁陪伴对话"
			title="拖动我，或点击打开安小宁"
			@click="togglePanel"
			@pointerdown="startDrag"
		>
			<span class="breath-ring"></span>
			<span class="face">安</span>
		</button>

		<section v-if="opened" :class="['chat-panel', panelPlacement]">
			<header>
				<div>
					<strong>安小宁</strong>
					<p>我在这里，陪你慢一点。</p>
				</div>
				<button type="button" aria-label="关闭安小宁" @click="opened = false">×</button>
			</header>

			<div class="privacy-note">
				只发送你主动输入的文字；不会自动上传图片、摄像头画面或识别记录。对话默认保存为你的疗愈回看记录，你也可以取消保存。
			</div>

			<div class="messages" ref="messageBoxRef">
				<div v-for="item in messages" :key="item.id" :class="['message', item.role]">
					<span class="message-content">{{ item.content }}</span>
					<button class="copy-message" type="button" title="复制这条消息" aria-label="复制这条消息" @click="copyMessage(item.content)">
						复制
					</button>
				</div>
			</div>

			<div class="suggestions" v-if="messages.length <= 1">
				<button v-for="item in suggestions" :key="item" type="button" @click="ask(item)">
					{{ item }}
				</button>
			</div>

			<label class="save-row">
				<input v-model="saveConversation" type="checkbox" />
				<span>保存这次对话，方便之后回看自己的照顾过程</span>
			</label>

			<div class="input-row">
				<textarea v-model="input" placeholder="把此刻的感觉写一点点就好" @keydown.ctrl.enter.prevent="send"></textarea>
				<button type="button" :disabled="loading || !input.trim()" @click="send">
					{{ loading ? '陪你想想' : '发送' }}
				</button>
			</div>
		</section>
	</div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import request from '/@/utils/request';
import { useUserInfo } from '/@/stores/userInfo';
import { storeToRefs } from 'pinia';

type Message = {
	id: number;
	role: 'assistant' | 'user';
	content: string;
};

const STORAGE_KEY = 'anxiaoning-float-position';
const FLOAT_SIZE = 64;
const EDGE_PADDING = 14;

const stores = useUserInfo();
const { userInfos } = storeToRefs(stores);
const opened = ref(false);
const input = ref('');
const loading = ref(false);
const saveConversation = ref(true);
const messageBoxRef = ref<HTMLElement>();
const position = reactive({ x: 0, y: 0 });
const viewport = reactive({ width: 1280, height: 720 });
const dragState = reactive({
	dragging: false,
	moved: false,
	startX: 0,
	startY: 0,
	offsetX: 0,
	offsetY: 0,
});

const messages = ref<Message[]>([
	{
		id: Date.now(),
		role: 'assistant',
		content: '嗨，我是安小宁。你不需要马上变好，我们可以先从看见此刻开始。',
	},
]);

const suggestions = ['我现在有点焦虑', '帮我做一次呼吸练习', '我想找回学习动力'];

const panelPlacement = computed(() => {
	const vertical = position.y < 360 ? 'panel-below' : 'panel-above';
	const horizontal = position.x > viewport.width - 420 ? 'panel-left' : 'panel-right';
	return `${vertical} ${horizontal}`;
});

const togglePanel = () => {
	if (dragState.moved) {
		dragState.moved = false;
		return;
	}
	opened.value = !opened.value;
};

const ask = (text: string) => {
	input.value = text;
	send();
};

const copyMessage = async (text: string) => {
	try {
		await navigator.clipboard.writeText(text);
		ElMessage.success('已复制');
	} catch {
		ElMessage.error('复制失败，可以手动选中文字复制');
	}
};

const scrollToBottom = async () => {
	await nextTick();
	if (messageBoxRef.value) messageBoxRef.value.scrollTop = messageBoxRef.value.scrollHeight;
};

const send = async () => {
	const content = input.value.trim();
	if (!content || loading.value) return;

	messages.value.push({ id: Date.now(), role: 'user', content });
	input.value = '';
	loading.value = true;
	scrollToBottom();

	try {
		const chatContext = messages.value.slice(-8).map((item) => ({
			role: item.role,
			content: item.content,
		}));
		const res = await request.post('/api/ai/chat', {
			message: content,
			username: userInfos.value.userName,
			saveConversation: saveConversation.value,
			messages: chatContext,
		});
		const reply = res?.data?.reply || '我在这里。我们先把事情拆小一点，慢慢来。';
		messages.value.push({ id: Date.now() + 1, role: 'assistant', content: reply });
	} catch (error) {
		ElMessage.error('安小宁暂时没有连上，但你可以先做三次慢慢呼吸。');
	} finally {
		loading.value = false;
		scrollToBottom();
	}
};

const startDrag = (event: PointerEvent) => {
	dragState.dragging = true;
	dragState.moved = false;
	dragState.startX = event.clientX;
	dragState.startY = event.clientY;
	dragState.offsetX = event.clientX - position.x;
	dragState.offsetY = event.clientY - position.y;
	(event.currentTarget as HTMLElement).setPointerCapture?.(event.pointerId);
	window.addEventListener('pointermove', onDrag);
	window.addEventListener('pointerup', stopDrag);
};

const onDrag = (event: PointerEvent) => {
	if (!dragState.dragging) return;
	const deltaX = Math.abs(event.clientX - dragState.startX);
	const deltaY = Math.abs(event.clientY - dragState.startY);
	if (deltaX + deltaY > 6) dragState.moved = true;
	position.x = clamp(event.clientX - dragState.offsetX, EDGE_PADDING, viewport.width - FLOAT_SIZE - EDGE_PADDING);
	position.y = clamp(event.clientY - dragState.offsetY, EDGE_PADDING, viewport.height - FLOAT_SIZE - EDGE_PADDING);
};

const stopDrag = () => {
	if (!dragState.dragging) return;
	dragState.dragging = false;
	savePosition();
	window.removeEventListener('pointermove', onDrag);
	window.removeEventListener('pointerup', stopDrag);
};

const clamp = (value: number, min: number, max: number) => Math.min(Math.max(value, min), max);

const updateViewport = () => {
	viewport.width = window.innerWidth;
	viewport.height = window.innerHeight;
	position.x = clamp(position.x, EDGE_PADDING, viewport.width - FLOAT_SIZE - EDGE_PADDING);
	position.y = clamp(position.y, EDGE_PADDING, viewport.height - FLOAT_SIZE - EDGE_PADDING);
};

const savePosition = () => {
	localStorage.setItem(STORAGE_KEY, JSON.stringify({ x: position.x, y: position.y }));
};

const restorePosition = () => {
	viewport.width = window.innerWidth;
	viewport.height = window.innerHeight;
	const saved = localStorage.getItem(STORAGE_KEY);
	if (saved) {
		try {
			const parsed = JSON.parse(saved);
			position.x = Number(parsed.x);
			position.y = Number(parsed.y);
			updateViewport();
			return;
		} catch {
			localStorage.removeItem(STORAGE_KEY);
		}
	}
	position.x = viewport.width - FLOAT_SIZE - 24;
	position.y = viewport.height - FLOAT_SIZE - 28;
};

onMounted(() => {
	restorePosition();
	window.addEventListener('resize', updateViewport);
});

onBeforeUnmount(() => {
	window.removeEventListener('resize', updateViewport);
	window.removeEventListener('pointermove', onDrag);
	window.removeEventListener('pointerup', stopDrag);
});
</script>

<style scoped>
.anxiaoning-float {
	position: fixed;
	z-index: 3000;
	width: 64px;
	height: 64px;
}

.anxiaoning-float.dragging {
	user-select: none;
}

.float-core {
	position: relative;
	width: 64px;
	height: 64px;
	border: none;
	border-radius: 50%;
	background: linear-gradient(145deg, #7fc9a5, #f3c77d);
	box-shadow: 0 14px 30px rgba(112, 132, 94, 0.28);
	color: #ffffff;
	cursor: grab;
	touch-action: none;
}

.float-core:active {
	cursor: grabbing;
}

.breath-ring {
	position: absolute;
	inset: -8px;
	border-radius: 50%;
	border: 1px solid rgba(127, 201, 165, 0.42);
	animation: breathe 2.8s ease-in-out infinite;
}

.face {
	position: relative;
	display: grid;
	place-items: center;
	width: 100%;
	height: 100%;
	font-size: 24px;
	font-weight: 800;
}

.chat-panel {
	position: absolute;
	width: min(360px, calc(100vw - 32px));
	border: 1px solid rgba(83, 132, 124, 0.16);
	border-radius: 8px;
	background: rgba(255, 255, 255, 0.97);
	box-shadow: 0 20px 50px rgba(45, 74, 70, 0.16);
	overflow: hidden;
	-webkit-user-select: text;
	user-select: text;
}

.panel-above {
	bottom: 78px;
}

.panel-below {
	top: 78px;
}

.panel-left {
	right: 0;
}

.panel-right {
	left: 0;
}

.chat-panel header {
	display: flex;
	justify-content: space-between;
	align-items: flex-start;
	padding: 16px;
	background: #f4fbf8;
}

.chat-panel header strong {
	color: #24433f;
	font-size: 16px;
}

.chat-panel header p {
	margin: 4px 0 0;
	color: #66807b;
}

.chat-panel header button {
	border: none;
	background: transparent;
	color: #66807b;
	font-size: 24px;
	cursor: pointer;
}

.privacy-note {
	padding: 10px 16px;
	background: #fffaf0;
	color: #7d6a46;
	font-size: 12px;
	line-height: 1.6;
}

.messages {
	max-height: 280px;
	overflow-y: auto;
	padding: 16px;
	-webkit-user-select: text;
	user-select: text;
}

.message {
	position: relative;
	width: fit-content;
	max-width: 86%;
	margin-bottom: 10px;
	padding: 10px 48px 10px 12px;
	border-radius: 8px;
	line-height: 1.7;
	white-space: pre-wrap;
	word-break: break-word;
	-webkit-user-select: text;
	user-select: text;
}

.message-content {
	-webkit-user-select: text;
	user-select: text;
}

.copy-message {
	position: absolute;
	right: 8px;
	top: 8px;
	border: none;
	background: transparent;
	color: #6a8f84;
	font-size: 12px;
	cursor: pointer;
	-webkit-user-select: none;
	user-select: none;
}

.message.assistant {
	background: #f3faf6;
	color: #385b55;
}

.message.user {
	margin-left: auto;
	background: #e8f2fb;
	color: #31506a;
}

.suggestions {
	display: grid;
	gap: 8px;
	padding: 0 16px 14px;
}

.suggestions button {
	border: 1px solid rgba(92, 174, 138, 0.22);
	border-radius: 8px;
	background: #ffffff;
	color: #4f8f76;
	padding: 8px 10px;
	text-align: left;
	cursor: pointer;
}

.save-row {
	display: flex;
	align-items: flex-start;
	gap: 8px;
	padding: 0 16px 12px;
	color: #667775;
	font-size: 12px;
	line-height: 1.5;
}

.save-row input {
	margin-top: 2px;
}

.input-row {
	display: grid;
	grid-template-columns: 1fr 64px;
	gap: 10px;
	padding: 14px 16px 16px;
	border-top: 1px solid rgba(83, 132, 124, 0.12);
}

.input-row textarea {
	min-height: 58px;
	resize: none;
	border: 1px solid rgba(92, 174, 138, 0.2);
	border-radius: 8px;
	padding: 9px 10px;
	color: #314f4b;
	outline: none;
	-webkit-user-select: text;
	user-select: text;
}

.input-row textarea:focus {
	border-color: #5cae8a;
}

.input-row button {
	border: none;
	border-radius: 8px;
	background: #5cae8a;
	color: #ffffff;
	font-weight: 700;
	cursor: pointer;
}

.input-row button:disabled {
	background: #b8ccc5;
	cursor: not-allowed;
}

@keyframes breathe {
	0%,
	100% {
		transform: scale(0.94);
		opacity: 0.42;
	}

	50% {
		transform: scale(1.08);
		opacity: 0.86;
	}
}
</style>
