export interface MockUser {
  name: string
  companion: string
  avatar: string
  mood: string
  moodEmoji: string
  streakDays: number
}

export const mockUser: MockUser = {
  name: '浩诚',
  companion: '安小宁',
  avatar: '',
  mood: '平静',
  moodEmoji: '😌',
  streakDays: 7,
}
