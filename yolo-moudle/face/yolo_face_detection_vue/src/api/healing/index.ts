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
