package de.kai_morich.simple_bluetooth_terminal.PIDsFormulas;

public class INTAKE_AIR_TEMPERATURE {
    public static String read(String firstHex){
        String response = null;

        int firstDecimal = Integer.parseInt(firstHex, 16);
        int temperature =  firstDecimal - 40;

        response = "Intake air temperature: " + Integer.toString(temperature) + " °C";

        return response;
    }
}
