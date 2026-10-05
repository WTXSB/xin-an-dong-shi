<template>
	<div class="guide-page layout-padding">
		<div class="guide-shell layout-padding-auto layout-padding-view">
			<!-- 审核中状态 -->
			<section v-if="viewMode === 'pending'" class="status-card">
				<div class="status-icon" aria-hidden="true">
					<svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
						<rect x="12" y="10" width="40" height="46" rx="5" stroke="#c98f5c" stroke-width="2.5" />
						<path d="M20 24h24M20 32h24M20 40h14" stroke="#e4b98a" stroke-width="2.5" stroke-linecap="round" />
						<circle cx="46" cy="46" r="9" fill="#fbf6ef" stroke="#c98f5c" stroke-width="2.5" />
						<path d="M46 41v5l3.5 2.5" stroke="#c98f5c" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
					</svg>
				</div>
				<h1>资料正在被轻轻翻阅</h1>
				<p>你的{{ identity?.identityType === 'practitioner' ? '心灵SPA师认证' : '实名信息' }}已经收到，正在审核中。审核通过后，这里会为你点亮完整的「心有灵犀」。</p>
				<div class="status-actions">
					<button class="primary-btn" type="button" @click="goHome">先逛逛</button>
					<button class="ghost-btn" type="button" @click="refreshIdentity">再看看好了没</button>
				</div>
			</section>

			<!-- 驳回状态 -->
			<section v-else-if="viewMode === 'rejected'" class="status-card">
				<h1>这次资料还差一点点</h1>
				<p class="reject-note">{{ identity?.auditNote || '审核暂时没有通过，可以补充后再提交一次。' }}</p>
				<div class="status-actions">
					<button class="primary-btn" type="button" @click="openResubmit">重新提交</button>
					<button class="ghost-btn" type="button" @click="goHome">先逛逛</button>
				</div>
			</section>

			<template v-else>
				<section class="hero">
					<p class="eyebrow">心有灵犀 · 初次见面</p>
					<h1>先认识一下彼此，再决定怎么相伴</h1>
					<span>这里不需要一下子给出答案。你可以只是来照顾自己，也可以是愿意倾听别人的心灵SPA师。</span>
				</section>

				<section v-if="viewMode === 'select'" class="choice-row">
					<article class="choice-card">
						<div class="choice-icon" aria-hidden="true">
							<svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
								<path d="M32 54S12 42 12 27a11 11 0 0 1 20-6 11 11 0 0 1 20 6c0 15-20 27-20 27z" stroke="#c98f5c" stroke-width="2.5" stroke-linejoin="round" />
							</svg>
						</div>
						<h2>我来照顾自己</h2>
						<p>大部分功能无需实名，向心灵SPA师倾诉前完成实名即可。先进去慢慢看看，什么时候准备好了都可以。</p>
						<div class="choice-actions">
							<button class="primary-btn" type="button" @click="goHome">先进去看看</button>
							<button class="ghost-btn" type="button" @click="goHome">今天先不选了</button>
						</div>
					</article>

					<article class="choice-card">
						<div class="choice-icon" aria-hidden="true">
							<svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
								<circle cx="32" cy="24" r="10" stroke="#c98f5c" stroke-width="2.5" />
								<path d="M14 52c2.5-9 9.5-14 18-14s15.5 5 18 14" stroke="#c98f5c" stroke-width="2.5" stroke-linecap="round" />
								<path d="M46 12l1.6 3.6L51 17l-3.4 1.4L46 22l-1.6-3.6L41 17l3.4-1.4z" fill="#e4b98a" />
							</svg>
						</div>
						<h2>我是心灵SPA师</h2>
						<p>完成认证后，你可以收到向你倾诉的信，陪伴那些愿意把心事交给你的人。</p>
						<div class="choice-actions">
							<button class="primary-btn" type="button" @click="viewMode = 'practitionerForm'">去认证</button>
						</div>
					</article>
				</section>

				<!-- 心灵SPA师认证表单 -->
				<section v-else-if="viewMode === 'practitionerForm'" class="form-card">
					<header class="form-head">
						<div>
							<p class="eyebrow">心灵SPA师认证</p>
							<h2>让信任从真实开始</h2>
						</div>
						<button class="ghost-btn" type="button" @click="viewMode = 'select'">返回选择</button>
					</header>

					<el-form :model="practitionerForm" :rules="practitionerRules" ref="practitionerFormRef" label-width="96px" label-position="left">
						<el-form-item label="真实姓名" prop="realName">
							<el-input v-model="practitionerForm.realName" placeholder="请输入真实姓名" />
						</el-form-item>
						<el-form-item label="资格证号" prop="licenseNo">
							<el-input v-model="practitionerForm.licenseNo" placeholder="心理咨询师 / 医师资格证编号" />
						</el-form-item>
						<el-form-item label="就职医院" prop="hospital">
							<el-input v-model="practitionerForm.hospital" placeholder="所在医院或机构" />
						</el-form-item>
						<el-form-item label="科室" prop="department">
							<el-input v-model="practitionerForm.department" placeholder="如 临床心理科" />
						</el-form-item>
						<el-form-item label="职称" prop="title">
							<el-input v-model="practitionerForm.title" placeholder="如 主治医师 / 心理咨询师" />
						</el-form-item>
						<el-form-item label="简介" prop="bio">
							<el-input v-model="practitionerForm.bio" type="textarea" :rows="3" placeholder="用几句话介绍你的陪伴方式（选填）" />
						</el-form-item>
						<el-form-item>
							<button class="primary-btn submit-btn" type="button" :disabled="submitting" @click="submitPractitioner">
								{{ submitting ? '正在轻轻递交…' : '提交认证' }}
							</button>
						</el-form-item>
					</el-form>

					<footer class="form-disclaimer">演示环境未接入权威核验，信息仅用于平台内信任建立。</footer>
				</section>
			</template>
		</div>
	</div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import Cookies from 'js-cookie';
