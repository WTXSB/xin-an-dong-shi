<template>
	<div class="diary-experience">
		<div v-if="error" class="diary-notice" role="alert">
			<span>{{ error }}</span
			><el-button round @click="refresh">重新加载</el-button><router-link v-if="loginRequired" to="/login">重新登录</router-link>
		</div>
		<section class="diary-calendar" aria-labelledby="diary-calendar-title">
			<div class="diary-section-heading">
				<div>
					<span class="diary-kicker">THE LITTLE MOMENTS</span>
					<h2 id="diary-calendar-title">情绪日历</h2>
					<p>把一天的三个片刻，轻轻放进日历。</p>
				</div>
				<el-button round :disabled="busy || loading" @click="selectToday">回到今天</el-button>
			</div>
			<div class="diary-calendar-layout" v-loading="loading">
				<div class="diary-month-panel">
					<div class="diary-month-heading">
						<h3>{{ monthTitle }}</h3>
						<div>
							<el-button circle :disabled="busy || month <= '1900-01'" aria-label="上个月" @click="moveMonth(-1)"
								><el-icon><ele-ArrowLeft /></el-icon></el-button
							><el-button circle :disabled="busy || month >= today.slice(0, 7)" aria-label="下个月" @click="moveMonth(1)"
								><el-icon><ele-ArrowRight /></el-icon
							></el-button>
						</div>
					</div>
					<div class="diary-weekdays">
						<span v-for="label in ['一', '二', '三', '四', '五', '六', '日']" :key="label">{{ label }}</span>
					</div>
					<div class="diary-days">
						<button
							v-for="day in days"
							:key="day.date"
							type="button"
							class="diary-day"
							:class="{ selected: selectedDate === day.date, today: day.date === today, outside: !day.current }"
							:disabled="day.date > today || busy"
							:aria-label="dayDescription(day.date)"
							:aria-pressed="selectedDate === day.date"
							@click="selectDate(day.date)"
						>
							<span class="diary-day-number">{{ day.day }}<i v-if="day.date === today">今天</i></span>
							<span class="diary-day-moods"
								><span v-for="slot in periods" :key="slot.key" :class="['diary-mini-mood', { empty: !findMood(day.date, slot.key) }]"
									><AnimalMood v-if="findMood(day.date, slot.key)" :score="findMood(day.date, slot.key)!.score" /><span v-else>·</span></span
								></span
							>
						</button>
					</div>
					<p class="diary-calendar-caption">每格从左至右：上午 · 下午 · 晚上。点选日期，回看或补记。</p>
				</div>
				<aside class="diary-day-detail">
					<span class="diary-kicker">A DAY, THREE MOMENTS</span>
					<h3>{{ selectedTitle }}</h3>
					<div v-for="slot in periods" :key="slot.key" class="diary-slot">
						<el-icon class="diary-slot-icon"><component :is="slot.icon" /></el-icon>
						<div class="diary-slot-copy">
							<strong>{{ slot.label }}</strong
							><small>{{ slot.time }}</small
							><span>{{ findMood(selectedDate, slot.key) ? moodName(findMood(selectedDate, slot.key)!.score) : '还没有记录' }}</span>
						</div>
						<AnimalMood v-if="findMood(selectedDate, slot.key)" :score="findMood(selectedDate,slot.key)!.score" />
						<button
							type="button"
							class="diary-slot-action"
							:disabled="!canRecord(slot.key) || busy || loading || loginRequired"
							@click="openMood(slot.key)"
						>
							{{ findMood(selectedDate, slot.key) ? `${findMood(selectedDate, slot.key)!.score}分 · 修改` : '记一笔' }}
						</button>
					</div>
					<p class="diary-small-note">心情是自己的感受，不是测验成绩。没有记录的时段会保持空白。</p>
				</aside>
			</div>
		</section>

		<section class="diary-book-section" aria-labelledby="diary-book-title">
			<div class="diary-section-heading">
				<div>
					<span class="diary-kicker">DEAR DIARY</span>
					<h2 id="diary-book-title">把今天写下来</h2>
					<p>不必写得完整，复杂的心情也可以在同一页相遇。</p>
				</div>
				<span class="diary-save-status" aria-live="polite">{{ dirty ? '有未保存内容' : savedAt ? '已保存至你的账号' : '等待你的第一行' }}</span>
			</div>
			<div class="diary-book" v-loading="entryLoading">
				<div class="diary-book-spine" aria-hidden="true"></div>
				<span class="diary-book-ribbon" aria-hidden="true"></span>
				<div class="diary-book-left">
					<span class="diary-kicker">ONE PAGE AT A TIME</span>
					<h3>{{ selectedTitle }}</h3>
					<p>今天发生了什么？<br />哪个片刻，让你的心情发生了变化？</p>
					<div class="diary-book-moods">
						<span v-for="slot in periods" :key="slot.key"
							><AnimalMood v-if="findMood(selectedDate, slot.key)" :score="findMood(selectedDate,slot.key)!.score" /><el-icon v-else
								><component :is="slot.icon" /></el-icon
							><small>{{ slot.label }}</small></span
						>
					</div>
					<img class="diary-book-art" :src="gardenArt" alt="" />
				</div>
				<div class="diary-book-right">
					<label class="diary-paper-label" for="daily-diary-text">{{ selectedDate }} · 我的日记</label
					><textarea
						id="daily-diary-text"
						v-model="draft"
						:disabled="busy || entryLoading || loginRequired"
						maxlength="10000"
						placeholder="从一件小事开始写吧……"
						@input="consent = false"
					></textarea>
					<div class="diary-word-count">
						<span>{{ draft.length }} / 10000 字</span><span>只在你选择分析时发送给 AI</span>
					</div>
				</div>
			</div>
			<div class="diary-writing-actions">
				<el-button round :disabled="busy || entryLoading || loginRequired || !dirty" :loading="saving" @click="saveCurrent">保存日记</el-button>
				<div class="diary-ai-consent">
					<el-checkbox v-model="consent" :disabled="busy || !draft.trim()">我同意将这一天的日记与三次心情评分发送给 DeepSeek 分析</el-checkbox
					><small>{{
						aiConfigured ? 'AI 分析是辅助回顾，不是心理或医学诊断。' : 'AI 尚未连接：日记可以正常保存，需在后端配置密钥后才能分析。'
					}}</small>
				</div>
				<el-button
					type="primary"
					round
					:loading="analyzing"
					:disabled="busy || entryLoading || !consent || !draft.trim() || loginRequired"
					@click="analyzeCurrent"
					>{{ analyzing ? '正在读懂这一页' : '分析日记' }}</el-button
				>
			</div>
			<p v-if="analysisError" class="diary-analysis-error" role="alert">{{ analysisError }}</p>
			<DiaryInsightCard v-if="entry?.analysis && !dirty" :entry="entry" />
			<p v-if="entry?.analysis && !dirty" class="diary-analysis-link">这份回顾也已收进 <router-link to="/trashRecords">觉察记录</router-link>。</p>
		</section>
		<el-dialog
			v-model="moodOpen"
			class="diary-mood-dialog"
			width="610px"
			append-to-body
			destroy-on-close
			:close-on-click-modal="false"
			:close-on-press-escape="!moodSaving"
			:show-close="!moodSaving"
			aria-label="心情打分"
			@closed="moodError = ''"
		>
			<template #header
				><span class="diary-modal-kicker">{{ moodDate }} · {{ periods.find((p) => p.key === moodPeriod)?.label }}的片刻</span></template
			>
			<div class="diary-mood-paper">
				<div class="diary-mood-seal" aria-hidden="true">
					<el-icon><ele-Sunny /></el-icon>
				</div>
				<h2>今天感觉怎么样？</h2>
				<p class="diary-mood-sub">安小宁想听听你的心情</p>
				<div class="diary-animal-choices">
					<button
						v-for="option in moodOptions"
						:key="option.score"
						type="button"
						:class="{ chosen: moodLevel(score) === moodLevel(option.score) }"
						:disabled="moodSaving"
						:aria-pressed="moodLevel(score) === moodLevel(option.score)"
						@click="score = option.score"
					>
						<AnimalMood :score="option.score" /><span>{{ option.label }}</span>
					</button>
				</div>
				<div class="diary-score-display">
					<strong>{{ score }}</strong
					><span>/ 10</span>
				</div>
				<label for="mood-score-slider" class="diary-slider-label">此刻的心情分数</label
				><input
					id="mood-score-slider"
					v-model.number="score"
					type="range"
					min="1"
					max="10"
					step="1"
					:disabled="moodSaving"
					:aria-valuetext="`${score} 分，${moodName(score)}`"
				/>
				<div class="diary-feedback" aria-live="polite">{{ moodOptions[moodLevel(score)].note }}</div>
				<p v-if="moodError" class="diary-analysis-error" role="alert">{{ moodError }}</p>
				<el-button type="primary" round :loading="moodSaving" class="diary-record-mood" @click="submitMood"
					>记录{{ periods.find((p) => p.key === moodPeriod)?.label }}的心情</el-button
				><button class="diary-later" type="button" :disabled="moodSaving" @click="moodOpen = false">稍后在日历里记录</button
				><img class="diary-mood-garden" :src="gardenArt" alt="" />
			</div>
		</el-dialog>
	</div>
