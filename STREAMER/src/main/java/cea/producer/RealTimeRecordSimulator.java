package cea.producer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import cea.util.ProblemType;
import net.rationalminds.LocalDateModel;
import net.rationalminds.Parser;

/**
 * Class for the real time record simulation 
 *
 */
public class RealTimeRecordSimulator {
	
	/**
	 * Methode tha realise the simulation
	 * @param line corresponds to the record which should be simulate to a real time record
	 * @param lastRecordDateTimeBeforeSimulation corresponds to date and time of record to be simulated
	 * @param previousRecordDateTimeAfterSimulation correspond to the current date and time for the first simulation,  and for the
	 * next simulations, it corresponds to the date and time of the previous simulated record
	 * @param epsilonne: corresponds to the short time we add to be sure that the simulated record time has not gone too far compared to the current time 
	 * @param scale : since we are simulated old records to real time records, we keep the same gap between them, and in the case of our
	 * data, for many of them, there is several time (one hour) between them, for the need of simulation, we introduce this variable, to reduce 
	 * time between record, when, it's gone to take long time. without the scale, we will have many records with date and time which are after 
	 * current date and time and that's not make sense.
	 * @param problemType : correspond to the type of problem we are streaming records.
	 * @return {@RawRecord} object
	 */
	public RawRecord simulateRecord(String line, LocalDateTime lastRecordDateTimeBeforeSimulation, LocalDateTime previousRecordDateTimeAfterSimulation, Long epsilonne, long scale, String problemType) {
		Parser parser=new Parser();  
		String [] lineContents;
	    String line1;
	    line1 = line.substring(1,line.length()-1);
		lineContents = line1.split("\";\"");
		
		 List<LocalDateModel> date=parser.parse(line);
		 
	        LocalDateTime currentRecordDateTimeBeforeSimulation = LocalDateTime.parse(date.get(0).getDateTimeString(),
	        		DateTimeFormatter.ofPattern(date.get(0).getConDateFormat()));
	    // teta corresponds to the gap between two olds records    
	    Long teta = (lastRecordDateTimeBeforeSimulation!=null)?((Duration.between(lastRecordDateTimeBeforeSimulation,
	    		currentRecordDateTimeBeforeSimulation)).toMillis()+epsilonne)/scale:epsilonne/scale;
	    
	    RawRecord l=new RawRecord();
	    // we update RawRecord object 
	    l.setRecordDateTimeBeforSimulation(currentRecordDateTimeBeforeSimulation);  
	    
	    // we add teta ( the time gap, between the record we are simulating and the previous record we have simulated) to the time 
	    // of the record we are simulating.
	    l.setRecordDateTimeAfterSimulation(previousRecordDateTimeAfterSimulation.plus(teta,ChronoUnit.MILLIS).withNano(0));
	    
	    l.setMesurement(lineContents[1]);
	    
	    if(lineContents.length==2) l.setClassification(null);
	    else l.setClassification(lineContents[2]);
	   
	    // we get dynamically the date format and update our RawRecord object
	    String dateFormat =ProblemType.valueOf(problemType).getDateFormat();
	    l.setDateFormat(dateFormat);
		/*
		 * l.setSimulatedRecord( line.replace(lineContents[0],
		 * l.getRecordDateTimeAfterSimulation().toLocalDate().format(DateTimeFormatter.
		 * ofPattern(dateFormat,Locale.ENGLISH))+" "+l.getRecordDateTimeAfterSimulation(
		 * ).toLocalTime()) );
		 */
	    return l;  
	}
	

}
