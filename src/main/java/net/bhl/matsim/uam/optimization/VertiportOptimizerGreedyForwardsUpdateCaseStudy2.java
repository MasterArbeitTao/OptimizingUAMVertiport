package net.bhl.matsim.uam.optimization;

import net.bhl.matsim.uam.optimization.utils.TripItemForOptimization;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;

public class VertiportOptimizerGreedyForwardsUpdateCaseStudy2 {
    public static Logger log = Logger.getLogger(VertiportOptimizerGreedyForwardsUpdateCaseStudy2.class);
    public static final double flightSpeed= 350/3.6; // m/s
    public static final double UAM_PROCESS_TIME= 10*60; // s
    public static final double takeOffLandingTime= 60; // s
    private static String vertiportCandidateFile;
    private static String fileName;

    public static double calculateEuciDistance(Coord coord1, Coord coord2) {
        double euciDistance = Math.sqrt(Math.pow(coord1.getX() - coord2.getX(), 2) + Math.pow(coord1.getY() - coord2.getY(), 2));
        return euciDistance;
    }
    public static void main(String[] args) throws IOException {
        // Provide the file via program arguments
        if (args.length > 0) {
            fileName = args[0];
            vertiportCandidateFile = args[1];
        }
        // Get the object TripItemForOptimization from the serialized file

        log.info("Loading the vertiport candidates...");
        VertiportReader vertiportReader = new VertiportReader();
        List<Vertiport> vertiportsCandidates = vertiportReader.getVertiports(vertiportCandidateFile);
        // Test
        // Only the first 50 vertiports are used for testing
   //      vertiportsCandidates = vertiportsCandidates.subList(0, 50);
        log.info("Finished loading the vertiport candidates.");
        log.info("Loading the trips...");
        List<TripItemForOptimization> deserializedTripItemForOptimizations = deserializeTripItems(fileName);
        log.info("Finished loading the trips.");
// Test
        // Only the first 5000 trips are used for testing
//        deserializedTripItemForOptimizations = deserializedTripItemForOptimizations.subList(0, 5000);
        HashMap<List<Integer>, Double> vertiportPairsScore = new HashMap<>();
        // Calculate the score of each two vertiports selection
        log.info("Calculating the score of each two vertiports selection...");
        int count = 0;
        for (int i = 0; i < vertiportsCandidates.size(); i++) {
            for (int j = i + 1; j < vertiportsCandidates.size(); j++) {
                List<Integer> vertiportPair = new ArrayList<>();
                vertiportPair.add(i);
                vertiportPair.add(j);
                List<Integer> chosenVertiportID = new ArrayList<>();
                chosenVertiportID.add(vertiportsCandidates.get(i).ID);
                chosenVertiportID.add(vertiportsCandidates.get(j).ID);
                double score = calculateSelectionScore(vertiportsCandidates,chosenVertiportID, deserializedTripItemForOptimizations);
                vertiportPairsScore.put(vertiportPair, score);
                count++;
                if(count%1000==0)
                    log.info("Finished calculating the score of " + count + "/19900 vertiport pairs.");
            }
        }

        log.info("Finished calculating the score of each two vertiports pair.");

        HashMap<List<Integer>,Double> selectedVertiports = new HashMap<>(); // key is the ID of selected vertiports, value is the score of the selected vertiports
        List<Integer> currentSelectedVertiportsID = new ArrayList<>();
        List<Integer> remainVetiportsCandidatesID = new ArrayList<>();
        for (int i = 0; i < vertiportsCandidates.size(); i++) {
            remainVetiportsCandidatesID.add(i);
        }

        while (currentSelectedVertiportsID.size() != 200) {
            // select the first two vertiports
            if (currentSelectedVertiportsID.size() == 0) {
                // select the first vertiport pair
                Integer maxA = null;
                Integer maxB = null;
                double maxScore = Double.NEGATIVE_INFINITY;
                for (Map.Entry<List<Integer>, Double> entry : vertiportPairsScore.entrySet()) {
                    if (entry.getValue() > maxScore) {
                        maxScore = entry.getValue();
                        maxA = entry.getKey().get(0);
                        maxB = entry.getKey().get(1);
                    }
                }
                currentSelectedVertiportsID.add(maxA);
                currentSelectedVertiportsID.add(maxB);
                remainVetiportsCandidatesID.remove(maxA);
                remainVetiportsCandidatesID.remove(maxB);
                log.info("The first two vertiport pair is: " + maxA + " and " + maxB);
            }
            else {
                // select the next vertiport
                 Integer maxVertiportID = null;
                 double maxScore = Double.NEGATIVE_INFINITY;

                    for (Integer vertiportID : remainVetiportsCandidatesID) {
                        List<Integer> vertiportsPair = new ArrayList<>();
                        vertiportsPair.add(vertiportID);
                        vertiportsPair.addAll(currentSelectedVertiportsID);
                        double score= calculateSelectionScore(vertiportsCandidates,vertiportsPair, deserializedTripItemForOptimizations);
                            if (score > maxScore) {
                                maxScore = score;
                                maxVertiportID = vertiportID;
                            }
                        }
                currentSelectedVertiportsID.add(maxVertiportID);
                remainVetiportsCandidatesID.remove(maxVertiportID);
                log.info("The "+ currentSelectedVertiportsID.size()+ "th vertiport is: " + maxVertiportID);

                if(currentSelectedVertiportsID.size()%10==0){
                    log.info("The selected vertiports are: " + currentSelectedVertiportsID);
                    log.info("The score of the selected vertiports is: " + maxScore);}
            }

        }

        log.info("The selected vertiports are: " + currentSelectedVertiportsID);
        log.info("The score of the selected vertiports is: " + calculateSelectionScore(vertiportsCandidates,currentSelectedVertiportsID, deserializedTripItemForOptimizations));
        /*

        // get the first i vertiports from the currentSelectedVertiportsID and store in the selectedVertiports
        for (int i = 2; i <= currentSelectedVertiportsID.size(); i++) {
            List<Integer> selectedVertiportsID = new ArrayList<>();
            for (int j = 0; j < i; j++) {
                selectedVertiportsID.add(currentSelectedVertiportsID.get(j));
            }
            selectedVertiports.put(selectedVertiportsID, calculateSelectionScore(vertiportsCandidates,selectedVertiportsID, deserializedTripItemForOptimizations));
        }

        // print the selected vertiports with the number of selected vertiports, find the solution with highest score
        double maxScore = Double.NEGATIVE_INFINITY;
        List<Integer> maxSelectedVertiportsID = new ArrayList<>();
        for (Map.Entry<List<Integer>,  Double> entry : selectedVertiports.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                maxSelectedVertiportsID = entry.getKey();
            }
            System.out.println("Number of selected vertiports: " + entry.getKey().size());
            System.out.println("Selected vertiports ID: " + entry.getKey());
            System.out.println("Score: " + entry.getValue());
        }

        System.out.println("The solution with highest score is: "+maxSelectedVertiportsID);
        System.out.println("The score of the solution is: "+maxScore);

         */
    }




public static double getPairScore(List<Integer> vertiportsPair, HashMap<List<Integer>, Double> vertiportPairsScore) {
   // sort the vertiport pair
    Collections.sort(vertiportsPair);
    return vertiportPairsScore.get(vertiportsPair);

}

