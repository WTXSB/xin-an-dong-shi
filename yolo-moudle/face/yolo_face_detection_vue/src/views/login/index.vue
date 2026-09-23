<template>
	<div class="login-container">
		<section class="brand-panel">
			<p class="eyebrow">心安动识</p>
			<h1>看见焦虑，遇见安宁</h1>
			<p class="brand-copy">
				我们用视觉模型帮助你看见情绪线索，再用温柔的练习与陪伴，帮你一点点找回节奏、内驱力和放松的能力。
			</p>
			<div class="principles">
				<span>陪伴而非打扰</span>
				<span>理解与接住</span>
				<span>觉察先于改变</span>
			</div>
		</section>

		<section class="login-box">
			<div class="title">
				<h2>欢迎回来</h2>
				<p>先深呼吸一下，再进入今天的觉察空间。</p>
			</div>

			<div class="quote-card">
				<span>今日给你的微光</span>
				<p>{{ currentQuote.text }}</p>
				<strong>{{ currentQuote.author }}</strong>
			</div>

			<el-form :model="ruleForm" :rules="registerRules" ref="ruleFormRef">
				<el-form-item prop="username">
					<el-input v-model="ruleForm.username" placeholder="演示账号 demo" prefix-icon="User" class="custom-input" />
				</el-form-item>

				<el-form-item prop="password">
					<el-input v-model="ruleForm.password" type="password" placeholder="演示密码 123456" prefix-icon="Lock" show-password class="custom-input" />
				</el-form-item>

				<el-form-item>
					<el-button class="login-btn" @click="submitForm(ruleFormRef)">进入心灵首页</el-button>
				</el-form-item>
			</el-form>

			<div class="entry-actions">
				<el-button text @click="goRegister">新用户注册</el-button>
				<el-button text @click="visitorSignIn">游客体验</el-button>
			</div>

			<div class="demo-tip">
				演示账号：<strong>demo</strong> / <strong>123456</strong>
			</div>
		</section>
	</div>
</template>

<script lang="ts" setup>
import { reactive, computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useI18n } from 'vue-i18n';
import Cookies from 'js-cookie';
import { storeToRefs } from 'pinia';
import { useThemeConfig } from '/@/stores/themeConfig';
import { initFrontEndControlRoutes } from '/@/router/frontEnd';
import { initBackEndControlRoutes } from '/@/router/backEnd';
import { Session } from '/@/utils/storage';
import { formatAxis } from '/@/utils/formatTime';
import { NextLoading } from '/@/utils/loading';
import type { FormInstance, FormRules } from 'element-plus';
import request from '/@/utils/request';

const { t } = useI18n();
const storesThemeConfig = useThemeConfig();
const { themeConfig } = storeToRefs(storesThemeConfig);
const route = useRoute();
const router = useRouter();
const ruleFormRef = ref<FormInstance>();

const quotes = [
	{ text: '天行健，君子以自强不息。', author: '《周易》' },
	{ text: '知之者不如好之者，好之者不如乐之者。', author: '孔子' },
	{ text: '路漫漫其修远兮，吾将上下而求索。', author: '屈原' },
	{ text: '每一个不曾起舞的日子，都是对生命的辜负。', author: '尼采' },
	{ text: 'The only way to do great work is to love what you do.', author: 'Steve Jobs' },
	{ text: 'In the middle of difficulty lies opportunity.', author: 'Albert Einstein' },
	{ text: '你不需要一下子变好，只要愿意向前一点点。', author: '心安动识' },
];

const currentQuote = ref(quotes[Math.floor(Math.random() * quotes.length)]);

const ruleForm = reactive({
	username: 'demo',
	password: '123456',
});

