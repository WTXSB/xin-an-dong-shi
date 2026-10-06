<template>
	<section class="diary-records-section" aria-labelledby="diary-reflections-title">
		<div class="diary-records-heading">
			<div>
				<span>THE PAGES WE KEEP</span>
				<h2 id="diary-reflections-title">日记心情回顾</h2>
				<p>从你写下的具体经历里，整理复杂心情，留下一份只属于那一天的祝福。</p>
			</div>
			<el-button round :loading="loading" @click="refresh">刷新日记回顾</el-button>
		</div>
		<p v-if="error" class="diary-records-error" role="alert">{{ error }} <router-link v-if="loginRequired" to="/login">重新登录</router-link></p>
		<div v-loading="loading" class="diary-records-content">
			<DiaryInsightCard v-for="entry in entries" :key="entry.date" :entry="entry" />
			<div v-if="!entries.length && !loading && !error" class="diary-reflection-empty">
				<div>
					<h3>等你写下一页，再慢慢读懂它</h3>
					<p>日记不会自动发送给 AI。在情绪日记中保存文字，并确认分析后，当天的回顾会出现在这里。</p>
					<router-link to="/dataView">打开情绪日记 <span aria-hidden="true">↗</span></router-link>
				</div>
				<img :src="insightArt" alt="" />
			</div>
		</div>
		<div v-if="page > 1 || entries.length === 6" class="diary-reflection-pages">
			<el-button round :disabled="page === 1 || loading" @click="changePage(-1)">上一页</el-button><span>第 {{ page }} 页</span
			><el-button round :disabled="entries.length < 6 || loading" @click="changePage(1)">下一页</el-button>
		</div>
	</section>
</template>
<script setup lang="ts">
import { ref, onMounted, onActivated, onBeforeUnmount } from 'vue';
import { getDiaryInsights, type DiaryEntry } from '/@/api/healing/diary';
import DiaryInsightCard from './DiaryInsightCard.vue';
import insightArt from '/@/assets/healing-frames/diary-reflection-letter-v1.png';
const entries = ref<DiaryEntry[]>([]),
	loading = ref(false),
	error = ref(''),
	loginRequired = ref(false),
	page = ref(1);
async function refresh() {
	if (loading.value) return;
	loading.value = true;
	try {
		entries.value = await getDiaryInsights(page.value);
		error.value = '';
		loginRequired.value = false;
	} catch (e: any) {
		error.value = e.message;
		loginRequired.value = e.code === '403';
		entries.value = [];
	} finally {
		loading.value = false;
	}
}
const updated = () => {
	page.value = 1;
	void refresh();
};
const changePage = async (step: number) => {
	page.value += step;
	await refresh();
};
onMounted(() => {
	void refresh();
	window.addEventListener('mindease:diary-updated', updated);
});
onActivated(() => void refresh());
onBeforeUnmount(() => window.removeEventListener('mindease:diary-updated', updated));
</script>
<style scoped lang="scss">
.diary-records-section {
	margin: 30px 0;
	min-width: 0;
	padding: clamp(22px, 3vw, 38px);
	background: linear-gradient(135deg, #f3ebd7, #edf0df);
	border-radius: 30px;
	box-shadow: 0 16px 38px #86704910;
	color: #614c3c;
}
.diary-records-heading {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 24px;
}
.diary-records-heading span {
	font-size: 11px;
	letter-spacing: 0.14em;
	color: #807254;
}
.diary-records-heading h2 {
	margin: 8px 0 10px;
	font-family: 'MindEase Art', sans-serif;
	font-weight: 400;
	font-size: 30px;
}
.diary-records-heading p {
	line-height: 1.9;
	color: #75634f;
	margin: 0;
}
.diary-records-content {
	min-height: 180px;
}
.diary-reflection-empty {
	display: grid;
	grid-template-columns: minmax(0, 1fr) 290px;
	align-items: center;
	gap: 28px;
	margin-top: 24px;
	background: #fff9ec;
	border-radius: 24px;
	padding: 30px;
}
.diary-reflection-empty h3 {
	margin: 0 0 15px;
	font-weight: 500;
	font-size: 23px;
}
.diary-reflection-empty p {
	line-height: 1.9;
	color: #7d6b57;
	max-width: 620px;
}
.diary-reflection-empty img {
	display: block;
	width: 100%;
	height: auto;
}
.diary-reflection-empty a,
.diary-records-error a {
	display: inline-block;
	color: #945733;
	border-bottom: 1px solid #be9c75;
	padding: 8px 0;
}
.diary-records-error {
	background: #fff6e9;
	padding: 16px;
	line-height: 1.8;
	border-radius: 14px;
}
.diary-reflection-pages {
	display: flex;
	justify-content: center;
	align-items: center;
	gap: 18px;
	margin-top: 24px;
}
@media (max-width: 760px) {
	.diary-records-heading {
		align-items: start;
		flex-direction: column;
		gap: 16px;
	}
	.diary-reflection-empty {
		grid-template-columns: 1fr;
		padding: 24px;
	}
	.diary-reflection-empty img {
		width: min(100%, 220px);
		justify-self: end;
	}
}
</style>
