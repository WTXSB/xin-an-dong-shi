<template><span class="diary-animal" aria-hidden="true" :style="{ '--animal-x': position[0], '--animal-y': position[1] }"></span></template>
<script setup lang="ts">
import { computed } from 'vue';
import { moodLevel } from './calendar';
const props = defineProps<{ score: number }>();
// Existing clean, transparent twelve-expression atlas; fixed-ratio windows never stretch.
const position = computed(
	() =>
		[
			[178, 540],
			[916, 540],
			[178, 858],
			[552, 195],
			[178, 195],
		][moodLevel(props.score)]
);
</script>
<style scoped>
.diary-animal {
	--animal-size: 66px;
	width: var(--animal-size);
	height: var(--animal-size);
	flex: 0 0 auto;
	display: inline-block;
	background-image: url('../../assets/healing-frames/animal-emotions-twelve-v2.png');
	background-repeat: no-repeat;
	background-size: calc(var(--animal-size) * 4.525) calc(var(--animal-size) * 3.39375);
	background-position: calc(var(--animal-size) * (0.5 - var(--animal-x) / 320)) calc(var(--animal-size) * (0.5 - var(--animal-y) / 320));
}
</style>
