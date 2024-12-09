package de.kai_morich.simple_bluetooth_terminal.PIDsFormulas;

public class LONG_TERM_FUEL_TRIM_BANK_1 {
    public static String read(String firstHex){
        String response = null;

        int percentage = Integer.parseInt(firstHex, 16);
        response = "Long term fuel trim bank 1: " + Integer.toString(percentage) + "%.";

        return response;
    }
}
