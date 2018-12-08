/*
 * Copyright 2018 Mobile Advanced Systems Inc.
 *
 * Developer Credit: Dennis Agostino
 * Contact: mobileadvancedsystems at gmail dot com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project donations are graciously accepted at:
 *
 *
 * https://paypal.me/mobileadvancedsystem
 *
 */

/*
Add this as a package to your project.
How To Create/Add New Package Inside Src Folder In Android Studio

    Step 3: Open App folder then open Java folder. Right click on Java folder and select New > Package.
    Step 4: Choose directory destination which main\java and click OK.
    Step 5: Give a name to new Package(For example: samplePackage). Click Ok.
    Step 5: Now you have samplePackage inside JAVA folder.
 */

/*
The code in this driver was derived directly from the TI Document: http://www.ti.com/lit/ds/symlink/drv2605.pdf
Also to Adafruit: https://learn.adafruit.com/adafruit-drv2605-haptic-controller-breakout?view=all
And Sparkfun boards. At the time of this driver writing, it wasn't tested on Sparkfun boards, just Adafruit.
*/
package hapticdrv2605;

import android.util.Log;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
//import android.support.annotation.IntDef;
//import android.support.annotation.VisibleForTesting;
//import java.lang.annotation.Retention;
//import java.lang.annotation.RetentionPolicy;


public class hapticdrv2605 implements AutoCloseable {

    /*
     * Address of our device
     */
    public static final int DEFAULT_I2C_ADDRESS = 0x5A;//The DRV2605 slave address is 0x5A (7-bit), or 1011010 in binary.

    public hapticdrv2605(String i2cBusName) throws IOException {
        this(i2cBusName, DEFAULT_I2C_ADDRESS);
    }

    public hapticdrv2605(String i2cBusName, int i2cAddress) throws IOException {
        PeripheralManager pioService = PeripheralManager.getInstance();
        I2cDevice device = pioService.openI2cDevice(i2cBusName, i2cAddress);
        try {
            initialize(device);
        } catch (IOException | RuntimeException e) {
            try {
                close();
            } catch (IOException | RuntimeException ignored) {
            }
            throw e;
        }
    }
    private I2cDevice mI2cDevice;
    /*
       Enable power to the device. Some devices Enable pin. Connect to VCC for most applications ie hardwired to ON.
    */
    private Gpio mGPIO_HAPTIC_ENABLE;

    private Gpio GPIO_ANALOG_PWM_INPUT;//	Analog and PWM signal input - play/vibrate music through the IN pin which takes PWM analog signals.

    private static final String I2C_HAPTIC_ENABLE_PIN_NAME = "BCM20";//   "BCM20" PHYSICAL PIN 38,  Wiring Pi pin 28

    private static int audio_haptics_enabled;
    private static boolean vibrator_is_playing;
    public boolean sparkfunboard = false;

