# Task 8: Entity Order dan OrderItem

> **Status:** Current
> **Audience:** Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-29 (screening kode aktual `src/`, `pom.xml` — V5-P0 by-feature layout)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## Tujuan
Membuat entity Order dan OrderItem dengan relationship OneToMany untuk sistem e-commerce.

## Acceptance Criteria
- [x] Order fields: id, orderNumber, userId, totalAmount, status (PENDING, PAID, SHIPPED, DELIVERED, CANCELLED)
- [x] OrderItem fields: id, productId, productName, quantity, price
- [x] Relationship: @OneToMany(mappedBy = "order", fetch = LAZY)
- [x] @ManyToOne di OrderItem ke Order
- [x] Unit test untuk Order dan OrderItem

## Detail Pekerjaan

### 1. File yang Dibuat

> **Catatan (V5-P0):** Struktur package telah di-restructure dari by-layer ke by-feature. Semua entity dan repository Order kini berada di `com.oms.order`.

```
src/main/java/com/oms/order/
├── entity/
│   ├── OrderStatus.java          ← Enum: PENDING, PAID, SHIPPED, DELIVERED, CANCELLED
│   ├── Order.java                ← Entity utama + @OneToMany (cascade = ALL)
│   └── OrderItem.java            ← Entity + @ManyToOne ke Order
└── repository/
    ├── OrderRepository.java      ← JPA Repository
    └── OrderItemRepository.java  ← JPA Repository

src/test/java/com/oms/order/entity/
└── OrderTest.java                ← Unit test
```

### 2. OrderStatus Enum

```java
public enum OrderStatus {
    PENDING,      // Menunggu pembayaran
    PAID,         // Sudah dibayar
    SHIPPED,      // Sudah dikirim
    DELIVERED,    // Sudah diterima
    CANCELLED     // Dibatalkan
}
```

### 3. Order Entity - Fields

| Field | Type | Constraint | Keterangan |
|---|---|---|---|
| `id` | UUID (auto) | PK, not null | Generated via `GenerationType.UUID` |
| `orderNumber` | String(50) | unique, not null | Nomor order unik |
| `userId` | UUID | not null | ID user yang membuat order |
| `totalAmount` | BigDecimal | not null | Total harga (precision 12, scale 2) |
| `status` | Enum(OrderStatus) | not null | Status order (default: PENDING) |
| `orderItems` | List<OrderItem> | @OneToMany | Relasi ke order items |
| `createdAt` | LocalDateTime | auto | @CreatedDate (dari BaseEntity) |
| `updatedAt` | LocalDateTime | auto | @LastModifiedDate (dari BaseEntity) |
| `deletedAt` | LocalDateTime | nullable | Soft delete marker (dari BaseEntity) |

### 4. OrderItem Entity - Fields

| Field | Type | Constraint | Keterangan |
|---|---|---|---|
| `id` | UUID (auto) | PK, not null | Generated via `GenerationType.UUID` |
| `productId` | UUID | not null | ID produk yang dibeli |
| `productName` | String(100) | not null | Nama produk (snapshot) |
| `quantity` | Integer | not null | Jumlah barang |
| `price` | BigDecimal | not null | Harga satuan (precision 10, scale 2) |
| `order` | Order | @ManyToOne | Relasi ke order |
| `createdAt` | LocalDateTime | auto | @CreatedDate (dari BaseEntity) |
| `updatedAt` | LocalDateTime | auto | @LastModifiedDate (dari BaseEntity) |
| `deletedAt` | LocalDateTime | nullable | Soft delete marker (dari BaseEntity) |

### 5. Relationship Mapping

```java
// Order.java — com.oms.order.entity
@OneToMany(mappedBy = "order", orphanRemoval = true, cascade = jakarta.persistence.CascadeType.ALL)
@Builder.Default
private List<OrderItem> orderItems = new ArrayList<>();

public List<OrderItem> getOrderItems() {
    return Collections.unmodifiableList(orderItems);
}

public void addOrderItem(OrderItem orderItem) {
    orderItems.add(orderItem);
    orderItem.setOrder(this);
}

public void removeOrderItem(OrderItem orderItem) {
    orderItems.remove(orderItem);
    orderItem.setOrder(null);
}
```

```java
// OrderItem.java — com.oms.order.entity
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "order_id", nullable = false)
private Order order;
```

> **Catatan:** Relasi `@OneToMany` memakai `cascade = CascadeType.ALL` sehingga OrderItem ikut di-persist/delete bersama Order. `orphanRemoval = true` memastikan item yang di-remove dari koleksi langsung dihapus dari database.

### 6. Helper Methods

**Order:**
- `addOrderItem(OrderItem)` - Tambah item ke order
- `removeOrderItem(OrderItem)` - Hapus item dari order
- `getOrderItems()` - Return unmodifiable list

**OrderItem:**
- `getOrder()` - Get order reference
- `setOrder(Order)` - Set order reference

### 7. Unit Test

```java
class OrderTest {
    @Test void testOrderCreation() { ... }
    @Test void testAddOrderItem() { ... }
    @Test void testRemoveOrderItem() { ... }
    @Test void testOrderItemsUnmodifiable() { ... }
}
```

**Test Coverage:**
- ✅ Order creation dengan builder
- ✅ Add order item (bidirectional relationship)
- ✅ Remove order item
- ✅ Unmodifiable list protection

### 8. Hasil Verifikasi

```
BUILD SUCCESS ✅
├── Compile: OK
├── Checkstyle: 0 violations
├── SpotBugs: 0 bugs (excluded via filter)
├── Tests: 5/5 passed
│   ├── OmsApplicationTest: 1/1
│   └── OrderTest: 4/4
└── Sonar: Clean
```

### 9. Catatan

- **cascade = CascadeType.ALL** — OrderItem otomatis di-persist dan di-delete bersama Order melalui cascade. Ini memungkinkan `orderRepository.save(order)` menyimpan semua `OrderItem` sekaligus tanpa perlu `OrderItemRepository.saveAll()` terpisah.
- **orphanRemoval = true** - OrderItem akan otomatis dihapus jika di-remove dari Order
- **LAZY fetch** - Order items hanya di-load ketika diakses (hindari N+1 — gunakan `@EntityGraph` untuk prefetch saat perlu)
- **BigDecimal** untuk price dan totalAmount - akurat untuk perhitungan uang
- **productName snapshot** - Menyimpan nama produk saat order dibuat (tidak bergantung pada Product entity)
- **Unmodifiable list** - Melindungi internal representation dari modification; setter `setOrderItems()` tersedia untuk keperluan test/Builder