</template>

<script setup lang="ts">
import { ref, computed, onActivated, onMounted, onBeforeUnmount, watch } from 'vue';
import { onBeforeRouteLeave, useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
	claimMoodPrompt,
	getDiaryStatus,
	getMoodMonth,
	getDiary,
	saveMood,
	saveDiary,
	analyzeDiary,
	type DiaryEntry,
	type Mood,
	type Period,
} from '/@/api/healing/diary';
import { dateKey, periodKey, calendarDays, periods, moodOptions, moodLevel } from './calendar';
import AnimalMood from './AnimalMood.vue';
import DiaryInsightCard from './DiaryInsightCard.vue';
import gardenArt from '/@/assets/healing-frames/diary-stationery-garden-v1.png';
const route = useRoute();
const today = ref(dateKey()),
	selectedDate = ref(today.value),
	month = ref(today.value.slice(0, 7));
const moods = ref<Mood[]>([]),
	entry = ref<DiaryEntry | null>(null),
	draft = ref(''),
	savedAt = ref('');
const loading = ref(false),
	entryLoading = ref(false),
	saving = ref(false),
	analyzing = ref(false),
	error = ref(''),
	loginRequired = ref(false),
	analysisError = ref(''),
	aiConfigured = ref(false),
	consent = ref(false);
