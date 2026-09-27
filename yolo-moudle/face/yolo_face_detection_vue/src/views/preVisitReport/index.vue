<template>
	<div class="report-page layout-padding">
		<div class="report-shell layout-padding-auto layout-padding-view">
			<div class="page-actions">
				<el-button plain @click="router.push('/trashRecords')">返回觉察记录</el-button>
				<div>
					<span>打印窗口中选择“另存为 PDF”，即可生成可携带的 PDF 文件。</span>
					<el-button type="primary" :disabled="!state.awareness || !state.analysis || state.loading" @click="printReport">打印 / 导出PDF</el-button>
				</div>
			</div>

			<section v-loading="state.loading" class="report-paper">
				<div v-if="state.error" class="report-error">
					<strong>暂时无法生成这份报告</strong>
					<p>{{ state.error }}</p>
					<el-button type="primary" @click="loadReport">重新加载</el-button>
				</div>

				<template v-else-if="state.awareness && state.analysis">
					<header class="report-header">
						<div>
							<p class="brand">心安动识 · 多模态心理预诊辅助平台</p>
							<h1>心理门诊预诊参考报告</h1>
							<span>情绪表情与 BFRB 行为线索结构化整理</span>
						</div>
						<div class="report-code">
							<span>报告编号</span>
							<strong>{{ reportNumber }}</strong>
							<small>生成时间：{{ formatDateTime(new Date().toISOString()) }}</small>
						</div>
					</header>

					<section class="notice">
						<strong>报告用途说明</strong>
						<p>本报告用于帮助患者向心理门诊医生回顾检测时段内的自述、情绪与动作线索，缩短基础信息整理时间。结果由算法辅助生成，不替代医生访谈、量表评估或临床诊断。</p>
					</section>

					<section class="report-section">
						<div class="section-title"><span>01</span><h2>本次预诊信息</h2></div>
						<div class="info-grid">
							<div><span>记录用户</span><strong>{{ state.awareness.username || '未填写' }}</strong></div>
							<div><span>检测来源</span><strong>{{ sourceName(state.analysis.record.sourceType || state.awareness.sourceType) }}</strong></div>
							<div><span>分析模式</span><strong>{{ analysisModeName(state.analysis.record.analysisMode) }}</strong></div>
							<div><span>检测时间</span><strong>{{ formatDateTime(state.analysis.record.detectedAt || state.awareness.createdAt) }}</strong></div>
							<div><span>情绪线索类型</span><strong>{{ state.analysis.emotionResults.length }} 类</strong></div>
							<div><span>BFRB连续事件</span><strong>{{ state.analysis.record.bfrbEventCount || 0 }} 次</strong></div>
						</div>
						<div class="patient-notes">
							<div><span>本次主诉</span><p>{{ state.analysis.record.complaint || '本次未填写主诉。' }}</p></div>
							<div><span>补充说明</span><p>{{ state.analysis.record.additionalNotes || '本次未填写补充说明。' }}</p></div>
						</div>
					</section>

					<section class="report-section">
						<div class="section-title"><span>02</span><h2>结构化检测摘要</h2></div>
						<div class="summary-box">
							<p>{{ structuredSummary }}</p>
							<p>{{ interpretationReminder }}</p>
						</div>
						<div class="metric-row">
							<div><strong>{{ dominantEmotion?.emotionType || '未形成' }}</strong><span>主要情绪线索</span></div>
							<div><strong>{{ dominantEmotion ? confidenceText(dominantEmotion.averageConfidence) : '—' }}</strong><span>主要线索平均置信度</span></div>
							<div><strong>{{ state.analysis.record.bfrbEventCount || 0 }}</strong><span>BFRB事件总数</span></div>
							<div><strong>{{ numberText(state.analysis.record.bfrbTotalDurationSeconds) }}s</strong><span>BFRB累计持续</span></div>
						</div>
					</section>

					<section class="report-section">
						<div class="section-title"><span>03</span><h2>情绪线索明细</h2></div>
						<div v-if="state.analysis.emotionResults.length" class="emotion-grid">
							<div v-for="emotion in sortedEmotions" :key="emotion.id || emotion.emotionType" class="emotion-card">
								<strong>{{ emotion.emotionType }}</strong>
								<span>{{ emotion.frameCount || 0 }} 次有效采样</span>
								<div><em :style="{ width: confidenceText(emotion.averageConfidence) }"></em></div>
								<small>平均 {{ confidenceText(emotion.averageConfidence) }} · 最高 {{ confidenceText(emotion.maxConfidence) }}</small>
							</div>
						</div>
						<p v-else class="empty-note">本次分析没有保存可展示的情绪分类明细。</p>
					</section>

					<section class="report-section">
						<div class="section-title"><span>04</span><h2>BFRB行为与证据明细</h2></div>
						<div v-if="behaviorSummary.length" class="behavior-summary">
							<div v-for="item in behaviorSummary" :key="item.key">
								<strong>{{ item.label }}</strong>
								<span>{{ item.count }} 次 · 累计 {{ numberText(item.duration) }} 秒</span>
							</div>
						</div>
						<div v-if="state.analysis.bfrbEvents.length" class="event-table-wrap">
							<table class="event-table">
								<thead><tr><th>行为线索</th><th>起止时间</th><th>持续</th><th>平均/最高置信度</th><th>关键帧</th><th>证据类型</th></tr></thead>
								<tbody>
									<tr v-for="event in state.analysis.bfrbEvents" :key="event.id || event.eventKey">
										<td>{{ event.cueType || event.behaviorCode || '未分类行为' }}</td>
										<td>{{ numberText(event.startSeconds) }}s–{{ numberText(event.endSeconds) }}s</td>
										<td>{{ numberText(event.durationSeconds) }}s</td>
										<td>{{ confidenceText(event.averageConfidence) }} / {{ confidenceText(event.maxConfidence) }}</td>
										<td>{{ numberText(event.keyFrameSeconds) }}s</td>
										<td>{{ evidenceName(event.evidenceType) }}</td>
									</tr>
								</tbody>
							</table>
						</div>
						<p v-else class="empty-note">本次没有形成满足连续性阈值的 BFRB 事件。零散命中未被重复计数，这也不代表相关行为一定不存在。</p>
					</section>

					<section class="report-section">
						<div class="section-title"><span>05</span><h2>供门诊沟通的参考要点</h2></div>
						<div class="handoff-grid">
							<div><span>来访者自述</span><p>{{ state.analysis.record.complaint || '建议在就诊时补充近期主要困扰、出现频率及持续时间。' }}</p></div>
							<div><span>可进一步核实</span><p>{{ clinicianPrompt }}</p></div>
							<div><span>觉察与照护提示</span><p>{{ state.awareness.suggestedPractice || '可在专业人员指导下进一步评估情绪、睡眠、压力事件及行为发生情境。' }}</p></div>
						</div>
					</section>

					<div v-if="!state.aiSummary" class="ai-generate-panel">
						<div>
							<strong>需要一份更便于门诊沟通的文字整理？</strong>
							<p>可在确认后生成 AI 辅助摘要。只整理已保存的主诉与去标识化检测统计，不输出诊断、严重程度或用药建议。</p>
						</div>
						<el-button type="primary" :loading="state.aiLoading" @click="generateAiSummary">生成AI辅助摘要</el-button>
					</div>

					<section v-else class="report-section ai-summary-section">
						<div class="section-title"><span>06</span><h2>AI辅助整理</h2></div>
						<div class="ai-summary-meta">
							<strong>{{ providerName(state.aiSummary.provider) }}</strong>
							<span>生成于 {{ formatDateTime(state.aiSummary.generatedAt) }}</span>
							<span>模型：{{ state.aiSummary.modelName }}</span>
							<span>规则版本：{{ state.aiSummary.promptVersion }}</span>
						</div>
						<div class="ai-objective-summary">
							<span>客观摘要</span>
							<p>{{ state.aiSummary.objectiveSummary }}</p>
						</div>
						<div class="ai-list-grid">
							<div>
								<strong>可与医生进一步讨论</strong>
								<ul><li v-for="item in state.aiSummary.clinicianQuestions" :key="item">{{ item }}</li></ul>
							</div>
							<div>
								<strong>就诊前可准备</strong>
								<ul><li v-for="item in state.aiSummary.visitPreparation" :key="item">{{ item }}</li></ul>
							</div>
						</div>
						<p class="ai-safety-note">{{ state.aiSummary.safetyNote }}</p>
					</section>

					<footer class="report-footer">
						<strong>重要声明</strong>
						<p>本报告仅供心理门诊预诊沟通与健康教育参考，不构成医学诊断、疾病筛查结论或治疗建议。模型结果可能受到光照、角度、遮挡、素材质量和个体差异影响，请由具备资质的专业人员结合访谈、量表及其他检查综合判断。如存在自伤、自杀或伤害他人的想法，请立即联系当地急救、警方或可信赖的专业支持。</p>
						<div><span>素材保留选择：{{ state.analysis.record.keepMedia ? '已选择保留素材路径' : '未选择保留素材路径' }}</span><span>报告数据来源：心安动识本地结构化检测记录</span></div>
					</footer>
				</template>

				<div v-else-if="!state.loading" class="report-error">
					<strong>没有找到报告数据</strong>
					<p>请返回觉察记录，选择一条已经保存完整分析的记录。</p>
				</div>
			</section>
		</div>
	</div>
