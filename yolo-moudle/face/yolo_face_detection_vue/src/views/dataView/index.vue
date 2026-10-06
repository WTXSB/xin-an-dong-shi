<template>
	<div class="emotion-page">
		<header class="page-heading">
			<div><span class="eyebrow">EMOTION DIARY</span><h1>情绪日记</h1></div>
			<p>轻轻回望今天被记录的情绪，让觉察成为照顾自己的开始。</p>

			<HealingIllustratedFrame variant="clouds" />
			</header>
			<HealingDecorationStrip variant="sky" />
		<EmotionDiary />


		<section class="stat-grid">
			<article v-for="item in statistics" :key="item.label" class="stat-card" :class="item.tone">
				<div class="stat-icon animal-emotion" aria-hidden="true"><el-icon><component :is="item.icon" /></el-icon></div>
				<div><span>{{ item.label }}</span><strong>{{ item.value }}<small> 次</small></strong></div>
			</article>
		</section>
			<HealingDecorationStrip variant="garden" />
		<AnimalEmotionGallery />


		<section class="records-card">
			<div class="card-heading">
				<div><h2>表情识别记录</h2><p>最近的感知记录会在这里安静地汇集。</p></div>
				<el-button plain round @click="refreshData"><el-icon><ele-Refresh /></el-icon>刷新</el-button>
			</div>
			<el-table :data="emotionRecords" class="records-table" empty-text="还没有识别记录">
				<el-table-column prop="recognitionTime" label="识别时间" min-width="170" />
				<el-table-column prop="emotionType" label="情绪类型" min-width="120">
					<template #default="scope"><el-tag :type="getEmotionTagType(scope.row.emotionType)" effect="light" round>{{ scope.row.emotionType }}</el-tag></template>
				</el-table-column>
				<el-table-column prop="confidence" label="感知可信度" min-width="210">
					<template #default="scope"><div class="confidence"><el-progress :percentage="scope.row.confidence" :stroke-width="8" :show-text="false" color="#80aa8a" /><span>{{ scope.row.confidence }}%</span></div></template>
				</el-table-column>
				<el-table-column prop="source" label="识别来源" min-width="150" />
			</el-table>

			<HealingIllustratedFrame variant="garden" />
			</section>

		<HealingDecorationStrip variant="rest" />
	</div>
</template>

<script setup lang="ts" name="dataView">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import request from '/@/utils/request';
import EmotionDiary from '/@/components/emotionDiary/EmotionDiary.vue';

interface EmotionRecord {
	id: number | string;
	recognitionTime: string;
	emotionType: string;
	confidence: number;
	source: string;
}

const totalRecognitions = ref(0);
const positiveEmotions = ref(0);
const negativeEmotions = ref(0);
const neutralEmotions = ref(0);
const emotionRecords = ref<EmotionRecord[]>([]);
let refreshTimer: ReturnType<typeof setInterval> | undefined;

const statistics = computed(() => [
	{ label: '今日识别总量', value: totalRecognitions.value, tone: 'cream', icon: 'ele-Sunny' },
	{ label: '积极情绪', value: positiveEmotions.value, tone: 'green', icon: 'ele-CircleCheck' },
	{ label: '消极情绪', value: negativeEmotions.value, tone: 'peach', icon: 'ele-Cloudy' },
	{ label: '中性情绪', value: neutralEmotions.value, tone: 'blue', icon: 'ele-MoonNight' },
]);

const getEmotionTagType = (emotionType: string) => {
	const map: Record<string, '' | 'success' | 'info' | 'warning' | 'danger'> = {
		高兴: 'success', 中性: '', 悲伤: 'info', 愤怒: 'danger', 厌恶: 'danger', 恐惧: 'warning', 惊讶: 'warning',
	};
	return map[emotionType] || 'info';
};

const getTodayStatistics = async () => {
	try {
		const res = await request.get('/api/emotion/today');
		if (res.code == 0) {
			totalRecognitions.value = res.data?.total_recognitions || 0;
			positiveEmotions.value = res.data?.positive_emotions || 0;
			negativeEmotions.value = res.data?.negative_emotions || 0;
			neutralEmotions.value = res.data?.neutral_emotions || 0;
		}
	} catch (error) {
		console.warn('今日情绪统计暂时无法加载', error);
	}
};

