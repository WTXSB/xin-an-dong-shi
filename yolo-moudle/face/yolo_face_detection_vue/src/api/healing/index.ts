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

export function linkAnalysisRecord(analysisRecordId: number, awarenessRecordId: number) {
	return request.put(`/api/analysisRecords/${analysisRecordId}/awareness/${awarenessRecordId}`);
}

export function getAnalysisRecordByAwareness(awarenessRecordId: number) {
	return request.get(`/api/analysisRecords/by-awareness/${awarenessRecordId}`);
}
