import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import api from "../../api/axiosClient";
import { getApiErrorMessage, showToast } from "../../context/toastService";
import { useAuth } from "../../context/AuthContext";

const EMPTY_FORM = {
    name: "",
    seatingMode: "RESERVED",
    rows: "",
    seatsPerRow: "",
    capacity: ""
};

function SeatPreview({rows, seatsPerRow}) {
    const previewRows = Math.min(rows, 5);
    const previewSeats = Math.min(seatsPerRow, 12);

    return (
        <div className="mt-4 overflow-x-auto">
            <div className="mx-auto mb-3 h-1 w-2/3 rounded bg-gray-300" aria-label="Stage or screen" />
            <div className="space-y-1">
                {Array.from({length: previewRows}, (_, rowIndex) => (
                    <div className="flex justify-center gap-1" key={rowIndex}>
                        {Array.from({length: previewSeats}, (_, seatIndex) => (
                            <span
                                className="flex h-5 w-5 items-center justify-center rounded-t-md bg-gray-200 text-[9px] text-gray-600"
                                key={seatIndex}
                                title={`Row ${rowIndex + 1}, seat ${seatIndex + 1}`}
                            >
                                {seatIndex + 1}
                            </span>
                        ))}
                        {seatsPerRow > previewSeats && <span className="px-1 text-xs text-gray-500">...</span>}
                    </div>
                ))}
                {rows > previewRows && <p className="text-center text-xs text-gray-500">... {rows - previewRows} more rows</p>}
            </div>
        </div>
    );
}

