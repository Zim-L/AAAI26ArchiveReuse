package operators;

import core.SelectionOperator;
import core.SolutionListUtils;
import core.Check;

import java.util.List;

/**
 * This class implements a random selection operator used for selecting randomly N solutions from a
 * list
 *
 * @author Antonio J. Nebro
 * @version 1.0
 */
@SuppressWarnings("serial")
public class NaryRandomSelection<S> implements SelectionOperator<List<S>, List<S>> {
  private int numberOfSolutionsToBeReturned;

  /** Constructor */
  public NaryRandomSelection() {
    this(1);
  }

  /** Constructor */
  public NaryRandomSelection(int numberOfSolutionsToBeReturned) {
    this.numberOfSolutionsToBeReturned = numberOfSolutionsToBeReturned;
  }

  /** Execute() method */
  public List<S> execute(List<S> solutionList) {
    Check.notNull(solutionList);
    Check.collectionIsNotEmpty(solutionList);
    Check.that(
        solutionList.size() >= numberOfSolutionsToBeReturned,
        "The solution list size ("
            + solutionList.size()
            + ") is less than "
            + "the number of requested solutions ("
            + numberOfSolutionsToBeReturned
            + ")");

    return SolutionListUtils.selectNRandomDifferentSolutions(
        numberOfSolutionsToBeReturned, solutionList);
  }
}
