package operators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import core.MutationOperator;
import core.PermutationSolution;
import core.Check;
import core.BoundedRandomGenerator;
import core.JMetalRandom;
import core.RandomGenerator;

public class InsertionMutation<T> implements MutationOperator<PermutationSolution<T>> {

	private double mutationProbability;
	private RandomGenerator<Double> mutationRandomGenerator;
	private BoundedRandomGenerator<Integer> positionRandomGenerator;

	public InsertionMutation(double mutationProbability) {
		this(
		        mutationProbability,
		        () -> JMetalRandom.getInstance().nextDouble(),
		        (a, b) -> JMetalRandom.getInstance().nextInt(a, b));
	}
	
	 /** Constructor */
	  public InsertionMutation(
	      double mutationProbability, RandomGenerator<Double> randomGenerator) {
	    this(
	        mutationProbability,
	        randomGenerator,
	        BoundedRandomGenerator.fromDoubleToInteger(randomGenerator));
	  }

	public InsertionMutation(double mutationProbability, 
			RandomGenerator<Double> mutationRandomGenerator, 
			BoundedRandomGenerator<Integer> positionRandomGenerator) {
		Check.probabilityIsValid(mutationProbability);
	    this.mutationProbability = mutationProbability;
	    this.mutationRandomGenerator = mutationRandomGenerator;
	    this.positionRandomGenerator = positionRandomGenerator;
	}

	@Override
	public double getMutationProbability() {
		return mutationProbability;
	}

	@Override
	public PermutationSolution<T> execute(PermutationSolution<T> solution) {
		Check.notNull(solution);

	    doMutation(solution);
	    return solution;
	}

	private void doMutation(PermutationSolution<T> solution) {
		int permutationLength;
	    permutationLength = solution.variables().size();
	    
	    if ((permutationLength != 0) && (permutationLength != 1)) {
	    	if (mutationRandomGenerator.getRandomValue() < mutationProbability) {
	    		int pos1 = positionRandomGenerator.getRandomValue(0, solution.variables().size()-1);
	    		int pos2 = positionRandomGenerator.getRandomValue(0, solution.variables().size()-1);
	    		while (pos2==pos1) pos2 = positionRandomGenerator.getRandomValue(0, solution.variables().size()-1);
	    		if (pos2<pos1) {
	    			int temp = pos1;
	    			pos1 = pos2;
	    			pos2 = temp;
	    		}
	    		T temp = solution.variables().get(pos1);
	    		for (int i=pos1; i<pos2; i++) {
	    			solution.variables().set(i, solution.variables().get(i+1));
	    		}
	    		solution.variables().set(pos2, temp);
	    	}

  		  for (int l1=0; l1<permutationLength; l1++)
  		  for (int l2=0; l2<permutationLength; l2++)
  			  if (l1!=l2)
  		      if (solution.variables().get(l1)==solution.variables().get(l2)) System.out.println("mutationBANG");
	    }
		
	}

}