</template>

<script setup lang="ts" name="PreVisitReport">
import { computed, onMounted, reactive } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
	generatePreVisitReportSummary,
	getAnalysisRecordByAwareness,
	getAwarenessRecord,
	getPreVisitReportSummary,
} from '/@/api/healing';

type AwarenessRecord = {
	id: number;
	username?: string;
	sourceType?: string;
	emotionLabel?: string;
	confidence?: string;
	bodySignal?: string;
	gentleSummary?: string;
	suggestedPractice?: string;
	createdAt?: string;
};

type EmotionResult = {
	id?: number;
	emotionType: string;
	frameCount?: number;
	averageConfidence?: number;
	maxConfidence?: number;
};

type BfrbEvent = {
	id?: number;
	eventKey?: string;
	behaviorCode?: string;
	cueType?: string;
	startSeconds?: number;
	endSeconds?: number;
	durationSeconds?: number;
	averageConfidence?: number;
	maxConfidence?: number;
	keyFrameSeconds?: number;
	evidenceType?: string;
};

type AnalysisDetail = {
	record: {
		id: number;
		sourceType?: string;
		analysisMode?: string;
		modelConfiguration?: string;
		bfrbEventCount?: number;
		bfrbTotalDurationSeconds?: number;
		complaint?: string;
		additionalNotes?: string;
		keepMedia?: boolean;
		detectedAt?: string;
	};
	emotionResults: EmotionResult[];
	bfrbEvents: BfrbEvent[];
	behaviorStats?: Array<Record<string, any>>;
};

