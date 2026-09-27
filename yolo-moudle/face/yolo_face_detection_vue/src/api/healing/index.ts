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
