<template>
	<div class="about-page">
		<section class="about-hero surface-card">
			<div class="hero-content">
				<p class="eyebrow">关于心安动识</p>
				<h1>让科技安静地工作，让陪伴温柔地发生</h1>
				<p>
					我们用已经训练好的视觉模型看见表情与身体线索，再把它转化成柔和的自我觉察、正念练习和疗愈建议。这里不急着给你贴标签，只陪你慢慢看见自己。
				</p>
				<div class="hero-actions">
					<el-button class="primary-action" :icon="View" @click="$router.push('/imgPredict')">进入温柔感知</el-button>
					<el-button class="secondary-action" :icon="ChatLineRound" @click="$router.push('/smartChat')">和安小宁聊聊</el-button>
				</div>
			</div>
			<div class="flower-panel">
				<div class="flower-visual" aria-hidden="true">
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
			<article class="quick-card surface-card" @click="$router.push('/imgPredict')">
				<el-icon><View /></el-icon>
				<h3>温柔感知</h3>
				<p>上传图片，获得一份柔和的情绪线索与陪伴式建议。</p>
			</article>
			<article class="quick-card surface-card" @click="$router.push('/smartChat')">
				<el-icon><ChatLineRound /></el-icon>
				<h3>安心对话</h3>
				<p>把当下的心情说出来，先被接住，再慢慢找到下一步。</p>
			</article>
			<article class="quick-card surface-card" @click="$router.push('/dataView')">
				<el-icon><TrendCharts /></el-icon>
				<h3>情绪画像</h3>
				<p>用柔和的趋势看见变化，不把任何一次波动当作失败。</p>
			</article>
			<article class="quick-card surface-card" @click="$router.push('/trashMap')">
				<el-icon><Sunny /></el-icon>
				<h3>正念片刻</h3>
				<p>从呼吸、身体扫描与附近资源中，找到适合此刻的放松方式。</p>
			</article>
		</section>

		<section class="care-layout">
			<div class="care-main surface-card">
				<span class="eyebrow">今日疗愈推荐</span>
				<h2>先让身体知道：现在是安全的</h2>
				<div class="practice-list">
					<div class="practice-item"><strong>4-4-6 呼吸</strong><span>吸气 4 秒，停 4 秒，呼气 6 秒。让节奏慢慢降下来。</span></div>
					<div class="practice-item"><strong>肩颈松开</strong><span>把肩膀轻轻向后绕三圈，告诉身体不必一直用力。</span></div>
					<div class="practice-item"><strong>写一句事实</strong><span>只写“我现在感觉到……”，不解释，不责备。</span></div>
				</div>
			</div>
			<div class="care-side surface-card">
				<span class="eyebrow">产品原则</span>
				<ul><li>陪伴而非打扰</li><li>理解与接住</li><li>引导而非说教</li><li>觉察先于改变</li></ul>
			</div>
		</section>

		<section class="metrics-band surface-card">
			<div class="metric-item"><strong>{{ statistics.users }}</strong><span>位体验者</span></div>
			<div class="metric-item"><strong>{{ statistics.records }}</strong><span>次情绪觉察</span></div>
			<div class="metric-item"><strong>3</strong><span>个疗愈方向</span></div>
			<div class="metric-item"><strong>0</strong><span>贴标签表达</span></div>
		</section>
	</div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ChatLineRound, Sunny, TrendCharts, View } from '@element-plus/icons-vue';
import request from '/@/utils/request';

const statistics = ref({ users: 68, records: 325 });
const params = { search: '', pageNum: 1, pageSize: 10 };

const loadStatistics = async () => {
	const [users, images, videos] = await Promise.allSettled([
		request.get('/api/user', { params }), request.get('/api/imgRecords', { params }), request.get('/api/videoRecords', { params }),
	]);
	if (users.status === 'fulfilled' && users.value.code == 0) statistics.value.users = users.value.data.total;
	const imageTotal = images.status === 'fulfilled' && images.value.code == 0 ? images.value.data.total : 0;
	const videoTotal = videos.status === 'fulfilled' && videos.value.code == 0 ? videos.value.data.total : 0;
	if (imageTotal || videoTotal) statistics.value.records = imageTotal + videoTotal;
};

onMounted(loadStatistics);
</script>