type AiSummary = {
	id: number;
	provider: string;
	modelName: string;
	promptVersion: string;
	objectiveSummary: string;
	clinicianQuestions: string[];
	visitPreparation: string[];
	safetyNote: string;
	generatedAt?: string;
	cached?: boolean;
};

const route = useRoute();
const router = useRouter();
const state = reactive({
	loading: false,
	error: '',
	awareness: null as AwarenessRecord | null,
	analysis: null as AnalysisDetail | null,
	aiSummary: null as AiSummary | null,
	aiLoading: false,
});

const awarenessRecordId = computed(() => Number(route.params.awarenessRecordId || 0));

const sortedEmotions = computed(() => {
	return [...(state.analysis?.emotionResults || [])].sort((a, b) => {
		return Number(b.frameCount || 0) - Number(a.frameCount || 0) || Number(b.averageConfidence || 0) - Number(a.averageConfidence || 0);
	});
});

const dominantEmotion = computed(() => sortedEmotions.value[0]);

const behaviorSummary = computed(() => {
	const grouped = new Map<string, { key: string; label: string; count: number; duration: number }>();
	for (const event of state.analysis?.bfrbEvents || []) {
		const key = event.behaviorCode || event.cueType || 'unknown';
		const current = grouped.get(key) || { key, label: event.cueType || event.behaviorCode || '未分类行为', count: 0, duration: 0 };
		current.count += 1;
		current.duration += Number(event.durationSeconds || 0);
		grouped.set(key, current);
	}
	return [...grouped.values()].sort((a, b) => b.count - a.count || b.duration - a.duration);
});

