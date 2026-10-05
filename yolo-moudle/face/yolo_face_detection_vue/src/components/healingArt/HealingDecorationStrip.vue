<template>
	<div class="healing-scene-strip" :class="'healing-scene-strip--' + variant" aria-hidden="true">
		<div v-if="variant === 'rest'" class="connected-landscape"></div>
		<svg v-else class="scene-connection" viewBox="0 0 1200 132" preserveAspectRatio="xMidYMid slice" fill="none">
			<path v-if="variant === 'garden'" d="M-20 128Q130 38 290 97T600 83T890 89T1230 66V145H-20Z" fill="#e7eddf" opacity=".7" />
			<path v-else d="M-20 87Q100 116 195 68Q230 33 280 65Q340 15 388 63Q470 54 495 93Q610 121 710 65Q775 17 822 58Q890 22 920 75Q1020 102 1220 59V145H-20Z" fill="#fff5d9" opacity=".76" />
			<path d="M-20 117Q170 73 360 104T730 99T1230 108" stroke="#c7bda0" stroke-width="1.4" opacity=".55" />
			<path d="M-20 129Q170 89 360 115T730 112T1230 120" stroke="#e0cfac" stroke-width="5" opacity=".45" />
		</svg>
		<span class="strip-animal"></span>
		<HealingDoodle class="strip-cloud" :kind="variant === 'rest' ? 'rainbow' : 'cloud'" />
		<HealingDoodle class="strip-sprig" kind="sprig" />
		<HealingDoodle class="strip-flower" kind="flower" />
	</div>
</template>

<script setup lang="ts">
import HealingDoodle from './HealingDoodle.vue';
withDefaults(defineProps<{ variant?: string }>(), { variant: 'sky' });
</script>

<style scoped lang="scss">
.healing-scene-strip {
 position: relative;
 flex: 0 0 auto;
 width: 100%;
 min-width: 0;
 height: 132px;
 margin: 12px auto;
 overflow: hidden;
 pointer-events: none;
 user-select: none;
 --scene-size: 132px;
 .scene-connection { position: absolute; inset: 0; width: 100%; height: 100%; }
 .strip-animal { position: absolute; width: var(--scene-size); height: var(--scene-size); left: 8%; top: 0; background: var(--healing-atlas) var(--scene-a-x) var(--scene-y) / calc(var(--scene-size) * 4) calc(var(--scene-size) * 3) no-repeat; }
 .strip-cloud { position: absolute; width: 100px; height: 58px; left: 46%; top: 22px; opacity: .78; }
 .strip-sprig { position: absolute; width: 84px; height: 80px; right: 6%; bottom: 8px; }
 .strip-flower { position: absolute; width: 54px; height: 68px; right: 15%; bottom: 10px; }
}
.healing-scene-strip--garden {
 .strip-animal { left: auto; right: 10%; background-position: var(--scene-b-x) var(--scene-y); }
 .strip-cloud { left: 30%; top: 48px; width: 78px; }
 .strip-sprig { right: auto; left: 5%; transform: rotate(-14deg); }
 .strip-flower { right: auto; left: 17%; width: 70px; height: 90px; }
}
.healing-scene-strip--rest {
 height: auto;
 aspect-ratio: 4 / 1;
 max-width: 1280px;
 margin: 26px auto 0;
 border-radius: 28px;
 background: #fbf8f1;
 box-shadow: 0 12px 30px rgba(115, 104, 72, .06);
 .connected-landscape { position: absolute; top: 0; left: 50%; transform: translateX(-50%); width: max(100%, 720px); aspect-ratio: 4 / 1; background: var(--healing-landscapes) var(--landscape-x) var(--landscape-y) / 200% 300% no-repeat; }
 .strip-animal, .strip-cloud, .strip-sprig, .strip-flower { display: none; }
 .strip-animal { left: 42%; background-position: var(--scene-b-x) var(--scene-y); }
 .strip-cloud { left: 8%; top: 35px; }
 .strip-sprig { right: 15%; transform: rotate(13deg); }
 .strip-flower { right: 7%; top: 6px; }
}
@media (max-width: 760px) {
 .healing-scene-strip { height: 106px; margin: 10px auto; --scene-size: 104px; }
 .healing-scene-strip .strip-animal { left: 2%; }
 .healing-scene-strip .strip-cloud { left: 46%; width: 70px; height: 44px; top: 10px; }
 .healing-scene-strip .strip-sprig { width: 48px; height: 56px; right: 3%; }
 .healing-scene-strip .strip-flower { width: 36px; height: 46px; right: 17%; }
 .healing-scene-strip--garden .strip-animal { left: auto; right: 0; }
 .healing-scene-strip--garden .strip-cloud { left: 31%; top: 43px; width: 56px; }
 .healing-scene-strip--garden .strip-sprig { left: 0; }
 .healing-scene-strip--garden .strip-flower { left: 13%; width: 48px; height: 60px; }
 .healing-scene-strip--rest .strip-animal { left: 35%; }
 .healing-scene-strip--rest .strip-cloud { left: 1%; }
 .healing-scene-strip--rest .strip-sprig { right: 3%; }
 .healing-scene-strip--rest .strip-flower { display: none; }
 .healing-scene-strip--rest { height: 180px; aspect-ratio: auto; margin-top: 20px; border-radius: 22px; }
 .healing-scene-strip--rest .connected-landscape { transform: translateX(calc(-50% + var(--landscape-mobile-shift, 0px))); }
}
</style>
