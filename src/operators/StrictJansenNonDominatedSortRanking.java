package operators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import core.Check;
import core.ConstraintViolationComparator;
import core.DominanceComparator;
import core.JMetalException;
import core.Ranking;
import core.Solution;

public class StrictJansenNonDominatedSortRanking<S extends Solution<?>> implements Ranking<S> {
	private String attributeId = getClass().getName();
	private Comparator<S> dominanceComparator;
	private static final Comparator<Solution<?>> CONSTRAINT_VIOLATION_COMPARATOR = new ConstraintViolationComparator<Solution<?>>();

	private List<ArrayList<S>> rankedSubPopulations;

	/** Constructor */
	public StrictJansenNonDominatedSortRanking(Comparator<S> comparator) {
		this.dominanceComparator = comparator;
		rankedSubPopulations = new ArrayList<>();
	}

	/** Constructor */
	public StrictJansenNonDominatedSortRanking() {
		this(new DominanceComparator<>());
	}

	@Override
	public Ranking<S> compute(List<S> solutionList) {
		if (solutionList == null)
			throw new JMetalException(getClass().getSimpleName() + ".compute() receives a null list");
		if (solutionList.isEmpty()) {
			rankedSubPopulations = new ArrayList<>();
			return this;
		}
		if (solutionList.get(0).objectives().length != 2)
			throw new JMetalException(getClass().getSimpleName() + " is bi-objective ONLY, detected "
					+ solutionList.get(0).objectives().length + " objectives");

		// 1. Split by feasibility
		List<S> feasible = new ArrayList<>(), infeasible = new ArrayList<>();
		for (S sol : solutionList) {
			double[] cons = sol.constraints();
			boolean ok = Arrays.stream(cons).allMatch(c -> c == 0);
			(ok ? feasible : infeasible).add(sol);
		}

		// 2. Sort feasibles by (f1, f2)
		feasible.sort(
				Comparator.comparingDouble((S s) -> s.objectives()[0]).thenComparingDouble(s -> s.objectives()[1]));

		// 3. Assign fronts via best-f2 array
		List<Double> bestF2PerFront = new ArrayList<>();
		rankedSubPopulations = new ArrayList<>();

		for (S sol : feasible) {
			double f2 = sol.objectives()[1];
			int rank = locateFront(bestF2PerFront, f2);

			if (rank == bestF2PerFront.size()) {
				bestF2PerFront.add(f2);
				rankedSubPopulations.add(new ArrayList<>());
			} else {
				bestF2PerFront.set(rank, f2);
			}

			sol.attributes().put(attributeId, rank);
			rankedSubPopulations.get(rank).add(sol);
		}

		// 4. Append infeasibles as worst fronts
		infeasible.sort(Comparator.comparingDouble(s -> Arrays.stream(s.constraints()).sum()));
		int startRank = bestF2PerFront.size();
		for (S sol : infeasible) {
			sol.attributes().put(attributeId, startRank);
			if (rankedSubPopulations.size() <= startRank) {
				rankedSubPopulations.add(new ArrayList<>());
			}
			rankedSubPopulations.get(startRank).add(sol);
		}

		return this;
	}

	/** Binary search the first front whose best-f2 > candidate f2 */
	private int locateFront(List<Double> bestF2, double f2) {
		int lo = 0, hi = bestF2.size() - 1;
		while (lo <= hi) {
			int mid = lo + (hi - lo) / 2;
			if (bestF2.get(mid) > f2) {
				hi = mid - 1;
			} else {
				lo = mid + 1;
			}
		}
		return lo;
	}

	@Override
	public List<S> getSubFront(int rank) {
		Check.that(rank < rankedSubPopulations.size(), "Invalid rank: " + rank);
		return rankedSubPopulations.get(rank);
	}

	@Override
	public int getNumberOfSubFronts() {
		return rankedSubPopulations.size();
	}

	@Override
	public Integer getRank(S solution) {
		Object v = solution.attributes().get(attributeId);
		return (v == null ? -1 : (Integer) v);
	}

	@Override
	public Object getAttributedId() {
		return attributeId;
	}

}
