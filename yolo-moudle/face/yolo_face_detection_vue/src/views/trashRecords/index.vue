<template>
	<div class="awareness-page layout-padding">
		<div class="awareness-shell layout-padding-auto layout-padding-view">
			<section class="hero">
				<div>
					<p class="eyebrow">觉察记录</p>
					<h1>把每一次看见自己，都轻轻收好</h1>
					<span>这里记录的是情绪与身体线索，不给你贴标签。你可以回看状态变化，也可以随时删掉不想留下的记录。</span>
				</div>
				<button class="refresh-btn" type="button" @click="getRecords">刷新</button>
			</section>

			<section class="summary-row">
				<div class="summary-item">
					<span>已保存记录</span>
					<strong>{{ state.total }}</strong>
				</div>
				<div class="summary-item">
					<span>本页图片觉察</span>
					<strong>{{ sourceCount.image }}</strong>
				</div>
				<div class="summary-item">
					<span>本页视频觉察</span>
					<strong>{{ sourceCount.video }}</strong>
				</div>
				<div class="summary-item">
					<span>本页摄像头觉察</span>
					<strong>{{ sourceCount.camera }}</strong>
				</div>
			</section>

			<section class="toolbar">
				<div class="filter-group">
					<button
						v-for="item in sourceOptions"
						:key="item.value"
						:class="['filter-btn', { active: state.params.sourceType === item.value }]"
						type="button"
						@click="changeSource(item.value)"
					>
						{{ item.label }}
					</button>
				</div>
				<el-input
					v-model="state.params.emotionLabel"
					clearable
					placeholder="搜索情绪线索，如 happy / sad / neutral"
					class="search-input"
					@clear="getRecords"
					@keyup.enter="getRecords"
				/>
				<button class="search-btn" type="button" @click="getRecords">查找</button>
			</section>

			<section v-loading="state.loading" class="timeline">
				<div v-if="state.records.length === 0" class="empty-state">
					<strong>还没有留下觉察记录</strong>
					<p>你可以先去图片、视频或摄像头感知页做一次轻柔识别。记录只会在你选择保存时留下。</p>
				</div>

				<article v-for="item in state.records" :key="item.id" class="record-card">
					<div class="record-time">
						<span>{{ formatDateText(item.createdAt) }}</span>
						<em>{{ sourceName(item.sourceType) }}</em>
					</div>

					<div class="record-main">
						<div class="record-head">
							<div>
								<p class="eyebrow">{{ sourceName(item.sourceType) }}</p>
								<h2>{{ emotionTitle(item.emotionLabel) }}</h2>
							</div>
							<div class="confidence" v-if="item.confidence">{{ item.confidence }}</div>
						</div>

						<div class="media-row" v-if="item.outputMedia || item.inputMedia">
							<el-image
								v-if="isImage(item.outputMedia || item.inputMedia)"
								:src="item.outputMedia || item.inputMedia"
								fit="cover"
								class="record-image"
								:preview-src-list="[item.outputMedia || item.inputMedia]"
							/>
							<div v-else class="media-pill">
								<span>{{ item.keepMedia ? '已保留素材路径' : '未保留原始素材' }}</span>
								<small>{{ item.outputMedia || item.inputMedia }}</small>
							</div>
						</div>

						<div class="reflection">
							<div>
								<span>我看见的线索</span>
								<p>{{ item.gentleSummary || fallbackSummary(item) }}</p>
							</div>
							<div>
								<span>身体可以被温柔询问</span>
								<p>{{ item.bodySignal || '可以留意呼吸、肩颈、胃部和手心的紧绷程度，看看身体此刻需要什么。' }}</p>
							</div>
							<div>
								<span>此刻的小练习</span>
								<p>{{ item.suggestedPractice || '先做三轮慢呼吸：吸气 4 秒，停留 2 秒，呼气 6 秒。' }}</p>
							</div>
						</div>

						<div v-if="state.expandedAnalysis[item.id]" v-loading="state.analysisLoading[item.id]" class="structured-analysis">
							<template v-if="state.analysisDetails[item.id]">
								<div class="analysis-heading">
									<div>
										<span>完整分析结果</span>
										<strong>{{ analysisModeName(state.analysisDetails[item.id].record.analysisMode) }}</strong>
									</div>
									<div class="analysis-metrics">
										<span>情绪类型 {{ state.analysisDetails[item.id].emotionResults.length }}</span>
										<span>BFRB事件 {{ state.analysisDetails[item.id].record.bfrbEventCount || 0 }}</span>
										<span>累计 {{ state.analysisDetails[item.id].record.bfrbTotalDurationSeconds || 0 }} 秒</span>
									</div>
								</div>

								<div v-if="state.analysisDetails[item.id].record.complaint || state.analysisDetails[item.id].record.additionalNotes" class="patient-notes">
									<p v-if="state.analysisDetails[item.id].record.complaint"><span>主诉</span>{{ state.analysisDetails[item.id].record.complaint }}</p>
									<p v-if="state.analysisDetails[item.id].record.additionalNotes"><span>补充说明</span>{{ state.analysisDetails[item.id].record.additionalNotes }}</p>
								</div>

								<div v-if="state.analysisDetails[item.id].emotionResults.length" class="emotion-results">
									<div v-for="emotion in state.analysisDetails[item.id].emotionResults" :key="emotion.id || emotion.emotionType">
										<strong>{{ emotion.emotionType }}</strong>
										<span>{{ emotion.frameCount }} 次采样</span>
										<span>平均 {{ confidenceText(emotion.averageConfidence) }}</span>
										<span>最高 {{ confidenceText(emotion.maxConfidence) }}</span>
									</div>
								</div>

								<div v-if="state.analysisDetails[item.id].bfrbEvents.length" class="event-details">
									<div v-for="event in state.analysisDetails[item.id].bfrbEvents" :key="event.id || event.eventKey">
										<div>
											<strong>{{ event.cueType || event.behaviorCode }}</strong>
											<span>{{ evidenceName(event.evidenceType) }}</span>
										</div>
										<p>{{ event.startSeconds }}s–{{ event.endSeconds }}s，持续 {{ event.durationSeconds }}s；关键帧 {{ event.keyFrameSeconds }}s；最高置信度 {{ confidenceText(event.maxConfidence) }}</p>
									</div>
								</div>
								<p v-else class="no-events">本次没有形成满足连续性阈值的 BFRB 事件，零散命中未被重复计数。</p>
							</template>
							<p v-else-if="!state.analysisLoading[item.id]" class="no-events">这是一条旧版觉察记录，尚未关联结构化检测明细。</p>
						</div>

						<div class="record-foot">
							<div class="privacy">
								<span>{{ item.keepRecord ? '已保存记录' : '未保存记录' }}</span>
								<span>{{ item.keepMedia ? '保留素材' : '不主动保留素材' }}</span>
								<span>{{ item.privacyNote || '你可以随时删除这条记录。' }}</span>
							</div>
							<div class="record-actions">
								<button type="button" class="detail-btn" @click="toggleAnalysisDetail(item)">
									{{ state.expandedAnalysis[item.id] ? '收起完整分析' : '查看完整分析' }}
								</button>
								<button type="button" class="delete-btn" @click="deleteRecord(item)">删除</button>
							</div>
						</div>
					</div>
				</article>
			</section>

			<el-pagination
				v-if="state.total > state.params.pageSize"
				class="pagination"
				background
				layout="total, prev, pager, next"
				v-model:current-page="state.params.pageNum"
				:page-size="state.params.pageSize"
				:total="state.total"
				@current-change="getRecords"
			/>
		</div>
	</div>
