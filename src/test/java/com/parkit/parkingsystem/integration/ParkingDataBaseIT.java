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
import static org.mockito.Mockito.verify;

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
        
        // Charge le ticket de plaque ABCDEF de la DB
        ticket = ticketDAO.getTicket("ABCDEF");
        // Vérifie qu'il y a une ticket
        assertThat(ticket).isNotEqualTo(null);
        // Vérifie que la prochaine place disponible est la place 2
        assertThat(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).isEqualTo(2);
    }

    @Test
    public void testParkingLotExit(){
    	
        testParkingACar(); 
         
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processExitingVehicle();
        
        //TODO: check that the fare generated and out time are populated correctly in the database
        
        // Charge le ticket de plaque ABCDEF de la DB
        ticket = ticketDAO.getTicket("ABCDEF");
        
        // Vérifie que le prix est bien proche de 0 dans la DB (et donc existe)
        assertThat(ticket.getPrice()).isCloseTo(0.0, within(0.5));
        // Vérifie que l'heure de sortie est proche de l'heure actuelle (et donc existe)
        assertThat(ticket.getOutTime()).isCloseTo(new Date(), TimeUnit.SECONDS.toMillis(5));
    }
    
    @Test
    public void testReccurentUserReduction() {
    	fareCalculatorService = new FareCalculatorService();
    	ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();
        parkingService.processExitingVehicle();
        
        Date inTime = new Date();
		inTime.setTime(System.currentTimeMillis() - (10 * 60 * 60 * 1000));// 10h parking time
		
		Date outTime = new Date();
		
		ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
		
		ticket.setInTime(inTime);
		ticket.setOutTime(outTime);
		ticket.setParkingSpot(parkingSpot);
		ticket.setVehicleRegNumber("ABCDEF");
		fareCalculatorService.calculateFare(ticket);
        
        
        assertThat(ticket.getPrice()).isCloseTo(10 * Fare.CAR_RATE_PER_HOUR * 0.95, within(0.01)); // 95% du prix
    }

}
