<template>
	<div class="image-sense-page layout-padding">
		<div class="image-sense-shell layout-padding-auto layout-padding-view">
			<section class="hero">
				<div>
					<p class="eyebrow">图片温柔感知</p>
					<h1>把此刻的情绪线索，轻轻看见</h1>
					<span>这里不会给你贴标签。图片识别只是一段自我觉察的入口，真正重要的是你愿意停下来照顾自己。</span>
				</div>
				<div class="hero-companion">
					<div class="soft-orb">安</div>
					<p>{{ companionText }}</p>
				</div>
			</section>

			<section class="privacy-panel">
				<div>
					<strong>开始前的隐私确认</strong>
					<p>图片会先上传到本地后端，再交给本地 YOLO 服务处理。你可以决定是否保存这次觉察记录，以及记录里是否保留素材路径。</p>
				</div>
				<div class="privacy-options">
					<el-checkbox v-model="privacyAccepted">我了解本次图片感知的用途</el-checkbox>
					<el-checkbox v-model="keepRecord">保存这次觉察记录</el-checkbox>
					<el-checkbox v-model="keepMedia" :disabled="!keepRecord">在记录中保留图片路径</el-checkbox>
				</div>
			</section>

			<section class="control-band">
				<div class="control-item">
					<span>感知类型</span>
					<el-select v-model="kind" size="large" @change="getModelData">
						<el-option v-for="item in state.kindItems" :key="item.value" :label="item.label" :value="item.value" />
					</el-select>
				</div>
				<div class="control-item">
					<span>模型</span>
					<el-select v-model="weight" size="large" placeholder="选择模型">
						<el-option v-for="item in state.weightItems" :key="item.value" :label="item.label" :value="item.value" />
					</el-select>
				</div>
				<div class="control-item threshold">
					<span>只接收较清晰的线索</span>
					<el-slider v-model="conf" :format-tooltip="formatTooltip" :min="20" :max="90" />
				</div>
				<el-button class="primary-action" :icon="VideoPlay" :loading="state.loading" @click="startPredict">开始温柔感知</el-button>
				<el-button class="secondary-action" :icon="RefreshRight" @click="loadSampleReflection">看看示例反馈</el-button>
			</section>

			<section class="workbench">
				<article class="panel media-panel">
					<div class="panel-title">
						<span>第一步</span>
						<h2>放入一张想被理解的照片</h2>
					</div>
					<el-upload
						class="warm-uploader"
						action="http://localhost:9999/files/upload"
						:show-file-list="false"
						:before-upload="beforeImageUpload"
						:on-success="handleUploadSuccess"
						:on-change="handleLocalPreview"
					>
						<el-image v-if="imageUrl" :src="imageUrl" class="preview-image" fit="contain" />
						<div v-else class="uploader-content">
							<el-icon><Plus /></el-icon>
							<strong>上传图片</strong>
							<span>我们会把它当作一次轻柔的自我观察</span>
						</div>
					</el-upload>
				</article>

				<article class="panel media-panel">
					<div class="panel-title">
						<span>第二步</span>
						<h2>看见模型标注的线索</h2>
					</div>
					<el-image v-if="predictedImageUrl" :src="predictedImageUrl" class="preview-image result-image" fit="contain" />
					<div v-else class="empty-state">
						<el-icon><Picture /></el-icon>
						<strong>还没有开始感知</strong>
						<span>结果会以柔和的方式出现，只帮助你观察，不给你下结论。</span>
					</div>
				</article>

				<article class="panel result-panel">
					<div class="panel-title">
						<span>第三步</span>
						<h2>收到一份陪伴式反馈</h2>
					</div>
					<div v-if="state.loading" class="empty-state">
						<el-icon><ChatLineRound /></el-icon>
						<strong>安小宁正在组织语言</strong>
						<span>会先接住情绪，再给一个可以马上尝试的小方法。</span>
					</div>
					<div v-else-if="state.resultReady" class="result-card">
						<div class="result-head">
							<div>
								<span>主要线索</span>
								<h3>{{ resultCard.title }}</h3>
							</div>
							<strong v-if="resultCard.confidence">{{ resultCard.confidence }}</strong>
						</div>

						<div class="emotion-list">
							<el-tag
								v-for="(label, index) in state.prediction.labels"
								:key="`${label}-${index}`"
								:class="['emotion-chip', `emotion-${label}`]"
								effect="plain"
							>
								{{ getEmotionChinese(label) }}
							</el-tag>
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
							<span>{{ keepMedia ? '记录里保留图片路径' : '记录里不保留图片路径' }}</span>
						</div>
					</div>
					<div v-else class="empty-state">
						<el-icon><ChatLineRound /></el-icon>
						<strong>这里会生成温柔反馈</strong>
						<span>看见线索、身体提醒、小练习和保存选择会放在同一张卡片里。</span>
					</div>
				</article>
			</section>

			<section class="body-signal-band">
				<div>
					<span class="eyebrow">下一步会继续完善</span>
					<h2>动作与身体信号会接入同一套陪伴式反馈</h2>
					<p>当咬指甲、搓手、抓挠皮肤等动作模型准备好后，这里会从“表情线索”扩展到“身体信号”。表达方式仍会保持陪伴感，而不是压迫感。</p>
				</div>
				<div class="body-steps">
					<span>看见身体信号</span>
					<span>识别高频时段</span>
					<span>引导呼吸或放松</span>
				</div>
			</section>
		</div>
	</div>
