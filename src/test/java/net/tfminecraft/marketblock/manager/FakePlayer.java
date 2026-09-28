package net.tfminecraft.marketblock.manager;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

/** A Player with a fixed permission answer and an empty hand, recording the messages it is sent. */
public final class FakePlayer {
    public final UUID id = UUID.randomUUID();
    public final List<String> messages = new ArrayList<>();
    public boolean admin;
    public final Player player;

    public FakePlayer(boolean admin) {
        this.admin = admin;
        PlayerInventory inventory = proxy(PlayerInventory.class, (name, args) -> null);
        this.player = proxy(Player.class, (name, args) -> switch (name) {
            case "getUniqueId" -> id;
            case "getName" -> "tester";
            case "hasPermission" -> this.admin && CommandManager.ADMIN_PERMISSION.equals(args[0]);
            case "getInventory" -> inventory;
            case "sendMessage" -> {
                if (args[0] instanceof String message) messages.add(message);
                yield null;
            }
            default -> null;
        });
    }

    public String lastMessage() {
        return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }

    private interface Answer {
        Object answer(String name, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (self, method, args) -> {
            switch (method.getName()) {
                case "equals": return self == args[0];
                case "hashCode": return System.identityHashCode(self);
                case "toString": return "Fake" + type.getSimpleName();
                default:
            }
            Object value = answer.answer(method.getName(), args == null ? new Object[0] : args);
            return value != null ? value : zero(method.getReturnType());
        });
    }

    private static Object zero(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        return 0;
    }
}
