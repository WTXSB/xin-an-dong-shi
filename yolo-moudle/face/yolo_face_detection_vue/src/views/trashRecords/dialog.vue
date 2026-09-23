<template>
	<div class="system-role-dialog-container">
		<el-dialog :title="state.dialog.title" v-model="state.dialog.isShowDialog" width="900px" class="dia">
			<el-form ref="greenhouseDialogFormRef" :model="state.form" size="default" label-width="100px" :rules="state.rules">
				<el-row :gutter="20">
					<el-col :xs="24" :sm="12" :md="12" :lg="12" :xl="12" class="mb15">
						<el-form-item label="情绪种类" prop="emotionKind">
							<el-select
								v-model="state.form.emotionKind"
								placeholder="请选择情绪种类"
								clearable
								style="width: 100%"
							>
								<el-option
									v-for="item in emotionKindOptions"
									:key="item.value"
									:label="item.label"
									:value="item.value"
								/>
							</el-select>
						</el-form-item>
					</el-col>
					
					<el-col :xs="24" :sm="12" :md="12" :lg="12" :xl="12" class="mb15">
						<el-form-item label="文字内容" prop="txt">
							<el-input
								v-model="state.form.txt"
								placeholder="请输入文字内容"
								clearable
								filterable
								:loading="state.txtLoading"
								style="width: 100%"
							>
							</el-input>
						</el-form-item>
					</el-col>
					<el-col :xs="24" :sm="12" :md="12" :lg="12" :xl="12" class="mb15">
						<el-form-item label="时间" prop="recordTime">
							<el-date-picker
								v-model="state.form.startTime"
								type="datetime"
								placeholder="请选择时间"
								style="width: 100%"
								value-format="YYYY-MM-DD HH:mm:ss"
							/>
						</el-form-item>
					</el-col>
				</el-row>
			</el-form>
			<template #footer>
				<span class="dialog-footer">
					<el-button @click="onCancel" size="default">取 消</el-button>
					<el-button type="primary" @click="onSubmit" size="default" :loading="state.submitLoading">{{ state.dialog.submitTxt }}</el-button>
				</span>
			</template>
		</el-dialog>
	</div>
</template>

<script setup lang="ts" name="systemRoleDialog">
import { reactive, ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import type { FormInstance } from 'element-plus';
import request from '/@/utils/request';

// 定义子组件向父组件传值/事件
const emit = defineEmits(['refresh']);

// 定义选项项类型
interface OptionItem {
	value: string | number;
	label: string;
}


const emotionKindOptions = [
  { value: '高兴', label: '高兴' },
  { value: '悲伤', label: '悲伤' },
  { value: '生气', label: '生气' },
  { value: '中性', label: '中性' }
];

// 定义变量内容
const greenhouseDialogFormRef = ref<FormInstance>();
const state = reactive({
	form: {
		id: null,
		emotionKind: '',
		txt: '',
		startTime: '',
	},
	txtOptions: [] as OptionItem[], 
	txtLoading: false, // 选项加载状态
	submitLoading: false, // 提交加载状态
	rules: {
		emotionKind: [{ required: true, message: '请选择情绪种类', trigger: 'change' }],
		txt: [{ required: true, message: '请输入文字', trigger: 'change' }],
		startTime: [{ required: true, message: '请选择时间', trigger: 'change' }]
	},
	dialog: {
		isShowDialog: false,
		type: '',
		title: '',
		submitTxt: '',
	},
});

// 组件挂载时加载一次区域选项
onMounted(() => {
	getParkAreaOptions();
});

// 获取区域选项
const getParkAreaOptions = async () => {
	state.txtLoading = true;
	try {
		const res = await request.get('/api/area');
		if (res.code == 0) {
			state.txtOptions = res.data.records.map((item: any) => ({
				value: item.txt,
				label: item.txt
			}));
		} else if (res.msg) {
			ElMessage.warning(res.msg);
		}
	} catch (error) {
		ElMessage.error('暂时没有取到数据，请稍后再试');
	} finally {
		state.txtLoading = false;
	}
};

// 打开弹窗
const openDialog = (type: string, row: any = {}) => {
	state.dialog.type = type;
	
	if (type === 'edit') {
		state.form = { ...row };
		state.dialog.title = '修改情绪记录信息';
		state.dialog.submitTxt = '修 改';
	} else {
		state.dialog.title = '新增情绪记录信息';
		state.dialog.submitTxt = '新 增';
		// 清空表单
		state.form = {
			id: null,
			emotionKind: '',
			txt: '',
			startTime: '',
		};
	}
	state.dialog.isShowDialog = true;
	
	// 如果选项为空则重新加载
	if (!state.txtOptions.length) {
		getParkAreaOptions();
	}
};

// 关闭弹窗
const closeDialog = () => {
	state.dialog.isShowDialog = false;
	state.submitLoading = false;
};

// 取消
const onCancel = () => {
	closeDialog();
};

// 提交
const onSubmit = async () => {
	if (!greenhouseDialogFormRef.value) return;
	
	try {
		// 表单验证
		const valid = await greenhouseDialogFormRef.value.validate();
		if (!valid) return;
		
		state.submitLoading = true;
		
		const isEdit = state.dialog.type === 'edit';
		const apiUrl = isEdit ? '/api/emotion/update' : '/api/emotion';
		const successMsg = isEdit ? '修改成功' : '添加成功';
		
		// 提交数据
		const res = await request.post(apiUrl, state.form);
		
		if (res.code == 0) {
			ElMessage.success(successMsg);
			closeDialog();
			emit('refresh');
		} else {
			ElMessage.error(res.msg || '操作失败');
		}
	} catch (error) {
		console.error('提交失败:', error);
	} finally {
		state.submitLoading = false;
	}
};

// 暴露变量
defineExpose({
	openDialog,
});
</script>

<style scoped lang="scss">
:deep(.dia) {
	.el-dialog {
		margin-top: 8vh !important;
		.el-dialog__body {
			padding: 15px 20px;
		}
		.el-dialog__header {
			padding: 15px 20px;
			margin-right: 0;
		}
		.el-dialog__footer {
			padding: 15px 20px;
		}
	}
}

.el-form {
	width: 100%;
	margin: 0;
}

.mb15 {
	margin-bottom: 15px;
}
</style>
