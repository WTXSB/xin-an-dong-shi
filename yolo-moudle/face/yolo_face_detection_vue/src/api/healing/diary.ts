import request from '/@/utils/request';

export type Period = 'morning' | 'afternoon' | 'evening';
export type Mood = { date: string; period: Period; score: number };
export type DiaryAnalysis = {
	summary: string;
	emotions: Array<{ label: string; evidence: string; reflection: string }>;
	suggestion: string;
	blessing: string;
	safetyNote?: string;
	provider: string;
	model: string;
	generatedAt: string;
};
export type DiaryEntry = { date: string; content: string; revision: number; analysis: DiaryAnalysis | null; updatedAt?: string };

async function result(call: Promise<any>) {
	const response = await call;
	if (String(response?.code) !== '0') {
		const error = new Error(response?.msg || '记录暂时没有连上，请稍后重试。') as Error & { code?: string };
		error.code = String(response?.code);
		throw error;
	}
	return response.data;
}
const timezoneOffset = () => new Date().getTimezoneOffset();
export const getDiaryStatus = () => result(request.get('/api/diary/status'));
export const claimMoodPrompt = () => result(request.post('/api/diary/prompt', { timezoneOffset: timezoneOffset() }));
export const getMoodMonth = (month: string): Promise<Mood[]> => result(request.get('/api/diary/month', { params: { month } }));
export const getDiary = (date: string): Promise<DiaryEntry> =>
	result(request.get('/api/diary/entry', { params: { date, timezoneOffset: timezoneOffset() } }));
export const saveMood = (date: string, period: Period, score: number): Promise<DiaryEntry> =>
	result(request.post('/api/diary/mood', { date, period, score, timezoneOffset: timezoneOffset() }));
export const saveDiary = (date: string, content: string, revision: number): Promise<DiaryEntry> =>
	result(request.put('/api/diary/entry', { date, content, revision, timezoneOffset: timezoneOffset() }));
export const analyzeDiary = (date: string, revision: number, consent: boolean): Promise<DiaryEntry> =>
	result(request.post('/api/diary/analyze', { date, revision, consent, timezoneOffset: timezoneOffset() }));
export const getDiaryInsights = (page = 1): Promise<DiaryEntry[]> => result(request.get('/api/diary/insights', { params: { page } }));
