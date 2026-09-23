<template>
	<div class="gentle-video-page layout-padding">
		<div class="page-shell layout-padding-auto layout-padding-view">
			<section class="hero">
				<div>
					<p class="eyebrow">视频温柔感知</p>
					<h1>把动态里的情绪线索，慢慢整理出来</h1>
					<span>上传的视频只用于本次感知。这里给你的不是结论，而是一份可以帮助你回看状态的温柔线索。</span>
				</div>
				<div class="hero-note">
					<strong>安小宁会这样陪你</strong>
					<p>先看见画面中的表情与动作变化，再把它们整理成觉察记录、小练习和可选择保留的素材路径。</p>
				</div>
			</section>

			<section class="privacy-panel">
				<div>
					<strong>开始前的隐私确认</strong>
					<p>视频会先上传到本地后端，再交给本地 YOLO 服务处理。你可以决定是否保存这次觉察记录，以及记录里是否保留素材路径。</p>
				</div>
				<div class="privacy-options">
					<el-checkbox v-model="privacyAccepted">我了解本次视频感知的用途</el-checkbox>
					<el-checkbox v-model="keepRecord">保存这次觉察记录</el-checkbox>
					<el-checkbox v-model="keepMedia" :disabled="!keepRecord">在记录中保留视频路径</el-checkbox>
				</div>
			</section>

			<section class="controls">
				<label>
					<span>感知类型</span>
					<el-select v-model="kind" size="large" @change="getData">
						<el-option v-for="item in state.kindItems" :key="item.value" :label="item.label" :value="item.value" />
					</el-select>
				</label>

				<label>
					<span>模型</span>
					<el-select v-model="weight" placeholder="选择模型" size="large">
						<el-option v-for="item in state.weightItems" :key="item.value" :label="item.label" :value="item.value" />
					</el-select>
				</label>

				<label class="slider-field">
					<span>只接收较清晰的线索</span>
					<el-slider v-model="conf" :format-tooltip="formatTooltip" :min="20" :max="90" />
				</label>

				<el-upload
					ref="uploadFile"
					class="upload-control"
					action="http://localhost:9999/files/upload"
					:show-file-list="false"
					:on-success="handleVideoSuccess"
					:before-upload="beforeVideoUpload"
				>
					<el-button class="soft-button">上传视频</el-button>
				</el-upload>

				<el-button class="primary-button" :disabled="!canStart || state.processing" :loading="state.processing" @click="startVideoSense">
					开始温柔感知
				</el-button>
			</section>

			<section class="status-row">
				<div class="status-card">
					<strong>当前视频</strong>
					<p>{{ state.form.inputVideo ? '已放好，可以开始处理' : '还没有上传视频' }}</p>
				</div>
				<div class="status-card">
					<strong>处理进度</strong>
					<el-progress v-if="state.showProgress" :text-inside="true" :stroke-width="18" :percentage="state.percentage" />
					<p v-else>开始后会在这里显示处理与整理进度。</p>
				</div>
				<div class="status-card">
					<strong>温柔提醒</strong>
					<p>如果这段画面暂时没有形成清晰线索，也没关系。它只是提醒我们换个角度照顾自己。</p>
				</div>
			</section>

			<section class="sense-grid">
				<article class="preview-panel">
					<div v-if="!state.videoPath" class="empty-state">
						<strong>等待一段想被理解的视频</strong>
						<p>勾选隐私确认并上传视频后，处理画面会出现在这里。</p>
					</div>
					<img v-else class="video-stream" :src="state.videoPath" alt="视频温柔感知处理画面" />
				</article>

				<article class="feedback-panel">
					<div class="panel-title">
						<span>陪伴式反馈</span>
						<h2>{{ state.resultReady ? '这段动态被轻轻整理好了' : '结果会在这里慢慢出现' }}</h2>
					</div>
					<div class="closed-loop">
						<div>
							<span>我看见的线索</span>
							<p>{{ resultCard.summary }}</p>
						</div>
						<div>
							<span>身体可以被温柔询问</span>
							<p>{{ resultCard.bodySignal }}</p>
						</div>
						<div>
							<span>此刻的小练习</span>
							<p>{{ resultCard.practice }}</p>
						</div>
					</div>
					<div class="save-note">
						<span>{{ keepRecord ? '这次会保存到觉察记录' : '这次不会保存为觉察记录' }}</span>
						<span>{{ keepMedia ? '记录里保留视频路径' : '记录里不保留视频路径' }}</span>
					</div>
				</article>
			</section>
		</div>
	</div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '/@/utils/request';
