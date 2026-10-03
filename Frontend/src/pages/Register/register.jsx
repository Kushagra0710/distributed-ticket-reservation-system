import { useState } from "react";
import {useAuth} from '../../context/AuthContext'
import styles from './register.module.css'
import {useNavigate,Link} from 'react-router-dom'

const validateFullName = (value) => {
    if (!value.trim()) return 'Name is required.';
    if (value.length > 50) return 'Name must be 50 characters or fewer.';
    if (!/^[\p{L} ]+$/u.test(value.trim())) return 'Use letters and spaces only.';
    return '';
};

const validateEmail = (value) => {
    const trimmedEmail = value.trim();
    if (!trimmedEmail) return 'Email is required.';
    if (value.length > 50) return 'Email must be 50 characters or fewer.';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmedEmail)) return 'Enter a valid email address.';
    return '';
};

const validatePassword = (value) => {
    if (!value) return 'Password is required.';
    if (value.length > 50) return 'Password must be 50 characters or fewer.';
    if (value.length < 8 || !/[A-Z]/.test(value) || !/[a-z]/.test(value) || !/[0-9]/.test(value) || !/[^A-Za-z0-9\s]/.test(value)) {
        return 'Use 8-50 characters with uppercase, lowercase, a number, and a special character.';
    }
    return '';
};

export default function Register(){

    const[fullName, setFullName]=useState('');
    const[email, setEmail]=useState('');
    const[password,setPassword]=useState('');
    const[error, setError]=useState('');
    const[errors, setErrors]=useState({fullName: '', email: '', password: ''});

    const navigate=useNavigate();
    const {register}=useAuth();

    const handleSubmit = async(e)=> {
        e.preventDefault();
        setError('');

        const fieldErrors = {
            fullName: validateFullName(fullName),
            email: validateEmail(email),
            password: validatePassword(password)
        };
        setErrors(fieldErrors);
        if (Object.values(fieldErrors).some(Boolean)) return;

        try{
            const response = await register(fullName.trim(), email.trim(), password);
            navigate('/login', {
                replace: true,
                state: {successMessage: response.data.message}
            });
        } catch(err){
            const responseErrors = err.response?.data?.errors;
            if (responseErrors) {
                setErrors((currentErrors) => ({...currentErrors, ...responseErrors}));
            }
            setError(err.response?.data?.message || 'Registration failed. Please try again.')
        }
    };

    const handleFieldChange = (field, value, setValue, validate) => {
        setValue(value);
        setErrors((currentErrors) => currentErrors[field]
            ? {...currentErrors, [field]: validate(value)}
            : currentErrors);
    };

    return (
        <div className={styles.pageWrapper}>
            <div className={styles.formContainer}>
                <h2 className={styles.title}>Create your account</h2>
                
                {error && <div className={styles.errorMessage}>{error}</div>}

                <form onSubmit={handleSubmit} className={styles.form} noValidate>
                    <div className={styles.inputGroup}>
                        <label htmlFor="fullName">Full Name</label>
                        <input
                            id="fullName"
                            type="text"
                            required
                            maxLength={50}
                            value={fullName}
                            aria-invalid={Boolean(errors.fullName)}
                            aria-describedby={errors.fullName ? 'fullName-error' : undefined}
                            onChange={(event) => handleFieldChange('fullName', event.target.value, setFullName, validateFullName)}
                        />
                        {errors.fullName && <span id="fullName-error" className={styles.fieldError}>{errors.fullName}</span>}
                    </div>

                    <div className={styles.inputGroup}>
                        <label htmlFor="email">Email address</label>
                        <input
                            id="email"
                            type="email"
                            required
                            maxLength={50}
                            value={email}
                            aria-invalid={Boolean(errors.email)}
                            aria-describedby={errors.email ? 'email-error' : undefined}
                            onChange={(event) => handleFieldChange('email', event.target.value, setEmail, validateEmail)}
                        />
                        {errors.email && <span id="email-error" className={styles.fieldError}>{errors.email}</span>}
                    </div>

                    <div className={styles.inputGroup}>
                        <label htmlFor="password">Password</label>
                        <input
                            id="password"
                            type="password"
                            required
                            maxLength={50}
                            value={password}
                            aria-invalid={Boolean(errors.password)}
                            aria-describedby={errors.password ? 'password-error' : undefined}
                            onChange={(event) => handleFieldChange('password', event.target.value, setPassword, validatePassword)}
                        />
                        {errors.password && <span id="password-error" className={styles.fieldError}>{errors.password}</span>}
                    </div>

                    <button type="submit" className={styles.submitButton}>
                        Sign up
                    </button>
                </form>

                <p className={styles.footerText}>
                    Already have an account? <Link to="/login" className={styles.link}>Sign in here</Link>
                </p>
            </div>
        </div>
    );

}