package core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
//import java.util.Map;
//import java.util.NavigableMap;
//import java.util.TreeMap;

/**
 * This class implements an archive containing non-dominated solutions
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 * @author Juan J. Durillo
 * @modified by Zimin Liang, 2025
 */
@SuppressWarnings("serial")
public class NonDominatedSolutionListArchive<S extends Solution<?>> implements Archive<S> {

    /* ==== Generic fields ==== */
    private List<S> solutionList;
    private Comparator<S> dominanceComparator;
    private Comparator<S> equalSolutions = new EqualSolutionsComparator<S>();

    /* ==== Optimisation flags ==== */
    private boolean biObjectiveMode = false;
    private List<S> sortedList; // Used only for 2D case
    private Comparator<S> objective0Comparator;

    private int numObjectives = -1; // Track dimensionality for consistency

    /** Default constructor */
    public NonDominatedSolutionListArchive() {
        this(new DominanceComparator<S>());
    }

    /** Constructor with custom comparator */
    public NonDominatedSolutionListArchive(DominanceComparator<S> comparator) {
        dominanceComparator = comparator;
        solutionList = new ArrayList<>();
    }

    /** Detects and activates bi-objective mode if appropriate */
    private void activateBiObjectiveMode() {
        biObjectiveMode = true;
        sortedList = new ArrayList<>();
        objective0Comparator = Comparator.comparingDouble(s -> s.objectives()[0]);
        // Transfer existing solutions into sorted structure
        for (S s : solutionList) {
            addBiObjective(s);
        }
        solutionList.clear();
    }

    @Override
    public boolean add(S solution) {
        /* --- Sanity check for dimensionality consistency --- */
        int dim = solution.objectives().length;

        if (numObjectives == -1) {
            numObjectives = dim;
        } else if (dim != numObjectives) {
            throw new IllegalStateException(
                String.format("Inconsistent objective dimensions: existing=%d, new=%d",
                              numObjectives, dim)
            );
        }

        /* --- Mode activation logic --- */
        if (dim == 2) {
            if (!biObjectiveMode) {
                activateBiObjectiveMode();
            }
        } else {
            if (biObjectiveMode) {
                throw new IllegalStateException(
                    "Cannot mix bi-objective mode with higher-dimensional solutions"
                );
            }
        }

        /* --- Route to correct insertion method --- */
        if (biObjectiveMode) {
            return addBiObjective(solution);
        } else {
            return addGeneral(solution);
        }
    }

    /** Generic O(n) insertion (legacy mode) */
    private boolean addGeneral(S solution) {
        boolean solutionInserted = false;

        if (solutionList.isEmpty()) {
            solutionList.add(solution);
            return true;
        }

        Iterator<S> iterator = solutionList.iterator();
        boolean isDominated = false;
        boolean isContained = false;

        while ((!isDominated && !isContained) && iterator.hasNext()) {
            S listIndividual = iterator.next();
            int flag = dominanceComparator.compare(solution, listIndividual);

            if (flag == -1) {
                iterator.remove();
            } else if (flag == 1) {
                isDominated = true;
            } else if (flag == 0) {
                int eqFlag = equalSolutions.compare(solution, listIndividual);
                if (eqFlag == 0) isContained = true;
            }
        }

        if (!isDominated && !isContained) {
            solutionList.add(solution);
            solutionInserted = true;
        }

        return solutionInserted;
    }

    /** Optimised O(log n) insertion for bi-objective case */
    private boolean addBiObjective(S solution) {
        double f1 = solution.objectives()[0];
        double f2 = solution.objectives()[1];

        // Binary search by f1
        int index = Collections.binarySearch(sortedList, solution, objective0Comparator);
        if (index < 0) index = -index - 1;

        // Check if dominated by previous
        if (index > 0) {
            S prev = sortedList.get(index - 1);
            if (prev.objectives()[1] <= f2) {
                return false; // dominated
            }
        }

        // Remove all following dominated solutions
        int removeStart = index;
        while (removeStart < sortedList.size() &&
                sortedList.get(removeStart).objectives()[1] >= f2) {
            removeStart++;
        }
        if (removeStart > index)
            sortedList.subList(index, removeStart).clear();

        // Check equality
        if (index < sortedList.size()) {
            S next = sortedList.get(index);
            if (next.objectives()[0] == f1 && next.objectives()[1] == f2)
                return false;
        }

        sortedList.add(index, solution);
        return true;
    }

    /** Join another archive */
    public Archive<S> join(Archive<S> archive) {
        return this.addAll(archive.getSolutionList());
    }

    /** Add all solutions */
    public Archive<S> addAll(List<S> list) {
        for (S s : list) add(s);
        return this;
    }

    /** Retrieve solution list (depends on mode) */
    @Override
    public List<S> getSolutionList() {
        return biObjectiveMode ? sortedList : solutionList;
    }

    @Override
    public int size() {
        return biObjectiveMode ? sortedList.size() : solutionList.size();
    }

    @Override
    public S get(int index) {
        return biObjectiveMode ? sortedList.get(index) : solutionList.get(index);
    }

    public void remove(S candidate) {
        if (biObjectiveMode) sortedList.remove(candidate);
        else solutionList.remove(candidate);
    }
}