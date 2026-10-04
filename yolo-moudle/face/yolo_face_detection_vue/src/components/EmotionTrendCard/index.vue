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
		animationDuration: 1100,
		animationEasing: 'cubicOut',
		color: ['#c96f3d', '#efbd45', '#8fa57b'],
		tooltip: {
			trigger: 'axis',
			backgroundColor: 'rgba(255,254,247,.98)',
			borderColor: '#efd3ae',
			borderWidth: 1,
			padding: [10, 13],
			textStyle: { color: '#5b4639', fontFamily: 'MindEase WenKai' },
			extraCssText: 'border-radius:14px;box-shadow:0 12px 30px rgba(141,83,41,.14)'
		},
		legend: {
			top: 6,
			left: 'center',
			itemWidth: 15,
			itemHeight: 8,
			itemGap: 24,
			textStyle: { color: '#78675b', fontSize: 13, fontFamily: 'MindEase WenKai' }
		},
		grid: { left: 24, right: 24, top: 62, bottom: 34, containLabel: true },
		xAxis: {
			type: 'category',
			boundaryGap: false,
			data: dates,
			axisTick: { show: true, alignWithLabel: true, lineStyle: { color: '#c9b8a7' } },
			axisLine: { lineStyle: { color: '#cdbfaf', width: 1.5 } },
			axisLabel: { color: '#76675c', margin: 14, fontFamily: 'MindEase WenKai' }
		},
		yAxis: {
			type: 'value',
			minInterval: 1,
			axisLabel: { color: '#88786c', margin: 14, fontFamily: 'MindEase WenKai' },
			axisLine: { show: false },
			axisTick: { show: false },
			splitLine: { lineStyle: { color: 'rgba(188,157,122,.18)', type: 'dashed' } }
		},
		dataZoom: [{ type: 'inside', xAxisIndex: 0, zoomOnMouseWheel: true, moveOnMouseMove: true }],
		series: [
			{
				name: '总识别次数',
				type: 'line',
				smooth: .42,
				symbol: 'circle',
				symbolSize: 11,
				showSymbol: true,
				data: total,
				z: 4,
				lineStyle: { width: 4, color: '#c96f3d', shadowColor: 'rgba(201,111,61,.18)', shadowBlur: 8 },
				itemStyle: { color: '#fffdf5', borderColor: '#c96f3d', borderWidth: 3 },
				areaStyle: {
					color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
						{ offset: 0, color: 'rgba(248,203,87,.36)' },
						{ offset: .72, color: 'rgba(255,235,169,.13)' },
						{ offset: 1, color: 'rgba(255,250,231,0)' },
					])
				}
			},
			{
				name: '积极情绪', type: 'line', smooth: .42, symbol: 'circle', symbolSize: 7, data: positive, z: 3,
				lineStyle: { width: 2, color: '#efbd45', type: 'dashed', opacity: .9 },
				itemStyle: { color: '#fffdf5', borderColor: '#efbd45', borderWidth: 2 }
			},
			{
				name: '消极情绪', type: 'line', smooth: .42, symbol: 'circle', symbolSize: 7, data: negative, z: 2,
				lineStyle: { width: 2, color: '#8fa57b', type: 'dashed', opacity: .86 },
				itemStyle: { color: '#fffdf5', borderColor: '#8fa57b', borderWidth: 2 }
			},
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
.trend-card { position: relative; overflow: hidden; padding: 30px 34px 27px; border: 1px solid rgba(163, 112, 65, .15); border-radius: 28px; background: rgba(255, 254, 248, .96); box-shadow: 0 18px 46px rgba(136, 88, 42, .1); }
.trend-card::before { position: absolute; width: 250px; height: 250px; right: -105px; top: -125px; content: ''; border-radius: 50%; background: rgba(255, 221, 119, .2); pointer-events: none; }
.trend-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; }
.trend-eyebrow { color: #c17043; font-size: 11px; letter-spacing: .16em; }
.trend-heading h2 { margin: 5px 0 4px; color: #4e392d; font-family: 'MindEase Art'; font-size: 30px; font-weight: 400; }
.trend-heading p { margin: 0; color: #958477; line-height: 1.7; }
.trend-chart { width: 100%; height: 286px; margin-top: 8px; }
.gentle-care { display: flex; align-items: center; gap: 16px; padding: 18px 20px; border: 1px solid #f0dac6; border-radius: 18px; background: linear-gradient(110deg, #fff7eb, #fffaf5); }
.care-icon { display: grid; place-items: center; width: 44px; height: 44px; flex: 0 0 auto; border-radius: 15px; background: #f6dfc8; color: #ac795f; font-size: 21px; }
.care-copy { flex: 1; }.care-copy strong { color: #785b48; font-size: 16px; }.care-copy p { margin: 5px 0 0; color: #9b7d69; line-height: 1.65; }
.chat-button { border-color: #c58657; color: #9e5a34; background: #fffaf0; }
@media (max-width: 600px) {
	.trend-card { padding: 22px 15px; border-radius: 20px; }
	.trend-heading { flex-direction: column; }
	.trend-chart { height: 280px; margin-top: 4px; }
	.gentle-care { align-items: flex-start; flex-wrap: wrap; }
	.chat-button { margin-left: 60px; }
}
</style>
