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
					<p>开始时浏览器会单独询问摄像头权限，只采集画面、不采集声音。画面逐帧交给本地 YOLO 服务分析，不会发送给安小宁或 DeepSeek。</p>
					<p>未选择“保留结果视频”时，服务端不会创建视频文件；停止、撤回授权或离开页面都会立即关闭浏览器摄像头。</p>
				</div>
				<div class="privacy-options">
					<el-checkbox v-model="cameraAccepted" :disabled="state.cameraIsOpen || state.isStarting">我已阅读并同意本次摄像头分析</el-checkbox>
					<el-checkbox v-model="keepRecord" :disabled="state.cameraIsOpen || state.isStarting">保存结构化觉察记录</el-checkbox>
					<el-checkbox v-model="keepMedia" :disabled="!keepRecord || state.cameraIsOpen || state.isStarting">保留本次处理后的视频</el-checkbox>
					<div class="permission-state" :class="`is-${state.permissionState}`">
						<span class="permission-dot"></span>
						{{ permissionLabel }}
					</div>
				</div>
			</section>

			<PreVisitNotes
				v-model:complaint="state.form.complaint"
				v-model:additional-notes="state.form.additionalNotes"
				:disabled="state.cameraIsOpen || state.isStarting || state.isStopping"
			/>

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

				<el-button class="primary-button" :disabled="!canStart || state.cameraIsOpen || state.isStarting" @click="startCameraSense">
					{{ state.isStarting ? '正在请求授权…' : '开始温柔感知' }}
				</el-button>
				<el-button class="soft-button" :disabled="!state.cameraIsOpen || state.isStopping" @click="stopCameraSense">
					{{ state.isStopping ? '正在停止并整理…' : '停止并撤回本次授权' }}
				</el-button>
			</section>

			<section class="status-row">
				<div class="status-card">
					<strong>当前状态</strong>
					<p>{{ state.isStopping ? '采集已停止，正在整理并保存本次线索…' : state.isStarting ? '正在等待浏览器授权并建立安全会话…' : state.cameraIsOpen ? '正在温柔感知中，你可以随时停止。' : '摄像头尚未开启。' }}</p>
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
						<p>勾选隐私确认并点击开始后，浏览器会显示系统级摄像头权限询问。</p>
					</div>
					<video ref="cameraVideo" v-show="state.cameraIsOpen && !state.processedFrame" class="video-stream" autoplay muted playsinline></video>
					<img v-show="state.cameraIsOpen && state.processedFrame" class="video-stream" :src="state.processedFrame" alt="摄像头温柔感知画面" />
					<canvas ref="captureCanvas" class="capture-canvas" aria-hidden="true"></canvas>
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
					<div v-if="savedAwarenessRecordId" class="report-entry">
						<el-button type="primary" @click="openPreVisitReport">生成预诊报告</el-button>
						<span>报告基于停止后保存的情绪与BFRB结构化结果。</span>
					</div>
				</article>
			</section>
		</div>
	</div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '/@/utils/request';
import { linkAnalysisRecord, saveAwarenessRecord, savePrivacyConsent } from '/@/api/healing';
import { useUserInfo } from '/@/stores/userInfo';
import { storeToRefs } from 'pinia';
import { formatDate } from '/@/utils/formatTime';
import { analysisModeItems, confidencePercent, createAnalysisSessionId, evidenceLabel, getAnalysisModelOptions, type AnalysisResult, type BfrbCue } from '/@/utils/analysisModes';
import PreVisitNotes from '/@/components/preVisitNotes/index.vue';

const stores = useUserInfo();
const { userInfos } = storeToRefs(stores);
const router = useRouter();

const conf = ref(30);
const kind = ref('combined');
const weight = ref('combined');
const cameraAccepted = ref(false);
const keepRecord = ref(true);
const keepMedia = ref(false);
const recordSaved = ref(false);
const savedAwarenessRecordId = ref(0);
const cameraVideo = ref<HTMLVideoElement | null>(null);
const captureCanvas = ref<HTMLCanvasElement | null>(null);
let mediaStream: MediaStream | null = null;
let captureTimer: number | undefined;
let captureActive = false;
let frameInFlight = false;
let pageDisposed = false;

