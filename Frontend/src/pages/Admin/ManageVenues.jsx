import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../../api/axiosClient";
import { getApiErrorMessage, showToast } from "../../context/toastService";
import { useAuth } from "../../context/AuthContext";

export default function ManageVenues(){
    const navigate = useNavigate();
    const {user} = useAuth();
    const isAdmin = user?.role === 'ADMIN';
    const [venues,setVenues]=useState([]);
    const [owners,setOwners]=useState([]);
    const [venueTypes] = useState(['Theatre','Concert Ground','Auditorium']);
    const [isLoading, setIsLoading]=useState(true);

    const [searchTerm, setSearchTerm]=useState('');
    const [currentPage, setCurrentPage]=useState(1);
    const itemsPerPage=10;
    const [activeDropdown, setActiveDropdown]=useState(null);
    const [modalState, setModalState]=useState({isOpen: false, mode: 'add'});

    const [formData, setFormData] = useState({id:'', name:'', address:'', pincode:'', city:'', state:'', type:'', ownerId:''});
    const [isSubmitting, setIsSubmitting]= useState(false);
    const [pincodeLookup, setPincodeLookup] = useState({isLoading: false, error: ''});

    const fetchVenues = useCallback(async()=> {
        setIsLoading(true);
        try{
            const response = await api.get('/admin/venue');
            setVenues(response.data.map(venue => ({
                ...venue,
                type: venue.venueType ?? venue.type,
                isActive: venue.isActive ?? !venue.deleteFlag
            })));
        } catch(error){
            showToast(getApiErrorMessage(error, "Failed to fetch venues."));
        } finally{
            setIsLoading(false);
        }
    }, []);

    useEffect(()=>{
        let isActive = true;
        api.get('/admin/venue')
            .then(({data}) => {
                if (!isActive) return;
                setVenues(data.map(venue => ({
                    ...venue,
                    type: venue.venueType ?? venue.type,
                    isActive: venue.isActive ?? !venue.deleteFlag
                })));
            })
            .catch(error => {
                if (isActive) showToast(getApiErrorMessage(error, "Failed to fetch venues."));
            })
            .finally(() => {
                if (isActive) setIsLoading(false);
            });

        if (isAdmin) {
            api.get('/admin/users/owners')
                .then(({data}) => {
                    if (isActive) setOwners(data);
                })
                .catch(error => {
                    if (isActive) showToast(getApiErrorMessage(error, "Failed to load venue owners."));
                });
        }
        return () => { isActive = false; };
    }, [isAdmin]);

    // client aide search and pagination
    const filteredVenues = venues.filter(venue=>
        venue.name.toLowerCase().includes(searchTerm.toLowerCase())||
        venue.city.toLowerCase().includes(searchTerm.toLowerCase()) ||
        venue.state.toLowerCase().includes(searchTerm.toLowerCase()) ||
        venue.type.toLowerCase().includes(searchTerm.toLowerCase())
    );

    const totalPages = Math.ceil(filteredVenues.length/itemsPerPage);
    const paginatedVenues=filteredVenues.slice((currentPage-1)*itemsPerPage, currentPage*itemsPerPage);

    useEffect(() => {
        if (!modalState.isOpen || !/^\d{6}$/.test(formData.pincode)) return undefined;

        const controller = new AbortController();
        const pincode = formData.pincode;
        Promise.resolve()
            .then(() => {
                if (controller.signal.aborted) return null;
                setPincodeLookup({isLoading: true, error: ''});
                return api.get(`/admin/venue/pincode/${pincode}`, {signal: controller.signal});
            })
            .then(response => {
                if (!response) return;
                const {data} = response;
                setFormData(current => current.pincode === pincode
                    ? {...current, city: data.city, state: data.state}
                    : current);
                setPincodeLookup({isLoading: false, error: ''});
            })
            .catch(error => {
                if (controller.signal.aborted) return;
                const message = getApiErrorMessage(error, 'Could not find a location for this pincode.');
                setPincodeLookup({isLoading: false, error: message});
                showToast(message);
            });

        return () => controller.abort();
    }, [formData.pincode, modalState.isOpen]);

    const openModal = (mode, venue=null)=>{
        setPincodeLookup({isLoading: false, error: ''});
        setModalState({isOpen:true, mode});
        setActiveDropdown(null);
        if(mode==='edit' && venue){
            setFormData({...venue});
        }else{
            setFormData({id:'', name:'', address:'', pincode:'', city:'', state:'', type: venueTypes[0]||'', ownerId:''});
        }
    };

    const closeModal=()=> setModalState({isOpen:false, mode:'add'});
    const handleSubmit = async(e)=>{
        e.preventDefault();
        setIsSubmitting(true);

        try{
            const venueRequest = {
                name: formData.name,
                address: formData.address,
                pincode: Number(formData.pincode),
                venueType: formData.type,
                ownerId: formData.ownerId ? Number(formData.ownerId) : null
            };

            if(modalState.mode==='add'){
                await api.post('/admin/venue', venueRequest);
                showToast('Venue created successfully.', 'success');
            } else {
                if (!formData.id) {
                    throw new Error('Venue ID is missing; cannot update this venue.');
                }
                await api.put(`/admin/venue/${formData.id}`, venueRequest);
                showToast('Venue updated successfully.', 'success');
            }
            fetchVenues();
            setTimeout(closeModal,1000);
        }catch(error){
            showToast(getApiErrorMessage(error, 'Could not save the venue.'));
        }finally{
            setIsSubmitting(false);
        }
    };

    const handleToggleStatus=async(id, currentStatus)=>{
        setActiveDropdown(null);
        if(window.confirm(`Are you sure you want to ${currentStatus ? 'disable':'enable'} this venue?`)){
            try{
                await api.patch(`/admin/venues/${id}/status`,{isActive: !currentStatus});
                showToast('Venue status updated.', 'success');
                fetchVenues();
            }catch (error){
                showToast(getApiErrorMessage(error, "Failed to update venue status."));
            }
        }
    };

    const handleDelete = async(id)=>{
        setActiveDropdown(null);
        if(window.confirm(`Warning: Are you sure you want permanently delete this venue? This may affect linked events.`)){
            try{
                await api.delete(`/admin/venue/${id}/delete`);
                showToast('Venue deleted successfully.', 'success');
                fetchVenues();
            }catch(error){
                showToast(getApiErrorMessage(error, "Failed to delete venue."));
            }
        }
    };

    return (
        <div className="w-full bg-white rounded-xl shadow-sm border border-gray-100 p-6 relative">
            <div className="flex flex-col md:flex-row justify-between items-center mb-6 space-y-4 md:space-y-0">
                <h2 className="text-2xl font-bold text-gray-900">{isAdmin ? 'Venue Management' : 'My Venues'}</h2>
            </div>

            <div className="flex justify-between items-center mb-6">
                <input
                    type="text"
                    placeholder="Search by venue name or city..."
                    className="w-full md:w-1/3 rounded-md border border-gray-300 px-4 py-2 focus:border-gray-900 focus:outline-none focus:ring-1 focus:ring-gray-900"
                    value={searchTerm}
                    onChange={(e) => {
                        setSearchTerm(e.target.value);
                        setCurrentPage(1);
                    }}
                />
                {isAdmin && (
                    <button type="button" onClick={() => openModal('add')} className="bg-gray-900 text-white px-4 py-2 rounded-md hover:bg-gray-800 transition-colors font-medium text-sm">
                        + Add Venue
                    </button>
                )}
            </div>

            <div className="overflow-visible w-full">
                <table className="w-full text-left border-collapse">
                    <thead>
                        <tr className="bg-gray-50 border-b border-gray-200">
                            <th className="px-6 py-3 text-sm font-semibold text-gray-600">Venue Name</th>
                            <th className="px-6 py-3 text-sm font-semibold text-gray-600">Type</th>
                            <th className="px-6 py-3 text-sm font-semibold text-gray-600">Location</th>
                            {isAdmin && <th className="px-6 py-3 text-sm font-semibold text-gray-600">Owner</th>}
                            <th className="px-6 py-3 text-sm font-semibold text-gray-600">Status</th>
                            <th className="px-6 py-3 text-sm font-semibold text-gray-600 text-center">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {isLoading ? (
                            <tr><td colSpan={isAdmin ? 6 : 5} className="text-center py-8 text-gray-500">Loading venues...</td></tr>
                        ) : paginatedVenues.length === 0 ? (
                            <tr><td colSpan={isAdmin ? 6 : 5} className="text-center py-8 text-gray-500">No venues found.</td></tr>
                        ) : (
                            paginatedVenues.map((venue) => (
                                <tr key={venue.id} className="border-b border-gray-100 hover:bg-gray-50">
                                    <td className="px-6 py-4 text-sm text-gray-900 font-medium">{venue.name}</td>
                                    <td className="px-6 py-4 text-sm text-gray-600">{venue.type}</td>
                                    <td className="px-6 py-4 text-sm text-gray-600">{venue.city}, {venue.state}</td>
                                    {isAdmin && <td className="px-6 py-4 text-sm text-gray-600">{venue.ownerName || 'Unassigned'}</td>}
                                    <td className="px-6 py-4 text-sm">
                                        <span className={`px-2 py-1 rounded-full text-xs font-semibold ${venue.isActive ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                                            {venue.isActive ? 'Active' : 'Disabled'}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 text-sm text-center relative">
                                        <button onClick={() => setActiveDropdown(activeDropdown === venue.id ? null : venue.id)} className="text-gray-500 hover:text-gray-900 p-2 font-bold text-lg">&#8942;</button>
                                        {activeDropdown === venue.id && (
                                            <div className="absolute right-8 top-10 w-48 bg-white border border-gray-200 rounded-md shadow-lg z-50 py-1">
                                                <button onClick={() => navigate(`/${isAdmin ? 'admin' : 'owner'}/venues/${venue.id}/spaces`)} className="w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">Manage screens and seating</button>
                                                {isAdmin && <>
                                                    <button onClick={() => openModal('edit', venue)} className="w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">Edit Details</button>
                                                    <button onClick={() => handleToggleStatus(venue.id, venue.isActive)} className="w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">
                                                        {venue.isActive ? 'Disable' : 'Enable'}
                                                    </button>
                                                    <button onClick={() => handleDelete(venue.id)} className="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50">Delete</button>
                                                </>}
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
                    <span className="text-sm text-gray-600">Showing {(currentPage - 1) * itemsPerPage + 1} to {Math.min(currentPage * itemsPerPage, filteredVenues.length)} of {filteredVenues.length}</span>
                    <div className="flex space-x-2">
                        <button disabled={currentPage === 1} onClick={() => setCurrentPage(prev => prev - 1)} className="px-3 py-1 border border-gray-300 rounded-md text-sm disabled:opacity-50">Previous</button>
                        <button disabled={currentPage === totalPages} onClick={() => setCurrentPage(prev => prev + 1)} className="px-3 py-1 border border-gray-300 rounded-md text-sm disabled:opacity-50">Next</button>
                    </div>
                </div>
            )}

            {/* Add/Edit Modal */}
            {modalState.isOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50 px-4">
                    <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6 relative max-h-[90vh] overflow-y-auto">
                        <button onClick={closeModal} className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 text-xl font-bold">&times;</button>
                        <h3 className="text-xl font-bold mb-4">{modalState.mode === 'add' ? 'Add New Venue' : 'Edit Venue'}</h3>
                        
                        <form onSubmit={handleSubmit} className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Venue Name</label>
                                <input type="text" required value={formData.name} onChange={(e) => setFormData({...formData, name: e.target.value})} className="w-full rounded-md border border-gray-300 px-3 py-2 focus:ring-1 focus:ring-gray-900" />
                            </div>
                            
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Venue Type</label>
                                <select required value={formData.type} onChange={(e) => setFormData({...formData, type: e.target.value})} className="w-full rounded-md border border-gray-300 px-3 py-2 focus:ring-1 focus:ring-gray-900 bg-white">
                                    <option value="" disabled>Select Type</option>
                                    {venueTypes.map(type => <option key={type} value={type}>{type}</option>)}
                                </select>
                            </div>

                            {isAdmin && (
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Venue owner</label>
                                    <select
                                        value={formData.ownerId || ''}
                                        onChange={(e) => setFormData({...formData, ownerId:e.target.value})}
                                        className="w-full rounded-md border border-gray-300 bg-white px-3 py-2"
                                    >
                                        <option value="">Unassigned</option>
                                        {owners.map(owner => (
                                            <option key={owner.id} value={owner.id}>
                                                {owner.fullName} · {owner.email}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                            )}

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Pincode</label>
                                <input
                                    type="text"
                                    inputMode="numeric"
                                    pattern="[0-9]{6}"
                                    maxLength={6}
                                    required
                                    value={formData.pincode}
                                    onChange={(e) => {
                                        setFormData({...formData, pincode: e.target.value.replace(/\D/g, ''), city: '', state: ''});
                                        setPincodeLookup({isLoading: false, error: ''});
                                    }}
                                    className="w-full rounded-md border border-gray-300 px-3 py-2 focus:ring-1 focus:ring-gray-900"
                                />
                                {pincodeLookup.isLoading && <p className="mt-1 text-sm text-gray-500">Looking up location...</p>}
                                {pincodeLookup.error && <p className="mt-1 text-sm text-red-600">{pincodeLookup.error}</p>}
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Address</label>
                                <textarea required rows="2" value={formData.address} onChange={(e) => setFormData({...formData, address: e.target.value})} className="w-full rounded-md border border-gray-300 px-3 py-2 focus:ring-1 focus:ring-gray-900" />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">City</label>
                                <input type="text" required readOnly value={formData.city} className="w-full rounded-md border border-gray-300 bg-gray-50 px-3 py-2" />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">State</label>
                                <input type="text" required readOnly value={formData.state} className="w-full rounded-md border border-gray-300 bg-gray-50 px-3 py-2" />
                            </div>

                            <div className="flex justify-end space-x-3 mt-6">
                                <button type="button" onClick={closeModal} className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50">Cancel</button>
                                <button type="submit" disabled={isSubmitting} className="px-4 py-2 bg-gray-900 text-white rounded-md text-sm font-medium hover:bg-gray-800 disabled:opacity-70">
                                    {isSubmitting ? 'Saving...' : modalState.mode === 'add' ? 'Save Venue' : 'Update Venue'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}