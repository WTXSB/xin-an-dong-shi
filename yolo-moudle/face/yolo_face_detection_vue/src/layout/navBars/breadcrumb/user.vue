<template>
	<div class="header-actions">
		<button class="search-button" type="button" aria-label="搜索菜单" @click="searchRef?.openSearch()">
			<el-icon><ele-Search /></el-icon>
		</button>
		<el-dropdown trigger="click" @command="onCommand">
			<button class="profile-button" type="button">
				<img :src="state.avatar" alt="用户头像" />
				<span class="profile-name">{{ userInfos.userName || '心安用户' }}</span>
				<el-icon><ele-ArrowDown /></el-icon>
			</button>
			<template #dropdown>
				<el-dropdown-menu>
					<el-dropdown-item command="/personal"><el-icon><ele-User /></el-icon>个人中心</el-dropdown-item>
					<el-dropdown-item v-if="isAdmin" command="/usermanage"><el-icon><ele-Setting /></el-icon>用户管理</el-dropdown-item>
					<el-dropdown-item divided command="logout"><el-icon><ele-SwitchButton /></el-icon>退出登录</el-dropdown-item>
				</el-dropdown-menu>
			</template>
		</el-dropdown>
		<Search ref="searchRef" />
	</div>
</template>

<script setup lang="ts" name="layoutBreadcrumbUser">
import { defineAsyncComponent, ref, computed, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import { storeToRefs } from 'pinia';
import { useUserInfo } from '/@/stores/userInfo';
import request from '/@/utils/request';
import { Session } from '/@/utils/storage';

const Search = defineAsyncComponent(() => import('/@/layout/navBars/breadcrumb/search.vue'));
const router = useRouter();
const { userInfos } = storeToRefs(useUserInfo());
const searchRef = ref();
const state = reactive({ avatar: '' });
const isAdmin = computed(() => userInfos.value.role === 'admin' || userInfos.value.roles?.includes('admin'));

const loadAvatar = async () => {
	state.avatar = userInfos.value.photo || '';
	if (!userInfos.value.userName) return;
	try {
		const res = await request.get('/api/user/' + userInfos.value.userName);
		if (res.code == 0 && res.data?.avatar) state.avatar = res.data.avatar;
	} catch (_) {
		// 保留账户默认头像，头像加载失败不打断主导航。
	}
};

const onCommand = async (command: string) => {
	if (command !== 'logout') {
		await router.push(command);
		return;
	}
	try {
		await ElMessageBox.confirm('确定要退出当前账号吗？', '退出登录', {
			confirmButtonText: '确定退出', cancelButtonText: '再看看', type: 'warning',
		});
		try { await request.post('/api/user/logout'); }
		finally { Session.clear(); window.location.reload(); }
	} catch (_) {
		// 用户取消退出。
	}
};

onMounted(loadAvatar);
</script>

<style scoped lang="scss">
.header-actions { display: flex; align-items: center; gap: 8px; padding-right: 0; }
.search-button, .profile-button { border: 0; background: transparent; color: #5f5a52; cursor: pointer; }
.search-button { display: grid; place-items: center; width: 38px; height: 38px; border-radius: 50%; font-size: 19px; }
.search-button:hover { background: #fff2c5; color: #ad6238; }
.profile-button { display: flex; align-items: center; gap: 8px; height: 44px; padding: 0 4px 0 6px; border-radius: 22px; font-family: inherit; }
.profile-button:hover { background: #fff5d8; }
.profile-button img { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; background: #fff0bc; border: 2px solid #fff; box-shadow: 0 4px 12px rgba(159, 91, 42, 0.16); }
.profile-name { max-width: 88px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
@media (max-width: 1000px) {
	.header-actions { order: 3; padding-right: 0; gap: 2px; }
	.profile-name { display: none; }
	.profile-button { padding: 0 2px; }
}
</style>
