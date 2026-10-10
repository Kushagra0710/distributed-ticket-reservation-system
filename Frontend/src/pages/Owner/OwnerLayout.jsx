import { Link, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

export default function OwnerLayout() {
    const {user, logout} = useAuth();
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        navigate("/login", {replace: true});
    };

    return (
        <div className="min-h-screen bg-gray-50">
            <header className="flex flex-wrap items-center justify-between gap-4 border-b border-gray-200 bg-white px-6 py-4">
                <div className="flex flex-wrap items-center gap-6">
                    <span className="text-lg font-bold text-gray-900">Venue Owner</span>
                    <nav className="flex flex-wrap gap-4 text-sm font-medium text-gray-700">
                        <Link to="/owner">My venues</Link>
                        <Link to="/owner/showings">Showtimes</Link>
                    </nav>
                </div>
                <div className="flex items-center gap-4">
                    <span className="text-sm text-gray-600">Welcome, {user?.userName}</span>
                    <button onClick={handleLogout} className="rounded bg-gray-900 px-3 py-2 text-sm text-white">Logout</button>
                </div>
            </header>
            <main className="mx-auto max-w-7xl space-y-6 p-4 sm:p-6 lg:p-8">
                <Outlet />
            </main>
        </div>
    );
}
