package de.kai_morich.simple_bluetooth_terminal.PIDsFormulas;

public class RELATIVE_ACCELERATOR_PEDAL_POSITION {
    public static String read(String firstHex) {
        String response = null;

        float firstDecimal = Integer.parseInt(firstHex, 16);
        float position = 100 * firstDecimal / 255;

        response = "Relative accelerator pedal position : " + Float.toString(position) + " % \n";
        return response;
    }
}
