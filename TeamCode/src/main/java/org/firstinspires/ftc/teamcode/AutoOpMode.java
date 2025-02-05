package org.firstinspires.ftc.teamcode;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.ServoImpl;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.android.util.Size;
import org.firstinspires.ftc.robotcore.external.function.Consumer;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.external.function.ContinuationResult;
import org.firstinspires.ftc.robotcore.external.hardware.camera.Camera;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureRequest;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureSequenceId;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureSession;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCharacteristics;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraException;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraFrame;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraManager;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.stream.CameraStreamServer;
import org.firstinspires.ftc.robotcore.external.stream.CameraStreamSource;
import org.firstinspires.ftc.robotcore.internal.collections.EvictingBlockingQueue;
import org.firstinspires.ftc.robotcore.internal.network.CallbackLooper;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.ContinuationSynchronizer;
import org.firstinspires.ftc.robotcore.internal.system.Deadline;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

@Autonomous
public class AutoOpMode extends LinearOpMode {

    private DcMotor front_left;
    private DcMotor front_right;
    private DcMotor back_left;
    private DcMotor back_right;

    private static final double TILT_STORED_POSITION = .35;
    private static final double TILT_DOWN_POSITION = .6;
    private static final double SCISSOR_STORED_POSITION = .35;
    private static final double SCISSOR_OPEN_POSITION = .95;

    //    private boolean is_blue = false;
//    private boolean is_red = true;
    private boolean wait_choice = false;
    private boolean strafe_left = false;
//    private boolean skip_duck = false;
//    private boolean just_duck = false;
//    private boolean visionary_auto=true;
//    private boolean vision_spinner=false;
    // vision spinner vs vision barrier refers to location of setup
//    private static final double BALL_INTAKE_POSITION = 0.50;
//    private static final double CUBE_INTAKE_POSITION = 0.625;
//    private static final double DROPOFF_POSITION = 0.43;
//    private static final double OBSTACLE_POSITION = 0.27;
    private static final String TAG = "Webcam Sample";
//    private static final double CAPPER_STORED_POSITION = .8;
//    private static final int SLIDE_LEVEL1_TICKS = 380;
//    private static final int SLIDE_LEVEL2_TICKS = 803;
//    private static final int SLIDE_LEVEL3_TICKS = 1358;

    private static final int SIGNAL_X = 126;
    private static final int SIGNAL_W = 61;
    private static final int SIGNAL_Y = 313;
    private static final int SIGNAL_H = 116;
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    /** How long we are to wait to be granted permission to use the camera before giving up. Here,
     * we wait indefinitely */
    private static final int secondsPermissionTimeout = Integer.MAX_VALUE;

    /** State regarding our interaction with the camera */
    private CameraManager cameraManager;
    private WebcamName cameraName;
    private Camera camera;
    private CameraCaptureSession cameraCaptureSession;

    /** The queue into which all frames from the camera are placed as they become available.
     * Frames which are not processed by the OpMode are automatically discarded. */
    private EvictingBlockingQueue<Bitmap> frameQueue;

    /** State regarding where and how to save frames when the 'A' button is pressed. */
    private int captureCounter = 0;
    private File captureDirectory = AppUtil.ROBOT_DATA_DIR;

    /** A utility object that indicates where the asynchronous callbacks from the camera
     * infrastructure are to run. In this OpMode, that's all hidden from you (but see {@link #startCamera}
     * if you're curious): no knowledge of multi-threading is needed here. */
    private Handler callbackHandler;

    private boolean captureWhenAvailable = true;
    private Continuation<? extends Consumer<Bitmap>> cameraStreamRequestContinuation;
    private int parking_zone;