const state = reactive({
	weightItems: getAnalysisModelOptions('combined'),
	kindItems: analysisModeItems,
	processedFrame: '',
	permissionState: 'idle' as 'idle' | 'requesting' | 'granted' | 'denied' | 'unsupported',
	percentage: 0,
	showProgress: false,
	cameraIsOpen: false,
	isStarting: false,
	isStopping: false,
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
		sessionId: '',
		complaint: '',
		additionalNotes: '',
		consentId: '',
	},
});

const canStart = computed(() => cameraAccepted.value && !!weight.value);
const permissionLabel = computed(() => ({
	idle: '摄像头权限尚未请求',
	requesting: '正在等待浏览器授权',
	granted: '本次摄像头权限已获得，可随时停止',
	denied: '摄像头权限未获得',
	unsupported: '当前浏览器不支持摄像头授权',
}[state.permissionState]));
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

	try {
		await ElMessageBox.confirm(
			`即将由浏览器请求摄像头权限，只采集画面、不采集声音。画面会逐帧交给本地 YOLO 服务分析；${
				keepRecord.value ? '本次会保存结构化觉察记录' : '本次不会保存觉察记录'
			}，${keepMedia.value ? '会保留处理后的视频' : '不会创建或保留视频文件'}。你可以随时停止。`,
			'本次摄像头授权确认',
			{
				confirmButtonText: '继续并由浏览器询问',
				cancelButtonText: '取消',
				type: 'info',
			}
		);
	} catch (_) {
		return;
	}

	if (!navigator.mediaDevices?.getUserMedia) {
		state.permissionState = 'unsupported';
		ElMessage.error('当前浏览器不支持摄像头授权，请使用最新版 Edge 或 Chrome，并通过 localhost 或 HTTPS 访问。');
		return;
	}

	state.isStarting = true;
	state.permissionState = 'requesting';
	try {
		mediaStream = await navigator.mediaDevices.getUserMedia({
			audio: false,
			video: { width: { ideal: 640 }, height: { ideal: 480 }, facingMode: 'user' },
		});
		if (pageDisposed) {
			releaseLocalCamera();
			return;
		}
		state.permissionState = 'granted';
	} catch (error: any) {
		state.isStarting = false;
		state.permissionState = 'denied';
		const blocked = error?.name === 'NotAllowedError' || error?.name === 'SecurityError';
		ElMessage.error(blocked ? '你没有允许摄像头权限，本次感知没有开始。可在浏览器地址栏的权限设置中重新允许。' : '摄像头暂时无法使用，请检查是否被其他程序占用。');
		return;
	}

	state.form.weight = weight.value || 'combined';
	state.form.kind = kind.value;
	state.form.conf = conf.value / 100;
	state.form.username = userInfos.value.userName;
	state.form.startTime = formatDate(new Date(), 'YYYY-mm-dd HH:MM:SS');
	state.form.saveRecord = keepRecord.value ? 'true' : 'false';
	state.form.keepMedia = keepMedia.value ? 'true' : 'false';
	state.form.sessionId = createAnalysisSessionId();
	state.resultReady = false;
	state.isStopping = false;
	state.percentage = 0;
	state.showProgress = false;
	recordSaved.value = false;
	savedAwarenessRecordId.value = 0;
	state.analysisResult = null;
	state.liveCues = [];
	state.processedFrame = '';

	try {
		const consentId = await saveCameraConsent();
		if (pageDisposed) {
			releaseLocalCamera();
			return;
		}
		state.form.consentId = String(consentId);
		const started: any = await request.post('/flask/cameraSession/start', { ...state.form });
		if (started?.status !== 200) throw new Error(started?.message || '摄像头会话建立失败');
		if (pageDisposed) {
			releaseLocalCamera();
			void stopCameraSessionOnLeave();
			return;
		}

		state.cameraIsOpen = true;
		state.isStarting = false;
		captureActive = true;
		await nextTick();
		if (!cameraVideo.value || !mediaStream) throw new Error('摄像头预览初始化失败');
		cameraVideo.value.srcObject = mediaStream;
		await cameraVideo.value.play();
		scheduleNextFrame(0);
		ElMessage.success('摄像头感知已开始，你可以随时停止或直接离开页面。');
	} catch (error: any) {
		releaseLocalCamera();
		state.cameraIsOpen = false;
		state.isStarting = false;
		state.permissionState = 'idle';
		ElMessage.error(error?.message || '授权记录或摄像头分析服务暂时不可用，本次感知没有开始。');
	}
};

