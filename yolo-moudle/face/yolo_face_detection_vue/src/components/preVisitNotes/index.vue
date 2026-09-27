<template>
	<details class="pre-visit-notes">
		<summary>
			<div>
				<strong>补充预诊信息</strong>
				<span>选填·帮助后续报告更完整</span>
			</div>
			<em>展开填写</em>
		</summary>

		<div class="notes-content">
			<label>
				<span>本次主诉</span>
				<el-input
					v-model="complaintValue"
					type="textarea"
					:rows="2"
					maxlength="500"
					show-word-limit
					:disabled="disabled"
					placeholder="例如：最近两周容易紧张，会反复咬指甲"
				/>
			</label>

			<label>
				<span>补充说明</span>
				<el-input
					v-model="additionalNotesValue"
					type="textarea"
					:rows="3"
					maxlength="1000"
					show-word-limit
					:disabled="disabled"
					placeholder="例如：通常在考试前出现，近期睡眠较少，已影响学习专注"
				/>
			</label>

			<p>内容会随本次结构化分析保存，仅用于生成预诊参考信息。不填写也可以正常完成感知。</p>
		</div>
	</details>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps({
	complaint: {
		type: String,
		default: '',
	},
	additionalNotes: {
		type: String,
		default: '',
	},
	disabled: {
		type: Boolean,
		default: false,
	},
});

const emit = defineEmits(['update:complaint', 'update:additionalNotes']);

const complaintValue = computed({
	get: () => props.complaint,
	set: (value: string) => emit('update:complaint', value),
});

const additionalNotesValue = computed({
	get: () => props.additionalNotes,
	set: (value: string) => emit('update:additionalNotes', value),
});
</script>

<style scoped lang="scss">
.pre-visit-notes {
	margin: 18px 0;
	border: 1px solid #eadbc8;
	border-radius: 18px;
	background: rgba(255, 252, 247, 0.94);
	box-shadow: 0 12px 30px rgba(122, 91, 57, 0.06);
	overflow: hidden;
}

.pre-visit-notes summary {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 20px;
	padding: 18px 22px;
	cursor: pointer;
	list-style: none;
	color: #554739;
}

.pre-visit-notes summary::-webkit-details-marker {
	display: none;
}

.pre-visit-notes summary div {
	display: flex;
	align-items: baseline;
	gap: 12px;
	flex-wrap: wrap;
}

.pre-visit-notes summary strong {
	font-size: 16px;
}

.pre-visit-notes summary span,
.pre-visit-notes summary em {
	font-size: 13px;
	font-style: normal;
	color: #9a7b5a;
}

.pre-visit-notes summary em::after {
	content: '＋';
	display: inline-block;
	margin-left: 8px;
	font-size: 18px;
	transition: transform 0.2s ease;
}

.pre-visit-notes[open] summary em::after {
	transform: rotate(45deg);
}

.notes-content {
	display: grid;
	grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
	gap: 18px;
	padding: 0 22px 20px;
	border-top: 1px solid #f1e7da;
}

.notes-content label {
	display: grid;
	gap: 8px;
	padding-top: 18px;
}

.notes-content label > span {
	font-size: 14px;
	font-weight: 700;
	color: #655442;
}

.notes-content p {
	grid-column: 1 / -1;
	margin: 0;
	font-size: 12px;
	line-height: 1.7;
	color: #9b846b;
}

:deep(.el-textarea__inner) {
	border: 0;
	border-radius: 12px;
	background: #fffaf3;
	box-shadow: 0 0 0 1px #eadfce inset;
	color: #4f4438;
	line-height: 1.6;
}

:deep(.el-textarea__inner:focus) {
	box-shadow: 0 0 0 1px #c9a777 inset;
}

@media (max-width: 900px) {
	.notes-content {
		grid-template-columns: 1fr;
	}

	.notes-content p {
		grid-column: auto;
	}
}
</style>
