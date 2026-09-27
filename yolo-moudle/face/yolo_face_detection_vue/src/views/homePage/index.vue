<template>
	<div class="home-showcase">
		<section
			class="carousel-shell"
			@mouseenter="autoplay = false"
			@mouseleave="autoplay = true"
			@touchstart="onTouchStart"
			@touchend="onTouchEnd"
		>
			<el-carousel
				ref="carouselRef"
				class="hero-carousel"
				height="calc(100vh - 82px)"
				:interval="5000"
				:autoplay="autoplay"
				:pause-on-hover="true"
				arrow="always"
				indicator-position="none"
				@change="onSlideChange"
			>
				<el-carousel-item v-for="slide in slides" :key="slide.src">
					<img class="hero-image" :src="slide.src" :alt="slide.alt" />
				</el-carousel-item>
			</el-carousel>

			<div class="hero-shade" aria-hidden="true"></div>
			<div class="hero-copy">
				<p class="hero-kicker">心安动识 · MINDEASE</p>
				<h1>看见焦虑，遇见安宁</h1>
				<p class="hero-description">
					用温柔的科技看见情绪与身体线索，让每一次觉察，都成为更靠近自己的开始。
				</p>
				<div class="hero-actions">
					<el-button class="primary-action" @click="$router.push('/imgPredict')">开始温柔感知</el-button>
					<el-button class="secondary-action" @click="$router.push('/aboutProduct')">了解心安动识</el-button>
				</div>
			</div>

			<div class="slide-dots" aria-label="轮播图切换">
				<button
					v-for="(_, index) in slides"
					:key="index"
					type="button"
					:class="{ active: activeIndex === index }"
					:aria-label="`查看第 ${index + 1} 张图片`"
					@click="carouselRef?.setActiveItem(index)"
				></button>
			</div>
		</section>
	</div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import heroJournal from '/@/assets/home-carousel/hero-journal.webp';
import heroMeadow from '/@/assets/home-carousel/hero-meadow.webp';
import heroCompanionship from '/@/assets/home-carousel/hero-companionship.webp';
import heroSeaside from '/@/assets/home-carousel/hero-seaside.webp';

const carouselRef = ref<any>();
const activeIndex = ref(0);
const touchStartX = ref(0);
const autoplay = ref(true);

const slides = [
	{ src: heroJournal, alt: '晨光中安静书写，给情绪留出被看见的空间' },
	{ src: heroMeadow, alt: '在晨光草地上慢慢行走，感受呼吸与身体' },
	{ src: heroCompanionship, alt: '在温暖的陪伴中表达心情，感受被认真倾听' },
	{ src: heroSeaside, alt: '迎着海边晨光安静远望，重新找回内心节奏' },
];

const onSlideChange = (index: number) => {
	activeIndex.value = index;
};

const onTouchStart = (event: TouchEvent) => {
	autoplay.value = false;
	touchStartX.value = event.changedTouches[0]?.clientX || 0;
};

const onTouchEnd = (event: TouchEvent) => {
	const distance = (event.changedTouches[0]?.clientX || 0) - touchStartX.value;
	if (Math.abs(distance) >= 48) {
		if (distance > 0) carouselRef.value?.prev();
		else carouselRef.value?.next();
	}
	autoplay.value = true;
};
</script>

<style scoped lang="scss">
.home-showcase {
	min-height: 100%;
	padding: 16px;
	background: #f8f4ec;
	font-family: 'MindEase WenKai', 'KaiTi', serif;
}

.carousel-shell {
	position: relative;
	min-height: 560px;
	overflow: hidden;
	border-radius: 22px;
	background: #dfe8df;
	box-shadow: 0 22px 58px rgba(50, 72, 61, 0.16);
}

.hero-carousel,
.hero-carousel :deep(.el-carousel__container),
.hero-carousel :deep(.el-carousel__item) {
	min-height: 560px;
}

.hero-image {
	display: block;
	width: 100%;
	height: 100%;
	object-fit: cover;
	object-position: center;
	transform: scale(1.002);
}

.hero-shade {
	position: absolute;
	inset: 0;
	z-index: 2;
	pointer-events: none;
	background:
		linear-gradient(90deg, rgba(24, 52, 44, 0.7) 0%, rgba(39, 63, 53, 0.48) 30%, rgba(52, 67, 57, 0.08) 64%, transparent 100%),
		linear-gradient(0deg, rgba(15, 36, 30, 0.2), transparent 42%);
}

