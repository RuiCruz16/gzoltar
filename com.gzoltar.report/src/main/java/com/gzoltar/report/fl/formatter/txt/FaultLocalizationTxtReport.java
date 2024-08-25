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
import com.gzoltar.core.util.NormalizationSaver;
import com.gzoltar.core.util.SuspiciousnessRange;
import com.gzoltar.fl.IFormula;
import com.gzoltar.report.fl.formatter.IFaultLocalizationReportFormatter;

public class FaultLocalizationTxtReport implements IFaultLocalizationReportFormatter {

  private final static String MATRIX_FILE_NAME = "matrix.txt";

  private final static String SPECTRA_FILE_NAME = "spectra.csv";

  private final static String RANKING_EXTENSION_NAME = ".ranking.csv";

  private final static String TESTS_FILES_NAME = "tests.csv";

  private final static String VALS_FILES_NAME = "newvals.csv";

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
    Map<String, SuspiciousnessRange> formulaSuspiciousnessRanges = new HashMap<>();

    for (final IFormula formula : formulas) {

      SuspiciousnessRange suspiciousnessRange = new SuspiciousnessRange(Double.MAX_VALUE, Double.NEGATIVE_INFINITY);

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
        suspiciousnessRange.updateRange(node.getSuspiciousnessValue(formula.getName()));

        formulaWriter.println(
            node.getNameWithLineNumber() + ";" + node.getSuspiciousnessValue(formula.getName()));
      }

      formulaWriter.close();

      formulaSuspiciousnessRanges.put(formula.getName(), suspiciousnessRange);
      //System.out.println("Max: " + suspiciousnessRange.getMaxValue() + " Min: " + suspiciousnessRange.getMinValue() + " Formula: " + formula.getName());
    }

    NormalizationSaver normalizationSaver = new NormalizationSaver();

    normalizationSaver.normalizeAndSaveValues(formulaSuspiciousnessRanges, nodes);

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
     * Print 'newvals'
     */

    PrintWriter newValWriter = new PrintWriter(outputDirectory + File.separator + VALS_FILES_NAME, "UTF-8");

    newValWriter.println("linenumber;suspiciousness_value");

    Map<Integer, List<Double>> normalizedValues = normalizationSaver.getNormalizedValuesMap();

    for (Map.Entry<Integer, List<Double>> entry : normalizedValues.entrySet()) {
      Integer lineNumber = entry.getKey();
      List<Double> values = entry.getValue();
      double m1 = values.get(0);
      double m2 = values.get(1);
      double m3 = values.get(2);
      System.out.println("Line " + lineNumber + ": m1 = " + m1 + ", m2 = " + m2 + ", m3 = " + m3);

      double K1 = m1 * (1 - m2) + (1 - m1) * m2;
      System.out.println("Calculated K1: " + K1);
      double m12, m12neg;

      if (1 - K1 == 0) {
        m12 = 0.0;
        m12neg = 0.0;
      } else {
        m12 = (m1 * m2) / (1 - K1);
        m12neg = ((1 - m1) * (1 - m2)) / (1 - K1);
      }

      System.out.println("Calculated m12: " + m12);
      System.out.println("Calculated m12neg: " + m12neg);

      double K2 = m12 * (1 - m3) + m12neg * m3;
      System.out.println("Calculated K2: " + K2);
      double m123;

      if (1 - K2 == 0) {
        m123 = 0.0;
      } else {
        m123 = (m12 * m3) / (1 - K2);
      }

      System.out.println("Calculated m123: " + m123);
      newValWriter.println("line_" + lineNumber + ";" + m123);
    }
    newValWriter.close();

  }
}
