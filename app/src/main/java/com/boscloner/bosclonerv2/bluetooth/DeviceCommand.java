//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.boscloner.bosclonerv2.bluetooth;

import android.annotation.SuppressLint;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.HashMap;

public class DeviceCommand {
    public DeviceCommand() {
    }

    public static byte[] GET_CONTINUOUS_DATA_SIZE(ContinuousDataType pType) {
        byte[] _getCommand = new byte[]{-96, pType.getValue(), 0};
        return sumCheck(_getCommand);
    }

    public static byte[] DEL_CONTINUOUS_DATA(ContinuousDataType pType) {
        byte[] _getCommand = new byte[]{-95, pType.getValue(), 0};
        return sumCheck(_getCommand);
    }

    public enum ContinuousDataType {
        PR((byte) 0),
        SPO2((byte) 1),
        BODY_MOVEMENT((byte) 2),
        RR_TIME((byte) 3);

        private final byte value;
        private static HashMap<Byte, ContinuousDataType> valuesToMap = null;

        ContinuousDataType(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }

        @SuppressLint("UseSparseArrays")
        public static ContinuousDataType getEnumByValue(byte value) {
            if (valuesToMap == null) {
                valuesToMap = new HashMap<>();
                for (ContinuousDataType code : values()) {
                    valuesToMap.put(code.value, code);
                }
            }
            return valuesToMap.get(value);
        }
    }

    public static byte[] GET_SPO2_PR_POINT(SPO2PointGetDataOptions pType) {
        byte[] getData = new byte[]{-111, (byte) pType.getValue(), 0};
        return sumCheck(getData);
    }

    public static byte[] GET_BATTERY_LEVEL() {
        byte[] getData = new byte[]{(byte)134, 0};
        return sumCheck(getData);
    }

    public enum SPO2PointGetDataOptions {
        START(0),
        GET_NEXT_GROUP_OF_DATA(1),
        RESEND_CURRENT_GROUP_OF_DATA(2),
        ALL_DATA_RECEIVED(127);
        private int value;

