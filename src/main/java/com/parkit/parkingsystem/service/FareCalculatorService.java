package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        //int inHour = ticket.getInTime().getHours();  méthode obsolète
        //int outHour = ticket.getOutTime().getHours();
        
        double inHour  = ticket.getInTime().getTime();  // getTime() => temps écoulé en ms depuis 1970
        double outHour = ticket.getOutTime().getTime();

        //TODO: Some tests are failing here. Need to check if this logic is correct
        //Solution: getHours() donne juste l'heure entière, besoin de compter les minutes + méthode obsolète
        double duration = outHour - inHour;
        duration = duration / (1000 * 60 *60); // milliseconds => hours

        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
                ticket.setPrice(duration * Fare.CAR_RATE_PER_HOUR);
                break;
            }
            case BIKE: {
                ticket.setPrice(duration * Fare.BIKE_RATE_PER_HOUR);
                break;
            }
            default: throw new IllegalArgumentException("Unkown Parking Type");
        }
        
        // parking gratuit si moins de 30m
        if (duration <= 0.5) {
        	ticket.setPrice(0);
        }
    }
}