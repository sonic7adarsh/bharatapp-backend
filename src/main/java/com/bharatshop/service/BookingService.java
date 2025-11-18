package com.bharatshop.service;

import com.bharatshop.domain.Order;
import com.bharatshop.domain.BookingDetails;
import com.bharatshop.entity.BookingEntity;
import com.bharatshop.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingService {
    private final BookingRepository bookingRepository;
    @org.springframework.beans.factory.annotation.Value("${app.orders.acceptanceWindowMinutes:15}")
    private int acceptanceWindowMinutes;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public Order placeBooking(String userId, BookingDetails booking, Order.Totals totals, String paymentMethod, Order.PaymentInfo paymentInfo, String storeId, String notes) {
        String id = UUID.randomUUID().toString();
        BookingEntity e = new BookingEntity();
        e.setId(id);
        e.setReference("BK-" + id.substring(0, 8));
        e.setUserId(userId);
        e.setStatus("placed");
        e.setPaymentMethod(paymentMethod);
        e.setCreatedAt(Instant.now());
        e.setStoreId(storeId);
        e.setNotes(notes);
        if (acceptanceWindowMinutes > 0) {
            e.setSellerResponseDeadline(Instant.now().plusSeconds(acceptanceWindowMinutes * 60L));
        }

        Double total = totals != null && totals.payable != null ? totals.payable : null;
        e.setTotal(total);

        if (booking != null) {
            e.setRoomId(booking.getRoomId());
            e.setCheckIn(booking.getCheckIn());
            e.setCheckOut(booking.getCheckOut());
            e.setGuests(booking.getGuests());
            e.setNights(booking.getNights());
            e.setRooms(booking.getRooms());
            e.setPerRoomMax(booking.getPerRoomMax());
            e.setExtraMattressAllowed(booking.getExtraMattressAllowed());
            e.setExtraMattressCount(booking.getExtraMattressCount());
            e.setMattressFeePerNight(booking.getMattressFeePerNight());
        }

        bookingRepository.save(e);
        Order dto = toOrderDto(e);
        dto.setBooking(booking);
        dto.setTotals(totals);
        dto.setPaymentInfo(paymentInfo);
        dto.setType("room_booking");
        return dto;
    }

    public List<Order> listBookings(String userId) {
        List<BookingEntity> entities = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Instant now = Instant.now();
        for (BookingEntity e : entities) {
            if ("placed".equalsIgnoreCase(e.getStatus()) && e.getSellerAcceptedAt() == null && e.getSellerResponseDeadline() != null && now.isAfter(e.getSellerResponseDeadline())) {
                e.setStatus("cancelled");
                e.setCancelledAt(now);
                if (e.getCancellationReason() == null || e.getCancellationReason().isBlank()) {
                    e.setCancellationReason("auto_cancelled_no_response");
                }
                bookingRepository.save(e);
            }
        }
        return entities.stream().map(this::toOrderDto).collect(Collectors.toList());
    }

    public Order updateStatus(String bookingId, String status, String notes) {
        return bookingRepository.findById(bookingId)
                .map(entity -> {
                    entity.setStatus(status);
                    if (notes != null && !notes.isBlank()) entity.setNotes(notes);
                    bookingRepository.save(entity);
                    return toOrderDto(entity);
                })
                .orElse(null);
    }

    private Order toOrderDto(BookingEntity e) {
        Order o = new Order();
        o.setId(e.getId());
        o.setReference(e.getReference());
        o.setStatus(e.getStatus());
        o.setTotal(e.getTotal());
        o.setPaymentMethod(e.getPaymentMethod());
        o.setType("room_booking");
        o.setCreatedAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        o.setSellerResponseDeadline(e.getSellerResponseDeadline() != null ? e.getSellerResponseDeadline().toString() : null);
        o.setSellerAcceptedAt(e.getSellerAcceptedAt() != null ? e.getSellerAcceptedAt().toString() : null);
        o.setCancelledAt(e.getCancelledAt() != null ? e.getCancelledAt().toString() : null);
        o.setCancellationReason(e.getCancellationReason());
        o.setStoreId(e.getStoreId());
        o.setNotes(e.getNotes());
        // Booking details
        BookingDetails b = new BookingDetails();
        b.setRoomId(e.getRoomId());
        b.setCheckIn(e.getCheckIn());
        b.setCheckOut(e.getCheckOut());
        b.setGuests(e.getGuests() != null ? e.getGuests() : 0);
        b.setNights(e.getNights() != null ? e.getNights() : 0);
        b.setRooms(e.getRooms());
        b.setPerRoomMax(e.getPerRoomMax());
        b.setExtraMattressAllowed(e.getExtraMattressAllowed());
        b.setExtraMattressCount(e.getExtraMattressCount());
        b.setMattressFeePerNight(e.getMattressFeePerNight());
        o.setBooking(b);
        // Totals fallback
        Order.Totals t = new Order.Totals();
        t.payable = e.getTotal();
        o.setTotals(t);
        return o;
    }
}