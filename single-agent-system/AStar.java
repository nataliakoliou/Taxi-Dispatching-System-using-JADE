package single-agent-system;

import java.util.Arrays;

public class AStar {
	
	private int[] distances = new int[4]; // distances array looks like e.x: {2,3,1,4} where each element i = nextPos_i - initialPos
	
	
	public String optimalPosition(String name, int[][] posFirst, int[] posNow, int[][] posAfter, int[][] posLast) {
		
		int ag = Integer.parseInt(name.replaceAll("[\\D]", "")); 
		
		// calculates the Manhattan distance between posFirst & posAfter and posAfter & posLast
		for(int drc = 0; drc < 4; drc++) {  // for each direction that the agent can possibly follow, calculate total cost f(x) = g(x) + h(x)
        	distances[drc] = 2*(Math.abs(posFirst[ag][0]-posAfter[drc][0]) + Math.abs(posFirst[ag][1]-posAfter[drc][1])) + 4*(Math.abs(posLast[ag][0]-posAfter[drc][0]) + Math.abs(posLast[ag][1]-posAfter[drc][1]));  
			//distances[drc] = distances[drc] + Grid.utility[posAfter[drc][0] + 1][posAfter[drc][1] + 1]; TODO: PLEASE IGNORE FOR NOW
        
				
			//CHECKS IF THIS MOVE IS FORBIDDEN | IF SO IT GETS ELIMINATED BY ASSIGNING A VERY BIG COST TO IT
			for (int f = 0; f < Grid.forbiddenMoves.size(); f++) {
				if (Grid.forbiddenMoves.get(f).get(0).toString().equals(MyAgent.directions[drc]) && Integer.parseInt((Grid.forbiddenMoves.get(f).get(1)).toString()) == posNow[0] && Integer.parseInt((Grid.forbiddenMoves.get(f).get(2)).toString()) == posNow[1]) {
					distances[drc] = 999999;
				}
			}
    	}
    		
    	for (int f = 0; f < Grid.forbiddenMoves.size(); f++) {
    		//GOAL POSITION IS G
    		if (posLast[ag][0] == 0 && posLast[ag][1] == 4) {
				if (posNow[0] == 0 && posNow[1] == 1 && Grid.forbiddenMoves.get(f).get(0).toString().equals(MyAgent.directions[1]) && Integer.parseInt((Grid.forbiddenMoves.get(f).get(1)).toString()) == posNow[0] && Integer.parseInt((Grid.forbiddenMoves.get(f).get(2)).toString()) == posNow[1]) {
					return "down";
				}
				else if (posNow[0] == 1 && posNow[1] == 1) {
					if (Grid.forbiddenMoves.get(f).get(0).toString().equals(MyAgent.directions[1]) && Integer.parseInt((Grid.forbiddenMoves.get(f).get(1)).toString()) == posNow[0] && Integer.parseInt((Grid.forbiddenMoves.get(f).get(2)).toString()) == posNow[1]) {
						return "down";
					}
					else {
						if (f == Grid.forbiddenMoves.size() - 1) {
							return "right";  //instead of choosing to go up
						}
						continue;
					}
				}
                else if (posNow[0] == 2 && posNow[1] == 1 && Grid.forbiddenMoves.get(f).get(0).toString().equals(MyAgent.directions[1]) && Integer.parseInt((Grid.forbiddenMoves.get(f).get(1)).toString()) == (posNow[0] - 1) && Integer.parseInt((Grid.forbiddenMoves.get(f).get(2)).toString()) == posNow[1]) {
					return "right";
				}
			}
    		//GOAL POSITION IS R
    		if (posLast[ag][0] == 0 && posLast[ag][1] == 0) {
				if (posNow[0] == 0 && posNow[1] == 2 && Grid.forbiddenMoves.get(f).get(0).toString().equals(MyAgent.directions[0]) && Integer.parseInt((Grid.forbiddenMoves.get(f).get(1)).toString()) == posNow[0] && Integer.parseInt((Grid.forbiddenMoves.get(f).get(2)).toString()) == posNow[1]) {
					return "down";
				}
				else if (posNow[0] == 1 && posNow[1] == 2) {
					if (Grid.forbiddenMoves.get(f).get(0).toString().equals(MyAgent.directions[0]) && Integer.parseInt((Grid.forbiddenMoves.get(f).get(1)).toString()) == posNow[0] && Integer.parseInt((Grid.forbiddenMoves.get(f).get(2)).toString()) == posNow[1]) {
						return "down";
					}
					else {
						if (f == Grid.forbiddenMoves.size() - 1) {
								return "left";  //instead of choosing to go up
						}
						continue;
					}
				}
                else if (posNow[0] == 2 && posNow[1] == 2 && Grid.forbiddenMoves.get(f).get(0).toString().equals(MyAgent.directions[0]) && Integer.parseInt((Grid.forbiddenMoves.get(f).get(1)).toString()) == (posNow[0] - 1) && Integer.parseInt((Grid.forbiddenMoves.get(f).get(2)).toString()) == posNow[1]) {
					return "left";
				}
			}
    		//GOAL POSITION IS B
    		if (posLast[ag][0] == 4 && posLast[ag][1] == 3) {
				if (((posNow[0] == 4 && posNow[1] == 0) || (posNow[0] == 4 && posNow[1] == 2)) && Grid.forbiddenMoves.get(f).get(0).toString().equals(MyAgent.directions[1]) && Integer.parseInt((Grid.forbiddenMoves.get(f).get(1)).toString()) == posNow[0] && Integer.parseInt((Grid.forbiddenMoves.get(f).get(2)).toString()) == posNow[1]) {
					return "up";
				}
				else if ((posNow[0] == 3 && posNow[1] == 0) || (posNow[0] == 3 && posNow[1] == 2)) {
					if (Grid.forbiddenMoves.get(f).get(0).toString().equals(MyAgent.directions[1]) && Integer.parseInt((Grid.forbiddenMoves.get(f).get(1)).toString()) == posNow[0] && Integer.parseInt((Grid.forbiddenMoves.get(f).get(2)).toString()) == posNow[1]) {
						return "up";
					}
					else {
						if (f == Grid.forbiddenMoves.size() - 1) {
							return "right";  //instead of choosing to go down
						}
						continue;
					}
				}
                else if (((posNow[0] == 2 && posNow[1] == 0) || (posNow[0] == 2 && posNow[1] == 2)) && Grid.forbiddenMoves.get(f).get(0).toString().equals(MyAgent.directions[1]) && Integer.parseInt((Grid.forbiddenMoves.get(f).get(1)).toString()) == (posNow[0] + 1) && Integer.parseInt((Grid.forbiddenMoves.get(f).get(2)).toString()) == posNow[1]) {
					return "right";
				}
			}
    		//GOAL POSITION IS Y
    		if (posLast[ag][0] == 4 && posLast[ag][1] == 0) {
				if (((posNow[0] == 4 && posNow[1] == 3) || (posNow[0] == 4 && posNow[1] == 1)) && Grid.forbiddenMoves.get(f).get(0).toString().equals(MyAgent.directions[0]) && Integer.parseInt((Grid.forbiddenMoves.get(f).get(1)).toString()) == posNow[0] && Integer.parseInt((Grid.forbiddenMoves.get(f).get(2)).toString()) == posNow[1]) {
					return "up";
				}
				else if ((posNow[0] == 3 && posNow[1] == 3) || (posNow[0] == 3 && posNow[1] == 1)) {
					if (Grid.forbiddenMoves.get(f).get(0).toString().equals(MyAgent.directions[0]) && Integer.parseInt((Grid.forbiddenMoves.get(f).get(1)).toString()) == posNow[0] && Integer.parseInt((Grid.forbiddenMoves.get(f).get(2)).toString()) == posNow[1]) {
						return "up";
					}
					else {
						if (f == Grid.forbiddenMoves.size() - 1) {
							return "left";  //instead of choosing to go down
						}
						continue;
					}
				}
                else if (((posNow[0] == 2 && posNow[1] == 1) || (posNow[0] == 2 && posNow[1] == 3)) && Grid.forbiddenMoves.get(f).get(0).toString().equals(MyAgent.directions[0]) && Integer.parseInt((Grid.forbiddenMoves.get(f).get(1)).toString()) == (posNow[0] + 1) && Integer.parseInt((Grid.forbiddenMoves.get(f).get(2)).toString()) == posNow[1]) {
					return "left"; 
				}
			}
    	}
        	
        //System.out.println("L, R, U, D costs:" + Arrays.toString(distances));
        System.out.println("Agent-" + ag + ": My goal position is > " + Arrays.deepToString(posLast));
    	
        // gets the minimum distance g(x) = |posAfter - posFirst| & it's corresponding directions
        int minDistance = getMin(distances);
        String optDirection = getCorrespondingString(minDistance, distances, MyAgent.directions);
		
	    return optDirection;
    }
	
	
	// method for getting the minimum value of an array
	public int getMin(int[] inputArray){
	    int minValue = inputArray[0]; 
	    for(int i=1;i<inputArray.length;i++){
	    	if(inputArray[i] < minValue){ 
	        minValue = inputArray[i];
	        } 
	    } 
	    return minValue; 
	  } 
	
	
	// method for getting the direction which corresponds to the minimum value of the distances array
		public String getCorrespondingString(int minValue, int[] intArray, String[] strArray){
			String best = null;
			for(int cs = 0; cs < intArray.length; cs++) { 
				if(intArray[cs] == minValue) {
					best = strArray[cs];
					break;
				}
			}
			return best;
		}
}
