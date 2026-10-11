import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, ArrowRight, CalendarDays, Clock3, Film, Landmark, MapPin, Search, Sparkles, Ticket } from "lucide-react";
import api from "../../api/axiosClient";
import { useAuth } from "../../context/AuthContext";
import { getApiErrorMessage, showToast } from "../../context/toastService";

const CITY_LANDMARKS = {
    agra: {name: "Taj Mahal", icon: "🤍", colors: "from-rose-100 to-orange-50"},
    delhi: {name: "India Gate", icon: "🏛️", colors: "from-amber-100 to-yellow-50"},
    hyderabad: {name: "Charminar", icon: "🕌", colors: "from-violet-100 to-fuchsia-50"},
    jaipur: {name: "Hawa Mahal", icon: "🪟", colors: "from-orange-100 to-rose-50"},
    kolkata: {name: "Victoria Memorial", icon: "🏛️", colors: "from-emerald-100 to-teal-50"},
    mumbai: {name: "Gateway of India", icon: "🌊", colors: "from-sky-100 to-cyan-50"}
};

function normalizeCities(data) {
    if (!Array.isArray(data)) return [];
    return [...new Map(data
        .filter(city => typeof city === "string" && city.trim())
        .map(city => [city.trim().toLowerCase(), city.trim()])).values()]
        .sort((first, second) => first.localeCompare(second));
}

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
            venues.set(showing.venueId, {id: showing.venueId, name: showing.venueName, showings: []});
        }
        venues.get(showing.venueId).showings.push(showing);
    });
    return [...venues.values()];
}

function getPosterSrc(posterUrl) {
    return posterUrl?.startsWith("/events/")
        ? `${api.defaults.baseURL.replace(/\/$/, "")}${posterUrl}`
        : posterUrl;
}

function LandmarkTile({city, onSelect}) {
    const landmark = CITY_LANDMARKS[city.toLowerCase()] || {
        name: "Local landmarks",
        icon: "✨",
        colors: "from-indigo-100 to-sky-50"
    };

    return (
        <button
            type="button"
            onClick={() => onSelect(city)}
            className={`group flex min-h-36 flex-col justify-between rounded-2xl bg-gradient-to-br ${landmark.colors} p-4 text-left transition hover:-translate-y-1 hover:shadow-lg`}
        >
            <span className="text-3xl" aria-hidden="true">{landmark.icon}</span>
            <span>
                <span className="block font-semibold text-gray-900">{city}</span>
                <span className="mt-0.5 block text-xs text-gray-600">{landmark.name}</span>
            </span>
        </button>
    );
}

function CityPicker({cities, value = "", isLoading = false, disabled = false, onSelect, id, compact = false}) {
    const [isOpen, setIsOpen] = useState(false);
    const [query, setQuery] = useState("");
    const filteredCities = cities.filter(city => city.toLowerCase().includes(query.trim().toLowerCase()));

    const chooseCity = city => {
        setQuery("");
        setIsOpen(false);
        onSelect(city);
    };

    const handleKeyDown = event => {
        if (event.key === "Escape") {
            setIsOpen(false);
            setQuery("");
        } else if (event.key === "Enter" && isOpen && filteredCities.length > 0) {
            event.preventDefault();
            chooseCity(filteredCities[0]);
        } else if (event.key === "ArrowDown") {
            event.preventDefault();
            setIsOpen(true);
        }
    };

    return (
        <div className="relative w-full">
            <label htmlFor={id} className="sr-only">Search and select a city</label>
            <div className={`flex items-center gap-2 rounded-xl border bg-white ${compact ? "border-gray-200 px-3" : "border-gray-300 px-4 shadow-sm"} focus-within:border-rose-500 focus-within:ring-2 focus-within:ring-rose-100`}>
                <Search size={compact ? 16 : 18} className="shrink-0 text-gray-400" aria-hidden="true" />
                <input
                    id={id}
                    type="text"
                    role="combobox"
                    aria-autocomplete="list"
                    aria-expanded={isOpen}
                    aria-controls={`${id}-options`}
                    autoComplete="off"
                    value={isOpen ? query : value}
                    placeholder={isLoading ? "Loading cities..." : "Search your city"}
                    disabled={disabled || isLoading || cities.length === 0}
                    onFocus={() => {
                        setQuery("");
                        setIsOpen(true);
                    }}
                    onChange={event => {
                        setQuery(event.target.value);
                        setIsOpen(true);
                    }}
                    onKeyDown={handleKeyDown}
                    onBlur={() => {
                        setIsOpen(false);
                        setQuery("");
                    }}
                    className={`min-w-0 flex-1 bg-transparent outline-none placeholder:text-gray-400 ${compact ? "max-w-40 py-2 text-sm font-semibold text-gray-800" : "min-h-12 py-3 text-gray-800"}`}
                />
                {isOpen && filteredCities.length > 0 && (
                    <ul
                        id={`${id}-options`}
                        role="listbox"
                        className="absolute left-0 right-0 top-full z-40 mt-2 max-h-60 overflow-y-auto rounded-xl border border-gray-200 bg-white p-1.5 shadow-xl"
                    >
                        {filteredCities.map(city => (
                            <li key={city} role="option" aria-selected={city === value}>
                                <button
                                    type="button"
                                    onMouseDown={event => event.preventDefault()}
                                    onClick={() => chooseCity(city)}
                                    className={`w-full rounded-lg px-3 py-2.5 text-left text-sm transition hover:bg-rose-50 hover:text-rose-800 ${city === value ? "font-bold text-rose-700" : "text-gray-700"}`}
                                >
                                    {city}
                                </button>
                            </li>
                        ))}
                    </ul>
                )}
                {isOpen && query && filteredCities.length === 0 && (
                    <div className="absolute left-0 right-0 top-full z-40 mt-2 rounded-xl border border-gray-200 bg-white px-4 py-3 text-sm text-gray-500 shadow-xl">
                        No available city matches “{query}”.
                    </div>
                )}
            </div>
        </div>
    );
}

