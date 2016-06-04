/*
 * Copyright (C) 2016 Jim Darby
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

import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.spi.SpiFactory;
import com.pi4j.io.spi.SpiDevice;
import com.pi4j.io.spi.SpiChannel;

/**
 * A Class to handle the ST7036 display driver. At the moment this only supports
 * SPI interface with R/W force to write, EXT mode and on SPI bus 0, device 0.
 * It also only supports 3 line displays.
 * 
 * This could do with enhancing but I'll need a device to test it with.
 * 
 * @author Jim Darby
 */
public class ST7036
{
    /**
     * Initialise the device. The form assumes something essentially identical
     * to the Pimoroni Displayotron 3000. EXT is forced to Vss, it's a 3 line
     * display and we're talking over SPI channel 0.
     * @param reg_select
     * @throws IOException 
     */
    public ST7036 (Pin reg_select) throws IOException
    {
        GpioController gpio = GpioFactory.getInstance ();
        
        // Configure register select pin
        rs = gpio.provisionDigitalOutputPin (reg_select);
        rs.high ();
        
        spi = SpiFactory.getInstance (SpiChannel.CS0, 1000000);
        
        function_set (true, true, false, 0);
        display_mode (true, false, false);
        entry_mode (true, false);
        set_bias (true, true);
        set_contrast (40);
        clear ();
    }
    
    /**
     * Clear the screen and home the cursor. 
     * @throws IOException In case of error.
     */
    public void clear () throws IOException
    {
        write_command (COMMAND_CLEAR, -1, 1080000);
    }

     /**
     * Home the cursor. 
     * @throws IOException In case of error.
     */
    public void home () throws IOException
    {
        write_command (COMMAND_HOME, -1, 1080000);
    }
    
    /**
     * Select the entry mode. Increment is true if we increment the address on
     * write, false and we decrement. This gives is left-to-right (true) or
     * right-to-left (false) movement. Shift sets the shifting of the entire
     * display on write.
     * @param increment True to increment write pointer, false to decrement.
     * Corresponds to the I/D bit in the chip documentation. 
     * @param shift True to shift the entire display on write. Corresponds to
     * the S bit in the documentation.
     * @throws IOException In case of error.
     */
    public void entry_mode (boolean increment, boolean shift) throws IOException
    {
        write_command (COMMAND_ENTRY_MODE |
                (increment ? 0b00000010 : 0) |
                (shift     ? 0b00000001 : 0),
                -1,
                26300);
    }
    
    /**
     * Set the display mode.
     * @param on Is the display turned on?
     * @param cursor Is the cursor enabled?
     * @param blink Should the cursor blink?
     * @throws IOException In case of error.
     */
    public void display_mode (boolean on, boolean cursor, boolean blink) throws IOException
    {
        write_command (COMMAND_DISPLAY_MODE |
                (on     ? 0b00000100 : 0) |
                (cursor ? 0b00000010 : 0) |
                (blink  ? 0b00000001 : 0),
                26300);
    }
    
    /**
     * Set various functions. You need to read the datasheet for this one. For
     * out simple case (3-line display accessed via SPI) bus 8 is ignored (we
     * hope), line2 should always be true for a 3 line display, dh sets the
     * double height and is sets the instruction set to use.
     * @param bus8 true for 8-bit bus, false for 4-bit. Corresponds to the DL
     * bit in the documentation.
     * @param line2 true for 2-line or 3-line mode, false for single line.
     * Corresponds to the N bit in the documentation.
     * @param dh true for double height, false for single. Corresponds to the
     * DH bit in the documentation.
     * @param is The instruction set to use. Corresponds to the IS2/IS1 bits in
     * the documentation.
     * @throws IOException In case of error. 
     */
    public void function_set (boolean bus8, boolean line2, boolean dh,
            int is) throws IOException
    {
        if (is < 0 || is > 3)
            throw new IOException ("Invalid instruction set");
        
        write_command (COMMAND_FUNCTION_SET |
                (bus8  ? 0b00010000 : 0) |
                (line2 ? 0b00001000 : 0) |
                (dh    ? 0b00000100 : 0) |
                is,
                26300);
        
        bus8_ = bus8;
        line2_ = line2;
        dh_ = dh;
        is_ = is;
    }

    /**
     * Writes some text at the current position.
     * 
     * @param text The text to write.
     * @throws IOException  In case of error.
     */
    public void write (String text) throws IOException
    {
        rs.high ();
        
        for (int i = 0; i < text.length(); ++i)
        {
            spi.write ((byte) text.charAt (i));
            
            try
            {
                Thread.sleep (0, 50000);
            }
            
            catch (InterruptedException e)
            {
                System.err.println ("Sleep interrupted");
            }
        }
    }
    
    /**
     * Set the bias on the output.
     * @param bias Enable bias
     * @param fx Must be true for 3-line applications, otherwise false.
     * @throws IOException In case of error.
     */
    public void set_bias (boolean bias, boolean fx) throws IOException
    {
        write_command (COMMAND_BIAS |
                (bias ? 0b00001000 : 0) |
                (fx   ? 0b00000001 : 0), 1, 26300);
    }
    
    public void set_contrast (int contrast) throws IOException
    {
        write_command (0b01010100 | ((contrast >> 4) & 0x03), 1, 26300);
        write_command (0b01101011, 1, 26300);
        write_command (0b01110000 | (contrast & 0x0F), 1, 26300);
    }
        
    private void write_command (int command, int set, int delay) throws IOException
    {
        set_is (set);
        write_command (command, delay);
    }
    
    private void set_is (int to) throws IOException
    {
        // Check for don't care
        if (to < 0)
            return;
        
        if (is_ != to)
            function_set (bus8_, line2_, dh_, to);
    }
    
    private void write_command (int command, int delay) throws IOException
    {
        if (command < 0 || command > 255)
            throw new IOException ("Invalid command code " + command);
        
        rs.low ();
        spi.write ((byte) command);
        
        try
        {
            Thread.sleep (delay / 1000000, delay % 1000000);
        }
        
        catch (InterruptedException e)
        {
            System.err.println ("Sleep interrupted!");
        }
    }
    
    private final GpioPinDigitalOutput rs;
    private final SpiDevice spi;
    
    /** Are we using an 8-bit bus? */
    private boolean bus8_;
    /** Is this a two-line display? */
    private boolean line2_;
    /** Are we running double height? */
    private boolean dh_;
    /** The current instruction set we're using. 0 to 3. */
    private int is_;
 
    private static final int COMMAND_CLEAR        = 0b00000001;
    private static final int COMMAND_HOME         = 0b00000010;
    private static final int COMMAND_ENTRY_MODE   = 0b00000100;
    private static final int COMMAND_DISPLAY_MODE = 0b00001000;
    private static final int COMMAND_FUNCTION_SET = 0b00100000;
    private static final int COMMAND_BIAS         = 0b00010100;
    private static final int COMMAND_SCROLL = 0b00010000;
    private static final int COMMAND_DOUBLE = 0b00010000;

    private static final int TOP = 1;
    private static final int BOTTOM = 0;

    public static void main (String args[]) throws IOException
    {
        SN3218 leds = new SN3218 (Pi2C.useBus ());
        
        for (int i = 0; i < 3; ++i)
            leds.set(i, 62, 64, 64);
        
        leds.update ();
        
        ST7036 lcd = new ST7036 (RaspiPin.GPIO_06);
        
        lcd.write ("Arse! This is a test to see what happens with wraparound.");
    }
}
