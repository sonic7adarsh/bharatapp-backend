package com.bharatshop.factory.impl;

import com.bharatshop.domain.Order;
import com.bharatshop.domain.Product;
import com.bharatshop.domain.Store;
import com.bharatshop.factory.SellerFactory;
import com.bharatshop.factory.ops.SellerAnnouncementOps;
import com.bharatshop.factory.ops.SellerAnalyticsOps;
import com.bharatshop.factory.ops.SellerBookingOps;
import com.bharatshop.factory.ops.SellerOrderOps;
import com.bharatshop.factory.ops.SellerProductOps;
import com.bharatshop.factory.ops.SellerPayoutOps;
import com.bharatshop.factory.ops.SellerStoreOps;
import com.bharatshop.repository.OrderItemRepository;
import com.bharatshop.repository.OrderRepository;
import com.bharatshop.repository.BookingRepository;
import com.bharatshop.repository.ProductRepository;
import com.bharatshop.repository.StoreRepository;
import com.bharatshop.service.OrderService;
import com.bharatshop.service.ProductService;
import com.bharatshop.service.StoreService;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Component
public class DefaultSellerFactory implements SellerFactory {

    private final ProductService productService;
    private final ProductRepository productRepository;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BookingRepository bookingRepository;
    private final StoreService storeService;
    private final StoreRepository storeRepository;

    private final List<Map<String, Object>> payoutStubs = new CopyOnWriteArrayList<>();
    private Map<String, Object> payoutConfig = new HashMap<>();

    public DefaultSellerFactory(ProductService productService,
                                ProductRepository productRepository,
                                OrderService orderService,
                                OrderRepository orderRepository,
                                OrderItemRepository orderItemRepository,
                                StoreService storeService,
                                StoreRepository storeRepository,
                                BookingRepository bookingRepository) {
        this.productService = productService;
        this.productRepository = productRepository;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.storeService = storeService;
        this.storeRepository = storeRepository;
        this.bookingRepository = bookingRepository;
        this.payoutConfig = new HashMap<>();
    }

    @Override
    public SellerProductOps products() {
        return new SellerProductOps() {
            @Override
            public List<Product> listByStore(String storeId, String search, String category, Boolean active, Integer page, Integer limit) {
                List<Product> all = productService.getByStore(storeId);
                if (search != null && !search.isBlank()) {
                    String s = search.toLowerCase();
                    all = all.stream().filter(p -> p.getName() != null && p.getName().toLowerCase().contains(s)).collect(Collectors.toList());
                }
                if (category != null && !category.isBlank()) {
                    String c = category.toLowerCase();
                    all = all.stream().filter(p -> p.getCategory() != null && p.getCategory().toLowerCase().equals(c)).collect(Collectors.toList());
                }
                if (active != null) {
                    all = all.stream().filter(p -> Objects.equals(p.getActive(), active)).collect(Collectors.toList());
                }
                if (limit != null && limit > 0) {
                    int p = page == null || page < 1 ? 1 : page;
                    int from = (p - 1) * limit;
                    if (from < all.size()) all = all.subList(from, Math.min(from + limit, all.size()));
                    else all = Collections.emptyList();
                }
                return all;
            }

            @Override
            public Product create(String storeId, Product product) {
                product.setStoreId(storeId);
                productService.add(product);
                return product;
            }

            @Override
            public Product updatePartial(String id, Map<String, Object> changes) {
                return productService.updatePartial(id, changes);
            }

            @Override
            public boolean delete(String id, boolean archive) {
                if (archive) {
                    Map<String, Object> change = new HashMap<>();
                    change.put("active", false);
                    productService.updatePartial(id, change);
                    return true;
                }
                return productRepository.findById(id).map(entity -> {
                    productRepository.delete(entity);
                    return true;
                }).orElse(false);
            }

            @Override
            public Product updateImage(String id, String filename) {
                return productService.updateImage(id, filename);
            }

            @Override
            public Product adjustInventory(String id, Integer stockDelta, Integer stockSet, Double price) {
                return productService.adjustInventory(id, stockDelta, stockSet, price);
            }
        };
    }

