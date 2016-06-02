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

/**
 *
 * @author jim
 */
public class SN3218 {
    public SN3218 ()
    {
        // Set the data up. All enabled, all off
        
        for (int i = 0; i < LEDS; ++i)
            data[i] = 0;
        
        for (int i = 0; i < ENABLES; ++i)
            data[LEDS + i] = 0x3f;
        
        data[LEDS + ENABLES] = 42;
        
        update ();
    }
    
    public void update ()
    {
    }
    
    /** The number of LEDs */
    private final int LEDS = 18;
    /** The number of enable bytes */
    private final int ENABLES = 3;
    /** The number of go bytes */
    private final int GOS = 1;
    /** The size of the data we hold: 18 values, 3 enables 1 go. */
    private final int DATA_SIZE = LEDS + ENABLES + GOS;
    /** The data we hold for the device. Starts at offset ONE in the device! */
    private final byte[] data = new byte[DATA_SIZE];
}