</template>

<script setup lang="ts" name="emotionRecognition">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import type { UploadFile, UploadProps } from 'element-plus';
import { ElMessage } from 'element-plus';
import { ChatLineRound, Picture, Plus, RefreshRight, VideoPlay } from '@element-plus/icons-vue';
import request from '/@/utils/request';
import { saveAwarenessRecord, savePrivacyConsent } from '/@/api/healing';
import { useUserInfo } from '/@/stores/userInfo';
import { storeToRefs } from 'pinia';
import { formatDate } from '/@/utils/formatTime';

const stores = useUserInfo();
const { userInfos } = storeToRefs(stores);

const imageUrl = ref('');
const predictedImageUrl = ref('');
const conf = ref(45);
const weight = ref('emotion.pt');
const kind = ref('emotion');
const privacyAccepted = ref(false);
const keepRecord = ref(true);
const keepMedia = ref(false);

const state = reactive({
	kindItems: [{ value: 'emotion', label: '情绪表情感知' }],
	weightItems: [] as Array<{ value: string; label: string }>,
	img: '',
	loading: false,
	resultReady: false,
	prediction: {
		labels: [] as string[],
		confidences: [] as number[],
		allTime: '',
		personCount: 0,
	},
	form: {
		username: '',
		inputImg: '',
		weight: '',
		conf: 0,
		kind: '',
		startTime: '',
		keepRecord: true,
		keepMedia: false,
	},
});

const emotionMap: Record<string, string> = {
	happy: '明亮',
	sad: '低落',
	angry: '紧绷',
	neutral: '平稳',
};

const companionText = computed(() => {
	const primary = state.prediction.labels[0];
	if (!primary) return '你可以慢慢来。我们先看见当下，不急着改变什么。';
	return getCompanionSentence(primary);
});

const resultCard = computed(() => {
	const primary = state.prediction.labels[0] || '';
	const confidence = state.prediction.confidences[0] ? `${state.prediction.confidences[0].toFixed(0)}%` : '';
	return {
		title: primary ? `${getEmotionChinese(primary)}感被看见了` : '这一刻被轻轻看见了',
		confidence,
		summary: buildSummary(primary, confidence),
		bodySignal: buildBodySignal(primary),
		practice: buildPractice(primary),
	};
});

watch(keepRecord, (value) => {
	if (!value) keepMedia.value = false;
});

const formatTooltip = (val: number) => `${val}%`;

const beforeImageUpload: UploadProps['beforeUpload'] = () => {
	if (!privacyAccepted.value) {
		ElMessage.warning('请先勾选隐私确认，再上传图片。');
		return false;
	}
	return true;
};

