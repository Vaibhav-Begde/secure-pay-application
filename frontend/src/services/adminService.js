import api from './api';

// Fallback initial mock users while backend does not expose /api/admin/users
let mockUsers = [
  {
    id: 1,
    username: 'admin_security',
    email: 'admin@securepay.internal',
    role: 'ADMIN',
    status: 'ACTIVE',
    createdAt: '2026-01-10T09:00:00Z',
    twoFactorEnabled: true,
  },
  {
    id: 2,
    username: 'analyst_sarah',
    email: 'sarah.analyst@securepay.internal',
    role: 'FRAUD_ANALYST',
    status: 'ACTIVE',
    createdAt: '2026-01-15T10:30:00Z',
    twoFactorEnabled: true,
  },
  {
    id: 3,
    username: 'customer_john',
    email: 'john.doe@example.com',
    role: 'CUSTOMER',
    status: 'ACTIVE',
    createdAt: '2026-02-01T14:15:00Z',
    twoFactorEnabled: true,
  },
  {
    id: 4,
    username: 'customer_alice',
    email: 'alice.crypto@example.com',
    role: 'CUSTOMER',
    status: 'ACTIVE',
    createdAt: '2026-02-12T16:45:00Z',
    twoFactorEnabled: false,
  },
  {
    id: 5,
    username: 'suspect_mark',
    email: 'mark99@disposable-mail.net',
    role: 'CUSTOMER',
    status: 'SUSPENDED',
    createdAt: '2026-02-20T11:20:00Z',
    twoFactorEnabled: false,
  },
];

export const adminService = {
  /**
   * Get all fraud rules
   */
  async getFraudRules() {
    const response = await api.get('/admin/fraud-rules');
    return response.data;
  },

  /**
   * Create a new fraud rule
   * @param {{ ruleCode: string, ruleName: string, description: string, riskPoints: number, enabled: boolean }} rule
   */
  async createFraudRule(rule) {
    const response = await api.post('/admin/fraud-rules', rule);
    return response.data;
  },

  /**
   * Update an existing fraud rule
   * @param {number|string} id
   * @param {{ ruleCode: string, ruleName: string, description: string, riskPoints: number, enabled: boolean }} rule
   */
  async updateFraudRule(id, rule) {
    const response = await api.put(`/admin/fraud-rules/${id}`, rule);
    return response.data;
  },

  /**
   * Delete a fraud rule
   * @param {number|string} id
   */
  async deleteFraudRule(id) {
    const response = await api.delete(`/admin/fraud-rules/${id}`);
    return response.data;
  },

  /**
   * Fetch users list
   * Note: Mocked until backend exposes /api/admin/users
   */
  async getUsers() {
    // TODO: Replace with real API call: const response = await api.get('/admin/users'); return response.data;
    await new Promise((resolve) => setTimeout(resolve, 300));
    return { success: true, data: [...mockUsers] };
  },

  /**
   * Update user status or role
   * @param {number|string} id
   * @param {Partial<typeof mockUsers[0]>} updates
   */
  async updateUser(id, updates) {
    // TODO: Replace with real API call: const response = await api.patch(`/admin/users/${id}`, updates); return response.data;
    await new Promise((resolve) => setTimeout(resolve, 250));
    mockUsers = mockUsers.map((u) => (u.id === Number(id) ? { ...u, ...updates } : u));
    return { success: true, data: mockUsers.find((u) => u.id === Number(id)) };
  },
};
