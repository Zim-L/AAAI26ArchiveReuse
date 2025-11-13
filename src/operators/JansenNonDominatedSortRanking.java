package operators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import core.Check;
import core.ConstraintViolationComparator;
import core.DominanceComparator;
import core.JMetalException;
import core.Ranking;
import core.Solution;


public class JansenNonDominatedSortRanking<S extends Solution<?>> implements Ranking<S> {
	private String attributeId = getClass().getName();
	private Comparator<S> dominanceComparator;
	private static final Comparator<Solution<?>> CONSTRAINT_VIOLATION_COMPARATOR = new ConstraintViolationComparator<Solution<?>>();

	private List<ArrayList<S>> rankedSubPopulations;

	/** Constructor */
	public JansenNonDominatedSortRanking(Comparator<S> comparator) {
		this.dominanceComparator = comparator;
		rankedSubPopulations = new ArrayList<>();
	}

	/** Constructor */
	public JansenNonDominatedSortRanking() {
		this(new DominanceComparator<>());
	}

	@Override
	public Ranking<S> compute(List<S> solutionList) {
		if (solutionList == null)
			throw new JMetalException(this.getClass().getSimpleName() + ".compute() receives a null object ");
		if (solutionList.size() == 0) {
			rankedSubPopulations = new ArrayList<>();
			return this;
		}
		if (solutionList.get(0).objectives().length != 2)
			throw new JMetalException(this.getClass().getSimpleName() + " is bi-objective ONLY, detected "
					+ solutionList.get(0).objectives().length + " objectives");
		
		// Constraint handling: separate valid and invalid solutions
		List<S> validSolutions = new ArrayList<>();
		List<S> invalidSolutions = new ArrayList<>();

		for (S solution : solutionList) {
			double[] constraints = solution.constraints();
			boolean isValid = Arrays.stream(constraints).allMatch(c -> c == 0);
			if (isValid) {
				validSolutions.add(solution);
			} else {
				invalidSolutions.add(solution);
			}
		}

		// Sort valid solutions by f1, with f2 as a tiebreaker
		validSolutions.sort((s1, s2) -> {
			double[] obj1 = s1.objectives();
			double[] obj2 = s2.objectives();
			if (obj1[0] != obj2[0]) {
				return Double.compare(obj1[0], obj2[0]);
			} else {
				return Double.compare(obj1[1], obj2[1]);
			}
		});
		
		// Containers for rank assignment and tracking best f2 per front
        List<Double> bestF2PerFront = new ArrayList<>();
        rankedSubPopulations = new ArrayList<>();
        
        double pf1 = Double.MAX_VALUE, pf2 = Double.MAX_VALUE;
        int prank = 0;
        for (S solution : validSolutions) {
        	double f1 = solution.objectives()[0];
            double f2 = solution.objectives()[1];
            if (pf1==f1 && pf2==f2) {
            	solution.attributes().put(attributeId, prank);
            	rankedSubPopulations.get(prank).add(solution);
            	continue;
            } 
            
            int rank = binarySearchBestF2(bestF2PerFront, f2);
            pf1 = f1;
            pf2 = f2;
            prank = rank;

            if (rank == bestF2PerFront.size()) {
                // New front
                bestF2PerFront.add(f2);
                rankedSubPopulations.add(new ArrayList<>());
            } else {
                // Update best f2 for the front
                bestF2PerFront.set(rank, f2);
            }

            // Assign rank to the solution
            solution.attributes().put(attributeId, rank);
            rankedSubPopulations.get(rank).add(solution);
        }
        
        // Sort invalid solutions by the sum of constraints
        invalidSolutions.sort((s1, s2) -> {
            double sum1 = Arrays.stream(s1.constraints()).sum();
            double sum2 = Arrays.stream(s2.constraints()).sum();
            return Double.compare(sum1, sum2);
        });

        // Assign ranks to invalid solutions (starting from the next front)
        int invalidStartRank = bestF2PerFront.size();
        for (S solution : invalidSolutions) {
            solution.attributes().put(attributeId, invalidStartRank);
            if (rankedSubPopulations.size() <= invalidStartRank) {
                rankedSubPopulations.add(new ArrayList<>());
            }
            rankedSubPopulations.get(invalidStartRank).add(solution);
        }

		return this;
	}

	private int binarySearchBestF2(List<Double> bestF2PerFront, double f2) {
		int low = 0, high = bestF2PerFront.size() - 1;
		while (low <= high) {
			int mid = low + (high - low) / 2;
			if (bestF2PerFront.get(mid) > f2) {
				high = mid - 1;
			} else {
				low = mid + 1;
			}
		}
		return low;
	}

	@Override
	public List<S> getSubFront(int rank) {
		Check.that(rank < rankedSubPopulations.size(),
				"Invalid rank: " + rank + ". Max rank = " + (rankedSubPopulations.size() - 1));

		return rankedSubPopulations.get(rank);
	}

	@Override
	public int getNumberOfSubFronts() {
		return rankedSubPopulations.size();
	}

	@Override
	public Integer getRank(S solution) {
		Check.notNull(solution);

		Integer result = -1;
		if (solution.attributes().get(attributeId) != null) {
			result = (Integer) solution.attributes().get(attributeId);
		}
		return result;
	}

	@Override
	public Object getAttributedId() {
		return attributeId;
	}

}
