package multi-agent-system;

import java.util.Arrays;

public class AStar {
	
	private int[] distances = new int[MyAgent.directions.length]; // distances array looks like e.x: {2,3,1,4,3} where each element is the heuristic value of every possible neighbor cell
	
	public String optimalPosition(String name, int[][] posAfter) {
		
		
		/***************************************************************  DISTANCES[DIRECTION] ARRAY  *****************************************************************************/
		int ag = Integer.parseInt(name.replaceAll("[\\D]", ""));
		int[][] posFirst = Grid.initialPosition;
		int[] posNow = MyAgent.currBlock[ag];
		int[] posLast = MyAgent.endBlock[ag];
		
		// calculates the Manhattan distance between posFirst & posAfter and posAfter & endBlock
		for(int drc = 0; drc < distances.length; drc++) {  // for each direction that the agent can possibly follow, calculate total cost f(x) = g(x) + h(x)
			
			if (posAfter[drc][0] > 4 || posAfter[drc][0] < 0 || posAfter[drc][1] > 4 || posAfter[drc][1] < 0) {
				distances[drc] = 2*(Math.abs(posFirst[ag][0]-posAfter[drc][0]) + Math.abs(posFirst[ag][1]-posAfter[drc][1])) + 8*(Math.abs(posLast[0]-posAfter[drc][0]) + Math.abs(posLast[1]-posAfter[drc][1])); 
			}
			else {
				distances[drc] = 2*((Grid.heuristics[ag][getArrayIndex(Grid.colours, posLast)][posFirst[ag][0]][posFirst[ag][1]]-Grid.heuristics[ag][getArrayIndex(Grid.colours, posLast)][posAfter[drc][0]][posAfter[drc][1]])) + 8*(Grid.heuristics[ag][getArrayIndex(Grid.colours, posLast)][posAfter[drc][0]][posAfter[drc][1]]);
			}
			//CHECKS IF THIS MOVE IS FORBIDDEN | IF SO IT GETS ELIMINATED BY ASSIGNING A VERY BIG COST TO IT
			for (int f = 0; f < Grid.forbiddenMoves.size(); f++) {
				if (isForbidden(ag, MyAgent.directions[drc], posNow)) {
					distances[drc] = 999999;
				}
			}
			distances[4] = 999999;  //staying "here" should not be an option under normal circumstances
    	}
    		
		
		/*********************************************************************  OPTIMAL DIRECTION  *********************************************************************************/      
        // gets the minimum distance g(x) = |posAfter - posFirst| & it's corresponding directions
        int minDistance = getMin(distances);
        String optDirection = getCorrespondingString(minDistance, distances);
	    return optDirection;
    }
	
	
	/**********************************************************************  METHOD: IS FORBIDDEN  *******************************************************************************/
	public static boolean isForbidden(int id, String drct, int[] position) { 
		for (int f = 0; f < Grid.forbiddenMoves.size(); f++) {
			if (Integer.parseInt((Grid.forbiddenMoves.get(f).get(3)).toString()) == id && Grid.forbiddenMoves.get(f).get(0).toString().equals(drct) && Integer.parseInt((Grid.forbiddenMoves.get(f).get(1)).toString()) == position[0] && Integer.parseInt((Grid.forbiddenMoves.get(f).get(2)).toString()) == position[1]) {
				return true;  // returns true if this move is known as "forbidden"
			}
			else {
				continue;
			}
		}
	    return false; 
	  } 
	
	
	/**********************************************************************  METHOD: GET MIN VALUE  *******************************************************************************/
	public static int getMin(int[] inputArray) {  // method for getting the minimum value of an array
	    int minValue = inputArray[0];
	    for(int i = 1; i < inputArray.length; i++){
	    	if(inputArray[i] < minValue){ 
	        minValue = inputArray[i];
	        } 
	    } 
	    return minValue; 
	  } 
	
	
	/*************************************************************  METHOD: GET CORRESPONDING STRING  *************************************************************************/
	public String getCorrespondingString(int minValue, int[] intArray){  /*method for getting the direction which corresponds
		to the minimum value of the distances array*/
		String best = null;
		for(int cs = 0; cs < intArray.length; cs++) { 
			if(intArray[cs] == minValue) {
				best = MyAgent.directions[cs];
				break;
			}
		}
		return best;
	}
	
	
	/************************************************************  METHOD: GET INDEX OF ELEMENT IN ARRAY  *************************************************************************/
	public static int getElementIndex(int arr[], int elmnt) {
		
	    for (int i = 0; i < arr.length; i++) {   // traverse in the array
	        if (arr[i] == elmnt) {
	        	return i;  // returns its index
	        }
	        else {
	        	continue;
	        }
	    }
	    return -1;
	}
		
		
	/*******************************************************  METHOD: GET INDEX OF SUB ARRAY IN SUPER ARRAY  *********************************************************************/
	public static int getArrayIndex(int super_arr[][], int sub_arr[]) {
		
	    for (int i = 0; i < super_arr.length; i++) {   // traverse in the array
	        if (Arrays.equals(super_arr[i], sub_arr)) {
	        	return i;  // returns its index
	        }
	        else {
	        	continue;
	        }
	    }
	    return -1;
	}
}
