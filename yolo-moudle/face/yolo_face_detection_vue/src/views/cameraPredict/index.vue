<template>
	<div class="gentle-camera-page layout-padding">
		<div class="page-shell layout-padding-auto layout-padding-view">
			<section class="hero">
				<div>
					<p class="eyebrow">摄像头温柔感知</p>
					<h1>实时看见身体与情绪的细微信号</h1>
					<span>摄像头只会在你主动确认后开启，也可以随时停止。结果用于自我觉察，不会给你贴标签。</span>
				</div>
				<div class="hero-note">
					<strong>你始终拥有选择权</strong>
					<p>是否开始、是否保存记录、是否保留结果视频路径，都由你决定。安小宁只负责陪你把线索说得更温柔。</p>
				</div>
			</section>

			<section class="privacy-panel">
				<div>
					<strong>开始前的隐私确认</strong>
					<p>摄像头画面会交给本地 YOLO 服务实时处理。画面不会自动发送给安小宁或 DeepSeek；本次是否保存记录和结果视频路径由你单独选择。</p>
				</div>
				<div class="privacy-options">
					<el-checkbox v-model="cameraAccepted">我了解摄像头只会在确认后开启</el-checkbox>
					<el-checkbox v-model="keepRecord">保存这次觉察记录</el-checkbox>
					<el-checkbox v-model="keepMedia" :disabled="!keepRecord">在记录中保留结果视频路径</el-checkbox>
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
					<span>模型方案</span>
					<el-select v-model="weight" placeholder="选择模型方案" size="large">
						<el-option v-for="item in state.weightItems" :key="item.value" :label="item.label" :value="item.value" />
					</el-select>
				</label>

				<label class="slider-field">
					<span>只接收较清晰的线索</span>
					<el-slider v-model="conf" :format-tooltip="formatTooltip" :min="20" :max="90" />
				</label>

				<el-button class="primary-button" :disabled="!canStart || state.cameraIsOpen" @click="startCameraSense">开始温柔感知</el-button>
				<el-button class="soft-button" :disabled="!state.cameraIsOpen" @click="stopCameraSense">停止并整理</el-button>
			</section>

			<section class="status-row">
				<div class="status-card">
					<strong>当前状态</strong>
					<p>{{ state.cameraIsOpen ? '正在温柔感知中，你可以随时停止。' : '摄像头尚未开启。' }}</p>
				</div>
				<div class="status-card">
					<strong>整理进度</strong>
					<el-progress v-if="state.showProgress" :text-inside="true" :stroke-width="18" :percentage="state.percentage" />
					<p v-else>停止后会显示视频整理进度。</p>
				</div>
				<div class="status-card">
					<strong>表达原则</strong>
					<p>咬指甲、搓手、低头等动作会被表达为“身心信号”，只是提醒我们多照顾自己一点。</p>
				</div>
			</section>

			<section class="sense-grid">
				<article class="preview-panel">
					<div v-if="!state.cameraIsOpen" class="empty-state">
						<strong>等你准备好再开始</strong>
						<p>勾选隐私确认后，点击开始。你始终可以暂停、停止和离开。</p>
					</div>
					<img v-else class="video-stream" :src="state.videoPath" alt="摄像头温柔感知画面" />
				</article>

				<article class="feedback-panel">
					<div class="panel-title">
						<span>陪伴式反馈</span>
						<h2>{{ state.resultReady ? '这段实时感知已被整理' : '停止后会生成一份温柔整理' }}</h2>
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
					<div v-if="state.resultReady" class="analysis-summary">
						<div class="summary-metrics">
							<div><strong>{{ bfrbSummary.eventCount }}</strong><span>BFRB事件</span></div>
							<div><strong>{{ bfrbSummary.totalDurationSeconds }}s</strong><span>累计持续</span></div>
							<div><strong>{{ state.analysisResult?.emotionSummary?.length || 0 }}</strong><span>情绪线索类型</span></div>
						</div>
						<div v-if="bfrbSummary.events.length" class="event-list">
							<div v-for="event in bfrbSummary.events.slice(0, 5)" :key="event.id" class="event-item">
								<div>
									<strong>{{ event.cueType }}</strong>
									<span>{{ event.startSeconds }}s–{{ event.endSeconds }}s · 持续 {{ event.durationSeconds }}s</span>
								</div>
								<el-tag type="warning" effect="plain">最高 {{ confidencePercent(event.maxConfidence) }}%</el-tag>
								<small>{{ evidenceLabel(event.evidenceType) }} · 关键帧 {{ event.keyFrameSeconds }}s</small>
							</div>
						</div>
						<p v-else class="no-event">本次没有形成满足连续性条件的BFRB事件，零散单帧命中不会计入次数。</p>
					</div>
					<div class="save-note">
						<span>{{ keepRecord ? '这次会保存到觉察记录' : '这次不会保存为觉察记录' }}</span>
						<span>{{ keepMedia ? '记录里保留结果视频路径' : '记录里不保留结果视频路径' }}</span>
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
import { SocketService } from '/@/utils/socket';
import { formatDate } from '/@/utils/formatTime';
import { analysisModeItems, confidencePercent, evidenceLabel, getAnalysisModelOptions, type AnalysisResult, type BfrbCue } from '/@/utils/analysisModes';

