package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes.LegacyGraphQLAbsoluteDirection;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes.LegacyGraphQLRelativeDirection;
import org.opentripplanner.ext.legacygraphqlapi.mapping.StreetNoteMapper;
import org.opentripplanner.model.plan.ElevationProfile.Step;
import org.opentripplanner.model.plan.WalkStep;
import org.opentripplanner.routing.alertpatch.TransitAlert;

public class LegacyGraphQLstepImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLStep {

  @Override
  public DataFetcher<LegacyGraphQLAbsoluteDirection> absoluteDirection() {
    return environment ->
      getSource(environment)
        .getAbsoluteDirection()
        .map(dir ->
          switch (dir) {
            case NORTH -> LegacyGraphQLAbsoluteDirection.NORTH;
            case NORTHEAST -> LegacyGraphQLAbsoluteDirection.NORTHEAST;
            case EAST -> LegacyGraphQLAbsoluteDirection.EAST;
            case SOUTHEAST -> LegacyGraphQLAbsoluteDirection.SOUTHEAST;
            case SOUTH -> LegacyGraphQLAbsoluteDirection.SOUTH;
            case SOUTHWEST -> LegacyGraphQLAbsoluteDirection.SOUTHWEST;
            case WEST -> LegacyGraphQLAbsoluteDirection.WEST;
            case NORTHWEST -> LegacyGraphQLAbsoluteDirection.NORTHWEST;
          }
        )
        .orElse(null);
  }

  @Override
  public DataFetcher<Iterable<TransitAlert>> alerts() {
    return environment ->
      getSource(environment)
        .getStreetNotes()
        .stream()
        .map(StreetNoteMapper::mapStreetNoteToAlert)
        .toList();
  }

  @Override
  public DataFetcher<Boolean> area() {
    return environment -> getSource(environment).getArea();
  }

  @Override
  public DataFetcher<Boolean> bogusName() {
    return environment -> getSource(environment).getBogusName();
  }

  @Override
  public DataFetcher<Double> distance() {
    return environment -> getSource(environment).getDistance();
  }

  @Override
  public DataFetcher<Iterable<Step>> elevationProfile() {
    return environment -> getSource(environment).getElevationProfile().steps();
  }

  @Override
  public DataFetcher<String> exit() {
    return environment -> getSource(environment).getExit();
  }

  @Override
  public DataFetcher<Double> lat() {
    return environment -> getSource(environment).getStartLocation().latitude();
  }

  @Override
  public DataFetcher<Double> lon() {
    return environment -> getSource(environment).getStartLocation().longitude();
  }

  @Override
  public DataFetcher<LegacyGraphQLRelativeDirection> relativeDirection() {
    return environment ->
      switch (getSource(environment).getRelativeDirection()) {
        case DEPART -> LegacyGraphQLRelativeDirection.DEPART;
        case HARD_LEFT -> LegacyGraphQLRelativeDirection.HARD_LEFT;
        case LEFT -> LegacyGraphQLRelativeDirection.LEFT;
        case SLIGHTLY_LEFT -> LegacyGraphQLRelativeDirection.SLIGHTLY_LEFT;
        case CONTINUE -> LegacyGraphQLRelativeDirection.CONTINUE;
        case SLIGHTLY_RIGHT -> LegacyGraphQLRelativeDirection.SLIGHTLY_RIGHT;
        case RIGHT -> LegacyGraphQLRelativeDirection.RIGHT;
        case HARD_RIGHT -> LegacyGraphQLRelativeDirection.HARD_RIGHT;
        case CIRCLE_CLOCKWISE -> LegacyGraphQLRelativeDirection.CIRCLE_CLOCKWISE;
        case CIRCLE_COUNTERCLOCKWISE -> LegacyGraphQLRelativeDirection.CIRCLE_COUNTERCLOCKWISE;
        case ELEVATOR -> LegacyGraphQLRelativeDirection.ELEVATOR;
        case UTURN_LEFT -> LegacyGraphQLRelativeDirection.UTURN_LEFT;
        case UTURN_RIGHT -> LegacyGraphQLRelativeDirection.UTURN_RIGHT;
      };
  }

  @Override
  public DataFetcher<Boolean> stayOn() {
    return environment -> getSource(environment).getStayOn();
  }

  @Override
  public DataFetcher<String> streetName() {
    return environment -> getSource(environment).getStreetName().toString(environment.getLocale());
  }

  @Override
  public DataFetcher<Boolean> walkingBike() {
    return environment -> getSource(environment).isWalkingBike();
  }

  private WalkStep getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
