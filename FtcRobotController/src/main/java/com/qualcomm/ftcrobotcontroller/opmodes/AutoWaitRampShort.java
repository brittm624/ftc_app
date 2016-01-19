package com.qualcomm.ftcrobotcontroller.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorController;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

public class AutoWaitRampShort extends OpMode {

  private static int MotorClicksPerRev = 1120;
  private static double GearRatio = 1;
  private static double ClicksPerRev = MotorClicksPerRev * GearRatio;
  private static double FeetPerWheelRev = 1.0;
  private static double FeetPerSpin = 4.2;
  private static double FeetPerCircle = 22.9;

  private static double InsideRatio = 5.0/8.0; // 4' outer-radius turn.

  private static double Speed = 0.4;
  private static double InnerSpeed = Speed * InsideRatio;

  private static double SecondsPerFoot = 0.5 / Speed;

  private enum State {
    Begin,
    Rest,
    Depart,
    Align,
    Approach,
    Dock
  }

  private static double DepartDist   = 8.0;
  private static double AlignDist    = FeetPerSpin * 135.0 / 360.0;
  private static double ApproachDist = 1.0;

  private static double DepartDur   =  DepartDist   * SecondsPerFoot;
  private static double RestDur     =  20.0; // seconds
  private static double AlignDur    =  AlignDist    * SecondsPerFoot;
  private static double ApproachDur =  ApproachDist * SecondsPerFoot;

  private static double RestEnd     = RestDur;
  private static double DepartEnd   = RestEnd   + DepartDur;
  private static double AlignEnd    = DepartEnd + AlignDur;
  private static double ApproachEnd = AlignEnd  + ApproachDur;

  private ElapsedTime timer = new ElapsedTime();

  private DcMotor rightWheel;
  private DcMotor leftWheel;

  private State state = State.Begin;

  private double leftPower  = 0.0;
  private double rightPower = 0.0;

  private double leftPosFt  = 0.0;
  private double rightPosFt = 0.0;

  private void setLeftPower(double pwr) {
    leftPower = Range.clip(pwr,  -1, 1);
    leftWheel.setPower(leftPower);
  }

  private void setRightPower(double pwr) {
    rightPower = Range.clip(pwr,  -1, 1);
    rightWheel.setPower(rightPower);
  }

  private void setPower(double left, double right) {
    setLeftPower(left);
    setRightPower(right);
  }

  private void setWheelMode(DcMotorController.RunMode mode) {
    rightWheel.setMode(mode);
    leftWheel.setMode(mode);
  }

  private static int Clicks(double ft)
    { return (int)(ft / FeetPerWheelRev * ClicksPerRev + 0.5); }

  private void setTarget(double leftFt, double rightFt) {
    int left  = Clicks(leftFt);
    int right = Clicks(rightFt);
    leftWheel.setTargetPosition(left);
    rightWheel.setTargetPosition(right);
  }

  private void move(double ft) {
    leftPosFt  += ft;
    rightPosFt += ft;
    setPower(Speed, Speed);
    setTarget(leftPosFt, rightPosFt);
  }

  private void turnLeft(double ft) {
    leftPosFt  += ft * InsideRatio;
    rightPosFt += ft;
    setPower(InnerSpeed, Speed);
    setTarget(leftPosFt, rightPosFt);
  }

  private void turnRight(double ft) {
    leftPosFt  += ft;
    rightPosFt += ft * InsideRatio;
    setPower(Speed, InnerSpeed);
    setTarget(leftPosFt, rightPosFt);
  }

  private void spin(double rev) {
    double d = rev * FeetPerSpin;
    leftPosFt  += d;
    rightPosFt -= d;
    setPower(Speed, Speed);
    setTarget(leftPosFt, rightPosFt);
  }

  public AutoWaitRampShort() { }

  @Override
  public void init() {
    rightWheel = hardwareMap.dcMotor.get("wheel_right");
    leftWheel  = hardwareMap.dcMotor.get("wheel_left");

    rightWheel.setDirection(DcMotor.Direction.REVERSE);
    leftWheel.setDirection(DcMotor.Direction.FORWARD);

    setWheelMode(DcMotorController.RunMode.RESET_ENCODERS);
  }

  @Override
  public void start() {
    setPower(0.0, 0.0);
    setWheelMode(DcMotorController.RunMode.RUN_TO_POSITION);
    leftPosFt  = 0.0;
    rightPosFt = 0.0;
    state = State.Begin;
  }

  @Override
  public void loop() {
    switch (state) {
      case Begin:
        timer.reset();
        state = State.Rest;
        // fall thru
      case Rest:
        if (timer.time() < RestEnd)
          break;
        move(DepartDist);
        state = State.Depart;
        // fall thru
      case Depart:
        if (timer.time() < DepartEnd)
          break;
        spin(-135.0 / 360);
        state = State.Align;
        // fall thru
      case Align:
        if (timer.time() < AlignEnd)
          break;
        move(ApproachDist);
        state = State.Approach;
        // fall thru
      case Approach:
        if (timer.time() < ApproachEnd)
          break;
        setPower(0.0, 0.0);
        state = State.Dock;
        // fall thru
      case Dock:
        break;
    }

    telemetry.addData("state", String.format("state: %s", state));
    telemetry.addData("left",  String.format("%.2f", leftPosFt));
    telemetry.addData("right", String.format("%.2f", rightPosFt));
  }

  @Override
  public void stop() { setPower(0.0, 0.0); }

} // AutoWaitRampShort
