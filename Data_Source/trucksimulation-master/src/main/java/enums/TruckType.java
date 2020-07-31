package enums;

public enum TruckType {
	LOCAL(0), LONG_DISTANCE(1), LONG_DISTANCE_TRAILER(2);

	private final int value;
	private TruckType(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static int getMinValue() {
		int minValue = Integer.MAX_VALUE;
		for (TruckType t : TruckType.values()) {
			if (t.getValue() < minValue) {
				minValue = t.getValue();
			}
		}
		return minValue;
	}

	public static int getMaxValue() {
		int maxValue = Integer.MIN_VALUE;
		for (TruckType t : TruckType.values()) {
			if (t.getValue() > maxValue) {
				maxValue = t.getValue();
			}
		}
		return maxValue;
	}

	public static TruckType from(int value) {
		for (TruckType t : TruckType.values()) {
			if (t.getValue() == value) {
				return t;
			}
		}
		return null;
	}
}