    @Override
    public void runOpMode() throws InterruptedException {

        front_left = hardwareMap.get(DcMotor.class, "front_left");
        front_right = hardwareMap.get(DcMotor.class, "front_right");
        back_right = hardwareMap.get(DcMotor.class, "back_right");
        back_left = hardwareMap.get(DcMotor.class, "back_left");
        front_right.setDirection(DcMotorSimple.Direction.REVERSE);
        back_right.setDirection(DcMotorSimple.Direction.REVERSE);


//        DcMotor slide = hardwareMap.get(DcMotor.class, "slide");

//        DcMotor carousel_spin_blue = hardwareMap.get(DcMotor.class, "carousel_spin_blue");
//        DcMotor carousel_spin_red = hardwareMap.get(DcMotor.class, "carousel_spin_red");
//        carousel_spin_red.setDirection(DcMotorSimple.Direction.REVERSE);

//        CRServo intake_left = hardwareMap.get(CRServo.class, "intake_left");
//        CRServo intake_right = hardwareMap.get(CRServo.class, "intake_right");



//        Servo capper = hardwareMap.get(Servo.class, "capper");
//        capper.setPosition(CAPPER_STORED_POSITION);

        callbackHandler = CallbackLooper.getDefault().getHandler();

        cameraManager = ClassFactory.getInstance().getCameraManager();
        cameraName = hardwareMap.get(WebcamName.class, "Webcam 1");

        initializeFrameQueue(2);
        AppUtil.getInstance().ensureDirectoryExists(captureDirectory);

        CameraStreamServer.getInstance().setSource(new CameraStreamSource() {
            @Override
            public void getFrameBitmap(Continuation<? extends Consumer<Bitmap>> continuation) {
                cameraStreamRequestContinuation = continuation;
                captureWhenAvailable = true;
            }
        });

        try {
            openCamera();
            if (camera == null) return;

            startCamera();
            if (cameraCaptureSession == null) return;

            choosewell();
        } finally {
            closeCamera();
        }
        Servo tilt = hardwareMap.get(Servo.class, "tilt");
        tilt.setPosition(TILT_STORED_POSITION);
        Servo scissor = hardwareMap.get(Servo.class, "scissor");
        scissor.setPosition(SCISSOR_OPEN_POSITION);

//        slide.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
//        slide.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
//        slide.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
//        ElapsedTime slide_timer = new ElapsedTime();
//        boolean slide_stable = true;
//        double slide_power;
//        int slide_ticks = slide.getCurrentPosition();

        if (wait_choice == true) {

            //Waits for 10 seconds
            waitfor(10000);
        }
        if(strafe_left==false){
            drive(0,.6,0,1150);
        }
        else if (strafe_left==true){
            drive(0,-.6,0,1000);
        }

    }


    private void choosewell() {

        while (!isStopRequested() && !isStarted()) {

//            telemetry.addData("Alliance Color Red(dpad left)", is_red ? "yes" : "no");
//            telemetry.addData("Alliance Color Blue(dpad right)", is_blue ? "yes" : "no");
            telemetry.addData("Wait(a/b)", wait_choice ? "yes" : "no");
            telemetry.addData("Strafe Direction(x Left /y Right)", strafe_left ? "left" : "right");
//            telemetry.addData("Skip duck(dpad up/dpad down)", skip_duck ? "yes" : "no");
//            telemetry.addData("Just duck(right bumper/left bumper)", just_duck ? "yes" : "no");
//            telemetry.addData("visionary auto (left trigger/right trigger)", visionary_auto ? "yes" : "no");
//            telemetry.addData("visionary auto side(start/back)", vision_spinner ? "spinner": "barrier");

            telemetry.addData("", ""); // blank line to separate configuration from actual telemetry

            telemetry.addData("parking zone, 1=left 2=mid, 3=right", String.format("%d", parking_zone));

            if (captureWhenAvailable) {
                Bitmap bmp = frameQueue.poll();
                if (bmp != null) {
                    //captureWhenAvailable = false;
                    onNewFrame(bmp);
                }
            }
            idle();
//            if (gamepad1.dpad_left) {
//                is_blue = false;
//                is_red = true;
//            }
//            if (gamepad1.dpad_right) {
//                is_red = false;
//                is_blue = true;
//            }
//            if(gamepad1.dpad_up) {
//              skip_duck = true;
//            }
//            if(gamepad1.dpad_down) {
//                skip_duck = false;
//            }
            if (gamepad1.a) {
                wait_choice = true;
            }
            if(gamepad1.b) {
                wait_choice = false;
            }
            if(gamepad1.x) {
                strafe_left = true;
            }
            if(gamepad1.y) {
                strafe_left = false;
            }
//            if(gamepad1.right_bumper) {
//                just_duck = true;
//            }
//            if(gamepad1.left_bumper) {
//                just_duck = false;
//            }
//            if(gamepad1.right_trigger>.5) {
//                visionary_auto = true;
//            }
//            if(gamepad1.left_trigger>.5){
//                visionary_auto=false;
//            }
//            if(gamepad1.start){
//                vision_spinner=false;
//            }
//            if(gamepad1.back){
//                vision_spinner=true;
//            }

            telemetry.update();
        }
    }


