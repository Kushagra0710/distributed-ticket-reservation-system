import './App.css'
import { useAuth } from './context/AuthContext'
import { BrowserRouter as Router, Routes, Route, Navigate, useNavigate } from 'react-router-dom'
import Login from './pages/Login/login'
import Register from './pages/Register/register'
import AdminLayout from './pages/Admin/adminLayout'
import ManageAdmins from './pages/Admin/ManageAdmins'

const AdminDashboard = () => (
  <div className="p-8">
    <h1 className="text-2xl font-bold">Admin Dashboard</h1>
    <p className="mt-2">Welcome to the admin panel.</p>
  </div>
);

const ProtectedRoute = ({children, requireAdmin = false})=> {
  const { user } = useAuth();
  if(!user) return <Navigate to="/login" replace/>
  if(requireAdmin && user.role !== 'ADMIN') return <Navigate to="/dashboard" replace/>
  return children ;
}

const GuestRoute = ({children}) => {
  const { user } = useAuth();
  if(user) return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/dashboard'} replace/>
  return children;
}

const Dashboard = () => {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login', {replace: true});
  };

  return (
    <main className="p-8">
      <p>Welcome to the Dashboard!</p>
      <button
        type="button"
        className="mt-4 rounded bg-red-600 px-4 py-2 text-white hover:bg-red-700"
        onClick={handleLogout}
      >
        Log out
      </button>
    </main>
  );
};

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/login" element={<GuestRoute><Login/></GuestRoute>}/>
        <Route path="/register" element={<GuestRoute><Register/></GuestRoute>}/>

        <Route
          path="/admin"
          element={
            <ProtectedRoute requireAdmin={true}>
              <AdminLayout/>
            </ProtectedRoute>
          }
        >
          <Route index element={<AdminDashboard />} />
          <Route path="users" element={<ManageAdmins />} />
          <Route path="movies" element={<div>Movie Management</div>} />
          <Route path="venues" element={<div>Venue Management</div>} />
        </Route>

        <Route 
          path="/dashboard"
          element={
            <ProtectedRoute>
              <Dashboard/>
            </ProtectedRoute>
          }
        />
      
          <Route path="*" element={<Navigate to = "/login" replace />}/>
      </Routes>
    </Router>
  );
}

export default App;