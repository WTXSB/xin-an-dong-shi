<template>
	<div
		v-if="shouldShow"
		class="anxiaoning-float"
		:class="{ dragging: dragState.dragging }"
		:style="{ left: `${position.x}px`, top: `${position.y}px` }"
	>
		<button
			class="float-core"
			type="button"
			:aria-label="`打开${selectedPet.name}陪伴对话`"
			:title="`拖动${selectedPet.name}，或点击打开安心对话`"
			@click="togglePanel"
			@pointerdown="startDrag"
			@pointerenter="greetPet"
		>
			<span class="pet-sprite" :style="spriteStyle" aria-hidden="true"></span>
		</button>

		<button
			class="pet-switch"
			type="button"
			aria-label="更换陪伴桌宠"
			title="更换陪伴桌宠"
			@click.stop="togglePetPicker"
			@pointerdown.stop
		>
			换
		</button>

		<section v-if="petPickerOpen" :class="['pet-picker', panelPlacement]" @pointerdown.stop>
			<header class="pet-picker-header">
				<div>
					<strong>选择陪伴伙伴</strong>
					<p>选择会保存在当前浏览器</p>
				</div>
				<button type="button" aria-label="关闭桌宠选择" @click="petPickerOpen = false">×</button>
			</header>

			<div class="pet-options">
				<button
					v-for="pet in PETS"
					:key="pet.id"
					type="button"
					:class="['pet-option', { selected: pet.id === selectedPet.id }]"
					:aria-pressed="pet.id === selectedPet.id"
					@click="selectPet(pet.id)"
				>
					<span class="pet-option-preview" :style="petPreviewStyle(pet)" aria-hidden="true"></span>
					<span>
						<strong>{{ pet.name }}</strong>
						<small>{{ pet.description }}</small>
					</span>
					<i v-if="pet.id === selectedPet.id" aria-hidden="true">✓</i>
				</button>
			</div>
		</section>

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
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
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
const PET_STORAGE_KEY = 'anxiaoning-selected-pet';
const FLOAT_SIZE = 82;
const EDGE_PADDING = 14;
const PET_FRAME_COUNT = 6;
const PET_FRAME_DURATIONS = [260, 130, 130, 180, 130, 130] as const;

const PETS = [
	{ id: 'bird', name: '小啾', description: '轻快活泼的小鸟', idleSprite: '/pets/pet-bird-idle.webp' },
	{ id: 'dog', name: '小安', description: '温暖陪伴的小狗', idleSprite: '/pets/pet-dog-idle.webp' },
	{ id: 'deer', name: '小鹿', description: '安静治愈的小鹿', idleSprite: '/pets/pet-deer-idle.webp' },
] as const;

const stores = useUserInfo();
const { userInfos } = storeToRefs(stores);
const route = useRoute();
const shouldShow = computed(() => route.path !== '/smartChat');
const opened = ref(false);
const petPickerOpen = ref(false);
const selectedPetId = ref<(typeof PETS)[number]['id']>('dog');
const selectedPet = computed(() => PETS.find((pet) => pet.id === selectedPetId.value) || PETS[1]);
const idleFrame = ref(0);
const spriteStyle = computed(() => ({
	backgroundImage: `url(${selectedPet.value.idleSprite})`,
	backgroundPosition: `${idleFrame.value * (100 / (PET_FRAME_COUNT - 1))}% center`,
}));
const petPreviewStyle = (pet: (typeof PETS)[number]) => ({ backgroundImage: `url(${pet.idleSprite})` });
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

let petIdleTimer: number | undefined;
let petFrameTimer: number | undefined;

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
	petPickerOpen.value = false;
	opened.value = !opened.value;
};

const togglePetPicker = () => {
	opened.value = false;
	petPickerOpen.value = !petPickerOpen.value;
};

const selectPet = (petId: (typeof PETS)[number]['id']) => {
	selectedPetId.value = petId;
	localStorage.setItem(PET_STORAGE_KEY, petId);
	petPickerOpen.value = false;
	idleFrame.value = 0;
	scheduleNextPetAnimation(true);
	ElMessage.success(`已切换为${selectedPet.value.name}`);
};

const clearPetAnimationTimers = () => {
	if (petIdleTimer !== undefined) {
		window.clearTimeout(petIdleTimer);
		petIdleTimer = undefined;
	}
	if (petFrameTimer !== undefined) {
		window.clearTimeout(petFrameTimer);
		petFrameTimer = undefined;
	}
};

const hasReducedMotion = () => window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;

const playPetAnimation = () => {
	clearPetAnimationTimers();
	if (hasReducedMotion()) {
		idleFrame.value = 0;
		return;
	}
	let frameIndex = 0;
	idleFrame.value = frameIndex;
	const advance = () => {
		frameIndex += 1;
		if (frameIndex >= PET_FRAME_COUNT) {
			idleFrame.value = 0;
			scheduleNextPetAnimation();
			return;
		}
		idleFrame.value = frameIndex;
		petFrameTimer = window.setTimeout(advance, PET_FRAME_DURATIONS[frameIndex]);
	};
	petFrameTimer = window.setTimeout(advance, PET_FRAME_DURATIONS[0]);
};

const scheduleNextPetAnimation = (soon = false) => {
	clearPetAnimationTimers();
	if (!shouldShow.value || hasReducedMotion()) return;
	const delay = soon ? 1200 + Math.random() * 1000 : 4200 + Math.random() * 3600;
	petIdleTimer = window.setTimeout(playPetAnimation, delay);
};

