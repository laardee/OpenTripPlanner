package org.opentripplanner.model.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.AbsoluteDirection.EAST;
import static org.opentripplanner.model.plan.AbsoluteDirection.NORTH;
import static org.opentripplanner.model.plan.AbsoluteDirection.SOUTHWEST;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;

public class WalkStepTest {

  @Test
  public void testRelativeDirection() {
    WalkStep step = new WalkStep(
      new NonLocalizedString("Any"),
      new WgsCoordinate(3.0, 4.0),
      false,
      0.0,
      false,
      false
    );

    double angle1 = degreesToRadians(0);
    double angle2 = degreesToRadians(90);

    step.setDirections(angle1, angle2, false);
    assertEquals(RelativeDirection.RIGHT, step.getRelativeDirection());
    assertEquals(Optional.of(EAST), step.getAbsoluteDirection());

    angle1 = degreesToRadians(0);
    angle2 = degreesToRadians(5);

    step.setDirections(angle1, angle2, false);
    assertEquals(RelativeDirection.CONTINUE, step.getRelativeDirection());
    assertEquals(Optional.of(NORTH), step.getAbsoluteDirection());

    angle1 = degreesToRadians(0);
    angle2 = degreesToRadians(240);

    step.setDirections(angle1, angle2, false);
    assertEquals(RelativeDirection.HARD_LEFT, step.getRelativeDirection());
    assertEquals(Optional.of(SOUTHWEST), step.getAbsoluteDirection());
  }

  private double degreesToRadians(double deg) {
    return deg * Math.PI / 180;
  }
}
