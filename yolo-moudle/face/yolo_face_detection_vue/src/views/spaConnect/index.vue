<template>
	<div class="spa-page layout-padding">
		<div class="spa-shell layout-padding-auto layout-padding-view">
			<section class="hero">
				<p class="eyebrow">心有灵犀</p>
				<h1>把心事交给愿意倾听的人</h1>
				<span>写一封信，等一封回信。这里没有催促，只有被认真接住的倾诉。</span>
			</section>

			<!-- admin 专属：认证审核 Tab -->
			<div v-if="isAdmin" class="module-tabs" role="tablist">
				<button type="button" role="tab" :class="{ active: activeTab === 'connect' }" @click="activeTab = 'connect'">心有灵犀</button>
				<button type="button" role="tab" :class="{ active: activeTab === 'review' }" @click="switchToReview">认证审核</button>
			</div>

			<!-- 认证审核面板 -->
			<section v-if="isAdmin && activeTab === 'review'" v-loading="reviewLoading" class="review-panel">
				<div v-if="pendingList.length === 0" class="empty-state">
					<strong>暂时没有等待审核的资料</strong>
					<p>每一份递交上来的信任，都会安静地排队等你翻阅。</p>
				</div>

				<article v-for="item in pendingList" :key="item.username" class="review-card">
					<header class="review-head">
						<span :class="['review-badge', item.identityType]">
							{{ item.identityType === 'practitioner' ? '医师认证' : '患者实名' }}
						</span>
						<strong>{{ item.realName }}</strong>
						<small>账号：{{ item.username }}</small>
					</header>
					<div class="review-fields">
						<p v-if="item.idCard"><span>身份证号</span>{{ item.idCard }}</p>
						<p v-if="item.licenseNo"><span>资格证号</span>{{ item.licenseNo }}</p>
						<p v-if="item.hospital"><span>就职医院</span>{{ item.hospital }}</p>
						<p v-if="item.department"><span>科室</span>{{ item.department }}</p>
						<p v-if="item.title"><span>职称</span>{{ item.title }}</p>
						<p v-if="item.bio"><span>简介</span>{{ item.bio }}</p>
					</div>
					<footer class="review-actions">
						<button class="primary-btn" type="button" :disabled="reviewing" @click="doReview(item, true)">通过</button>
						<button class="ghost-btn danger" type="button" :disabled="reviewing" @click="openReject(item)">驳回</button>
					</footer>
				</article>
			</section>

			<template v-else>
				<!-- 加载中 -->
				<section v-if="viewState === 'loading'" v-loading="true" element-loading-text="正在轻轻展开…" class="loading-block"></section>

				<template v-else>
					<!-- 无身份引导卡 -->
					<section v-if="viewState === 'guest'" class="guide-card">
						<div>
							<h2>第一次见面，先打个招呼吧</h2>
							<p>去身份向导选择你的角色；如果已经准备好向心灵SPA师倾诉，也可以直接完成实名。</p>
						</div>
						<div class="guide-actions">
							<button class="primary-btn" type="button" @click="router.push('/identityGuide')">去身份向导</button>
							<button class="ghost-btn" type="button" @click="realNameDialogVisible = true">我想倾诉，先完成实名</button>
						</div>
					</section>

					<!-- 审核中 -->
					<section v-else-if="viewState === 'pending'" class="status-card">
						<h2>资料正在被轻轻翻阅，请稍候</h2>
						<p>你的{{ myIdentity?.identityType === 'practitioner' ? '心灵SPA师认证' : '实名信息' }}正在审核中，通过之后就可以完整使用「心有灵犀」了。</p>
						<div class="status-actions">
							<button class="ghost-btn" type="button" @click="loadIdentity">再看看好了没</button>
							<button class="primary-btn" type="button" @click="router.push('/')">先逛逛</button>
						</div>
					</section>

					<!-- 已驳回 -->
					<section v-else-if="viewState === 'rejected'" class="status-card">
						<h2>这次资料还差一点点</h2>
						<p class="reject-note">{{ myIdentity?.auditNote || '审核暂时没有通过，补充后可以再提交一次。' }}</p>
						<div class="status-actions">
							<button class="primary-btn" type="button" @click="router.push('/identityGuide')">重新提交</button>
							<button class="ghost-btn" type="button" @click="router.push('/')">先逛逛</button>
						</div>
					</section>

					<!-- 书信式对话界面 -->
					<section v-else-if="activeRequest" class="chat-layout" :class="{ 'with-records': isPractitionerView }">
						<div class="chat-main">
							<header class="chat-head">
								<button class="ghost-btn" type="button" @click="closeConversation">返回列表</button>
								<div class="chat-peer">
									<strong>{{ counterpartName(activeRequest) }}</strong>
									<span>{{ counterpartSubtitle(activeRequest) }}</span>
								</div>
							</header>

							<div ref="lettersRef" v-loading="messagesLoading" class="letters">
								<div v-for="msg in messages" :key="msg.id" :class="['letter-item', { mine: msg.senderUsername === username }]">
									<div class="letter-card">
										<p class="letter-tag">{{ msg.senderUsername === username ? '我写的' : 'TA 的回信' }}</p>
										<p class="letter-content">{{ msg.content }}</p>
										<p class="letter-time">{{ formatTime(msg.createdAt) }}</p>
									</div>
								</div>
								<div v-if="!messagesLoading && messages.length === 0" class="empty-state">
									<strong>信纸还是空白的</strong>
									<p>写下第一句话，让这段陪伴从这一刻开始。</p>
								</div>
							</div>

							<footer class="reply-box">
								<el-input
									v-model="draft"
									type="textarea"
									:rows="3"
									resize="none"
									placeholder="写下想对TA说的话…（Enter 发送，Shift+Enter 换行）"
									@keydown.enter.exact.prevent="sendCurrentMessage"
								/>
								<button class="primary-btn send-btn" type="button" :disabled="sending || !draft.trim()" @click="sendCurrentMessage">
									{{ sending ? '寄出中…' : '寄出' }}
								</button>
							</footer>
						</div>

						<!-- 心灵SPA师侧栏：TA 的近期记录 -->
						<aside v-if="isPractitionerView" class="records-aside">
							<header class="records-head">
								<strong>TA 的近期记录</strong>
								<p>对方向你倾诉后即同意你查看这些记录，请温柔以待。</p>
							</header>
							<div v-loading="recordsLoading" class="records-list">
								<div v-if="!recordsLoading && patientRecords.length === 0" class="empty-state small">
									<strong>暂时没有可看的记录</strong>
								</div>
								<article v-for="rec in patientRecords" :key="rec.id" class="record-item" @click="openRecord(rec)">
									<div class="record-line">
										<span class="emotion-pill">{{ rec.emotionLabel || '未标注' }}</span>
										<small>{{ formatTime(rec.createdAt) }}</small>
									</div>
									<p>{{ rec.gentleSummary || '这条记录暂时没有摘要。' }}</p>
								</article>
							</div>
						</aside>
					</section>

					<template v-else>
						<!-- 心灵SPA师卡片网格（无身份与患者视角可见） -->
						<section v-if="viewState === 'guest' || viewState === 'patient'" class="practitioner-section">
							<h2 class="section-title">愿意倾听你的心灵SPA师</h2>
							<div v-loading="practitionersLoading" class="practitioner-grid">
								<div v-if="!practitionersLoading && practitioners.length === 0" class="empty-state">
									<strong>心灵SPA师们还在路上</strong>
									<p>等他们完成认证后，就会在这里等你写信。</p>
								</div>
								<article v-for="p in practitioners" :key="p.username" class="practitioner-card">
									<header>
										<strong>{{ p.realName }}</strong>
										<span v-if="p.title" class="title-pill">{{ p.title }}</span>
									</header>
									<p class="org">{{ [p.hospital, p.department].filter(Boolean).join(' · ') || '心灵SPA师' }}</p>
									<p class="bio">{{ p.bio || '愿意安静地听你把话说完。' }}</p>
									<button class="primary-btn" type="button" @click="onConfide(p)">向TA倾诉</button>
								</article>
							</div>
						</section>

						<!-- 求助单列表 -->
						<section v-if="viewState === 'patient' || viewState === 'practitioner'" class="requests-section">
							<h2 class="section-title">{{ viewState === 'practitioner' ? '收到的倾诉' : '我的书信往来' }}</h2>
							<div v-loading="requestsLoading" class="requests-list">
								<div v-if="!requestsLoading && requests.length === 0" class="empty-state">
									<strong>{{ viewState === 'practitioner' ? '还没有收到倾诉' : '还没有书信往来' }}</strong>
									<p>{{ viewState === 'practitioner' ? '当有人向你递出第一封信，会出现在这里。' : '挑一位心灵SPA师，写下你的第一封信吧。' }}</p>
								</div>
								<article v-for="item in requests" :key="item.id" class="request-card" @click="openConversation(item)">
									<div class="request-main">
										<header>
											<strong>{{ counterpartName(item) }}</strong>
											<span class="org">{{ counterpartSubtitle(item) }}</span>
										</header>
										<p class="preview">{{ latestPreview(item) }}</p>
									</div>
									<div class="request-meta">
										<span :class="['status-pill', item.status]">{{ statusText(item.status) }}</span>
										<small>{{ formatTime(item.createdAt) }}</small>
									</div>
								</article>
							</div>
						</section>
					</template>
				</template>
			</template>
		</div>

		<!-- 实名对话框（倾诉前完成） -->
		<el-dialog v-model="realNameDialogVisible" title="倾诉前，先完成实名" width="440px" class="warm-dialog">
			<el-form :model="realNameForm" :rules="realNameRules" ref="realNameFormRef" label-width="88px">
				<el-form-item label="真实姓名" prop="realName">
					<el-input v-model="realNameForm.realName" placeholder="请输入真实姓名" />
				</el-form-item>
				<el-form-item label="身份证号" prop="idCard">
					<el-input v-model="realNameForm.idCard" placeholder="请输入身份证号" />
				</el-form-item>
			</el-form>
			<p class="dialog-disclaimer">演示环境未接入权威核验，信息仅用于平台内信任建立。</p>
			<template #footer>
				<button class="ghost-btn" type="button" @click="realNameDialogVisible = false">再想想</button>
				<button class="primary-btn" type="button" :disabled="submittingRealName" @click="submitRealName">
					{{ submittingRealName ? '递交中…' : '提交实名' }}
				</button>
			</template>
		</el-dialog>

		<!-- 写第一封信对话框 -->
		<el-dialog v-model="letterDialogVisible" :title="`写给 ${letterTarget?.realName || 'TA'} 的第一封信`" width="520px" class="warm-dialog">
			<el-input
				v-model="letterDraft"
				type="textarea"
				:rows="6"
				resize="none"
				placeholder="慢慢写，想到哪里就写到哪里。TA 会认真读完。"
			/>
			<template #footer>
				<button class="ghost-btn" type="button" @click="letterDialogVisible = false">再想想</button>
				<button class="primary-btn" type="button" :disabled="sendingLetter || !letterDraft.trim()" @click="submitFirstLetter">
					{{ sendingLetter ? '寄出中…' : '寄出这封信' }}
				</button>
			</template>
		</el-dialog>

		<!-- 驳回备注对话框 -->
		<el-dialog v-model="rejectDialogVisible" title="驳回认证申请" width="440px" class="warm-dialog">
			<el-input v-model="rejectNote" type="textarea" :rows="4" resize="none" placeholder="请温柔地说明驳回原因（必填）" />
			<template #footer>
				<button class="ghost-btn" type="button" @click="rejectDialogVisible = false">取消</button>
				<button class="primary-btn" type="button" :disabled="reviewing || !rejectNote.trim()" @click="confirmReject">确认驳回</button>
			</template>
		</el-dialog>
	</div>