<style scoped lang="scss">
.about-page { min-height: 100%; padding: 30px clamp(18px, 4vw, 62px) 42px; background: radial-gradient(circle at 12% 8%, rgba(244, 207, 145, 0.2), transparent 28%), linear-gradient(135deg, #f5faf7, #fffaf2); color: #263c36; font-family: 'MindEase WenKai', 'KaiTi', serif; }
.surface-card { border: 1px solid rgba(76, 115, 98, 0.12); border-radius: 22px; background: rgba(255, 255, 255, 0.88); box-shadow: 0 16px 42px rgba(48, 81, 69, 0.08); }
.about-hero { display: grid; grid-template-columns: minmax(0, 1.35fr) minmax(300px, 0.65fr); overflow: hidden; }
.hero-content { padding: clamp(36px, 5vw, 70px); }
.hero-content h1 { max-width: 760px; margin: 12px 0 20px; font-family: 'MindEase Art', 'STXingkai', 'KaiTi', cursive; font-size: clamp(42px, 4.5vw, 68px); font-weight: 400; line-height: 1.25; color: #254e42; }
.hero-content p { max-width: 780px; margin: 0; font-size: 18px; line-height: 1.95; color: #637570; }
.eyebrow { display: inline-flex; color: #5c927b; font-size: 14px; font-weight: 800; letter-spacing: 0.12em; }
.hero-actions { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 30px; }
.primary-action, .secondary-action { height: 44px; border-radius: 999px; font-family: inherit; font-weight: 700; }
.primary-action { border: none; background: #5f9f82; color: #fff; }
.secondary-action { border-color: rgba(95, 159, 130, 0.42); background: #f8fcfa; color: #467e67; }
.flower-panel { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 38px; background: linear-gradient(145deg, #edf8f1, #fff4e5); text-align: center; }
.flower-panel h2 { margin: 18px 0 8px; font-size: 24px; }
.flower-panel p { max-width: 320px; margin: 0; color: #687a74; font-size: 16px; line-height: 1.8; }
.flower-visual { position: relative; width: 168px; aspect-ratio: 1; display: grid; place-items: center; }
.petal, .flower-core { position: absolute; border-radius: 50%; }
.petal { width: 80px; height: 106px; background: linear-gradient(180deg, #b9e4d2, #78c9a5); opacity: 0.86; transform-origin: 50% 84%; }
.petal-one { transform: translateY(-28px); }
.petal-two { transform: rotate(90deg) translateY(-28px); background: linear-gradient(180deg, #c8e5f4, #8ec5e8); }
.petal-three { transform: rotate(180deg) translateY(-28px); background: linear-gradient(180deg, #ffe2bf, #f3bf7a); }
.petal-four { transform: rotate(270deg) translateY(-28px); background: linear-gradient(180deg, #e7dafa, #c0a4e8); }
.flower-core { width: 72px; height: 72px; display: grid; place-items: center; background: #fff; color: #316353; font-family: 'MindEase Art', cursive; font-size: 32px; box-shadow: 0 8px 22px rgba(54, 88, 86, 0.12); }
.quick-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 18px; margin-top: 20px; }
.quick-card { min-height: 178px; padding: 24px; cursor: pointer; transition: transform 0.25s ease, box-shadow 0.25s ease; }
.quick-card .el-icon { width: 44px; height: 44px; border-radius: 14px; background: #eef8f2; color: #4f9276; font-size: 22px; }
.quick-card h3 { margin: 17px 0 8px; font-size: 20px; }
.quick-card p { margin: 0; color: #687973; font-size: 15px; line-height: 1.75; }
.quick-card:hover { transform: translateY(-4px); box-shadow: 0 22px 44px rgba(48, 81, 69, 0.13); }
.care-layout { display: grid; grid-template-columns: minmax(0, 1.4fr) minmax(280px, 0.6fr); gap: 20px; margin-top: 20px; }
.care-main, .care-side { padding: 28px; }
.care-main h2 { margin: 9px 0 20px; font-size: 28px; }
.practice-list { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.practice-item { padding: 18px; border-radius: 16px; background: #f5faf7; }
.practice-item strong { display: block; margin-bottom: 9px; color: #376d59; font-size: 17px; }
.practice-item span { color: #677873; line-height: 1.75; }
.care-side ul { list-style: none; margin: 16px 0 0; padding: 0; display: grid; gap: 11px; }
.care-side li { padding: 13px 15px; border-radius: 14px; background: #fbf6ed; color: #526b62; font-weight: 700; }
.metrics-band { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 1px; margin-top: 20px; overflow: hidden; }
.metric-item { padding: 24px; background: rgba(255, 255, 255, 0.68); text-align: center; }
.metric-item strong { display: block; color: #2e6653; font-size: 32px; }
.metric-item span { color: #687873; }
@media (max-width: 1100px) { .about-hero, .care-layout { grid-template-columns: 1fr; } .quick-grid, .practice-list, .metrics-band { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 640px) { .about-page { padding: 16px 14px 28px; } .hero-content { padding: 30px 24px; } .flower-panel { padding: 30px 22px; } .quick-grid, .practice-list, .metrics-band { grid-template-columns: 1fr; } .care-main, .care-side { padding: 22px; } }
</style>