const stores = useUserInfo();
const { userInfos } = storeToRefs(stores);

const conf = ref(30);
const kind = ref('combined');
const weight = ref('combined');
const cameraAccepted = ref(false);
const keepRecord = ref(true);
const keepMedia = ref(false);
const recordSaved = ref(false);

const state = reactive({
	weightItems: getAnalysisModelOptions('combined'),
	kindItems: analysisModeItems,
	videoPath: '',
	percentage: 0,
	showProgress: false,
	cameraIsOpen: false,
	resultReady: false,
	analysisResult: null as AnalysisResult | null,
	liveCues: [] as BfrbCue[],
	form: {
		username: '',
		weight: '',
		conf: 0,
		kind: '',
		startTime: '',
		saveRecord: 'true',
		keepMedia: 'false',
	},
});

const canStart = computed(() => cameraAccepted.value && !!weight.value);
const socketService = new SocketService();
const bfrbSummary = computed(() => state.analysisResult?.bfrbSummary || {
	eventCount: 0,
	totalDurationSeconds: 0,
	events: [],
	byBehavior: [],
});

const resultCard = computed(() => {
	if (state.cameraIsOpen) {
		return {
			summary: '实时画面正在被温柔感知。你可以把注意力放回身体，知道自己随时可以停止。',
			bodySignal: '可以留意手心、肩颈、下颌和呼吸，看看哪里正在用力。',
			practice: '让双脚踩稳地面，呼气时把肩膀放低一点。只需要一点点就很好。',
		};
	}
	if (state.resultReady) {
		const eventCount = bfrbSummary.value.eventCount;
		const leadingEvent = bfrbSummary.value.byBehavior[0];
		return {
			summary: eventCount
				? `实时感知形成了 ${eventCount} 个BFRB行为事件${leadingEvent ? `，主要线索是“${leadingEvent.cueType}”` : ''}。次数已经按连续事件合并。`
				: '实时感知已经整理完成，没有形成满足连续性条件的BFRB事件；零散命中没有被当作行为次数。',
			bodySignal: eventCount ? '可以结合事件时间和持续时长，回想当时手心、下颌、肩颈或呼吸是否更紧绷。' : '可以回想刚才身体是否整体较平稳，也允许模型暂时没有看清。',
			practice: '给自己 60 秒：吸气时默念“我在这里”，呼气时默念“我可以慢一点”。然后再决定下一步。',
		};
	}
	return {
		summary: '开始后，这里会出现一份陪伴式反馈。它会尽量把动作线索说得轻一点、可照顾一点。',
		bodySignal: '你可以先观察呼吸、肩颈和手心，看看身体此刻最想被怎样对待。',
		practice: '先坐稳，慢慢呼气。等准备好了，再开始摄像头感知。',
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
		completeCameraReflection();
	}
});

socketService.on('bfrb_live', (data: any) => {
	if (data?.scene === 'camera') state.liveCues = data.cues || [];
});

socketService.on('analysis_result', (data: AnalysisResult) => {
	if (data?.scene !== 'camera') return;
	state.analysisResult = data;
	completeCameraReflection();
});

const formatTooltip = (val: number) => `${val}%`;

const getData = () => {
	state.weightItems = getAnalysisModelOptions(kind.value);
	weight.value = state.weightItems[0].value;
};

