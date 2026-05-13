import api from './client';
import type {
  AuthResponse, ConversionResult, HistoricalDataPoint,
  Alert, PageResponse, ConversionHistoryItem, User
} from '../types';

export const authApi = {
  register: (name: string, email: string, password: string) =>
    api.post<AuthResponse>('/api/auth/register', { name, email, password }),
  login: (email: string, password: string) =>
    api.post<AuthResponse>('/api/auth/login', { email, password }),
};

export const ratesApi = {
  getCurrent: (base = 'USD') =>
    api.get<Record<string, number>>('/api/rates/current', { params: { base } }),
  convert: (from: string, to: string, amount: number) =>
    api.get<ConversionResult>('/api/rates/convert', { params: { from, to, amount } }),
  getHistorical: (from: string, to: string, period: string) =>
    api.get<HistoricalDataPoint[]>('/api/rates/historical', { params: { from, to, period } }),
};

export const historyApi = {
  getHistory: (page = 0, size = 20) =>
    api.get<PageResponse<ConversionHistoryItem>>('/api/history', { params: { page, size } }),
};

export const alertsApi = {
  getAlerts: () => api.get<Alert[]>('/api/alerts'),
  createAlert: (fromCurrency: string, toCurrency: string, targetRate: number, direction: 'ABOVE' | 'BELOW') =>
    api.post<Alert>('/api/alerts', { fromCurrency, toCurrency, targetRate, direction }),
  deleteAlert: (id: number) => api.delete(`/api/alerts/${id}`),
};

export const userApi = {
  getProfile: () => api.get<User>('/api/user/profile'),
  addFavorite: (pair: string) => api.post('/api/user/favorites', { pair }),
  removeFavorite: (pair: string) => api.delete(`/api/user/favorites/${encodeURIComponent(pair)}`),
};
