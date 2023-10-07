package frc.robot.subsystems;

import com.ctre.phoenix.motorcontrol.can.WPI_TalonFX;
import com.ctre.phoenix.sensors.WPI_Pigeon2;
import com.ctre.phoenix.sensors.Pigeon2.AxisDirection;
import com.swervedrivespecialties.swervelib.MkModuleConfiguration;
import com.swervedrivespecialties.swervelib.MkSwerveModuleBuilder;
import com.swervedrivespecialties.swervelib.MotorType;
import com.swervedrivespecialties.swervelib.SdsModuleConfigurations;
import com.swervedrivespecialties.swervelib.SwerveModule;

import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.filter.SlewRateLimiter;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

import static frc.robot.Constants.*;

import java.util.ArrayList;
import java.util.List;

public class Drivebase extends SubsystemBase {
    
   
    public static final double MAX_VOLTAGE = 12.0;

    public static final double MAX_VELOCITY_METERS_PER_SECOND = 6380.0 / 60.0 *
                    SdsModuleConfigurations.MK4_L2.getDriveReduction() *
                    SdsModuleConfigurations.MK4_L2.getWheelDiameter() * Math.PI;
  
    public static final double MAX_ANGULAR_VELOCITY_RADIANS_PER_SECOND = MAX_VELOCITY_METERS_PER_SECOND /
                    Math.hypot(DRIVETRAIN_TRACKWIDTH_METERS / 2.0, DRIVETRAIN_WHEELBASE_METERS / 2.0);

    public static final double MIN_ANGULAR_VELOCITY = 0.5;
    // Max input acceleration (ChassisSpeeds meters per second per second) for x/y movement
    public static final double SLEW_RATE_LIMIT_TRANSLATION = MAX_VELOCITY_METERS_PER_SECOND * 2;
    // Max input acceleration (ChassisSpeeds radians per second per second) for rotational movement
    public static final double SLEW_RATE_LIMIT_ROTATION = MAX_ANGULAR_VELOCITY_RADIANS_PER_SECOND * 10;

    public final SwerveDriveKinematics m_kinematics = new SwerveDriveKinematics(
                    // Front left
                    new Translation2d(DRIVETRAIN_TRACKWIDTH_METERS / 2.0, DRIVETRAIN_WHEELBASE_METERS / 2.0),
                    // Front right
                    new Translation2d(DRIVETRAIN_TRACKWIDTH_METERS / 2.0, -DRIVETRAIN_WHEELBASE_METERS / 2.0),
                    // Back left
                    new Translation2d(-DRIVETRAIN_TRACKWIDTH_METERS / 2.0, DRIVETRAIN_WHEELBASE_METERS / 2.0),
                    // Back right
                    new Translation2d(-DRIVETRAIN_TRACKWIDTH_METERS / 2.0, -DRIVETRAIN_WHEELBASE_METERS / 2.0)
    );
    
    public final WPI_Pigeon2 pigeon2;

    public final SwerveDrivePoseEstimator poseEstimator;

    // These are our modules. We initialize them in the constructor.
    private final SwerveModule m_frontLeftModule;
    private final SwerveModule m_frontRightModule;
    private final SwerveModule m_backLeftModule;
    private final SwerveModule m_backRightModule;

    private ChassisSpeeds m_chassisSpeeds = new ChassisSpeeds(0.0, 0.0, 0.0);

    private final SlewRateLimiter filter_vx;
    private final SlewRateLimiter filter_vy;
    private final SlewRateLimiter filter_or;

    public final Field2d field2d;

