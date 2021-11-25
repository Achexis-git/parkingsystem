package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.when;

import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import java.util.Date;
import java.util.concurrent.TimeUnit;


@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;
    
    private Ticket ticket;
    private static FareCalculatorService fareCalculatorService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    
    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
        ticket = new Ticket();
    }

    @AfterAll
    private static void tearDown(){

    }

    @Test
    public void testParkingACar(){
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();
        //TODO: check that a ticket is actually saved in DB and Parking table is updated with availability
        
        // Get the ticket of reg "ABCDEF" from the DB
        ticket = ticketDAO.getTicket("ABCDEF");
        // Verify if there is a ticket
        assertThat(ticket).isInstanceOf(Ticket.class);
        // Verify if the next available spot is the 2 (give 2 if 1 not available)
        assertThat(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).isEqualTo(2);
    }

    @Test
    public void testParkingLotExit(){
    	
        testParkingACar(); 
         
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processExitingVehicle();
        
        //TODO: check that the fare generated and out time are populated correctly in the database
        
        // Get the ticket of reg "ABCDEF" from the DB
        ticket = ticketDAO.getTicket("ABCDEF");
        
        // Verify if the price is close to 0 in the DB (and so exist)
        assertThat(ticket.getPrice()).isCloseTo(0.0, within(0.5));
        // Verify that the out time is close to the actual time (and so exist)
        //assertThat(ticket.getOutTime()).isCloseTo(new Date(), TimeUnit.SECONDS.toMillis(5)); // possible error if testParkingCar() is too slow
        assertThat(ticket.getOutTime()).isInstanceOf(Date.class);
    }
    
    /**
     * Test if the 5% reduction is applied for multiple visits
     */
    @Test
    public void testReccurentUserReduction() {
    	fareCalculatorService = new FareCalculatorService();
    	ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
    	
        parkingService.processIncomingVehicle();
        parkingService.processExitingVehicle();   
        parkingService.processIncomingVehicle(); 
        
        ticket = ticketDAO.getTicket("ABCDEF");
        
        Date inTime = new Date();
		inTime.setTime(System.currentTimeMillis() - (10 * 60 * 60 * 1000));// 10h parking time	
		Date outTime = new Date();
		
		ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
		
		ticket.setInTime(inTime);
		ticket.setOutTime(outTime);
		ticket.setParkingSpot(parkingSpot);
		ticket.setVehicleRegNumber("ABCDEF");
		ticket.setRecurringCustomer(ticketDAO.isItARecurringCustomer("ABCDEF"));
		
		fareCalculatorService.calculateFare(ticket);
        
        assertThat(ticket.getPrice()).isCloseTo(10 * Fare.CAR_RATE_PER_HOUR * 0.95, within(0.01)); // 95% of price
        assertThat(ticket.getRecurringCustomer()).isEqualTo(true); // Does the program detects a recurring customer ?
    }

}