    //-------------------------------------------------------------------------------------------------
    private static final int STATUS_REG 				        = 0x00; //STATUS REGISTER
    private static final int MODE_REG 					        = 0x01;	//MODE REGISTER
    private static final int RTP_INPUT_REG 				        = 0x02;	//REAL TIME PLAYBACK INPUT REGISTER
    private static final int LIBRARY_SELECT_REG 			    = 0x03; //Library Select REGISTER
    private static final int WAVEFORM_SEQ_REG1 				    = 0x04;	//Waveform Sequencer REGISTER 0x04 to 0x0B
    private static final int WAVEFORM_SEQ_REG2 				    = 0x05; //Waveform Sequencer REGISTER 0x04 to 0x0B
    private static final int WAVEFORM_SEQ_REG3 				    = 0x06; //Waveform Sequencer REGISTER 0x04 to 0x0B
    private static final int WAVEFORM_SEQ_REG4 				    = 0x07; //Waveform Sequencer REGISTER 0x04 to 0x0B
    private static final int WAVEFORM_SEQ_REG5 				    = 0x08; //Waveform Sequencer REGISTER 0x04 to 0x0B
    private static final int WAVEFORM_SEQ_REG6 				    = 0x09; //Waveform Sequencer REGISTER 0x04 to 0x0B
    private static final int WAVEFORM_SEQ_REG7 				    = 0x0A; //Waveform Sequencer REGISTER 0x04 to 0x0B
    private static final int WAVEFORM_SEQ_REG8 				    = 0x0B; //Waveform Sequencer REGISTER 0x04 to 0x0B
    private static final int GO_REG 					        = 0x0C; //GO BIT REGISTER
    private static final int OVERDRIVE_TIME_OFFSET_REG 	   	    = 0x0D; //Overdrive Time Offset Register
    private static final int SUSTAIN_TIME_OFFSET_POSITIVE_REG 	= 0x0E; //Sustain Time REGISTER, Positive Register
    private static final int SUSTAIN_TIME_OFFSET_NEGATIVE_REG   = 0x0F; //Sustain Time REGISTER, Negative Register
    private static final int BREAK_TIME_OFFSET_REG 			    = 0x10; //BRAKE TIME OFFSET BIT REGISTER
    private static final int AUDIO_TO_VIBE_CTRL_REG 			= 0x11; //AUDIO TO VIBE CONTROL REGISTER
    private static final int AUDIO_TO_VIBE_MIN_INPUT_LEVEL_REG 	= 0x12; //AUDO TO VIBE MINIMUM INPUT LEVEL REGISTER
    private static final int AUDIO_TO_VIBE_MAX_INPUT_LEVEL_REG 	= 0x13; //AUDO TO VIBE MAXIMUM INPUT LEVEL REGISTER
    private static final int AUDIO_TO_VIBE_MINIMUM_OUTPUT_DRIVE_REG = 0x14; //AUDIO TO VIBE MINIMUM OUTPUT DRIVE REGISTER
    private static final int AUDIO_TO_VIBE_MAXIMUM_OUTPUT_DRIVE_REG = 0x15; //AUDIO TO VIBE MAXIMUM OUTPUT DRIVE REGISTER
    private static final int RATED_VOLTAGE_REG 				    = 0x16; //RATED VOLTAGE REGISTER
    private static final int OVERDRIVE_CLAMP_VOLTAGE_REG 		= 0x17; //OVERDRIVE CLAMP VOLTAGE REGISTER
    private static final int AUTOCALIBRATION_COMP_RESULT_REG 	= 0x18; //AUTO CALIBRATION COMPENSATION RESULT REGISTER
    private static final int AUTOCALIBRATION_BACK_EMF_RESULT_REG= 0x19; //AUTO CALIBRATION BACK EMF RESULT REGISTER
    private static final int FEEDBACK_CONTROL_REG 			    = 0x1A; //FEEDBACK CONTROL REGISTER
    private static final int CONTROL1_REG 				        = 0x1B; //CONTROL1 REGISTER
    private static final int CONTROL2_REG 				        = 0x1C; //CONTROL2 REGISTER
    private static final int CONTROL3_REG 				        = 0x1D; //CONTROL3 REGISTER
    private static final int CONTROL4_REG 				        = 0x1E; //CONTROL4 REGISTER
    private static final int Vbat_VOLTAGE_MONITOR_REG 			= 0x21; //V BAT VOLTAGE MONITOR REGISTER
    private static final int LRA_RESONANCE_PERIOD_REG 			= 0x22; //LRA RESONANCE PERIOD REGISTER

//-------------------------------------------------------------------------------------------------

    //Mode register
    private static final int  MODE_REG_INTTRIG      =0x00;
    private static final int  MODE_REG_EXTTRIGEDGE  =0x01;
    private static final int  MODE_REG_EXTTRIGLVL   =0x02;
    private static final int  MODE_REG_PWMANALOG    =0x03;
    private static final int  MODE_REG_AUDIOVIBE    =0x04;
    private static final int  MODE_REG_REALTIME     =0x05;
    private static final int  MODE_REG_DIAGNOS      =0x06;
    private static final int  MODE_REG_AUTOCAL      =0x07;


/*
The DRV2605 device features the TI haptic broadcast mode where the DRV2605 responds to the slave address
0x58 (7-bit) or 1011000. This mode is useful in the event that multiple drivers implementing the TI haptic
broadcast mode are installed in the system. In such a scenario, writing the GO bit to the TI haptic broadcast
slave address will cause all haptic drivers to trigger the process  at the same time.
*/




    //Three types of motors: http://www.ti.com/lit/an/sloa194/sloa194.pdf
    //ERM, LRA, PIEZO

    //ERM - Eccentric rotating mass - classic cellphone vibrator wih a armature and disc
    private static final int EFFECT_LIBRARY_A__ERM_RATED_VOLTAGE 		        = 0X3E;
    private static final int EFFECT_LIBRARY_A__ERM_OVERDRIVE_CLAMP_VOLTAGE  	= 0X90;

    private static final int EFFECT_LIBRARY_B__ERM_RATED_VOLTAGE 		        = 0X90;
    private static final int EFFECT_LIBRARY_B__ERM_OVERDRIVE_CLAMP_VOLTAGE 	    = 0X90;

    private static final int EFFECT_LIBRARY_C__ERM_RATED_VOLTAGE 		        = 0X90;
    private static final int EFFECT_LIBRARY_C__ERM_OVERDRIVE_CLAMP_VOLTAGE 	    = 0X90;

    private static final int EFFECT_LIBRARY_D__ERM_RATED_VOLTAGE 		        = 0X90;
    private static final int EFFECT_LIBRARY_D__ERM_OVERDRIVE_CLAMP_VOLTAGE 	    = 0X90;

    private static final int EFFECT_LIBRARY_E__ERM_RATED_VOLTAGE 		        = 0X90;
    private static final int EFFECT_LIBRARY_E__ERM_OVERDRIVE_CLAMP_VOLTAGE 	    = 0X90;

