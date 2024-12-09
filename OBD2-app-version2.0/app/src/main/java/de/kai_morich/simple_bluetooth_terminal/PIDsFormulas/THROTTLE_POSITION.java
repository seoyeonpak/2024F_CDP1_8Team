package de.kai_morich.simple_bluetooth_terminal.PIDsFormulas;

public class THROTTLE_POSITION {
    public static String read(String firstHex){
        String response = null;

        float firstDecimal = Integer.parseInt(firstHex, 16);
        float throttlePosition = 100 * firstDecimal / 255;

        response = "Current throttle position: " + Float.toString(throttlePosition)+ "%.";
        return response;
    }
}