const handleUploadSuccess: UploadProps['onSuccess'] = (response, file) => {
	if (file.raw) imageUrl.value = URL.createObjectURL(file.raw);
	state.img = response.data;
	state.resultReady = false;
	state.prediction.labels = [];
	state.prediction.confidences = [];
	predictedImageUrl.value = '';
	ElMessage.success('图片已放好，可以开始温柔感知。');
};

const handleLocalPreview = (file: UploadFile) => {
	if (file.raw) imageUrl.value = URL.createObjectURL(file.raw);
};

const getModelData = () => {
	request
		.get('/api/flask/file_names')
		.then((res) => {
			if (res.code === '0' || res.code === 0) {
				const data = typeof res.data === 'string' ? JSON.parse(res.data) : res.data;
				const weights = data.weight_items || [];
				state.weightItems = weights.filter((item: any) => item.value.includes(kind.value) || item.value.includes('emotion'));
				if (state.weightItems.length > 0) weight.value = state.weightItems[0].value;
			} else {
				ElMessage.warning('暂时没有拿到模型列表，稍后可以再试一次。');
			}
		})
		.catch(() => {
			state.weightItems = [{ value: 'emotion.pt', label: 'emotion.pt' }];
			weight.value = 'emotion.pt';
		});
};

const startPredict = async () => {
	if (!privacyAccepted.value) {
		ElMessage.warning('请先确认隐私说明，我们再一起开始。');
		return;
	}
	if (!state.img) {
		ElMessage.warning('先上传一张图片，我们再一起轻轻看看。');
		return;
	}

	state.loading = true;
	state.form = {
		username: userInfos.value.userName,
		inputImg: state.img,
		weight: weight.value || 'emotion.pt',
		conf: conf.value / 100,
		kind: kind.value,
		startTime: formatDate(new Date(), 'YYYY-mm-dd HH:MM:SS'),
		keepRecord: keepRecord.value,
		keepMedia: keepMedia.value,
	};

	saveImageConsent();

	try {
		const res = await request.post('/api/flask/predict', state.form);
		if (res.code === '0' || res.code === 0) {
			await processPredictionResult(res.data);
			ElMessage.success('感知完成。谢谢你愿意停下来看见自己。');
		} else {
			ElMessage.warning(res.msg || '这次没有捕捉到清晰线索，可以换一张图片试试。');
		}
	} catch (error) {
		ElMessage.warning('感知服务暂时没有连上，你也可以先看看示例反馈。');
	} finally {
		state.loading = false;
	}
};

const processPredictionResult = async (data: any) => {
	const parsed = typeof data === 'string' ? JSON.parse(data) : data;
	state.prediction.labels = normalizeLabels(parsed.label);
	state.prediction.confidences = normalizeConfidence(parsed.confidence);
	state.prediction.allTime = parsed.allTime || '0';
	state.prediction.personCount = parsed.personCount || state.prediction.labels.length;
	predictedImageUrl.value = parsed.outImg || '';
	state.resultReady = true;

	if (keepRecord.value) {
		await saveCurrentAwarenessRecord(parsed);
	}
};

const saveImageConsent = () => {
	savePrivacyConsent({
		username: userInfos.value.userName,
		scene: 'image',
		consentType: 'image-recognition',
		consentText: '用户确认图片会上传到后端并交给本地 YOLO 服务处理，结果仅作为觉察线索。',
		agreed: privacyAccepted.value,
		keepRecord: keepRecord.value,
		keepMedia: keepMedia.value,
	}).catch(() => {});
};

const saveCurrentAwarenessRecord = async (parsed: any) => {
	const primary = state.prediction.labels[0] || '';
	const confidence = state.prediction.confidences[0] ? `${state.prediction.confidences[0].toFixed(0)}%` : '';
	await saveAwarenessRecord({
		username: userInfos.value.userName,
		sourceType: 'image',
		emotionLabel: primary,
		confidence,
		bodySignal: resultCard.value.bodySignal,
		gentleSummary: resultCard.value.summary,
		suggestedPractice: resultCard.value.practice,
		inputMedia: keepMedia.value ? state.form.inputImg : '',
		outputMedia: keepMedia.value ? parsed.outImg || predictedImageUrl.value : '',
		keepRecord: keepRecord.value,
		keepMedia: keepMedia.value,
		privacyNote: keepMedia.value ? '你选择在记录中保留图片路径，之后可以在觉察记录中删除。' : '你选择不在觉察记录中保留图片路径，只留下温柔摘要。',
	});
};