    private static final int EFFECT_LIBRARY_GENERIC__ERM_RATED_VOLTAGE 		    = 0X90;
    private static final int EFFECT_LIBRARY_GENERIC__ERM_OVERDRIVE_CLAMP_VOLTAGE= 0X90;


    //Linear Resonator Activator - small watch battery sized vibrator
    private static final int LRA_RATED_VOLTAGE = 0X56;
    private static final int LRA_OVERDRIVE_CLAMP_VOLTAGE=0X90;

    private static final int LRA_RTP_STRENGTH=0X7F;
    private static final int LRA_RATED_VOLTAGE_SEMCO0934=0X51;
    private static final int LRA_OVERDRIVE_CLAMP_VOLTAGE_SEMCO09434=0X72;
    private static final int SKIP_LRA_AUTOCAL =1;
    private static final int GO_BIT_POLL_INTERVAL =15;
    private static final int REAL_TIME_PLAYBACKSTRENGTH_LIBA=0X7F;
    private static final int REAL_TIME_PLAYBACKSTRENGTH_LIBB=0X7F;
    private static final int REAL_TIME_PLAYBACKSTRENGTH_LIBC=0X7F;
    private static final int REAL_TIME_PLAYBACKSTRENGTH_LIBD=0X7F;
    private static final int REAL_TIME_PLAYBACKSTRENGTH_LIBE=0X7F;
    private static final int REAL_TIME_PLAYBACKSTRENGTH_LIBF=0X7F;

    public static final int MAX_TIMEOUT =15000;	/* 15s */
    public static final int DEFAULT_EFFECT =14;	/* Strong buzz 100% */
    public static final int RTP_CLOSED_LOOP_ENABLE =0; /* Set closed loop mode for RTP */
    public static final int RTP_ERM_OVERDRIVE_CLAMP_VOLTAGE =    0xF0;
    public static final int I2C_RETRY_DELAY	=	20; /* ms */
    public static final int I2C_RETRIES	=	5;

//-------------------------------------------------------------------------------------------------

    /*DRV2605L board has the following pins.
        GND	Connect to ground.
        VCC	Used to power the DRV2605L Haptic Motor Driver. Must be between 2.0 - 5.2V
        SDA	I2C data
        SCL	I2C clock
        IN	Analog and PWM signal input
        EN	Enable pin. Connect to VCC for most applications.
        O-	Negative motor terminal.
        O+	Positive motor terminal.

        The Adafruit board has the following:

        Power Pins

        The motor driver/controller on the breakout requires 3-5V power. You can use either, whichever logic level you use on your embedded processor

            Vin - To power the board, give it the same power as the logic level of your microcontroller - e.g. for a 5V micro like Arduino, use 5V
            GND - common ground for power and logic

        I2C Pins

            SCL - I2C clock pin, connect to your microcontrollers I2C clock line. This pin can be used with 3V or 5V logic, and there's a 10K pullup on this pin.
            SDA - I2C data pin, connect to your microcontrollers I2C data line. This pin can be used with 3V or 5V logic, and there's a 10K pullup on this pin.

        Other!

            IN/TRIG - This is a general purpose pin that can be used for a couple different uses. One use is to read analog audio in to control the audio-to-haptic code. Another use is to 'trigger' the effects to go rather than sending a I2C command.

            It is not necessary to wire up the IN/TRIG pin. You can leave this as a blank connection.
            It's just that you won't have certain functionality enabled if you do.

    */
    private void initialize(I2cDevice device) throws IOException {
        mI2cDevice = device;

        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open- initialization");
        }

        try {
            TimeUnit.MICROSECONDS.sleep(250);//Wait at least 250 uS before i2c device begins to accept commands.
        }
        catch(Exception ex) {
        }