const registerRules = reactive<FormRules>({
	username: [
		{ required: true, message: '请输入账号', trigger: 'blur' },
		{ min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' },
	],
	password: [
		{ required: true, message: '请输入密码', trigger: 'blur' },
		{ min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' },
	],
});

const currentTime = computed(() => {
	return formatAxis(new Date());
});

const onSignIn = async () => {
	Session.set('token', Math.random().toString(36).substr(0));
	Cookies.set('userName', ruleForm.username);
	if (!themeConfig.value.isRequestRoutes) {
		const isNoPower = await initFrontEndControlRoutes();
		signInSuccess(isNoPower);
	} else {
		const isNoPower = await initBackEndControlRoutes();
		signInSuccess(isNoPower);
	}
};

const signInSuccess = (isNoPower: boolean | undefined) => {
	if (isNoPower) {
		ElMessage.warning('暂时没有访问权限');
		Session.clear();
	} else {
		if (route.query?.redirect) {
			router.push({
				path: <string>route.query?.redirect,
				query: Object.keys(<string>route.query?.params).length > 0 ? JSON.parse(<string>route.query?.params) : '',
			});
		} else {
			router.push('/');
		}
		ElMessage.success(`${currentTime.value}，${t('message.signInText')}`);
		NextLoading.start();
	}
};

const submitForm = (formEl: FormInstance | undefined) => {
	if (!formEl) return;
	formEl.validate((valid) => {
		if (!valid) return false;
		request.post('/api/user/login', ruleForm).then((res) => {
			if (res.code == 0) {
				Cookies.set('role', res.data.role);
				onSignIn();
			} else {
				ElMessage({
					type: 'error',
					message: res.msg,
				});
			}
		});
	});
};

const visitorSignIn = () => {
	ruleForm.username = `visitor_${Date.now().toString().slice(-6)}`;
	Cookies.set('role', 'others');
	ElMessage.success('已为你开启游客体验，体验数据不会绑定到真实身份。');
	onSignIn();
};

const goRegister = () => {
	router.push('/register');
};
</script>

<style scoped>
.login-container {
	min-height: 100vh;
	display: grid;
	grid-template-columns: minmax(0, 1fr) 420px;
	align-items: center;
	gap: 28px;
	padding: 42px;
	background:
		linear-gradient(135deg, rgba(244, 250, 248, 0.94), rgba(252, 248, 241, 0.95)),
		linear-gradient(90deg, rgba(123, 200, 164, 0.18), rgba(74, 144, 217, 0.12));
}

.brand-panel,
.login-box {
	border: 1px solid rgba(91, 124, 121, 0.14);
	background: rgba(255, 255, 255, 0.84);
	border-radius: 8px;
	box-shadow: 0 18px 40px rgba(54, 88, 86, 0.1);
}

.brand-panel {
	min-height: 520px;
	padding: 54px;
	display: flex;
	flex-direction: column;
	justify-content: center;
}

.eyebrow {
	color: #4f8f76;
	font-size: 14px;
	font-weight: 800;
}

.brand-panel h1 {
	margin: 12px 0 18px;
	color: #1f3d3a;
	font-size: 44px;
	line-height: 1.15;
	letter-spacing: 0;
}

.brand-copy {
	max-width: 720px;
	margin: 0;
	color: #61716f;
	font-size: 17px;
	line-height: 1.9;
}

.principles {
	display: flex;
	flex-wrap: wrap;
	gap: 12px;
	margin-top: 28px;
}

.principles span {
	padding: 10px 14px;
	border-radius: 8px;
	background: #f2faf6;
	color: #3f8268;
	font-weight: 700;
}

.login-box {
	padding: 34px;
}

.title {
	margin-bottom: 18px;
}

.title h2 {
	margin: 0 0 8px;
	color: #243f3c;
	font-size: 26px;
	letter-spacing: 0;
}

.title p {
	margin: 0;
	color: #667775;
	line-height: 1.7;
}

.quote-card {
	margin-bottom: 22px;
	padding: 16px;
	border-radius: 8px;
	background: #f7fbf9;
	border: 1px solid rgba(92, 174, 138, 0.14);
}

.quote-card span {
	display: inline-flex;
	margin-bottom: 8px;
	color: #4f8f76;
	font-size: 13px;
	font-weight: 800;
}

.quote-card p {
	margin: 0;
	color: #344d4a;
	font-size: 15px;
	line-height: 1.7;
}

.quote-card strong {
	display: block;
	margin-top: 8px;
	color: #7a8a87;
	font-size: 13px;
	text-align: right;
}

:deep(.custom-input .el-input__wrapper) {
	border-radius: 8px;
	padding: 10px 12px;
	background: #f8fcfa;
	box-shadow: 0 0 0 1px rgba(92, 174, 138, 0.16);
}

:deep(.custom-input .el-input__wrapper.is-focus) {
	box-shadow: 0 0 0 1px #5cae8a;
	background: #ffffff;
}

.login-btn {
	width: 100%;
	height: 42px;
	border-radius: 8px;
	border: none;
	background: #5cae8a;
	color: #ffffff;
	font-size: 16px;
	font-weight: 700;
}

.login-btn:hover {
	background: #4f9f7c;
	color: #ffffff;
}

.entry-actions {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-top: 8px;
}

.entry-actions :deep(.el-button) {
	color: #4f8f76;
	font-weight: 700;
}

.demo-tip {
	margin-top: 18px;
	padding: 12px 14px;
	border-radius: 8px;
	background: #f7fbf9;
	color: #647572;
	text-align: center;
}

.demo-tip strong {
	color: #2f6653;
}

@media (max-width: 960px) {
	.login-container {
		grid-template-columns: 1fr;
		padding: 22px;
	}

	.brand-panel {
		min-height: auto;
		padding: 32px;
	}

	.brand-panel h1 {
		font-size: 34px;
	}

	.login-box {
		width: 100%;
	}
}
</style>
