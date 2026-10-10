import { useEffect, useState } from "react";
import api from "../../api/axiosClient";
import { getApiErrorMessage, showToast } from "../../context/toastService";


export default function ManageAdmins(){

    //DATA STATE
    const[admins,setAdmins]=useState([]);
    const[isLoading, setIsLoading]=useState(true);
    // UI STATE for search and pagination
    const[searchTerm, setSearchTerm]=useState('');
    const[currentPage, setCurrentPage]=useState(1);
    const itemsPerPage=10;

    // UI state for modals and dropdown
    const[activeDropDown, setActiveDropDown]=useState(null);
    const[modalState, setModalState]=useState({isOpen: false, mode:'add'}); // add or edit

    // form state
    const[formData,setFormData]=useState({id:'', fullName:'', email:'', phoneNumber:'', role:'ADMIN'})
    const[formStatus, setFormStatus]=useState({type:'', message:''});
    const [isSubmitting, setIsSubmitting] = useState(false);

    const fetchAdmins = async ()=> {
        setIsLoading(true);
        try{
            const response = await api.get('/admin/users/except-current');
            setAdmins(response.data);
        }catch(error){
            showToast(getApiErrorMessage(error, "Failed to fetch admins."));
        } finally{
            setIsLoading(false);
        }
    };

    useEffect(()=>{
        let isActive = true;
        api.get('/admin/users/except-current')
            .then(({data}) => {
                if (isActive) setAdmins(data);
            })
            .catch(error => {
                if (isActive) showToast(getApiErrorMessage(error, "Failed to fetch admins."));
            })
            .finally(() => {
                if (isActive) setIsLoading(false);
            });
        return () => { isActive = false; };
    },[]);

    const filteredAdmins = admins.filter(admin =>
        (admin.fullName || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
        (admin.email || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
        (admin.phoneNumber || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
        (admin.role || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
        (admin.isActive ? 'active' : 'disabled').includes(searchTerm.toLowerCase())
    );

    const totalPages = Math.ceil(filteredAdmins.length / itemsPerPage);
    const paginatedAdmins = filteredAdmins.slice(
        (currentPage - 1) * itemsPerPage,
        currentPage * itemsPerPage
    );

    // 3. form handling(open, close,submit)
    const openModal = (mode, admin=null)=>{
        setFormStatus({type:'',message:''});
        setModalState({isOpen:true, mode});
        setActiveDropDown(null);
        
        if(mode === 'edit' && admin){
            setFormData({id:admin.id, fullName:admin.fullName, phoneNumber:admin.phoneNumber || '', email:admin.email, role:admin.role || 'ADMIN'});
        }else{
            setFormData({id:'', fullName:'', email:'', phoneNumber:'', role:'ADMIN'});
        }
    };

    const closeModal = ()=> setModalState({isOpen: false, mode:'add'});  

    const handleSubmit = async(e)=>{
        e.preventDefault();
        setIsSubmitting(true);
        setFormStatus({type:'',message:''});

        try{
            if (modalState.mode==='add'){
                const createRequest = {...formData};
                await api.post('/admin/users', createRequest);
                const accountType = formData.role === 'OWNER' ? 'Owner' : 'Admin';
                setFormStatus({type:'success', message:`${accountType} account created successfully.`})
                showToast(`${accountType} account created successfully.`, 'success');
            }else{
                await api.put(`/admin/users/${formData.id}`,{fullName:formData.fullName, phoneNumber:formData.phoneNumber});
                setFormStatus({type:'success',message:'Account updated successfully.'})
                showToast('Account updated successfully.', 'success');
            }
            fetchAdmins();//refresh table
            setTimeout(closeModal,1000);
        }catch(error){
            setFormStatus({
                type: 'error',
                message:getApiErrorMessage(error, 'Failed to create or update account.')
            });
            showToast(getApiErrorMessage(error, 'Failed to create or update account.'));
        }finally{
            setIsSubmitting(false);
        }
    };

    const handleToggleDisable=async(id, currentStatus)=>{
        setActiveDropDown(null);
        if(window.confirm(`Are you sure you want to ${currentStatus ? 'disable':'enable'} this admin?`)){
            try{
                await api.patch(`/admin/users/${id}/status`,{active: !currentStatus});
                await fetchAdmins();
                showToast('Admin status updated.', 'success');
            }catch(error){
                showToast(getApiErrorMessage(error, "Failed to update admin status."));
            }
        }
    };

    return (
        <div className="w-full bg-white rounded-xl shadow-sm border border-gray-100 p-6 relative">
            {/* Header & Toolbar */}
            <div className="flex flex-col md:flex-row justify-between items-center mb-6 space-y-4 md:space-y-0">
                <h2 className="text-2xl font-bold text-gray-900">Admin &amp; Owner Accounts</h2>
            </div>
            
            <div className="flex justify-between items-center mb-6">
                <input
                    type="text"
                    placeholder="Search by name or email..."
                    className="w-full md:w-1/3 rounded-md border border-gray-300 px-4 py-2 focus:border-gray-900 focus:outline-none focus:ring-1 focus:ring-gray-900"
                    value={searchTerm}
                    onChange={(e) => {
                        setSearchTerm(e.target.value);
                        setCurrentPage(1);
                    }}
                />
                <button
                    onClick={() => openModal('add')}
                    className="bg-gray-900 text-white px-4 py-2 rounded-md hover:bg-gray-800 transition-colors font-medium text-sm"
                >
                    + Add Account
                </button>
            </div>

            {/* Data Table */}
            <div className="overflow-visible w-full">
                <table className="w-full text-left border-collapse">
                    <thead>
                        <tr className="bg-gray-50 border-b border-gray-200">
                            <th className="px-6 py-3 text-sm font-semibold text-gray-600">Name</th>
                            <th className="px-6 py-3 text-sm font-semibold text-gray-600">Email</th>
                            <th className="px-6 py-3 text-sm font-semibold text-gray-600">Phone</th>
                            <th className="px-6 py-3 text-sm font-semibold text-gray-600">Role</th>
                            <th className="px-6 py-3 text-sm font-semibold text-gray-600">Status</th>
                            <th className="px-6 py-3 text-sm font-semibold text-gray-600 text-center">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {isLoading ? (
                            <tr><td colSpan="6" className="text-center py-8 text-gray-500">Loading accounts...</td></tr>
                        ) : paginatedAdmins.length === 0 ? (
                            <tr><td colSpan="6" className="text-center py-8 text-gray-500">No accounts found.</td></tr>
                        ) : (
                            paginatedAdmins.map((admin) => (
                                <tr key={admin.id} className="border-b border-gray-100 hover:bg-gray-50">
                                    <td className="px-6 py-4 text-sm text-gray-900 font-medium">{admin.fullName}</td>
                                    <td className="px-6 py-4 text-sm text-gray-600">{admin.email}</td>
                                    <td className="px-6 py-4 text-sm text-gray-600">{admin.phoneNumber || 'N/A'}</td>
                                    <td className="px-6 py-4 text-sm text-gray-600">{admin.role}</td>
                                    <td className="px-6 py-4 text-sm">
                                        <span className={`px-2 py-1 rounded-full text-xs font-semibold ${admin.isActive ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                                            {admin.isActive ? 'Active' : 'Disabled'}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 text-sm text-center relative">
                                        {/* Ellipsis Button */}
                                        <button 
                                            onClick={() => setActiveDropDown(activeDropDown === admin.id ? null : admin.id)}
                                            className="text-gray-500 hover:text-gray-900 p-2 font-bold text-lg"
                                        >
                                            &#8942;
                                        </button>
                                        
                                        {/* Action Dropdown */}
                                        {activeDropDown === admin.id && (
                                            <div className="absolute right-8 top-10 w-32 bg-white border border-gray-200 rounded-md shadow-lg z-50 py-1">
                                                <button 
                                                    onClick={() => openModal('edit', admin)}
                                                    className="w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                                                >
                                                    Edit
                                                </button>
                                                <button 
                                                    onClick={() => handleToggleDisable(admin.id, admin.isActive)}
                                                    className="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50"
                                                >
                                                    {admin.isActive ? 'Disable' : 'Enable'}
                                                </button>
                                            </div>
                                        )}
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>

            {/* Pagination Controls */}
            {totalPages > 1 && (
                <div className="flex justify-between items-center mt-6">
                    <span className="text-sm text-gray-600">
                        Showing {(currentPage - 1) * itemsPerPage + 1} to {Math.min(currentPage * itemsPerPage, filteredAdmins.length)} of {filteredAdmins.length}
                    </span>
                    <div className="flex space-x-2">
                        <button 
                            disabled={currentPage === 1}
                            onClick={() => setCurrentPage(prev => prev - 1)}
                            className="px-3 py-1 border border-gray-300 rounded-md text-sm disabled:opacity-50"
                        >
                            Previous
                        </button>
                        <button 
                            disabled={currentPage === totalPages}
                            onClick={() => setCurrentPage(prev => prev + 1)}
                            className="px-3 py-1 border border-gray-300 rounded-md text-sm disabled:opacity-50"
                        >
                            Next
                        </button>
                    </div>
                </div>
            )}

            {/* Add/Edit Modal Wrapper */}
            {modalState.isOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50 px-4">
                    <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6 relative">
                        <button onClick={closeModal} className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 text-xl font-bold">&times;</button>
                        <h3 className="text-xl font-bold mb-4">{modalState.mode === 'add' ? 'Add Account' : 'Edit Account'}</h3>
                        
                        {formStatus.message && (
                            <div className={`p-3 mb-4 rounded-md text-sm ${formStatus.type === 'success' ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'}`}>
                                {formStatus.message}
                            </div>
                        )}

                        <form onSubmit={handleSubmit} className="space-y-4">
                            {modalState.mode === 'add' && (
                                <>
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1">Account role</label>
                                        <select
                                            value={formData.role}
                                            onChange={(e) => setFormData({...formData, role:e.target.value})}
                                            className="w-full rounded-md border border-gray-300 px-3 py-2"
                                        >
                                            <option value="ADMIN">Admin</option>
                                            <option value="OWNER">Venue owner</option>
                                        </select>
                                    </div>
                                    {formData.role === 'OWNER' && (
                                        <p className="text-sm text-gray-500">
                                            A secure initial password will be generated automatically for this owner.
                                        </p>
                                    )}
                                </>
                            )}
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Full Name</label>
                                <input
                                    type="text" required
                                    value={formData.fullName}
                                    onChange={(e) => setFormData({...formData, fullName: e.target.value})}
                                    className="w-full rounded-md border border-gray-300 px-3 py-2 focus:ring-1 focus:ring-gray-900"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Email Address</label>
                                <input
                                    type="email" required
                                    value={formData.email}
                                    onChange={(e) => setFormData({...formData, email: e.target.value})}
                                    disabled={modalState.mode === 'edit'} // Disabled on edit
                                    className={`w-full rounded-md border border-gray-300 px-3 py-2 ${modalState.mode === 'edit' ? 'bg-gray-100 text-gray-500 cursor-not-allowed' : 'focus:ring-1 focus:ring-gray-900'}`}
                                />
                                {modalState.mode === 'edit' && <span className="text-xs text-gray-500 mt-1">Email cannot be changed.</span>}
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Phone Number</label>
                                <input
                                    type="tel" required
                                    value={formData.phoneNumber}
                                    onChange={(e) => setFormData({...formData, phoneNumber: e.target.value})}
                                    className="w-full rounded-md border border-gray-300 px-3 py-2 focus:ring-1 focus:ring-gray-900"
                                />
                            </div>
                            <div className="flex justify-end space-x-3 mt-6">
                                <button type="button" onClick={closeModal} className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50">Cancel</button>
                                <button type="submit" disabled={isSubmitting} className="px-4 py-2 bg-gray-900 text-white rounded-md text-sm font-medium hover:bg-gray-800 disabled:opacity-70">
                                    {isSubmitting ? 'Saving...' : 'Save Account'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}