const reportNumber = computed(() => {
	const rawDate = state.analysis?.record.detectedAt || state.awareness?.createdAt || '';
	const date = rawDate ? rawDate.slice(0, 10).replaceAll('-', '') : 'UNKNOWN';
	return `XA-${date}-${String(awarenessRecordId.value).padStart(6, '0')}`;
});

const structuredSummary = computed(() => {
	if (!state.analysis) return '';
	const emotionText = dominantEmotion.value
		? `主要情绪线索为“${dominantEmotion.value.emotionType}”，共记录 ${dominantEmotion.value.frameCount || 0} 次有效采样，平均置信度 ${confidenceText(dominantEmotion.value.averageConfidence)}。`
		: '本次未保存可展示的情绪分类明细。';
	const eventCount = Number(state.analysis.record.bfrbEventCount || 0);
	const bfrbText = eventCount
		? `检测时段内形成 ${eventCount} 个满足连续性规则的 BFRB 行为事件，累计持续 ${numberText(state.analysis.record.bfrbTotalDurationSeconds)} 秒。`
		: '检测时段内没有形成满足连续性规则的 BFRB 行为事件。';
	return `${emotionText}${bfrbText}`;
});

const interpretationReminder = computed(() => {
	if (!state.analysis) return '';
	return Number(state.analysis.record.bfrbEventCount || 0) > 0
		? '上述动作是特定时段内的算法观察线索，需要结合压力情境、主观体验、出现频率及功能影响由医生进一步核实。'
		: '未形成连续事件不等同于排除相关行为，可能受到观察时长、画面质量、遮挡和阈值设置影响。';
});

const clinicianPrompt = computed(() => {
	if (!state.analysis) return '';
	const leadingBehavior = behaviorSummary.value[0];
	if (leadingBehavior) {
		return `可重点询问“${leadingBehavior.label}”出现前后的压力事件、主观冲动、可控程度、身体损伤情况，以及是否影响学习、工作、睡眠或人际交往。`;
	}
	return '建议进一步询问情绪变化、睡眠、近期压力事件、重复性身体行为以及对学习、工作和日常功能的影响。';
});

const loadExistingAiSummary = async () => {
	state.aiSummary = null;
	try {
		const response = await getPreVisitReportSummary(awarenessRecordId.value);
		if ((response.code === '0' || response.code === 0) && response.data) {
			state.aiSummary = response.data as AiSummary;
		}
	} catch (error) {
		// 尚未生成摘要或后端暂不可用时，仍正常展示原始预诊报告。
	}
};

