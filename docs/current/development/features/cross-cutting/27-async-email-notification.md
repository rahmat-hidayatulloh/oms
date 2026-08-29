# OMS-024: Implementasi @Async untuk Email Notification

> **Status:** Current
> **Audience:** Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-28 (screening kode aktual `src/`)
> **Source of truth:** Repository source code (`src/`)

## Deskripsi

Pengiriman email konfirmasi order dilakukan secara **asynchronous** agar `POST /api/v1/orders` tidak terblok oleh proses pengiriman email. Email **disimulasikan lewat log** (`log.info`) — tidak ada SMTP server, `JavaMailSender`, atau dependency email lain di project.

## Alur Eksekusi

```
User → POST /api/v1/orders
  └─ OrderService.createOrder(OrderRequest, userId)
      └─ stock booking + save (orderRepository.save)
      └─ eventPublisher.publishEvent(
           new OrderCreatedEvent(orderNumber, totalAmount, userId, userEmail))
          └─ OrderCreatedEventListener.handleOrderCreatedEvent(event)
              // @Async @EventListener
              └─ EmailService.sendOrderConfirmationEmail(
                     to, orderNumber, totalAmount, userId)   // @Async("emailExecutor")
                  └─ log "=== SIMULATED EMAIL ==="
```

`OrderService` mem-publish domain event `OrderCreatedEvent` via `ApplicationEventPublisher` di akhir `createOrder()` (setelah order tersimpan). Event ditangkap oleh `OrderCreatedEventListener` dengan kombinasi `@EventListener` + `@Async` — eksekusi dipindah ke thread pool `emailExecutor`, sehingga respons HTTP tidak menunggu "pengiriman" email.

## Konfigurasi

### AsyncConfig (`src/main/java/com/oms/config/AsyncConfig.java`)

- Annotation: `@Configuration` + `@EnableAsync`, `implements AsyncConfigurer`
- Executor bean: `@Bean(name = "emailExecutor")` → `ThreadPoolTaskExecutor`
- Nilai (dapat dikonfigurasi, default di kode):
  - Core pool size: `app.email.thread-pool.core-size` (default 5)
  - Max pool size: `app.email.thread-pool.max-size` (default 10)
  - Queue capacity: `app.email.queue.capacity` (default 20)
  - Thread name prefix: `EmailExecutor-`
  - Wait for tasks to complete on shutdown: `setWaitForTasksToCompleteOnShutdown(true)`
- `getAsyncUncaughtExceptionHandler()` → `AsyncExceptionHandler`

### Exception Handler (`src/main/java/com/oms/exception/AsyncExceptionHandler.java`)

- Mencatat exception tak terduga dari method `@Async` ke log dengan detail method & parameter (tidak menggagalkan transaksi order karena berjalan di thread terpisah)

### EmailService (`src/main/java/com/oms/order/service/EmailService.java`)

- `@Service`, implements `EmailServiceInterface`
- Method: `sendOrderConfirmationEmail(String to, String orderNumber, BigDecimal totalAmount, UUID userId)` ber-annotasi `@Async("emailExecutor")`
- Simulasi: mencetak log sebagai ganti pengiriman email real

### OrderCreatedEventListener (`src/main/java/com/oms/order/event/listener/OrderCreatedEventListener.java`)

- _Event definition_: `OrderCreatedEvent` (record `orderNumber`, `totalAmount`, `userId`, `userEmail`) ada di `OrderService`
- Handler `handleOrderCreatedEvent(@Async @EventListener OrderCreatedEvent event)` dipanggil otomatis oleh Spring saat event di-publish

## File yang Dibuat/Diubah

1. `src/main/java/com/oms/config/AsyncConfig.java` (baru)
2. `src/main/java/com/oms/exception/AsyncExceptionHandler.java` (baru)
3. `src/main/java/com/oms/order/service/EmailService.java` (baru)
4. `src/main/java/com/oms/order/service/EmailServiceInterface.java` (baru)
5. `src/main/java/com/oms/order/event/listener/OrderCreatedEventListener.java` (baru)
6. `src/main/java/com/oms/order/service/OrderService.java` (diubah — publish `OrderCreatedEvent`, inject `ApplicationEventPublisher`)

## Contoh Log Output

```log
=== SIMULATED EMAIL ===
To      : customer@example.com
Subject : Order Confirmation OMS-20260806-4821
Body    :
Order OMS-20260806-4821 has been created successfully.
Total   : Rp 170000.00
User ID : 550e8400-e29b-41d4-a716-446655440000
=== END SIMULATED EMAIL ===
```

Log ditulis dari thread pool `EmailExecutor-*`, bukan thread request — indikasi `@Async` bekerja.