</template>

<script setup lang="ts" name="AwarenessRecords">
import { computed, onMounted, reactive } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '/@/utils/request';
import { getAnalysisRecordByAwareness, getAwarenessRecords } from '/@/api/healing';
import { useUserInfo } from '/@/stores/userInfo';
import { storeToRefs } from 'pinia';

type AwarenessRecord = {
	id: number;
	username?: string;
	sourceType?: string;
	emotionLabel?: string;
	confidence?: string;
	bodySignal?: string;
	gentleSummary?: string;
	suggestedPractice?: string;
	inputMedia?: string;
	outputMedia?: string;
	keepRecord?: boolean;
	keepMedia?: boolean;
	privacyNote?: string;
	createdAt?: string;
};

type AnalysisDetail = {
	record: {
		id: number;
		analysisMode?: string;
		modelConfiguration?: string;
		bfrbEventCount?: number;
		bfrbTotalDurationSeconds?: number;
		complaint?: string;
		additionalNotes?: string;
	};
	emotionResults: Array<{
		id?: number;
		emotionType: string;
		frameCount: number;
		averageConfidence: number;
		maxConfidence: number;
	}>;
	bfrbEvents: Array<{
		id?: number;
		eventKey?: string;
		behaviorCode?: string;
		cueType?: string;
		startSeconds?: number;
		endSeconds?: number;
		durationSeconds?: number;
		keyFrameSeconds?: number;
		maxConfidence?: number;
		evidenceType?: string;
	}>;
	behaviorStats: Array<Record<string, any>>;
};

