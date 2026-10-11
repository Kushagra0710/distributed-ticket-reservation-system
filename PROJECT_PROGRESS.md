# Distributed Ticket Reservation System — Progress Handoff

This document summarizes the implemented application structure and current scope. Use it as context when continuing development with Gemini or another assistant.

## Product overview

A ticket-reservation platform with separate event-catalog and venue-showing data:

- An **event catalog entry** is shared metadata for a movie or event, created once by an admin.
- A **showing** is a scheduled instance of a catalog event at a venue space, with its own start/end time and prices.
- A **venue** represents a theatre, auditorium, or concert ground; it can contain multiple screens, halls, or stages.
- **Admins** manage platform-wide data. **Owners** manage only venues assigned to them. Customers can browse public event listings by city.

## Implemented functionality

### Authentication and roles

- JWT-based authentication with `ADMIN`, `OWNER`, and `USER` roles.
- Admins can create and manage admins and venue-owner accounts.
- Owner account passwords are generated randomly on the backend and hashed before storage. A password delivery/reset workflow is not implemented yet.
- Backend authorization and service-level ownership checks restrict owners to their assigned venues and showings; admin access is platform-wide.

### Venue and space management

- Admins create and edit venues, assign an active owner, and manage venue status.
- Pincode lookup fills the venue city and state.
- Owners see and manage only their assigned venues.
- Venue spaces are managed separately from venues. A space can represent a screen, hall, or stage and use either reserved seating (rows and seats per row) or general admission (capacity).
- Reserved seating is represented by layout dimensions, not individual persisted seat records.

### Event catalog and showings

- Admins create and edit shared catalog entries with title, description, optional duration, and a poster image.
- Poster images are uploaded as JPEG or PNG, limited to 5 MB, and stored in a separate PostgreSQL table from catalog metadata. The public image endpoint serves the uploaded bytes.
- Admins can permanently delete a catalog event only if no showing record references it. The backend enforces this check; if a showing exists, deletion is rejected.
- Admins and owners create showings by selecting a catalog event, venue, space, schedule, and price bands.
- Schedule validation rejects invalid times, existing conflicts, and overlaps within a submitted batch.
- Price bands belong to a showing. Reserved seating bands must cover every row exactly once; general-admission spaces use a single price band.
- Existing legacy event rows are retained and linked to catalog entries through a startup backfill where possible.

### Customer event discovery

- Public pages at `/` and `/events` begin with a welcoming city-selection screen with decorative city/landmark tiles.
- City choices are derived from active venues. The city picker supports typing/filtering, distinguishes an empty venue list from an API failure, and the selected city is stored in browser local storage and can be changed from the customer header.
- Saved city selections that no longer match the active-venue city list are cleared, returning the customer to city selection. Customers can explicitly return to city selection from the header.
- City and event API failures are shown with retry actions rather than being presented as an empty event list.
- Selecting a city opens a sidebar-free event dashboard.
- Event cards navigate to `/events/{eventId}`, which shows event details and venue/space showtimes with starting prices.
- Public discovery endpoints provide city choices, event catalog listings, per-event venue/showtime listings, and poster images.
- Seat selection, booking, and checkout are not implemented; the event detail view marks booking as coming soon.

### Shared application behavior

- A compact global toast-notification system is used to surface success and API errors.
- The API returns standard error responses for validation, access-denied, and application errors.

## Main data relationships

```text
User (OWNER) 1 ─── * Venue
Venue        1 ─── * VenueSpace
EventCatalog 1 ─── * Event (showing)
Venue        1 ─── * Event (showing)
VenueSpace   1 ─── * Event (showing)
Event        1 ─── * EventPriceBand
EventCatalog 1 ─── 0..1 EventCatalogPoster
```

`Event` is the existing schedule/showing entity and keeps legacy fields for compatibility. The catalog link is optional for legacy data. Catalog posters are stored separately so listing catalog metadata does not load image bytes.

## Important API routes

| Purpose | Route |
|---|---|
| Admin event catalog list/create/update/delete | `/api/admin/events` |
| Admin and owner showing list/create | `/api/admin/showings`, `/api/owner/showings` |
| Admin/owner venue and space management | `/api/admin/venue/**` |
| Admin user/owner management | `/api/admin/users/**` |
| Public cities and event discovery | `/api/events/cities`, `/api/events?city=...` |
| Public showings for an event in a city | `/api/events/{id}/showings?city=...` |
| Public uploaded event poster | `/api/events/{id}/poster` |
| Authentication | `/api/auth/**` |

Catalog create/update uses `multipart/form-data`: an `event` JSON part and an optional `poster` file part. Customer event cards load catalog metadata first; venue showings and price bands are fetched only after opening an event.

## Technology and local configuration

- Backend: Spring Boot 3.3.4, Java 17, Spring Security/JWT, Spring Data JPA, PostgreSQL.
- Frontend: React, Vite, React Router, Axios, and Tailwind CSS.
- Hibernate uses `ddl-auto=update` in the current configuration.
- The frontend API base URL is configured with `VITE_API_BASE_URL`; local development currently points to the backend on port 8080.
- Do not copy credentials or secrets from local configuration files into documentation or source control.

## Not implemented yet / follow-up areas

- Actual seat inventory, seat locking, seat selection, reservations, ticket issuance, and payment processing.
- Secure delivery of generated owner passwords and owner password reset/change flows.
- External object storage/CDN for posters. Posters currently live in PostgreSQL for simplicity; this is suitable for the current scope but may be less efficient as image volume grows.
- Automated schema migrations. The current Hibernate `update` setting is convenient for development but should be replaced with a migration strategy before production.
- Account disablement does not necessarily revoke already-issued JWTs before they expire.

## Verification at last implementation update

- Backend test suite: 20 tests passed, including execution of the public event-list and per-event-showings discovery queries.
- Frontend production build passed.
- Targeted ESLint checks for the event-catalog and customer-discovery screens passed.

When making follow-up changes, inspect the current worktree first and preserve unrelated local changes.
