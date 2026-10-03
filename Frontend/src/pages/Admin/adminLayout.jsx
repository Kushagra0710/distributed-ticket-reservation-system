import {Link, Outlet, useNavigate} from 'react-router-dom';
import {useAuth} from '../../context/AuthContext';
import styles from './adminLayout.module.css';

export default function AdminLayout(){
    const {user, logout}=useAuth();
    const navigate = useNavigate();

    const handleLogout = ()=> {
        logout();
        navigate('/login');
    }

    return (
        <div className={styles.layoutContainer}>
            {/* Sidebar */}
            <aside className={styles.sidebar}>
                <div className={styles.brand}>TicketAdmin</div>
                <nav className={styles.nav}>
                    <Link to="/admin" className={styles.navLink}>Dashboard</Link>
                    <Link to="/admin/movies" className={styles.navLink}>Movies</Link>
                    <Link to="/admin/events" className={styles.navLink}>Events</Link>
                    <Link to="/admin/venues" className={styles.navLink}>Venues</Link>
                    <Link to="/admin/users" className={styles.navLink}>Manage Admins</Link>
                </nav>
            </aside>

            {/* Main Content Area */}
            <div className={styles.mainWrapper}>
                <header className={styles.topbar}>
                    <div className={styles.userInfo}>
                        <span>Welcome, {user?.userName}</span>
                        <button onClick={handleLogout} className={styles.logoutBtn}>
                            Logout
                        </button>
                    </div>
                </header>

                <main className={styles.content}>
                    {/* Nested routes will render inside this Outlet */}
                    <Outlet /> 
                </main>
            </div>
        </div>
    );
}