import type { FormInstance, FormRules } from 'element-plus';
import { applyIdentity, getMyIdentity, isNotFound, isOk, type IdentityRecord } from '/@/api/spa';

const router = useRouter();
const username = Cookies.get('userName') || '';

const viewMode = ref<'loading' | 'select' | 'practitionerForm' | 'pending' | 'rejected'>('loading');
const identity = ref<IdentityRecord | null>(null);
const submitting = ref(false);
const practitionerFormRef = ref<FormInstance>();

const practitionerForm = reactive({
	realName: '',
	licenseNo: '',
	hospital: '',
	department: '',
	title: '',
	bio: '',
});

const practitionerRules = reactive<FormRules>({
	realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
	licenseNo: [{ required: true, message: '请输入资格证号', trigger: 'blur' }],
	hospital: [{ required: true, message: '请输入就职医院或机构', trigger: 'blur' }],
	department: [{ required: true, message: '请输入科室', trigger: 'blur' }],
});

const goHome = () => {
	router.push('/');
};

const refreshIdentity = async () => {
	if (!username) {
		viewMode.value = 'select';
		return;
	}
	try {
		const res = await getMyIdentity(username);
		if (isNotFound(res)) {
			identity.value = null;
			viewMode.value = 'select';
			return;
		}
		if (isOk(res) && res.data) {
			identity.value = res.data;
			if (res.data.auditStatus === 'pending') {
				viewMode.value = 'pending';
			} else if (res.data.auditStatus === 'rejected') {
				viewMode.value = 'rejected';
			} else {
				ElMessage.success('你的身份已经审核通过啦。');
				router.push('/spaConnect');
			}
			return;
		}
		viewMode.value = 'select';
	} catch (error) {
		// 后端未就绪时不阻塞，直接进入选择页
		viewMode.value = 'select';
	}
};

const openResubmit = () => {
	if (identity.value?.identityType === 'practitioner') {
		practitionerForm.realName = identity.value.realName || '';
		practitionerForm.licenseNo = identity.value.licenseNo || '';
		practitionerForm.hospital = identity.value.hospital || '';
		practitionerForm.department = identity.value.department || '';
		practitionerForm.title = identity.value.title || '';
		practitionerForm.bio = identity.value.bio || '';
		viewMode.value = 'practitionerForm';
	} else {
		router.push('/spaConnect');
	}
};

const submitPractitioner = () => {
	practitionerFormRef.value?.validate(async (valid) => {
		if (!valid) return;
		submitting.value = true;
		try {
			const res = await applyIdentity({
				username,
				identityType: 'practitioner',
				realName: practitionerForm.realName,
				licenseNo: practitionerForm.licenseNo,
				hospital: practitionerForm.hospital,
				department: practitionerForm.department,
				title: practitionerForm.title,
				bio: practitionerForm.bio,
			});
			if (isOk(res)) {
				ElMessage.success('认证资料已收到，我们会尽快轻轻翻阅。');
				identity.value = { identityType: 'practitioner', realName: practitionerForm.realName, auditStatus: 'pending' };
				viewMode.value = 'pending';
			} else {
				ElMessage.warning(res.msg || '提交暂时没有成功，稍后再试一次。');
			}
		} catch (error) {
			ElMessage.warning('提交暂时没有成功，稍后再试一次。');
		} finally {
			submitting.value = false;
		}
	});
};