import { saveAwarenessRecord, savePrivacyConsent } from '/@/api/healing';
import { useUserInfo } from '/@/stores/userInfo';
import { storeToRefs } from 'pinia';
import type { UploadProps } from 'element-plus';
import { SocketService } from '/@/utils/socket';
import { formatDate } from '/@/utils/formatTime';

const stores = useUserInfo();
const { userInfos } = storeToRefs(stores);

const conf = ref(50);
const kind = ref('emotion');
const weight = ref('');
const privacyAccepted = ref(false);
const keepRecord = ref(true);
const keepMedia = ref(false);
const recordSaved = ref(false);

const state = reactive({
	weightItems: [] as Array<{ value: string; label: string }>,
	kindItems: [{ value: 'emotion', label: '情绪与动作感知' }],
	videoPath: '',
	percentage: 0,
	showProgress: false,
	processing: false,
	resultReady: false,
	form: {
		username: '',
		inputVideo: '',
		weight: '',
		conf: 0,
		kind: '',
		startTime: '',
		saveRecord: 'true',
		keepMedia: 'false',
	},
});

const canStart = computed(() => privacyAccepted.value && !!state.form.inputVideo && !!weight.value);
const socketService = new SocketService();

const resultCard = computed(() => {
	if (state.processing) {
		return {
			summary: '视频正在被逐帧整理。你可以先让肩膀落下来，给自己一点等待的空间。',
			bodySignal: '等待时可以留意呼吸、手心和肩颈，看看身体是否正在用力。',
			practice: '做一轮慢呼吸：吸气 4 秒，呼气 6 秒。让注意力先回到脚底。',
		};
	}
	if (state.resultReady) {
		return {
			summary: '这段视频已经完成感知。系统把动态里的表情和动作变化整理成一份参考线索，帮助你回看当时的状态。',
			bodySignal: '可以留意视频对应的时段里，肩颈、胃部、手心或呼吸是否更容易紧绷。',
			practice: '写下这段视频发生前后的一件小事，再问自己：接下来我更需要加速、放慢，还是先休息十分钟？',
		};
	}
	return {
		summary: '上传视频并开始后，这里会出现一份柔和反馈。它不会给你贴标签，只帮助你看见线索。',
		bodySignal: '你可以先观察身体此刻哪里最需要被照顾，例如肩颈、眼睛、胃部或手心。',
		practice: '先把今天最重要的一件事拆成一个很小的动作，让自己轻一点开始。',
	};
});

watch(keepRecord, (value) => {
	if (!value) keepMedia.value = false;
});

socketService.on('message', (data: string) => {
	if (data) ElMessage.success(data);
});

socketService.on('progress', (data: string) => {
	const value = Math.round(Number(data));
	if (Number.isNaN(value)) return;
	state.percentage = value;
	state.showProgress = value < 100;
	if (value >= 100) {
		completeVideoReflection();
	}
});

const formatTooltip = (val: number) => `${val}%`;

const beforeVideoUpload: UploadProps['beforeUpload'] = () => {
	if (!privacyAccepted.value) {
		ElMessage.warning('请先勾选隐私确认，再上传视频。');
		return false;
	}
	return true;
};

const handleVideoSuccess: UploadProps['onSuccess'] = (response) => {
	state.form.inputVideo = response.data;
	state.resultReady = false;
	state.videoPath = '';
	recordSaved.value = false;
	ElMessage.success('视频已放好，可以开始温柔感知。');
};

const getData = () => {
	request
		.get('/api/flask/file_names')
		.then((res) => {
			if (res.code === 0 || res.code === '0') {
				const data = typeof res.data === 'string' ? JSON.parse(res.data) : res.data;
				state.weightItems = (data.weight_items || []).filter((item: any) => item.value.includes(kind.value) || item.value.includes('emotion'));
				if (state.weightItems.length > 0) weight.value = state.weightItems[0].value;
			}
		})
		.catch(() => {
			state.weightItems = [{ value: 'emotion.pt', label: 'emotion.pt' }];
			weight.value = 'emotion.pt';
		});
};

