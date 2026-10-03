import { createContext, useState, useEffect, useContext, Children } from "react";
import api from '../api/axiosClient';

const AuthContext = createContext();

export const AuthProvider = ({children})=>{
    const[user, setUser] = useState(null);
    const[loading, setLoading] =useState(true);

    useEffect(()=>{
        const storedToken = localStorage.getItem('token');
        const storedUser = localStorage.getItem('user');

        if(storedToken && storedUser){
            setUser(JSON.parse(storedUser));
        }
        setLoading(false);
    },[]);

    const handleAuthResponse = (response)=>{
        const{ token, email, role, userName }=response.data;
        const userData = {email, role, userName};

        localStorage.setItem('token', token);
        localStorage.setItem('user', JSON.stringify(userData));
        setUser(userData);
    };

    const login = async (email, password)=>{
        const response = await api.post('/auth/login', {email, password});
        handleAuthResponse(response);
    };

    const register = async(fullName, email, password) => {
        return api.post('/auth/register', {fullName, email, password});
    };

    const googleLogin = async (idToken)=>{
        const response = await api.post('/auth/google', {idToken});
        handleAuthResponse(response);
    };

    const logout = () =>{
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        setUser(null);
    };

    return (
        <AuthContext.Provider value = {{user,login,register,googleLogin,logout,loading}}>
            {!loading && children}
        </AuthContext.Provider>
    );
};

export const useAuth = ()=> useContext(AuthContext);