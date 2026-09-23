<template>
	<div class="system-role-dialog-container">
		<el-dialog :title="state.dialog.title" v-model="state.dialog.isShowDialog" width="900px" class="dia">
			<el-form ref="greenhouseDialogFormRef" :model="state.form" size="default" label-width="100px" :rules="state.rules">
				<el-row :gutter="20">
					<el-col :xs="24" :sm="12" :md="12" :lg="12" :xl="12" class="mb15">
						<el-form-item label="区域名称" prop="trashArea">
							<el-input v-model="state.form.trashArea" placeholder="请输入投放垃圾区域名称" clearable />
						</el-form-item>
					</el-col>
					<el-col :xs="24" :sm="12" :md="12" :lg="12" :xl="12" class="mb15">
						<el-form-item label="空余容量" prop="unused">
							<el-input v-model="state.form.unused" placeholder="请输入空余容量" clearable />
						</el-form-item>
					</el-col>
					<el-col :xs="24" :sm="12" :md="12" :lg="12" :xl="12" class="mb15">
						<el-form-item label="已用容量" prop="used">
							<el-input v-model="state.form.used" placeholder="请输入已用容量" />
						</el-form-item>
					</el-col>
					<el-col :xs="24" :sm="12" :md="12" :lg="12" :xl="12" class="mb15">
						<el-form-item label="状态" prop="areaStatus">
							<el-select v-model="state.form.areaStatus" placeholder="请选择状态" clearable style="width: 100%">
								<el-option label="可用" :value="0" />
								<el-option label="已满" :value="1" />
							</el-select>
						</el-form-item>
					</el-col>
				</el-row>
			</el-form>
			<template #footer>
				<span class="dialog-footer">
					<el-button @click="onCancel" size="default">取 消</el-button>
					<el-button type="primary" @click="onSubmit" size="default">{{ state.dialog.submitTxt }}</el-button>
				</span>
			</template>
		</el-dialog>
	</div>
</template>

<script setup lang="ts" name="systemRoleDialog">
import { nextTick, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import type { FormInstance } from 'element-plus';
import request from '/@/utils/request';

// 定义子组件向父组件传值/事件
const emit = defineEmits(['refresh']);

// 定义变量内容
const greenhouseDialogFormRef = ref<FormInstance>();
const state = reactive({
	form: {
		id: null,
		trashArea: '',
		unused: '',
		used: '',
		areaStatus: '',
	},
	rules: {
		trashArea: [{ required: true, message: '请输入区域名称', trigger: 'blur' }],
		unused: [{ required: true, message: '请输入空余容量', trigger: 'blur' }],
		used: [{ required: true, message: '请输入已用容量', trigger: 'blur' }],
		areaStatus: [{ required: true, message: '请选择状态', trigger: 'blur' }]
	},
	dialog: {
		isShowDialog: false,
		type: '',
		title: '',
		submitTxt: '',
	},
});

// 打开弹窗
const openDialog = (type: string, row: any) => {
	if (type === 'edit') {
		state.form = { ...row };
		state.dialog.title = '修改区域信息';
		state.dialog.submitTxt = '修 改';
	} else {
		state.dialog.title = '新增区域信息';
		state.dialog.submitTxt = '新 增';
		// 清空表单
		nextTick(() => {
			state.form = {
				id: null,
				trashArea: '',
				unused: '',
				used: '',
				areaStatus: '',
			};
		});
	}
	state.dialog.isShowDialog = true;
};

// 关闭弹窗
const closeDialog = () => {
	state.dialog.isShowDialog = false;
};

// 取消
const onCancel = () => {
	closeDialog();
};

// 提交
const onSubmit = () => {
	if (!greenhouseDialogFormRef.value) return;
	greenhouseDialogFormRef.value.validate((valid: boolean) => {
		if (valid) {
			if (state.dialog.title === '修改区域信息') {
				request.post('/api/area/update', state.form).then((res) => {
					if (res.code == 0) {
						ElMessage.success('修改成功！');
						closeDialog();
						emit('refresh');
					} else {
						ElMessage({
							type: 'error',
							message: res.msg,
						});
					}
				});
			} else {
				request.post('/api/area', state.form).then((res) => {
					if (res.code == 0) {
						ElMessage.success('添加成功！');
						closeDialog();
						emit('refresh');
					} else {
						ElMessage({
							type: 'error',
							message: res.msg,
						});
					}
				});
			}
		} else {
			return false;
		}
	});
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