    public static List<TripItemForOptimization> deserializeTripItems(String fileName) {
        List<TripItemForOptimization> tripItemForOptimizations = new ArrayList<>();

        try  {
            FileInputStream fileIn = new FileInputStream(fileName);
            ObjectInputStream objectIn = new ObjectInputStream(fileIn)  ;
            tripItemForOptimizations = (List<TripItemForOptimization>) objectIn.readObject();


        } catch (Exception e) {
            e.printStackTrace();
        }

        return tripItemForOptimizations;
    }
    public static List<Vertiport> findAvailableNeighbourVertiports(List<Integer> intList, List<Vertiport> vertiportList) {
        List<Vertiport> duplicates = new ArrayList<>();

        for (Vertiport vertiport : vertiportList) {
            if (intList.contains(vertiport.ID)) {
                duplicates.add(vertiport);
            } else {
                continue;
            }
        }

        return duplicates;
    }
    public static List<Integer> getRandomNumbers(int min, int max, int n) {
        List<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < n; i++) {
            int num = (int) (Math.random() * (max - min)) + min;
            if (!list.contains(num)) {
                list.add(num);
            } else {
                i--;
            }
        }
        return list;
    }

    public static List<Double> calculateModeProbability(double UAM_Utlility, double carUtility, double ptUtility){


        double sumUtilityExponential = Math.exp(carUtility) + Math.exp(ptUtility) + Math.exp(UAM_Utlility);
        double UAMProbability=Math.exp(UAM_Utlility)/ sumUtilityExponential;
        double carProbability=Math.exp(carUtility)/ sumUtilityExponential;
        double ptProbability=Math.exp(ptUtility)/ sumUtilityExponential;
        List<Double> modeProbability=new ArrayList<>();
        modeProbability.add(UAMProbability);
        modeProbability.add(carProbability);
        modeProbability.add(ptProbability);
        return modeProbability;
    }

    public static double calculateSelectionScore( List<Vertiport> vertiportCandidate,List<Integer> chosenVertiportID, List<TripItemForOptimization> deserializedTripItemForOptimizations) {
        // 实现适应度函数的具体逻辑
        double sumGeneralizedCost=0.0;
        double sumVertiportConstructionCost=0.0;
        double savedGeneralizedCost=0.0;
        double score=0.0;
        List<TripItemForOptimization> uamAvailableTrips = new ArrayList<>();
        // calculate the sum of vertiport construction cost
        for (Integer vertiportID:chosenVertiportID) {
            sumVertiportConstructionCost=sumVertiportConstructionCost+vertiportCandidate.get(vertiportID).constructionCost;
        }

        for (TripItemForOptimization tripItemForOptimization : deserializedTripItemForOptimizations)
        {
            tripItemForOptimization.isUAMAvailable = false;
            tripItemForOptimization.uamUtility=-9999;
            List<Vertiport> originNeighbourVertiports = findAvailableNeighbourVertiports(chosenVertiportID, tripItemForOptimization.originNeighborVertiportCandidates);
            List<Vertiport> destinationNeighbourVertiports = findAvailableNeighbourVertiports(chosenVertiportID, tripItemForOptimization.destinationNeighborVertiportCandidates);
            if (originNeighbourVertiports.size() > 0 && destinationNeighbourVertiports.size() > 0) {
                tripItemForOptimization.isUAMAvailable = true;
                tripItemForOptimization.originNeighborVertiports = originNeighbourVertiports;
                tripItemForOptimization.destinationNeighborVertiports = destinationNeighbourVertiports;
                uamAvailableTrips.add(tripItemForOptimization);}
        }

        for (TripItemForOptimization tripItemForOptimization : uamAvailableTrips) {

            for (Vertiport vertiport : tripItemForOptimization.originNeighborVertiports) {
                tripItemForOptimization.originNeighborVertiportsTimeAndDistance.put(vertiport, tripItemForOptimization.originNeighborVertiportCandidatesTimeAndDistance.get(vertiport));
            }
            for (Vertiport vertiport : tripItemForOptimization.destinationNeighborVertiports) {
                tripItemForOptimization.destinationNeighborVertiportsTimeAndDistance.put(vertiport, tripItemForOptimization.destinationNeighborVertiportCandidatesTimeAndDistance.get(vertiport));
            }

            // Initialize the Vertiport Allocation
            // Find the vertiport pair for a trip with the lowest uam generalized cost, the access and ergress vertiport could not be the same
            double lowestUAMGeneralizedCost = Double.MAX_VALUE;
            Vertiport originVertiport = null;
            Vertiport destinationVertiport = null;
            for (Vertiport origin : tripItemForOptimization.originNeighborVertiports) {
                for (Vertiport destination :  tripItemForOptimization.destinationNeighborVertiports) {
                    if (origin.ID != destination.ID) {
                        double accessTime = tripItemForOptimization.originNeighborVertiportsTimeAndDistance.get(origin).get("travelTime");
                        double egressTime = tripItemForOptimization.destinationNeighborVertiportsTimeAndDistance.get(destination).get("travelTime");
                        double accessDistance = tripItemForOptimization.originNeighborVertiportsTimeAndDistance.get(origin).get("distance");
                        double egressDistance = tripItemForOptimization.destinationNeighborVertiportsTimeAndDistance.get(destination).get("distance");
                        double accessCost =0;
                        double egressCost =0;
                        double flightDistance= calculateEuciDistance(origin.coord,destination.coord);
                        double flightTime=flightDistance/flightSpeed+takeOffLandingTime;
                        double flightCost=6.1+ calculateEuciDistance(origin.coord,destination.coord)/1000*0.6;
                        double uamTravelTime=accessTime+egressTime+flightTime+UAM_PROCESS_TIME;
                        if (tripItemForOptimization.accessMode.equals("car") ){
                            accessCost=accessDistance/1000*0.42;
                        }
                        if (tripItemForOptimization.egressMode.equals("car") ){
                            egressCost=egressDistance/1000*0.42;
                        }
                        double UAMCost=accessCost+egressCost+flightCost;
                        double UAMGeneralizedCost=UAMCost+uamTravelTime* tripItemForOptimization.VOT;
                        if (UAMGeneralizedCost < lowestUAMGeneralizedCost) {
                            tripItemForOptimization.uamTravelTime=uamTravelTime;
                            tripItemForOptimization.UAMCost=UAMCost;
                            tripItemForOptimization.UAMUtilityVar=-2.48*UAMCost/100-4.28*flightTime/6000-6.79*(uamTravelTime-flightTime)/6000;
                            tripItemForOptimization.uamUtility= tripItemForOptimization.UAMUtilityFix+ tripItemForOptimization.UAMUtilityVar;
                            tripItemForOptimization.UAMGeneralizedCost=UAMGeneralizedCost;
                            tripItemForOptimization.accessVertiport = origin;
                            tripItemForOptimization.egressVertiport = destination;
                            lowestUAMGeneralizedCost = UAMGeneralizedCost;
                        }
                    }
                }
            }
            // determine the probability of mode choice of each trip
            tripItemForOptimization.uamProbability=calculateModeProbability(tripItemForOptimization.uamUtility, tripItemForOptimization.carUtility, tripItemForOptimization.ptUtility).get(0);
            tripItemForOptimization.carProbability=calculateModeProbability(tripItemForOptimization.uamUtility, tripItemForOptimization.carUtility, tripItemForOptimization.ptUtility).get(1);
            tripItemForOptimization.ptProbability=calculateModeProbability(tripItemForOptimization.uamUtility, tripItemForOptimization.carUtility, tripItemForOptimization.ptUtility).get(2);
            double generalizedCostOneTripBefore= tripItemForOptimization.carGeneralizedCost*calculateModeProbability(-9999, tripItemForOptimization.carUtility, tripItemForOptimization.ptUtility).get(1)+ tripItemForOptimization.ptGeneralizedCost*calculateModeProbability(-9999, tripItemForOptimization.carUtility, tripItemForOptimization.ptUtility).get(2);
            double generalizedCostOneTripAfter= tripItemForOptimization.UAMGeneralizedCost* tripItemForOptimization.uamProbability+ tripItemForOptimization.carGeneralizedCost* tripItemForOptimization.carProbability+ tripItemForOptimization.ptGeneralizedCost* tripItemForOptimization.ptProbability;
            double savedGeneralizedCostOneTrip=generalizedCostOneTripBefore-generalizedCostOneTripAfter;
            if (savedGeneralizedCostOneTrip<0){
                savedGeneralizedCostOneTrip=0;
            }
            if (tripItemForOptimization.tripPurpose.startsWith("H")){
                savedGeneralizedCostOneTrip=savedGeneralizedCostOneTrip*2;
            }
            savedGeneralizedCost=savedGeneralizedCost+savedGeneralizedCostOneTrip;


        }



            /*
            int carCount=0;
            int ptCount=0;
            int uamCount=0;
            // generate 100 scenarios of each trip
            int j=0;
            for (int modeChoiceIterator=0;modeChoiceIterator<100;modeChoiceIterator++){
                j=modeChoiceIterator;
                ModeDecider modeDecider=new ModeDecider(tripItem.carProbability,tripItem.ptProbability,tripItem.uamProbability);
                String mode=modeDecider.decideMode();
                if (mode.equals("car")){
                    generalizedCostOneTrip=generalizedCostOneTrip+tripItem.carGeneralizedCost;
                    carCount++;
                }
                if (mode.equals("pt")){
                    generalizedCostOneTrip=generalizedCostOneTrip+tripItem.ptGeneralizedCost;
                    ptCount++;
                }
                if (mode.equals("uam")){
                    generalizedCostOneTrip=generalizedCostOneTrip+tripItem.UAMGeneralizedCost;
                    uamCount++;
                }
                if (tripItem.carProbability>0.9999 || tripItem.ptProbability>0.9999 || tripItem.uamProbability>0.9999){
                    carCount=carCount*100;
                    ptCount=ptCount*100;
                    uamCount=uamCount*100;
                    break;
                }
            }
            // calculate the average generalized cost of each trip
            generalizedCostOneTrip=generalizedCostOneTrip/(j+1);

             */




        score=savedGeneralizedCost;
        return score;
    }



}