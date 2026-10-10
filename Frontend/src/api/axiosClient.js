import axios from 'axios';

const api=axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
    headers:{
        'Content-Type':'application/json',
    },
});

// Automatically attach the JWT token to every request
api.interceptors.request.use((config)=>{
    const token = localStorage.getItem('token');
    if(token){
        config.headers.Authorization=`Bearer ${token}`;
    }
    return config;
}, (error)=>{
    return Promise.reject(error);
});

// Automatically handle 401 Unauthorised responses
api.interceptors.response.use((response)=>{
    return response;
}, (error)=>{
    if(error.response && error.response.status===401){
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        if (!error.config?.url?.startsWith('/auth/')) {
            window.location.href='/login';
        }
    }
    return Promise.reject(error);
});

export default api;
