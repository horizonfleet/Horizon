package enums;

public enum RoadType {
	URBAN(0), INTERURBAN(1), HIGHWAY(2), FREEWAY(3);

	private final int value;
	private RoadType(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static int getMinValue() {
		int minValue = Integer.MAX_VALUE;
		for (RoadType t : RoadType.values()) {
			if (t.getValue() < minValue) {
				minValue = t.getValue();
			}
		}
		return minValue;
	}

	public static int getMaxValue() {
		int maxValue = Integer.MIN_VALUE;
		for (RoadType t : RoadType.values()) {
			if (t.getValue() > maxValue) {
				maxValue = t.getValue();
			}
		}
		return maxValue;
	}

	public static RoadType from(int value) {
		for (RoadType t : RoadType.values()) {
			if (t.getValue() == value) {
				return t;
			}
		}
		return null;
	}
}