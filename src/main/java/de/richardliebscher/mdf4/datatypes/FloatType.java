/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.datatypes;


import java.util.Optional;

/**
 * Floating-point type.
 */
public final class FloatType implements DataType {

  private final int bitCount;
  private final Integer precision;

  /**
   * Construct a float type.
   *
   * @param bitCount  Bit count
   * @param precision Nullable precision
   */
  public FloatType(int bitCount, Integer precision) {
    if (bitCount <= 0) {
      throw new IllegalArgumentException("bitCount <= 0");
    }

    this.bitCount = bitCount;
    this.precision = precision;
  }

  /**
   * Return number of bits used by type.
   */
  public int getBitCount() {
    return bitCount;
  }

  /**
   * Return count of decimal places for display.
   *
   * <p>{@code Integer.MAX_VALUE} means infinite precision.
   */
  public Optional<Integer> getPrecision() {
    return Optional.of(precision);
  }

  @Override
  public <R, E extends Throwable> R accept(Visitor<R, E> visitor) throws E {
    return visitor.visit(this);
  }
}
