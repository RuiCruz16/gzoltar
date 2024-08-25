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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NormalizationSaver {
    private Map<Integer, List<Double>> normalizedValuesMap = new HashMap<>();

    public void normalizeAndSaveValues(Map<String, SuspiciousnessRange> formulaSuspiciousnessRanges, List<Node> nodes) {
        for (Map.Entry<String, SuspiciousnessRange> entry : formulaSuspiciousnessRanges.entrySet()) {
            String formulaName = entry.getKey();
            SuspiciousnessRange range = entry.getValue();
            double minSuspiciousnessVal = range.getMinValue();
            double maxSuspiciousnessVal = range.getMaxValue();

            for (Node node : nodes) {
                double suspiciousnessValue = node.getSuspiciousnessValue(formulaName);

                double normalizedValue;
                if (maxSuspiciousnessVal == minSuspiciousnessVal) {
                    normalizedValue = 0.0;
                } else {
                    normalizedValue = (suspiciousnessValue - minSuspiciousnessVal) /
                            (maxSuspiciousnessVal - minSuspiciousnessVal);
                }

                int lineNumber = node.getLineNumber();
                normalizedValuesMap.computeIfAbsent(lineNumber, k -> new ArrayList<>()).add(normalizedValue);
            }
        }
    }

    public Map<Integer, List<Double>> getNormalizedValuesMap() {
        return normalizedValuesMap;
    }
}