</template>

<script lang="ts" setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import Cookies from 'js-cookie';
import type { FormInstance, FormRules } from 'element-plus';
import {
	applyIdentity,
	createSpaRequest,
	getMyIdentity,
	getMyRequests,
	getPatientRecords,
	getPendingIdentities,
	getPractitioners,
	getSpaMessages,
	isNotFound,
	isOk,
	reviewIdentity,
	sendSpaMessage,
	type IdentityRecord,
	type PatientRecordItem,
	type PendingIdentity,
	type Practitioner,
	type SpaMessage,
	type SpaRequestItem,
} from '/@/api/spa';

const router = useRouter();
const username = Cookies.get('userName') || '';
const role = Cookies.get('role') || '';
const isAdmin = computed(() => role === 'admin');

type ViewState = 'loading' | 'guest' | 'pending' | 'rejected' | 'patient' | 'practitioner';
const viewState = ref<ViewState>('loading');
const myIdentity = ref<IdentityRecord | null>(null);

// ---------- 身份 ----------
const loadIdentity = async () => {
	if (!username) {
		viewState.value = 'guest';
		return;
	}
	try {
		const res = await getMyIdentity(username);
		if (isNotFound(res)) {
			myIdentity.value = null;
			viewState.value = 'guest';
			loadPractitioners();
			return;
		}
		if (isOk(res) && res.data) {
			myIdentity.value = res.data;
			if (res.data.auditStatus === 'pending') {
				viewState.value = 'pending';
			} else if (res.data.auditStatus === 'rejected') {
				viewState.value = 'rejected';
			} else if (res.data.identityType === 'practitioner') {
				viewState.value = 'practitioner';
				loadRequests();
			} else {
				viewState.value = 'patient';
				loadPractitioners();
				loadRequests();
			}
			return;
		}
		ElMessage.warning(res.msg || '身份状态暂时没有拿到，稍后再试一次。');
		viewState.value = 'guest';
	} catch (error) {
		viewState.value = 'guest';
		ElMessage.warning('暂时没有连上，先不用着急。');
	}
};