    public Drivebase() {
        pigeon2 = new WPI_Pigeon2(ID_PIGEON2, CANBUS_DRIVETRAIN);
        pigeon2.configMountPose(AxisDirection.NegativeX, AxisDirection.PositiveZ);

        MkModuleConfiguration moduleConfig = MkModuleConfiguration.getDefaultSteerFalcon500();
        moduleConfig.setDriveCurrentLimit(40.0);
        moduleConfig.setSteerCurrentLimit(30.0);

        m_frontLeftModule = new MkSwerveModuleBuilder(moduleConfig)
                .withGearRatio(SdsModuleConfigurations.MK4_L2)
                .withDriveMotor(MotorType.FALCON, FRONT_LEFT_MODULE_DRIVE_MOTOR, CANBUS_DRIVETRAIN)
                .withSteerMotor(MotorType.FALCON, FRONT_LEFT_MODULE_STEER_MOTOR, CANBUS_DRIVETRAIN)
                .withSteerEncoderPort(FRONT_LEFT_MODULE_STEER_ENCODER, CANBUS_DRIVETRAIN)
                .withSteerOffset(FRONT_LEFT_MODULE_STEER_OFFSET)
                .build();

        m_frontRightModule = new MkSwerveModuleBuilder(moduleConfig)
                .withGearRatio(SdsModuleConfigurations.MK4_L2)
                .withDriveMotor(MotorType.FALCON, FRONT_RIGHT_MODULE_DRIVE_MOTOR, CANBUS_DRIVETRAIN)
                .withSteerMotor(MotorType.FALCON, FRONT_RIGHT_MODULE_STEER_MOTOR, CANBUS_DRIVETRAIN)
                .withSteerEncoderPort(FRONT_RIGHT_MODULE_STEER_ENCODER, CANBUS_DRIVETRAIN)
                .withSteerOffset(FRONT_RIGHT_MODULE_STEER_OFFSET)
                .build();

        m_backLeftModule = new MkSwerveModuleBuilder(moduleConfig)
                .withGearRatio(SdsModuleConfigurations.MK4_L2)
                .withDriveMotor(MotorType.FALCON, BACK_LEFT_MODULE_DRIVE_MOTOR, CANBUS_DRIVETRAIN)
                .withSteerMotor(MotorType.FALCON, BACK_LEFT_MODULE_STEER_MOTOR, CANBUS_DRIVETRAIN)
                .withSteerEncoderPort(BACK_LEFT_MODULE_STEER_ENCODER, CANBUS_DRIVETRAIN)
                .withSteerOffset(BACK_LEFT_MODULE_STEER_OFFSET)
                .build();

        m_backRightModule = new MkSwerveModuleBuilder(moduleConfig)
                .withGearRatio(SdsModuleConfigurations.MK4_L2)
                .withDriveMotor(MotorType.FALCON, BACK_RIGHT_MODULE_DRIVE_MOTOR, CANBUS_DRIVETRAIN)
                .withSteerMotor(MotorType.FALCON, BACK_RIGHT_MODULE_STEER_MOTOR, CANBUS_DRIVETRAIN)
                .withSteerEncoderPort(BACK_RIGHT_MODULE_STEER_ENCODER, CANBUS_DRIVETRAIN)
                .withSteerOffset(BACK_RIGHT_MODULE_STEER_OFFSET)
                .build();

        poseEstimator = new SwerveDrivePoseEstimator(m_kinematics, getGyroscopeRotation(), getPositions(), new Pose2d());
        

        filter_vx = new SlewRateLimiter(SLEW_RATE_LIMIT_TRANSLATION);
        filter_vy = new SlewRateLimiter(SLEW_RATE_LIMIT_TRANSLATION);
        filter_or = new SlewRateLimiter(SLEW_RATE_LIMIT_ROTATION);

        SmartDashboard.putData("Gyro", pigeon2);

        ShuffleboardTab visionTab = Shuffleboard.getTab("Vision");

        field2d = new Field2d();

        visionTab.addString("Pose", this::getFormattedPose)
            .withPosition(0, 0)
            .withSize(2, 0);
        visionTab.add("Field", field2d)
            .withPosition(2, 0)
            .withSize(6,4);
    }

    private String getFormattedPose() {
        Pose2d pose = getEstimatedPosition();
        return String.format("(%.2f, %.2f) %.2f degrees",
            pose.getX(),
            pose.getY(),
            pose.getRotation().getDegrees());
    }


    public void resetOdometry(Pose2d pose) {
        poseEstimator.resetPosition(getGyroscopeRotation(), getPositions(), pose);
    }

    public void addVisionMeasurement(Pose2d visionRobotPoseMeters, double timestampSeconds) {
        poseEstimator.addVisionMeasurement(visionRobotPoseMeters, timestampSeconds);
    }

    public Pose2d getEstimatedPosition() {
        return poseEstimator.getEstimatedPosition();
    }

    public void zeroGyroscope() {
        pigeon2.reset();
        resetOdometry(getEstimatedPosition());
    }

    public void resetGyroAt(double yaw) {
        pigeon2.setYaw(yaw);
     
    }

    public Rotation2d getGyroscopeRotation() {
        return pigeon2.getRotation2d();
    }

    public WPI_Pigeon2 getGyro() {
        return pigeon2;
    }