onMounted(() => {
	refreshIdentity();
});
</script>

<style scoped>
.guide-page.layout-padding {
	background: linear-gradient(160deg, #fdf9f3 0%, #fbf6ef 100%);
	min-height: 100%;
	height: 100%;
	overflow-y: auto; /* 表单较长时页面自身可滚动，保证提交按钮可达 */
}

.guide-shell.layout-padding-auto,
.guide-shell.layout-padding-view {
	background: transparent;
	border: none;
	box-shadow: none;
	max-width: 880px;
	margin: 0 auto;
	padding-bottom: 64px; /* 底部留白，避免按钮贴着视口边缘 */
}

.hero {
	margin-bottom: 26px;
	padding: 8px 4px;
}

.eyebrow {
	margin: 0 0 8px;
	color: #c98f5c;
	font-size: 14px;
	font-weight: 800;
	letter-spacing: 0.06em;
}

.hero h1 {
	margin: 0 0 10px;
	color: #4a3b30;
	font-size: 30px;
	line-height: 1.3;
}

.hero span {
	color: #8a7a6c;
	font-size: 15px;
	line-height: 1.8;
}

.choice-row {
	display: grid;
	grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
	gap: 20px;
}

.choice-card,
.form-card,
.status-card {
	padding: 30px;
	border: 1px solid rgba(201, 143, 92, 0.22);
	border-radius: 14px;
	background: #fffdf8;
	box-shadow: 0 14px 34px rgba(150, 108, 66, 0.08);
}

.choice-card {
	display: flex;
	flex-direction: column;
	gap: 12px;
}

.choice-icon svg {
	width: 56px;
	height: 56px;
}

.choice-card h2 {
	margin: 0;
	color: #4a3b30;
	font-size: 20px;
}

.choice-card p {
	flex: 1;
	margin: 0;
	color: #8a7a6c;
	font-size: 14px;
	line-height: 1.9;
}

.choice-actions {
	display: flex;
	flex-wrap: wrap;
	gap: 10px;
	margin-top: 6px;
}

.primary-btn,
.ghost-btn {
	border: none;
	border-radius: 8px;
	padding: 10px 22px;
	font-size: 15px;
	font-weight: 700;
	cursor: pointer;
	transition: background 0.2s ease, color 0.2s ease;
}

.primary-btn {
	background: #c98f5c;
	color: #ffffff;
}

.primary-btn:hover {
	background: #b67c4b;
}

.primary-btn:disabled {
	opacity: 0.7;
	cursor: not-allowed;
}

.ghost-btn {
	background: transparent;
	color: #b07d4e;
	box-shadow: inset 0 0 0 1px rgba(201, 143, 92, 0.45);
}

.ghost-btn:hover {
	background: #f6ecdf;
}

.form-card {
	max-width: 680px;
	margin: 0 auto;
}

.form-head {
	display: flex;
	justify-content: space-between;
	align-items: flex-start;
	margin-bottom: 18px;
}

.form-head h2 {
	margin: 0;
	color: #4a3b30;
	font-size: 22px;
}

.submit-btn {
	width: 160px;
	height: 42px;
}

.form-disclaimer {
	margin-top: 14px;
	padding: 12px 14px;
	border-radius: 8px;
	background: #f7efe4;
	color: #96785a;
	font-size: 13px;
	line-height: 1.7;
}

.status-card {
	max-width: 560px;
	margin: 40px auto;
	text-align: center;
}

.status-icon svg {
	width: 72px;
	height: 72px;
}

.status-card h1 {
	margin: 14px 0 10px;
	color: #4a3b30;
	font-size: 24px;
}

.status-card p {
	margin: 0;
	color: #8a7a6c;
	font-size: 15px;
	line-height: 1.9;
}

.reject-note {
	padding: 12px 16px;
	border-radius: 8px;
	background: #fbf1e4;
}

.status-actions {
	display: flex;
	justify-content: center;
	gap: 12px;
	margin-top: 22px;
}
</style>