const loadReport = async () => {
	state.loading = true;
	state.error = '';
	state.awareness = null;
	state.analysis = null;
	state.aiSummary = null;
	if (!awarenessRecordId.value) {
		state.error = '报告编号无效，请从觉察记录重新进入。';
		state.loading = false;
		return;
	}
	try {
		const [awarenessRes, analysisRes] = await Promise.all([
			getAwarenessRecord(awarenessRecordId.value),
			getAnalysisRecordByAwareness(awarenessRecordId.value),
		]);
		if ((awarenessRes.code !== '0' && awarenessRes.code !== 0) || !awarenessRes.data) {
			state.error = '未找到对应的觉察记录，它可能已经被删除。';
			return;
		}
		if (analysisRes.code !== '0' && analysisRes.code !== 0) {
			state.error = '这是一条旧版觉察记录，尚未关联完整结构化分析，因此不能生成可靠的预诊报告。';
			return;
		}
		state.awareness = awarenessRes.data as AwarenessRecord;
		state.analysis = analysisRes.data as AnalysisDetail;
		await loadExistingAiSummary();
	} catch (error) {
		state.error = '报告数据暂时没有连接成功，请稍后重试。';
	} finally {
		state.loading = false;
	}
};

const generateAiSummary = async () => {
	if (!state.awareness || !state.analysis || state.aiLoading) return;
	try {
		await ElMessageBox.confirm(
			'系统将把本次主诉、补充说明和去标识化检测统计交给后端配置的 AI 服务进行文字整理；不会发送用户名或素材路径。AI 内容不构成诊断。是否继续？',
			'生成AI辅助摘要',
			{ confirmButtonText: '确认生成', cancelButtonText: '暂不生成', type: 'warning' }
		);
	} catch (error) {
		return;
	}
	state.aiLoading = true;
	try {
		const response = await generatePreVisitReportSummary(awarenessRecordId.value);
		if ((response.code === '0' || response.code === 0) && response.data) {
			state.aiSummary = response.data as AiSummary;
			ElMessage.success(state.aiSummary.provider === 'deepseek' ? 'AI辅助摘要已生成并保存' : '已生成并保存本地规则备用摘要');
			return;
		}
		ElMessage.error(response.msg || 'AI辅助摘要生成失败，请稍后重试');
	} catch (error) {
		ElMessage.error('AI辅助摘要暂时没有生成成功，原报告不受影响');
	} finally {
		state.aiLoading = false;
	}
};

const sourceName = (sourceType?: string) => {
	const labels: Record<string, string> = { image: '图片觉察', video: '视频觉察', camera: '摄像头实时觉察' };
	return labels[sourceType || ''] || '未标明来源';
};

const analysisModeName = (mode?: string) => {
	const labels: Record<string, string> = {
		combined: '综合分析（情绪 + BFRB双证据）',
		emotion: '情绪表情识别',
		bfrb_behavior: 'BFRB行为直检',
		bfrb_geometry: 'BFRB手脸几何',
	};
	return labels[mode || ''] || '结构化检测';
};

const evidenceName = (evidence?: string) => {
	if (evidence === 'behavior-model+hand-face-geometry') return '行为模型 + 手脸几何';
	if (evidence === 'hand-face-geometry') return '手脸几何';
	if (evidence === 'behavior-model') return 'BFRB行为模型';
	return '辅助线索';
};

const providerName = (provider?: string) => {
	return provider === 'deepseek' ? 'DeepSeek 辅助整理' : '本地规则备用摘要';
};

const confidenceText = (value?: number) => `${Math.round(Number(value || 0) * 1000) / 10}%`;
const numberText = (value?: number) => String(Math.round(Number(value || 0) * 100) / 100);

const formatDateTime = (value?: string) => {
	if (!value) return '未记录';
	const date = new Date(value);
	if (Number.isNaN(date.getTime())) return value;
	return date.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
};

