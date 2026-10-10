import { useCallback, useEffect, useRef, useState } from "react";
import api from "../../api/axiosClient";
import { getApiErrorMessage, showToast } from "../../context/toastService";
import { useAuth } from "../../context/AuthContext";

const newPerformance = (key) => ({
    key,
    venueSpaceId: "",
    startTime: "",
    endTime: "",
    priceBands: []
});

function formatDateTime(value) {
    return new Date(value).toLocaleString();
}

export default function ManageEvents() {
    const {user} = useAuth();
    const isOwner = user?.role === "OWNER";
    const [venues, setVenues] = useState([]);
    const [venueId, setVenueId] = useState("");
    const [spaces, setSpaces] = useState([]);
    const [isSpacesLoading, setIsSpacesLoading] = useState(false);
    const [catalogEvents, setCatalogEvents] = useState([]);
    const [showings, setShowings] = useState([]);
    const [performances, setPerformances] = useState([newPerformance(0)]);
    const [selectedEventId, setSelectedEventId] = useState("");
    const [isLoading, setIsLoading] = useState(true);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const nextPerformanceKey = useRef(1);

    const fetchShowings = useCallback(async () => {
        const {data} = await api.get(isOwner ? "/owner/showings" : "/admin/showings");
        setShowings(data);
    }, [isOwner]);

    useEffect(() => {
        let isActive = true;
        Promise.all([
            api.get("/admin/venue"),
            api.get(isOwner ? "/owner/events" : "/admin/events"),
            api.get(isOwner ? "/owner/showings" : "/admin/showings")
        ])
            .then(([venueResponse, eventResponse, showingResponse]) => {
                if (!isActive) return;
                setVenues(venueResponse.data);
                setCatalogEvents(eventResponse.data);
                setShowings(showingResponse.data);
            })
            .catch(error => {
                if (!isActive) return;
                showToast(getApiErrorMessage(error, "Failed to load event setup data."));
            })
            .finally(() => {
                if (isActive) setIsLoading(false);
            });
        return () => { isActive = false; };
    }, [isOwner]);

    useEffect(() => {
        if (!venueId) {
            return undefined;
        }
        const controller = new AbortController();
        api.get(`/admin/venue/${venueId}/spaces`, {signal: controller.signal})
            .then(({data}) => {
                setSpaces(data.spaces);
                setIsSpacesLoading(false);
                setPerformances(current => current.map(performance => {
                    if (!performance.venueSpaceId) return performance;
                    return newPerformance(performance.key);
                }));
            })
            .catch(error => {
                if (!controller.signal.aborted) {
                    setIsSpacesLoading(false);
                    showToast(getApiErrorMessage(error, "Failed to load venue screens and stages."));
                }
            });
        return () => controller.abort();
    }, [venueId]);

    const getSpace = (spaceId) => spaces.find(space => String(space.id) === String(spaceId));

    const updatePerformance = (key, changes) => {
        setPerformances(current => current.map(performance =>
            performance.key === key ? {...performance, ...changes} : performance
        ));
    };

    const handleSpaceChange = (key, spaceId) => {
        const space = getSpace(spaceId);
        const priceBands = !space ? [] : space.seatingMode === "RESERVED"
            ? [{categoryName: "Standard", rowFrom: 1, rowTo: space.rows, price: ""}]
            : [{categoryName: "General admission", rowFrom: "", rowTo: "", price: ""}];
        updatePerformance(key, {venueSpaceId: spaceId, priceBands});
    };

    const updatePriceBand = (performanceKey, bandIndex, changes) => {
        setPerformances(current => current.map(performance => {
            if (performance.key !== performanceKey) return performance;
            return {
                ...performance,
                priceBands: performance.priceBands.map((band, index) =>
                    index === bandIndex ? {...band, ...changes} : band
                )
            };
        }));
    };

    const addPriceBand = (performanceKey) => {
        setPerformances(current => current.map(performance => {
            if (performance.key !== performanceKey) return performance;
            return {
                ...performance,
                priceBands: [...performance.priceBands, {
                    categoryName: "",
                    rowFrom: "",
                    rowTo: "",
                    price: ""
                }]
            };
        }));
    };

    const removePriceBand = (performanceKey, bandIndex) => {
        setPerformances(current => current.map(performance => {
            if (performance.key !== performanceKey) return performance;
            return {
                ...performance,
                priceBands: performance.priceBands.filter((_, index) => index !== bandIndex)
            };
        }));
    };

    const validateForm = () => {
        if (!selectedEventId) return "Select an event from the catalog.";
        if (!venueId) return "Select a venue.";
        if (performances.length === 0 || performances.some(performance => !performance.venueSpaceId)) {
            return "Select a screen, hall, or stage for every schedule.";
        }
        for (const performance of performances) {
            if (!performance.startTime || !performance.endTime
                    || new Date(performance.endTime) <= new Date(performance.startTime)) {
                return "Every schedule needs an end time later than its start time.";
            }
            if (performance.priceBands.length === 0 || performance.priceBands.some(band =>
                !band.categoryName.trim() || band.price === "" || Number(band.price) < 0
            )) {
                return "Enter a name and price for every seat category.";
            }
            const space = getSpace(performance.venueSpaceId);
            if (space?.seatingMode === "RESERVED" && performance.priceBands.some(band =>
                band.rowFrom === "" || band.rowTo === ""
            )) {
                return "Set a row range for every reserved-seat category.";
            }
        }
        return "";
    };

    const handleSubmit = async (event) => {
        event.preventDefault();
        const validationError = validateForm();
        if (validationError) {
            showToast(validationError);
            return;
        }

        setIsSubmitting(true);
        const request = {
            eventId: Number(selectedEventId),
            venueId: Number(venueId),
            performances: performances.map(performance => ({
                venueSpaceId: Number(performance.venueSpaceId),
                startTime: performance.startTime,
                endTime: performance.endTime,
                priceBands: performance.priceBands.map(band => ({
                    categoryName: band.categoryName.trim(),
                    rowFrom: band.rowFrom === "" ? null : Number(band.rowFrom),
                    rowTo: band.rowTo === "" ? null : Number(band.rowTo),
                    price: Number(band.price)
                }))
            }))
        };

        try {
            const {data} = await api.post(isOwner ? "/owner/showings" : "/admin/showings", request);
            await fetchShowings();
            showToast(`Created ${data.length} showtime${data.length === 1 ? "" : "s"}.`, "success");
            setSelectedEventId("");
            setVenueId("");
            setSpaces([]);
            setIsSpacesLoading(false);
            setPerformances([newPerformance(nextPerformanceKey.current++)]);
        } catch (error) {
            showToast(getApiErrorMessage(error, "Could not create showtimes."));
        } finally {
            setIsSubmitting(false);
        }
    };

    const addPerformance = () => {
        setPerformances(current => [...current, newPerformance(nextPerformanceKey.current++)]);
    };

    const removePerformance = (key) => {
        setPerformances(current => current.length > 1
            ? current.filter(performance => performance.key !== key)
            : current);
    };

    return (
        <div className="space-y-6">
            <section className="rounded-xl border border-gray-100 bg-white p-6 shadow-sm">
                <h1 className="text-2xl font-bold text-gray-900">Showtime Management</h1>
                <p className="mt-1 text-sm text-gray-600">
                    Select a catalog event, venue and screens, then set schedule-specific seat prices.
                </p>

                {isLoading ? (
                    <p className="py-8 text-center text-gray-500">Loading venues and events...</p>
                ) : (
                    <form onSubmit={handleSubmit} className="mt-6 space-y-6">
                        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                            <div>
                                <label htmlFor="event-title" className="mb-1 block text-sm font-medium text-gray-700">Catalog event</label>
                                <select
                                    id="event-title"
                                    required
                                    value={selectedEventId}
                                    onChange={event => setSelectedEventId(event.target.value)}
                                    className="w-full rounded-md border border-gray-300 bg-white px-3 py-2"
                                >
                                    <option value="">Select event</option>
                                    {catalogEvents.map(item => <option key={item.id} value={item.id}>{item.title}</option>)}
                                </select>
                            </div>
                            <div>
                                <label htmlFor="event-venue" className="mb-1 block text-sm font-medium text-gray-700">Venue</label>
                                <select
                                    id="event-venue"
                                    required
                                    value={venueId}
                                    onChange={event => {
                                        setSpaces([]);
                                        setIsSpacesLoading(Boolean(event.target.value));
                                        setVenueId(event.target.value);
                                    }}
                                    className="w-full rounded-md border border-gray-300 bg-white px-3 py-2"
                                >
                                    <option value="">Select venue</option>
                                    {venues.map(venue => <option key={venue.id} value={venue.id}>{venue.name}</option>)}
                                </select>
                            </div>
                        </div>

                        {isSpacesLoading && <p className="text-sm text-gray-500">Loading screens, halls, and stages...</p>}
                        {venueId && !isSpacesLoading && spaces.length === 0 && (
                            <p className="rounded-md bg-amber-50 p-3 text-sm text-amber-800">
                                Add at least one screen, hall, or stage from Venue Management before scheduling events.
                            </p>
                        )}

                        <div className="space-y-4">
                            <div className="flex items-center justify-between">
                                <h2 className="text-lg font-semibold text-gray-900">Schedules and pricing</h2>
                                <button
                                    type="button"
                                    disabled={!spaces.length}
                                    onClick={addPerformance}
                                    className="rounded-md border border-gray-300 px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
                                >
                                    + Add screen/time
                                </button>
                            </div>

                            {performances.map((performance, performanceIndex) => {
                                const space = getSpace(performance.venueSpaceId);
                                return (
                                    <article key={performance.key} className="space-y-4 rounded-lg border border-gray-200 p-4">
                                        <div className="flex items-center justify-between">
                                            <h3 className="font-semibold text-gray-800">Schedule {performanceIndex + 1}</h3>
                                            {performances.length > 1 && (
                                                <button type="button" onClick={() => removePerformance(performance.key)} className="text-sm text-red-700 hover:underline">
                                                    Remove schedule
                                                </button>
                                            )}
                                        </div>

                                        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                                            <div>
                                                <label htmlFor={`space-${performance.key}`} className="mb-1 block text-sm font-medium text-gray-700">Screen, hall, or stage</label>
                                                <select
                                                    id={`space-${performance.key}`}
                                                    required
                                                    value={performance.venueSpaceId}
                                                    onChange={event => handleSpaceChange(performance.key, event.target.value)}
                                                    disabled={!spaces.length}
                                                    className="w-full rounded-md border border-gray-300 bg-white px-3 py-2"
                                                >
                                                    <option value="">Select space</option>
                                                    {spaces.map(item => (
                                                        <option key={item.id} value={item.id}>
                                                            {item.name} · {item.capacity} capacity
                                                        </option>
                                                    ))}
                                                </select>
                                            </div>
                                            <div>
                                                <label htmlFor={`start-${performance.key}`} className="mb-1 block text-sm font-medium text-gray-700">Start</label>
                                                <input
                                                    id={`start-${performance.key}`}
                                                    type="datetime-local"
                                                    required
                                                    value={performance.startTime}
                                                    onChange={event => updatePerformance(performance.key, {startTime: event.target.value})}
                                                    className="w-full rounded-md border border-gray-300 px-3 py-2"
                                                />
                                            </div>
                                            <div>
                                                <label htmlFor={`end-${performance.key}`} className="mb-1 block text-sm font-medium text-gray-700">End</label>
                                                <input
                                                    id={`end-${performance.key}`}
                                                    type="datetime-local"
                                                    required
                                                    value={performance.endTime}
                                                    onChange={event => updatePerformance(performance.key, {endTime: event.target.value})}
                                                    className="w-full rounded-md border border-gray-300 px-3 py-2"
                                                />
                                            </div>
                                        </div>

                                        {space && (
                                            <div className="space-y-3">
                                                <div>
                                                    <h4 className="text-sm font-semibold text-gray-800">Event seat prices</h4>
                                                    <p className="text-xs text-gray-500">
                                                        {space.seatingMode === "RESERVED"
                                                            ? `Assign non-overlapping row ranges covering all ${space.rows} rows.`
                                                            : `One ticket price for general admission (capacity ${space.capacity}).`}
                                                    </p>
                                                </div>
                                                {performance.priceBands.map((band, bandIndex) => (
                                                    <div key={`${performance.key}-band-${bandIndex}`} className="grid grid-cols-1 items-end gap-3 sm:grid-cols-4">
                                                        <div>
                                                            <label htmlFor={`band-name-${performance.key}-${bandIndex}`} className="mb-1 block text-xs font-medium text-gray-600">Category</label>
                                                            <input
                                                                id={`band-name-${performance.key}-${bandIndex}`}
                                                                required
                                                                maxLength={40}
                                                                value={band.categoryName}
                                                                onChange={event => updatePriceBand(performance.key, bandIndex, {categoryName: event.target.value})}
                                                                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
                                                            />
                                                        </div>
                                                        {space.seatingMode === "RESERVED" ? (
                                                            <>
                                                                <div>
                                                                    <label htmlFor={`band-from-${performance.key}-${bandIndex}`} className="mb-1 block text-xs font-medium text-gray-600">From row</label>
                                                                    <input
                                                                        id={`band-from-${performance.key}-${bandIndex}`}
                                                                        type="number"
                                                                        min="1"
                                                                        max={space.rows}
                                                                        required
                                                                        value={band.rowFrom}
                                                                        onChange={event => updatePriceBand(performance.key, bandIndex, {rowFrom: event.target.value})}
                                                                        className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
                                                                    />
                                                                </div>
                                                                <div>
                                                                    <label htmlFor={`band-to-${performance.key}-${bandIndex}`} className="mb-1 block text-xs font-medium text-gray-600">To row</label>
                                                                    <input
                                                                        id={`band-to-${performance.key}-${bandIndex}`}
                                                                        type="number"
                                                                        min="1"
                                                                        max={space.rows}
                                                                        required
                                                                        value={band.rowTo}
                                                                        onChange={event => updatePriceBand(performance.key, bandIndex, {rowTo: event.target.value})}
                                                                        className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
                                                                    />
                                                                </div>
                                                            </>
                                                        ) : <div className="hidden sm:block" />}
                                                        <div className="flex gap-2">
                                                            <div className="min-w-0 flex-1">
                                                                <label htmlFor={`band-price-${performance.key}-${bandIndex}`} className="mb-1 block text-xs font-medium text-gray-600">Price</label>
                                                                <input
                                                                    id={`band-price-${performance.key}-${bandIndex}`}
                                                                    type="number"
                                                                    min="0"
                                                                    step="0.01"
                                                                    required
                                                                    value={band.price}
                                                                    onChange={event => updatePriceBand(performance.key, bandIndex, {price: event.target.value})}
                                                                    className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
                                                                />
                                                            </div>
                                                            {performance.priceBands.length > 1 && (
                                                                <button type="button" onClick={() => removePriceBand(performance.key, bandIndex)} className="mb-0.5 rounded px-2 py-2 text-sm text-red-700 hover:bg-red-50" aria-label="Remove price category">
                                                                    Remove
                                                                </button>
                                                            )}
                                                        </div>
                                                    </div>
                                                ))}
                                                {space.seatingMode === "RESERVED" && (
                                                    <button type="button" onClick={() => addPriceBand(performance.key)} className="text-sm font-medium text-gray-700 hover:underline">
                                                        + Add price category
                                                    </button>
                                                )}
                                            </div>
                                        )}
                                    </article>
                                );
                            })}
                        </div>

                        <div className="flex justify-end">
                            <button
                                type="submit"
                                disabled={isSubmitting || !spaces.length}
                                className="rounded-md bg-gray-900 px-5 py-2.5 text-sm font-medium text-white hover:bg-gray-800 disabled:opacity-60"
                            >
                                {isSubmitting ? "Creating..." : "Create event schedules"}
                            </button>
                        </div>
                    </form>
                )}
            </section>

            <section className="rounded-xl border border-gray-100 bg-white p-6 shadow-sm">
                <h2 className="mb-4 text-xl font-bold text-gray-900">Scheduled showtimes</h2>
                {showings.length === 0 ? (
                    <p className="py-6 text-center text-sm text-gray-500">No showtimes have been scheduled yet.</p>
                ) : (
                    <div className="overflow-x-auto">
                        <table className="w-full min-w-[720px] border-collapse text-left">
                            <thead>
                                <tr className="border-b border-gray-200 bg-gray-50">
                                    <th className="px-4 py-3 text-sm font-semibold text-gray-600">Event</th>
                                    <th className="px-4 py-3 text-sm font-semibold text-gray-600">Venue / space</th>
                                    <th className="px-4 py-3 text-sm font-semibold text-gray-600">Time</th>
                                    <th className="px-4 py-3 text-sm font-semibold text-gray-600">Prices</th>
                                </tr>
                            </thead>
                            <tbody>
                                {showings.map(event => (
                                    <tr key={event.id} className="border-b border-gray-100">
                                        <td className="px-4 py-3 text-sm font-medium text-gray-900">{event.title}</td>
                                        <td className="px-4 py-3 text-sm text-gray-600">{event.venueName} · {event.venueSpaceName || "No space selected"}</td>
                                        <td className="px-4 py-3 text-sm text-gray-600">
                                            {formatDateTime(event.startTime)}
                                            {event.endTime && <> – {formatDateTime(event.endTime)}</>}
                                        </td>
                                        <td className="px-4 py-3 text-sm text-gray-600">
                                            {event.priceBands.map(band =>
                                                `${band.categoryName}${band.rowFrom ? ` (rows ${band.rowFrom}-${band.rowTo})` : ""}: ${band.price}`
                                            ).join(" · ") || "—"}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </section>
        </div>
    );
}
