<template>
	<section class="trend-card">
		<div class="trend-heading">
			<div>
				<span class="trend-eyebrow">A GENTLE LOOK BACK</span>
				<h2>近 10 天表情识别趋势</h2>
				<p>趋势只是帮助你理解自己的线索，不是对情绪的评判。</p>
			</div>
			<el-button plain round :loading="loading" @click="loadTrend"><el-icon><ele-Refresh /></el-icon>更新趋势</el-button>
		</div>
		<div ref="chartEl" class="trend-chart" role="img" aria-label="近十天总识别次数、积极情绪与消极情绪折线图"></div>
		<div v-if="consecutiveNegativeDays >= 3" class="gentle-care">
			<div class="care-icon"><el-icon><ele-Umbrella /></el-icon></div>
			<div class="care-copy">
				<strong>最近几天似乎有些不容易</strong>
				<p>我们留意到连续 {{ consecutiveNegativeDays }} 天消极情绪相对较多。无需急着改变，先找个安全的地方说一说也很好。</p>
			</div>
			<el-button class="chat-button" round @click="router.push('/smartChat')">和安小宁聊聊</el-button>
		</div>
	</section>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import * as echarts from 'echarts/core';
import { LineChart } from 'echarts/charts';
import { TooltipComponent, GridComponent, LegendComponent, DataZoomComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import request from '/@/utils/request';

echarts.use([LineChart, TooltipComponent, GridComponent, LegendComponent, DataZoomComponent, CanvasRenderer]);

const router = useRouter();
const chartEl = ref<HTMLElement>();
const loading = ref(false);
const consecutiveNegativeDays = ref(0);
let chart: echarts.ECharts | undefined;
let refreshTimer: ReturnType<typeof setInterval> | undefined;

const renderChart = (dates: string[], total: number[], positive: number[], negative: number[]) => {
	if (!chartEl.value) return;
	chart ||= echarts.init(chartEl.value);
	chart.setOption({
		animationDuration: 900,
		color: ['#b69a72', '#77a581', '#d48c79'],
		tooltip: { trigger: 'axis', backgroundColor: 'rgba(255,253,248,.96)', borderColor: '#dfe7dc', textStyle: { color: '#5c5148' } },
		legend: { top: 4, right: 10, itemWidth: 18, itemHeight: 8, textStyle: { color: '#776c63', fontFamily: 'MindEase WenKai' } },
		grid: { left: 24, right: 22, top: 54, bottom: 34, containLabel: true },
		xAxis: { type: 'category', boundaryGap: false, data: dates, axisLine: { lineStyle: { color: '#d9dfd5' } }, axisLabel: { color: '#8c8279' } },
		yAxis: { type: 'value', minInterval: 1, axisLabel: { color: '#8c8279' }, splitLine: { lineStyle: { color: '#edf0e9', type: 'dashed' } } },
		dataZoom: [{ type: 'inside', xAxisIndex: 0, zoomOnMouseWheel: true, moveOnMouseMove: true }],
		series: [
			{ name: '总识别次数', type: 'line', smooth: true, symbolSize: 7, data: total, lineStyle: { width: 3 }, areaStyle: { opacity: .07 } },
			{ name: '积极情绪', type: 'line', smooth: true, symbolSize: 7, data: positive, lineStyle: { width: 3 }, areaStyle: { opacity: .08 } },
			{ name: '消极情绪', type: 'line', smooth: true, symbolSize: 7, data: negative, lineStyle: { width: 2 }, areaStyle: { opacity: .05 } },
		],
	});
};

const loadTrend = async () => {
	loading.value = true;
	try {
		const res = await request.get('/api/emotion/recent?days=10');
		if (res.code != 0) return;
		const dates = res.data?.dates || [];
		const total = res.data?.totalData || [];
		const positive = res.data?.positiveData || [];
		const negative = res.data?.negativeData || [];
		let consecutive = 0;
		for (let index = Math.min(negative.length, positive.length) - 1; index >= 0; index -= 1) {
			if (Number(negative[index]) > Number(positive[index])) consecutive += 1;
			else break;
		}
		consecutiveNegativeDays.value = consecutive;
		await nextTick();
		renderChart(dates, total, positive, negative);
	} catch (error) {
		console.warn('情绪趋势暂时无法加载', error);
	} finally {
		loading.value = false;
	}
};

const resizeChart = () => chart?.resize();
onMounted(async () => {
	await nextTick();
	await loadTrend();
	window.addEventListener('resize', resizeChart);
	refreshTimer = setInterval(loadTrend, 300000);
});
onUnmounted(() => {
	window.removeEventListener('resize', resizeChart);
	if (refreshTimer) clearInterval(refreshTimer);
	chart?.dispose();
});
</script>

<style scoped lang="scss">
.trend-card { padding: 28px 32px 26px; border: 1px solid rgba(130, 137, 111, .13); border-radius: 24px; background: rgba(255, 255, 255, .94); box-shadow: 0 14px 42px rgba(84, 88, 68, .08); }
.trend-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; }
.trend-eyebrow { color: #b98b6f; font-size: 11px; letter-spacing: .16em; }
.trend-heading h2 { margin: 5px 0 4px; color: #56695a; font-family: 'MindEase Art'; font-size: 29px; font-weight: 400; }
.trend-heading p { margin: 0; color: #9a8c80; line-height: 1.7; }
.trend-chart { width: 100%; height: 250px; margin-top: 10px; }
.gentle-care { display: flex; align-items: center; gap: 16px; padding: 18px 20px; border: 1px solid #f0dac6; border-radius: 18px; background: linear-gradient(110deg, #fff7eb, #fffaf5); }
.care-icon { display: grid; place-items: center; width: 44px; height: 44px; flex: 0 0 auto; border-radius: 15px; background: #f6dfc8; color: #ac795f; font-size: 21px; }
.care-copy { flex: 1; }.care-copy strong { color: #785b48; font-size: 16px; }.care-copy p { margin: 5px 0 0; color: #9b7d69; line-height: 1.65; }
.chat-button { border-color: #83a58a; color: #607d66; background: #f6fbf5; }
@media (max-width: 600px) {
	.trend-card { padding: 22px 15px; border-radius: 20px; }
	.trend-heading { flex-direction: column; }
	.trend-chart { height: 280px; margin-top: 4px; }
	.gentle-care { align-items: flex-start; flex-wrap: wrap; }
	.chat-button { margin-left: 60px; }
}
</style>