const isPractitionerView = computed(() => viewState.value === 'practitioner' && myIdentity.value?.identityType === 'practitioner');

// ---------- 心灵SPA师列表 ----------
const practitioners = ref<Practitioner[]>([]);
const practitionersLoading = ref(false);

const loadPractitioners = async () => {
	practitionersLoading.value = true;
	try {
		const res = await getPractitioners();
		if (isOk(res)) {
			practitioners.value = res.data || [];
		}
	} catch (error) {
		// 静默失败，页面保持空态
	} finally {
		practitionersLoading.value = false;
	}
};

// ---------- 实名（倾诉前） ----------
const realNameDialogVisible = ref(false);
const submittingRealName = ref(false);
const realNameFormRef = ref<FormInstance>();
const realNameForm = reactive({ realName: '', idCard: '' });
const realNameRules = reactive<FormRules>({
	realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
	idCard: [{ required: true, message: '请输入身份证号', trigger: 'blur' }],
});

const submitRealName = () => {
	realNameFormRef.value?.validate(async (valid) => {
		if (!valid) return;
		submittingRealName.value = true;
		try {
			const res = await applyIdentity({
				username,
				identityType: 'patient',
				realName: realNameForm.realName,
				idCard: realNameForm.idCard,
			});
			if (isOk(res)) {
				ElMessage.success('已收到你的实名信息，请稍候，正在等待温柔审核。');
				realNameDialogVisible.value = false;
				loadIdentity();
			} else {
				ElMessage.warning(res.msg || '提交暂时没有成功，稍后再试一次。');
			}
		} catch (error) {
			ElMessage.warning('提交暂时没有成功，稍后再试一次。');
		} finally {
			submittingRealName.value = false;
		}
	});
};

