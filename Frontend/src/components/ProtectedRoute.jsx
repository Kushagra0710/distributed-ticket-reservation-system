import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function ProtectedRoute({children, requireAdmin}){
    const {user,loading} = useAuth();

    if(loading) return <div>Loading...</div>; //prevent redirecting while checking
    if(!user) return <Navigate to ="/login" replace />;

    if(requireAdmin && user.role !== 'ADMIN'){
        return <Navigate to ="/" replace />; //redirect standard user to home page
    }

    return children;
}