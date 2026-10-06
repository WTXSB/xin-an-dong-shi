<template>
	<div class="el-menu-horizontal-warp">
		<el-scrollbar @wheel.native.prevent="onElMenuHorizontalScroll" ref="elMenuHorizontalScrollRef">
			<el-menu router :default-active="state.defaultActive" :ellipsis="false" background-color="transparent" mode="horizontal">
				<template v-for="val in menuLists">
					<el-sub-menu :index="val.path" v-if="val.children && val.children.length > 0" :key="val.path" :class="{ 'curtain-open': expandedNav === val.path }" :aria-label="$t(val.meta.title)" @click="revealNav(val.path)">
						<template #title>
							<SvgIcon :name="val.path === '/aboutProduct' ? 'ele-InfoFilled' : val.meta.icon" :size="20" />
							<HealingNavCurtain v-if="expandedNav === val.path" :key="curtainSequence" />
							<span class="curtain-label">{{ $t(val.meta.title) }}</span>
						</template>
						<SubItem :chil="val.children" />
					</el-sub-menu>
					<template v-else>
						<el-menu-item :index="val.path" :key="val.path" :class="{ 'curtain-open': expandedNav === val.path }" :aria-label="$t(val.meta.title)" @click="revealNav(val.path)">
							<template #title v-if="!val.meta.isLink || (val.meta.isLink && val.meta.isIframe)">
								<SvgIcon :name="val.path === '/aboutProduct' ? 'ele-InfoFilled' : val.meta.icon" :size="20" />
								<HealingNavCurtain v-if="expandedNav === val.path" :key="curtainSequence" />
							<span class="curtain-label">{{ $t(val.meta.title) }}</span>
							</template>
							<template #title v-else>
								<a class="w100" @click.prevent="onALinkClick(val)">
									<SvgIcon :name="val.path === '/aboutProduct' ? 'ele-InfoFilled' : val.meta.icon" :size="20" />
									<HealingNavCurtain v-if="expandedNav === val.path" :key="curtainSequence" />
							<span class="curtain-label">{{ $t(val.meta.title) }}</span>
								</a>
							</template>
						</el-menu-item>
					</template>
				</template>
			</el-menu>
		</el-scrollbar>
	</div>
</template>

<script setup lang="ts" name="navMenuHorizontal">
import { defineAsyncComponent, reactive, computed, onMounted, nextTick, ref, watch } from 'vue';
import { useRoute, RouteRecordRaw } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useRoutesList } from '/@/stores/routesList';
import { useThemeConfig } from '/@/stores/themeConfig';
import other from '/@/utils/other';
import mittBus from '/@/utils/mitt';
import HealingNavCurtain from './HealingNavCurtain.vue';

// 引入组件
const SubItem = defineAsyncComponent(() => import('/@/layout/navMenu/subItem.vue'));

// 定义父组件传过来的值
const props = defineProps({
	// 菜单列表
	menuList: {
		type: Array<RouteRecordRaw>,
		default: () => [],
	},
});

// 定义变量内容
const elMenuHorizontalScrollRef = ref();
// 导航的视觉展开状态跟随已完成的路由；原有跳转与菜单数据保持不变。
const expandedNav = ref('');
const curtainSequence = ref(0);
const revealNav = (path: string) => {
	expandedNav.value = path; curtainSequence.value += 1;
	// Includes repeat clicks on the current route; the server claims each time slot only once.
	if (path === '/dataView') nextTick(() => window.dispatchEvent(new Event('mindease:diary-enter')));
};
const stores = useRoutesList();
const storesThemeConfig = useThemeConfig();
const { routesList } = storeToRefs(stores);
const { themeConfig } = storeToRefs(storesThemeConfig);
const route = useRoute();
const state = reactive({
	defaultActive: '' as string | undefined,
});

// 获取父级菜单数据
const menuLists = computed(() => {
	return <RouteItems>props.menuList;
});
// 设置横向滚动条可以鼠标滚轮滚动
const onElMenuHorizontalScroll = (e: WheelEventType) => {
	const eventDelta = e.wheelDelta || -e.deltaY * 40;
	elMenuHorizontalScrollRef.value.$refs.wrapRef.scrollLeft = elMenuHorizontalScrollRef.value.$refs.wrapRef.scrollLeft + eventDelta / 4;
};
// 初始化数据，页面刷新时，滚动条滚动到对应位置
const initElMenuOffsetLeft = () => {
	nextTick(() => {
		let els = <HTMLElement>document.querySelector('.el-menu.el-menu--horizontal li.is-active');
		if (!els) return false;
		elMenuHorizontalScrollRef.value.$refs.wrapRef.scrollLeft = els.offsetLeft;
	});
};
// 路由过滤递归函数
const filterRoutesFun = <T extends RouteItem>(arr: T[]): T[] => {
	return arr
		.filter((item: T) => !item.meta?.isHide)
		.map((item: T) => {
			item = Object.assign({}, item);
			if (item.children) item.children = filterRoutesFun(item.children);
			return item;
		});
};
// 传送当前子级数据到菜单中
const setSendClassicChildren = (path: string) => {
	const currentPathSplit = path.split('/');
	let currentData: MittMenu = { children: [] };
	filterRoutesFun(routesList.value).map((v, k) => {
		if (v.path === `/${currentPathSplit[1]}`) {
			v['k'] = k;
			currentData['item'] = { ...v };
			currentData['children'] = [{ ...v }];
			if (v.children) currentData['children'] = v.children;
		}
	});
	return currentData;
};
// 设置页面当前路由高亮
const setCurrentRouterHighlight = (currentRoute: RouteToFrom) => {
	const { path, meta } = currentRoute;
	if (themeConfig.value.layout === 'classic') {
		state.defaultActive = `/${path?.split('/')[1]}`;
	} else {
		const pathSplit = meta?.isDynamic ? meta.isDynamicPath!.split('/') : path!.split('/');
		if (pathSplit.length >= 4 && meta?.isHide) state.defaultActive = pathSplit.splice(0, 3).join('/');
		else state.defaultActive = path;
	}
};
// 打开外部链接
const onALinkClick = (val: RouteItem) => {
	other.handleOpenLink(val);
};
// 页面加载时
onMounted(() => {
	initElMenuOffsetLeft();
});
// 布局导航不是页面路由组件：监听实际路由，覆盖页面按钮、前进/后退和刷新。
watch([() => route.fullPath, menuLists], () => {
	setCurrentRouterHighlight(route);
	const containsCurrentRoute = (item: RouteItem): boolean =>
		item.path === state.defaultActive || route.matched.some((record) => record.path === item.path) ||
		Boolean(item.children?.some(containsCurrentRoute));
	const currentMenu = menuLists.value.find(containsCurrentRoute);
	const path = currentMenu?.path || '';
	if (expandedNav.value !== path) {
		expandedNav.value = path;
		curtainSequence.value += 1;
	}
	// 修复经典布局开启切割菜单时，点击tagsView后左侧导航菜单数据不变的问题
	let { layout, isClassicSplitMenu } = themeConfig.value;
	if (layout === 'classic' && isClassicSplitMenu) {
		mittBus.emit('setSendClassicChildren', setSendClassicChildren(route.path));
	}
	initElMenuOffsetLeft();
}, { immediate: true, flush: 'post' });
</script>