const startVideoSense = async () => {
	if (!privacyAccepted.value) {
		ElMessage.warning('请先确认隐私说明，我们再一起开始。');
		return;
	}
	if (!state.form.inputVideo) {
		ElMessage.warning('先上传一段视频，我们再一起轻轻看看。');
		return;
	}

	await ElMessageBox.confirm(
		'即将把这段视频交给本地 YOLO 服务处理。结果只作为自我觉察线索，是否保存记录和素材路径由你决定。',
		'开始视频温柔感知',
		{
			confirmButtonText: '开始感知',
			cancelButtonText: '再等一下',
			type: 'info',
		}
	);

	state.form.weight = weight.value || 'emotion.pt';
	state.form.conf = conf.value / 100;
	state.form.username = userInfos.value.userName;
	state.form.kind = kind.value;
	state.form.startTime = formatDate(new Date(), 'YYYY-mm-dd HH:MM:SS');
	state.form.saveRecord = keepRecord.value ? 'true' : 'false';
	state.form.keepMedia = keepMedia.value ? 'true' : 'false';
	state.processing = true;
	state.resultReady = false;
	state.percentage = 0;
	state.showProgress = true;
	recordSaved.value = false;

	saveVideoConsent();
	const queryParams = new URLSearchParams(state.form as any).toString();
	state.videoPath = `http://127.0.0.1:5000/predictVideo?${queryParams}`;
	ElMessage.success('正在处理，画面会逐步出现。');
};

const saveVideoConsent = () => {
	savePrivacyConsent({
		username: userInfos.value.userName,
		scene: 'video',
		consentType: 'video-upload',
		consentText: '用户确认视频会上传到后端并交给本地 YOLO 服务处理，结果仅作为觉察线索。',
		agreed: privacyAccepted.value,
		keepRecord: keepRecord.value,
		keepMedia: keepMedia.value,
	}).catch(() => {});
};

const completeVideoReflection = async () => {
	state.processing = false;
	state.resultReady = true;
	state.showProgress = false;
	state.percentage = 100;
	if (!keepRecord.value || recordSaved.value) return;
	recordSaved.value = true;
	await saveAwarenessRecord({
		username: userInfos.value.userName,
		sourceType: 'video',
		emotionLabel: '动态线索',
		confidence: `线索阈值 ${conf.value}%`,
		bodySignal: resultCard.value.bodySignal,
		gentleSummary: resultCard.value.summary,
		suggestedPractice: resultCard.value.practice,
		inputMedia: keepMedia.value ? state.form.inputVideo : '',
		outputMedia: '',
		keepRecord: keepRecord.value,
		keepMedia: keepMedia.value,
		privacyNote: keepMedia.value ? '你选择在记录中保留视频输入路径；结果视频路径由本地处理服务按需保存。' : '你选择不在觉察记录中保留视频路径，只留下温柔摘要。',
	});
};

onMounted(() => {
	getData();
});

onUnmounted(() => {
	socketService.disconnect();
});
</script>

<style scoped lang="scss">
.gentle-video-page {
	min-height: 100%;
	background: #fbf6ef;
	color: #3f3b35;
	overflow: visible;
}

.gentle-video-page.layout-padding {
	position: relative;
	display: block;
	height: auto;
	min-height: 100%;
	overflow: visible;
}

.page-shell {
	padding: 22px;
	min-height: 100%;
}

.page-shell.layout-padding-auto,
.page-shell.layout-padding-view {
	display: block;
	height: auto;
	min-height: 100%;
	overflow: visible;
}

.hero,
.privacy-panel,
.controls,
.status-card,
.preview-panel,
.feedback-panel {
	border: 1px solid rgba(174, 133, 91, 0.14);
	border-radius: 8px;
	background: rgba(255, 255, 255, 0.92);
	box-shadow: 0 14px 32px rgba(91, 70, 45, 0.08);
}

