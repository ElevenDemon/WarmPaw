export interface Envelope<T> { code: number; message: string; data: T }
export interface Page<T> { list: T[]; total: number; page: number; size: number }
export interface User { id: number; name: string; role: 'OWNER' | 'SITTER'; permissions: string[]; canSwitchToSitter?: boolean }
export interface RecordData { id: number; [key: string]: any }
