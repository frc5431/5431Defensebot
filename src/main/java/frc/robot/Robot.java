// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import edu.wpi.first.math.Pair;
import edu.wpi.first.wpilibj.DataLogManager;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;

import java.util.ArrayList;

import com.pathplanner.lib.server.PathPlannerServer;

public class Robot extends TimedRobot {
  public static final ArrayList<Pair<Runnable, Double>> periodics = new ArrayList<>();
  private Command m_autonomousCommand;

  private RobotContainer m_robotContainer;

  @Override
  public void robotInit() {
    DriverStation.silenceJoystickConnectionWarning(true); // false
    PathPlannerServer.startServer(5811);
    DataLogManager.start();
    m_robotContainer = new RobotContainer();

    // try {
    //   CameraServer.startAutomaticCapture();
    // } catch (VideoException e) {
    //   Logger.l("Unable to start automatic capture for CameraServer!");
    // }

    // initialization should have finished, so register periodics
    for (var period : periodics) {
      addPeriodic(period.getFirst(), period.getSecond());
    }
  }

  @Override
  public void robotPeriodic() {
    CommandScheduler.getInstance().run();
    m_robotContainer.robotPeriodic();
  }

  @Override
  public void disabledInit() {
    m_robotContainer.disabledInit();
  }

  @Override
  public void disabledPeriodic() {}

  @Override
  public void disabledExit() {}

  @Override
  public void autonomousInit() {
 
  }

  @Override
  public void autonomousPeriodic() {}

  @Override
  public void autonomousExit() {}

  @Override
  public void teleopInit() {
    if (m_autonomousCommand != null) {
      m_autonomousCommand.cancel();
    }
    m_robotContainer.teleopInit();
  }

  @Override
  public void teleopPeriodic() {
    m_robotContainer.teleopPeriodic();
  }

  @Override
  public void teleopExit() {}

  @Override
  public void testInit() {
    CommandScheduler.getInstance().cancelAll();
  }

  @Override
  public void testPeriodic() {}

  @Override
  public void testExit() {}

  @Override
  public void simulationInit() {
    DriverStation.silenceJoystickConnectionWarning(true);
  }
}
