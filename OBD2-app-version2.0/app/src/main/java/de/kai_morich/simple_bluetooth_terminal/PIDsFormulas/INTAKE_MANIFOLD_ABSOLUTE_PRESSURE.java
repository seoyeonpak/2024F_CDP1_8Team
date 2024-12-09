package de.kai_morich.simple_bluetooth_terminal.PIDsFormulas;

public class INTAKE_MANIFOLD_ABSOLUTE_PRESSURE {
    public static String read(String firstHex){
        String response = null;

        int pressure = Integer.parseInt(firstHex, 16);
        response = "Intake manifold absolute pressure: " + Integer.toString(pressure) + " kPa.";

        return response;
    }
}