        try {

            audio_haptics_enabled=0;
            vibrator_is_playing=false;
            sparkfunboard=false;
            //Device Reset
            //mI2cDevice.writeRegByte(MODE_REG, (byte) 0x10000000);// Bit self clears after reset. Reset clears all registers.

            //Assert ENable pin High to Enable the I2C device
            //Create GPIO connection to the device ENable pin.
            //Note: Adafruit https://learn.adafruit.com/adafruit-drv2605-haptic-controller-breakout/pinouts is hard wired to on.
            //https://learn.sparkfun.com/tutorials/haptic-motor-driver-hook-up-guide/all board has an enable pin for a GPIO hookup.
            if(sparkfunboard) {
                PeripheralManager manager = PeripheralManager.getInstance();
                mGPIO_HAPTIC_ENABLE = manager.openGpio(I2C_HAPTIC_ENABLE_PIN_NAME);
                mGPIO_HAPTIC_ENABLE.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
                //mGPIO_HAPTIC_ENABLE.setActiveType(Gpio.ACTIVE_HIGH);// High voltage is considered active   	--disabled. output gpio only
                //mGPIO_HAPTIC_ENABLE.setEdgeTriggerType(Gpio.EDGE_BOTH);					--disabled. output gpio only
                //mGPIO_HAPTIC_ENABLE.registerGpioCallback(mI2CHAPTICCallback);					--disabled. output gpio only
            }




            // check status of DIAG_RESULT (3rd bit) in register 0x00 (=DRV2605_REG_STATUS)
            //uint8_t res = drv.readRegister8(DRV2605_REG_STATUS) & 0x08;
            //int statusreg=mI2cDevice.readRegByte(STATUS_REG)&0x08;
            //byte status = mI2cDevice.readRegByte(STATUS_REG);//Read the diag results to ensure auto calibration is completed without faults.

            //Log.d("HAPTIC I2C", "Read I2C DRV 2605 status byte: 0x" + Integer.toHexString(statusreg));//0xE0 should be good
            // byte statusreg = mI2cDevice.readRegByte(STATUS_REG);//Read the diag results to ensure auto calibration is completed without faults.
            // Log.d("HAPTIC I2C", "Read I2C DRV 2605 status byte: 0x" + String.format("%02X", statusreg));//0xE0 should be good


            // try {
            //    TimeUnit.MICROSECONDS.sleep(200);//
            // }
            // catch(Exception ex) {
            //}




            // boolean test=false;
            //  if (test) {
                //Write the mode register (0x01) a value of 0x00 to remove the device from standby mode. 0=Device Ready
                mI2cDevice.writeRegByte(MODE_REG, (byte) 0);

                //Auto Calibration Procedure - this depends on the type of haptic motor you use.

                //https://www.adafruit.com/product/1201 The default motor used for this driver. Vibrating mini disc motor.
                //Ensure Enable pin is set to high
                //Take device out of standby. This places MODE[2:0] bits in auto-calibration mode
                //ERM_LRA 		— selection will depend on desired actuator

                //To configure the DRV2605 device in ERM open-loop operation, the ERM must be selected by writing the N_ERM_LRA bit to 0 (in register 0x1A),
                //and the ERM_OPEN_LOOP bit to 1 in register 0x1D.
                //N_ERM_LRA This bit sets the DRV2605 device in ERM or LRA mode. This bit should be set prior to running auto calibration. 0: ERM Mode 1: LRA Mode
                //mI2cDevice.writeRegByte(FEEDBACK_CONTROL_REG, (byte) 0x00000000);//0x00 - Select ERM as opposed to LRA
                mI2cDevice.writeRegByte(CONTROL3_REG, (byte) 0x00100000);//ERM_OPEN_LOOP BIT to 1 - bit 5   readRegister8(DRV2605_REG_CONTROL3) | 0x20

                // ERM open loop

                // turn off N_ERM_LRA
                //writeRegister8(DRV2605_REG_FEEDBACK, readRegister8(DRV2605_REG_FEEDBACK) & 0x7F);
                // turn on ERM_OPEN_LOOP
                //writeRegister8(DRV2605_REG_CONTROL3, readRegister8(DRV2605_REG_CONTROL3) | 0x20);


                //FB_BRAKE_FACTOR[2:0] 	— A value of 2:3x is valid for most actuators.
                //mI2cDevice.writeRegByte(FEEDBACK_CONTROL_REG, (byte) 0x00100000);//   select 2:3x

                //LOOP_GAIN[1:0] 	— A value of 2 is valid for most actuators.
                mI2cDevice.writeRegByte(FEEDBACK_CONTROL_REG, (byte) 0x00101000);//0x28 Select ERM, select 2:3x, set to HIGH readRegister8(DRV2605_REG_FEEDBACK) & 0x7F  0x01111111

                //RATED_VOLTAGE[7:0] 	— See the Rated Voltage Programming section for calculating the correct register value.
                mI2cDevice.writeRegByte(RATED_VOLTAGE_REG, (byte) EFFECT_LIBRARY_A__ERM_RATED_VOLTAGE);

                //OD_CLAMP[7:0] 	— See the Overdrive Voltage-Clamp Programming section for calculating the correct register value.
                mI2cDevice.writeRegByte(OVERDRIVE_CLAMP_VOLTAGE_REG, (byte) EFFECT_LIBRARY_A__ERM_OVERDRIVE_CLAMP_VOLTAGE);

                //AUTO_CAL_TIME[1:0] 	— A value of 3 is valid for most actuators.
                mI2cDevice.writeRegByte(CONTROL4_REG, (byte) 0x00100000);

                //DRIVE_TIME[3:0] 	— See the Drive-Time Programming for calculating the correct register value. Drive time (ms) = DRIVE_TIME[4:0] × 0.1 ms + 0.5 ms.
                //ERM Mode : Sets the sample rate for the back-EMF detection. Lower drive times cause higher peak-to-average ratios
                //in the output signal, requiring more supply headroom. Higher drive times cause the feedback to react at a slower rate.
                //Drive Time (ms) = DRIVE_TIME[4:0] × 0.2 ms + 1 ms
                mI2cDevice.writeRegByte(CONTROL1_REG, (byte) 0x00000010);//set arbitrary value to 2

                //SAMPLE_TIME[1:0] 	— A value of 3 is valid for most actuators.
                //mI2cDevice.writeRegByte(CONTROL2_REG, 0x00110000);//set arbitrary value to 2

                //BLANKING_TIME[1:0]	— A value of 1 is valid for most actuators.
                //mI2cDevice.writeRegByte(CONTROL2_REG, 0x00110100);

                //DISS_TIME[1:0]	— A value of 1 is valid for most actuators. Current dissipation time between PWM cycles.
                mI2cDevice.writeRegByte(CONTROL2_REG, (byte) 0x00110101);

                //Set the GO bit (write 0x01 to register 0x0C) to start the auto-calibration process. When auto calibration is complete, the GO bit automatically
                //clears. The auto-calibration results are written in the respective registers
                mI2cDevice.writeRegByte(GO_REG, (byte) 0x00000001);//0x01

                //Auto calibration results written to status register 0x00 bit 3
                //Read status register
                byte status = mI2cDevice.readRegByte(STATUS_REG);//Read the diag results to ensure auto calibration is completed without faults.
                if ((status == (byte)0xE0)||(status == (byte)0xE4)) {
                    Log.d("HAPTIC I2C", "Read I2C DRV 2605 status byte: 0x" + String.format("%02X", status) + " Healthy Device");//0xE0 should be good
                }
                else if(status == (byte)0xFF){
                    //Auto Calibration Failed - Actuator Not present
                    //Feedback Controller has timed out.
                    //Device Exceeded Temperature threshold
                    //Overcurrent event detected
                    Log.d("HAPTIC I2C", "Read I2C DRV 2605 status byte: 0x" + String.format("%02X", status) + " Faulty Device");//0xE0 should be good
                    Log.d("HAPTIC I2C", "I2C Auto Calibration Failed - Actuator Not present. ");
                    Log.d("HAPTIC I2C", "I2C Feedback controller timed out.");
                    Log.d("HAPTIC I2C", "I2C Device Exceeded Temperature Threshold");
                    Log.d("HAPTIC I2C", "I2C Overcurrent event detected.");
                }
                else if(status == (byte)0xEF){
                    //Auto Calibration Failed - Actuator Not present
                    //Feedback Controller has timed out.
                    //Device Exceeded Temperature threshold
                    //Overcurrent event detected
                    Log.d("HAPTIC I2C", "Read I2C DRV 2605 status byte: 0x" + String.format("%02X", status) + " Faulty Device");//0xE0 should be good
                    Log.d("HAPTIC I2C", "I2C Auto Calibration Failed - Actuator Not present. ");
                    Log.d("HAPTIC I2C", "I2C Feedback controller timed out.");
                    Log.d("HAPTIC I2C", "I2C Device Exceeded Temperature Threshold");
                    Log.d("HAPTIC I2C", "I2C Overcurrent event detected.");
                }
                else if(status == (byte)0xE7){
                    //Auto Calibration Failed - Actuator Not present
                    //Feedback Controller has timed out.
                    //Device Exceeded Temperature threshold
                    //Overcurrent event detected
                    Log.d("HAPTIC I2C", "Read I2C DRV 2605 status byte: 0x" + String.format("%02X", status) + " Faulty Device");//0xE0 should be good
                    Log.d("HAPTIC I2C", "I2C Auto Calibration PASSED ");
                    Log.d("HAPTIC I2C", "I2C Feedback controller timed out.");
                    Log.d("HAPTIC I2C", "I2C Device Exceeded Temperature Threshold");
                    Log.d("HAPTIC I2C", "I2C Overcurrent event detected.");
                }
                else if(status == (byte)0xE8){
                    Log.d("HAPTIC I2C", "Read I2C DRV 2605 status byte: 0x" + String.format("%02X", status) + " Faulty Device");//0xE0 should be good
                    Log.d("HAPTIC I2C", "I2C Auto Calibration Failed - Actuator Not present. ");
                }
                //else if(status == (byte)0xE4){
                    //This bit is for debug purposes only, and may sometimes be set
                    //under normal operation when extensive braking periods are used.
                //    Log.d("HAPTIC I2C", "Read I2C DRV 2605 status byte: 0x" + String.format("%02X", status) + " Faulty Device");//0xE0 should be good
                //    Log.d("HAPTIC I2C", "I2C Feedback controller timed out. This maybe ok.");
                //}
                else if(status == (byte)0xE2){
                    Log.d("HAPTIC I2C", "Read I2C DRV 2605 status byte: 0x" + String.format("%02X", status) + " Faulty Device");//0xE0 should be good
                    Log.d("HAPTIC I2C", "I2C Device Exceeded Temperature Threshold");
                 }
                else if(status == (byte)0xE1){
                    Log.d("HAPTIC I2C", "Read I2C DRV 2605 status byte: 0x" + String.format("%02X", status) + " Faulty Device");//0xE0 should be good
                    Log.d("HAPTIC I2C", "I2C Overcurrent event detected.");
                }
                else{
                    Log.d("HAPTIC I2C", "Read I2C DRV 2605 status byte: 0x" + String.format("%02X", status) + " Defective Device");
                }

                //Write the library selection (0x03) to select a library
                mI2cDevice.writeRegByte(LIBRARY_SELECT_REG, (byte) 0);

                //The default setup is closed loop bi-directional mode.
                //Put the device in standby mode ie deassert EN pin, the user can select the desired MODE (0x01) at the same time the STANDBY bit is set.
                mI2cDevice.writeRegByte(MODE_REG, (byte) 0x01000000);//0x40 standby 1=Device in Software Standby
           // }


        }
        catch (Exception ex){
            Log.d("HAPTIC I2C", "HAPTIC I2C - INIT: " + ex.toString());
        }

    }

    @Override
    public void close() throws IOException {
        if (mI2cDevice != null) {
            try {
                mI2cDevice.close();
            } finally {
                mI2cDevice = null;
            }
        }
    }


    public byte drv260x_GET_STATUS_REGISTER() throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        byte status = mI2cDevice.readRegByte(STATUS_REG);//Read the diag results to ensure auto calibration is completed without faults.
        return status;
    }

    public void drv260x_SET_MODE_REGISTER(byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        mI2cDevice.writeRegByte(MODE_REG, (byte) value);
    }

    private void SET_RTP_INPUT_REGISTER(byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        mI2cDevice.writeRegByte(RTP_INPUT_REG, (byte) value);
    }

    public void SET_LIBRARYSELECT_REGISTER(byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        mI2cDevice.writeRegByte(LIBRARY_SELECT_REG, (byte) value);
    }

    private void SET_WAVEFORM_SEQ_REG1_REGISTER(byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        mI2cDevice.writeRegByte(WAVEFORM_SEQ_REG1, (byte) value);
    }

    private void SET_WAVEFORM_SEQ_REG2_REGISTER(byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        mI2cDevice.writeRegByte(WAVEFORM_SEQ_REG2, (byte) value);
    }

    private void SET_WAVEFORM_SEQ_REG3_REGISTER(byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        mI2cDevice.writeRegByte(WAVEFORM_SEQ_REG3, (byte) value);
    }

    private void SET_WAVEFORM_SEQ_REG4_REGISTER(byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        mI2cDevice.writeRegByte(WAVEFORM_SEQ_REG4, (byte) value);
    }

    private void SET_WAVEFORM_SEQ_REG5_REGISTER(byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        mI2cDevice.writeRegByte(WAVEFORM_SEQ_REG5, (byte) value);
    }

    private void SET_WAVEFORM_SEQ_REG6_REGISTER(byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        mI2cDevice.writeRegByte(WAVEFORM_SEQ_REG6, (byte) value);
    }

    private void SET_WAVEFORM_SEQ_REG7_REGISTER(byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        mI2cDevice.writeRegByte(WAVEFORM_SEQ_REG7, (byte) value);
    }

    private void SET_WAVEFORM_SEQ_REG8_REGISTER(byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        mI2cDevice.writeRegByte(WAVEFORM_SEQ_REG8, (byte) value);
    }

    private void SET_GO_BIT_REGISTER(byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        mI2cDevice.writeRegByte(GO_REG, (byte) value);
    }

    private void SET_OVERDRIVE_TIME_OFFSET_REGISTER(byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        mI2cDevice.writeRegByte(OVERDRIVE_TIME_OFFSET_REG, (byte) value);
    }

    private void SET_SUSTAIN_TIME_OFFSET_POSITIVE_REGISTER(byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        mI2cDevice.writeRegByte(SUSTAIN_TIME_OFFSET_POSITIVE_REG, (byte) value);
    }

    private void SET_SUSTAIN_TIME_OFFSET_NEGATIVE_REGISTER(byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        mI2cDevice.writeRegByte(SUSTAIN_TIME_OFFSET_NEGATIVE_REG, (byte) value);
    }

    private void SET_BREAK_TIME_OFFSET_REGISTER(byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        mI2cDevice.writeRegByte(BREAK_TIME_OFFSET_REG, (byte) value);
    }


    private void SET_AUDIO_TO_VIBE_CTRL_REGISTER(byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        mI2cDevice.writeRegByte(AUDIO_TO_VIBE_CTRL_REG, (byte) value);
    }

    private void SET_AUDIO_TO_VIBE_MIN_INPUT_LEVEL_REGISTER(byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        mI2cDevice.writeRegByte(AUDIO_TO_VIBE_MIN_INPUT_LEVEL_REG, (byte) value);
    }

    private void SET_AUDIO_TO_VIBE_MAX_INPUT_LEVEL_REGISTER(byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        mI2cDevice.writeRegByte(AUDIO_TO_VIBE_MAX_INPUT_LEVEL_REG, (byte) value);
    }

    private void SET_AUDIO_TO_VIBE_MINIMUM_OUTPUT_DRIVE_REGISTER(byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        mI2cDevice.writeRegByte(AUDIO_TO_VIBE_MINIMUM_OUTPUT_DRIVE_REG, (byte) value);
    }

    private void SET_AUDIO_TO_VIBE_MAXIMUM_OUTPUT_REGISTER(byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        mI2cDevice.writeRegByte(AUDIO_TO_VIBE_MAXIMUM_OUTPUT_DRIVE_REG, (byte) value);
    }

    private void SET_RATED_VOLTAGE_REGISTER(byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        mI2cDevice.writeRegByte(RATED_VOLTAGE_REG, (byte) value);
    }

    private void SET_OVERDRIVE_CLAMP_VOLTAGE_REGISTER(byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        mI2cDevice.writeRegByte(OVERDRIVE_CLAMP_VOLTAGE_REG, (byte) value);
    }

    private byte GET_AUTOCALIBRATION_COMP_RESULT_REGISTER() throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        byte compresult = mI2cDevice.readRegByte(AUTOCALIBRATION_COMP_RESULT_REG);//Read the diag results to ensure auto calibration is completed without faults.
        return compresult;
    }

    private byte GET_AUTOCALIBRATION_BACK_EMF_RESULT_REGISTER() throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        byte emfresult = mI2cDevice.readRegByte(AUTOCALIBRATION_BACK_EMF_RESULT_REG);//Read the diag results to ensure auto calibration is completed without faults.
        return emfresult;
    }

    private void SET_FEEDBACK_CONTROL_REGISTER(byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        mI2cDevice.writeRegByte(FEEDBACK_CONTROL_REG, (byte) value);
    }

    private void SET_CONTROL1_REGISTER(byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        mI2cDevice.writeRegByte(CONTROL1_REG, (byte) value);
    }

    private void SET_CONTROL2_REGISTER(byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        mI2cDevice.writeRegByte(CONTROL2_REG, (byte) value);
    }

    private void SET_CONTROL3_REGISTER(byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        mI2cDevice.writeRegByte(CONTROL3_REG, (byte) value);
    }

    private void SET_CONTROL4_REGISTER(byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        mI2cDevice.writeRegByte(CONTROL4_REG, (byte) value);
    }

    private byte GET_Vbat_VOLTAGE_MONITOR_REGISTER() throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        byte vbatresult = mI2cDevice.readRegByte(Vbat_VOLTAGE_MONITOR_REG);//
        return vbatresult;
    }

    private byte GET_LRA_RESONANCE_PERIOD_REGISTER() throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        byte lraresonanceperiodresult = mI2cDevice.readRegByte(LRA_RESONANCE_PERIOD_REG);//
        return lraresonanceperiodresult;
    }

    public int drv260x_write_reg_val(int register, byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        else {
            try {
                mI2cDevice.writeRegByte(register, (byte) value);
            }
            catch (Exception ex){
                Log.d("HAPTIC I2C", "Haptic I2C - Error Writing Register Byte : " + ex.toString());
                return 0;
            }
        }
        return 1;
    }
    
    public byte drv260x_read_reg_val(int register) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }

        byte regvalue=0x00;
        try {
            regvalue = mI2cDevice.readRegByte(register);//
        }
        catch (Exception ex){
            Log.d("HAPTIC I2C", "Haptic I2C - Error Reading Register Byte : " + ex.toString());
        }
        return regvalue;
    }

    public byte drv2605_poll_go_bit() throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        byte gobitregvalue = mI2cDevice.readRegByte(GO_REG);//
        return gobitregvalue;
    }

    public void drv2605_select_library(int library) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }

        switch(library) {
            case 1:
                SET_LIBRARYSELECT_REGISTER((byte)0x01);
                break;
            case 2:
                SET_LIBRARYSELECT_REGISTER((byte)0x02);
                break;
            case 3:
                SET_LIBRARYSELECT_REGISTER((byte)0x03);
                break;
            case 4:
                SET_LIBRARYSELECT_REGISTER((byte)0x04);
                break;
            case 5:
                SET_LIBRARYSELECT_REGISTER((byte)0x05);
                break;
            case 6:
                SET_LIBRARYSELECT_REGISTER((byte)0x06);
                break;
            case 7:
                SET_LIBRARYSELECT_REGISTER((byte)0x07);
                break;
            case 8:
                SET_LIBRARYSELECT_REGISTER((byte)0x08);
                break;
            default:
                SET_LIBRARYSELECT_REGISTER((byte)0x00);
                break;
        }
    }



    public void drv260x_set_rtp_val(byte rtp) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        SET_RTP_INPUT_REGISTER(rtp);
    }


    public void drv2605_set_waveform_sequence(int sequence,byte value) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        switch (sequence) {
            case 1:
                SET_WAVEFORM_SEQ_REG1_REGISTER(value);
                break;
            case 2:
                SET_WAVEFORM_SEQ_REG2_REGISTER(value);
                break;
            case 3:
                SET_WAVEFORM_SEQ_REG3_REGISTER(value);
                break;
            case 4:
                SET_WAVEFORM_SEQ_REG4_REGISTER(value);
                break;
            case 5:
                SET_WAVEFORM_SEQ_REG5_REGISTER(value);
                break;
            case 6:
                SET_WAVEFORM_SEQ_REG6_REGISTER(value);
                break;
            case 7:
                SET_WAVEFORM_SEQ_REG7_REGISTER(value);
                break;
            case 8:
                SET_WAVEFORM_SEQ_REG8_REGISTER(value);
                break;
            default:
                SET_WAVEFORM_SEQ_REG1_REGISTER(value);
                break;

        }

    }

    public void drv260x_drv260x_change_mode(byte mode) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        drv260x_SET_MODE_REGISTER(mode);
    }

    public void drv260x_set_audio_haptics_enabled(int mode) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        audio_haptics_enabled = mode;
    }

    public boolean drv260x_check_vibrator_is_playing() throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        if (vibrator_is_playing) {
            return true;
        }
        else return false;
    }

    public int drv260x_vibrator_get_time() throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        //DO WORK
        return 0;

    }

    public int drv260x_vibrator_off() throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        try {
            drv260x_write_reg_val(GO_REG, (byte) 0x00);
            vibrator_is_playing=false;
        }
        catch (Exception ex){
            Log.d("HAPTIC I2C", "Unable to stop motor : " + ex.toString());
        }
        return 0;

    }

    public int drv260x_vibrator_on() throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        try {
            drv260x_write_reg_val(GO_REG, (byte) 0x01);
            vibrator_is_playing=true;
        }
        catch (Exception ex){
            Log.d("HAPTIC I2C", "Unable to start motor : " + ex.toString());
        }
        return 1;
    }

    public int drv260x_play_effect(byte effect) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        SET_LIBRARYSELECT_REGISTER(effect);
        return 0;
    }

    public int drv2605_Play_Waveform(byte Effect)
    {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        /* Exit standby mode and use internal trigger */
        try {
            if (drv260x_write_reg_val(MODE_REG, (byte) 0x00) != 0) return -1;

            if (drv260x_write_reg_val(WAVEFORM_SEQ_REG1, (byte) Effect) != 0) return -1;

            if (drv260x_write_reg_val(WAVEFORM_SEQ_REG2, (byte) 0x00) != 0) return -1;

            if (drv260x_write_reg_val(GO_REG, (byte) 0x01) != 0) return -1;

            vibrator_is_playing=true;
        }
        catch (Exception ex){
            Log.d("HAPTIC I2C", "Unable to play waveform : " + ex.toString());
        }
        return 0;
    }

    public int drv260x_setAudioHapticsEnabled(int enable) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        audio_haptics_enabled=1;
        return 1;
    }

    private void setPwm(int channel, int on, int off) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        //int offset = 4 * channel;
        //mI2cDevice.writeRegByte(REG_LED_0_ON_L + offset, (byte) (on & 0xFF));
        //mI2cDevice.writeRegByte(REG_LED_0_ON_H + offset, (byte) (on >> 8));
        //mI2cDevice.writeRegByte(REG_LED_0_OFF_L + offset, (byte) (off & 0xFF));
        //mI2cDevice.writeRegByte(REG_LED_0_OFF_H + offset, (byte) (off >> 8));
    }

    public void drv2605_useERM ()//Eccentric Rotating Mass, ERM
    {
       // writeRegister8(DRV2605_REG_FEEDBACK, readRegister8(DRV2605_REG_FEEDBACK) & 0x7F);
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        try {
            drv260x_write_reg_val(FEEDBACK_CONTROL_REG, (byte) 0x7F);
        }
        catch (Exception ex){
            Log.d("HAPTIC I2C", "Unable to write ERM to feedback control : " + ex.toString());
        }
    }

    public void drv2605_useLRA ()//Linear Resonant Actuator, LRA
    {
        //  writeRegister8(DRV2605_REG_FEEDBACK, readRegister8(DRV2605_REG_FEEDBACK) | 0x80);
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        try {
            drv260x_write_reg_val(FEEDBACK_CONTROL_REG, (byte) 0x80);
        }
        catch (Exception ex){
            Log.d("HAPTIC I2C", "Unable to write LRA to feedback control : " + ex.toString());
        }
    }

    public void drv2605_go_enable() {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        try {
            drv260x_write_reg_val(GO_REG, (byte) 0x01);
            vibrator_is_playing=true;
        }
        catch (Exception ex){
            Log.d("HAPTIC I2C", "Unable to start motor : " + ex.toString());
        }
    }

    public void drv2605_go_stop_disable() {
        if (mI2cDevice == null) {
            throw new IllegalStateException("DRV2605 - I2C device not open");
        }
        try {
            drv260x_write_reg_val(GO_REG, (byte) 0x00);
            vibrator_is_playing=false;
        }
        catch (Exception ex){
            Log.d("HAPTIC I2C", "Unable to stop motor : " + ex.toString());
        }
    }


}



