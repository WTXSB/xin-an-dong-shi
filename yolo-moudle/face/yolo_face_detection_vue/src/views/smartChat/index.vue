<template>
	<div class="chat-container">
		<header class="chat-header">
			<p>安心对话</p>
			<h1>把心里的声音慢慢说出来</h1>
			<span>安小宁会用陪伴式语言回应你，不给你贴标签，也不会用生硬语气定义你。</span>
		</header>

		<section class="privacy-note">
			本页面只发送你主动输入的文字。图片、摄像头画面、识别记录不会自动进入对话；DeepSeek API Key 只保存在后端环境变量中。
		</section>

		<section class="chat-messages" ref="messageContainer">
			<div v-for="message in messages" :key="message.id" :class="['message', message.role]">
				<div class="avatar">{{ message.role === 'user' ? '我' : '安' }}</div>
				<div class="text">
					<span class="message-content">{{ message.content }}</span>
					<button class="copy-message" type="button" title="复制这条消息" aria-label="复制这条消息" @click="copyMessage(message.content)">
						复制
					</button>
				</div>
			</div>

			<div v-if="loading" class="message assistant">
				<div class="avatar">安</div>
				<div class="text">我在认真听你说，稍等我一下。</div>
			</div>
		</section>

		<section class="suggested-questions" v-if="messages.length === 1">
			<button v-for="question in suggestedQuestions" :key="question" type="button" @click="selectQuestion(question)">
				{{ question }}
			</button>
		</section>

		<footer class="chat-input">
			<label class="save-row">
				<input v-model="saveConversation" type="checkbox" />
				<span>保存这次对话，方便之后回看自己的照顾过程</span>
			</label>
			<div class="input-line">
				<el-input
					v-model="userInput"
					type="textarea"
					:rows="3"
					placeholder="写下一点点就好，比如：我今天有点紧绷"
					@keyup.enter.ctrl="sendMessage"
				/>
				<el-button class="send-btn" :loading="loading" :disabled="!userInput.trim()" @click="sendMessage">发送</el-button>
			</div>
		</footer>
	</div>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue';
import { ElMessage } from 'element-plus';
import request from '/@/utils/request';
import { useUserInfo } from '/@/stores/userInfo';
import { storeToRefs } from 'pinia';

type ChatMessage = {
	id: number;
	role: 'assistant' | 'user';
	content: string;
};

const stores = useUserInfo();
const { userInfos } = storeToRefs(stores);
const messageContainer = ref<HTMLElement>();
const userInput = ref('');
const loading = ref(false);
const saveConversation = ref(true);
const messages = ref<ChatMessage[]>([
	{
		id: Date.now(),
		role: 'assistant',
		content: '你好，我是安小宁。你可以把此刻的感受说给我听，我们先不急着解决，先一起看见它。',
	},
]);

const suggestedQuestions = ['我最近学习动力不足', '我一想到任务就紧绷', '带我做一次三分钟放松'];

const selectQuestion = (question: string) => {
	userInput.value = question;
	sendMessage();
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
	if (messageContainer.value) messageContainer.value.scrollTop = messageContainer.value.scrollHeight;
};

const sendMessage = async () => {
	const content = userInput.value.trim();
	if (!content || loading.value) return;

	messages.value.push({ id: Date.now(), role: 'user', content });
	userInput.value = '';
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
		messages.value.push({
			id: Date.now() + 1,
			role: 'assistant',
			content: res?.data?.reply || '我在这里。我们先慢慢呼吸一次，再把事情拆小一点。',
		});
	} catch (error) {
		ElMessage.error('安小宁暂时没有连上，但你可以先慢慢呼吸三次。');
	} finally {
		loading.value = false;
		scrollToBottom();
	}
};
</script>

<style scoped>
.chat-container {
	min-height: calc(100vh - 84px);
	display: flex;
	flex-direction: column;
	background: #fbf6ef;
	color: #3f3b35;
	-webkit-user-select: text;
	user-select: text;
}

.chat-header {
	padding: 30px 34px 22px;
	background: linear-gradient(135deg, #fff8ef, #eef7ee);
	border-bottom: 1px solid rgba(174, 133, 91, 0.14);
}

.chat-header p {
	margin: 0 0 8px;
	color: #b47b4f;
	font-weight: 900;
}

.chat-header h1 {
	margin: 0 0 10px;
	color: #3f332b;
	font-size: 28px;
	letter-spacing: 0;
}

.chat-header span,
.privacy-note {
	color: #75695e;
	line-height: 1.7;
}

.privacy-note {
	margin: 18px 34px 0;
	padding: 12px 14px;
	border-radius: 8px;
	background: #fffaf4;
	color: #7a5a43;
	border: 1px solid rgba(201, 143, 92, 0.16);
}

.chat-messages {
	flex: 1;
	overflow-y: auto;
	padding: 24px 34px;
	-webkit-user-select: text;
	user-select: text;
}

.message {
	display: flex;
	gap: 12px;
	margin-bottom: 18px;
	align-items: flex-start;
}

.message.user {
	flex-direction: row-reverse;
}

.avatar {
	width: 38px;
	height: 38px;
	border-radius: 50%;
	display: grid;
	place-items: center;
	flex: 0 0 38px;
	background: #8fcfa7;
	color: #3f332b;
	font-weight: 900;
}

.message.user .avatar {
	background: #f3c77d;
}

.text {
	position: relative;
	max-width: min(720px, 78%);
	padding: 12px 58px 12px 14px;
	border-radius: 8px;
	background: #ffffff;
	color: #67594e;
	line-height: 1.8;
	box-shadow: 0 8px 24px rgba(91, 70, 45, 0.08);
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
	right: 12px;
	top: 12px;
	border: none;
	background: transparent;
	color: #9b7657;
	font-size: 12px;
	cursor: pointer;
	-webkit-user-select: none;
	user-select: none;
}

:deep(.el-textarea__inner) {
	-webkit-user-select: text;
	user-select: text;
}

.message.user .text {
	background: #fff0d8;
	color: #604838;
}

.suggested-questions {
	display: flex;
	flex-wrap: wrap;
	gap: 10px;
	padding: 0 34px 18px;
}

.suggested-questions button {
	border: 1px solid rgba(201, 143, 92, 0.28);
	border-radius: 8px;
	background: #ffffff;
	color: #7a5a43;
	padding: 10px 12px;
	cursor: pointer;
}

.chat-input {
	display: grid;
	gap: 10px;
	padding: 16px 34px 24px;
	background: rgba(255, 255, 255, 0.9);
	border-top: 1px solid rgba(174, 133, 91, 0.14);
}

.save-row {
	display: flex;
	align-items: center;
	gap: 8px;
	color: #75695e;
	font-size: 13px;
}

.input-line {
	display: grid;
	grid-template-columns: 1fr 92px;
	gap: 12px;
}

.send-btn {
	height: 74px;
	border-radius: 8px;
	border: none;
	background: #c98f5c;
	color: #ffffff;
	font-weight: 800;
}

.send-btn:hover {
	background: #b77e4d;
	color: #ffffff;
}

@media (max-width: 720px) {
	.chat-header,
	.chat-messages,
	.chat-input {
		padding-left: 18px;
		padding-right: 18px;
	}

	.privacy-note {
		margin-left: 18px;
		margin-right: 18px;
	}

	.input-line {
		grid-template-columns: 1fr;
	}

	.send-btn {
		height: 42px;
	}
}
</style>
