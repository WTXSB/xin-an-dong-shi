import request from '/@/utils/request';

/**
 * 「心有灵犀」医患联动模块接口
 * 统一返回 { code: '0', data: ... } 包装，code 非 '0' 为错误（含 403 / 404）
 */

export type IdentityType = 'patient' | 'practitioner';
export type AuditStatus = 'pending' | 'approved' | 'rejected';

export interface IdentityApplyPayload {
	username: string;
	identityType: IdentityType;
	realName: string;
	idCard?: string;
	licenseNo?: string;
	hospital?: string;
	department?: string;
	title?: string;
	bio?: string;
}

export interface IdentityRecord {
	identityType: IdentityType;
	realName: string;
	idCard?: string;
	licenseNo?: string;
	hospital?: string;
	department?: string;
	title?: string;
	bio?: string;
	auditStatus: AuditStatus;
	auditNote?: string;
}

export interface PendingIdentity extends IdentityRecord {
	username: string;
}

export interface Practitioner {
	username: string;
	realName: string;
	hospital?: string;
	department?: string;
	title?: string;
	bio?: string;
}

export interface SpaRequestItem {
	id: number;
	patientUsername: string;
	practitionerUsername: string;
	initialMessage: string;
	status: string;
	createdAt?: string;
	/** 对方展示名（后端字段名可能不同，读取时做多字段回退） */
	counterpartName?: string;
	counterpartHospital?: string;
	counterpartDepartment?: string;
	counterpartTitle?: string;
	latestMessage?: string | { content?: string; senderUsername?: string; createdAt?: string } | null;
	[key: string]: any;
}

export interface SpaMessage {
	id: number;
	senderUsername: string;
	senderIdentity?: IdentityType;
	content: string;
	createdAt?: string;
}

export interface PatientRecordItem {
	id: number;
	sourceType?: string;
	emotionLabel?: string;
	gentleSummary?: string;
	createdAt?: string;
}

export interface ReviewPayload {
	adminUsername: string;
	username: string;
	approve: boolean;
	note?: string;
}

// 提交身份认证申请（患者实名 / 心灵SPA师认证）
export function applyIdentity(data: IdentityApplyPayload) {
	return request.post('/api/spa/identity/apply', data);
}

// 查询我的身份记录，无记录时 code 为 '404'
export function getMyIdentity(username: string) {
	return request.get('/api/spa/identity/mine', { params: { username } });
}

// （admin）待审核身份列表
export function getPendingIdentities(username: string) {
	return request.get('/api/spa/identity/pending', { params: { username } });
}

// （admin）审核身份
export function reviewIdentity(data: ReviewPayload) {
	return request.post('/api/spa/identity/review', data);
}

// 已通过认证的心灵SPA师列表
export function getPractitioners() {
	return request.get('/api/spa/practitioners');
}

// 创建求助单（写第一封信）
export function createSpaRequest(data: { patientUsername: string; practitionerUsername: string; initialMessage: string }) {
	return request.post('/api/spa/requests', data);
}

// 我的求助单列表
export function getMyRequests(username: string) {
	return request.get('/api/spa/requests/mine', { params: { username } });
}

// 在求助单中发送一条消息
export function sendSpaMessage(requestId: number, data: { username: string; content: string }) {
	return request.post(`/api/spa/requests/${requestId}/messages`, data);
}

// 增量拉取消息（升序），afterId 之后的消息
export function getSpaMessages(requestId: number, username: string, afterId = 0) {
	return request.get(`/api/spa/requests/${requestId}/messages`, { params: { username, afterId } });
}

// （仅该单心灵SPA师）查看对方的近期觉察记录
export function getPatientRecords(requestId: number, username: string) {
	return request.get(`/api/spa/requests/${requestId}/patient-records`, { params: { username } });
}

/** 判断接口是否成功（后端 code 可能是字符串 '0' 或数字 0） */
export function isOk(res: any): boolean {
	return res && (res.code === '0' || res.code === 0);
}

/** 判断是否“无记录”（code 为 '404'） */
export function isNotFound(res: any): boolean {
	return res && (res.code === '404' || res.code === 404);
}
