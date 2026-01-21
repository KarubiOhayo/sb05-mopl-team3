package io.mopl.socket.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.mopl.socket.websocket.session.SessionSubscriptionRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class SocketMetrics {

  private final MeterRegistry registry;
  private final Counter wsConnectTotal;
  private final Counter wsDisconnectTotal;
  private final Counter sseConnectTotal;
  private final Counter sseDisconnectTotal;
  private final AtomicInteger wsActiveConnections = new AtomicInteger(0);
  private final AtomicInteger sseActiveConnections = new AtomicInteger(0);
  private final Map<String, Counter> wsSubscribeCounters = new ConcurrentHashMap<>();
  private final Map<String, Counter> wsUnsubscribeCounters = new ConcurrentHashMap<>();
  private final Map<String, Counter> wsMessageInCounters = new ConcurrentHashMap<>();
  private final Map<String, Counter> sseSendCounters = new ConcurrentHashMap<>();
  private final Map<String, Counter> sseSendFailCounters = new ConcurrentHashMap<>();

  public SocketMetrics(MeterRegistry registry, SessionSubscriptionRegistry subscriptionRegistry) {
    this.registry = registry;
    this.wsConnectTotal =
        Counter.builder("mopl_socket_ws_connections_total").register(this.registry);
    this.wsDisconnectTotal =
        Counter.builder("mopl_socket_ws_disconnects_total").register(this.registry);
    this.sseConnectTotal =
        Counter.builder("mopl_socket_sse_connections_total").register(this.registry);
    this.sseDisconnectTotal =
        Counter.builder("mopl_socket_sse_disconnects_total").register(this.registry);

    Gauge.builder("mopl_socket_ws_connections_active", wsActiveConnections, AtomicInteger::get)
        .register(this.registry);
    Gauge.builder(
            "mopl_socket_ws_subscriptions_active",
            subscriptionRegistry,
            SessionSubscriptionRegistry::size)
        .register(this.registry);
    Gauge.builder("mopl_socket_sse_connections_active", sseActiveConnections, AtomicInteger::get)
        .register(this.registry);
  }

  public void onWsConnect() {
    wsConnectTotal.increment();
    wsActiveConnections.incrementAndGet();
  }

  public void onWsDisconnect() {
    wsDisconnectTotal.increment();
    wsActiveConnections.updateAndGet(current -> Math.max(0, current - 1));
  }

  public void onWsSubscribe(String type) {
    wsSubscribeCounters.computeIfAbsent(type, this::buildWsSubscribeCounter).increment();
  }

  public void onWsUnsubscribe(String type) {
    wsUnsubscribeCounters.computeIfAbsent(type, this::buildWsUnsubscribeCounter).increment();
  }

  public void onWsMessageIn(String type) {
    wsMessageInCounters.computeIfAbsent(type, this::buildWsMessageInCounter).increment();
  }

  public void onSseConnect() {
    sseConnectTotal.increment();
    sseActiveConnections.incrementAndGet();
  }

  public void onSseDisconnect() {
    sseDisconnectTotal.increment();
    sseActiveConnections.updateAndGet(current -> Math.max(0, current - 1));
  }

  public void onSseSend(String type) {
    sseSendCounters.computeIfAbsent(type, this::buildSseSendCounter).increment();
  }

  public void onSseSendFail(String type) {
    sseSendFailCounters.computeIfAbsent(type, this::buildSseSendFailCounter).increment();
  }

  private Counter buildWsSubscribeCounter(String type) {
    return Counter.builder("mopl_socket_ws_subscribe_total").tag("type", type).register(registry);
  }

  private Counter buildWsUnsubscribeCounter(String type) {
    return Counter.builder("mopl_socket_ws_unsubscribe_total").tag("type", type).register(registry);
  }

  private Counter buildWsMessageInCounter(String type) {
    return Counter.builder("mopl_socket_ws_messages_in_total").tag("type", type).register(registry);
  }

  private Counter buildSseSendCounter(String type) {
    return Counter.builder("mopl_socket_sse_send_total").tag("type", type).register(registry);
  }

  private Counter buildSseSendFailCounter(String type) {
    return Counter.builder("mopl_socket_sse_send_fail_total").tag("type", type).register(registry);
  }
}
