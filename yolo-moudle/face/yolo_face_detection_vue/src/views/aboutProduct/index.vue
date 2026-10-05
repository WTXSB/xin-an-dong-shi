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

			<HealingIllustratedFrame variant="clouds" />
			</div>
			<div class="evidence-panel">
				<div class="evidence-heading">
					<span class="eyebrow">核心技术路径</span>
					<h2>多证据协同觉察</h2>
					<p>将不同视觉线索相互佐证，把一次检测整理成可追溯的预诊信息。</p>
				</div>
				<div class="evidence-list" aria-label="三类视觉证据">
					<div class="evidence-item emotion-evidence"><span>情</span><div><strong>情绪表情</strong><small>面部情绪与置信度</small></div></div>
					<div class="evidence-item behavior-evidence"><span>行</span><div><strong>BFRB 行为</strong><small>连续事件与持续时间</small></div></div>
					<div class="evidence-item geometry-evidence"><span>几</span><div><strong>手脸几何</strong><small>接近关系与动作线索</small></div></div>
				</div>
				<div class="evidence-bridge"><i></i><span>多模型交叉佐证</span><i></i></div>
				<div class="report-output">
					<div class="report-mark">报告</div>
					<div><strong>形成医生可读的预诊资料</strong><small>结构化记录 · AI 辅助摘要 · PDF 导出</small></div>
				</div>

			<HealingIllustratedFrame variant="garden" />
			</div>
		</section>
			<HealingDecorationStrip variant="sky" />


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
			<HealingDecorationStrip variant="garden" />


		<section class="care-layout">
			<div class="care-main surface-card">
				<span class="eyebrow">今日疗愈推荐</span>
				<h2>先让身体知道：现在是安全的</h2>
				<div class="practice-list">
					<div class="practice-item"><strong>4-4-6 呼吸</strong><span>吸气 4 秒，停 4 秒，呼气 6 秒。让节奏慢慢降下来。</span></div>
					<div class="practice-item"><strong>肩颈松开</strong><span>把肩膀轻轻向后绕三圈，告诉身体不必一直用力。</span></div>
					<div class="practice-item"><strong>写一句事实</strong><span>只写“我现在感觉到……”，不解释，不责备。</span></div>
				</div>

			<HealingIllustratedFrame variant="rest" />
			</div>
			<div class="care-side surface-card">
				<span class="eyebrow">产品原则</span>
				<ul><li>陪伴而非打扰</li><li>理解与接住</li><li>引导而非说教</li><li>觉察先于改变</li></ul>

			<HealingIllustratedFrame variant="clouds" />
			</div>
		</section>

		<section class="metrics-band surface-card">
			<div class="metric-item"><strong>{{ statistics.users }}</strong><span>位体验者</span></div>
			<div class="metric-item"><strong>{{ statistics.records }}</strong><span>次情绪觉察</span></div>
			<div class="metric-item"><strong>3</strong><span>个疗愈方向</span></div>
			<div class="metric-item"><strong>0</strong><span>贴标签表达</span></div>
		</section>

		<HealingDecorationStrip variant="rest" />
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
.about-page { min-height: 100%; padding: 30px clamp(18px, 4vw, 62px) 42px; background: radial-gradient(circle at 12% 8%, rgba(255, 215, 107, .24), transparent 28%), linear-gradient(135deg, #fffdf7, #fff7e3); color: #49382f; font-family: 'MindEase WenKai', 'KaiTi', serif; }
.surface-card { border: 1px solid rgba(155, 101, 55, .14); border-radius: 26px; background: rgba(255, 254, 249, .92); box-shadow: 0 18px 46px rgba(130, 80, 37, .1); }
.about-hero { display: grid; grid-template-columns: minmax(0, 1.35fr) minmax(300px, 0.65fr); overflow: hidden; }
.hero-content { padding: clamp(36px, 5vw, 70px); }
.hero-content h1 { max-width: 760px; margin: 12px 0 20px; font-family: 'MindEase Art', 'STXingkai', 'KaiTi', cursive; font-size: clamp(42px, 4.5vw, 68px); font-weight: 400; line-height: 1.25; color: #4c392e; }
.hero-content p { max-width: 780px; margin: 0; font-size: 18px; line-height: 1.95; color: #637570; }
.eyebrow { display: inline-flex; color: #bd6f41; font-size: 14px; font-weight: 800; letter-spacing: 0.12em; }
.hero-actions { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 30px; }
.primary-action, .secondary-action { height: 44px; border-radius: 999px; font-family: inherit; font-weight: 700; }
.primary-action { border: none; background: #c97543; color: #fff; }
.secondary-action { border-color: rgba(179, 98, 54, .4); background: #fffaf0; color: #9f5a35; }
.evidence-panel { position: relative; display: flex; flex-direction: column; justify-content: center; gap: 18px; padding: clamp(30px, 3vw, 46px); overflow: hidden; background: linear-gradient(150deg, #fff0b9 0%, #fff8dd 50%, #fff4e8 100%); }
.evidence-panel::before, .evidence-panel::after { position: absolute; content: ''; border-radius: 50%; pointer-events: none; }
.evidence-panel::before { width: 210px; height: 210px; top: -110px; right: -72px; background: rgba(236, 169, 82, .2); }
.evidence-panel::after { width: 150px; height: 150px; bottom: -78px; left: -58px; background: rgba(243, 191, 122, 0.14); }
.evidence-heading, .evidence-list, .evidence-bridge, .report-output { position: relative; z-index: 1; }
.evidence-heading h2 { margin: 9px 0 10px; color: #5b3e2c; font-size: clamp(25px, 2.2vw, 32px); }
.evidence-heading p { margin: 0; color: #667873; font-size: 15px; line-height: 1.75; }
.evidence-list { display: grid; gap: 10px; }
.evidence-item { display: flex; align-items: center; gap: 12px; padding: 13px 15px; border: 1px solid rgba(75, 111, 97, 0.11); border-radius: 16px; background: rgba(255, 255, 255, 0.76); box-shadow: 0 8px 20px rgba(59, 87, 76, 0.05); }
.evidence-item > span { width: 38px; height: 38px; display: grid; place-items: center; flex: 0 0 38px; border-radius: 12px; color: #fff; font-weight: 800; }
.evidence-item > div { min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.evidence-item strong { color: #34584c; font-size: 15px; }
.evidence-item small { color: #75857f; font-size: 12px; }
.emotion-evidence > span { background: linear-gradient(145deg, #f2c251, #d9903e); }
.behavior-evidence > span { background: linear-gradient(145deg, #df9d68, #bc6a40); }
.geometry-evidence > span { background: linear-gradient(145deg, #a4b88d, #7f996b); }
.evidence-bridge { display: flex; align-items: center; gap: 10px; color: #5d776d; font-size: 12px; font-weight: 800; letter-spacing: 0.08em; white-space: nowrap; }
.evidence-bridge i { height: 1px; flex: 1; background: linear-gradient(90deg, transparent, rgba(79, 126, 106, 0.35)); }
.evidence-bridge i:last-child { transform: rotate(180deg); }
.report-output { display: flex; align-items: center; gap: 13px; padding: 16px; border: 1px solid rgba(82, 130, 109, 0.16); border-radius: 18px; background: rgba(240, 249, 244, 0.9); }
.report-mark { width: 48px; height: 48px; display: grid; place-items: center; flex: 0 0 48px; border-radius: 14px; background: #b9663b; color: #fff; font-size: 12px; font-weight: 800; }
.report-output > div:last-child { min-width: 0; display: flex; flex-direction: column; gap: 5px; }
.report-output strong { color: #315b4c; font-size: 14px; }
.report-output small { color: #71827b; font-size: 11px; line-height: 1.5; }
.quick-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 18px; margin-top: 20px; }
.quick-card { min-height: 178px; padding: 24px; cursor: pointer; transition: transform 0.25s ease, box-shadow 0.25s ease; }
.quick-card .el-icon { width: 44px; height: 44px; border-radius: 14px; background: #fff1c5; color: #b7683c; font-size: 22px; }
.quick-card h3 { margin: 17px 0 8px; font-size: 20px; }
.quick-card p { margin: 0; color: #687973; font-size: 15px; line-height: 1.75; }
.quick-card:hover { transform: translateY(-4px); box-shadow: 0 22px 44px rgba(48, 81, 69, 0.13); }
.care-layout { display: grid; grid-template-columns: minmax(0, 1.4fr) minmax(280px, 0.6fr); gap: 20px; margin-top: 20px; }
.care-main, .care-side { padding: 28px; }
.care-main h2 { margin: 9px 0 20px; font-size: 28px; }
.practice-list { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.practice-item { padding: 18px; border-radius: 16px; background: #fff8e6; }
.practice-item strong { display: block; margin-bottom: 9px; color: #9b5836; font-size: 17px; }
.practice-item span { color: #677873; line-height: 1.75; }
.care-side ul { list-style: none; margin: 16px 0 0; padding: 0; display: grid; gap: 11px; }
.care-side li { padding: 13px 15px; border-radius: 14px; background: #fbf6ed; color: #526b62; font-weight: 700; }
.metrics-band { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 1px; margin-top: 20px; overflow: hidden; }
.metric-item { padding: 24px; background: rgba(255, 255, 255, 0.68); text-align: center; }
.metric-item strong { display: block; color: #a85e37; font-size: 32px; }
.metric-item span { color: #687873; }
@media (max-width: 1100px) { .about-hero, .care-layout { grid-template-columns: 1fr; } .quick-grid, .practice-list, .metrics-band { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 640px) { .about-page { padding: 16px 14px 28px; } .hero-content { padding: 30px 24px; } .evidence-panel { padding: 30px 22px; } .quick-grid, .practice-list, .metrics-band { grid-template-columns: 1fr; } .care-main, .care-side { padding: 22px; } }
</style>
