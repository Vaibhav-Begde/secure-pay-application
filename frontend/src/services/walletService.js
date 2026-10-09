import api from './api';

export const walletService = {
  /**
   * Get virtual wallet for the logged-in customer
   */
  async getWallet() {
    const response = await api.get('/wallet');
    return response.data;
  },
};