<style scoped lang="scss">
.el-menu-horizontal-warp {
	flex: 1;
	overflow: hidden;
	margin: 0 24px 0 8px;
	:deep(.el-scrollbar__bar.is-vertical) {
		display: none;
	}
	:deep(a) {
		width: 100%;
	}
	.el-menu.el-menu--horizontal {
		display: flex;
		align-items: center;
		gap: 4px;
		height: 100%;
		width: 100%;
		box-sizing: border-box;
		border-bottom: 0;
	}
	:deep(.el-menu--horizontal > .el-menu-item),
	:deep(.el-menu--horizontal > .el-sub-menu .el-sub-menu__title) {
		height: 42px;
		padding: 0 15px;
		border: 0 !important;
		border-radius: 15px;
		color: #65564b;
		font-size: 15px;
		line-height: 42px;
		transition: color .2s ease, background .2s ease, transform .2s ease;
	}
	:deep(.el-menu--horizontal > .el-menu-item:hover),
	:deep(.el-menu--horizontal > .el-sub-menu:hover .el-sub-menu__title) {
		color: #a95d34;
		background: #fff7dc;
		transform: translateY(-1px);
	}
	:deep(.el-menu--horizontal > .el-menu-item.is-active),
	:deep(.el-menu--horizontal > .el-sub-menu.is-active .el-sub-menu__title) {
		color: #9f552f !important;
		background: transparent !important;
		box-shadow: none !important;
	}
	:deep(.el-menu-item .svg-icon),
	:deep(.el-sub-menu__title .svg-icon) {
		margin-right: 7px;
		color: #c57a48;
	}
}
</style>

<style scoped lang="scss">
.el-menu-horizontal-warp {
 :deep(.el-menu--horizontal > .el-menu-item),
 :deep(.el-menu--horizontal > .el-sub-menu > .el-sub-menu__title) {
  position: relative;
  isolation: isolate;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 54px;
  height: 54px !important;
  padding: 0 17px;
  overflow: hidden;
  background: transparent;
  border-radius: 15px;
  line-height: 20px;
  &::before, &::after { content: none; }
 }
 :deep(.curtain-label) {
  display: inline-block;
  position: relative;
  max-width: 0;
  margin-left: 0;
  overflow: hidden;
  opacity: 0;
  white-space: nowrap;
  transition: max-width .7s ease, opacity 1.2s ease .55s, margin-left .7s ease;
 }
 :deep(.el-menu--horizontal > .el-menu-item > i),
 :deep(.el-menu--horizontal > .el-menu-item a > i),
 :deep(.el-menu--horizontal > .el-sub-menu > .el-sub-menu__title > i:not(.el-sub-menu__icon-arrow)) {
  position: relative;
  margin-right: 0;
  width: 20px;
  height: 20px;
  line-height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
 }
 :deep(.el-sub-menu__title .el-sub-menu__icon-arrow) { display: none; }
 :deep(.el-menu--horizontal > .curtain-open .curtain-label) { max-width: 150px; opacity: 1; margin-left: 8px; }
 :deep(.el-menu--horizontal > .el-menu-item.curtain-open),
 :deep(.el-menu--horizontal > .el-sub-menu.curtain-open > .el-sub-menu__title) {
  padding-inline: 26px;
  background: transparent !important;
  box-shadow: none !important;
 }
 :deep(.el-menu--horizontal > .el-menu-item:focus-visible),
 :deep(.el-menu--horizontal > .el-sub-menu > .el-sub-menu__title:focus-visible) {
  outline: 2px solid #a67236;
  outline-offset: -2px;
 }
 @media (prefers-reduced-motion: reduce) { :deep(.curtain-label) { transition: none; } }
 :deep(.el-menu--horizontal > .el-menu-item),
 :deep(.el-menu--horizontal > .el-sub-menu > .el-sub-menu__title) {
  background: transparent !important;
  background-image: none !important;
  box-shadow: none !important;
 }
}
</style>
