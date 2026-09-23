<template>
	<div class="layout-logo" v-if="setShowLogo" @click="onThemeConfigChange">
		<div class="soft-logo-mark" aria-hidden="true">
			<span class="soft-logo-heart"></span>
			<span class="soft-logo-dot"></span>
		</div>
		<span>{{ themeConfig.globalTitle }}</span>
	</div>
	<div class="layout-logo-size" v-else @click="onThemeConfigChange">
		<div class="soft-logo-mark mini" aria-hidden="true">
			<span class="soft-logo-heart"></span>
			<span class="soft-logo-dot"></span>
		</div>
	</div>
</template>

<script setup lang="ts" name="layoutLogo">
import { computed } from 'vue';
import { storeToRefs } from 'pinia';
import { useThemeConfig } from '/@/stores/themeConfig';

// 定义变量内容
const storesThemeConfig = useThemeConfig();
const { themeConfig } = storeToRefs(storesThemeConfig);

// 设置 logo 的显示。classic 经典布局默认显示 logo
const setShowLogo = computed(() => {
	let { isCollapse, layout } = themeConfig.value;
	return !isCollapse || layout === 'classic' || document.body.clientWidth < 1000;
});
// logo 点击实现菜单展开/收起
const onThemeConfigChange = () => {
	if (themeConfig.value.layout === 'transverse') return false;
	themeConfig.value.isCollapse = !themeConfig.value.isCollapse;
};
</script>

<style scoped lang="scss">
.layout-logo {
	width: 220px;
	height: 50px;
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: rgb(0 21 41 / 2%) 0px 1px 4px;
	color: #6d4c36;
	font-size: 16px;
	font-weight: 800;
	cursor: pointer;
	animation: logoAnimation 0.3s ease-in-out;
	span {
		white-space: nowrap;
		display: inline-block;
	}
	&:hover {
		span {
			color: var(--color-primary-light-2);
		}
	}
}
.layout-logo-size {
	width: 100%;
	height: 50px;
	display: flex;
	cursor: pointer;
	animation: logoAnimation 0.3s ease-in-out;
	align-items: center;
	justify-content: center;
	&:hover {
		.soft-logo-mark {
			animation: logoAnimation 0.3s ease-in-out;
		}
	}
}

.soft-logo-mark {
	position: relative;
	width: 34px;
	height: 34px;
	margin-right: 10px;
	border-radius: 50%;
	background: linear-gradient(145deg, #8fcfa7, #f4c978);
	box-shadow: 0 8px 18px rgba(109, 76, 54, 0.16);
	flex: 0 0 auto;
}

.soft-logo-mark.mini {
	margin-right: 0;
	width: 30px;
	height: 30px;
}

.soft-logo-heart {
	position: absolute;
	left: 9px;
	top: 10px;
	width: 15px;
	height: 15px;
	background: #fff8ef;
	transform: rotate(45deg);
	border-radius: 4px;
}

.soft-logo-heart::before,
.soft-logo-heart::after {
	content: '';
	position: absolute;
	width: 15px;
	height: 15px;
	border-radius: 50%;
	background: #fff8ef;
}

.soft-logo-heart::before {
	left: -7px;
	top: 0;
}

.soft-logo-heart::after {
	left: 0;
	top: -7px;
}

.soft-logo-dot {
	position: absolute;
	right: 7px;
	bottom: 7px;
	width: 7px;
	height: 7px;
	border-radius: 50%;
	background: #7a5a43;
	box-shadow: 0 0 0 3px rgba(255, 248, 239, 0.72);
}
</style>