const moodOpen = ref(false),
	moodSaving = ref(false),
	moodDate = ref(today.value),
	moodPeriod = ref<Period>(periodKey()),
	score = ref(5),
	moodError = ref('');
let enterPending = false,
	active = false,
	loadSequence = 0;
const dirty = computed(() => !!entry.value && draft.value !== entry.value.content),
	busy = computed(() => saving.value || analyzing.value || moodSaving.value);
const days = computed(() => calendarDays(month.value)),
	monthTitle = computed(() => `${Number(month.value.slice(0, 4))} 年 ${Number(month.value.slice(5))} 月`);
const selectedTitle = computed(() =>
	new Date(`${selectedDate.value}T12:00:00`).toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' })
);
const findMood = (date: string, period: Period) => moods.value.find((m) => m.date === date && m.period === period);
const moodName = (n: number) => moodOptions[moodLevel(n)].label;
const canRecord = (period: Period) =>
	selectedDate.value < today.value ||
	(selectedDate.value === today.value && periods.findIndex((p) => p.key === period) <= periods.findIndex((p) => p.key === periodKey()));
const dayDescription = (date: string) =>
	`${date}，${periods
		.map((p) => {
			const m = findMood(date, p.key);
			return `${p.label}${m ? `${m.score}分，${moodName(m.score)}` : '未记录'}`;
		})
		.join('；')}`;
