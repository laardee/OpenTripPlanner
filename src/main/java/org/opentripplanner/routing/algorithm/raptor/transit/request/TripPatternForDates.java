package org.opentripplanner.routing.algorithm.raptor.transit.request;

import java.util.Arrays;
import java.util.List;
import java.util.function.IntUnaryOperator;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternWithRaptorStopIndexes;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorConstrainedTripScheduleBoardingSearch;
import org.opentripplanner.transit.raptor.api.transit.RaptorRoute;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;

/**
 * A collection of all the TripSchedules active on a range of consecutive days. The outer list of tripSchedulesByDay
 * refers to days in order.
 */
public class TripPatternForDates
        implements
                RaptorRoute<TripSchedule>,
                RaptorTimeTable<TripSchedule>,
                RaptorTripPattern
{

    private final TripPatternWithRaptorStopIndexes tripPattern;

    private final TripPatternForDate[] tripPatternForDates;

    private final int[] offsets;

    private final int numberOfTripSchedules;

    private final int[] arrivalTimes;

    private final int[] departureTimes;

    TripPatternForDates(
            TripPatternWithRaptorStopIndexes tripPattern,
            List<TripPatternForDate> tripPatternForDates,
            List<Integer> offsets
    ) {
        this.tripPattern = tripPattern;
        this.tripPatternForDates = tripPatternForDates.toArray(new TripPatternForDate[]{});
        this.offsets = offsets.stream().mapToInt(i -> i).toArray();
        this.numberOfTripSchedules = Arrays.stream(this.tripPatternForDates).mapToInt(TripPatternForDate::numberOfTripSchedules).sum();
        final int nStops = tripPattern.getStopIndexes().length;
        this.arrivalTimes = new int[nStops * numberOfTripSchedules];
        this.departureTimes = new int[nStops * numberOfTripSchedules];
        int i = 0;
        for (int d = 0; d < tripPatternForDates.size(); d++) {
            int offset = this.offsets[d];
            for (var trip : tripPatternForDates.get(d).tripTimes()) {
                for (int s = 0; s < nStops; s++) {
                    this.arrivalTimes[s * numberOfTripSchedules + i] = trip.getArrivalTime(s) + offset;
                    this.departureTimes[s * numberOfTripSchedules + i] = trip.getDepartureTime(s) + offset;
                }
                i++;
            }
        }
    }

    public TripPatternWithRaptorStopIndexes getTripPattern() {
        return tripPattern;
    }

    // Implementing RaptorRoute
    @Override
    public RaptorTimeTable<TripSchedule> timetable() {
        return this;
    }

    @Override
    public RaptorTripPattern pattern() {
        return this;
    }

    @Override
    public RaptorConstrainedTripScheduleBoardingSearch<TripSchedule> transferConstraintsForwardSearch() {
        return getTripPattern().constrainedTransferForwardSearch();
    }

    @Override
    public RaptorConstrainedTripScheduleBoardingSearch<TripSchedule> transferConstraintsReverseSearch() {
        return getTripPattern().constrainedTransferReverseSearch();
    }

    // Implementing RaptorTripPattern

    @Override public int stopIndex(int stopPositionInPattern) {
        return tripPattern.stopIndex(stopPositionInPattern);
    }

    @Override
    public boolean boardingPossibleAt(int stopPositionInPattern) {
        return tripPattern.getPattern().canBoard(stopPositionInPattern);
    }

    @Override
    public boolean alightingPossibleAt(int stopPositionInPattern) {
        return tripPattern.getPattern().canAlight(stopPositionInPattern);
    }

    @Override public int numberOfStopsInPattern() {
        return tripPattern.getStopIndexes().length;
    }

    @Override
    public String debugInfo() {
        return tripPattern.getTransitMode().name() + " " + tripPattern.getPattern().getRoute().getShortName();
    }


    // Implementing RaptorTimeTable

    @Override public TripSchedule getTripSchedule(int index) {
        for (int i = 0; i < tripPatternForDates.length; i++) {
            TripPatternForDate tripPatternForDate = tripPatternForDates[i];

            if (index < tripPatternForDate.numberOfTripSchedules()) {
                return new TripScheduleWithOffset(this, tripPatternForDate.getLocalDate(),
                        tripPatternForDate.getTripTimes(index), offsets[i]);
            }
            index -= tripPatternForDate.numberOfTripSchedules();
        }
        throw new IndexOutOfBoundsException("Index out of bound: " + index);
    }

    @Override
    public IntUnaryOperator getArrivalTimes(int stopPositionInPattern) {
        final int base = stopPositionInPattern * numberOfTripSchedules;
        return (int i) -> arrivalTimes[base + i];
    }

    @Override
    public IntUnaryOperator getDepartureTimes(int stopPositionInPattern) {
        final int base = stopPositionInPattern * numberOfTripSchedules;
        return (int i) -> departureTimes[base + i];
    }

    @Override public int numberOfTripSchedules() {
        return numberOfTripSchedules;
    }

    @Override
    public String toString() {
        return ToStringBuilder.of(TripPatternForDates.class)
                .addObj("pattern", debugInfo())
                .addServiceTimeSchedule("offsets", offsets)
                .addNum("nTrips", numberOfTripSchedules)
                .toString();
    }
}