const getEmotionRecords = async () => {
	try {
		const res = await request.get('/api/imgRecords', { params: { search: '', pageNum: 1, pageSize: 10 } });
		if (res.code != 0) return;
		const emotionMap: Record<string, string> = {
			happy: '高兴', neutral: '中性', sad: '悲伤', angry: '愤怒', disgust: '厌恶', fear: '恐惧', surprise: '惊讶',
		};
		emotionRecords.value = (res.data?.records || []).slice(0, 10).map((record: any) => {
			let emotionType = '未知';
			let confidence = 0;
			try {
				const labels = record.label ? JSON.parse(record.label) : [];
				const confidences = record.confidence ? JSON.parse(record.confidence) : [];
				if (labels.length) emotionType = emotionMap[labels[0]] || labels[0];
				if (confidences.length) confidence = Math.round(Number(confidences[0]) * 100);
			} catch (_) {
				// 旧记录格式异常时仍保留该行。
			}
			return { id: record.id, recognitionTime: record.startTime, emotionType, confidence, source: `${record.username || '用户'} (${record.kind || '图片'})` };
		});
	} catch (error) {
		console.warn('表情识别记录暂时无法加载', error);
	}
};

const refreshData = () => Promise.all([getTodayStatistics(), getEmotionRecords()]);
onMounted(() => {
	refreshData();
	refreshTimer = setInterval(refreshData, 300000);
});
onUnmounted(() => refreshTimer && clearInterval(refreshTimer));
</script>

<style scoped lang="scss">
.emotion-page { min-height: 100%; padding: 34px clamp(18px, 3vw, 48px) 48px; background: radial-gradient(circle at 92% 7%, rgba(255, 219, 119, .2), transparent 24%), linear-gradient(145deg, #fffdf7 0%, #fff8e9 58%, #f7f5e9 100%); color: #5b4437; }
.page-heading { display: flex; align-items: end; justify-content: space-between; gap: 24px; margin: 0 auto 28px; max-width: 1280px; }
.page-heading h1 { margin: 3px 0 0; font-family: 'MindEase Art'; font-size: clamp(34px, 4vw, 48px); font-weight: 400; color: #4d392d; }
.page-heading p { max-width: 460px; margin: 0 0 5px; color: #8d7d70; line-height: 1.8; text-align: right; }
.eyebrow { color: #c17043; font-size: 12px; letter-spacing: .16em; }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 18px; max-width: 1280px; margin: 0 auto 24px; }
.stat-card { display: flex; align-items: center; gap: 17px; min-height: 112px; padding: 22px; border: 1px solid rgba(161, 109, 62, .14); border-radius: 24px; background: rgba(255, 254, 249, .92); box-shadow: 0 14px 36px rgba(136, 88, 42, .09); }
.stat-icon { display: grid; place-items: center; width: 50px; height: 50px; flex: 0 0 auto; border-radius: 17px; font-size: 23px; }
.stat-card span { display: block; margin-bottom: 7px; color: #938579; font-size: 14px; }
.stat-card strong { display: block; color: #51463e; font-size: 27px; }
.stat-card small { color: #9b8e83; font-size: 13px; font-weight: 400; }
.cream .stat-icon { background: #fff0c4; color: #bc753d; } .green .stat-icon { background: #eef3e5; color: #71875f; }
.peach .stat-icon { background: #fae5d8; color: #bd7154; } .blue .stat-icon { background: #f4edda; color: #8e765d; }
.records-card { max-width: 1280px; margin: 0 auto; padding: 26px 28px 20px; border: 1px solid rgba(161, 109, 62, .14); border-radius: 28px; background: rgba(255, 254, 249, .94); box-shadow: 0 18px 46px rgba(136, 88, 42, .1); }
.card-heading { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 18px; }
.card-heading h2 { margin: 0 0 5px; color: #5b6558; font-size: 22px; }
.card-heading p { margin: 0; color: #a09287; }
.confidence { display: flex; align-items: center; gap: 12px; }.confidence .el-progress { width: 125px; }.confidence span { margin: 0; color: #6a776b; }
.records-table { --el-table-header-bg-color: #fff4d5; --el-table-row-hover-bg-color: #fff9e9; --el-table-border-color: transparent; color: #635047; }
@media (max-width: 900px) { .stat-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 600px) {
	.emotion-page { padding: 24px 14px 34px; }
	.page-heading { align-items: flex-start; flex-direction: column; gap: 8px; }
	.page-heading p { text-align: left; }
	.stat-grid { grid-template-columns: 1fr 1fr; gap: 10px; }
	.stat-card { min-height: 94px; padding: 14px; gap: 11px; border-radius: 17px; }
	.stat-icon { width: 40px; height: 40px; border-radius: 13px; }
	.stat-card strong { font-size: 23px; }
	.records-card { padding: 20px 14px; border-radius: 20px; overflow: hidden; }
	.card-heading { align-items: flex-start; }
}
</style>