    private void drive(double speed, double strafe, double rotate, long milis){

//        if (is_blue == true) {
//            strafe = -strafe;
//        }

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

    /** Do something with the frame */
    private void onNewFrame(Bitmap bitmap) {
        int signalAverageColor = MeasureRectangle(bitmap, SIGNAL_X, SIGNAL_W, SIGNAL_Y, SIGNAL_H);
        telemetry.addData("signal Average:", String.format("%06X",signalAverageColor));

        int signalyellow= ComputeYellowness(signalAverageColor);
        int signalblue= ComputeBlueness(signalAverageColor);
        int signalpink= ComputePinkness(signalAverageColor);
        telemetry.addData("yellow quantity:", String.format("%06X", signalyellow));
        telemetry.addData("pink quantity:", String.format("%06X", signalpink));
        telemetry.addData("blue quantity:", String.format("%06X", signalblue));
        telemetry.update();

        if( signalyellow>signalblue&& signalyellow>signalpink){
            parking_zone=3;
        }
        if( signalblue>signalyellow&& signalblue>signalpink){
            parking_zone=1;
        }
        if( signalpink>signalblue&& signalpink>signalyellow){
            parking_zone=2;
        }

        if (cameraStreamRequestContinuation != null) {
            DrawRectangle(bitmap, SIGNAL_X, SIGNAL_W, SIGNAL_Y, SIGNAL_H);
            cameraStreamRequestContinuation.dispatch(new ContinuationResult<Consumer<Bitmap>>() {
                @Override
                public void handle(Consumer<Bitmap> consumer) {
                    consumer.accept(bitmap);
                }
            });
            cameraStreamRequestContinuation = null;
            saveBitmap(bitmap);
        } else {
            bitmap.recycle(); // not strictly necessary, but helpful
        }
    }
    private int ComputeYellowness (int color){
        int red = (color & 0x00FF0000) >> 16;
        int green = (color & 0x0000FF00) >> 8;
        int blue = (color & 0x000000FF);
        return red+green-blue;
    }
    private int ComputeBlueness (int color){
        int red = (color & 0x00FF0000) >> 16;
        int green = (color & 0x0000FF00) >> 8;
        int blue = (color & 0x000000FF);
        return blue+green-red;
    }
    private int ComputePinkness (int color){
        int red = (color & 0x00FF0000) >> 16;
        int green = (color & 0x0000FF00) >> 8;
        int blue = (color & 0x000000FF);
        return red+blue-green;
    }

    private void DrawRectangle(Bitmap bitmap, int x, int w, int y, int h) {
        DrawHLine(bitmap, x, y - 1, w);
        DrawHLine(bitmap, x, y + h, w);
        DrawVLine(bitmap, x - 1, y - 1, h + 2);
        DrawVLine(bitmap, x + w, y - 1, h + 2);
    }

    private void DrawHLine(Bitmap bitmap, int x0, int y, int w) {
        for (int x = x0; x < x0 + w; x++) {
            bitmap.setPixel(x, y, 0xFFAF00AF);
        }
    }

    private void DrawVLine(Bitmap bitmap, int x, int y0, int h) {
        for (int y = y0; y < y0 + h; y++) {
            bitmap.setPixel(x, y, 0xFFAF00AF);
        }
    }

    private int MeasureRectangle(Bitmap bitmap, int x, int w, int y, int h) {
        int totalRed = 0;
        int totalGreen = 0;
        int totalBlue = 0;

        for(int runningX = x; runningX < x+w; runningX++){
            for(int runningY = y; runningY < y+h; runningY++){
                int pixel = bitmap.getPixel(runningX, runningY);
                int red = (pixel & 0x00FF0000) >> 16;
                int green = (pixel & 0x0000FF00) >> 8;
                int blue = (pixel & 0x000000FF);
                totalRed = totalRed + red;
                totalGreen = totalGreen + green;
                totalBlue = totalBlue + blue;

            }
        }

        totalRed = totalRed/(w*h);
        totalGreen = totalGreen/(w*h);
        totalBlue = totalBlue/(w*h);
        int totalAverage = (totalRed << 16) | (totalGreen << 8) | (totalBlue);
        return totalAverage;
    }

    //----------------------------------------------------------------------------------------------
    // Camera operations
    //----------------------------------------------------------------------------------------------

    private void initializeFrameQueue(int capacity) {
        /** The frame queue will automatically throw away bitmap frames if they are not processed
         * quickly by the OpMode. This avoids a buildup of frames in memory */
        frameQueue = new EvictingBlockingQueue<Bitmap>(new ArrayBlockingQueue<Bitmap>(capacity));
        frameQueue.setEvictAction(new Consumer<Bitmap>() {
            @Override public void accept(Bitmap frame) {
                // RobotLog.ii(TAG, "frame recycled w/o processing");
                frame.recycle(); // not strictly necessary, but helpful
            }
        });
    }

    private void openCamera() {
        if (camera != null) return; // be idempotent

        Deadline deadline = new Deadline(secondsPermissionTimeout, TimeUnit.SECONDS);
        camera = cameraManager.requestPermissionAndOpenCamera(deadline, cameraName, null);
        if (camera == null) {
            error("camera not found or permission to use not granted: %s", cameraName);
        }
    }

    private void startCamera() {
        if (cameraCaptureSession != null) return; // be idempotent

        /** YUY2 is supported by all Webcams, per the USB Webcam standard: See "USB Device Class Definition
         * for Video Devices: Uncompressed Payload, Table 2-1". Further, often this is the *only*
         * image format supported by a camera */
        final int imageFormat = ImageFormat.YUY2;

        /** Verify that the image is supported, and fetch size and desired frame rate if so */
        CameraCharacteristics cameraCharacteristics = cameraName.getCameraCharacteristics();
        if (!contains(cameraCharacteristics.getAndroidFormats(), imageFormat)) {
            error("image format not supported");
            return;
        }
        final Size size = cameraCharacteristics.getDefaultSize(imageFormat);
        final int fps = cameraCharacteristics.getMaxFramesPerSecond(imageFormat, size);

        /** Some of the logic below runs asynchronously on other threads. Use of the synchronizer
         * here allows us to wait in this method until all that asynchrony completes before returning. */
        final ContinuationSynchronizer<CameraCaptureSession> synchronizer = new ContinuationSynchronizer<>();
        try {
            /** Create a session in which requests to capture frames can be made */
            camera.createCaptureSession(Continuation.create(callbackHandler, new CameraCaptureSession.StateCallbackDefault() {
                @Override public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        /** The session is ready to go. Start requesting frames */
                        final CameraCaptureRequest captureRequest = camera.createCaptureRequest(imageFormat, size, fps);
                        session.startCapture(captureRequest,
                                new CameraCaptureSession.CaptureCallback() {
                                    @Override public void onNewFrame(@NonNull CameraCaptureSession session, @NonNull CameraCaptureRequest request, @NonNull CameraFrame cameraFrame) {
                                        /** A new frame is available. The frame data has <em>not</em> been copied for us, and we can only access it
                                         * for the duration of the callback. So we copy here manually. */
                                        Bitmap bmp = captureRequest.createEmptyBitmap();
                                        cameraFrame.copyToBitmap(bmp);
                                        frameQueue.offer(bmp);
                                    }
                                },
                                Continuation.create(callbackHandler, new CameraCaptureSession.StatusCallback() {
                                    @Override public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, CameraCaptureSequenceId cameraCaptureSequenceId, long lastFrameNumber) {
                                        RobotLog.ii(TAG, "capture sequence %s reports completed: lastFrame=%d", cameraCaptureSequenceId, lastFrameNumber);
                                    }
                                })
                        );
                        synchronizer.finish(session);
                    } catch (CameraException |RuntimeException e) {
                        RobotLog.ee(TAG, e, "exception starting capture");
                        error("exception starting capture");
                        session.close();
                        synchronizer.finish(null);
                    }
                }
            }));
        } catch (CameraException|RuntimeException e) {
            RobotLog.ee(TAG, e, "exception starting camera");
            error("exception starting camera");
            synchronizer.finish(null);
        }

        /** Wait for all the asynchrony to complete */
        try {
            synchronizer.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        /** Retrieve the created session. This will be null on error. */
        cameraCaptureSession = synchronizer.getValue();
    }

    private void stopCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.stopCapture();
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
    }

    private void closeCamera() {
        stopCamera();
        if (camera != null) {
            camera.close();
            camera = null;
        }
    }

    //----------------------------------------------------------------------------------------------
    // Utilities
    //----------------------------------------------------------------------------------------------

    private void error(String msg) {
        telemetry.log().add(msg);
        telemetry.update();
    }
    private void error(String format, Object...args) {
        telemetry.log().add(format, args);
        telemetry.update();
    }

    private boolean contains(int[] array, int value) {
        for (int i : array) {
            if (i == value) return true;
        }
        return false;
    }

    private void saveBitmap(Bitmap bitmap) {
        File file = new File(captureDirectory, String.format(Locale.getDefault(), "webcam-frame-%d.jpg", captureCounter++));
        try {
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                telemetry.log().add("captured %s", file.getName());
            }
        } catch (IOException e) {
            RobotLog.ee(TAG, e, "exception in saveBitmap()");
            error("exception saving %s", file.getName());
        }
    }


}
