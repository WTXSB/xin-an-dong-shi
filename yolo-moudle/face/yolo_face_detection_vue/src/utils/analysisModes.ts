export const analysisModeItems = [
	{ value: 'combined', label: '综合分析（情绪 + BFRB双证据）' },
	{ value: 'emotion', label: '情绪表情识别' },
	{ value: 'bfrb_behavior', label: 'BFRB行为直检' },
	{ value: 'bfrb_geometry', label: 'BFRB手脸几何' },
];

const modelOptions: Record<string, Array<{ value: string; label: string }>> = {
	combined: [{ value: 'combined', label: '多模型协作：情绪 + 行为 + 手脸几何' }],
	emotion: [{ value: 'emotion.pt', label: 'emotion.pt（情绪识别）' }],
	bfrb_behavior: [{ value: 'bfrb_behavior.pt', label: 'bfrb_behavior.pt（行为直检）' }],
	bfrb_geometry: [{ value: 'bfrb_geometry', label: '手部姿态 + 人脸几何规则' }],
};

export const getAnalysisModelOptions = (mode: string) => modelOptions[mode] || modelOptions.combined;

export interface BfrbCue {
	behavior?: string;
	cueType: string;
	confidence: number;
	bbox?: number[];
	evidenceType?: string;
	geometry?: Record<string, any>;
}

export interface BfrbEvent {
	id: string;
	behavior: string;
	cueType: string;
	startSeconds: number;
	endSeconds: number;
	durationSeconds: number;
	averageConfidence: number;
	maxConfidence: number;
	keyFrameSeconds: number;
	evidenceType: string;
	geometry?: Record<string, any>;
}

export interface AnalysisResult {
	scene?: 'image' | 'video' | 'camera';
	mode?: string;
	sessionId?: string;
	analysisRecordId?: number;
	structuredSaved?: boolean;
	emotionSummary?: Array<{
		label: string;
		frameCount: number;
		averageConfidence: number;
		maxConfidence: number;
	}>;
	bfrbSummary?: {
		eventCount: number;
		totalDurationSeconds: number;
		events: BfrbEvent[];
		byBehavior: Array<{
			cueType: string;
			count: number;
			totalDurationSeconds: number;
			maxConfidence: number;
		}>;
		rules?: { note?: string };
	};
}

export const createAnalysisSessionId = () => {
	if (typeof globalThis.crypto?.randomUUID === 'function') return globalThis.crypto.randomUUID();
	return `analysis-${Date.now()}-${Math.random().toString(16).slice(2)}`;
};

export const confidencePercent = (value?: number) => {
	const number = Number(value || 0);
	return Math.round((number <= 1 ? number * 100 : number) * 10) / 10;
};

export const evidenceLabel = (value?: string) => {
	if (value === 'behavior-model+hand-face-geometry') return '行为模型 + 手脸几何双证据';
	if (value === 'hand-face-geometry') return '手脸几何证据';
	return 'BFRB行为模型证据';
};
