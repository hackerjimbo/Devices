/*
 * Copyright (C) 2016 Jim Darby.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package Jimbo.Devices;

import java.io.IOException;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.i2c.I2CDevice;

/**
 * The class interfaces to the Si-EN Technology SN3214 LED driver chip. This
 * device is used in many cool devices, not least in things from Pimoroni for
 * the Raspberry Pi.
 * 
 * @author Jim Darby
 */
public class SN3218 {
    public SN3218 (I2CBus bus) throws IOException
    {
        // Set the data up. All enabled, all off
        
        for (int i = 0; i < LEDS; ++i)
            data[i] = 0;
        
        for (int i = 0; i < ENABLES; ++i)
            data[LEDS + i] = 0x3f;
        
        // Tell the device to go. Any value will do but this is in memory of
        // Douglas Adams.
        data[LEDS + ENABLES] = 42;
        
        // Allocate device
        device = bus.getDevice (0x54);
     
        // And set everything up
        device.write (0, wakeup, 0, wakeup.length);
    }
    
    /**
     * Set a single output to a value.
     * @param led The LED (in the range 0 to 17).
     * @param value The value (in the range 0 to 255).
     * @throws IOException On an invalid parameter.
     */
    public void set (int led, int value) throws IOException
    {
        if (led < 0 || led >= LEDS)
            throw new IOException ("Invalid LED " + led);
        
        if (value < 0 || value > 255)
            throw new IOException ("Invalid level " + value);
        
        data[led] = (byte) value;
    }
    
    /**
     * Set an RGB LED group with three values. The way the device is wired up
     * determines the specific groupings.
     * @param led The LED in the range 0 - 5.
     * @param v1 The first value.
     * @param v2 The second value.
     * @param v3 The third value.
     * @throws IOException On an invalid parameter.
     */
    public void set (int led, int v1, int v2, int v3) throws IOException
    {
        if (led < 0 || led >= LEDS / 3)
            throw new IOException ("Invalid RGB LED " + led);
        
        if (v1 < 0 || v1 > 255 || v2 < 0 || v2 > 255 || v3 < 0 || v3 > 255)
            throw new IOException ("Invalid colour value");
        
        data[led * 3    ] = (byte) v1;
        data[led * 3 + 1] = (byte) v2;
        data[led * 3 + 2] = (byte) v3;
    }
    
    /**
     * Update the display. This sends all the updated value to the LEDs.
     * @throws IOException In case of an I2C error.
     */
    public void update () throws IOException
    {
        device.write(1, data, 0, data.length);
    }
    
    public static void main (String args[]) throws IOException
    {
        int use_i2cbus = I2CBus.BUS_1;
        final I2CBus bus = I2CFactory.getInstance (use_i2cbus);
        final SN3218 s = new SN3218 (bus);
        
        while (true)
        {
            final int cycle = 3 * 256;
            final int step = cycle / (LEDS / 3);
            
            for (int i = 0; i < cycle; ++i)
            {
                for (int j = 0; j < LEDS / 3; ++j)
                    set (s, j, (i + j * step) % cycle);
                
                s.update ();
            }
        }
    }
    
    private static void set (SN3218 s, int led, int i) throws IOException
    {
        final boolean part1 = i < 256;
        final boolean part2 = !part1 && i < 512;
        final boolean part3 = i > 2 * 256;
        final int m = i % 256;
        
        final int p1 = part1 ? m : part2 ? (255 - m) : 0;
        final int p2 = part2 ? m : part3 ? (255 - m) : 0;
        final int p3 = part3 ? m : part1 ? (255 - m) : 0;

        s.set (led, p1, p2, p3);
    }
   
    /** The I2C device */
    private final I2CDevice device;
    /** The number of LEDs */
    private final static int LEDS = 18;
    /** The number of enable bytes */
    private final static int ENABLES = 3;
    /** The number of go bytes */
    private final static int GOS = 1;
    /** The size of the data we hold: 18 values, 3 enables 1 go. */
    private final static int DATA_SIZE = LEDS + ENABLES + GOS;
    /** The data we hold for the device. Starts at offset ONE in the device! */
    private final byte[] data = new byte[DATA_SIZE];
    /** Wakeup data */
    private final byte[] wakeup = {
        0x01,                               // Wake up the device
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // Data part one
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // Data part two
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // Data part three
        0x3f, 0x3f, 0x3f,                   // Enable all outputs
        0x42                                // And go!
    };
}