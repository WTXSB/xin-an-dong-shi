<template>
	<svg v-if="kind === 'cloud'" viewBox="0 0 120 70" fill="none" aria-hidden="true">
		<path d="M17 51C-1 48 2 29 18 28C18 7 47 1 57 22C67 9 90 13 93 30C119 27 125 50 107 55C80 61 44 60 17 51Z" fill="#fffaf0" stroke="#d5b77f" stroke-width="1.3" />
		<path d="M24 46C40 52 57 50 68 48M80 45L99 47" stroke="#eed9ac" stroke-linecap="round" />
		<path d="M77 6L80 1M84 10L89 8" stroke="#dbb867" stroke-linecap="round" />
	</svg>
	<svg v-else-if="kind === 'rainbow'" viewBox="0 0 120 70" fill="none" aria-hidden="true">
		<path d="M17 58C18 1 101 1 103 58" stroke="#efd2b7" stroke-width="9" stroke-linecap="round" />
		<path d="M29 58C30 18 90 17 91 58" stroke="#ecdca8" stroke-width="7" stroke-linecap="round" />
		<path d="M40 58C41 33 79 33 80 58" stroke="#c8d2b7" stroke-width="6" stroke-linecap="round" />
		<path d="M5 63C0 53 13 46 20 53C28 45 44 53 39 62M79 62C74 51 90 44 98 53C106 45 119 53 116 62" fill="#fffaf0" stroke="#d9c8a5" />
	</svg>
	<svg v-else-if="kind === 'flower'" viewBox="0 0 100 100" fill="none" aria-hidden="true">
		<path d="M50 95C53 72 45 60 49 39" stroke="#869877" stroke-width="1.8" stroke-linecap="round" />
		<path d="M51 72C66 48 82 63 52 80M49 68C22 45 18 73 50 78" fill="#c3cfaa" stroke="#94a183" />
		<path d="M49 13C60 -1 74 15 64 27C85 18 88 43 68 43C83 57 61 71 52 52C45 76 23 56 37 43C9 43 20 19 38 28C25 9 47 -1 49 13Z" fill="var(--scene-flower, #f3d7ba)" stroke="#cba581" stroke-width="1.2" />
		<circle cx="51" cy="35" r="8" fill="#e9c46d" />
		<circle cx="48" cy="33" r="2" fill="#fff0bd" />
	</svg>
	<svg v-else viewBox="0 0 110 90" fill="none" aria-hidden="true">
		<path d="M14 83C43 56 62 34 93 10" stroke="#9a9e76" stroke-width="1.6" stroke-linecap="round" />
		<path d="M30 67C9 39 13 29 37 53M48 48C35 21 46 17 55 40M68 31C61 7 72 2 76 24M39 59C66 58 69 70 43 65M59 39C83 39 86 51 64 45M80 22C101 24 108 13 92 15" fill="#c0cda4" stroke="#929c77" stroke-width=".8" />
		<path d="M21 18L24 12M18 12L27 16" stroke="#efc377" stroke-linecap="round" />
	</svg>
</template>

<script setup lang="ts">
defineProps<{ kind?: 'cloud' | 'flower' | 'sprig' | 'rainbow' }>();
</script>
