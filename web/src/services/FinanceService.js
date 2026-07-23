import apiClient from '../api/apiClient';

export const FinanceService = {
  getExpenses: async () => {
    const response = await apiClient.get('/expenses');
    return response.data || [];
  },

  getIncomes: async () => {
    const response = await apiClient.get('/incomes');
    return response.data || [];
  },

  getBills: async () => {
    const response = await apiClient.get('/bills');
    return response.data || [];
  },

  getGoals: async () => {
    const response = await apiClient.get('/goals');
    return response.data || [];
  }
};
