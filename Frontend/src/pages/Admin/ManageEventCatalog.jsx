import { useEffect, useState } from "react";
import api from "../../api/axiosClient";
import { getApiErrorMessage, showToast } from "../../context/toastService";

const EMPTY_FORM = {title: "", description: "", durationMinutes: ""};

export default function ManageEventCatalog() {
    const [events, setEvents] = useState([]);
    const [form, setForm] = useState(EMPTY_FORM);
    const [editingId, setEditingId] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [posterFile, setPosterFile] = useState(null);
    const [deletingId, setDeletingId] = useState(null);

    const getPosterSrc = posterUrl => posterUrl?.startsWith("/events/")
        ? `${api.defaults.baseURL.replace(/\/$/, "")}${posterUrl}`
        : posterUrl;

    const fetchEvents = async () => {
        try {
            const {data} = await api.get("/admin/events");
            setEvents(data);
        } catch (error) {
            showToast(getApiErrorMessage(error, "Failed to load event catalog."));
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        let isActive = true;
        api.get("/admin/events")
            .then(({data}) => {
                if (isActive) setEvents(data);
            })
            .catch(error => {
                if (isActive) showToast(getApiErrorMessage(error, "Failed to load event catalog."));
            })
            .finally(() => {
                if (isActive) setIsLoading(false);
            });
        return () => { isActive = false; };
    }, []);

    const handleSubmit = async (event) => {
        event.preventDefault();
        setIsSaving(true);
        const eventDetails = {
            title: form.title.trim(),
            description: form.description.trim(),
            durationMinutes: form.durationMinutes ? Number(form.durationMinutes) : null
        };
        const request = new FormData();
        request.append("event", new Blob([JSON.stringify(eventDetails)], {type: "application/json"}));
        if (posterFile) request.append("poster", posterFile);
        try {
            if (editingId) {
                await api.put(`/admin/events/${editingId}`, request, {headers: {"Content-Type": "multipart/form-data"}});
                showToast("Catalog event updated.", "success");
            } else {
                await api.post("/admin/events", request, {headers: {"Content-Type": "multipart/form-data"}});
                showToast("Catalog event created.", "success");
            }
            setForm(EMPTY_FORM);
            setEditingId(null);
            setPosterFile(null);
            await fetchEvents();
        } catch (error) {
            showToast(getApiErrorMessage(error, "Could not save catalog event."));
        } finally {
            setIsSaving(false);
        }
    };

    const editEvent = (event) => {
        setEditingId(event.id);
        setForm({
            title: event.title,
            description: event.description || "",
            durationMinutes: event.durationMinutes || ""
        });
        setPosterFile(null);
    };

    const cancelEdit = () => {
        setEditingId(null);
        setForm(EMPTY_FORM);
        setPosterFile(null);
    };

    const deleteEvent = async event => {
        if (!window.confirm(`Permanently delete "${event.title}"? Events linked to venue showtimes cannot be deleted.`)) return;
        setDeletingId(event.id);
        try {
            await api.delete(`/admin/events/${event.id}`);
            setEvents(currentEvents => currentEvents.filter(item => item.id !== event.id));
            if (editingId === event.id) cancelEdit();
            showToast("Catalog event permanently deleted.", "success");
        } catch (error) {
            showToast(getApiErrorMessage(error, "Could not delete catalog event."));
        } finally {
            setDeletingId(null);
        }
    };

    return (
        <div className="space-y-6">
            <section className="rounded-xl border border-gray-100 bg-white p-6 shadow-sm">
                <h1 className="text-2xl font-bold text-gray-900">Event Catalog</h1>
                <p className="mt-1 text-sm text-gray-600">
                    Create each movie or event once. Venue owners select it when adding their showtimes.
                </p>
                <form onSubmit={handleSubmit} className="mt-6 grid grid-cols-1 gap-4 md:grid-cols-2">
                    <div>
                        <label htmlFor="catalog-title" className="mb-1 block text-sm font-medium text-gray-700">Title</label>
                        <input
                            id="catalog-title"
                            required
                            maxLength={120}
                            value={form.title}
                            onChange={event => setForm({...form, title: event.target.value})}
                            className="w-full rounded-md border border-gray-300 px-3 py-2"
                        />
                    </div>
                    <div>
                        <label htmlFor="catalog-poster" className="mb-1 block text-sm font-medium text-gray-700">Poster image</label>
                        <input
                            id="catalog-poster"
                            type="file"
                            accept="image/jpeg,image/png"
                            onChange={event => {
                                const file = event.target.files?.[0] || null;
                                if (file && file.size > 5 * 1024 * 1024) {
                                    showToast("Poster image must be 5 MB or smaller.");
                                    event.target.value = "";
                                    setPosterFile(null);
                                    return;
                                }
                                setPosterFile(file);
                            }}
                            className="w-full rounded-md border border-gray-300 px-3 py-2"
                        />
                        <p className="mt-1 text-xs text-gray-500">JPEG or PNG, up to 5 MB. Leave empty to keep the current poster when editing.</p>
                        {posterFile && <p className="mt-1 text-xs text-gray-700">Selected: {posterFile.name}</p>}
                    </div>
                    <div>
                        <label htmlFor="catalog-duration" className="mb-1 block text-sm font-medium text-gray-700">Duration (minutes)</label>
                        <input
                            id="catalog-duration"
                            type="number"
                            min="1"
                            max="1440"
                            value={form.durationMinutes}
                            onChange={event => setForm({...form, durationMinutes: event.target.value})}
                            className="w-full rounded-md border border-gray-300 px-3 py-2"
                        />
                    </div>
                    <div className="md:row-span-2">
                        <label htmlFor="catalog-description" className="mb-1 block text-sm font-medium text-gray-700">Description</label>
                        <textarea
                            id="catalog-description"
                            rows="5"
                            maxLength={5000}
                            value={form.description}
                            onChange={event => setForm({...form, description: event.target.value})}
                            className="w-full rounded-md border border-gray-300 px-3 py-2"
                        />
                    </div>
                    <div className="flex items-end gap-2 md:col-start-1">
                        <button
                            type="submit"
                            disabled={isSaving}
                            className="rounded-md bg-gray-900 px-4 py-2 text-sm font-medium text-white hover:bg-gray-800 disabled:opacity-60"
                        >
                            {isSaving ? "Saving..." : editingId ? "Save changes" : "Add catalog event"}
                        </button>
                        {editingId && (
                            <button type="button" onClick={cancelEdit} className="rounded-md border border-gray-300 px-4 py-2 text-sm">
                                Cancel
                            </button>
                        )}
                    </div>
                </form>
            </section>

            <section className="rounded-xl border border-gray-100 bg-white p-6 shadow-sm">
                <h2 className="mb-4 text-xl font-bold text-gray-900">Catalog events</h2>
                {isLoading ? <p className="py-6 text-center text-sm text-gray-500">Loading events...</p>
                    : events.length === 0 ? <p className="py-6 text-center text-sm text-gray-500">No catalog events created.</p>
                        : <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                            {events.map(event => (
                                <article key={event.id} className="overflow-hidden rounded-lg border border-gray-200">
                                    {event.posterUrl
                                        ? <img src={getPosterSrc(event.posterUrl)} alt={`${event.title} poster`} className="h-56 w-full bg-gray-100 object-cover" />
                                        : <div className="flex h-56 items-center justify-center bg-gray-100 text-sm text-gray-500">No poster added</div>}
                                    <div className="space-y-2 p-4">
                                        <h3 className="font-semibold text-gray-900">{event.title}</h3>
                                        {event.durationMinutes && <p className="text-xs text-gray-500">{event.durationMinutes} minutes</p>}
                                        {event.description && <p className="line-clamp-3 text-sm text-gray-600">{event.description}</p>}
                                        <button type="button" onClick={() => editEvent(event)} className="text-sm font-medium text-gray-800 hover:underline">
                                            Edit event
                                        </button>
                                        <button
                                            type="button"
                                            onClick={() => deleteEvent(event)}
                                            disabled={deletingId === event.id}
                                            className="ml-4 text-sm font-medium text-red-700 hover:underline disabled:opacity-60"
                                        >
                                            {deletingId === event.id ? "Deleting..." : "Delete permanently"}
                                        </button>
                                    </div>
                                </article>
                            ))}
                        </div>}
            </section>
        </div>
    );
}