const loadSampleReflection = () => {
	state.prediction.labels = ['sad'];
	state.prediction.confidences = [78];
	state.prediction.allTime = '0.12';
	state.prediction.personCount = 1;
	state.resultReady = true;
	predictedImageUrl.value = imageUrl.value;
};

const normalizeLabels = (labels: any) => {
	if (Array.isArray(labels)) return labels.map((item) => String(item).toLowerCase());
	if (typeof labels === 'string') {
		try {
			const parsed = JSON.parse(labels);
			if (Array.isArray(parsed)) return parsed.map((item) => String(item).toLowerCase());
		} catch {
			return [labels.toLowerCase()];
		}
	}
	return [];
};

const normalizeConfidence = (confidence: any) => {
	const normalize = (value: any) => {
		const num = typeof value === 'string' ? parseFloat(value) : Number(value);
		if (Number.isNaN(num)) return 0;
		return num <= 1 ? num * 100 : num;
	};
	if (Array.isArray(confidence)) return confidence.map(normalize);
	if (typeof confidence === 'string') {
		try {
			const parsed = JSON.parse(confidence);
			if (Array.isArray(parsed)) return parsed.map(normalize);
			return [normalize(parsed)];
		} catch {
			return [normalize(confidence)];
		}
	}
	if (confidence !== undefined && confidence !== null) return [normalize(confidence)];
	return [];
};

const getEmotionChinese = (emotion: string) => emotionMap[emotion?.toLowerCase()] || emotion || '未命名线索';

const getCompanionSentence = (emotion: string) => {
	const textMap: Record<string, string> = {
		happy: '我看见了一点明亮的能量。也许今天有某个瞬间正在支撑你，值得被好好记住。',
		sad: '我注意到一些低落的信号。没关系，情绪落下来时，我们可以先不追赶效率，先照顾自己。',
		angry: '我看见了一些紧绷感。紧绷常常是在保护重要的边界，我们可以先让身体安全地放松一点。',
		neutral: '此刻看起来比较平稳。平稳不是空白，它也可能是在给你恢复能量的间隙。',
	};
	return textMap[emotion] || '我看见了一点情绪线索。我们先温柔地观察，不急着给它下结论。';
};

const buildSummary = (emotion: string, confidence: string) => {
	if (!emotion) return '这张图片里暂时没有形成清晰线索。没有关系，平静或模糊本身也可以被温柔看见。';
	const prefix = confidence ? `模型看到的主要线索是“${getEmotionChinese(emotion)}”，清晰程度约 ${confidence}。` : `模型看到的主要线索是“${getEmotionChinese(emotion)}”。`;
	return `${prefix} 这只是帮助你觉察当下状态的参考，不代表对你的定义。`;
};

const buildBodySignal = (emotion: string) => {
	const map: Record<string, string> = {
		happy: '可以感受胸口、脸部和肩膀是否更舒展，把这个轻一点的瞬间记在身体里。',
		sad: '可以留意眼睛、肩颈、胃部和呼吸是否有些沉，先允许身体慢下来。',
		angry: '可以留意下颌、手心、肩膀是否紧绷，轻轻松开一点点就够了。',
		neutral: '可以观察呼吸是否平稳、身体哪里最舒服，把注意力放在那里停一会儿。',
	};
	return map[emotion] || '可以留意呼吸、肩颈、胃部和手心的紧绷程度，看看身体此刻需要什么。';
};

