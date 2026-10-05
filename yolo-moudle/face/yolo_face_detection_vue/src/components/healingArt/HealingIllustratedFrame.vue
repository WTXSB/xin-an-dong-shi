<template>
	<div class="healing-frame" :class="'healing-frame--' + variant" aria-hidden="true">
		<svg class="frame-wash" viewBox="0 0 1000 1000" preserveAspectRatio="none" fill="none">
			<path d="M53 24Q500 13 947 26Q982 26 974 84Q989 500 975 939Q975 984 917 975Q500 987 66 975Q21 975 25 916Q13 500 26 72Q26 24 53 24Z" stroke="currentColor" stroke-width="1.2" vector-effect="non-scaling-stroke" />
		</svg>
		<span class="frame-piece corner corner--tl"></span>
		<span class="frame-piece corner corner--tr"></span>
		<span class="frame-piece scene-animal scene-animal--a"></span>
		<span class="frame-piece scene-animal scene-animal--b"></span>
		<HealingDoodle class="frame-doodle frame-doodle--top" :kind="variant === 'garden' ? 'sprig' : variant === 'rest' ? 'rainbow' : 'cloud'" />
		<HealingDoodle class="frame-doodle frame-doodle--left" :kind="variant === 'garden' ? 'flower' : variant === 'rest' ? 'cloud' : 'sprig'" />
		<HealingDoodle class="frame-doodle frame-doodle--right" :kind="variant === 'garden' ? 'cloud' : variant === 'rest' ? 'sprig' : 'flower'" />
		<HealingDoodle class="frame-doodle frame-doodle--bottom" :kind="variant === 'garden' ? 'flower' : variant === 'rest' ? 'cloud' : 'sprig'" />
	</div>
</template>

<script setup lang="ts">
import HealingDoodle from './HealingDoodle.vue';
withDefaults(defineProps<{ variant?: string }>(), { variant: 'clouds' });
</script>

<style scoped lang="scss">
.healing-frame {
 position: absolute;
 inset: 0;
 z-index: -1;
 overflow: hidden;
 border-radius: inherit;
 pointer-events: none;
 user-select: none;
 opacity: .86;
 .frame-wash { position: absolute; inset: 5px; width: calc(100% - 10px); height: calc(100% - 10px); color: #d5c69d; opacity: .55; }
}
.frame-piece { position: absolute; display: block; background-repeat: no-repeat; }
.corner { width: 94px; height: 64px; background-image: var(--healing-frame); background-size: 276px 184px; }
.corner--tl { top: 0; left: 0; background-position: left top; }
.corner--tr { top: 0; right: 0; background-position: right top; }
.scene-animal {
 --scene-size: 76px;
 width: var(--scene-size);
 height: var(--scene-size);
 bottom: 0;
 background-image: var(--healing-atlas);
 background-size: calc(var(--scene-size) * 4) calc(var(--scene-size) * 3);
}
.scene-animal--a { left: 2px; background-position: var(--scene-a-x) var(--scene-y); }
.scene-animal--b { right: 2px; background-position: var(--scene-b-x) var(--scene-y); }
.frame-doodle { position: absolute; }
.frame-doodle--top { width: 88px; height: 44px; top: 3px; left: 48%; }
.frame-doodle--left { width: 48px; height: 64px; top: 42%; left: 2px; }
.frame-doodle--right { width: 44px; height: 60px; top: 57%; right: 4px; }
.frame-doodle--bottom { width: 76px; height: 44px; bottom: 2px; left: 38%; transform: rotate(17deg); }
.healing-frame--garden {
 .frame-doodle--top { left: 27%; transform: rotate(-9deg); }
 .frame-doodle--left { top: 62%; transform: rotate(-12deg); }
 .frame-doodle--right { top: 26%; }
 .frame-doodle--bottom { left: 61%; transform: rotate(-13deg); }
 .corner--tr { background-position: right bottom; }
}
.healing-frame--rest {
 .corner--tl { background-position: left bottom; }
 .frame-doodle--top { left: 58%; }
 .frame-doodle--left { top: 28%; }
 .frame-doodle--right { top: 64%; transform: rotate(12deg); }
 .scene-animal--a { left: auto; right: 2px; }
 .scene-animal--b { right: auto; left: 2px; }
}
@media (max-width: 760px) {
 .corner { width: 68px; height: 48px; background-size: 216px 144px; }
 .scene-animal { --scene-size: 56px; }
 .frame-doodle--top { width: 62px; height: 35px; }
 .frame-doodle--left { width: 28px; height: 46px; }
 .frame-doodle--right { width: 28px; height: 44px; }
 .frame-doodle--bottom { width: 55px; height: 36px; }
}
</style>