// ---------- 写第一封信 ----------
const letterDialogVisible = ref(false);
const sendingLetter = ref(false);
const letterDraft = ref('');
const letterTarget = ref<Practitioner | null>(null);

const onConfide = (p: Practitioner) => {
	// 未完成实名 / 实名未通过时，先引导完成实名
	if (!myIdentity.value || myIdentity.value.identityType !== 'patient' || myIdentity.value.auditStatus !== 'approved') {
		realNameDialogVisible.value = true;
		return;
	}
	letterTarget.value = p;
	letterDraft.value = '';
	letterDialogVisible.value = true;
};

const submitFirstLetter = async () => {
	if (!letterTarget.value) return;
	sendingLetter.value = true;
	try {
		const res = await createSpaRequest({
			patientUsername: username,
			practitionerUsername: letterTarget.value.username,
			initialMessage: letterDraft.value.trim(),
		});
		if (isOk(res)) {
			ElMessage.success('信已经寄出，耐心等一封回信吧。');
			letterDialogVisible.value = false;
			await loadRequests();
			const newId = res.data?.id;
			const target = newId ? requests.value.find((r) => r.id === newId) : requests.value.find((r) => r.practitionerUsername === letterTarget.value?.username);
			if (target) openConversation(target);
		} else {
			ElMessage.warning(res.msg || '信暂时没有寄出去，稍后再试一次。');
		}
	} catch (error) {
		ElMessage.warning('信暂时没有寄出去，稍后再试一次。');
	} finally {
		sendingLetter.value = false;
	}
};

// ---------- 求助单 ----------
const requests = ref<SpaRequestItem[]>([]);
const requestsLoading = ref(false);

const loadRequests = async () => {
	requestsLoading.value = true;
	try {
		const res = await getMyRequests(username);
		if (isOk(res)) {
			requests.value = res.data || [];
		}
	} catch (error) {
		// 静默失败
	} finally {
		requestsLoading.value = false;
	}
};

const statusText = (status?: string) => {
	const map: Record<string, string> = {
		pending: '等待回应',
		open: '联结中',
		accepted: '联结中',
		closed: '已温柔收尾',
	};
	return map[status || ''] || status || '进行中';
};

// 列表预览：latestMessage 是 {content, senderUsername, createdAt} 对象，只取正文
const latestPreview = (item: SpaRequestItem) => {
	const latest: any = item.latestMessage;
	if (latest && typeof latest === 'object' && latest.content) {
		const mine = latest.senderUsername === username;
		return `${mine ? '我' : 'TA'}：${latest.content}`;
	}
	if (typeof latest === 'string' && latest) return latest;
	return item.initialMessage || '';
};

