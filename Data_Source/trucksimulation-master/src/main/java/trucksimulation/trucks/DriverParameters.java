package trucksimulation.trucks;

import enums.WarningType;
import helpers.GenerationHelper;

public class DriverParameters {
	// Speeding
	private double speedFactor;
	// Maximum speed
	private double maxSpeed;
	// Acceleration
	private double a_min;
	private double a_max;
	// Acceleration change
	private double adt_min;
	private double adt_max;
	// Pedal gradients - value between 2=fast and 4=slow
	private double gasPedalReleaseGradient;
	private double brakePedalReleaseGradient;
	// Energy level - value between 0=tired and 1=awake
	private double energyLevel;

	public DriverParameters(Driver driver) {
		if (driver.getSpeedWarning() == WarningType.NORMAL) {
			speedFactor = GenerationHelper.getRandomValue(0.9, 1.0);
			maxSpeed = GenerationHelper.getRandomValue(75.0/3.6, 80.0/3.6);
		} else if (driver.getSpeedWarning() == WarningType.CONSPICIOUS) {
			speedFactor = GenerationHelper.getRandomValue(0.94, 1.04);
			maxSpeed = GenerationHelper.getRandomValue(78.0/3.6, 83.0/3.6);
		} else if (driver.getSpeedWarning() == WarningType.BAD) {
			speedFactor = GenerationHelper.getRandomValue(0.99, 1.1);
			maxSpeed = GenerationHelper.getRandomValue(82.0/3.6, 88.0/3.6);
		}

		if (driver.getAccelerationWarning() == WarningType.NORMAL) {
			a_max = GenerationHelper.getRandomValue(0.4, 0.6);
			adt_max = GenerationHelper.getRandomValue(0.2, 0.35);
			gasPedalReleaseGradient = GenerationHelper.getRandomValue(3.2, 4.0);
		} else if (driver.getAccelerationWarning() == WarningType.CONSPICIOUS) {
			a_max = GenerationHelper.getRandomValue(0.6, 0.9);
			adt_max = GenerationHelper.getRandomValue(0.35, 0.45);
			gasPedalReleaseGradient = GenerationHelper.getRandomValue(2.6, 3.2);
		} else if (driver.getAccelerationWarning() == WarningType.BAD) {
			a_max = GenerationHelper.getRandomValue(0.9, 1.1);
			adt_max = GenerationHelper.getRandomValue(0.4, 0.6);
			gasPedalReleaseGradient = GenerationHelper.getRandomValue(2.0, 2.6);
		}

		if (driver.getBrakeWarning() == WarningType.NORMAL) {
			a_min = GenerationHelper.getRandomValue(-2.5, -1.5);
			adt_min = GenerationHelper.getRandomValue(-0.5, -0.2);
			brakePedalReleaseGradient = GenerationHelper.getRandomValue(3.2, 4.0);
		} else if (driver.getBrakeWarning() == WarningType.CONSPICIOUS) {
			a_min = GenerationHelper.getRandomValue(-3.5, -2.5);
			adt_min = GenerationHelper.getRandomValue(-0.9, -0.5);
			brakePedalReleaseGradient = GenerationHelper.getRandomValue(2.6, 3.2);
		} else if (driver.getBrakeWarning() == WarningType.BAD) {
			a_min = GenerationHelper.getRandomValue(-5.5, -3.5);
			adt_min = GenerationHelper.getRandomValue(-1.6, -0.9);
			brakePedalReleaseGradient = GenerationHelper.getRandomValue(2.0, 2.6);
		}

		if (driver.getTiredWarning() == WarningType.NORMAL) {
			energyLevel = GenerationHelper.getRandomValue(0.8, 1.0);
		} else if (driver.getTiredWarning() == WarningType.CONSPICIOUS) {
			energyLevel = GenerationHelper.getRandomValue(0.4, 0.6);
		} else if (driver.getTiredWarning() == WarningType.BAD) {
			energyLevel = GenerationHelper.getRandomValue(0.0, 0.2);
		}
	}
	
	public double getSpeedFactor() {
		return speedFactor;
	}
	public double getMaxSpeed() {
		return maxSpeed;
	}
	public double getAMin() {
		return a_min;
	}
	public double getAMax() {
		return a_max;
	}
	public double getADtMin() {
		return adt_min;
	}
	public double getADtMax() {
		return adt_max;
	}
	public double getGasPedalReleaseGradient() {
		return gasPedalReleaseGradient;
	}
	public double getBrakePedalReleaseGradient() {
		return brakePedalReleaseGradient;
	}
	public double getEnergyLevel() {
		return energyLevel;
	}
}
