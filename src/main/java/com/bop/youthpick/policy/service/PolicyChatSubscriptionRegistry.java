package com.bop.youthpick.policy.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class PolicyChatSubscriptionRegistry {

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Subscription>> sessions =
            new ConcurrentHashMap<>();

    public void register(
            String sessionId,
            String subscriptionId,
            Long userId,
            String userName,
            Long policyId,
            SubscriptionType type) {
        sessions.computeIfAbsent(sessionId, ignored -> new ConcurrentHashMap<>())
                .put(subscriptionId, new Subscription(userId, userName, policyId, type));
    }

    public void unregister(String sessionId, String subscriptionId) {
        sessions.computeIfPresent(
                sessionId,
                (ignored, subscriptions) -> {
                    subscriptions.remove(subscriptionId);
                    return subscriptions.isEmpty() ? null : subscriptions;
                });
    }

    public void disconnect(String sessionId) {
        sessions.remove(sessionId);
    }

    public Collection<Subscriber> messageSubscribers(Long policyId) {
        Map<String, Subscriber> subscribers = new ConcurrentHashMap<>();
        sessions.values().stream()
                .flatMap(subscriptions -> subscriptions.values().stream())
                .filter(subscription -> subscription.type() == SubscriptionType.MESSAGES)
                .filter(subscription -> subscription.policyId().equals(policyId))
                .forEach(
                        subscription ->
                                subscribers.putIfAbsent(
                                        subscription.userName(),
                                        new Subscriber(
                                                subscription.userId(), subscription.userName())));
        return List.copyOf(subscribers.values());
    }

    int registrationCount() {
        return sessions.values().stream().mapToInt(Map::size).sum();
    }

    long registrationCount(Long policyId, SubscriptionType type) {
        return sessions.values().stream()
                .flatMap(subscriptions -> subscriptions.values().stream())
                .filter(subscription -> subscription.policyId().equals(policyId))
                .filter(subscription -> subscription.type() == type)
                .count();
    }

    public enum SubscriptionType {
        MESSAGES,
        ERRORS
    }

    public record Subscriber(Long userId, String userName) {}

    private record Subscription(
            Long userId, String userName, Long policyId, SubscriptionType type) {}
}
