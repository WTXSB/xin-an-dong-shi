<template>
	<div class="layout-navbars-breadcrumb-index">
		<Logo v-if="setIsShowLogo" />
		<Horizontal v-if="isLayoutTransverse" class="desktop-navigation" :menu-list="state.menuList" />
		<button class="mobile-menu-button" type="button" aria-label="打开导航菜单" @click="state.drawerOpen = true">
			<el-icon><ele-Menu /></el-icon>
		</button>
		<User />

		<el-drawer v-model="state.drawerOpen" class="mobile-nav-drawer" direction="ltr" size="82%" :with-header="false">
			<div class="drawer-brand">
				<span class="drawer-mark">心</span>
				<div><strong>心安动识</strong><small>让每一次感知都被温柔接住</small></div>
			</div>
			<el-menu router :default-active="route.path" @select="state.drawerOpen = false">
				<template v-for="item in state.menuList" :key="item.path">
					<el-sub-menu v-if="item.children && item.children.length" :index="item.path">
						<template #title><SvgIcon :name="item.meta.icon" /><span>{{ item.meta.title }}</span></template>
						<SubItem :chil="item.children" />
					</el-sub-menu>
					<el-menu-item v-else :index="item.path">
						<SvgIcon :name="item.meta.icon" /><span>{{ item.meta.title }}</span>
					</el-menu-item>
				</template>
			</el-menu>
		</el-drawer>
	</div>
</template>

<script setup lang="ts" name="layoutBreadcrumbIndex">
import { defineAsyncComponent, computed, reactive, onMounted, onUnmounted } from 'vue';
import { useRoute } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useRoutesList } from '/@/stores/routesList';
import { useThemeConfig } from '/@/stores/themeConfig';
import mittBus from '/@/utils/mitt';

const User = defineAsyncComponent(() => import('/@/layout/navBars/breadcrumb/user.vue'));
const Logo = defineAsyncComponent(() => import('/@/layout/logo/index.vue'));
const Horizontal = defineAsyncComponent(() => import('/@/layout/navMenu/horizontal.vue'));
const SubItem = defineAsyncComponent(() => import('/@/layout/navMenu/subItem.vue'));

const stores = useRoutesList();
const storesThemeConfig = useThemeConfig();
const { themeConfig } = storeToRefs(storesThemeConfig);
const { routesList } = storeToRefs(stores);
const route = useRoute();
const state = reactive({ menuList: [] as RouteItems, drawerOpen: false });
const menuOrder = ['/homePage', '/aboutProduct', '/diseaseDetection', '/dataView', '/trashMap', '/trashRecords', '/smartChat'];

const setIsShowLogo = computed(() => themeConfig.value.isShowLogo && themeConfig.value.layout === 'transverse');
const isLayoutTransverse = computed(() => themeConfig.value.layout === 'transverse');

const filterRoutesFun = <T extends RouteItem>(arr: T[]): T[] =>
	arr.filter((item: T) => !item.meta?.isHide).map((item: T) => {
		const copied = Object.assign({}, item);
		if (copied.children) copied.children = filterRoutesFun(copied.children);
		return copied;
	});

const setFilterRoutes = () => {
	state.menuList = filterRoutesFun(routesList.value).sort((a, b) => {
		const aIndex = menuOrder.indexOf(a.path);
		const bIndex = menuOrder.indexOf(b.path);
		return (aIndex < 0 ? 99 : aIndex) - (bIndex < 0 ? 99 : bIndex);
	});
};

onMounted(() => {
	setFilterRoutes();
	mittBus.on('getBreadcrumbIndexSetFilterRoutes', setFilterRoutes);
});
onUnmounted(() => mittBus.off('getBreadcrumbIndexSetFilterRoutes', setFilterRoutes));
</script>

<style scoped lang="scss">
.layout-navbars-breadcrumb-index {
	height: 66px;
	display: flex;
	align-items: center;
	padding: 0 clamp(10px, 1.7vw, 28px);
	background: rgba(255, 254, 248, 0.94);
	border-bottom: 1px solid rgba(151, 107, 66, 0.14);
	box-shadow: 0 8px 30px rgba(127, 87, 46, 0.07);
	backdrop-filter: blur(18px);
}
.mobile-menu-button { display: none; }
.drawer-brand {
	display: flex; align-items: center; gap: 12px; padding: 26px 20px 22px; color: #58483b;
	.drawer-mark { display: grid; place-items: center; width: 42px; height: 42px; border-radius: 50%; background: #fff0b8; color: #b56538; font-family: 'MindEase Art'; font-size: 24px; }
	strong { display: block; font-family: 'MindEase Art'; font-size: 24px; font-weight: 400; }
	small { display: block; margin-top: 3px; color: #9a8979; font-size: 12px; }
}
:global(.mobile-nav-drawer .el-drawer__body) { padding: 0; background: #fffdf7; }
:global(.mobile-nav-drawer .el-menu) { border-right: 0; background: transparent; }
:global(.mobile-nav-drawer .el-menu-item),
:global(.mobile-nav-drawer .el-sub-menu__title) { height: 54px; color: #66584b; font-size: 16px; }
:global(.mobile-nav-drawer .el-menu-item.is-active) { color: #a95d34; background: #fff1c4; }

@media (max-width: 1000px) {
	.layout-navbars-breadcrumb-index { justify-content: space-between; height: 60px; padding: 0 12px; }
	.desktop-navigation { display: none; }
	.mobile-menu-button {
		display: grid; place-items: center; order: 2; width: 38px; height: 38px; margin-left: auto;
		border: 1px solid rgba(185, 119, 57, 0.12); border-radius: 14px; background: #fff1c4; color: #a55f38; font-size: 20px; cursor: pointer;
	}
}
</style>
