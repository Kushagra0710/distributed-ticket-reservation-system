import './App.css'
import { useAuth } from './context/AuthContext'
import { BrowserRouter as Router, Routes, Route, Navigate, useNavigate, Link } from 'react-router-dom'
import Login from './pages/Login/login'
import Register from './pages/Register/register'
import AdminLayout from './pages/Admin/adminLayout'
import ManageAdmins from './pages/Admin/ManageAdmins'
import ManageVenues from './pages/Admin/ManageVenues'
import ManageVenueSpaces from './pages/Admin/ManageVenueSpaces'
import ManageEvents from './pages/Admin/ManageEvents'
import ManageEventCatalog from './pages/Admin/ManageEventCatalog'
import OwnerLayout from './pages/Owner/OwnerLayout'
import EventDiscovery from './pages/Customer/EventDiscovery'

const AdminDashboard = () => (
  <div className="p-8">
    <h1 className="text-2xl font-bold">Admin Dashboard</h1>
    <p className="mt-2">Welcome to the admin panel.</p>
  </div>
);

const ProtectedRoute = ({children, requireAdmin = false, requireOwner = false})=> {
  const { user } = useAuth();
  if(!user) return <Navigate to="/login" replace/>
  if(requireAdmin && user.role !== 'ADMIN') return <Navigate to="/dashboard" replace/>
  if(requireOwner && user.role !== 'OWNER') return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/dashboard'} replace/>
  return children ;
}

const GuestRoute = ({children}) => {
  const { user } = useAuth();
  if(user) return <Navigate to={user.role === 'ADMIN' ? '/admin' : user.role === 'OWNER' ? '/owner' : '/dashboard'} replace/>
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
    <Link to="/events" className="mt-4 inline-block rounded bg-gray-900 px-4 py-2 text-white hover:bg-gray-800">
      Browse events
    </Link>
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
        <Route path="/" element={<EventDiscovery/>}/>
        <Route path="/events" element={<EventDiscovery/>}/>
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
          <Route path="movies" element={<ManageEventCatalog/>} />
          <Route path="events" element={<ManageEventCatalog/>} />
          <Route path="showings" element={<ManageEvents/>} />
          <Route path="venues" element={<ManageVenues/>} />
          <Route path="venues/:venueId/spaces" element={<ManageVenueSpaces/>} />
        </Route>

        <Route
          path="/owner"
          element={
            <ProtectedRoute requireOwner={true}>
              <OwnerLayout/>
            </ProtectedRoute>
          }
        >
          <Route index element={<ManageVenues/>} />
          <Route path="showings" element={<ManageEvents/>} />
          <Route path="venues/:venueId/spaces" element={<ManageVenueSpaces/>} />
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