function CustomerHeader({cities, selectedCity, isLoadingCities, onCityChange, onChangeLocation}) {
    const {user, logout} = useAuth();
    const navigate = useNavigate();

    const handleSignOut = () => {
        logout();
        navigate("/", {replace: true});
    };

    return (
        <header className="sticky top-0 z-30 border-b border-gray-200/80 bg-white/95 backdrop-blur">
            <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-3 px-4 py-3 sm:px-6 lg:px-8">
                <Link to="/events" className="flex items-center gap-2 text-lg font-extrabold tracking-tight text-gray-950">
                    <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-rose-600 text-white">
                        <Ticket size={19} aria-hidden="true" />
                    </span>
                    ShowTime
                </Link>
                <div className="flex items-center gap-3">
                    {selectedCity && (
                        <div className="flex items-center gap-2">
                            <MapPin size={16} className="hidden shrink-0 text-rose-600 sm:block" aria-hidden="true" />
                            <CityPicker
                                id="header-city"
                                cities={cities}
                                value={selectedCity}
                                isLoading={isLoadingCities}
                                onSelect={onCityChange}
                                compact
                            />
                            <button
                                type="button"
                                onClick={onChangeLocation}
                                className="text-xs font-semibold text-gray-500 hover:text-rose-700"
                            >
                                Change
                            </button>
                        </div>
                    )}
                    {user
                        ? <button type="button" onClick={handleSignOut} className="text-sm font-medium text-gray-600 hover:text-gray-950">Sign out</button>
                        : <Link to="/login" className="text-sm font-semibold text-gray-700 hover:text-gray-950">Sign in</Link>}
                </div>
            </div>
        </header>
    );
}