const stores = useUserInfo();
const { userInfos } = storeToRefs(stores);

const sourceOptions = [
	{ label: '全部', value: '' },
	{ label: '图片觉察', value: 'image' },
	{ label: '视频觉察', value: 'video' },
	{ label: '摄像头觉察', value: 'camera' },
];

const state = reactive({
	loading: false,
	records: [] as AwarenessRecord[],
	total: 0,
	analysisDetails: {} as Record<number, AnalysisDetail>,
	expandedAnalysis: {} as Record<number, boolean>,
	analysisLoading: {} as Record<number, boolean>,
	params: {
		pageNum: 1,
		pageSize: 8,
		username: '',
		sourceType: '',
		emotionLabel: '',
	},
});

const sourceCount = computed(() => {
	return state.records.reduce(
		(acc, item) => {
			const key = item.sourceType || 'image';
			if (key === 'image' || key === 'video' || key === 'camera') acc[key] += 1;
			return acc;
		},
		{ image: 0, video: 0, camera: 0 }
	);
});

const getRecords = async () => {
	state.loading = true;
	try {
		state.params.username = userInfos.value.userName === 'admin' ? '' : userInfos.value.userName;
		const res = await getAwarenessRecords(state.params);
		if (res.code === '0' || res.code === 0) {
			state.records = res.data?.records || [];
			state.total = res.data?.total || 0;
		} else {
			ElMessage.warning(res.msg || '暂时没有拿到记录，稍后再试一次。');
		}
	} catch (error) {
		ElMessage.warning('觉察记录暂时没有连上，先不用着急。');
	} finally {
		state.loading = false;
	}
};

const changeSource = (sourceType: string) => {
	state.params.sourceType = sourceType;
	state.params.pageNum = 1;
	getRecords();
};

const deleteRecord = async (item: AwarenessRecord) => {
	await ElMessageBox.confirm('删除后这条觉察记录不会继续保留。确认要删除吗？', '删除记录', {
		confirmButtonText: '确认删除',
		cancelButtonText: '先保留',
		type: 'warning',
	});
	const structured = await request.delete(`/api/analysisRecords/by-awareness/${item.id}`);
	if (structured.code !== '0' && structured.code !== 0 && structured.code !== '404') {
		ElMessage.warning(structured.msg || '完整分析结果暂时无法删除，请稍后再试。');
		return;
	}
	const res = await request.delete(`/api/awarenessRecords/${item.id}`);
	if (res.code === '0' || res.code === 0) {
		delete state.analysisDetails[item.id];
		delete state.expandedAnalysis[item.id];
		ElMessage.success('已经帮你删掉这条记录。');
		getRecords();
	} else {
		ElMessage.warning(res.msg || '删除暂时没有完成，稍后再试一次。');
	}
};

const toggleAnalysisDetail = async (item: AwarenessRecord) => {
	if (state.expandedAnalysis[item.id]) {
		state.expandedAnalysis[item.id] = false;
		return;
	}
	state.expandedAnalysis[item.id] = true;
	if (state.analysisDetails[item.id]) return;
	state.analysisLoading[item.id] = true;
	try {
		const res = await getAnalysisRecordByAwareness(item.id);
		if (res.code === '0' || res.code === 0) {
			state.analysisDetails[item.id] = res.data as AnalysisDetail;
		}
	} catch (error) {
		ElMessage.warning('完整分析结果暂时没有连上，稍后再试一次。');
	} finally {
		state.analysisLoading[item.id] = false;
	}
};

const analysisModeName = (mode?: string) => {
	const labels: Record<string, string> = {
		combined: '综合分析（情绪 + BFRB 双证据）',
		emotion: '情绪表情识别',
		bfrb_behavior: 'BFRB 行为直检',
		bfrb_geometry: 'BFRB 手脸几何',
	};
	return labels[mode || ''] || '结构化检测';
};

const evidenceName = (evidence?: string) => {
	if (evidence === 'behavior-model+hand-face-geometry') return '行为模型 + 手脸几何双证据';
	if (evidence === 'hand-face-geometry') return '手脸几何证据';
	if (evidence === 'behavior-model') return 'BFRB 行为模型证据';
	return '辅助线索';
};

const confidenceText = (value?: number) => `${Math.round(Number(value || 0) * 1000) / 10}%`;