const stopCameraSense = async () => {
	if (state.isStopping) return;
	state.isStopping = true;
	captureActive = false;
	if (captureTimer) window.clearTimeout(captureTimer);
	releaseLocalCamera();
	state.showProgress = true;
	state.percentage = Math.max(state.percentage, 10);
	try {
		const result: any = await request.post('/flask/cameraSession/stop', { sessionId: state.form.sessionId });
		if (result?.status !== 200) throw new Error(result?.message || '摄像头结果整理失败');
		state.analysisResult = result as AnalysisResult;
		state.percentage = 100;
		await completeCameraReflection();
	} catch (error) {
		state.cameraIsOpen = false;
		state.isStopping = false;
		state.showProgress = false;
		state.permissionState = 'idle';
		ElMessage.error('摄像头已经关闭，但本次结果整理失败，请稍后重试。');
	}
};

const saveCameraConsent = async () => {
	const saved: any = await savePrivacyConsent({
		username: userInfos.value.userName,
		scene: 'camera',
		consentType: 'camera-recognition',
		consentText: '摄像头隐私说明 v2：浏览器原生授权；仅采集画面不采集声音；画面逐帧交给本地YOLO服务；不发送给DeepSeek；未勾选保留素材时不创建视频；停止或离开页面立即释放摄像头。',
		agreed: cameraAccepted.value,
		keepRecord: keepRecord.value,
		keepMedia: keepMedia.value,
	});
	if ((saved?.code !== '0' && saved?.code !== 0) || !saved?.data?.id) {
		throw new Error(saved?.msg || '隐私授权记录保存失败');
	}
	return Number(saved.data.id);
};

const scheduleNextFrame = (delay = 320) => {
	if (!captureActive) return;
	if (captureTimer) window.clearTimeout(captureTimer);
	captureTimer = window.setTimeout(captureAndAnalyzeFrame, delay);
};

const canvasBlob = (canvas: HTMLCanvasElement) => new Promise<Blob>((resolve, reject) => {
	canvas.toBlob((blob) => blob ? resolve(blob) : reject(new Error('摄像头画面编码失败')), 'image/jpeg', 0.82);
});

const captureAndAnalyzeFrame = async () => {
	if (!captureActive || frameInFlight || !cameraVideo.value || !captureCanvas.value) return;
	const video = cameraVideo.value;
	if (video.readyState < HTMLMediaElement.HAVE_CURRENT_DATA || !video.videoWidth) {
		scheduleNextFrame(120);
		return;
	}
	frameInFlight = true;
	try {
		const width = Math.min(video.videoWidth, 640);
		const height = Math.round(video.videoHeight * (width / video.videoWidth));
		const canvas = captureCanvas.value;
		canvas.width = width;
		canvas.height = height;
		canvas.getContext('2d')?.drawImage(video, 0, 0, width, height);
		const form = new FormData();
		form.append('sessionId', state.form.sessionId);
		form.append('frame', await canvasBlob(canvas), 'camera-frame.jpg');
		const result: any = await request.post('/flask/cameraSession/frame', form, {
			headers: { 'Content-Type': 'multipart/form-data' },
			timeout: 120000,
		});
		if (captureActive && result?.status === 200) {
			state.processedFrame = result.image || state.processedFrame;
			state.liveCues = result.cues || [];
		}
	} catch (error: any) {
		if (captureActive) {
			captureActive = false;
			releaseLocalCamera();
			state.cameraIsOpen = false;
			state.permissionState = 'idle';
			ElMessage.error(error?.response?.data?.message || '实时画面分析中断，摄像头已自动关闭。');
			void stopCameraSessionOnLeave();
		}
	} finally {
		frameInFlight = false;
		if (captureActive) scheduleNextFrame();
	}
};