    @Override
    public SellerOrderOps orders() {
        return new SellerOrderOps() {
            @Override
            public List<Order> list(String storeId, String status, String from, String to, Integer page, Integer limit) {
                List<com.bharatshop.entity.OrderEntity> entities = orderRepository.findAll();
                // Auto-cancel expired placed orders
                Instant now = Instant.now();
                for (com.bharatshop.entity.OrderEntity e : entities) {
                    if ("placed".equalsIgnoreCase(e.getStatus()) && e.getSellerAcceptedAt() == null && e.getSellerResponseDeadline() != null && now.isAfter(e.getSellerResponseDeadline())) {
                        e.setStatus("cancelled");
                        e.setCancelledAt(now);
                        if (e.getCancellationReason() == null || e.getCancellationReason().isBlank()) {
                            e.setCancellationReason("auto_cancelled_no_response");
                        }
                        orderRepository.save(e);
                    }
                }
                Instant fromTs = parseDayStart(from);
                Instant toTs = parseDayStart(to);
                if (status != null && !status.isBlank()) {
                    String s = status.toLowerCase();
                    entities = entities.stream().filter(o -> o.getStatus() != null && o.getStatus().equalsIgnoreCase(s)).collect(Collectors.toList());
                }
                if (fromTs != null) {
                    Instant f = fromTs;
                    entities = entities.stream().filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().isBefore(f)).collect(Collectors.toList());
                }
                if (toTs != null) {
                    Instant t = toTs;
                    entities = entities.stream().filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().isAfter(t)).collect(Collectors.toList());
                }
                entities.sort(Comparator.comparing(com.bharatshop.entity.OrderEntity::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
                List<Order> dto = entities.stream().map(DefaultSellerFactory.this::toDto).collect(Collectors.toList());
                if (limit != null && limit > 0) {
                    int p = page == null || page < 1 ? 1 : page;
                    int fromIdx = (p - 1) * limit;
                    if (fromIdx < dto.size()) dto = dto.subList(fromIdx, Math.min(fromIdx + limit, dto.size()));
                    else dto = Collections.emptyList();
                }
                return dto;
            }

            @Override
            public Order get(String orderId) {
                return orderRepository.findById(orderId)
                        .map(e -> {
                            Instant now = Instant.now();
                            if ("placed".equalsIgnoreCase(e.getStatus()) && e.getSellerAcceptedAt() == null && e.getSellerResponseDeadline() != null && now.isAfter(e.getSellerResponseDeadline())) {
                                e.setStatus("cancelled");
                                e.setCancelledAt(now);
                                if (e.getCancellationReason() == null || e.getCancellationReason().isBlank()) {
                                    e.setCancellationReason("auto_cancelled_no_response");
                                }
                                orderRepository.save(e);
                            }
                            return DefaultSellerFactory.this.toDto(e);
                        })
                        .orElse(null);
            }

            @Override
            public Order updateStatus(String orderId, String status, String notes) {
                return orderRepository.findById(orderId).map(entity -> {
                    entity.setStatus(status);
                    orderRepository.save(entity);
                    return DefaultSellerFactory.this.toDto(entity);
                }).orElse(null);
            }

            @Override
            public Map<String, Object> refund(String orderId, Double amount, String reason) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("orderId", orderId);
                resp.put("amount", amount);
                resp.put("reason", reason);
                resp.put("status", "initiated");
                return resp;
            }
        };
    }

    @Override
    public SellerBookingOps bookings() {
        return new SellerBookingOps() {
            @Override
            public List<Order> listBookings(String storeId, String status, String from, String to) {
                List<com.bharatshop.entity.BookingEntity> entities = bookingRepository.findAll();
                Instant fromTs = parseDayStart(from);
                Instant toTs = parseDayStart(to);
                if (storeId != null && !storeId.isBlank()) {
                    String sId = storeId;
                    entities = entities.stream().filter(b -> sId.equals(b.getStoreId())).collect(Collectors.toList());
                }
                if (status != null && !status.isBlank()) {
                    String s = status.toLowerCase();
                    entities = entities.stream().filter(b -> b.getStatus() != null && b.getStatus().equalsIgnoreCase(s)).collect(Collectors.toList());
                }
                if (fromTs != null) {
                    Instant f = fromTs;
                    entities = entities.stream().filter(b -> b.getCreatedAt() != null && !b.getCreatedAt().isBefore(f)).collect(Collectors.toList());
                }
                if (toTs != null) {
                    Instant t = toTs;
                    entities = entities.stream().filter(b -> b.getCreatedAt() != null && !b.getCreatedAt().isAfter(t)).collect(Collectors.toList());
                }
                entities.sort(Comparator.comparing(com.bharatshop.entity.BookingEntity::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
                return entities.stream().map(DefaultSellerFactory.this::toDtoFromBooking).collect(Collectors.toList());
            }

            @Override
            public Order updateStatus(String bookingId, String status, String notes) {
                return bookingRepository.findById(bookingId).map(entity -> {
                    entity.setStatus(status);
                    if (notes != null && !notes.isBlank()) entity.setNotes(notes);
                    bookingRepository.save(entity);
                    return DefaultSellerFactory.this.toDtoFromBooking(entity);
                }).orElse(null);
            }
        };
    }

    @Override
    public SellerAnalyticsOps analytics() {
        return new SellerAnalyticsOps() {
            @Override
            public Map<String, Object> overview(String storeId, String from, String to) {
                Instant fromTs = parseDayStart(from);
                Instant toTs = parseDayStart(to);
                List<com.bharatshop.entity.OrderEntity> entities = orderRepository.findAll();
                if (fromTs != null) {
                    Instant f = fromTs;
                    entities = entities.stream().filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().isBefore(f)).collect(Collectors.toList());
                }
                if (toTs != null) {
                    Instant t = toTs;
                    entities = entities.stream().filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().isAfter(t)).collect(Collectors.toList());
                }

                int ordersCount = entities.size();
                double revenue = entities.stream().map(com.bharatshop.entity.OrderEntity::getTotal).filter(Objects::nonNull).mapToDouble(Double::doubleValue).sum();

                Map<String, Integer> productCounts = new HashMap<>();
                for (com.bharatshop.entity.OrderEntity e : entities) {
                    for (com.bharatshop.entity.OrderItemEntity oi : orderItemRepository.findByOrderId(e.getId())) {
                        productCounts.merge(oi.getName(), oi.getQuantity(), Integer::sum);
                    }
                }
                List<Map<String, Object>> topProducts = productCounts.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .limit(5)
                        .map(en -> {
                            Map<String, Object> m = new LinkedHashMap<>();
                            m.put("name", en.getKey());
                            m.put("count", en.getValue());
                            return m;
                        }).collect(Collectors.toList());

                Map<String, Object> overview = new LinkedHashMap<>();
                overview.put("orders", ordersCount);
                overview.put("revenue", revenue);
                overview.put("topProducts", topProducts);
                return overview;
            }
        };
    }

    @Override
    public SellerStoreOps stores() {
        return new SellerStoreOps() {
            @Override
            public List<Store> list(String search, Integer page, Integer limit) {
                var principal = com.bharatshop.security.UserPrincipal.current();
                if (principal == null) return java.util.Collections.emptyList();
                String ownerId = principal.getUserId();
                List<com.bharatshop.entity.StoreEntity> owned = storeRepository.findByOwnerId(ownerId);
                if (search != null && !search.isBlank()) {
                    String q = search.trim().toLowerCase();
                    owned = owned.stream().filter(e -> e.getName() != null && e.getName().toLowerCase().contains(q)).collect(Collectors.toList());
                }
                List<Store> all = owned.stream().map(e -> {
                    Store s = new Store(e.getId(), e.getName(), e.getArea(), e.getCategory());
                    s.setOwnerId(e.getOwnerId());
                    s.setOwnerPhone(e.getOwnerPhone());
                    s.setStatus(e.getStatus());
                    s.setOrderingDisabled(e.getOrderingDisabled());
                    s.setClosedReason(e.getClosedReason());
                    s.setClosedUntil(e.getClosedUntil());
                    s.setLogo(e.getLogo());
                    s.setUpdatedAt(e.getUpdatedAt());
                    return s;
                }).sorted(Comparator.comparing(Store::getName)).collect(Collectors.toList());
                if (limit != null && limit > 0) {
                    int p = page == null || page < 1 ? 1 : page;
                    int from = (p - 1) * limit;
                    if (from < all.size()) all = all.subList(from, Math.min(from + limit, all.size()));
                    else all = java.util.Collections.emptyList();
                }
                return all;
            }

            @Override
            public Store get(String id) {
                return storeService.get(id);
            }

            @Override
            public Store create(Store store) {
                if (store.getId() == null || store.getId().isBlank()) {
                    store.setId(java.util.UUID.randomUUID().toString());
                }
                return storeService.add(store);
            }

            @Override
            public Store updatePartial(String id, java.util.Map<String, Object> changes) {
                return storeRepository.findById(id).map(entity -> {
                    if (changes.containsKey("name")) entity.setName((String) changes.get("name"));
                    if (changes.containsKey("area")) entity.setArea((String) changes.get("area"));
                    if (changes.containsKey("city")) entity.setArea((String) changes.get("city"));
                    if (changes.containsKey("category")) entity.setCategory((String) changes.get("category"));
                    if (changes.containsKey("status")) entity.setStatus((String) changes.get("status"));
                    if (changes.containsKey("orderingDisabled")) {
                        Object v = changes.get("orderingDisabled");
                        if (v instanceof Boolean) entity.setOrderingDisabled((Boolean) v);
                        else if (v instanceof String) entity.setOrderingDisabled(Boolean.parseBoolean((String) v));
                    }
                    if (changes.containsKey("closedReason")) entity.setClosedReason((String) changes.get("closedReason"));
                    if (changes.containsKey("closedUntil")) {
                        Object cu = changes.get("closedUntil");
                        if (cu instanceof String && !((String) cu).isBlank()) {
                            try { entity.setClosedUntil(Instant.parse((String) cu)); } catch (Exception ignored) {}
                        } else if (cu instanceof java.time.Instant) {
                            entity.setClosedUntil((java.time.Instant) cu);
                        }
                    }
                    if (changes.containsKey("logo")) entity.setLogo((String) changes.get("logo"));
                    entity.setUpdatedAt(Instant.now());
                    storeRepository.save(entity);
                    Store s = new Store(entity.getId(), entity.getName(), entity.getArea(), entity.getCategory());
                    s.setOwnerId(entity.getOwnerId());
                    s.setOwnerPhone(entity.getOwnerPhone());
                    s.setStatus(entity.getStatus());
                    s.setOrderingDisabled(entity.getOrderingDisabled());
                    s.setClosedReason(entity.getClosedReason());
                    s.setClosedUntil(entity.getClosedUntil());
                    s.setLogo(entity.getLogo());
                    s.setUpdatedAt(entity.getUpdatedAt());
                    return s;
                }).orElse(null);
            }
        };
    }

    @Override
    public SellerPayoutOps payouts() {
        return new SellerPayoutOps() {
            @Override
            public List<Map<String, Object>> list() {
                return payoutStubs;
            }

            @Override
            public Map<String, Object> request(double amount) {
                Map<String, Object> req = new HashMap<>();
                req.put("id", UUID.randomUUID().toString());
                req.put("amount", amount);
                req.put("status", "pending");
                req.put("requestedAt", Instant.now().toString());
                payoutStubs.add(req);
                return req;
            }

            @Override
            public Map<String, Object> getConfig() {
                return payoutConfig;
            }

            @Override
            public Map<String, Object> updateConfig(Map<String, Object> body) {
                payoutConfig.putAll(body);
                return payoutConfig;
            }
        };
    }

    @Override
    public SellerAnnouncementOps announcements() {
        return new SellerAnnouncementOps() {
            private final List<Map<String, Object>> announcements = new CopyOnWriteArrayList<>();

            @Override
            public Map<String, Object> post(String storeId, String message, String activeUntil) {
                Map<String, Object> ann = new HashMap<>();
                ann.put("id", UUID.randomUUID().toString());
                ann.put("storeId", storeId);
                ann.put("message", message);
                ann.put("activeUntil", activeUntil);
                ann.put("createdAt", Instant.now().toString());
                announcements.add(ann);
                return ann;
            }
        };
    }

    private Instant parseDayStart(String d) {
        if (d == null || d.isBlank()) return null;
        try { return java.time.LocalDate.parse(d).atStartOfDay().toInstant(java.time.ZoneOffset.UTC); }
        catch (java.time.format.DateTimeParseException ex) { return null; }
    }

    private Order toDtoFromBooking(com.bharatshop.entity.BookingEntity e) {
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
        com.bharatshop.domain.BookingDetails b = new com.bharatshop.domain.BookingDetails();
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
        Order.Totals t = new Order.Totals();
        t.payable = e.getTotal();
        o.setTotals(t);
        return o;
    }

    private Order toDto(com.bharatshop.entity.OrderEntity e) {
        Order o = new Order();
        o.setId(e.getId());
        o.setReference(e.getReference());
        o.setStatus(e.getStatus());
        o.setTotal(e.getTotal());
        o.setPaymentMethod(e.getPaymentMethod());
        o.setType(e.getType());
        o.setCreatedAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        o.setSellerResponseDeadline(e.getSellerResponseDeadline() != null ? e.getSellerResponseDeadline().toString() : null);
        o.setSellerAcceptedAt(e.getSellerAcceptedAt() != null ? e.getSellerAcceptedAt().toString() : null);
        o.setCancelledAt(e.getCancelledAt() != null ? e.getCancelledAt().toString() : null);
        o.setCancellationReason(e.getCancellationReason());
        o.setStoreId(e.getStoreId());
        o.setNotes(e.getNotes());
        Order.Totals t = new Order.Totals();
        t.payable = e.getTotal();
        o.setTotals(t);
        List<com.bharatshop.entity.OrderItemEntity> items = orderItemRepository.findByOrderId(e.getId());
        List<com.bharatshop.domain.CartItem> cartItems = items.stream().map(oi -> {
            com.bharatshop.domain.CartItem ci = new com.bharatshop.domain.CartItem();
            ci.setId(oi.getId());
            ci.setName(oi.getName());
            ci.setPrice(oi.getPrice());
            ci.setQuantity(oi.getQuantity());
            ci.setRequiresPrescription(oi.getRequiresPrescription());
            return ci;
        }).collect(Collectors.toList());
        o.setItems(cartItems);
        return o;
    }
}