const sourceName = (sourceType?: string) => {
	const map: Record<string, string> = {
		image: '图片觉察',
		video: '视频觉察',
		camera: '摄像头觉察',
	};
	return map[sourceType || 'image'] || '温柔觉察';
};

const emotionTitle = (label?: string) => {
	const map: Record<string, string> = {
		happy: '明亮感被看见了',
		sad: '低落感被轻轻接住',
		angry: '紧绷感正在提醒边界',
		neutral: '平稳感也值得被记录',
	};
	return map[(label || '').toLowerCase()] || '这一刻的状态被记录下来了';
};

const fallbackSummary = (item: AwarenessRecord) => {
	return `这次${sourceName(item.sourceType)}捕捉到的主要线索是“${item.emotionLabel || '暂不清晰'}”。它只是帮助你回看当下状态的参考，不代表对你的定义。`;
};

const formatDateText = (value?: string) => {
	if (!value) return '刚刚';
	const date = new Date(value);
	if (Number.isNaN(date.getTime())) return value;
	return date.toLocaleString('zh-CN', {
		month: '2-digit',
		day: '2-digit',
		hour: '2-digit',
		minute: '2-digit',
	});
};

const isImage = (url?: string) => {
	if (!url) return false;
	return /\.(png|jpe?g|gif|webp|bmp)(\?|$)/i.test(url) || url.startsWith('blob:') || url.startsWith('data:image');
};

onMounted(() => {
	getRecords();
});
</script>

<style scoped lang="scss">
.awareness-page {
	min-height: 100vh;
	background: #fbf6ef;
	color: #3f3b35;
}

.awareness-shell {
	padding: 22px;
}

.hero,
.summary-item,
.toolbar,
.record-main,
.empty-state {
	border: 1px solid rgba(174, 133, 91, 0.14);
	border-radius: 8px;
	background: rgba(255, 255, 255, 0.9);
	box-shadow: 0 14px 32px rgba(91, 70, 45, 0.08);
}

