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
public class RandomSelection<S> implements SelectionOperator<List<S>, S> {

  /** Execute() method */
  public S execute(List<S> solutionList) {
    Check.notNull(solutionList);
    Check.collectionIsNotEmpty(solutionList);


    return SolutionListUtils.selectNRandomDifferentSolutions(
        1, solutionList).get(0);
  }
}
