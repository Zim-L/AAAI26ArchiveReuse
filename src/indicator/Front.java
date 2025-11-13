package indicator;


import java.io.Serializable;
import java.util.Comparator;

import core.Point;

/**
 * A front is a list of points
 *
 * @author Antonio J. Nebro
 */
@Deprecated
public interface Front extends Serializable {
  int getNumberOfPoints();

  int getPointDimensions();

  Point getPoint(int index);

  void setPoint(int index, Point point);

  void sort(Comparator<Point> comparator);

  double[][] getMatrix() ;
}