export default function ManageVenueSpaces() {
    const {venueId} = useParams();
    const navigate = useNavigate();
    const {user} = useAuth();
    const [venue, setVenue] = useState(null);
    const [spaces, setSpaces] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [modalState, setModalState] = useState({isOpen: false, mode: "add", spaceId: null});
    const [formData, setFormData] = useState(EMPTY_FORM);
    const [formError, setFormError] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);

    const fetchSpaces = useCallback(async () => {
        setIsLoading(true);
        try {
            const {data} = await api.get(`/admin/venue/${venueId}/spaces`);
            setVenue(data);
            setSpaces(data.spaces);
        } catch (error) {
            showToast(getApiErrorMessage(error, "Failed to load venue spaces."));
        } finally {
            setIsLoading(false);
        }
    }, [venueId]);

    useEffect(() => {
        let isActive = true;
        api.get(`/admin/venue/${venueId}/spaces`)
            .then(({data}) => {
                if (!isActive) return;
                setVenue(data);
                setSpaces(data.spaces);
            })
            .catch(error => {
                if (isActive) showToast(getApiErrorMessage(error, "Failed to load venue spaces."));
            })
            .finally(() => {
                if (isActive) setIsLoading(false);
            });
        return () => { isActive = false; };
    }, [venueId]);

    const openModal = (mode, space = null) => {
        setFormError("");
        setModalState({isOpen: true, mode, spaceId: space?.id ?? null});
        setFormData(space
            ? {
                name: space.name,
                seatingMode: space.seatingMode,
                rows: space.rows ?? "",
                seatsPerRow: space.seatsPerRow ?? "",
                capacity: space.seatingMode === "GENERAL_ADMISSION" ? space.capacity : ""
            }
            : EMPTY_FORM);
    };

    const closeModal = () => setModalState({isOpen: false, mode: "add", spaceId: null});

    const handleSubmit = async (event) => {
        event.preventDefault();
        setIsSubmitting(true);
        setFormError("");
        const request = {
            name: formData.name.trim(),
            seatingMode: formData.seatingMode,
            ...(formData.seatingMode === "RESERVED"
                ? {rows: Number(formData.rows), seatsPerRow: Number(formData.seatsPerRow)}
                : {capacity: Number(formData.capacity)})
        };

        try {
            if (modalState.mode === "add") {
                await api.post(`/admin/venue/${venueId}/spaces`, request);
            } else {
                await api.put(`/admin/venue/${venueId}/spaces/${modalState.spaceId}`, request);
            }
            closeModal();
            showToast(modalState.mode === "add" ? "Space added successfully." : "Space updated successfully.", "success");
            await fetchSpaces();
        } catch (error) {
            const message = getApiErrorMessage(error, "Could not save this space.");
            setFormError(message);
            showToast(message);
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleDelete = async (space) => {
        if (!window.confirm(`Remove ${space.name} from this venue?`)) return;
        try {
            await api.delete(`/admin/venue/${venueId}/spaces/${space.id}`);
            showToast("Space removed successfully.", "success");
            await fetchSpaces();
        } catch (error) {
            showToast(getApiErrorMessage(error, "Could not remove this space."));
        }
    };

    const reservedCapacity = Number(formData.rows) * Number(formData.seatsPerRow);
    const spaceLabel = venue?.spaceType === "SCREEN" ? "Screen"
        : venue?.spaceType === "STAGE" ? "Stage"
            : "Hall";

    return (
        <section className="w-full rounded-xl border border-gray-100 bg-white p-6 shadow-sm">
            <button
                type="button"
                onClick={() => navigate(user?.role === "OWNER" ? "/owner" : "/admin/venues")}
                className="mb-4 text-sm font-medium text-gray-600 hover:text-gray-900"
            >
                &larr; Back to venues
            </button>

            <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div>
                    <h1 className="text-2xl font-bold text-gray-900">Venue Management</h1>
                    {venue && (
                        <p className="mt-1 text-sm text-gray-600">
                            {venue.venueName} &middot; Manage {spaceLabel.toLowerCase()}s and their seating layouts.
                        </p>
                    )}
                </div>
                <button
                    type="button"
                    onClick={() => openModal("add")}
                    className="rounded-md bg-gray-900 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-gray-800"
                >
                    + Add {spaceLabel}
                </button>
            </div>

            {isLoading ? (
                <p className="py-8 text-center text-gray-500">Loading spaces...</p>
            ) : spaces.length === 0 ? (
                <div className="rounded-lg border border-dashed border-gray-300 py-12 text-center">
                    <p className="font-medium text-gray-800">No {spaceLabel.toLowerCase()}s added yet.</p>
                    <p className="mt-1 text-sm text-gray-500">Add a {spaceLabel.toLowerCase()} to configure its capacity and seating.</p>
                </div>
            ) : (
                <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
                    {spaces.map(space => (
                        <article key={space.id} className="rounded-lg border border-gray-200 p-5">
                            <div className="flex items-start justify-between gap-3">
                                <div>
                                    <h2 className="text-lg font-semibold text-gray-900">{space.name}</h2>
                                    <p className="mt-1 text-sm text-gray-600">
                                        {space.capacity.toLocaleString()} capacity
                                        {space.seatingMode === "RESERVED"
                                            ? ` · ${space.rows} rows · ${space.seatsPerRow} seats per row`
                                            : " · General admission"}
                                    </p>
                                </div>
                                <div className="flex gap-2">
                                    <button
                                        type="button"
                                        onClick={() => openModal("edit", space)}
                                        className="rounded border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50"
                                    >
                                        Edit
                                    </button>
                                    <button
                                        type="button"
                                        onClick={() => handleDelete(space)}
                                        className="rounded border border-red-200 px-3 py-1.5 text-sm text-red-700 hover:bg-red-50"
                                    >
                                        Remove
                                    </button>
                                </div>
                            </div>
                            {space.seatingMode === "RESERVED" && (
                                <SeatPreview rows={space.rows} seatsPerRow={space.seatsPerRow} />
                            )}
                        </article>
                    ))}
                </div>
            )}

            {modalState.isOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4 py-6">
                    <div role="dialog" aria-modal="true" aria-labelledby="space-modal-title" className="max-h-full w-full max-w-md overflow-y-auto rounded-xl bg-white p-6 shadow-xl">
                        <h2 id="space-modal-title" className="mb-5 text-xl font-bold text-gray-900">
                            {modalState.mode === "add" ? `Add ${spaceLabel}` : `Edit ${spaceLabel}`}
                        </h2>
                        {formError && <div role="alert" className="mb-4 rounded-md bg-red-50 p-3 text-sm text-red-700">{formError}</div>}

                        <form onSubmit={handleSubmit} className="space-y-4">
                            <div>
                                <label htmlFor="space-name" className="mb-1 block text-sm font-medium text-gray-700">
                                    {spaceLabel} name
                                </label>
                                <input
                                    id="space-name"
                                    required
                                    maxLength={80}
                                    value={formData.name}
                                    onChange={event => setFormData({...formData, name: event.target.value})}
                                    placeholder={spaceLabel === "Screen" ? "Screen 1" : `${spaceLabel} 1`}
                                    className="w-full rounded-md border border-gray-300 px-3 py-2 focus:ring-1 focus:ring-gray-900"
                                />
                            </div>

                            <div>
                                <label htmlFor="seating-mode" className="mb-1 block text-sm font-medium text-gray-700">
                                    Seating
                                </label>
                                <select
                                    id="seating-mode"
                                    value={formData.seatingMode}
                                    onChange={event => setFormData({...EMPTY_FORM, seatingMode: event.target.value, name: formData.name})}
                                    className="w-full rounded-md border border-gray-300 bg-white px-3 py-2 focus:ring-1 focus:ring-gray-900"
                                >
                                    <option value="RESERVED">Numbered seats</option>
                                    <option value="GENERAL_ADMISSION">General admission</option>
                                </select>
                            </div>

                            {formData.seatingMode === "RESERVED" ? (
                                <>
                                    <div className="grid grid-cols-2 gap-3">
                                        <div>
                                            <label htmlFor="space-rows" className="mb-1 block text-sm font-medium text-gray-700">Rows</label>
                                            <input
                                                id="space-rows"
                                                type="number"
                                                min="1"
                                                max="200"
                                                required
                                                value={formData.rows}
                                                onChange={event => setFormData({...formData, rows: event.target.value})}
                                                className="w-full rounded-md border border-gray-300 px-3 py-2"
                                            />
                                        </div>
                                        <div>
                                            <label htmlFor="seats-per-row" className="mb-1 block text-sm font-medium text-gray-700">Seats per row</label>
                                            <input
                                                id="seats-per-row"
                                                type="number"
                                                min="1"
                                                max="200"
                                                required
                                                value={formData.seatsPerRow}
                                                onChange={event => setFormData({...formData, seatsPerRow: event.target.value})}
                                                className="w-full rounded-md border border-gray-300 px-3 py-2"
                                            />
                                        </div>
                                    </div>
                                    <p className="text-sm text-gray-600">
                                        Total seat capacity: <strong>{reservedCapacity > 0 && reservedCapacity <= 10000 ? reservedCapacity.toLocaleString() : "—"}</strong>
                                    </p>
                                </>
                            ) : (
                                <div>
                                    <label htmlFor="space-capacity" className="mb-1 block text-sm font-medium text-gray-700">Maximum capacity</label>
                                    <input
                                        id="space-capacity"
                                        type="number"
                                        min="1"
                                        max="10000"
                                        required
                                        value={formData.capacity}
                                        onChange={event => setFormData({...formData, capacity: event.target.value})}
                                        className="w-full rounded-md border border-gray-300 px-3 py-2"
                                    />
                                </div>
                            )}

                            <div className="flex justify-end gap-3 pt-2">
                                <button type="button" onClick={closeModal} className="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50">
                                    Cancel
                                </button>
                                <button type="submit" disabled={isSubmitting} className="rounded-md bg-gray-900 px-4 py-2 text-sm font-medium text-white hover:bg-gray-800 disabled:opacity-60">
                                    {isSubmitting ? "Saving..." : modalState.mode === "add" ? "Add space" : "Save changes"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </section>
    );
}