const counterpartName = (item: SpaRequestItem) => {
	return (
		item.counterpartName ||
		item.counterpartRealName ||
		item.practitionerRealName ||
		item.patientRealName ||
		item.practitionerName ||
		item.patientName ||
		(isPractitionerView.value ? item.patientUsername : item.practitionerUsername) ||
		'TA'
	);
};

const counterpartSubtitle = (item: SpaRequestItem) => {
	if (isPractitionerView.value) return '求助者';
	const parts = [item.counterpartHospital || item.hospital, item.counterpartDepartment || item.department].filter(Boolean);
	return parts.length ? parts.join(' · ') : '心灵SPA师';
};

// ---------- 对话 ----------
const activeRequest = ref<SpaRequestItem | null>(null);
const messages = ref<SpaMessage[]>([]);
const messagesLoading = ref(false);
const draft = ref('');
const sending = ref(false);
const lettersRef = ref<HTMLElement>();
let pollTimer: ReturnType<typeof setInterval> | null = null;

const lastMessageId = computed(() => (messages.value.length ? messages.value[messages.value.length - 1].id : 0));

const scrollToBottom = () => {
	nextTick(() => {
		if (lettersRef.value) lettersRef.value.scrollTop = lettersRef.value.scrollHeight;
	});
};

const fetchMessages = async (incremental: boolean) => {
	if (!activeRequest.value) return;
	if (!incremental) messagesLoading.value = true;
	try {
		const res = await getSpaMessages(activeRequest.value.id, username, incremental ? lastMessageId.value : 0);
		if (isOk(res)) {
			const list: SpaMessage[] = res.data || [];
			if (incremental) {
				if (list.length) {
					messages.value = [...messages.value, ...list];
					scrollToBottom();
				}
			} else {
				messages.value = list;
				scrollToBottom();
			}
		}
	} catch (error) {
		// 轮询失败时静默，下一轮再试
	} finally {
		messagesLoading.value = false;
	}
};

const stopPolling = () => {
	if (pollTimer) {
		clearInterval(pollTimer);
		pollTimer = null;
	}
};

const openConversation = (item: SpaRequestItem) => {
	activeRequest.value = item;
	messages.value = [];
	draft.value = '';
	fetchMessages(false);
	stopPolling();
	pollTimer = setInterval(() => fetchMessages(true), 5000);
	if (isPractitionerView.value) loadPatientRecords();
};

const closeConversation = () => {
	stopPolling();
	activeRequest.value = null;
	loadRequests();
};

const sendCurrentMessage = async () => {
	const content = draft.value.trim();
	if (!content || !activeRequest.value || sending.value) return;
	sending.value = true;
	try {
		const res = await sendSpaMessage(activeRequest.value.id, { username, content });
		if (isOk(res)) {
			draft.value = '';
			await fetchMessages(true);
		} else {
			ElMessage.warning(res.msg || '这封信暂时没有寄出去，稍后再试一次。');
		}
	} catch (error) {
		ElMessage.warning('这封信暂时没有寄出去，稍后再试一次。');
	} finally {
		sending.value = false;
	}
};

// ---------- 心灵SPA师侧栏：TA 的近期记录 ----------
const patientRecords = ref<PatientRecordItem[]>([]);
const recordsLoading = ref(false);

const loadPatientRecords = async () => {
	if (!activeRequest.value) return;
	recordsLoading.value = true;
	try {
		const res = await getPatientRecords(activeRequest.value.id, username);
		if (isOk(res)) {
			patientRecords.value = res.data || [];
		}
	} catch (error) {
		// 静默失败
	} finally {
		recordsLoading.value = false;
	}
};

const openRecord = (rec: PatientRecordItem) => {
	window.open(`#/preVisitReport/${rec.id}`, '_blank');
};

// ---------- admin：认证审核 ----------
const activeTab = ref<'connect' | 'review'>('connect');
const pendingList = ref<PendingIdentity[]>([]);
const reviewLoading = ref(false);
const reviewing = ref(false);
const rejectDialogVisible = ref(false);
const rejectNote = ref('');
const rejectTarget = ref<PendingIdentity | null>(null);

const switchToReview = () => {
	activeTab.value = 'review';
	loadPending();
};

const loadPending = async () => {
	reviewLoading.value = true;
	try {
		const res = await getPendingIdentities(username);
		if (isOk(res)) {
			pendingList.value = res.data || [];
		} else {
			ElMessage.warning(res.msg || '待审列表暂时没有拿到。');
		}
	} catch (error) {
		ElMessage.warning('待审列表暂时没有连上。');
	} finally {
		reviewLoading.value = false;
	}
};

