// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import static edu.wpi.first.wpilibj2.command.Commands.run;
import static edu.wpi.first.wpilibj2.command.Commands.runOnce;

import frc.robot.commands.DefaultDriveCommand;
import frc.robot.commands.DriveLockedRotCommand;
import frc.team5431.titan.core.joysticks.CommandXboxController;


import java.util.function.BooleanSupplier;
import java.util.function.Supplier;


import edu.wpi.first.math.Pair;

import edu.wpi.first.math.kinematics.ChassisSpeeds;

import frc.robot.subsystems.Drivebase;


public class RobotContainer {
    private final Systems systems = new Systems();
    public final Drivebase drivebase = systems.getDrivebase();

    private final CommandXboxController driver = new CommandXboxController(0);

    public RobotContainer() {

        driver.setDeadzone(0.15);

        drivebase.setDefaultCommand(new DefaultDriveCommand(
            systems,
            () -> {
                double inX = -driver.getLeftY(); // swap intended
                double inY = -driver.getLeftX();
                double mag = Math.hypot(inX, inY);
                double theta = Math.atan2(inY, inX);
                return Pair.of(modifyAxis(mag) * Drivebase.MAX_VELOCITY_METERS_PER_SECOND, theta);
            },
            () -> modifyAxis(-driver.getRightX()) * Drivebase.MAX_ANGULAR_VELOCITY_RADIANS_PER_SECOND));

        configureBindings();

    }


    private void configureBindings() {
        // Y button zeros the gyroscope
        driver.back().onTrue(runOnce(drivebase::zeroGyroscope));

        // D-Pad cardinal directions
        driver.povUp().whileTrue(run(
                () -> drivebase.drive(new ChassisSpeeds(Drivebase.MAX_VELOCITY_METERS_PER_SECOND*0.15, 0, 0)), drivebase));
        driver.povDown().whileTrue(run(
                () -> drivebase.drive(new ChassisSpeeds(-Drivebase.MAX_VELOCITY_METERS_PER_SECOND*0.15, 0, 0)), drivebase));
        driver.povLeft().whileTrue(run(
                () -> drivebase.drive(new ChassisSpeeds(0, Drivebase.MAX_VELOCITY_METERS_PER_SECOND*0.15, 0)), drivebase));
        driver.povRight().whileTrue(run(
                () -> drivebase.drive(new ChassisSpeeds(0, -Drivebase.MAX_VELOCITY_METERS_PER_SECOND*0.15, 0)), drivebase));


        Supplier<Pair<Double, Double>> defaultDrive = () -> {
            double inX = -Math.pow(driver.getLeftY(), 2); // swap intended
            double inY = -Math.pow(driver.getLeftX(), 2);
            double mag = Math.hypot(inX, inY);
            double theta = Math.atan2(inY, inX);
            return Pair.of(modifyAxis(mag) * Drivebase.MAX_VELOCITY_METERS_PER_SECOND, theta);
        };

        BooleanSupplier isManualAdjustment = () -> {
            return modifyAxis(-driver.getRightX()) != 0;
        };

        driver.a().onTrue(new DriveLockedRotCommand(systems, defaultDrive, 180, isManualAdjustment));
        driver.b().onTrue(new DriveLockedRotCommand(systems, defaultDrive, 270, isManualAdjustment));
        driver.x().onTrue(new DriveLockedRotCommand(systems, defaultDrive, 90, isManualAdjustment));
        driver.y().onTrue(new DriveLockedRotCommand(systems, defaultDrive, 0, isManualAdjustment));

    }


    private static double deadband(double value, double deadband) {
        if (Math.abs(value) > deadband) {
            if (value > 0.0) {
                return (value - deadband) / (1.0 - deadband);
            } else {
                return (value + deadband) / (1.0 - deadband);
            }
        } else {
            return 0.0;
        }
    }

    private static double modifyAxis(double value) {
        // Deadband
        value = deadband(value, 0.15);

        // More sensitive at smaller speeds
        double newValue = Math.pow(value, 2);

        // Copy the sign to the new value
        newValue = Math.copySign(newValue, value);

        return newValue;
    }

    public void teleopPeriodic() {}
         
    public void robotPeriodic() {
       
    }

    public void teleopInit() {
       
    }

    public void disabledInit() {
    }
}
