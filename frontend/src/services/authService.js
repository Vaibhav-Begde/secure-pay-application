import api from './api';

export const authService = {
  /**
   * Register a new user
   * @param {{ username: string, email: string, password: string, role?: string }} data
   */
  async register(data) {
    const response = await api.post('/auth/register', data);
    return response.data;
  },

  /**
   * Login user
   * @param {{ usernameOrEmail: string, password: string }} credentials
   */
  async login(credentials) {
    const response = await api.post('/auth/login', credentials);
    return response.data;
  },

  /**
   * Get current authenticated user details
   */
  async getCurrentUser() {
    const response = await api.get('/auth/me');
    return response.data;
  },

  /**
   * Set or update the customer's transaction PIN after confirming account password
   * @param {{ currentPassword: string, transactionPin: string }} data
   */
  async setTransactionPin(data) {
    const response = await api.post('/auth/transaction-pin', data);
    return response.data;
  },

  /**
   * Send step-up OTP challenge
   * @param {{ referenceCode: string }} data
   */
  async sendOtp(data) {
    const response = await api.post('/otp/send', data);
    return response.data;
  },

  /**
   * Verify step-up OTP code
   * @param {{ referenceCode: string, otpCode: string }} data
   */
  async verifyOtp(data) {
    const response = await api.post('/otp/verify', data);
    return response.data;
  },
};