const doReview = async (item: PendingIdentity, approve: boolean, note = '') => {
	reviewing.value = true;
	try {
		const res = await reviewIdentity({ adminUsername: username, username: item.username, approve, note });
		if (isOk(res)) {
			ElMessage.success(approve ? '已通过这份认证。' : '已温柔地退回这份申请。');
			loadPending();
		} else {
			ElMessage.warning(res.msg || '操作暂时没有成功。');
		}
	} catch (error) {
		ElMessage.warning('操作暂时没有成功。');
	} finally {
		reviewing.value = false;
	}
};

const openReject = (item: PendingIdentity) => {
	rejectTarget.value = item;
	rejectNote.value = '';
	rejectDialogVisible.value = true;
};

const confirmReject = () => {
	if (!rejectTarget.value || !rejectNote.value.trim()) return;
	doReview(rejectTarget.value, false, rejectNote.value.trim());
	rejectDialogVisible.value = false;
};

// ---------- 工具 ----------
const formatTime = (val?: string) => {
	if (!val) return '';
	const d = new Date(val);
	if (isNaN(d.getTime())) return val;
	const pad = (n: number) => String(n).padStart(2, '0');
	return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

onMounted(() => {
	loadIdentity();
});

onBeforeUnmount(() => {
	stopPolling();
});
</script>

<style scoped>
.spa-page.layout-padding {
	background: linear-gradient(160deg, #fdf9f3 0%, #fbf6ef 100%);
	min-height: 100%;
}

.spa-shell.layout-padding-auto,
.spa-shell.layout-padding-view {
	background: transparent;
	border: none;
	box-shadow: none;
}

.hero {
	margin-bottom: 20px;
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
	font-size: 28px;
	line-height: 1.3;
}

.hero span {
	color: #8a7a6c;
	font-size: 15px;
	line-height: 1.8;
}

.loading-block {
	min-height: 240px;
}

.section-title {
	margin: 26px 4px 14px;
	color: #4a3b30;
	font-size: 19px;
}

.primary-btn,
.ghost-btn {
	border: none;
	border-radius: 8px;
	padding: 9px 20px;
	font-size: 14px;
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

.ghost-btn.danger {
	color: #b05c4a;
	box-shadow: inset 0 0 0 1px rgba(176, 92, 74, 0.45);
}

.empty-state {
	padding: 40px 20px;
	text-align: center;
	color: #8a7a6c;
}

.empty-state strong {
	display: block;
	margin-bottom: 8px;
	color: #6d5c4d;
	font-size: 16px;
}

.empty-state p {
	margin: 0;
	font-size: 14px;
	line-height: 1.8;
}

.empty-state.small {
	padding: 20px 10px;
}

/* admin Tab */
.module-tabs {
	display: inline-flex;
	gap: 8px;
	margin-bottom: 18px;
	padding: 5px;
	border-radius: 10px;
	background: #f3e8d8;
}

.module-tabs button {
	border: none;
	border-radius: 8px;
	padding: 8px 22px;
	background: transparent;
	color: #8a6f52;
	font-size: 14px;
	font-weight: 700;
	cursor: pointer;
}

.module-tabs button.active {
	background: #fffdf8;
	color: #b07d4e;
	box-shadow: 0 2px 8px rgba(150, 108, 66, 0.12);
}

/* 引导卡 / 状态卡 */
.guide-card,
.status-card {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 18px;
	padding: 22px 26px;
	border: 1px solid rgba(201, 143, 92, 0.25);
	border-radius: 12px;
	background: #fffdf8;
	box-shadow: 0 10px 26px rgba(150, 108, 66, 0.08);
}

.status-card {
	flex-direction: column;
	align-items: flex-start;
	max-width: 620px;
}

.guide-card h2,
.status-card h2 {
	margin: 0 0 8px;
	color: #4a3b30;
	font-size: 19px;
}

.guide-card p,
.status-card p {
	margin: 0;
	color: #8a7a6c;
	font-size: 14px;
	line-height: 1.8;
}

.reject-note {
	padding: 10px 14px;
	border-radius: 8px;
	background: #fbf1e4;
}

.guide-actions,
.status-actions {
	display: flex;
	flex-shrink: 0;
	gap: 10px;
}

/* 心灵SPA师卡片 */
.practitioner-grid {
	display: grid;
	grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
	gap: 16px;
}

.practitioner-card {
	display: flex;
	flex-direction: column;
	gap: 8px;
	padding: 20px;
	border: 1px solid rgba(201, 143, 92, 0.2);
	border-radius: 12px;
	background: #fffdf8;
	box-shadow: 0 10px 24px rgba(150, 108, 66, 0.07);
}

.practitioner-card header {
	display: flex;
	align-items: center;
	gap: 10px;
}

.practitioner-card header strong {
	color: #4a3b30;
	font-size: 17px;
}

.title-pill {
	padding: 3px 10px;
	border-radius: 999px;
	background: #f3e2cc;
	color: #a4713e;
	font-size: 12px;
	font-weight: 700;
}

.practitioner-card .org {
	margin: 0;
	color: #a08b78;
	font-size: 13px;
}

.practitioner-card .bio {
	flex: 1;
	margin: 0;
	color: #8a7a6c;
	font-size: 14px;
	line-height: 1.8;
	display: -webkit-box;
	-webkit-line-clamp: 3;
	-webkit-box-orient: vertical;
	overflow: hidden;
}

.practitioner-card .primary-btn {
	align-self: flex-start;
	margin-top: 4px;
}

/* 求助单列表 */
.requests-list {
	display: flex;
	flex-direction: column;
	gap: 12px;
}

.request-card {
	display: flex;
	justify-content: space-between;
	align-items: center;
	gap: 16px;
	padding: 16px 20px;
	border: 1px solid rgba(201, 143, 92, 0.2);
	border-radius: 12px;
	background: #fffdf8;
	box-shadow: 0 8px 20px rgba(150, 108, 66, 0.06);
	cursor: pointer;
	transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.request-card:hover {
	transform: translateY(-2px);
	box-shadow: 0 14px 28px rgba(150, 108, 66, 0.12);
}

.request-main {
	min-width: 0;
}

.request-main header {
	display: flex;
	align-items: baseline;
	gap: 10px;
	margin-bottom: 6px;
}

.request-main header strong {
	color: #4a3b30;
	font-size: 16px;
}

.request-main .org {
	color: #a08b78;
	font-size: 13px;
}

.request-main .preview {
	margin: 0;
	color: #8a7a6c;
	font-size: 14px;
	line-height: 1.7;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
	max-width: 560px;
}

.request-meta {
	display: flex;
	flex-direction: column;
	align-items: flex-end;
	gap: 8px;
	flex-shrink: 0;
}

.request-meta small {
	color: #b3a08d;
}

.status-pill {
	padding: 4px 12px;
	border-radius: 999px;
	background: #f3e2cc;
	color: #a4713e;
	font-size: 12px;
	font-weight: 700;
}

.status-pill.closed {
	background: #eee9e0;
	color: #93866f;
}

/* 对话界面 */
.chat-layout {
	display: grid;
	grid-template-columns: minmax(0, 1fr);
	gap: 16px;
}

.chat-layout.with-records {
	grid-template-columns: minmax(0, 1fr) 300px;
}

.chat-main {
	display: flex;
	flex-direction: column;
	border: 1px solid rgba(201, 143, 92, 0.2);
	border-radius: 12px;
	background: #fffdf8;
	box-shadow: 0 10px 26px rgba(150, 108, 66, 0.08);
	overflow: hidden;
}

.chat-head {
	display: flex;
	align-items: center;
	gap: 16px;
	padding: 14px 18px;
	border-bottom: 1px solid rgba(201, 143, 92, 0.16);
	background: #fdf7ec;
}

.chat-peer {
	display: flex;
	flex-direction: column;
}

.chat-peer strong {
	color: #4a3b30;
	font-size: 16px;
}

.chat-peer span {
	color: #a08b78;
	font-size: 13px;
}

.letters {
	flex: 1;
	min-height: 320px;
	max-height: 52vh;
	overflow-y: auto;
	padding: 20px;
	display: flex;
	flex-direction: column;
	gap: 14px;
	background: linear-gradient(180deg, #fffdf8 0%, #fbf6ef 100%);
}

.letter-item {
	display: flex;
	justify-content: flex-start;
}

.letter-item.mine {
	justify-content: flex-end;
}

.letter-card {
	max-width: 72%;
	padding: 12px 16px;
	border-radius: 12px;
	border: 1px solid rgba(201, 143, 92, 0.18);
	background: #fdf8ef;
	box-shadow: 0 6px 14px rgba(150, 108, 66, 0.06);
}

.letter-item.mine .letter-card {
	background: #f4dfc4;
	border-color: rgba(201, 143, 92, 0.35);
}

.letter-tag {
	margin: 0 0 6px;
	color: #b07d4e;
	font-size: 12px;
	font-weight: 800;
}

.letter-content {
	margin: 0;
	color: #5b4a3c;
	font-size: 15px;
	line-height: 1.8;
	white-space: pre-wrap;
	word-break: break-word;
}

.letter-time {
	margin: 8px 0 0;
	color: #b3a08d;
	font-size: 12px;
	text-align: right;
}

.reply-box {
	display: flex;
	align-items: flex-end;
	gap: 12px;
	padding: 14px 18px;
	border-top: 1px solid rgba(201, 143, 92, 0.16);
	background: #fdf7ec;
}

.reply-box :deep(.el-textarea__inner) {
	border-radius: 10px;
	background: #fffdf8;
	box-shadow: 0 0 0 1px rgba(201, 143, 92, 0.28);
}

.reply-box :deep(.el-textarea__inner:focus) {
	box-shadow: 0 0 0 1px #c98f5c;
}

.send-btn {
	flex-shrink: 0;
	height: 40px;
}

/* 侧栏：近期记录 */
.records-aside {
	display: flex;
	flex-direction: column;
	border: 1px solid rgba(201, 143, 92, 0.2);
	border-radius: 12px;
	background: #fbf4e8;
	box-shadow: 0 10px 26px rgba(150, 108, 66, 0.08);
	overflow: hidden;
}

.records-head {
	padding: 14px 16px;
	border-bottom: 1px solid rgba(201, 143, 92, 0.16);
}

.records-head strong {
	color: #4a3b30;
	font-size: 15px;
}

.records-head p {
	margin: 6px 0 0;
	color: #a08b78;
	font-size: 12px;
	line-height: 1.7;
}

.records-list {
	flex: 1;
	overflow-y: auto;
	max-height: 60vh;
	padding: 12px;
	display: flex;
	flex-direction: column;
	gap: 10px;
}

.record-item {
	padding: 12px;
	border: 1px solid rgba(201, 143, 92, 0.18);
	border-radius: 10px;
	background: #fffdf8;
	cursor: pointer;
	transition: box-shadow 0.15s ease;
}

.record-item:hover {
	box-shadow: 0 8px 18px rgba(150, 108, 66, 0.12);
}

.record-line {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 6px;
}

.emotion-pill {
	padding: 2px 10px;
	border-radius: 999px;
	background: #f3e2cc;
	color: #a4713e;
	font-size: 12px;
	font-weight: 700;
}

.record-line small {
	color: #b3a08d;
}

.record-item p {
	margin: 0;
	color: #8a7a6c;
	font-size: 13px;
	line-height: 1.7;
	display: -webkit-box;
	-webkit-line-clamp: 3;
	-webkit-box-orient: vertical;
	overflow: hidden;
}

/* 审核面板 */
.review-panel {
	display: flex;
	flex-direction: column;
	gap: 14px;
}

.review-card {
	padding: 18px 22px;
	border: 1px solid rgba(201, 143, 92, 0.22);
	border-radius: 12px;
	background: #fffdf8;
	box-shadow: 0 10px 24px rgba(150, 108, 66, 0.07);
}

.review-head {
	display: flex;
	align-items: center;
	gap: 12px;
	margin-bottom: 12px;
}

.review-head strong {
	color: #4a3b30;
	font-size: 17px;
}

.review-head small {
	color: #a08b78;
}

.review-badge {
	padding: 3px 12px;
	border-radius: 999px;
	font-size: 12px;
	font-weight: 800;
}

.review-badge.practitioner {
	background: #e9f0e4;
	color: #5d7a4c;
}

.review-badge.patient {
	background: #f3e2cc;
	color: #a4713e;
}

.review-fields {
	display: grid;
	grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
	gap: 6px 20px;
}

.review-fields p {
	margin: 0;
	color: #6d5c4d;
	font-size: 14px;
	line-height: 1.8;
}

.review-fields span {
	display: inline-block;
	min-width: 64px;
	margin-right: 8px;
	color: #b3a08d;
	font-size: 13px;
}

.review-actions {
	display: flex;
	gap: 10px;
	margin-top: 14px;
}

/* 对话框 */
.dialog-disclaimer {
	margin: 4px 0 0;
	padding: 10px 12px;
	border-radius: 8px;
	background: #f7efe4;
	color: #96785a;
	font-size: 13px;
	line-height: 1.7;
}

.warm-dialog :deep(.el-dialog__title) {
	color: #4a3b30;
	font-weight: 800;
}

@media (max-width: 960px) {
	.chat-layout.with-records {
		grid-template-columns: 1fr;
	}

	.guide-card {
		flex-direction: column;
		align-items: flex-start;
	}
}
</style>