const printReport = () => {
	if (!state.awareness || !state.analysis) return;
	const originalTitle = document.title;
	document.title = `心安动识预诊参考报告-${reportNumber.value}`;
	document.documentElement.classList.add('previsit-printing');
	document.body.classList.add('previsit-printing');
	const restoreTitle = () => {
		document.title = originalTitle;
		document.documentElement.classList.remove('previsit-printing');
		document.body.classList.remove('previsit-printing');
		window.removeEventListener('afterprint', restoreTitle);
	};
	window.addEventListener('afterprint', restoreTitle);
	window.print();
	window.setTimeout(restoreTitle, 1000);
};

onMounted(loadReport);
</script>

<style scoped lang="scss">
.report-page {
	min-height: 100%;
	background: #f2eee8;
	color: #393631;
	overflow: visible;
}

.report-page.layout-padding {
	position: relative;
	display: block;
	height: auto;
	min-height: 100%;
	overflow: visible;
}

.report-shell {
	max-width: 1180px;
	padding: 24px;
	min-height: 100%;
}

.report-shell.layout-padding-auto,
.report-shell.layout-padding-view {
	display: block;
	height: auto;
	min-height: 100%;
	overflow: visible;
}

.page-actions {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16px;
	margin-bottom: 16px;

	> div { display: flex; align-items: center; gap: 12px; }
	span { color: #887d71; font-size: 13px; }
}

.report-paper {
	min-height: 600px;
	padding: 46px 54px;
	border: 1px solid #e3d8ca;
	background: #fffefa;
	box-shadow: 0 16px 48px rgba(67, 55, 42, 0.09);
}

.report-header {
	display: flex;
	justify-content: space-between;
	gap: 32px;
	padding-bottom: 28px;
	border-bottom: 2px solid #72553d;

	.brand { margin: 0 0 10px; color: #a57148; font-weight: 800; letter-spacing: 0.06em; }
	h1 { margin: 0; color: #3e3026; font-size: 34px; letter-spacing: 0.08em; }
	span { display: block; margin-top: 10px; color: #756a60; }
}

.report-code {
	display: grid;
	align-content: start;
	justify-items: end;
	gap: 6px;
	text-align: right;

	span, small { margin: 0; color: #897d71; }
	strong { color: #72553d; font-size: 18px; }
}

.notice {
	margin: 24px 0;
	padding: 16px 18px;
	border-left: 4px solid #8fbf9f;
	background: #f1f7f2;

	strong { color: #456451; }
	p { margin: 7px 0 0; color: #5e6b61; line-height: 1.8; }
}

.report-section {
	padding: 26px 0;
	border-bottom: 1px solid #ece4da;
}

.section-title {
	display: flex;
	align-items: center;
	gap: 12px;
	margin-bottom: 18px;

	span { display: grid; width: 34px; height: 34px; place-items: center; border-radius: 50%; background: #72553d; color: #fff; font-size: 12px; font-weight: 900; }
	h2 { margin: 0; color: #49382c; font-size: 20px; }
}

.info-grid {
	display: grid;
	grid-template-columns: repeat(3, minmax(0, 1fr));
	border: 1px solid #e9dfd4;
	border-radius: 8px;
	overflow: hidden;

	div { display: grid; gap: 6px; padding: 15px; border-right: 1px solid #eee5db; border-bottom: 1px solid #eee5db; }
	span { color: #8b7b6c; font-size: 12px; }
	strong { color: #514438; }
}

.patient-notes, .handoff-grid {
	display: grid;
	gap: 12px;
	margin-top: 14px;

	div { padding: 15px 17px; border-radius: 8px; background: #fbf7f1; }
	span { color: #9a6948; font-size: 13px; font-weight: 900; }
	p { margin: 7px 0 0; color: #5f554d; line-height: 1.8; white-space: pre-wrap; }
}

.summary-box {
	padding: 18px;
	border-radius: 8px;
	background: #f7f3ed;

	p { margin: 0; color: #574d45; line-height: 1.9; }
	p + p { margin-top: 8px; color: #817368; }
}

.metric-row {
	display: grid;
	grid-template-columns: repeat(4, minmax(0, 1fr));
	gap: 12px;
	margin-top: 14px;

	div { padding: 16px; border: 1px solid #e6dccf; border-radius: 8px; text-align: center; }
	strong, span { display: block; }
	strong { color: #6d4d36; font-size: 22px; }
	span { margin-top: 6px; color: #86796e; font-size: 12px; }
}

.emotion-grid {
	display: grid;
	grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
	gap: 12px;
}

.emotion-card {
	display: grid;
	gap: 7px;
	padding: 15px;
	border: 1px solid #e8ded2;
	border-radius: 8px;

	strong { color: #514237; }
	span, small { color: #82766c; }
	div { height: 7px; border-radius: 99px; background: #eee7df; overflow: hidden; }
	em { display: block; height: 100%; border-radius: inherit; background: #8fbf9f; }
}

.behavior-summary {
	display: flex;
	flex-wrap: wrap;
	gap: 10px;
	margin-bottom: 14px;

	div { padding: 10px 14px; border-radius: 8px; background: #edf6ef; }
	strong { color: #456451; }
	span { margin-left: 10px; color: #6d7b70; font-size: 12px; }
}

.event-table-wrap { overflow-x: auto; }
.event-table {
	width: 100%;
	border-collapse: collapse;
	font-size: 13px;

	th, td { padding: 11px 9px; border: 1px solid #e6ded5; text-align: left; }
	th { background: #f7f2eb; color: #6a584a; white-space: nowrap; }
	td { color: #5f5750; }
}

.empty-note {
	margin: 0;
	padding: 15px;
	border-radius: 8px;
	background: #f7f3ed;
	color: #776d64;
	line-height: 1.8;
}

.ai-generate-panel {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 20px;
	margin-top: 24px;
	padding: 18px 20px;
	border: 1px solid #d8e6dc;
	border-radius: 10px;
	background: #f3f8f4;

	strong { color: #456451; }
	p { margin: 7px 0 0; color: #68766b; line-height: 1.7; }
	.el-button { flex: 0 0 auto; }
}

.ai-summary-meta {
	display: flex;
	flex-wrap: wrap;
	gap: 8px 16px;
	margin-bottom: 14px;
	color: #80756b;
	font-size: 12px;

	strong { color: #456451; font-size: 14px; }
}

.ai-objective-summary {
	padding: 17px 18px;
	border-left: 4px solid #8fbf9f;
	background: #f3f8f4;

	span { color: #456451; font-size: 13px; font-weight: 900; }
	p { margin: 8px 0 0; color: #56635a; line-height: 1.85; }
}

.ai-list-grid {
	display: grid;
	grid-template-columns: 1fr 1fr;
	gap: 14px;
	margin-top: 14px;

	> div { padding: 16px 18px; border: 1px solid #e5ddd2; border-radius: 8px; }
	strong { color: #6d4d36; }
	ul { margin: 10px 0 0; padding-left: 20px; }
	li { margin: 6px 0; color: #60574f; line-height: 1.7; }
}

.ai-safety-note {
	margin: 14px 0 0;
	padding: 12px 14px;
	border-radius: 8px;
	background: #fff7f5;
	color: #80625f;
	font-size: 12px;
	line-height: 1.7;
}

.report-footer {
	margin-top: 28px;
	padding: 20px;
	border: 1px solid #e7c8c4;
	border-radius: 8px;
	background: #fff7f5;

	strong { color: #9c514d; }
	p { margin: 8px 0 14px; color: #6d5b59; line-height: 1.85; }
	div { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 8px 20px; color: #8a7774; font-size: 12px; }
}

.report-error {
	display: grid;
	place-items: center;
	min-height: 420px;
	text-align: center;

	strong { color: #6d4d36; font-size: 22px; }
	p { max-width: 560px; color: #7d7167; line-height: 1.8; }
}

@media (max-width: 820px) {
	.report-paper { padding: 26px 20px; }
	.report-header, .page-actions, .page-actions > div, .ai-generate-panel { align-items: flex-start; flex-direction: column; }
	.report-code { justify-items: start; text-align: left; }
	.info-grid, .metric-row, .ai-list-grid { grid-template-columns: 1fr 1fr; }
}

@media (max-width: 520px) {
	.report-shell { padding: 12px; }
	.report-header h1 { font-size: 26px; }
	.info-grid, .metric-row, .ai-list-grid { grid-template-columns: 1fr; }
}
</style>

<style lang="scss">
@page {
	size: A4 portrait;
	margin: 12mm;
}

@media print {
	html.previsit-printing,
	body.previsit-printing,
	body.previsit-printing #app {
		width: auto !important;
		height: auto !important;
		min-height: 0 !important;
		overflow: visible !important;
		background: #ffffff !important;
	}

	body.previsit-printing {
		-webkit-print-color-adjust: exact;
		print-color-adjust: exact;
	}

	.previsit-printing .layout-aside,
	.previsit-printing .layout-header,
	.previsit-printing .layout-navbars-container,
	.previsit-printing .layout-footer,
	.previsit-printing .el-backtop,
	.previsit-printing .page-actions,
	.previsit-printing .ai-generate-panel {
		display: none !important;
	}

	.previsit-printing .layout-container,
	.previsit-printing .layout-container-view,
	.previsit-printing .layout-main,
	.previsit-printing .layout-main-scroll,
	.previsit-printing .layout-parent,
	.previsit-printing .report-page,
	.previsit-printing .report-shell {
		position: static !important;
		display: block !important;
		width: 100% !important;
		height: auto !important;
		min-height: 0 !important;
		padding: 0 !important;
		margin: 0 !important;
		overflow: visible !important;
		background: #ffffff !important;
		border: 0 !important;
	}

	.previsit-printing .el-scrollbar__wrap,
	.previsit-printing .el-scrollbar__view {
		position: static !important;
		height: auto !important;
		max-height: none !important;
		overflow: visible !important;
	}

	.previsit-printing .el-scrollbar__bar {
		display: none !important;
	}

	.previsit-printing .report-paper {
		width: 100% !important;
		min-height: 0 !important;
		padding: 0 !important;
		border: 0 !important;
		box-shadow: none !important;
		background: #ffffff !important;
		font-size: 10.5pt;
	}

	.previsit-printing .report-header {
		padding-bottom: 16px !important;
		break-after: avoid-page;
	}

	.previsit-printing .report-header h1 {
		font-size: 24pt !important;
	}

	.previsit-printing .notice,
	.previsit-printing .summary-box,
	.previsit-printing .metric-row,
	.previsit-printing .emotion-card,
	.previsit-printing .behavior-summary,
	.previsit-printing .patient-notes > div,
	.previsit-printing .handoff-grid > div,
	.previsit-printing .ai-objective-summary,
	.previsit-printing .ai-list-grid > div,
	.previsit-printing .ai-safety-note,
	.previsit-printing .report-footer {
		break-inside: avoid;
	}

	.previsit-printing .report-section {
		padding: 16px 0 !important;
		break-inside: auto;
	}

	.previsit-printing .section-title {
		margin-bottom: 10px !important;
		break-after: avoid-page;
	}

	.previsit-printing .info-grid,
	.previsit-printing .metric-row {
		grid-template-columns: repeat(3, minmax(0, 1fr)) !important;
	}

	.previsit-printing .event-table {
		font-size: 8.5pt !important;
	}

	.previsit-printing .event-table thead {
		display: table-header-group;
	}

	.previsit-printing .event-table tr {
		break-inside: avoid;
	}

	.previsit-printing .event-table th,
	.previsit-printing .event-table td {
		padding: 6px 5px !important;
	}

	.previsit-printing .report-footer {
		margin-top: 18px !important;
	}
}
</style>
