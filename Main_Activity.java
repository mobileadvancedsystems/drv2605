      
        I2cDevice mDevice;
        try {
            int effect=1;
            hapticdrv2605 hapticdrv2605_device = new hapticdrv2605("I2C1", 0x5A);
            //Only one I2C bus on raspberry pi 3. so I2C1 is valid not I2C2 for multiple device
            //We select the device on I2C1 bus by specifying the address of 0x5A
            if (hapticdrv2605_device == null) {
                 throw new IllegalStateException("HAPTIC I2C device not open");
            }
            //hapticdrv2605_device.drv260x_check_vibrator_is_playing();
            //hapticdrv2605_device.drv260x_play_effect((byte)0x10);
            //hapticdrv2605_device.drv2605_Play_Waveform((byte)0x11);
            hapticdrv2605_device.drv2605_select_library(1);//Strong Click 100%
            hapticdrv2605_device.drv260x_SET_MODE_REGISTER((byte)0x00);
            if (effect > 117) effect = 1;
            //effect=16;
            hapticdrv2605_device.drv2605_Play_Waveform((byte)effect);
            //Play the effect
            hapticdrv2605_device.drv2605_go_enable();//Bit clears after played
        }
        catch (Exception ex){
            Log.d(TAG, "Haptic I2C - MainActivity - problem attaching to device.: " + ex.toString());
        }
