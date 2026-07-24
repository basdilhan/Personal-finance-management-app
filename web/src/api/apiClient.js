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

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response && error.response.status === 401) {
      console.warn("Unauthorized access detected. Signing out...");
      try {
        await auth.signOut();
        // The AuthContext will notice currentUser is gone,
        // and PrivateRoute will redirect to /login automatically.
      } catch (err) {
        console.error("Error signing out after 401:", err);
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;
