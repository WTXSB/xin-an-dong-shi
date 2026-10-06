<template>
	<article v-if="entry.analysis" class="diary-insight-card">
		<header class="diary-insight-heading">
			<div>
				<span>REFLECTION LETTER · {{ entry.date }}</span>
				<h3>这一页里，被看见的心情</h3>
			</div>
			<span class="diary-insight-stamp">日记回顾</span>
		</header>
		<p class="diary-insight-summary">{{ entry.analysis.summary }}</p>
		<div class="diary-insight-emotions">
			<section v-for="(emotion, index) in entry.analysis.emotions" :key="index">
				<h4>{{ emotion.label }}</h4>
				<blockquote>“{{ emotion.evidence }}”</blockquote>
				<p>{{ emotion.reflection }}</p>
			</section>
		</div>
		<div class="diary-insight-bottom">
			<div>
				<h4>贴近今天的一小步</h4>
				<p>{{ entry.analysis.suggestion }}</p>
				<div class="diary-personal-blessing">
					<span>写给今天的你</span>
					<p>{{ entry.analysis.blessing }}</p>
				</div>
			</div>
			<img :src="insightArt" alt="" />
		</div>
		<p v-if="entry.analysis.safetyNote" class="diary-safety-note" role="note">{{ entry.analysis.safetyNote }}</p>
		<footer class="diary-insight-footer">
			<span>DeepSeek · {{ entry.analysis.model }}</span
			><span>这是辅助回顾，不是诊断。感受的解释权始终属于你。</span>
		</footer>
	</article>
</template>
<script setup lang="ts">
import type { DiaryEntry } from '/@/api/healing/diary';
import insightArt from '/@/assets/healing-frames/diary-reflection-letter-v1.png';
defineProps<{ entry: DiaryEntry }>();
</script>
<style scoped lang="scss">
.diary-insight-card {
	position: relative;
	isolation: isolate;
	width: 100%;
	min-width: 0;
	padding: clamp(24px, 3.5vw, 48px);
	margin-top: 28px;
	border-radius: 12px 36px 20px 32px;
	background: linear-gradient(135deg, #fffcf4, #fff3de 72%, #f1efd8);
	box-shadow: 0 18px 45px #806b4820, 0 2px 0 #fff8e8 inset;
	color: #584334;
	overflow-wrap: anywhere;
}
.diary-insight-card::before {
	content: '';
	position: absolute;
	inset: 10px;
	border: 1px solid #b5b79766;
	border-radius: 8px 30px 15px 26px;
	pointer-events: none;
}
.diary-insight-card::after {
	content: '';
	position: absolute;
	height: 10px;
	top: -4px;
	left: 38px;
	width: 85px;
	background: #9eaa7899;
	transform: rotate(-3deg);
	border-radius: 2px;
}
.diary-insight-heading {
	display: flex;
	align-items: start;
	justify-content: space-between;
	gap: 14px;
	margin-bottom: 20px;
}
.diary-insight-heading span {
	letter-spacing: 0.12em;
	font-size: 11px;
	color: #7b6b54;
}
.diary-insight-heading h3 {
	margin: 8px 0 0;
	font-size: clamp(23px, 2.2vw, 30px);
	font-family: 'MindEase Art', sans-serif;
	font-weight: 400;
}
.diary-insight-heading .diary-insight-stamp {
	letter-spacing: 0.08em;
	padding: 9px 13px;
	border: 1px solid #b7856777;
	outline: 1px solid #b7856744;
	outline-offset: 3px;
	border-radius: 12px;
	color: #925a3d;
	white-space: nowrap;
	transform: rotate(4deg);
}
.diary-insight-summary {
	margin: 0 0 24px;
	line-height: 1.95;
	font-size: 16px;
	white-space: pre-line;
}
.diary-insight-emotions {
	display: grid;
	grid-template-columns: repeat(auto-fit, minmax(min(100%, 220px), 1fr));
	gap: 16px;
}
.diary-insight-emotions section {
	border-radius: 18px;
	padding: 20px;
	background: #e8ebdb9c;
}
.diary-insight-emotions section:nth-child(3n + 2) {
	background: #f4ded18c;
}
.diary-insight-emotions section:nth-child(3n) {
	background: #e7dfec85;
}
h4 {
	margin: 0 0 8px;
	font-size: 16px;
	color: #645540;
}
blockquote {
	margin: 10px 0;
	color: #8c654b;
	font-size: 14px;
	line-height: 1.8;
}
section p,
.diary-insight-bottom p {
	line-height: 1.9;
	margin: 0;
	white-space: pre-line;
}
.diary-insight-bottom {
	display: grid;
	grid-template-columns: minmax(0, 1fr) 230px;
	align-items: end;
	gap: 24px;
	margin-top: 28px;
}
.diary-insight-bottom img {
	display: block;
	width: 100%;
	height: auto;
}
.diary-personal-blessing {
	position: relative;
	margin-top: 20px;
	padding: 22px 0 0;
	border-top: 1px dashed #b3966866;
}
.diary-personal-blessing span {
	color: #9a623c;
	display: block;
	margin-bottom: 10px;
	font-family: 'MindEase Art', sans-serif;
	font-size: 23px;
}
.diary-personal-blessing p {
	font-size: 17px;
	color: #73543c;
}
.diary-insight-footer {
	display: flex;
	flex-wrap: wrap;
	gap: 8px 24px;
	margin-top: 25px;
	color: #776b59;
	font-size: 12px;
	line-height: 1.7;
}
.diary-safety-note {
	padding: 16px 20px;
	border-radius: 16px;
	background: #f2dbcc;
	color: #74432d;
	line-height: 1.8;
}
@media (max-width: 800px) {
	.diary-insight-emotions {
		grid-template-columns: 1fr;
	}
	.diary-insight-bottom {
		grid-template-columns: 1fr;
	}
	.diary-insight-bottom img {
		width: min(100%, 240px);
		justify-self: end;
	}
	.diary-insight-heading .diary-insight-stamp {
		font-size: 11px;
	}
	.diary-insight-card {
		padding: 28px 22px;
	}
}
</style>