.hero {
	display: grid;
	grid-template-columns: minmax(0, 1.3fr) minmax(280px, 0.7fr);
	gap: 18px;
	padding: 30px;
	background: linear-gradient(135deg, #fff8ef, #eef7ee);

	h1 {
		margin: 8px 0 10px;
		color: #3f332b;
		font-size: 34px;
		line-height: 1.18;
		letter-spacing: 0;
	}

	span,
	p {
		line-height: 1.8;
	}
}

.eyebrow,
.controls span,
.panel-title span {
	margin: 0;
	color: #b47b4f;
	font-size: 13px;
	font-weight: 900;
}

.hero-note {
	padding: 18px;
	border-radius: 8px;
	background: rgba(255, 250, 244, 0.84);

	strong {
		color: #604838;
	}

	p {
		margin: 8px 0 0;
		color: #67594e;
	}
}

.privacy-panel {
	display: grid;
	grid-template-columns: minmax(0, 1fr) 320px;
	gap: 18px;
	align-items: center;
	margin-top: 16px;
	padding: 18px 20px;

	strong {
		color: #604838;
		font-size: 17px;
	}

	p {
		margin: 6px 0 0;
		color: #75695e;
		line-height: 1.8;
	}
}

.privacy-options {
	display: grid;
	gap: 8px;
}

.controls {
	display: grid;
	grid-template-columns: 200px 200px minmax(240px, 1fr) 140px 150px;
	gap: 14px;
	align-items: end;
	margin-top: 16px;
	padding: 16px;
}

.controls label {
	display: grid;
	gap: 8px;
}

.soft-button,
.primary-button {
	width: 100%;
	height: 42px;
	border-radius: 8px;
	font-weight: 800;
}

.soft-button {
	border-color: rgba(201, 143, 92, 0.32);
	color: #7a5a43;
	background: #fffaf4;
}

.primary-button {
	border: none;
	background: #c98f5c;
	color: #ffffff;
}

.status-row,
.sense-grid {
	display: grid;
	gap: 16px;
	margin-top: 16px;
}

.status-row {
	grid-template-columns: repeat(3, minmax(0, 1fr));
}

.status-card {
	padding: 16px;

	strong {
		color: #604838;
	}

	p {
		margin: 8px 0 0;
		color: #75695e;
		line-height: 1.8;
	}
}

.sense-grid {
	grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.8fr);
}

.preview-panel {
	min-height: 430px;
	display: grid;
	place-items: center;
	overflow: hidden;
	background: #fffaf4;
}

.empty-state {
	padding: 28px;
	text-align: center;
	color: #75695e;

	strong {
		color: #604838;
	}

	p {
		line-height: 1.8;
	}
}

.video-stream {
	width: 100%;
	height: 100%;
	max-height: 640px;
	object-fit: contain;
}

.feedback-panel {
	padding: 18px;
}

.panel-title h2 {
	margin: 6px 0 0;
	color: #3f332b;
	font-size: 20px;
	letter-spacing: 0;
}

.closed-loop {
	display: grid;
	gap: 10px;
	margin-top: 14px;

	div {
		padding: 12px;
		border-radius: 8px;
		background: #fffaf4;
	}

	span {
		color: #9a6847;
		font-size: 13px;
		font-weight: 900;
	}

	p {
		margin: 6px 0 0;
		color: #67594e;
		line-height: 1.75;
	}
}

.save-note {
	display: flex;
	flex-wrap: wrap;
	gap: 8px;
	margin-top: 12px;

	span {
		padding: 6px 10px;
		border-radius: 999px;
		background: #f4e6d3;
		color: #75563f;
		font-size: 12px;
	}
}

:deep(.el-slider__bar) {
	background-color: #c98f5c;
}

:deep(.el-slider__button) {
	border-color: #c98f5c;
}

@media (max-width: 1180px) {
	.hero,
	.privacy-panel,
	.controls,
	.status-row,
	.sense-grid {
		grid-template-columns: 1fr;
	}
}

@media (max-width: 720px) {
	.page-shell {
		padding: 14px;
	}

	.hero {
		padding: 22px;

		h1 {
			font-size: 28px;
		}
	}
}
</style>