const releaseLocalCamera = () => {
	mediaStream?.getTracks().forEach((track) => track.stop());
	mediaStream = null;
	if (cameraVideo.value) cameraVideo.value.srcObject = null;
};

const stopCameraSessionOnLeave = async () => {
	const sessionId = state.form.sessionId;
	if (!sessionId) return;
	try {
		await fetch('/flask/cameraSession/stop', {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ sessionId }),
			keepalive: true,
		});
	} catch (_) {
		// The local camera has already been released; the server session also
		// expires from further frame input even if the best-effort request fails.
	}
};

const completeCameraReflection = async () => {
	state.cameraIsOpen = false;
	state.isStopping = false;
	state.processedFrame = '';
	state.permissionState = 'idle';
	state.resultReady = true;
	state.showProgress = false;
	state.percentage = 100;
	if (!keepRecord.value) {
		ElMessage.success('本次分析已完成，已按你的选择不保存觉察记录。');
		return;
	}
	if (recordSaved.value) return;
	recordSaved.value = true;
	try {
		const saved: any = await saveAwarenessRecord({
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
		const awarenessRecordId = Number(saved?.data?.id || 0);
		const analysisRecordId = Number(state.analysisResult?.analysisRecordId || 0);
		if (analysisRecordId && awarenessRecordId) {
			const linked: any = await linkAnalysisRecord(analysisRecordId, awarenessRecordId);
			if (linked.code === '0' || linked.code === 0) savedAwarenessRecordId.value = awarenessRecordId;
		}
		ElMessage.success('本次摄像头觉察已保存，可在“觉察记录”中查看。');
	} catch (error) {
		recordSaved.value = false;
		ElMessage.error('分析已完成，但觉察记录保存失败，请保留当前页面并重试。');
	}
};

const openPreVisitReport = () => {
	if (!savedAwarenessRecordId.value) return;
	router.push({ name: 'preVisitReport', params: { awarenessRecordId: savedAwarenessRecordId.value } });
};

onMounted(() => {
	pageDisposed = false;
	getData();
	window.addEventListener('beforeunload', cleanupCameraOnLeave);
});

function cleanupCameraOnLeave() {
	if (captureTimer) window.clearTimeout(captureTimer);
	captureActive = false;
	releaseLocalCamera();
	if (state.cameraIsOpen || state.isStopping) void stopCameraSessionOnLeave();
}

onUnmounted(() => {
	pageDisposed = true;
	state.isStarting = false;
	window.removeEventListener('beforeunload', cleanupCameraOnLeave);
	cleanupCameraOnLeave();
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

.permission-state {
	display: flex;
	align-items: center;
	gap: 8px;
	padding: 8px 10px;
	border-radius: 8px;
	background: #f5f1eb;
	color: #75695e;
	font-size: 12px;
}

.permission-dot {
	width: 8px;
	height: 8px;
	border-radius: 50%;
	background: #a99f94;
}

.permission-state.is-requesting .permission-dot { background: #d49b50; }
.permission-state.is-granted .permission-dot { background: #5eaa73; }
.permission-state.is-denied .permission-dot,
.permission-state.is-unsupported .permission-dot { background: #d96f67; }

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

.capture-canvas {
	display: none;
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

.report-entry {
	display: flex;
	align-items: center;
	gap: 12px;
	margin-top: 14px;

	span { color: #8b7b6e; font-size: 12px; line-height: 1.6; }
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
