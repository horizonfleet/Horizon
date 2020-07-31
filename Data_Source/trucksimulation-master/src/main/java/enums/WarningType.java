package enums;

public enum WarningType {
	NORMAL(0), CONSPICIOUS(1), BAD(2);

	private final int value;
	private WarningType(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static int getMinValue() {
		int minValue = Integer.MAX_VALUE;
		for (WarningType t : WarningType.values()) {
			if (t.getValue() < minValue) {
				minValue = t.getValue();
			}
		}
		return minValue;
	}

	public static int getMaxValue() {
		int maxValue = Integer.MIN_VALUE;
		for (WarningType t : WarningType.values()) {
			if (t.getValue() > maxValue) {
				maxValue = t.getValue();
			}
		}
		return maxValue;
	}

	public static WarningType from(int value) {
		for (WarningType t : WarningType.values()) {
			if (t.getValue() == value) {
				return t;
			}
		}
		return null;
	}
}