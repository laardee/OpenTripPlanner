package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.api.mapping.PlannerErrorMapper;
import org.opentripplanner.api.resource.DebugOutput;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.util.ResourceBundleSingleton;

import java.util.stream.Collectors;

public class LegacyGraphQLPlanImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLPlan {

  @Override
  public DataFetcher<Long> date() {
    return environment -> getSource(environment).getTripPlan().date.getTime();
  }

  @Override
  public DataFetcher<Place> from() {
    return environment -> getSource(environment).getTripPlan().from;
  }

  @Override
  public DataFetcher<Place> to() {
    return environment -> getSource(environment).getTripPlan().to;
  }

  @Override
  public DataFetcher<Iterable<Itinerary>> itineraries() {
    return environment -> getSource(environment).getTripPlan().itineraries;
  }

  @Override
  public DataFetcher<Iterable<String>> messageEnums() {
    return environment -> getSource(environment)
        .getRoutingErrors()
        .stream()
        .map(routingError -> routingError.code)
        .map(Enum::name)
        .collect(Collectors.toList());
  }

  @Override
  public DataFetcher<Iterable<String>> messageStrings() {
    return environment -> getSource(environment)
        .getRoutingErrors()
        .stream()
        .map(PlannerErrorMapper::mapMessage)
        .map(plannerError -> plannerError.message.get(environment.getLocale()))
        .collect(Collectors.toList());
  }

  //TODO
  @Override
  public DataFetcher<DebugOutput> debugOutput() {
    return environment -> null;
  }

  private RoutingResponse getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
