import axios from 'axios';
import { auth } from '../config/firebase';

const BASE_URL = 'https://personal-finance-management-app-backend.onrender.com/api';

const apiClient = axios.create({
  baseURL: BASE_URL,
});

apiClient.interceptors.request.use(
  async (config) => {
    const user = auth.currentUser;
    if (user) {
      const token = await user.getIdToken();
      config.headers['Authorization'] = `Bearer ${token}`;
      config.headers['X-User-Id'] = user.uid;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

export default apiClient;