const buildPractice = (emotion: string) => {
	const map: Record<string, string> = {
		happy: '写下一件刚刚支撑你的事，哪怕很小。让这份明亮成为今天继续前进的一点燃料。',
		sad: '做三轮慢呼吸，然后把今天的任务缩小到“只做第一小步”。先让自己重新落地。',
		angry: '把双脚踩稳，呼气时放松下颌和肩膀。然后写下：我正在保护什么边界？',
		neutral: '保持这个节奏一分钟，轻轻问自己：接下来最适合快一点，还是慢一点？',
	};
	return map[emotion] || '先做三轮慢呼吸：吸气 4 秒，停留 2 秒，呼气 6 秒。然后把此刻最需要的一件小事写下来。';
};

onMounted(() => {
	getModelData();
});
</script>

<style scoped lang="scss">
.image-sense-page {
	min-height: 100%;
	background: #fbf6ef;
	color: #3f3b35;
	overflow: visible;
}

.image-sense-page.layout-padding {
	position: relative;
	display: block;
	height: auto;
	min-height: 100%;
	overflow: visible;
}

.image-sense-shell {
	padding: 22px;
	min-height: 100%;
}

.image-sense-shell.layout-padding-auto,
.image-sense-shell.layout-padding-view {
	display: block;
	height: auto;
	min-height: 100%;
	overflow: visible;
}

.hero,
.privacy-panel,
.control-band,
.panel,
.body-signal-band {
	border: 1px solid rgba(174, 133, 91, 0.14);
	border-radius: 8px;
	background: rgba(255, 255, 255, 0.9);
	box-shadow: 0 14px 32px rgba(91, 70, 45, 0.08);
}

