# Booking Systems — LLD Section

Inventory + time. The hard part is **not double-selling** the same seat/room/car.

| Problem | Core skill |
|---------|------------|
| [Movie Ticket Booking](./Movie-Ticket-Booking/) | Seat lock TTL, payment confirm |
| [Hotel Booking](./Hotel-Booking/) | Date-range overlap |
| [Cab Booking](./Cab-Booking/) | Matching + trip lifecycle |
| [Car Rental](./Car-Rental/) | Vehicle inventory + days pricing |

**Overlap rule (memorize):**  
`newStart < existingEnd && newEnd > existingStart` (half-open intervals recommended).