function CityWelcome({cities, isLoadingCities, cityLoadError, onCitySelect, onRetry}) {
    const quickCities = cities
        .filter(city => CITY_LANDMARKS[city.toLowerCase()])
        .slice(0, 4);
    const displayedCities = quickCities.length > 0 ? quickCities : cities.slice(0, 4);

    return (
        <main className="min-h-[calc(100vh-61px)] overflow-hidden bg-[#fffaf7]">
            <section className="relative mx-auto grid max-w-7xl gap-10 px-4 py-12 sm:px-6 md:py-20 lg:grid-cols-[1.1fr_0.9fr] lg:items-center lg:px-8">
                <div className="relative z-10">
                    <div className="mb-5 inline-flex items-center gap-2 rounded-full bg-rose-100 px-3 py-1.5 text-sm font-semibold text-rose-800">
                        <Sparkles size={16} aria-hidden="true" />
                        Your next great night starts here
                    </div>
                    <h1 className="max-w-xl text-4xl font-black leading-tight tracking-tight text-gray-950 sm:text-5xl lg:text-6xl">
                        Big stories.<br />
                        <span className="text-rose-600">Closer to you.</span>
                    </h1>
                    <p className="mt-5 max-w-lg text-lg leading-7 text-gray-600">
                        Pick your city and discover movies, live shows, and the venues bringing them to life.
                    </p>

                    <div className="mt-8 max-w-md">
                        <label htmlFor="welcome-city" className="mb-2 block text-sm font-bold text-gray-800">
                            Where are you watching from?
                        </label>
                        <CityPicker
                            id="welcome-city"
                            cities={cities}
                            isLoading={isLoadingCities}
                            disabled={Boolean(cityLoadError)}
                            onSelect={onCitySelect}
                        />
                        {cityLoadError ? (
                            <div className="mt-3 text-sm text-red-700">
                                <p>{cityLoadError}</p>
                                <button type="button" onClick={onRetry} className="mt-1 font-semibold underline">Try again</button>
                            </div>
                        ) : !isLoadingCities && cities.length === 0 ? (
                            <p className="mt-2 text-sm text-gray-600">
                                No cities are available yet. Cities appear here after an active venue has been added to the platform.
                            </p>
                        ) : (
                            <p className="mt-2 text-xs text-gray-500">Start typing to search. You can change your city any time.</p>
                        )}
                    </div>
                </div>

                <div className="relative mx-auto grid w-full max-w-lg grid-cols-2 gap-3">
                    <div className="pointer-events-none absolute -left-10 -top-8 h-36 w-36 rounded-full bg-amber-200/60 blur-2xl" />
                    <div className="pointer-events-none absolute -bottom-8 -right-8 h-44 w-44 rounded-full bg-rose-200/70 blur-2xl" />
                    {displayedCities.map((city, index) => (
                        <div key={`${city}-${index}`} className={index % 2 === 1 ? "mt-8" : ""}>
                            <LandmarkTile city={city} onSelect={onCitySelect} />
                        </div>
                    ))}
                    {displayedCities.length === 0 && ["🏛️", "🌊", "✨", "🎭"].map((icon, index) => (
                        <div
                            key={icon}
                            className={`flex min-h-36 items-center justify-center rounded-2xl bg-gradient-to-br from-rose-100 to-amber-50 text-4xl ${index % 2 === 1 ? "mt-8" : ""}`}
                            aria-hidden="true"
                        >
                            {icon}
                        </div>
                    ))}
                    <div className="absolute left-1/2 top-1/2 flex h-16 w-16 -translate-x-1/2 -translate-y-1/2 items-center justify-center rounded-2xl border-4 border-[#fffaf7] bg-gray-950 text-rose-300 shadow-xl">
                        <Landmark size={27} aria-hidden="true" />
                    </div>
                </div>
            </section>
            <div className="mx-auto max-w-7xl px-4 pb-8 text-center text-xs text-gray-500 sm:px-6 lg:px-8">
                City illustrations are decorative. Events and showtimes depend on your selected location.
            </div>
        </main>
    );
}

function EventCard({event, city, onSelect}) {
    return (
        <button
            type="button"
            onClick={onSelect}
            className="group overflow-hidden rounded-2xl border border-gray-200 bg-white text-left shadow-sm transition hover:-translate-y-1 hover:shadow-xl"
        >
            {event.posterUrl
                ? <img src={getPosterSrc(event.posterUrl)} alt={`${event.title} poster`} className="h-72 w-full bg-gray-100 object-cover transition duration-300 group-hover:scale-[1.02]" />
                : <div className="flex h-72 items-center justify-center bg-gradient-to-br from-rose-100 to-amber-50 text-rose-500">
                    <Film size={54} strokeWidth={1.3} aria-label="No poster available" />
                </div>}
            <span className="block p-4">
                <span className="block truncate text-lg font-bold text-gray-950">{event.title}</span>
                <span className="mt-2 flex items-center justify-between gap-2 text-sm text-gray-600">
                    <span>View venues &amp; showtimes</span>
                    <span className="flex items-center gap-1 text-rose-700">
                        {city} <ArrowRight size={15} aria-hidden="true" />
                    </span>
                </span>
            </span>
        </button>
    );
}

