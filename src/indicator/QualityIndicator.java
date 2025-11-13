package indicator;


import java.io.Serializable;

/**
 * @author Antonio J. Nebro <antonio@lcc.uma.es>

 * @param <Evaluate> Entity to runAlgorithm
 * @param <Result> Result of the evaluation
 */
@Deprecated
public interface QualityIndicator<Evaluate, Result> extends Serializable {
  Result evaluate(Evaluate evaluate) ;
}