    public void drive(ChassisSpeeds chassisSpeeds) {
        ChassisSpeeds speedsModified = new ChassisSpeeds(
            filter_vx.calculate(chassisSpeeds.vxMetersPerSecond),
            filter_vy.calculate(chassisSpeeds.vyMetersPerSecond),
            filter_or.calculate(chassisSpeeds.omegaRadiansPerSecond)
        );
        driveRaw(speedsModified);
    }

    public void driveRaw(ChassisSpeeds chassisSpeeds) {
        m_chassisSpeeds = chassisSpeeds;
    }
    public void stop() {
        driveRaw(new ChassisSpeeds());
    }

    public SwerveModulePosition[] getPositions() {
        return new SwerveModulePosition[] {
            m_frontLeftModule.getPosition(),
            m_frontRightModule.getPosition(),
            m_backLeftModule.getPosition(),
            m_backRightModule.getPosition()
        };
    }

    public List<WPI_TalonFX> getMotors() {
        List<WPI_TalonFX> retval = new ArrayList<>();
        for (SwerveModule s : new SwerveModule[]{ m_frontLeftModule, m_frontRightModule, m_backLeftModule, m_backRightModule }) {
            retval.add((WPI_TalonFX) s.getSteerMotor());
            retval.add((WPI_TalonFX) s.getDriveMotor());
        }
        return retval;
    }


    @Override
    public void periodic() {
        poseEstimator.update(getGyroscopeRotation(), getPositions());
        field2d.setRobotPose(getEstimatedPosition());
        SmartDashboard.putNumber("Pitch", pigeon2.getPitch());
        
        final double zeroDeadzone = 0.001;

        // Set deadzone on translation
        if (Math.abs(m_chassisSpeeds.vxMetersPerSecond) < zeroDeadzone) {
            m_chassisSpeeds.vxMetersPerSecond = 0;
        }
        if (Math.abs(m_chassisSpeeds.vyMetersPerSecond) < zeroDeadzone) {
            m_chassisSpeeds.vyMetersPerSecond = 0;
        }

        // Hockey-lock if stopped by setting rotation to realllly low number
        if (m_chassisSpeeds.vxMetersPerSecond == 0 && 
            m_chassisSpeeds.vyMetersPerSecond == 0 && 
            Math.abs(m_chassisSpeeds.omegaRadiansPerSecond) < zeroDeadzone) {
            m_chassisSpeeds.omegaRadiansPerSecond = 0.00001;
        }

        SmartDashboard.putNumber("DT X spd", m_chassisSpeeds.vxMetersPerSecond);
        SmartDashboard.putNumber("DT Y spd", m_chassisSpeeds.vyMetersPerSecond);
        SmartDashboard.putNumber("DT . spd", Math.hypot(m_chassisSpeeds.vxMetersPerSecond, m_chassisSpeeds.vyMetersPerSecond));
        SmartDashboard.putNumber("DT O rot", m_chassisSpeeds.omegaRadiansPerSecond);

        SwerveModuleState[] states = m_kinematics.toSwerveModuleStates(m_chassisSpeeds);

        SwerveDriveKinematics.desaturateWheelSpeeds(states, MAX_VELOCITY_METERS_PER_SECOND);

        SwerveModulePosition[] positions = getPositions();
        for (int i = 0; i < states.length; i++) {
            states[i] = SwerveModuleState.optimize(states[i], positions[i].angle);
        }

        double flVoltage;
        double frVoltage;
        double blVoltage;
        double brVoltage;

        flVoltage = states[0].speedMetersPerSecond;
        frVoltage = states[1].speedMetersPerSecond;
        blVoltage = states[2].speedMetersPerSecond;
        brVoltage = states[3].speedMetersPerSecond;

        flVoltage = flVoltage / MAX_VELOCITY_METERS_PER_SECOND * MAX_VOLTAGE;
        frVoltage = frVoltage / MAX_VELOCITY_METERS_PER_SECOND * MAX_VOLTAGE;
        blVoltage = blVoltage / MAX_VELOCITY_METERS_PER_SECOND * MAX_VOLTAGE;
        brVoltage = brVoltage / MAX_VELOCITY_METERS_PER_SECOND * MAX_VOLTAGE;

        m_frontLeftModule.set(flVoltage, states[0].angle.getRadians());
        m_frontRightModule.set(frVoltage, states[1].angle.getRadians());
        m_backLeftModule.set(blVoltage, states[2].angle.getRadians());
        m_backRightModule.set(brVoltage, states[3].angle.getRadians());
    }
}
