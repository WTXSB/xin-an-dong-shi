<template>
	<section class="emotion-gallery" aria-label="动物情绪表情">
		<article v-for="(group, row) in groups" :key="group.title" class="emotion-family" :class="group.tone">
			<h2>{{ group.title }}</h2>
			<div class="expression-grid">
				<figure v-for="(label, column) in group.labels" :key="label">
					<span class="expression-art" aria-hidden="true" :style="{ '--atlas-x': [178, 552, 916, 1270][column], '--atlas-y': [195, 540, 858][row] }"></span>
					<figcaption>{{ label }}</figcaption>
				</figure>
			</div>
		</article>
	</section>
</template>

<script setup lang="ts">
// 独立展示插画，不参与识别结果、统计或记录的数据处理。
const groups = [
	{ title: '积极情绪', tone: 'positive', labels: ['开心', '满足', '期待', '感激'] },
	{ title: '消极情绪', tone: 'negative', labels: ['悲伤', '愤怒', '担忧', '疲惫'] },
	{ title: '中性情绪', tone: 'neutral', labels: ['平静', '专注', '思考', '观察'] },
];
</script>

<style scoped lang="scss">
.emotion-gallery { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 22px; max-width: 1280px; margin: 30px auto; }
.emotion-family { position: relative; padding: 28px 22px; min-width: 0; border-radius: 30px 36px 26px 34px; background: linear-gradient(155deg, #f4f7e9, #fffdf5); box-shadow: 0 12px 30px rgba(117, 103, 72, .05); }
.negative { background: linear-gradient(155deg, #fff0e9, #fffaf5); border-radius: 36px 26px 34px 28px; }
.neutral { background: linear-gradient(155deg, #f1eef8, #fffdf7); border-radius: 28px 34px 36px 26px; }
h2 { margin: 0 0 22px; color: #5f6853; font-size: 20px; font-weight: 600; }
.negative h2 { color: #835f52; }.neutral h2 { color: #706581; }
.expression-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 24px 16px; }
figure { display: flex; align-items: center; flex-direction: column; margin: 0; gap: 10px; }
.expression-art { --art-size: 104px; display: block; flex: 0 0 auto; width: var(--art-size); height: var(--art-size); background-image: url('../../assets/healing-frames/animal-emotions-twelve-v2.png'); background-size: calc(var(--art-size) * 4.525) calc(var(--art-size) * 3.39375); background-position: calc(var(--art-size) * (.5 - var(--atlas-x) / 320)) calc(var(--art-size) * (.5 - var(--atlas-y) / 320)); background-repeat: no-repeat; }
figcaption { color: #5d5148; line-height: 1.6; font-size: 15px; letter-spacing: .08em; }
@media (max-width: 1050px) { .emotion-family { padding: 24px 14px; }.expression-art { --art-size: 88px; }.expression-grid { gap: 22px 8px; } }
@media (max-width: 700px) { .emotion-gallery { grid-template-columns: 1fr; gap: 18px; margin: 22px auto; }.emotion-family { padding: 26px 24px; }.expression-art { --art-size: 104px; }.expression-grid { gap: 24px 18px; } }
</style>