const greetPet = () => {
	if (dragState.dragging || hasReducedMotion()) return;
	playPetAnimation();
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
	const stayAtRightEdge = position.x >= viewport.width - FLOAT_SIZE - 80;
	const stayAtBottomEdge = position.y >= viewport.height - FLOAT_SIZE - 80;
	viewport.width = window.innerWidth;
	viewport.height = window.innerHeight;
	position.x = stayAtRightEdge
		? viewport.width - FLOAT_SIZE - 24
		: clamp(position.x, EDGE_PADDING, viewport.width - FLOAT_SIZE - EDGE_PADDING);
	position.y = stayAtBottomEdge
		? viewport.height - FLOAT_SIZE - 28
		: clamp(position.y, EDGE_PADDING, viewport.height - FLOAT_SIZE - EDGE_PADDING);
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

const restorePet = () => {
	const savedPet = localStorage.getItem(PET_STORAGE_KEY);
	if (PETS.some((pet) => pet.id === savedPet)) {
		selectedPetId.value = savedPet as (typeof PETS)[number]['id'];
	}
};

onMounted(() => {
	restorePet();
	restorePosition();
	scheduleNextPetAnimation(true);
	window.addEventListener('resize', updateViewport);
});

watch(shouldShow, (visible) => {
	idleFrame.value = 0;
	if (visible) scheduleNextPetAnimation(true);
	else clearPetAnimationTimers();
});

onBeforeUnmount(() => {
	clearPetAnimationTimers();
	window.removeEventListener('resize', updateViewport);
	window.removeEventListener('pointermove', onDrag);
	window.removeEventListener('pointerup', stopDrag);
});
</script>

<style scoped>
.anxiaoning-float {
	position: fixed;
	z-index: 3000;
	width: 82px;
	height: 82px;
}

.anxiaoning-float.dragging {
	user-select: none;
}

.float-core {
	position: relative;
	display: block;
	width: 82px;
	height: 82px;
	padding: 0;
	overflow: hidden;
	border: none;
	border-radius: 0;
	background: transparent;
	box-shadow: none;
	cursor: grab;
	touch-action: none;
}

.float-core:active {
	cursor: grabbing;
}

.pet-sprite {
	display: block;
	width: 76px;
	height: 82px;
	margin: 0 auto;
	background-repeat: no-repeat;
	background-size: 600% 100%;
	pointer-events: none;
	will-change: background-position;
}

.pet-switch {
	position: absolute;
	top: -9px;
	right: -9px;
	z-index: 4;
	display: grid;
	place-items: center;
	width: 30px;
	height: 30px;
	padding: 0;
	border: 2px solid #ffffff;
	border-radius: 50%;
	background: #5d9b82;
	box-shadow: 0 6px 14px rgba(54, 88, 75, 0.25);
	color: #ffffff;
	font-size: 12px;
	font-weight: 800;
	cursor: pointer;
	opacity: 0;
	transform: scale(0.84);
	transition: opacity 0.18s ease, transform 0.18s ease, background 0.18s ease;
}

.anxiaoning-float:hover .pet-switch,
.pet-switch:focus-visible {
	opacity: 1;
	transform: scale(1);
}

.pet-switch:hover {
	background: #4d8b72;
}

.chat-panel,
.pet-picker {
	position: absolute;
	border: 1px solid rgba(83, 132, 124, 0.16);
	border-radius: 16px;
	background: rgba(255, 255, 255, 0.97);
	box-shadow: 0 20px 50px rgba(45, 74, 70, 0.16);
	overflow: hidden;
	-webkit-user-select: text;
	user-select: text;
}

.chat-panel {
	width: min(360px, calc(100vw - 32px));
}

.pet-picker {
	width: min(292px, calc(100vw - 32px));
}

.panel-above {
	bottom: 98px;
}

.panel-below {
	top: 98px;
}

.panel-left {
	right: 0;
}

.panel-right {
	left: 0;
}

.pet-picker-header {
	display: flex;
	justify-content: space-between;
	align-items: flex-start;
	padding: 15px 16px 12px;
	background: linear-gradient(135deg, #f2faf6, #fffaf0);
}

.pet-picker-header strong {
	color: #294d44;
	font-size: 15px;
}

.pet-picker-header p {
	margin: 3px 0 0;
	color: #758780;
	font-size: 12px;
}

.pet-picker-header button {
	border: none;
	background: transparent;
	color: #66807b;
	font-size: 22px;
	cursor: pointer;
}

.pet-options {
	display: grid;
	gap: 8px;
	padding: 12px;
}

.pet-option {
	display: grid;
	grid-template-columns: 52px 1fr 24px;
	gap: 10px;
	align-items: center;
	width: 100%;
	padding: 8px;
	border: 1px solid rgba(83, 132, 124, 0.14);
	border-radius: 12px;
	background: #ffffff;
	color: #38564f;
	text-align: left;
	cursor: pointer;
	transition: border-color 0.2s ease, background 0.2s ease;
}

.pet-option:hover,
.pet-option.selected {
	border-color: rgba(93, 155, 130, 0.48);
	background: #f3faf6;
}

.pet-option-preview {
	display: block !important;
	width: 52px;
	height: 52px;
	background-repeat: no-repeat;
	background-position: 0 center;
	background-size: 600% 100%;
	pointer-events: none;
}

.pet-option span {
	display: grid;
	gap: 3px;
}

.pet-option strong {
	font-size: 14px;
}

.pet-option small {
	color: #7d8d87;
	font-size: 12px;
}

.pet-option i {
	display: grid;
	place-items: center;
	width: 22px;
	height: 22px;
	border-radius: 50%;
	background: #5d9b82;
	color: #ffffff;
	font-style: normal;
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

@media (hover: none) {
	.pet-switch {
		opacity: 1;
		transform: scale(1);
	}
}
</style>
