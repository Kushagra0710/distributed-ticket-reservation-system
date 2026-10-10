import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../../api/axiosClient";
import { getApiErrorMessage, showToast } from "../../context/toastService";

function formatDateTime(value) {
    return new Date(value).toLocaleString([], {
        weekday: "short",
        month: "short",
        day: "numeric",
        hour: "numeric",
        minute: "2-digit"
    });
}

function groupShowingsByVenue(showings) {
    const venues = new Map();
    showings.forEach(showing => {
        if (!venues.has(showing.venueId)) {
            venues.set(showing.venueId, {name: showing.venueName, showings: []});
        }
        venues.get(showing.venueId).showings.push(showing);
    });
    return [...venues.values()];
}

export default function EventDiscovery() {
    const [cities, setCities] = useState([]);
    const [selectedCity, setSelectedCity] = useState("");
    const [events, setEvents] = useState([]);
    const [selectedEventId, setSelectedEventId] = useState(null);
    const [isLoadingCities, setIsLoadingCities] = useState(true);
    const [isLoadingEvents, setIsLoadingEvents] = useState(false);

    const getPosterSrc = posterUrl => posterUrl?.startsWith("/events/")
        ? `${api.defaults.baseURL.replace(/\/$/, "")}${posterUrl}`
        : posterUrl;

    useEffect(() => {
        let isActive = true;
        api.get("/events/cities")
            .then(({data}) => {
                if (isActive) setCities(data);
            })
            .catch(error => {
                if (isActive) showToast(getApiErrorMessage(error, "Could not load event locations."));
            })
            .finally(() => {
                if (isActive) setIsLoadingCities(false);
            });
        return () => { isActive = false; };
    }, []);

    useEffect(() => {
        if (!selectedCity) return undefined;
        let isActive = true;
        Promise.resolve()
            .then(() => {
                if (!isActive) return null;
                setIsLoadingEvents(true);
                return api.get("/events", {params: {city: selectedCity}});
            })
            .then(response => {
                if (isActive && response) setEvents(response.data);
            })
            .catch(error => {
                if (isActive) showToast(getApiErrorMessage(error, "Could not load events for this location."));
            })
            .finally(() => {
                if (isActive) setIsLoadingEvents(false);
            });
        return () => { isActive = false; };
    }, [selectedCity]);

    return (
        <main className="min-h-screen bg-gray-50">
            <header className="border-b border-gray-200 bg-white">
                <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-5 sm:px-6 lg:px-8">
                    <h1 className="text-xl font-bold text-gray-900">Discover events</h1>
                    <Link to="/login" className="text-sm font-medium text-gray-700 hover:text-gray-950">Sign in</Link>
                </div>
            </header>

            <section className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
                <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
                    <div>
                        <h2 className="text-3xl font-bold text-gray-900">Find your next show</h2>
                        <p className="mt-2 text-gray-600">Choose a city to see events and the theatres showing them.</p>
                    </div>
                    <div className="w-full sm:max-w-xs">
                        <label htmlFor="event-city" className="mb-1 block text-sm font-medium text-gray-700">City</label>
                        <select
                            id="event-city"
                            value={selectedCity}
                            disabled={isLoadingCities}
                            onChange={event => {
                                setEvents([]);
                                setSelectedEventId(null);
                                if (!event.target.value) setIsLoadingEvents(false);
                                setSelectedCity(event.target.value);
                            }}
                            className="w-full rounded-md border border-gray-300 bg-white px-3 py-2.5"
                        >
                            <option value="">{isLoadingCities ? "Loading cities..." : "Select your city"}</option>
                            {cities.map(city => <option key={city} value={city}>{city}</option>)}
                        </select>
                    </div>
                </div>

                {!selectedCity && !isLoadingCities && (
                    <p className="rounded-lg border border-dashed border-gray-300 bg-white p-8 text-center text-gray-600">
                        Select a city to browse available events.
                    </p>
                )}
                {isLoadingEvents && <p className="py-12 text-center text-gray-500">Loading events...</p>}
                {selectedCity && !isLoadingEvents && events.length === 0 && (
                    <p className="rounded-lg bg-white p-8 text-center text-gray-600">
                        No upcoming events are listed in {selectedCity} yet.
                    </p>
                )}

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
                    {events.map(event => {
                        const expanded = selectedEventId === event.id;
                        return (
                            <article key={event.id} className={`overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm ${expanded ? "sm:col-span-2 lg:col-span-4" : ""}`}>
                                <button
                                    type="button"
                                    onClick={() => setSelectedEventId(expanded ? null : event.id)}
                                    aria-expanded={expanded}
                                    className="block w-full text-left"
                                >
                                    {event.posterUrl
                                        ? <img src={getPosterSrc(event.posterUrl)} alt={`${event.title} poster`} className="h-72 w-full bg-gray-100 object-cover" />
                                        : <div className="flex h-72 items-center justify-center bg-gray-100 text-sm text-gray-500">Poster coming soon</div>}
                                    <div className="p-4">
                                        <h3 className="font-semibold text-gray-900">{event.title}</h3>
                                        {event.durationMinutes && <p className="mt-1 text-xs text-gray-500">{event.durationMinutes} minutes</p>}
                                        <p className="mt-2 text-sm font-medium text-gray-700">
                                            {event.showings.length} showtime{event.showings.length === 1 ? "" : "s"} in {selectedCity}
                                        </p>
                                    </div>
                                </button>

                                {expanded && (
                                    <div className="space-y-4 border-t border-gray-200 p-4 sm:col-span-2 lg:col-span-4">
                                        {event.description && <p className="text-sm text-gray-600">{event.description}</p>}
                                        {groupShowingsByVenue(event.showings).map(venue => (
                                            <section key={venue.name}>
                                                <h4 className="font-semibold text-gray-900">{venue.name}</h4>
                                                <div className="mt-2 flex flex-wrap gap-2">
                                                    {venue.showings.map(showing => (
                                                        <div key={showing.id} className="rounded-md border border-gray-200 px-3 py-2">
                                                            <p className="text-sm font-medium text-gray-800">{showing.venueSpaceName}</p>
                                                            <p className="text-xs text-gray-600">{formatDateTime(showing.startTime)}</p>
                                                            {showing.priceBands.length > 0 && (
                                                                <p className="mt-1 text-xs text-gray-500">
                                                                    From {Math.min(...showing.priceBands.map(band => Number(band.price))).toFixed(2)}
                                                                </p>
                                                            )}
                                                        </div>
                                                    ))}
                                                </div>
                                            </section>
                                        ))}
                                    </div>
                                )}
                            </article>
                        );
                    })}
                </div>
            </section>
        </main>
    );
}
