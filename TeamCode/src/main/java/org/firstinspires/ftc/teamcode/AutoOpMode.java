package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.ServoImpl;
import com.qualcomm.robotcore.util.ElapsedTime;

@Autonomous
public class AutoOpMode extends LinearOpMode {

    private DcMotor front_left;
    private DcMotor front_right;
    private DcMotor back_left;
    private DcMotor back_right;
    private boolean is_blue;
    private boolean is_red;
    private boolean fancy_auto;
    private boolean wait_choice;
    private static final double BALL_INTAKE_POSITION = 0.50;
    private static final double CUBE_INTAKE_POSITION = 0.625;
    private static final double DROPOFF_POSITION = 0.43;
    private static final double OBSTACLE_POSITION = 0.27;


    @Override
    public void runOpMode() throws InterruptedException {

        front_left = hardwareMap.get(DcMotor.class, "front_left");
        front_right = hardwareMap.get(DcMotor.class, "front_right");
        back_left = hardwareMap.get(DcMotor.class, "back_left");
        back_right = hardwareMap.get(DcMotor.class, "back_right");
        front_right.setDirection(DcMotorSimple.Direction.REVERSE);
        back_right.setDirection(DcMotorSimple.Direction.REVERSE);
        DcMotor carousel_spin_blue = hardwareMap.get(DcMotor.class, "carousel_spin_blue");
        carousel_spin_blue.setDirection(DcMotorSimple.Direction.REVERSE);
        DcMotor carousel_spin_red = hardwareMap.get(DcMotor.class, "carousel_spin_red");
        CRServo left_intake = hardwareMap.get(CRServo.class, "intake_left");
        CRServo right_intake = hardwareMap.get(CRServo.class, "intake_right");
        left_intake.setDirection(DcMotorSimple.Direction.REVERSE);
        ServoImpl intake_pivot = hardwareMap.get(ServoImpl.class, "intake_pivot");

        intake_pivot.setPosition(OBSTACLE_POSITION);

        choosewell();


        // Go forward until the alliance shipping hub
        drive(.55, 0, 0, 553);
        // Change intake position and out-take the cube
        intake_pivot.setPosition(DROPOFF_POSITION);
        waitfor(500);
        left_intake.setPower(.5);
        right_intake.setPower(.5);
        waitfor(500);
        intake_pivot.setPosition(OBSTACLE_POSITION);
        waitfor(500);
        // Strafe right until the border
        drive(0, .55, 0, 1473);
        // Go back until the carousel
        drive(-.55, 0, 0, 1000);
        // Spin the carousel
        if (is_blue = true){
            carousel_spin_blue.setPower(.55);
        } else if (is_red = true) {
            carousel_spin_red.setPower(.55);
        }
        waitfor(1000);
        // Go forward to line up with the warehouse
        drive(.55, 0, 0, 1000);
        // Strafe left to get into the warehouse
        drive(0, -.6, 0, 6000);
    }

    private void choosewell() {
        while (!isStopRequested() && !isStarted()) {
            if (gamepad1.dpad_left) {
                is_blue = false;
                is_red = true;
            }
            if (gamepad1.dpad_right) {
                is_red = false;
                is_blue = true;
            }
            if (gamepad1.a) {
                fancy_auto = true;
            }
            if (gamepad1.b) {
                fancy_auto = false;
            }

            telemetry.addData("fancy auto(a/b)", fancy_auto ? "yes" : "no");
            telemetry.addData("Alliance Color Red(dpad left/dpad right)", is_red ? "yes" : "no");
            telemetry.addData("Alliance Color Blue(dpad up/dpad down)", is_blue ? "yes" : "no");
            telemetry.addData("wait(x/y)", wait_choice ? "true" : "false");
            telemetry.update();
        }
    }

    private void drive(double speed, double strafe, double rotate, long milis){
        double front_left_power = (speed+strafe+rotate);
        double front_right_power = (speed-strafe-rotate);
        double back_left_power = (speed-strafe+rotate);
        double back_right_power = (speed+strafe-rotate);
        double max = Math.max(Math.max(Math.abs(front_left_power), Math.abs(front_right_power)),
                Math.max(Math.abs(back_left_power), Math.abs(back_right_power)));
        double scale;
        if (max>1){
            scale = 1/max;
        } else{
            scale = 1;
        }
        front_left.setPower(scale*front_left_power);
        front_right.setPower(scale*front_right_power);
        back_left.setPower(scale*back_left_power);
        back_right.setPower(scale*back_right_power);

        waitfor(milis);

        front_left.setPower(0);
        back_left.setPower(0);
        front_right.setPower(0);
        back_right.setPower(0);
    }

    private void waitfor(long milis){
        ElapsedTime timer=new ElapsedTime();
        while (opModeIsActive() && timer.milliseconds()<milis){
            idle();
        }
    }


}
