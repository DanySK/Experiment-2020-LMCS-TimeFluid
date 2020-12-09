/*
 * Copyright (C) 2010-2019, Danilo Pianini and contributors listed in the main project's alchemist/build.gradle file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.model.implementations.timedistributions;

import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import it.unibo.alchemist.model.implementations.times.DoubleTime;
import it.unibo.alchemist.model.math.RealDistributionUtil;
import it.unibo.alchemist.model.interfaces.Environment;
import it.unibo.alchemist.model.interfaces.Time;

/**
 * This class is able to use any distribution provided by Apache Math 3 as a
 * subclass of {@link RealDistribution}.
 * {@link it.unibo.alchemist.model.interfaces.Condition#getPropensityContribution()}.
 *
 * @param <T>
 *            concentration type
 */
public class AnyRealDistribution<T> extends AbstractDistribution<T> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private final RealDistribution distribution;
    private Time next;

    /**
     * @param rng
     *            the {@link RandomGenerator}
     * @param distribution
     *            the distribution name (case insensitive). Must be mappable to an entity implementing {@link RealDistribution}
     * @param parameters
     *            the parameters for the distribution
     */
    public AnyRealDistribution(final RandomGenerator rng, final String distribution, final double... parameters) {
        this(Time.ZERO, rng, distribution, parameters);
    }

    /**
     * @param start
     *            the initial time
     * @param rng
     *            the {@link RandomGenerator}
     * @param distribution
     *            the distribution name (case insensitive). Must be mappable to an entity implementing {@link RealDistribution}
     * @param parameters
     *            the parameters for the distribution
     */
    public AnyRealDistribution(
            final Time start,
            final RandomGenerator rng,
            final String distribution,
            final double... parameters
    ) {
        this(start, RealDistributionUtil.makeRealDistribution(rng, distribution, parameters));
    }

    /**
     * @param distribution
     *            the {@link AnyRealDistribution} to use. To ensure
     *            reproducibility, such distribution must get created using the
     *            simulation {@link RandomGenerator}.
     */
    public AnyRealDistribution(final RealDistribution distribution) {
        this(Time.ZERO, distribution);
    }

    /**
     * @param start
     *            distribution start time
     * @param distribution
     *            the {@link AnyRealDistribution} to use. To ensure
     *            reproducibility, such distribution must get created using the
     *            simulation {@link RandomGenerator}.
     */
    public AnyRealDistribution(final Time start, final RealDistribution distribution) {
        super(start);
        this.distribution = distribution;
    }

    @Override
    public final double getRate() {
        return distribution.getNumericalMean();
    }

    @Override
    protected final void updateStatus(
            final Time currentTime,
            final boolean hasBeenExecuted,
            final double additionalParameter,
            final Environment<T, ?> environment
    ) {
        if (
            next == null
            || hasBeenExecuted
            || currentTime.compareTo(next) < 0
            || additionalParameter > 0 && getNextOccurence().isInfinite()
        ) {
            // New time generation necessary
            next = new DoubleTime(currentTime.toDouble() + distribution.sample());
        }
        if (additionalParameter == 0) {
            // The execution is blocked
            setTau(Time.INFINITY);
        } else {
            setTau(next);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractDistribution<T> clone(final Time currentTime) {
        return new AnyRealDistribution<>(currentTime, distribution);
    }

}
