import api from './api';

export const fraudService = {
  /**
   * Get KPI and risk distribution stats for analyst dashboard
   */
  async getDashboardStats() {
    const response = await api.get('/analyst/dashboard');
    return response.data;
  },

  /**
   * Get all fraud alerts
   */
  async getAlerts() {
    const response = await api.get('/analyst/alerts');
    return response.data;
  },

  /**
   * Mark a fraud alert as REVIEWED
   * @param {number|string} id
   */
  async reviewAlert(id) {
    const response = await api.patch(`/analyst/alerts/${id}/review`);
    return response.data;
  },
};