function EventDetails({event, showings, city, onBack}) {
    const venues = groupShowingsByVenue(showings);

    return (
        <main className="mx-auto min-h-[calc(100vh-61px)] max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
            <button type="button" onClick={onBack} className="mb-5 inline-flex items-center gap-2 text-sm font-semibold text-gray-600 hover:text-gray-950">
                <ArrowLeft size={17} aria-hidden="true" /> All events in {city}
            </button>

            <section className="overflow-hidden rounded-3xl border border-gray-200 bg-white shadow-sm">
                <div className="grid lg:grid-cols-[320px_1fr]">
                    {event.posterUrl
                        ? <img src={getPosterSrc(event.posterUrl)} alt={`${event.title} poster`} className="h-80 w-full bg-gray-100 object-cover lg:h-full lg:min-h-[420px]" />
                        : <div className="flex h-80 items-center justify-center bg-gradient-to-br from-rose-100 to-amber-50 text-rose-500 lg:h-full lg:min-h-[420px]">
                            <Film size={72} strokeWidth={1.2} aria-label="No poster available" />
                        </div>}
                    <div className="p-6 sm:p-9">
                        <p className="flex items-center gap-2 text-sm font-semibold text-rose-700">
                            <MapPin size={16} aria-hidden="true" /> Showing in {city}
                        </p>
                        <h1 className="mt-3 text-3xl font-black tracking-tight text-gray-950 sm:text-4xl">{event.title}</h1>
                        {event.durationMinutes && (
                            <p className="mt-3 flex items-center gap-2 text-sm text-gray-600">
                                <Clock3 size={16} aria-hidden="true" /> {event.durationMinutes} minutes
                            </p>
                        )}
                        {event.description && <p className="mt-5 max-w-2xl whitespace-pre-line leading-7 text-gray-600">{event.description}</p>}
                        <div className="mt-8 flex flex-wrap items-center justify-between gap-3 border-t border-gray-100 pt-5">
                            <h2 className="text-xl font-bold text-gray-950">Choose a venue and showtime</h2>
                            <span className="text-sm text-gray-500">{venues.length} venue{venues.length === 1 ? "" : "s"}</span>
                        </div>

                        <div className="mt-4 space-y-4">
                            {venues.map(venue => (
                                <section key={venue.id} className="rounded-2xl border border-gray-200 p-4 sm:p-5">
                                    <h3 className="flex items-start gap-2 font-bold text-gray-900">
                                        <Landmark size={18} className="mt-0.5 shrink-0 text-rose-600" aria-hidden="true" />
                                        {venue.name}
                                    </h3>
                                    <div className="mt-3 flex flex-wrap gap-2">
                                        {venue.showings.map(showing => {
                                            const lowestPrice = showing.priceBands.length
                                                ? Math.min(...showing.priceBands.map(band => Number(band.price)))
                                                : null;
                                            return (
                                                <div key={showing.id} className="min-w-40 rounded-xl border border-gray-200 bg-gray-50 px-3 py-2.5">
                                                    <p className="font-semibold text-gray-800">{showing.venueSpaceName || "Venue"}</p>
                                                    <p className="mt-1 flex items-center gap-1.5 text-xs text-gray-600">
                                                        <CalendarDays size={13} aria-hidden="true" /> {formatDateTime(showing.startTime)}
                                                    </p>
                                                    {lowestPrice !== null && (
                                                        <p className="mt-1 text-xs font-medium text-gray-600">From {lowestPrice.toFixed(2)}</p>
                                                    )}
                                                    <button
                                                        type="button"
                                                        disabled
                                                        title="Ticket booking will be available after seat selection and checkout are implemented."
                                                        className="mt-3 inline-flex w-full items-center justify-center gap-2 rounded-lg bg-gray-300 px-3 py-2 text-sm font-bold text-gray-600"
                                                    >
                                                        <Ticket size={15} aria-hidden="true" /> Booking coming soon
                                                    </button>
                                                </div>
                                            );
                                        })}
                                    </div>
                                </section>
                            ))}
                        </div>
                    </div>
                </div>
            </section>
        </main>
    );
}

