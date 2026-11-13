package com.marosirobi.workouttrackerapi.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GeoUtilsTest {

    @Test
    void samePoint_isZero() {
        assertThat(GeoUtils.distanceMeters(47.4979, 19.0402, 47.4979, 19.0402)).isZero();
    }

    @Test
    void oneDegreeLatitude_isAbout111Km() {
        assertThat(GeoUtils.distanceMeters(0, 0, 1, 0)).isCloseTo(111_195, within(1.0));
    }

    @Test
    void budapestToDebrecen() {
        assertThat(GeoUtils.distanceMeters(47.4979, 19.0402, 47.5316, 21.6273)).isCloseTo(194_321, within(10.0));
    }

    @Test
    void isSymmetric() {
        double there = GeoUtils.distanceMeters(47.4979, 19.0402, 46.2530, 20.1414);
        double back = GeoUtils.distanceMeters(46.2530, 20.1414, 47.4979, 19.0402);

        assertThat(there).isCloseTo(back, within(1e-6));
    }
}
