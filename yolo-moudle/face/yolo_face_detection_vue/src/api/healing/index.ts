import request from '/@/utils/request';

export function saveAwarenessRecord(data: Record<string, any>) {
	return request.post('/api/awarenessRecords/fromPrediction', data);
}

export function savePrivacyConsent(data: Record<string, any>) {
	return request.post('/api/privacyConsents', data);
}

export function getAwarenessRecords(params: Record<string, any>) {
	return request.get('/api/awarenessRecords', { params });
}

export function getAwarenessRecord(id: number) {
	return request.get(`/api/awarenessRecords/${id}`);
}

export function linkAnalysisRecord(analysisRecordId: number, awarenessRecordId: number) {
	return request.put(`/api/analysisRecords/${analysisRecordId}/awareness/${awarenessRecordId}`);
}

export function getAnalysisRecordByAwareness(awarenessRecordId: number) {
	return request.get(`/api/analysisRecords/by-awareness/${awarenessRecordId}`);
}

export function getPreVisitReportSummary(awarenessRecordId: number) {
	return request.get(`/api/preVisitReportSummaries/by-awareness/${awarenessRecordId}`);
}

export function generatePreVisitReportSummary(awarenessRecordId: number) {
	return request.post(`/api/preVisitReportSummaries/generate/${awarenessRecordId}`);
}

export type SafetyFlag = {
	level: 'prompt' | 'attention' | 'priority';
	reason: string;
};

export type CareLetterData = {
	id: number;
	awarenessRecordId: number;
	analysisRecordId?: number;
	provider?: string;
	modelName?: string;
	promptVersion?: string;
	letterText: string;
	generatedAt?: string;
	cached?: boolean;
};

export function getCareLetter(awarenessRecordId: number) {
	return request.get(`/api/preVisitReportSummaries/care-letter/${awarenessRecordId}`);
}

export function generateCareLetter(awarenessRecordId: number) {
	return request.post(`/api/preVisitReportSummaries/care-letter/generate/${awarenessRecordId}`);
}
