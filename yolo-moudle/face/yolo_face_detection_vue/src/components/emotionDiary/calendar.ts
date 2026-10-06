import type { Period } from '/@/api/healing/diary';

export const periods: Array<{ key: Period; label: string; time: string; icon: string }> = [
	{ key: 'morning', label: '上午', time: '00:00–11:59', icon: 'ele-Sunrise' },
	{ key: 'afternoon', label: '下午', time: '12:00–17:59', icon: 'ele-Sunny' },
	{ key: 'evening', label: '晚上', time: '18:00–23:59', icon: 'ele-Moon' },
];
export const dateKey = (d = new Date()) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
export const periodKey = (d = new Date()): Period => (d.getHours() < 12 ? 'morning' : d.getHours() < 18 ? 'afternoon' : 'evening');
export const moodLevel = (score: number) => (score <= 2 ? 0 : score <= 4 ? 1 : score <= 6 ? 2 : score <= 8 ? 3 : 4);
export const moodOptions = [
	{ score: 2, label: '低落', note: '今天可能有些难熬，可以如实记录，不必勉强自己开心。' },
	{ score: 4, label: '有些不安', note: '担心和犹豫也有位置，可以慢慢说说发生了什么。' },
	{ score: 5, label: '平静', note: '很平常的一天呢，平静也是一种力量。' },
	{ score: 8, label: '轻松', note: '这一点轻松值得收好，可以记下让你松一口气的瞬间。' },
	{ score: 10, label: '开心', note: '把今天让你笑起来的小事写下来，留给以后的自己。' },
];
export function calendarDays(month: string): Array<{ date: string; day: number; current: boolean }> {
	const [year, m] = month.split('-').map(Number);
	const first = new Date(year, m - 1, 1, 12);
	const start = new Date(year, m - 1, 1 - ((first.getDay() + 6) % 7), 12);
	return Array.from({ length: 42 }, (_, i) => {
		const d = new Date(start);
		d.setDate(start.getDate() + i);
		return { date: dateKey(d), day: d.getDate(), current: d.getMonth() === m - 1 };
	});
}