function setError(e: any) {
	error.value = e?.message || '暂时无法加载记录。';
	loginRequired.value = e?.code === '403';
}
async function load() {
	const sequence = ++loadSequence;
	loading.value = true;
	entryLoading.value = true;
	entry.value = null;
	draft.value = '';
	savedAt.value = '';
	try {
		const [list, data, status] = await Promise.all([getMoodMonth(month.value), getDiary(selectedDate.value), getDiaryStatus()]);
		if (sequence !== loadSequence) return;
		moods.value = list;
		entry.value = data;
		draft.value = data.content;
		savedAt.value = data.updatedAt || '';
		aiConfigured.value = !!status.aiConfigured;
		error.value = '';
		loginRequired.value = false;
	} catch (e) {
		if (sequence === loadSequence) setError(e);
	} finally {
		if (sequence === loadSequence) {
			loading.value = false;
			entryLoading.value = false;
		}
	}
}
async function confirmLeave() {
	if (!dirty.value) return true;
	try {
		await ElMessageBox.confirm('这一页还有未保存的文字。保存后再离开吗？', '收好这一页', {
			confirmButtonText: '保存并继续',
			cancelButtonText: '继续编辑',
			closeOnClickModal: false,
		});
		return await saveCurrent();
	} catch {
		return false;
	}
}
async function selectDate(date: string) {
	if (busy.value || date > today.value) return;
	if (!(await confirmLeave())) return;
	selectedDate.value = date;
	month.value = date.slice(0, 7);
	consent.value = false;
	analysisError.value = '';
	await load();
}
async function selectToday() {
	today.value = dateKey();
	await selectDate(today.value);
}
async function moveMonth(step: number) {
	if (busy.value || !(await confirmLeave())) return;
	const [y, m] = month.value.split('-').map(Number);
	const target = dateKey(new Date(y, m - 1 + step, 1, 12)).slice(0, 7);
	if (target > today.value.slice(0, 7) || target < '1900-01') return;
	month.value = target;
	selectedDate.value = target === today.value.slice(0, 7) ? today.value : `${target}-01`;
	consent.value = false;
	analysisError.value = '';
	await load();
}
async function refresh() {
	if (await confirmLeave()) await load();
}
function openMood(period: Period, date = selectedDate.value) {
	if (busy.value || moodOpen.value || loginRequired.value) return;
	moodDate.value = date;
	moodPeriod.value = period;
	score.value = findMood(date, period)?.score || 5;
	moodError.value = '';
	moodOpen.value = true;
}
async function submitMood() {
	if (moodSaving.value) return;
	moodSaving.value = true;
	try {
		const data = await saveMood(moodDate.value, moodPeriod.value, score.value);
		if (selectedDate.value === moodDate.value) {
			const keepDraft = draft.value;
			const previousEntry = entry.value;
			const hadDraft = dirty.value;
			if (hadDraft && previousEntry && data.content !== previousEntry.content) {
				// Do not rebase a local draft onto text changed in another window.
				entry.value = { ...data, content: previousEntry.content, revision: previousEntry.revision };
				ElMessage.warning('心情已保存，但日记已在其他窗口更新。请保留当前文字，刷新后再编辑。');
			} else {
				entry.value = data;
			}
			draft.value = hadDraft ? keepDraft : data.content;
			consent.value = false;
		}
		moods.value = await getMoodMonth(month.value);
		moodOpen.value = false;
		window.dispatchEvent(new Event('mindease:diary-updated'));
		ElMessage.success('这个片刻已收进情绪日历。');
	} catch (e: any) {
		moodError.value = e.message;
	} finally {
		moodSaving.value = false;
	}
}
async function saveCurrent() {
	if (!entry.value || saving.value) return false;
	saving.value = true;
	try {
		entry.value = await saveDiary(selectedDate.value, draft.value, entry.value.revision);
		savedAt.value = entry.value.updatedAt || 'saved';
		window.dispatchEvent(new Event('mindease:diary-updated'));
		ElMessage.success('这一页已经保存。');
		return true;
	} catch (e: any) {
		analysisError.value = e.message;
		return false;
	} finally {
		saving.value = false;
	}
}
async function analyzeCurrent() {
	if (analyzing.value || !consent.value || !entry.value) return;
	analysisError.value = '';
	if (dirty.value && !(await saveCurrent())) return;
	analyzing.value = true;
	const date = selectedDate.value,
		revision = entry.value.revision;
	try {
		entry.value = await analyzeDiary(date, revision, true);
		consent.value = false;
		window.dispatchEvent(new Event('mindease:diary-updated'));
		ElMessage.success('日记回顾已收进觉察记录。');
	} catch (e: any) {
		analysisError.value = e.message;
	} finally {
		analyzing.value = false;
	}
}
async function enter() {
	if (!active || route.path !== '/dataView' || enterPending || moodOpen.value || busy.value) return;
	enterPending = true;
	today.value = dateKey();
	try {
		const claim = await claimMoodPrompt();
		if (claim.show) openMood(claim.period, claim.date);
	} catch (e) {
		setError(e);
	} finally {
		enterPending = false;
	}
}
const navEnter = () => {
	if (route.path === '/dataView') void enter();
};
const beforeUnload = (e: BeforeUnloadEvent) => {
	if (dirty.value || busy.value) {
		e.preventDefault();
		e.returnValue = '';
	}
};
onBeforeRouteLeave(async () => (busy.value ? false : await confirmLeave()));
onMounted(() => {
	active = true;
	void load().then(enter);
	window.addEventListener('mindease:diary-enter', navEnter);
	window.addEventListener('beforeunload', beforeUnload);
});
onActivated(() => {
	active = true;
	if (entry.value && !dirty.value) void load().then(enter);
});
watch(
	() => route.path,
	(p) => {
		active = p === '/dataView';
		if (!active) moodOpen.value = false;
	}
);
onBeforeUnmount(() => {
	active = false;
	window.removeEventListener('mindease:diary-enter', navEnter);
	window.removeEventListener('beforeunload', beforeUnload);
});
</script>
<style lang="scss">
@use './emotion-diary.scss';
</style>
