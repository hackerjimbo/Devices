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

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.system.SystemInfo;

import java.io.IOException;

/**
 * This class let's us find the right I2C bus to use. It does so by looking at
 * the hardware version. Now it can also return the board type, whether it's a
 * plus (40 pin) board and if it's the original rev1.
 * 
 * @author Jim Darby
 */
public class Pi2C
{
    /**
     * Determine the correct I2C bus to use.
     * @return The bus.
     * @throws IOException In case of error.
     * @throws InterruptedException In case of error.
     */
    public static I2CBus useBus () throws IOException, InterruptedException
    {
        check_board ();
        
        return bus;
    }
    
    /**
     * Determine the board type.
     * @return The board type.
     * @throws IOException In case of error.
     * @throws InterruptedException In case of error.
     */
    public static SystemInfo.BoardType boardType () throws IOException, InterruptedException
    {
        check_board ();
        
        return type;
    }
    
    /**
     * Are we a plus (40-pin) model?
     * @return if we are.
     * @throws IOException In case of error.
     * @throws InterruptedException In case of error.
     */
    public static boolean isPlus () throws IOException, InterruptedException
    {
        check_board ();
        
        return plus;
    }
    
    private static synchronized void check_board () throws IOException, InterruptedException
    {   
        int use_i2cbus;
        type = SystemInfo.getBoardType ();
            
        switch (type)
        {
            // What do we know that it's bus 0? This is the most useful as
            // we know about all the old boards.
            case ModelA_Rev1:
            case ModelB_Rev1:
                // Use the original I2C bus, rev1 and not plus
                use_i2cbus  = I2CBus.BUS_0;
                rev1        = true;
                plus        = false;
                break;
                
            // What do we *know* that it's bus 1?
            case ModelB_Rev2:
                use_i2cbus  = I2CBus.BUS_1;
                rev1        = false;
                plus        = false;
                break;
                
            case ModelA_Plus_Rev1:
            case ModelB_Plus_Rev1:
            case Model2B_Rev1:
                use_i2cbus  = I2CBus.BUS_1;
                rev1 = false;
                plus = true;
                break;

            // The clear ommission here is the Pi 3. However that should
            // work (but hasn't been formally tested). The Pi Zero has been
            // tested and works well.
            case UNKNOWN:
                System.err.println ("Unknown board type, hoping for the best (a 3 or 0)");
                use_i2cbus  = I2CBus.BUS_1;
                rev1 = false;
                plus = true;
                break;

            default:
                System.err.println ("getBoardType gave an unrecognised result, hoping for the best");
                use_i2cbus  = I2CBus.BUS_1;
                rev1 = false;
                plus = true;
                break;
            }
                
        bus = I2CFactory.getInstance (use_i2cbus);
        board_looked = true;
    }
    
    /** Hold the bus once we find it */
    private static I2CBus bus = null;
    /** Have we looked for the board type? */
    private static boolean board_looked = false;
    /** What board type have we found? */
    private static SystemInfo.BoardType type;
    /** Are we running on a Plus? */
    private static boolean plus;
    /** Are we running on a 1 rev 1? */
    private static boolean rev1;
}
