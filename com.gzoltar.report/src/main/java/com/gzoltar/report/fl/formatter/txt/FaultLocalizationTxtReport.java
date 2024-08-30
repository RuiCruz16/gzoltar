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
package com.gzoltar.report.fl.formatter.txt;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import com.gzoltar.core.model.Node;
import com.gzoltar.core.model.Transaction;
import com.gzoltar.core.model.TransactionOutcome;
import com.gzoltar.core.runtime.Probe;
import com.gzoltar.core.runtime.ProbeGroup;
import com.gzoltar.core.spectrum.ISpectrum;
import com.gzoltar.core.util.EvidenceCombination;
import com.gzoltar.core.util.NormalizationSaver;
import com.gzoltar.core.util.SuspiciousnessRange;
import com.gzoltar.fl.IFormula;
import com.gzoltar.report.fl.formatter.IFaultLocalizationReportFormatter;

public class FaultLocalizationTxtReport implements IFaultLocalizationReportFormatter {

  private final static String MATRIX_FILE_NAME = "matrix.txt";

  private final static String SPECTRA_FILE_NAME = "spectra.csv";

  private final static String RANKING_EXTENSION_NAME = ".ranking.csv";

  private final static String TESTS_FILES_NAME = "tests.csv";

  private final static String RES_FILES_NAME = "fuzzy.res.csv";

