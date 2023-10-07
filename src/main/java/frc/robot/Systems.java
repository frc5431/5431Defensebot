package frc.robot;


import frc.robot.subsystems.*;

public class Systems {
    private Drivebase drivebase;
 

    public Systems() {
        drivebase = new Drivebase();
        if(Robot.isReal()) {
        }  
    }

    public Drivebase getDrivebase() {
        return drivebase;
    }

}
