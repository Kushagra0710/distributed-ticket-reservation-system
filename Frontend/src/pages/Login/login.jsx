import { useState } from "react";
import {useNavigate,Link,useLocation} from 'react-router-dom';
import  {useAuth}  from '../../context/AuthContext';
import { showToast } from '../../context/toastService';
import styles from './Login.module.css';
import { GoogleLogin } from "@react-oauth/google";

const getErrorMessage = (error, fallback) => {
    if (!error.response) {
        return 'Unable to connect to the server. Check your connection and try again.';
    }

    const data = error.response.data;
    if (typeof data === 'string') return data;
    if (typeof data?.message === 'string') {
        const details = Object.values(data.errors || {}).filter((value) => typeof value === 'string');
        return details.length ? `${data.message}: ${details.join(', ')}` : data.message;
    }

    return fallback;
};

export default function Login(){
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const {login, googleLogin} = useAuth('');
    const navigate = useNavigate();
    const location = useLocation();
    const [successMessage] = useState(location.state?.successMessage || '');

    const handleSubmit = async (e)=>{
        e.preventDefault();
        setError('');
        setLoading(true);
        try{
            await login(email, password);
            const loggedUser = JSON.parse(localStorage.getItem('user') || '{}');
            navigate(loggedUser.role === 'ADMIN' ? '/admin' : loggedUser.role === 'OWNER' ? '/owner' : '/events');
        }catch(err) {
            const message = getErrorMessage(err, 'Sign-in failed. Please check your credentials.');
            setError(message);
            showToast(message);
        } finally {
            setLoading(false);
        }
    };

    const handleGoogleSucess = async (credentialResponse) => {
        setError('');
        try{
            await googleLogin(credentialResponse.credential);
            const loggedUser = JSON.parse(localStorage.getItem('user') || '{}');
            navigate(loggedUser.role === 'ADMIN' ? '/admin' : loggedUser.role === 'OWNER' ? '/owner' : '/events');
        }catch(err){
            const message = getErrorMessage(err, 'Google authentication failed.');
            setError(message);
            showToast(message);
        }
    };

    return (
        <div className={styles.pageWrapper}>
            <div className={styles.formCard}>
                <div>
                    <h2 className={styles.headerTitle}>
                        Sign in to your account
                    </h2>
                </div>
                
                {error && (
                    <div className={styles.errorBox}>
                        <p className={styles.errorText}>{error}</p>
                    </div>
                )}
                {successMessage && (
                    <div className={styles.successBox}>
                        <p className={styles.successText}>{successMessage}</p>
                    </div>
                )}

                <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
                    <div className={styles.inputGroup}>
                        <div>
                            <label htmlFor="email-address" className="sr-only">Email address</label>
                            <input
                                id="email-address"
                                name="email"
                                type="email"
                                required
                                className={styles.inputField}
                                placeholder="Email address"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                            />
                        </div>
                        <div>
                            <label htmlFor="password" className="sr-only">Password</label>
                            <input
                                id="password"
                                name="password"
                                type="password"
                                required
                                className={styles.inputField}
                                placeholder="Password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                            />
                        </div>
                    </div>

                    <div>
                        <button
                            type="submit"
                            disabled={loading}
                            className={styles.submitButton}
                        >
                            {loading ? 'Signing in...' : 'Sign in'}
                        </button>
                    </div>
                </form>

                <div className={styles.dividerContainer}>
                    <div className="relative">
                        <div className={styles.dividerLine}>
                            <div className="w-full border-t border-gray-300" />
                        </div>
                        <div className={styles.dividerText}>
                            <span className="bg-white px-2 text-gray-500">Or continue with</span>
                        </div>
                    </div>

                    <div className={styles.googleBtnContainer}>
                        <GoogleLogin
                            onSuccess={handleGoogleSucess}
                            onError={() => setError('Google login widget failed to load.')}
                            theme="outline"
                            size="large"
                            width="100%"
                        />
                    </div>
                </div>

                <p className="mt-4 text-center text-sm text-gray-600">
                    Don't have an account?{' '}
                    <Link to="/register" className="font-medium text-blue-600 hover:text-blue-500">
                        Sign up here
                    </Link>
                </p>
            </div>
        </div>
    );
}