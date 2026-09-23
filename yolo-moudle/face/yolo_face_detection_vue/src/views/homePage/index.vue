<template>
	<div class="home-page">
		<section class="home-hero">
			<div class="hero-content">
				<p class="eyebrow">心安动识</p>
				<h1>看见焦虑，遇见安宁</h1>
				<p>
					用已经训练好的视觉模型看见表情线索，再把它转化成温柔的自我觉察、正念练习和疗愈建议。科技在背后工作，陪伴在你身边发生。
				</p>
				<div class="hero-actions">
					<el-button class="primary-action" :icon="View" @click="$router.push('/imgPredict')">进入温柔感知</el-button>
					<el-button class="secondary-action" :icon="ChatLineRound" @click="$router.push('/smartChat')">和安小宁聊聊</el-button>
				</div>
			</div>
			<div class="flower-panel">
				<div class="flower-visual">
					<div class="petal petal-one"></div>
					<div class="petal petal-two"></div>
					<div class="petal petal-three"></div>
					<div class="petal petal-four"></div>
					<div class="flower-core">安</div>
				</div>
				<h2>今日情绪之花</h2>
				<p>不急着盛开也没关系。愿意觉察，就是在给自己浇水。</p>
			</div>
		</section>

		<section class="quick-grid">
			<article class="quick-card" @click="$router.push('/imgPredict')">
				<el-icon><View /></el-icon>
				<h3>温柔感知</h3>
				<p>上传图片，获得一份柔和的情绪线索与陪伴式建议。</p>
			</article>
			<article class="quick-card" @click="$router.push('/smartChat')">
				<el-icon><ChatLineRound /></el-icon>
				<h3>安心对话</h3>
				<p>把当下的心情说出来，先被接住，再慢慢找到下一步。</p>
			</article>
			<article class="quick-card" @click="$router.push('/dataView')">
				<el-icon><TrendCharts /></el-icon>
				<h3>情绪画像</h3>
				<p>用柔和的趋势看见变化，不把任何一次波动当作失败。</p>
			</article>
			<article class="quick-card">
				<el-icon><Sunny /></el-icon>
				<h3>正念片刻</h3>
				<p>预留 3 分钟呼吸、身体扫描和睡前放松，下一阶段接入。</p>
			</article>
		</section>

		<section class="care-layout">
			<div class="care-main">
				<span class="eyebrow">今日疗愈推荐</span>
				<h2>先让身体知道：现在是安全的</h2>
				<div class="practice-list">
					<div class="practice-item">
						<strong>4-4-6 呼吸</strong>
						<span>吸气 4 秒，停 4 秒，呼气 6 秒。让节奏慢慢降下来。</span>
					</div>
					<div class="practice-item">
						<strong>肩颈松开</strong>
						<span>把肩膀轻轻向后绕三圈，告诉身体不必一直用力。</span>
					</div>
					<div class="practice-item">
						<strong>写一句事实</strong>
						<span>只写“我现在感觉到……”，不解释，不责备。</span>
					</div>
				</div>
			</div>
			<div class="care-side">
				<span class="eyebrow">产品原则</span>
				<ul>
					<li>陪伴而非打扰</li>
					<li>理解与接住</li>
					<li>引导而非说教</li>
					<li>觉察先于改变</li>
				</ul>
			</div>
		</section>

		<section class="metrics-band">
			<div class="metric-item">
				<strong>{{ statistics.users }}</strong>
				<span>位体验者</span>
			</div>
			<div class="metric-item">
				<strong>{{ statistics.records }}</strong>
				<span>次情绪觉察</span>
			</div>
			<div class="metric-item">
				<strong>3</strong>
				<span>个疗愈方向</span>
			</div>
			<div class="metric-item">
				<strong>0</strong>
				<span>贴标签表达</span>
			</div>
		</section>
	</div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ChatLineRound, Sunny, TrendCharts, View } from '@element-plus/icons-vue';
import request from '/@/utils/request';

const statistics = ref({
	users: 68,
	records: 325,
});

const params = {
	search: '',
	pageNum: 1,
	pageSize: 10,
};

const getTableData = () => {
	request
		.get('/api/user', {
			params,
		})
		.then((res) => {
			if (res.code == 0) statistics.value.users = res.data.total;
		})
		.catch(() => undefined);
};

const getPlateData = () => {
	request
		.get('/api/imgRecords', {
			params,
		})
		.then((res) => {
			if (res.code == 0) statistics.value.records = res.data.total;
		})
		.catch(() => undefined);
};

const getVideoData = () => {
	request
		.get('/api/videoRecords', {
			params,
		})
		.then((res) => {
			if (res.code == 0) statistics.value.records = statistics.value.records + res.data.total;
		})
		.catch(() => undefined);
};

onMounted(() => {
	getTableData();
	getPlateData();
	getVideoData();
});
</script>

<style scoped lang="scss">
.home-page {
	min-height: 100vh;
	padding: 22px;
	background:
		linear-gradient(135deg, rgba(244, 250, 248, 0.96), rgba(252, 248, 241, 0.95)),
		linear-gradient(90deg, rgba(123, 200, 164, 0.18), rgba(74, 144, 217, 0.12));
	color: #263238;
}

.home-hero {
	display: grid;
	grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.65fr);
	gap: 18px;
	align-items: stretch;
}

.hero-content,
.flower-panel,
.quick-card,
.care-main,
.care-side,
.metrics-band {
	border: 1px solid rgba(91, 124, 121, 0.14);
	background: rgba(255, 255, 255, 0.84);
	border-radius: 8px;
	box-shadow: 0 12px 30px rgba(54, 88, 86, 0.08);
}