.hero-copy {
	position: absolute;
	left: clamp(42px, 6vw, 102px);
	top: 50%;
	z-index: 3;
	width: min(610px, 48vw);
	color: #fffef9;
	transform: translateY(-50%);
	text-shadow: 0 3px 22px rgba(16, 42, 34, 0.28);
}

.hero-kicker {
	margin: 0 0 18px;
	font-family: 'MindEase WenKai', 'KaiTi', serif;
	font-size: 15px;
	font-weight: 700;
	letter-spacing: 0.22em;
	opacity: 0.9;
}

h1 {
	margin: 0;
	font-family: 'MindEase Art', 'STXingkai', 'KaiTi', cursive;
	font-size: clamp(58px, 6vw, 92px);
	font-weight: 400;
	line-height: 1.18;
	letter-spacing: 0.06em;
}

.hero-description {
	max-width: 560px;
	margin: 24px 0 0;
	font-size: 19px;
	line-height: 1.9;
	letter-spacing: 0.06em;
	opacity: 0.94;
}

.hero-actions {
	display: flex;
	flex-wrap: wrap;
	gap: 14px;
	margin-top: 34px;
}

.primary-action,
.secondary-action {
	height: 48px;
	padding: 0 27px;
	border-radius: 999px;
	font-family: 'MindEase WenKai', 'KaiTi', serif;
	font-size: 16px;
	font-weight: 700;
	letter-spacing: 0.04em;
}

.primary-action {
	border-color: #fff8e9;
	background: #fff8e9;
	color: #315f4f;
}

.primary-action:hover {
	border-color: #ffffff;
	background: #ffffff;
	color: #254d40;
}

.secondary-action {
	border-color: rgba(255, 255, 255, 0.72);
	background: rgba(255, 255, 255, 0.12);
	color: #ffffff;
	backdrop-filter: blur(8px);
}

.secondary-action:hover {
	border-color: #ffffff;
	background: rgba(255, 255, 255, 0.22);
	color: #ffffff;
}

.slide-dots {
	position: absolute;
	left: 50%;
	bottom: 24px;
	z-index: 4;
	display: flex;
	gap: 9px;
	transform: translateX(-50%);

	button {
		width: 9px;
		height: 9px;
		padding: 0;
		border: 1px solid rgba(255, 255, 255, 0.8);
		border-radius: 999px;
		background: rgba(255, 255, 255, 0.38);
		cursor: pointer;
		transition: width 0.25s ease, background 0.25s ease;
	}

	button.active {
		width: 30px;
		background: #fff9ec;
	}
}

:deep(.el-carousel__arrow) {
	width: 44px;
	height: 44px;
	background: rgba(255, 255, 255, 0.82);
	color: #365e51;
	box-shadow: 0 8px 24px rgba(34, 63, 53, 0.18);
}

@media (max-width: 1000px) {
	.home-showcase {
		padding: 0;
	}

	.carousel-shell {
		border-radius: 0;
	}

	.hero-copy {
		left: 34px;
		width: min(620px, 72vw);
	}
}

@media (max-width: 640px) {
	.carousel-shell,
	.hero-carousel,
	.hero-carousel :deep(.el-carousel__container),
	.hero-carousel :deep(.el-carousel__item) {
		min-height: 620px;
	}

	.hero-image {
		object-position: 66% center;
	}

	.hero-shade {
		background:
			linear-gradient(90deg, rgba(24, 52, 44, 0.74), rgba(35, 59, 49, 0.4) 72%, rgba(35, 59, 49, 0.2)),
			linear-gradient(0deg, rgba(15, 36, 30, 0.34), transparent 55%);
	}

	.hero-copy {
		left: 24px;
		right: 24px;
		top: 48%;
		width: auto;
	}

	.hero-kicker {
		font-size: 12px;
	}

	h1 {
		font-size: clamp(44px, 14vw, 62px);
		line-height: 1.24;
	}

	.hero-description {
		font-size: 16px;
		line-height: 1.75;
	}

	.hero-actions {
		gap: 10px;
		margin-top: 26px;
	}

	.primary-action,
	.secondary-action {
		height: 44px;
		padding: 0 20px;
		font-size: 14px;
	}

	:deep(.el-carousel__arrow) {
		display: none;
	}
}
</style>