.hero {
	display: grid;
	grid-template-columns: minmax(0, 1.5fr) minmax(280px, 0.7fr);
	gap: 18px;
	align-items: stretch;
	padding: 30px;
	background: linear-gradient(135deg, #fff8ef, #f4eadc);

	h1 {
		margin: 8px 0 10px;
		color: #3f332b;
		font-size: 34px;
		line-height: 1.18;
		letter-spacing: 0;
	}

	span {
		color: #75695e;
		line-height: 1.8;
	}
}

.eyebrow {
	margin: 0;
	color: #b47b4f;
	font-size: 13px;
	font-weight: 900;
}

.hero-companion {
	display: flex;
	align-items: center;
	gap: 16px;
	padding: 18px;
	border-radius: 8px;
	background: rgba(255, 250, 244, 0.86);

	p {
		margin: 0;
		color: #67594e;
		line-height: 1.7;
	}
}

.soft-orb {
	width: 70px;
	aspect-ratio: 1;
	border-radius: 50%;
	display: grid;
	place-items: center;
	flex: 0 0 auto;
	background: radial-gradient(circle at 35% 30%, #ffffff 0 18%, #8fcfa7 19% 58%, #f3c77d 59% 100%);
	color: #5b432f;
	font-size: 24px;
	font-weight: 900;
	animation: breathe 3.8s ease-in-out infinite;
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

.control-band {
	display: grid;
	grid-template-columns: 200px 200px minmax(260px, 1fr) 150px 150px;
	gap: 14px;
	align-items: end;
	margin-top: 16px;
	padding: 16px;
}

.control-item {
	display: flex;
	flex-direction: column;
	gap: 8px;

	span {
		color: #7b6d62;
		font-size: 13px;
		font-weight: 800;
	}
}

.primary-action,
.secondary-action {
	height: 42px;
	border-radius: 8px;
	font-weight: 800;
}

.primary-action {
	border: none;
	background: #c98f5c;
	color: #ffffff;
}

.secondary-action {
	border-color: rgba(201, 143, 92, 0.32);
	color: #7a5a43;
	background: #fffaf4;
}

.workbench {
	display: grid;
	grid-template-columns: repeat(3, minmax(0, 1fr));
	gap: 18px;
	margin-top: 18px;
}

.panel {
	padding: 18px;
	min-width: 0;
}

.panel-title {
	min-height: 58px;

	span {
		color: #b47b4f;
		font-size: 13px;
		font-weight: 900;
	}

	h2 {
		margin: 6px 0 0;
		color: #3f332b;
		font-size: 18px;
		letter-spacing: 0;
	}
}

.warm-uploader,
.preview-image,
.empty-state,
.result-card {
	width: 100%;
	height: 380px;
	border-radius: 8px;
}

.warm-uploader {
	border: 1px dashed rgba(201, 143, 92, 0.42);
	background: linear-gradient(180deg, #fffdf9, #f8f0e5);
	display: flex;
	align-items: center;
	justify-content: center;
	overflow: hidden;
}

.preview-image {
	background: #fffaf4;
	object-fit: contain;
}

.result-image {
	border: 1px solid rgba(201, 143, 92, 0.18);
}

.uploader-content,
.empty-state {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	gap: 10px;
	text-align: center;
	padding: 28px;
	color: #75695e;

	.el-icon {
		font-size: 34px;
		color: #c98f5c;
	}

	strong {
		color: #604838;
		font-size: 16px;
	}

	span {
		line-height: 1.7;
	}
}

.result-card {
	padding: 16px;
	background: #fffaf4;
	border: 1px solid rgba(201, 143, 92, 0.16);
	overflow-y: auto;
}

.result-head {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 12px;

	span {
		color: #b47b4f;
		font-size: 13px;
		font-weight: 900;
	}

	h3 {
		margin: 6px 0 0;
		color: #3f332b;
		font-size: 21px;
		letter-spacing: 0;
	}

	strong {
		padding: 8px 11px;
		border-radius: 999px;
		background: #eef8ef;
		color: #4e7e5f;
	}
}

.emotion-list {
	display: flex;
	flex-wrap: wrap;
	gap: 8px;
	margin-top: 14px;
}

.emotion-chip {
	border-radius: 8px;
	font-weight: 800;
}

.emotion-happy {
	background: #fff9e8;
	border-color: #f3c96d;
	color: #8a651d;
}

.emotion-sad {
	background: #edf7ff;
	border-color: #8ec5e8;
	color: #356b8b;
}

.emotion-angry {
	background: #fff1ed;
	border-color: #f1a48d;
	color: #9b4b37;
}

.emotion-neutral {
	background: #f2f7f4;
	border-color: #9fc8b2;
	color: #4d755f;
}

.closed-loop {
	display: grid;
	gap: 10px;
	margin-top: 14px;

	div {
		padding: 12px;
		border-radius: 8px;
		background: #ffffff;
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

.body-signal-band {
	display: grid;
	grid-template-columns: minmax(0, 1.2fr) minmax(280px, 0.8fr);
	gap: 20px;
	margin-top: 18px;
	padding: 22px;

	h2 {
		margin: 8px 0 10px;
		color: #3f332b;
		font-size: 22px;
		letter-spacing: 0;
	}

	p {
		margin: 0;
		color: #75695e;
		line-height: 1.8;
	}
}

.body-steps {
	display: grid;
	grid-template-columns: repeat(3, minmax(0, 1fr));
	gap: 10px;

	span {
		display: grid;
		place-items: center;
		min-height: 76px;
		padding: 12px;
		border-radius: 8px;
		background: #fff7ed;
		color: #7a5a43;
		font-weight: 800;
		text-align: center;
	}
}

:deep(.el-slider__bar) {
	background-color: #c98f5c;
}

:deep(.el-slider__button) {
	border-color: #c98f5c;
}

@keyframes breathe {
	0%,
	100% {
		transform: scale(0.96);
		box-shadow: 0 0 0 0 rgba(201, 143, 92, 0.24);
	}
	50% {
		transform: scale(1.04);
		box-shadow: 0 0 0 16px rgba(201, 143, 92, 0);
	}
}

@media (max-width: 1180px) {
	.hero,
	.privacy-panel,
	.control-band,
	.workbench,
	.body-signal-band {
		grid-template-columns: 1fr;
	}
}

@media (max-width: 720px) {
	.image-sense-shell {
		padding: 14px;
	}

	.hero {
		padding: 22px;

		h1 {
			font-size: 28px;
		}
	}

	.warm-uploader,
	.preview-image,
	.empty-state,
	.result-card {
		height: 320px;
	}

	.body-steps {
		grid-template-columns: 1fr;
	}
}
</style>