.hero {
	display: flex;
	align-items: flex-end;
	justify-content: space-between;
	gap: 20px;
	padding: 28px 30px;
	background: linear-gradient(135deg, #fff8ef, #f3eadc);

	h1 {
		margin: 8px 0 10px;
		color: #3f332b;
		font-size: 30px;
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
	font-weight: 800;
}

.refresh-btn,
.search-btn,
.filter-btn,
.detail-btn,
.delete-btn {
	border: none;
	border-radius: 8px;
	font-weight: 800;
	cursor: pointer;
	transition: 0.2s ease;
}

.refresh-btn,
.search-btn {
	padding: 11px 18px;
	background: #c98f5c;
	color: #ffffff;
}

.summary-row {
	display: grid;
	grid-template-columns: repeat(4, minmax(0, 1fr));
	gap: 14px;
	margin-top: 16px;
}

.summary-item {
	padding: 18px;

	span {
		display: block;
		color: #7b6d62;
	}

	strong {
		display: block;
		margin-top: 8px;
		color: #6d4c36;
		font-size: 28px;
	}
}

.toolbar {
	display: grid;
	grid-template-columns: minmax(360px, 1fr) minmax(260px, 360px) 90px;
	gap: 12px;
	align-items: center;
	margin-top: 16px;
	padding: 14px;
}

.filter-group {
	display: flex;
	flex-wrap: wrap;
	gap: 10px;
}

.filter-btn {
	padding: 10px 14px;
	background: #f4e6d3;
	color: #7a5a43;
}

.filter-btn.active {
	background: #8fbf9f;
	color: #ffffff;
}

.search-input {
	width: 100%;
}

.timeline {
	display: grid;
	gap: 18px;
	margin-top: 18px;
	min-height: 240px;
}

.empty-state {
	display: grid;
	place-items: center;
	min-height: 260px;
	padding: 30px;
	text-align: center;

	strong {
		color: #6d4c36;
		font-size: 20px;
	}

	p {
		max-width: 520px;
		color: #75695e;
		line-height: 1.8;
	}
}

.record-card {
	display: grid;
	grid-template-columns: 118px minmax(0, 1fr);
	gap: 16px;
}

.record-time {
	padding-top: 18px;
	text-align: right;

	span,
	em {
		display: block;
	}

	span {
		color: #6d4c36;
		font-weight: 800;
	}

	em {
		margin-top: 6px;
		color: #9c8d80;
		font-style: normal;
	}
}

.record-main {
	padding: 18px;
}

.record-head {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 16px;

	h2 {
		margin: 6px 0 0;
		color: #3f332b;
		font-size: 22px;
		letter-spacing: 0;
	}
}

.confidence {
	flex: 0 0 auto;
	padding: 9px 12px;
	border-radius: 999px;
	background: #eef8ef;
	color: #4e7e5f;
	font-weight: 900;
}

.media-row {
	margin-top: 14px;
}

.record-image {
	width: 180px;
	height: 110px;
	border-radius: 8px;
	background: #f7efe4;
}

.media-pill {
	display: grid;
	gap: 6px;
	padding: 12px;
	border-radius: 8px;
	background: #fff7ed;
	color: #6b5748;

	small {
		color: #9a8877;
		word-break: break-all;
	}
}

.reflection {
	display: grid;
	grid-template-columns: repeat(3, minmax(0, 1fr));
	gap: 12px;
	margin-top: 16px;

	div {
		padding: 14px;
		border-radius: 8px;
		background: #fffaf4;
	}

	span {
		color: #9a6847;
		font-size: 13px;
		font-weight: 900;
	}

	p {
		margin: 8px 0 0;
		color: #67594e;
		line-height: 1.75;
	}
}

.record-foot {
	display: flex;
	align-items: flex-end;
	justify-content: space-between;
	gap: 16px;
	margin-top: 16px;
	padding-top: 14px;
	border-top: 1px dashed rgba(174, 133, 91, 0.2);
}

.structured-analysis {
	margin-top: 16px;
	padding: 16px;
	border: 1px solid rgba(143, 191, 159, 0.32);
	border-radius: 8px;
	background: #f7fbf7;
}

.analysis-heading {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 16px;

	span,
	strong {
		display: block;
	}

	span {
		color: #708071;
		font-size: 12px;
	}

	strong {
		margin-top: 5px;
		color: #42604d;
	}
}

.analysis-metrics,
.record-actions {
	display: flex;
	flex-wrap: wrap;
	gap: 8px;
}

.analysis-metrics span {
	padding: 6px 9px;
	border-radius: 999px;
	background: #e8f4ea;
	color: #50705b;
}

.patient-notes {
	display: grid;
	gap: 8px;
	margin-top: 14px;

	p {
		margin: 0;
		color: #62594f;
		line-height: 1.7;
	}

	span {
		margin-right: 10px;
		color: #8a654a;
		font-weight: 800;
	}
}

.emotion-results {
	display: grid;
	grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
	gap: 10px;
	margin-top: 14px;

	div {
		display: grid;
		gap: 4px;
		padding: 12px;
		border-radius: 8px;
		background: #ffffff;
	}

	strong {
		color: #58473c;
	}

	span {
		color: #7b7068;
		font-size: 12px;
	}
}

.event-details {
	display: grid;
	gap: 10px;
	margin-top: 14px;

	> div {
		padding: 12px;
		border-left: 3px solid #8fbf9f;
		border-radius: 6px;
		background: #ffffff;
	}

	> div > div {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 12px;
	}

	strong {
		color: #4d5f51;
	}

	span,
	p {
		color: #776e67;
		font-size: 12px;
	}

	p {
		margin: 8px 0 0;
		line-height: 1.7;
	}
}

.no-events {
	margin: 14px 0 0;
	color: #7b7068;
	line-height: 1.7;
}

.privacy {
	display: flex;
	flex-wrap: wrap;
	gap: 8px;

	span {
		padding: 6px 10px;
		border-radius: 999px;
		background: #f4e6d3;
		color: #75563f;
		font-size: 12px;
	}
}

.delete-btn {
	flex: 0 0 auto;
	padding: 9px 14px;
	background: #f8e9e3;
	color: #a45e58;
}

.detail-btn {
	padding: 9px 14px;
	background: #e8f4ea;
	color: #4e745a;
}

.pagination {
	justify-content: flex-end;
	margin-top: 18px;
}

@media (max-width: 1180px) {
	.summary-row,
	.reflection {
		grid-template-columns: 1fr 1fr;
	}

	.toolbar {
		grid-template-columns: 1fr;
	}
}

@media (max-width: 760px) {
	.awareness-shell {
		padding: 14px;
	}

	.hero,
	.analysis-heading,
	.record-foot {
		align-items: flex-start;
		flex-direction: column;
	}

	.summary-row,
	.reflection,
	.record-card {
		grid-template-columns: 1fr;
	}

	.record-time {
		padding-top: 0;
		text-align: left;
	}
}
</style>