export default function EventDiscovery() {
    const {eventId} = useParams();
    const navigate = useNavigate();
    const [cities, setCities] = useState([]);
    const [selectedCity, setSelectedCity] = useState(() => window.localStorage.getItem("selectedCity") || "");
    const [events, setEvents] = useState([]);
    const [isLoadingCities, setIsLoadingCities] = useState(true);
    const [cityLoadError, setCityLoadError] = useState("");
    const [isLoadingEvents, setIsLoadingEvents] = useState(false);
    const [eventsLoadError, setEventsLoadError] = useState("");
    const [eventsRetry, setEventsRetry] = useState(0);
    const [showings, setShowings] = useState([]);
    const [isLoadingShowings, setIsLoadingShowings] = useState(false);
    const [showingsLoadError, setShowingsLoadError] = useState("");
    const [showingsRetry, setShowingsRetry] = useState(0);

    const fetchCities = () => {
        setCityLoadError("");
        setIsLoadingCities(true);
        api.get("/events/cities")
            .then(({data}) => {
                const availableCities = normalizeCities(data);
                setCities(availableCities);
                const savedCity = window.localStorage.getItem("selectedCity");
                if (savedCity && !availableCities.some(city => city.toLowerCase() === savedCity.toLowerCase())) {
                    window.localStorage.removeItem("selectedCity");
                    setSelectedCity("");
                }
            })
            .catch(error => {
                const message = getApiErrorMessage(error, "Could not load available cities.");
                setCityLoadError(message);
                showToast(message);
            })
            .finally(() => setIsLoadingCities(false));
    };

    useEffect(() => {
        let isActive = true;
        api.get("/events/cities")
            .then(({data}) => {
                if (!isActive) return;
                const availableCities = normalizeCities(data);
                setCities(availableCities);
                const savedCity = window.localStorage.getItem("selectedCity");
                if (savedCity && !availableCities.some(city => city.toLowerCase() === savedCity.toLowerCase())) {
                    window.localStorage.removeItem("selectedCity");
                    setSelectedCity("");
                }
            })
            .catch(error => {
                if (isActive) {
                    const message = getApiErrorMessage(error, "Could not load available cities.");
                    setCityLoadError(message);
                    showToast(message);
                }
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
                setEventsLoadError("");
                return api.get("/events", {params: {city: selectedCity}});
            })
            .then(response => {
                if (isActive && response) setEvents(response.data);
            })
            .catch(error => {
                if (isActive) {
                    const message = getApiErrorMessage(error, "Could not load events for this location.");
                    setEventsLoadError(message);
                    showToast(message);
                }
            })
            .finally(() => {
                if (isActive) setIsLoadingEvents(false);
            });
        return () => { isActive = false; };
    }, [selectedCity, eventsRetry]);

    useEffect(() => {
        if (!eventId || !selectedCity) return undefined;
        let isActive = true;
        Promise.resolve()
            .then(() => {
                if (!isActive) return null;
                setIsLoadingShowings(true);
                setShowingsLoadError("");
                setShowings([]);
                return api.get(`/events/${eventId}/showings`, {params: {city: selectedCity}});
            })
            .then(response => {
                if (isActive && response) setShowings(response.data);
            })
            .catch(error => {
                if (isActive) {
                    const message = getApiErrorMessage(error, "Could not load venues and showtimes.");
                    setShowingsLoadError(message);
                    showToast(message);
                }
            })
            .finally(() => {
                if (isActive) setIsLoadingShowings(false);
            });
        return () => { isActive = false; };
    }, [eventId, selectedCity, showingsRetry]);

    const changeCity = city => {
        if (!city) return;
        window.localStorage.setItem("selectedCity", city);
        setEvents([]);
        setEventsLoadError("");
        setShowings([]);
        setShowingsLoadError("");
        setSelectedCity(city);
        navigate("/events", {replace: Boolean(eventId)});
    };

    const returnToCitySelection = () => {
        window.localStorage.removeItem("selectedCity");
        setSelectedCity("");
        setEvents([]);
        setEventsLoadError("");
        setShowings([]);
        setShowingsLoadError("");
        navigate("/", {replace: true});
    };

    const selectedEvent = eventId ? events.find(event => String(event.id) === eventId) : null;

    return (
        <div className="min-h-screen bg-[#fafafa]">
            <CustomerHeader
                cities={cities}
                selectedCity={selectedCity}
                isLoadingCities={isLoadingCities}
                onCityChange={changeCity}
                onChangeLocation={returnToCitySelection}
            />

            {!selectedCity ? (
                <CityWelcome
                    cities={cities}
                    isLoadingCities={isLoadingCities}
                    cityLoadError={cityLoadError}
                    onCitySelect={changeCity}
                    onRetry={fetchCities}
                />
            ) : eventId ? (
                isLoadingEvents || isLoadingShowings ? (
                    <p className="py-24 text-center text-gray-500">Loading event details...</p>
                ) : selectedEvent ? (
                    showingsLoadError ? (
                        <main className="mx-auto max-w-7xl px-4 py-16 text-center sm:px-6">
                            <h1 className="text-2xl font-bold text-gray-900">Could not load venues for {selectedEvent.title}</h1>
                            <p className="mt-2 text-sm text-gray-600">{showingsLoadError}</p>
                            <button
                                type="button"
                                onClick={() => setShowingsRetry(retry => retry + 1)}
                                className="mt-4 rounded-lg bg-rose-700 px-4 py-2 text-sm font-bold text-white hover:bg-rose-600"
                            >
                                Try again
                            </button>
                        </main>
                    ) : (
                        <EventDetails event={selectedEvent} showings={showings} city={selectedCity} onBack={() => navigate("/events")} />
                    )
                ) : (
                    <main className="mx-auto max-w-7xl px-4 py-16 text-center sm:px-6">
                        <h1 className="text-2xl font-bold text-gray-900">Event not found in {selectedCity}</h1>
                        <button type="button" onClick={() => navigate("/events")} className="mt-4 font-semibold text-rose-700 hover:underline">
                            Browse city events
                        </button>
                    </main>
                )
            ) : (
                <main className="mx-auto min-h-[calc(100vh-61px)] max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
                    {cityLoadError && (
                        <div role="alert" className="mb-6 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
                            <span>Could not verify available cities. {cityLoadError}</span>
                            <button type="button" onClick={fetchCities} className="font-bold underline">Retry</button>
                        </div>
                    )}
                    <section className="mb-8 flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
                        <div>
                            <p className="flex items-center gap-2 text-sm font-bold uppercase tracking-wider text-rose-700">
                                <MapPin size={16} aria-hidden="true" /> Around {selectedCity}
                            </p>
                            <h1 className="mt-2 text-3xl font-black tracking-tight text-gray-950 sm:text-4xl">
                                Find your next show
                            </h1>
                            <p className="mt-2 text-gray-600">The events and venues making plans happen in your city.</p>
                        </div>
                        <div className="hidden items-center gap-2 rounded-full bg-amber-100 px-4 py-2 text-sm font-semibold text-amber-900 sm:flex">
                            <Sparkles size={16} aria-hidden="true" /> Make it a memorable night
                        </div>
                    </section>

                    {isLoadingEvents ? (
                        <p className="py-16 text-center text-gray-500">Finding events in {selectedCity}...</p>
                    ) : eventsLoadError ? (
                        <div role="alert" className="rounded-2xl border border-red-200 bg-red-50 px-6 py-12 text-center">
                            <h2 className="text-lg font-bold text-red-900">Could not load events for {selectedCity}</h2>
                            <p className="mt-2 text-sm text-red-800">{eventsLoadError}</p>
                            <button
                                type="button"
                                onClick={() => setEventsRetry(retry => retry + 1)}
                                className="mt-4 rounded-lg bg-red-800 px-4 py-2 text-sm font-bold text-white hover:bg-red-700"
                            >
                                Try again
                            </button>
                        </div>
                    ) : events.length === 0 ? (
                        <div className="rounded-2xl border border-dashed border-gray-300 bg-white px-6 py-16 text-center">
                            <Film size={42} className="mx-auto text-gray-300" aria-hidden="true" />
                            <h2 className="mt-4 text-lg font-bold text-gray-900">Nothing on the marquee just yet</h2>
                            <p className="mt-1 text-sm text-gray-600">Check back soon for events in {selectedCity}, or choose another city above.</p>
                        </div>
                    ) : (
                        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
                            {events.map(event => (
                                <EventCard
                                    key={event.id}
                                    event={event}
                                    city={selectedCity}
                                    onSelect={() => navigate(`/events/${event.id}`)}
                                />
                            ))}
                        </div>
                    )}
                </main>
            )}
        </div>
    );
}
