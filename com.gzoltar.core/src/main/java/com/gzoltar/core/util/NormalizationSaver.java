/**
 * Copyright (C) 2020 GZoltar contributors.
 *
 * This file is part of GZoltar.
 *
 * GZoltar is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * GZoltar is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with GZoltar. If
 * not, see <https://www.gnu.org/licenses/>.
 */
package com.gzoltar.core.util;

import com.gzoltar.core.model.Node;

import java.util.*;

public final class NormalizationSaver {
    private Map<Integer, List<Double>> normalizedValuesMap = new TreeMap<>();

    public void normalizeAndSaveValues(Map<Integer, SuspiciousnessRange> formulaSuspiciousnessRanges, Map<Integer, double[]> fuzzySuspiciousness) {
        for (Map.Entry<Integer, double[]> entry : fuzzySuspiciousness.entrySet()) {
            Integer lineNumber = entry.getKey();
            double[] fuzzyValues = entry.getValue();

            List<Double> normalizedValues = new ArrayList<>();

            for (int formulaIndex = 0; formulaIndex < fuzzyValues.length; formulaIndex++) {
                SuspiciousnessRange range = formulaSuspiciousnessRanges.get(formulaIndex);
                double minSuspiciousnessVal = range.getMinValue();
                double maxSuspiciousnessVal = range.getMaxValue();

                double fuzzyValue = fuzzyValues[formulaIndex];
                double normalizedValue;

                if (maxSuspiciousnessVal == minSuspiciousnessVal) {
                    normalizedValue = 0.0;
                } else {
                    normalizedValue = (fuzzyValue - minSuspiciousnessVal) / (maxSuspiciousnessVal - minSuspiciousnessVal);
                }

                normalizedValues.add(normalizedValue);
            }

            normalizedValuesMap.put(lineNumber, normalizedValues);
        }
    }

    public Map<Integer, List<Double>> getNormalizedValuesMap() {
        return normalizedValuesMap;
    }
}

