import api from './api';

export const transactionService = {
  /**
   * Evaluate fraud risk for a transfer before executing
   * @param {{ receiverUsernameOrEmail: string, amount: number|string, description?: string, deviceId?: string, ipAddress?: string, location?: string }} payload
   */
  async evaluate(payload) {
    const response = await api.post('/transactions/evaluate', payload);
    return response.data;
  },

  /**
   * Transfer funds from customer's wallet
   * @param {{ receiverUsernameOrEmail: string, amount: number|string, description?: string, deviceId?: string, ipAddress?: string, location?: string }} payload
   */
  async transfer(payload) {
    const response = await api.post('/transactions/transfer', payload);
    return response.data;
  },

  /**
   * Get transaction history for current customer
   */
  async getHistory() {
    const response = await api.get('/transactions/history');
    return response.data;
  },

  /**
   * Get all transactions in the system (Analyst/Admin)
   */
  async getAllTransactions() {
    const response = await api.get('/analyst/transactions');
    return response.data;
  },

  /**
   * Approve a pending or flagged transaction (Analyst/Admin)
   * @param {number|string} id
   */
  async approveTransaction(id) {
    const response = await api.patch(`/analyst/transactions/${id}/approve`);
    return response.data;
  },

  /**
   * Block a high-risk transaction (Analyst/Admin)
   * @param {number|string} id
   */
  async blockTransaction(id) {
    const response = await api.patch(`/analyst/transactions/${id}/block`);
    return response.data;
  },
};