  /**
   * {@inheritDoc}
   */
  @Override
  public void generateFaultLocalizationReport(final File outputDirectory, final ISpectrum spectrum,
      final List<IFormula> formulas) throws IOException {
    if (!outputDirectory.exists()) {
      outputDirectory.mkdirs();
    }

    List<ProbeGroup> probeGroups = new ArrayList<ProbeGroup>(spectrum.getProbeGroups());

    List<Transaction> transactions = spectrum.getTransactions();

    /**
     * Print 'matrix'
     */

    PrintWriter matrixWriter =
        new PrintWriter(outputDirectory + File.separator + MATRIX_FILE_NAME, "UTF-8");

    for (Transaction transaction : transactions) {
      StringBuilder transactionStr = new StringBuilder();

      for (ProbeGroup probeGroup : probeGroups) {
        for (Probe probe : probeGroup.getProbes()) {
          if (transaction.isProbeActived(probeGroup, probe.getArrayIndex())) {
            transactionStr.append("1 ");
          } else {
            transactionStr.append("0 ");
          }
        }
      }

      if (transaction.hasFailed()) {
        transactionStr.append(TransactionOutcome.FAIL.getSymbol());
      } else {
        transactionStr.append(TransactionOutcome.PASS.getSymbol());
      }

      matrixWriter.println(transactionStr.toString());
    }

    matrixWriter.close();

    /**
     * Print 'spectra'
     */

    PrintWriter spectraWriter =
        new PrintWriter(outputDirectory + File.separator + SPECTRA_FILE_NAME, "UTF-8");

    // header
    spectraWriter.println("name");

    // content
    for (ProbeGroup probeGroup : probeGroups) {
      for (Probe probe : probeGroup.getProbes()) {
        spectraWriter.println(probe.getNode().getNameWithLineNumber());
      }
    }

    spectraWriter.close();

    /**
     * Print a ranking file per formula
     */

    List<Node> nodes = new ArrayList<Node>(spectrum.getNodes());


    for (final IFormula formula : formulas) {

      PrintWriter formulaWriter = new PrintWriter(outputDirectory + File.separator
          + formula.getName().toLowerCase() + RANKING_EXTENSION_NAME, "UTF-8");

      // header
      formulaWriter.println("name;suspiciousness_value");

      // sort (DESC) nodes by their suspiciousness value
      Collections.sort(nodes, new Comparator<Node>() {
        @Override
        public int compare(Node node0, Node node1) {
          return Double.compare(node1.getSuspiciousnessValue(formula.getName()),
              node0.getSuspiciousnessValue(formula.getName()));
        }
      });

      for (Node node : nodes) {
        formulaWriter.println(
            node.getNameWithLineNumber() + ";" + node.getSuspiciousnessValue(formula.getName()));
      }

      formulaWriter.close();
    }

    /**
     * Print 'tests'
     */

    PrintWriter testsWriter =
        new PrintWriter(outputDirectory + File.separator + TESTS_FILES_NAME, "UTF-8");

    // header
    testsWriter.println("name,outcome,runtime,stacktrace");

    // content
    for (Transaction transaction : transactions) {
      testsWriter.println(transaction.getName() + ","
          + (transaction.hasFailed() ? TransactionOutcome.FAIL.name()
              : TransactionOutcome.PASS.name())
          + "," + transaction.getRuntime() + "," + transaction.getStackTrace());
    }

    testsWriter.close();

    /**
     * Print 'fuzzy.res'
     */

    PrintWriter resultsWriter = new PrintWriter(outputDirectory + File.separator + RES_FILES_NAME, "UTF-8");
    resultsWriter.println("name;suspiciousness_value");

    Map<Integer, double[]> fuzzySuspiciousness = new TreeMap<>();
    Map<Integer, SuspiciousnessRange> formulaSuspiciousnessRanges = new HashMap<>();
    NormalizationSaver normalizationSaver = new NormalizationSaver();

    int formulaIndex = 0;
    int window_size = 3;

    for (final IFormula formula : formulas) {
      Map<Integer, Double> originalSuspiciousness = new TreeMap<>();

      for (Node node : nodes) {
        originalSuspiciousness.put(node.getLineNumber(), node.getSuspiciousnessValue(formula.getName()));
      }
      //System.out.println("originalSuspiciousness: " + originalSuspiciousness);

      double minFuzzyValue = Double.MAX_VALUE;
      double maxFuzzyValue = Double.MIN_VALUE;

      for (Integer lineNumber : originalSuspiciousness.keySet()) {
        double sum = 0.0;
        int count = 0;

        for (int i = -window_size; i <= window_size; i++) {
          int currentLine = lineNumber + i;
          if (originalSuspiciousness.containsKey(currentLine)) {
            sum += originalSuspiciousness.get(currentLine);
            count++;
          }
        }

        double fuzzyValue = sum / count;

        if (fuzzyValue < minFuzzyValue) {
          minFuzzyValue = fuzzyValue;
        }
        if (fuzzyValue > maxFuzzyValue) {
          maxFuzzyValue = fuzzyValue;
        }

        double[] fuzzyValues = fuzzySuspiciousness.computeIfAbsent(lineNumber, k -> new double[3]);
        fuzzyValues[formulaIndex] = fuzzyValue;

        //System.out.println("Line Number: " + lineNumber);
        //System.out.println("Fuzzy Value: " + fuzzyValue);
        //System.out.println("Fuzzy Values Array: " + Arrays.toString(fuzzyValues));

        fuzzySuspiciousness.put(lineNumber, fuzzyValues);
      }

      formulaSuspiciousnessRanges.put(formulaIndex, new SuspiciousnessRange(minFuzzyValue, maxFuzzyValue));
      //System.out.println("formula index: " + formulaIndex + " minFuzzyValue: " + minFuzzyValue + " maxFuzzyValue: " + maxFuzzyValue);
      formulaIndex++;
    }

    normalizationSaver.normalizeAndSaveValues(formulaSuspiciousnessRanges, fuzzySuspiciousness);
    Map<Integer, List<Double>> normalizedValues = normalizationSaver.getNormalizedValuesMap();
    //System.out.println("normalizedValues: " + normalizedValues.toString());

    for (Map.Entry<Integer, List<Double>> entry : normalizedValues.entrySet()) {
      Integer lineNumber = entry.getKey();
      List<Double> values = entry.getValue();
      double m1 = values.get(0);
      double m2 = values.get(1);
      double m3 = values.get(2);
      System.out.println("m1: " + m1 + " m2: " + m2 + " m3: " + m3);

      double m12 = new EvidenceCombination().calculateDempster(m1, m2);
      System.out.println("m12: " + m12);
      double m123 = new EvidenceCombination().calculateDempster(m12, m3);
      System.out.println("m123: " + m123);

      resultsWriter.println("line_" + lineNumber + ";" + m123);
    }

    resultsWriter.close();
  }
}
