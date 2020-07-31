package trucksimulation.trucks;

import enums.WarningType;
import helpers.GenerationHelper;

public class Driver {
	private WarningType speed;
	private WarningType acceleration;
	private WarningType brake;
	private WarningType tired;
	private WarningType power;
	private WarningType gears;
	private DriverParameters parameters;

	public Driver() {
		speed = WarningType.from(GenerationHelper.getRandomValue(WarningType.getMinValue(), WarningType.getMaxValue()));
		acceleration = WarningType.from(GenerationHelper.getRandomValue(WarningType.getMinValue(), WarningType.getMaxValue()));
		brake = WarningType.from(GenerationHelper.getRandomValue(WarningType.getMinValue(), WarningType.getMaxValue()));
		tired = WarningType.from(GenerationHelper.getRandomValue(WarningType.getMinValue(), WarningType.getMaxValue()));;
		power = WarningType.NORMAL;
		gears = WarningType.NORMAL;

		parameters = new DriverParameters(this);
	}
	
	public WarningType getSpeedWarning() {
		return speed;
	}
	public WarningType getAccelerationWarning() {
		return acceleration;
	}
	public WarningType getBrakeWarning() {
		return brake;
	}
	public WarningType getTiredWarning() {
		return tired;
	}
	public WarningType getPowerWarning() {
		return power;
	}
	public WarningType getGearsWarning() {
		return gears;
	}
	public DriverParameters getParameters() {
		return parameters;
	}
}
