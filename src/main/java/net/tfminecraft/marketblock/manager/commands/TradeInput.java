package net.tfminecraft.marketblock.manager.commands;

/**
 * Parses the numbers typed during /marketblock add. Each method returns null when the text is not
 * a usable value, so a typo or a value such as NaN or Infinity cannot reach a saved trade.
 */
public final class TradeInput {

    public static final String CANCEL = "cancel";

    private TradeInput() {
    }

    public static boolean isCancel(String message) {
        return message != null && message.trim().equalsIgnoreCase(CANCEL);
    }

    /** Demand limit: a finite number of at least 1, the lowest a trade allows. */
    public static Double demandLimit(String message) {
        Double value = finite(message);
        return value != null && value >= 1 ? value : null;
    }

    /** Price change: a finite number of at least 0. */
    public static Double priceChange(String message) {
        Double value = finite(message);
        return value != null && value >= 0 ? value : null;
    }

    /** Resting price: a finite number above 0. */
    public static Double restingPrice(String message) {
        Double value = finite(message);
        return value != null && value > 0 ? value : null;
    }

    private static Double finite(String message) {
        if (message == null) return null;
        try {
            double value = Double.parseDouble(message.trim());
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