const startCameraSense = async () => {
	if (!cameraAccepted.value) {
		ElMessage.warning('请先勾选摄像头隐私确认。');
		return;
	}

	await ElMessageBox.confirm(
		'即将开启摄像头实时感知。画面会交给本地 YOLO 服务处理；结果只作为自我觉察线索，是否保存记录和素材路径由你决定。',
		'开启摄像头温柔感知',
		{
			confirmButtonText: '我准备好了',
			cancelButtonText: '再等一下',
			type: 'info',
		}
	);

	state.form.weight = weight.value || 'combined';
	state.form.kind = kind.value;
	state.form.conf = conf.value / 100;
	state.form.username = userInfos.value.userName;
	state.form.startTime = formatDate(new Date(), 'YYYY-mm-dd HH:MM:SS');
	state.form.saveRecord = keepRecord.value ? 'true' : 'false';
	state.form.keepMedia = keepMedia.value ? 'true' : 'false';
	state.resultReady = false;
	state.cameraIsOpen = true;
	state.percentage = 0;
	state.showProgress = false;
	recordSaved.value = false;
	state.analysisResult = null;
	state.liveCues = [];

	saveCameraConsent();
	const queryParams = new URLSearchParams(state.form as any).toString();
	state.videoPath = `http://127.0.0.1:5000/predictCamera?${queryParams}`;
	ElMessage.success('摄像头感知已开始，你可以随时停止。');
};

const stopCameraSense = () => {
	request.get('/api/flask/stopCamera').finally(() => {
		state.cameraIsOpen = false;
		state.showProgress = true;
		ElMessage.success('摄像头感知已停止，正在整理这次线索。');
	});
};

const saveCameraConsent = () => {
	savePrivacyConsent({
		username: userInfos.value.userName,
		scene: 'camera',
		consentType: 'camera-recognition',
		consentText: '用户确认摄像头只在主动开始后开启，画面交给本地 YOLO 服务实时处理。',
		agreed: cameraAccepted.value,
		keepRecord: keepRecord.value,
		keepMedia: keepMedia.value,
	}).catch(() => {});
};

const completeCameraReflection = async () => {
	state.resultReady = true;
	state.showProgress = false;
	state.percentage = 100;
	if (!keepRecord.value || recordSaved.value) return;
	recordSaved.value = true;
	await saveAwarenessRecord({
		username: userInfos.value.userName,
		sourceType: 'camera',
		emotionLabel: bfrbSummary.value.eventCount ? `BFRB事件 ${bfrbSummary.value.eventCount} 次` : '实时综合线索',
		confidence: bfrbSummary.value.events.length ? `最高置信度 ${confidencePercent(Math.max(...bfrbSummary.value.events.map((event) => event.maxConfidence)))}%` : `线索阈值 ${conf.value}%`,
		bodySignal: resultCard.value.bodySignal,
		gentleSummary: resultCard.value.summary,
		suggestedPractice: resultCard.value.practice,
		inputMedia: '',
		outputMedia: '',
		keepRecord: keepRecord.value,
		keepMedia: keepMedia.value,
		privacyNote: keepMedia.value ? '你选择保留结果视频路径；路径由本地处理服务按需写入原始记录。' : '你选择不在觉察记录中保留结果视频路径，只留下温柔摘要。',
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
.gentle-camera-page {
	min-height: 100%;
	background: #fbf6ef;
	color: #3f3b35;
	overflow: visible;
}

.gentle-camera-page.layout-padding {
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
	grid-template-columns: minmax(0, 1fr) 340px;
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
	grid-template-columns: 200px 200px minmax(240px, 1fr) 150px 150px;
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

.analysis-summary {
	margin-top: 14px;
	padding: 14px;
	border: 1px solid rgba(92, 151, 111, 0.24);
	border-radius: 8px;
	background: #f7fbf6;
}

.summary-metrics {
	display: grid;
	grid-template-columns: repeat(3, minmax(0, 1fr));
	gap: 8px;

	div {
		display: grid;
		gap: 4px;
		padding: 10px;
		border-radius: 8px;
		background: #ffffff;
	}

	strong {
		color: #477255;
		font-size: 20px;
	}

	span {
		color: #718078;
		font-size: 12px;
	}
}

.event-list {
	display: grid;
	gap: 8px;
	margin-top: 12px;
}

.event-item {
	display: grid;
	grid-template-columns: minmax(0, 1fr) auto;
	gap: 6px 10px;
	padding: 10px;
	border-radius: 8px;
	background: #ffffff;

	div {
		display: grid;
		gap: 4px;
	}

	span,
	small {
		color: #75695e;
		font-size: 12px;
	}

	small {
		grid-column: 1 / -1;
	}
}

.no-event {
	margin: 12px 0 0;
	color: #66766d;
	line-height: 1.7;
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
