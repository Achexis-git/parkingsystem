package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        //int inHour = ticket.getInTime().getHours();  deprecated method
        //int outHour = ticket.getOutTime().getHours();
        
        double inHour  = ticket.getInTime().getTime();  // getTime() => time since 1970 in ms
        double outHour = ticket.getOutTime().getTime();

        //TODO: Some tests are failing here. Need to check if this logic is correct
        //Solution: getHours() give only the whole hour, problem if between hours => compute with milliseconds + deprecated method
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
        
        applyReductions(ticket, duration);
    }
    
    /**
     * Apply the reductions to the ticket price
     * 
     * @param ticket Parking ticket
     * @param duration Parking time
     * 
     */
    private void applyReductions(Ticket ticket, double duration) {
    	// free parking if less than 30m
    	if (duration <= 0.5) {
        	ticket.setPrice(0);
        }
    	// -5% if recurring customer
    	else if (ticket.getRecurringCustomer()) {
        	ticket.setPrice(ticket.getPrice() * 0.95);
        }
    }
}