        SPO2PointGetDataOptions(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static byte[] GET_PR_CONTINUOUS_DATA(ContinuousGetDataOption pType) {
        byte[] getData = new byte[]{-94, (byte) pType.getValue(), 0, 0, 0};
        return sumCheck(getData);
    }

    public static byte[] GET_SPO2_CONTINUOUS_DATA(ContinuousGetDataOption pType) {
        byte[] getData = new byte[]{-93, (byte) pType.getValue(), 0, 0, 0};
        return sumCheck(getData);
    }

    public static byte[] GET_MOVE_FRAGMENT(ContinuousGetDataOption pType) {
        byte[] getData = new byte[]{-92, (byte) pType.getValue(), 0, 0, 0};
        return sumCheck(getData);
    }

    public enum ContinuousGetDataOption {
        START(0),
        ALL_DATA_RECEIVED(127);
        private int value;

        ContinuousGetDataOption(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static byte[] GET_RR_DATA(int mType) {
        byte[] getRrdata = new byte[]{-91, 0, 0, 0, 0};
        return sumCheck(getRrdata);
    }

    public static byte[] GET_STEP_DAY(StepDayGetDataOptions pType) {
        byte[] getData = new byte[]{-110, (byte) pType.getValue(), 0};
        return sumCheck(getData);
    }

    public enum StepDayGetDataOptions {
        START(0),
        GET_NEXT_GROUP_OF_DATA(1),
        RESEND_CURRENT_GROUP_OF_DATA(2),
        ALL_DATA_RECEIVED(127);
        private int value;

        StepDayGetDataOptions(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static byte[] GET_STEP_MIN(int pType) {
        byte[] getData = new byte[]{-109, (byte) pType, 0};
        return sumCheck(getData);
    }

    public static byte[] GET_STEP_MIN_DATA(int pType) {
        byte[] getData = new byte[]{-108, (byte) pType, 0};
        return sumCheck(getData);
    }

    public static byte[] GET_ECG_INFO(int pType) {
        byte[] getData = new byte[]{-107, (byte) pType, 0};
        return sumCheck(getData);
    }

    public static byte[] GET_ECG_DATA(int pType) {
        byte[] getData = new byte[]{-106, (byte) pType, 0};
        return sumCheck(getData);
    }

    public static byte[] GET_DATA_SIZE(DataType pType) {
        byte[] _getCommand = new byte[]{-112, (byte) pType.ordinal(), 0};
        return sumCheck(_getCommand);
    }

    public enum DataType {
        SINGLE_SPO2_AND_PR_VALUES,
        TOTAL_STEPS_IN_ONE_DAY,
        STEPS_PER_5_MINUTES_IN_ONE_DAY,
        ECG_SEGMENT,
        PULSE_WAVEFORM_SEGMENT
    }

    public static byte[] SET_STEP_TIME(int startTime, int endTime) {
        byte[] STEP_TIME = new byte[]{-124, (byte) startTime, (byte) endTime, 0};
        return sumCheck(STEP_TIME);
    }

    public static byte[] SET_WEIGHT(int pWeight) {
        byte _weight_l = (byte) (pWeight & 7);
        byte _weight_m = (byte) (pWeight >> 7 & 7);
        byte _weight_h = (byte) (pWeight >> 14 & 7);
        byte[] _weightCommand = new byte[]{-123, _weight_l, _weight_m, _weight_h, 0};
        return sumCheck(_weightCommand);
    }

    public static byte[] SET_CALORIE(int calorie, int starttime, int endtime) {
        int time = endtime - starttime;
        byte[] set_calorie = new byte[]{-117, (byte) (calorie & 127), (byte) (calorie >> 7 & 127), (byte) (time & 128), (byte) (time >> 1), 0};
        return sumCheck(set_calorie);
    }

    /**
     * Sets the screen brightness
     *
     * @param pulseScreen screen brightness, int value in the range from 0-3
     * @return byte array that will be sent to the device
     */
    public static byte[] setInit(BrightnessLevel pulseScreen) {
        int ecglrhand = 0;
        byte[] SET_INIT = new byte[]{-116, 0, 0, 0, 0, 0, 0, 0, (byte) pulseScreen.ordinal(), 0, 0, (byte) ecglrhand, 0, 0, 0};
        return sumCheck(SET_INIT);
    }

    public enum BrightnessLevel {
        LEVEL_0, LEVEL_1, LEVEL_2, LEVEL_3
    }

    public static byte[] SET_TIME() {
        int mYear = Calendar.getInstance().get(Calendar.YEAR) - 2000;
        int mMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
        int mDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        int mHours = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int mMinutes = Calendar.getInstance().get(Calendar.MINUTE);
        int mSeconds = Calendar.getInstance().get(Calendar.SECOND);
        byte[] setTime = new byte[]{-125, (byte) mYear, (byte) mMonth, (byte) mDay, (byte) mHours, (byte) mMinutes, (byte) mSeconds, 0, 0, 0};
        return sumCheck(setTime);
    }

    public static byte[] sumCheck(byte[] pack) {
        int checkSum = 0;
        for (int i = 0; i < pack.length - 1; ++i) {
            checkSum += pack[i] & 255;
        }
        pack[pack.length - 1] = (byte) (checkSum & 127);
        return pack;
    }

    public static byte[] sum_Calorie(byte[] pack) {
        int SUM_CALORIE = 0;
        int _size = pack.length - 2;

        for (int i = 0; i < _size; ++i) {
            SUM_CALORIE += pack[i] & 127;
        }

        return pack;
    }

    public static byte[] setUpdate(byte[] SET_TIME) {
        return sumCheck(SET_TIME);
    }

    public static byte[] setInitDate(byte[] SET_TIME) {
        return sumCheck(SET_TIME);
    }

    public static byte[] set_SJ() {
        byte[] set_sj = new byte[]{-72, 0};
        return sumCheck(set_sj);
    }

    public static byte[] set_CompareVesion() {
        byte[] set_sj = new byte[]{-126, 0};
        return sumCheck(set_sj);
    }

    public static byte[] getCode(byte[] data) {
        byte[] code = new byte[8];

        try {
            byte[] md5_code = MessageDigest.getInstance("MD5").digest(data);

            for (int i = 0; i < 8; ++i) {
                code[i] = (byte) ((md5_code[i] ^ md5_code[i + 8]) & 127);
            }

            byte[] returnValue = new byte[]{-65, code[0], code[1], code[2], code[3], code[4], code[5], code[6], code[7], 0};
            return sumCheck(returnValue);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] getProductIdentification() {
        byte[] getInentification = new byte[]{-127, 0};
        return sumCheck(getInentification);
    }

    public enum Result {
        SET_TIME_SUCCESS((byte) 0x10),
        SET_TIME_ERROR((byte) 0x11),
        SET_STEP_TIME_SUCCESS((byte) 0x20),
        SET_STEP_TIME_ERROR((byte) 0x21),
        SET_WEIGHT_SUCCESS((byte) 0x30),
        SET_WEIGHT_ERROR((byte) 0x31),
        SET_TARGET_CALORIE_SUCCESS((byte) 0x90),
        SET_TARGET_CALORIE_ERROR((byte) 0x91),
        SET_INIT_SUCCESS((byte) 0x18),
        SET_INIT_ERROR((byte) 0x19),
        SINGLE_SPO2_AND_PR_VALUE_DATA_EXISTS((byte) 0x40),
        SINGLE_SPO2_AND_PR_VALUE_DATA_EMPTY((byte) 0x41),
        SINGLE_SPO2_AND_PR_VALUE_DATA_RECEIVING((byte) 0x42),
        SINGLE_SPO2_AND_PR_VALUE_DATA_RECEIVED_SUCCESS((byte) 0x43),
        SINGLE_SPO2_AND_PR_VALUE_DATA_RECEIVED_ERROR((byte) 0x44),
        TOTAL_STEPS_IN_ONE_DAY_DATA_EXISTS((byte) 0x50),
        TOTAL_STEPS_IN_ONE_DAY_DATA_EMPTY((byte) 0x51),
        TOTAL_STEPS_IN_ONE_DAY_DATA_RECEIVING((byte) 0x52),
        TOTAL_STEPS_IN_ONE_DAY_DATA_RECEIVED_SUCCESS((byte) 0x53),
        TOTAL_STEPS_IN_ONE_DAY_DATA_RECEIVED_ERROR((byte) 0x55),
        STEPS_PER_5_MINUTES_IN_ONE_DAY_DATA_EXISTS((byte) 0x60),
        STEPS_PER_5_MINUTES_IN_ONE_DAY_DATA_EMPTY((byte) 0x61),
        ECG_SEGMENTS_DATA_EXISTS((byte) 0x70),
        ECG_SEGMENTS_DATA_EMPTY((byte) 0x71),
        PULSE_WAVEFORM_SEGMENT_DATA_EXISTS((byte) 0x80),
        PULSE_WAVEFORM_SEGMENT_DATA_EMPTY((byte) 0x81),
        CONTINUOUS_PR_DATA_EXISTS((byte) 0xB0),
        CONTINUOUS_PR_DATA_EMPTY((byte) 0xB1),
        CONTINUOUS_PR_DATA_RECEIVED_SUCCESS((byte) 0x83),
        CONTINUOUS_PR_DATA_RECEIVING((byte) 0x82),
        CONTINUOUS_SPO2_DATA_EXISTS((byte) 0xB2),
        CONTINUOUS_SPO2_DATA_EMPTY((byte) 0xB3),
        CONTINUOUS_SPO2_DATA_RECEIVING((byte) 0x92),
        CONTINUOUS_SPO2_DATA_RECEIVED_SUCCESS((byte) 0x93),
        CONTINUOUS_BODY_MOVEMENT_DATA_EXISTS((byte) 0xB4),
        CONTINUOUS_BODY_MOVEMENT_DATA_EMPTY((byte) 0xB5),
        CONTINUOUS_RR_TIME_DATA_EXISTS((byte) 0xB8),
        CONTINUOUS_RR_TIME_DATA_EMPTY((byte) 0xB9),
        BATTERY_LEVEL_RECEIVED_SUCCESS((byte) 0xF6),
        DELETE_CONTINUOUS_PR_DATA_SUCCESS((byte) 0xC0),
        DELETE_CONTINUOUS_SPO2_DATA_SUCCESS((byte) 0xC1),
        DELETE_CONTINUOUS_MOVEMENT_DATA_SUCCESS((byte) 0xC2),
        DELETE_CONTINUOUS_RR_DATA_SUCCESS((byte) 0xC3);


        private final byte value;
        private static HashMap<Byte, Result> valueToResultMap = null;

        Result(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }

        @SuppressLint("UseSparseArrays")
        public static Result getEnumByValue(byte value) {
            if (valueToResultMap == null) {
                valueToResultMap = new HashMap<>();
                for (Result code : values()) {
                    valueToResultMap.put(code.value, code);
                }
            }
            return valueToResultMap.get(value);
        }
    }
}