.hero-content {
	padding: 36px;

	h1 {
		margin: 8px 0 14px;
		font-size: 40px;
		line-height: 1.15;
		color: #1f3d3a;
		letter-spacing: 0;
	}

	p {
		max-width: 780px;
		margin: 0;
		font-size: 17px;
		line-height: 1.9;
		color: #61716f;
	}
}

.eyebrow {
	display: inline-flex;
	color: #4f8f76;
	font-size: 13px;
	font-weight: 800;
}

.hero-actions {
	display: flex;
	flex-wrap: wrap;
	gap: 12px;
	margin-top: 26px;
}

.primary-action,
.secondary-action {
	height: 42px;
	border-radius: 8px;
	font-weight: 700;
}

.primary-action {
	border: none;
	background: #5cae8a;
	color: #ffffff;
}

.secondary-action {
	border-color: rgba(92, 174, 138, 0.42);
	color: #3f8268;
	background: #f8fcfa;
}

.flower-panel {
	padding: 28px;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	text-align: center;

	h2 {
		margin: 18px 0 8px;
		color: #243f3c;
		font-size: 22px;
	}

	p {
		margin: 0;
		color: #647572;
		line-height: 1.8;
	}
}

.flower-visual {
	position: relative;
	width: 168px;
	aspect-ratio: 1;
	display: grid;
	place-items: center;
}

.petal,
.flower-core {
	position: absolute;
	border-radius: 50%;
}

.petal {
	width: 80px;
	height: 106px;
	background: linear-gradient(180deg, #b9e4d2, #78c9a5);
	opacity: 0.86;
	transform-origin: 50% 84%;
}

.petal-one {
	transform: translateY(-28px);
}

.petal-two {
	transform: rotate(90deg) translateY(-28px);
	background: linear-gradient(180deg, #c8e5f4, #8ec5e8);
}

.petal-three {
	transform: rotate(180deg) translateY(-28px);
	background: linear-gradient(180deg, #ffe2bf, #f3bf7a);
}

.petal-four {
	transform: rotate(270deg) translateY(-28px);
	background: linear-gradient(180deg, #e7dafa, #c0a4e8);
}

.flower-core {
	width: 72px;
	height: 72px;
	display: grid;
	place-items: center;
	background: #ffffff;
	color: #316353;
	font-size: 26px;
	font-weight: 900;
	box-shadow: 0 8px 22px rgba(54, 88, 86, 0.12);
}

.quick-grid {
	display: grid;
	grid-template-columns: repeat(4, minmax(0, 1fr));
	gap: 16px;
	margin-top: 18px;
}

.quick-card {
	min-height: 170px;
	padding: 20px;
	cursor: pointer;
	transition: transform 0.2s ease, box-shadow 0.2s ease;

	.el-icon {
		width: 42px;
		height: 42px;
		border-radius: 8px;
		display: inline-flex;
		align-items: center;
		justify-content: center;
		background: #f1faf6;
		color: #4f9b7b;
		font-size: 22px;
	}

	h3 {
		margin: 16px 0 8px;
		color: #243f3c;
		font-size: 18px;
	}

	p {
		margin: 0;
		color: #667775;
		line-height: 1.75;
	}

	&:hover {
		transform: translateY(-2px);
		box-shadow: 0 16px 34px rgba(54, 88, 86, 0.1);
	}
}

.care-layout {
	display: grid;
	grid-template-columns: minmax(0, 1.4fr) minmax(280px, 0.6fr);
	gap: 18px;
	margin-top: 18px;
}

.care-main,
.care-side {
	padding: 24px;
}

.care-main {
	h2 {
		margin: 8px 0 18px;
		font-size: 24px;
		color: #243f3c;
	}
}

.practice-list {
	display: grid;
	grid-template-columns: repeat(3, minmax(0, 1fr));
	gap: 12px;
}

.practice-item {
	padding: 16px;
	border-radius: 8px;
	background: #f7fbf9;

	strong {
		display: block;
		margin-bottom: 8px;
		color: #376d59;
	}

	span {
		color: #657674;
		line-height: 1.7;
	}
}

.care-side {
	ul {
		list-style: none;
		margin: 14px 0 0;
		padding: 0;
		display: grid;
		gap: 10px;
	}

	li {
		padding: 12px 14px;
		border-radius: 8px;
		background: #f8fbff;
		color: #45656f;
		font-weight: 700;
	}
}

.metrics-band {
	display: grid;
	grid-template-columns: repeat(4, minmax(0, 1fr));
	gap: 1px;
	margin-top: 18px;
	padding: 0;
	overflow: hidden;
}

.metric-item {
	padding: 20px;
	background: rgba(255, 255, 255, 0.6);
	text-align: center;

	strong {
		display: block;
		font-size: 28px;
		color: #2e6653;
	}

	span {
		color: #667775;
	}
}

@media (max-width: 1180px) {
	.home-hero,
	.care-layout {
		grid-template-columns: 1fr;
	}

	.quick-grid,
	.practice-list,
	.metrics-band {
		grid-template-columns: repeat(2, minmax(0, 1fr));
	}
}

@media (max-width: 720px) {
	.home-page {
		padding: 14px;
	}

	.hero-content {
		padding: 24px;

		h1 {
			font-size: 30px;
		}
	}

	.quick-grid,
	.practice-list,
	.metrics-band {
		grid-template-columns: 1fr;
	}
}
</style>
